package foatto.server.ds

import foatto.server.entity.SensorEntity
import foatto.server.model.SensorConfigGeo
import foatto.server.sql.CoreAdvancedConnection
import kotlinx.datetime.TimeZone
import java.nio.channels.SocketChannel
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.round

abstract class MMSNioHandler : AbstractTelematicNioHandler() {

    companion object {
        val timeZone: TimeZone = TimeZone.currentSystemDefault()
    }

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override val configSessionLogPath: String = "mms_log_session"
    override val configJournalLogPath: String = "mms_log_journal"

    //--- серийный номер прибора
    protected var serialNo = ""

    //--- номер версии прошивки
    protected var fwVersion = ""

    //--- конфигурация устройства
    protected var deviceConfig: DeviceConfig? = null
    protected val sensorConfigs: MutableMap<Int, MutableList<SensorEntity>> = mutableMapOf()               // portNum to sensorEntities
    protected val sensorCalibrations: MutableMap<Int, MutableList<Pair<Double, Double>>> = mutableMapOf()  // sensorId to list of <sensorValue to dataValue> pairs

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun prepareErrorCommand(dataWorker: CoreNioWorker) {
        MMSTelematicFunction.writeError(
            conn = dataWorker.conn,
            dirSessionLog = dirSessionLog,
            timeZone = timeZone,
            deviceConfig = deviceConfig,
            fwVersion = fwVersion,
            begTime = begTime,
            address = selectionKey?.let { sk ->
                (sk.channel() as SocketChannel).remoteAddress.toString() + " -> " + (sk.channel() as SocketChannel).localAddress.toString()
            } ?: "(unknown remote address)",
            status = status,
            errorText = "Disconnect from device ID = $serialNo",
            dataCount = dataCount,
            firstPointTime = firstPointTime,
            lastPointTime = lastPointTime,
        )
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    //--- для Galileo-приборов
    protected fun loadDeviceConfig(conn: CoreAdvancedConnection): DeviceConfig? =
        DeviceConfig.getDeviceConfig(conn, serialNo)?.let { dc ->
            status += " ID 2;"

            if (dc.objectId != 0) {
                loadSensorConfigs(conn, dc)
            }
            dc
        } ?: run {
            //--- неизвестный контроллер
            MMSTelematicFunction.writeError(
                conn = conn,
                dirSessionLog = dirSessionLog,
                timeZone = timeZone,
                deviceConfig = deviceConfig,
                fwVersion = fwVersion,
                begTime = begTime,
                address = selectionKey?.let { sk ->
                    (sk.channel() as SocketChannel).remoteAddress.toString() + " -> " + (sk.channel() as SocketChannel).localAddress.toString()
                } ?: "(unknown remote address)",
                status = status,
                errorText = "Unknown device ID = $serialNo",
                dataCount = dataCount,
                firstPointTime = firstPointTime,
                lastPointTime = lastPointTime,
            )
            CoreTelematicFunction.writeJournal(
                dirJournalLog = dirJournalLog,
                timeZone = timeZone,
                address = (selectionKey!!.channel() as SocketChannel).remoteAddress.toString() + " -> " + (selectionKey!!.channel() as SocketChannel).localAddress.toString(),
                errorText = "Unknown device ID = $serialNo",
            )
            null
        }

    //--- для Galileo-приборов
    private fun loadSensorConfigs(conn: CoreAdvancedConnection, dc: DeviceConfig) {
        sensorConfigs.clear()

        var rs = conn.executeQuery(
            """
                SELECT port_num , id , sensor_type , beg_time , end_time ,  
                    min_moving_time , min_parking_time , min_over_speed_time , 
                        is_absolute_run , speed_round_rule , run_koef ,
                        is_use_pos , is_use_speed , is_use_run ,
                    ignore_min_sensor , ignore_max_sensor , dim ,
                    active_value , bound_value , idle_border , limit_border , min_on_time , min_off_time ,
                    analog_min_view , analog_max_view , analog_min_limit , analog_max_limit , smooth_time ,   
                    is_absolute_count , in_out_type ,  
                    container_type ,
                    energo_phase ,                             
                    liquid_name , liquid_norm
                FROM MMS_sensor
                WHERE id <> 0 
                AND object_id = ${dc.objectId}                         
            """
        )
        while (rs.next()) {
            var pos = 1

            val portNum = rs.getInt(pos++)
            val sensorEntities = sensorConfigs.getOrPut(portNum) {
                mutableListOf()
            }
            //--- для Galileo-приборов
            sensorEntities += SensorEntity(
                id = rs.getInt(pos++),
                obj = null,
                name = null,
                group = null,
                descr = null,
                portNum = portNum,
                sensorType = rs.getInt(pos++),
                begTime = rs.getInt(pos++),
                endTime = rs.getInt(pos++),
                serialNo = null,

                minMovingTime = rs.getInt(pos++),
                minParkingTime = rs.getInt(pos++),
                minOverSpeedTime = rs.getInt(pos++),
                isAbsoluteRun = rs.getInt(pos++) != 0,
                speedRoundRule = rs.getInt(pos++),
                runKoef = rs.getDouble(pos++),
                isUsePos = rs.getInt(pos++) != 0,
                isUseSpeed = rs.getInt(pos++) != 0,
                isUseRun = rs.getInt(pos++) != 0,

                minIgnore = rs.getDouble(pos++),
                maxIgnore = rs.getDouble(pos++),
                dim = rs.getString(pos++),

                isAboveBorder = rs.getInt(pos++) != 0,
                onOffBorder = rs.getDouble(pos++),
                idleBorder = rs.getDouble(pos++),
                limitBorder = rs.getDouble(pos++),
                minOnTime = rs.getInt(pos++),
                minOffTime = rs.getInt(pos++),

                minView = rs.getDouble(pos++),
                maxView = rs.getDouble(pos++),
                minLimit = rs.getDouble(pos++),
                maxLimit = rs.getDouble(pos++),
                smoothTime = rs.getInt(pos++),
                indicatorDelimiterCount = null,
                indicatorMultiplicator = null,

                isAbsoluteCount = rs.getInt(pos++) != 0,
                inOutType = rs.getInt(pos++),

                containerType = rs.getInt(pos++),

                phase = rs.getInt(pos++),

                liquidName = rs.getString(pos++),
                liquidNorm = rs.getDouble(pos++),

                schemeX = null,
                schemeY = null,
            )
        }
        rs.close()

        sensorCalibrations.clear()

        sensorConfigs.values.forEach { sensorEntities ->
            sensorEntities.forEach { sensorEntity ->
                val pairs = sensorCalibrations.getOrPut(sensorEntity.id) {
                    mutableListOf()
                }
                rs = conn.executeQuery(
                    """
                        SELECT value_sensor , value_data
                        FROM MMS_sensor_calibration
                        WHERE sensor_id = ${sensorEntity.id}
                        ORDER BY value_sensor 
                    """
                )
                while (rs.next()) {
                    var pos = 1

                    pairs += rs.getDouble(pos++) to rs.getDouble(pos++)
                }
                rs.close()
            }
        }
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    protected fun roundSpeed(speed: Double): Int =
        when (deviceConfig!!.speedRoundRule) {
            SensorConfigGeo.SPEED_ROUND_RULE_LESS -> floor(speed).toInt()
            SensorConfigGeo.SPEED_ROUND_RULE_GREATER -> ceil(speed).toInt()
            SensorConfigGeo.SPEED_ROUND_RULE_STANDART -> round(speed).toInt()
            else -> round(speed).toInt()
        }

}
