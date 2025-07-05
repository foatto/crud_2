package foatto.server.service

import foatto.core.util.getCurrentTimeInt
import foatto.server.ds.CoreTelematicFunction
import foatto.server.ds.DeviceConfig
import foatto.server.ds.MMSTelematicFunction
import foatto.server.ds.PortNumbers
import foatto.server.ds.PulsarData
import foatto.server.entity.SensorEntity
import foatto.server.repository.ObjectRepository
import foatto.server.repository.SensorCalibrationRepository
import foatto.server.repository.SensorRepository
import foatto.server.util.AdvancedByteBuffer
import foatto.server.util.AdvancedLogger
import jakarta.persistence.EntityManager
import jakarta.servlet.http.HttpServletRequest
import kotlinx.datetime.TimeZone
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.io.File

@Service
class MMSPulsarDataService(
    private val entityManager: EntityManager,
    private val objectRepository: ObjectRepository,
    private val sensorRepository: SensorRepository,
    private val sensorCalibrationRepository: SensorCalibrationRepository,
) {

    private val METHOD_NAME = "MMSPulsarDataController.getPulsarData"
    private val BLOCK_ID = "PulsarMeasure"
    private val ID_PREFIX = "ID"

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    @Value("\${mms_log_session}")
    val configSessionLogPath: String = ""

    @Value("\${mms_log_journal}")
    val configJournalLogPath: String = ""

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    fun getPulsarData(
        request: HttpServletRequest,
        arrData: Array<PulsarData>,
    ) {
        val timeZone: TimeZone = TimeZone.currentSystemDefault()

        //--- по 16 типизированных датчиков от юриковского радиоудлиннителя
        val tmLLSLevel = sortedMapOf<Int, Double>()
        val tmGalileoVoltage = sortedMapOf<Int, Double>()
        val tmGalileoCount = sortedMapOf<Int, Double>()

        //--- 4 вида счётчиков энергии от сброса (активная прямая, активная обратная, реактивная прямая, реактивная обратная)
        val tmEnergoCountActiveDirect = sortedMapOf<Int, Double>()
        val tmEnergoCountActiveReverse = sortedMapOf<Int, Double>()
        val tmEnergoCountReactiveDirect = sortedMapOf<Int, Double>()
        val tmEnergoCountReactiveReverse = sortedMapOf<Int, Double>()

        //--- напряжение по фазам
        val tmEnergoVoltageA = sortedMapOf<Int, Double>()
        val tmEnergoVoltageB = sortedMapOf<Int, Double>()
        val tmEnergoVoltageC = sortedMapOf<Int, Double>()

        //--- ток по фазам
        val tmEnergoCurrentA = sortedMapOf<Int, Double>()
        val tmEnergoCurrentB = sortedMapOf<Int, Double>()
        val tmEnergoCurrentC = sortedMapOf<Int, Double>()

        //--- коэффициент мощности по фазам
        val tmEnergoPowerKoefA = sortedMapOf<Int, Double>()
        val tmEnergoPowerKoefB = sortedMapOf<Int, Double>()
        val tmEnergoPowerKoefC = sortedMapOf<Int, Double>()

        //--- energy power (active, reactive, full/summary) by phase by 4 indicators
        val tmEnergoPowerActiveA = sortedMapOf<Int, Double>()
        val tmEnergoPowerActiveB = sortedMapOf<Int, Double>()
        val tmEnergoPowerActiveC = sortedMapOf<Int, Double>()
        val tmEnergoPowerReactiveA = sortedMapOf<Int, Double>()
        val tmEnergoPowerReactiveB = sortedMapOf<Int, Double>()
        val tmEnergoPowerReactiveC = sortedMapOf<Int, Double>()
        val tmEnergoPowerFullA = sortedMapOf<Int, Double>()
        val tmEnergoPowerFullB = sortedMapOf<Int, Double>()
        val tmEnergoPowerFullC = sortedMapOf<Int, Double>()
        val tmEnergoPowerActiveABC = sortedMapOf<Int, Double>()
        val tmEnergoPowerReactiveABC = sortedMapOf<Int, Double>()
        val tmEnergoPowerFullABC = sortedMapOf<Int, Double>()

        val tmEnergoTransformKoefCurrent = sortedMapOf<Int, Double>()
        val tmEnergoTransformKoefVoltage = sortedMapOf<Int, Double>()

        //--- датчики ЭМИС/Эльметро
        val tmEmisDensity = sortedMapOf<Int, Double>()              // плотность
        val tmEmisTemperature = sortedMapOf<Int, Double>()          // температура
        val tmEmisMassFlow = sortedMapOf<Int, Double>()             // массовый расход
        val tmEmisVolumeFlow = sortedMapOf<Int, Double>()           // объёмный расход
        val tmEmisAccumulatedMass = sortedMapOf<Int, Double>()      // накопленная масса
        val tmEmisAccumulatedVolume = sortedMapOf<Int, Double>()    // накопленный объём

        //--- датчики ПМП-201/218
        val tmPMPLevel = sortedMapOf<Int, Double>()         // уровень [м]
        val tmPMPTemperature = sortedMapOf<Int, Double>()   // температура
        val tmPMPVolume = sortedMapOf<Int, Double>()        // (накопленный) объём [куб.м]
        val tmPMPMass = sortedMapOf<Int, Double>()          // (накопленная) масса [тонна]
        val tmPMPDensity = sortedMapOf<Int, Double>()       // плотность [г/куб.см == тонна/куб.м]

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

        //--- количество считанных и записанных блоков данных (например, точек)
        var dataCount = 0

        //--- время первого и последнего блока данных (например, точки)
        var firstPointTime = 0
        var lastPointTime = 0

        val dirSessionLog = File(configSessionLogPath)
        val dirJournalLog = File(configJournalLogPath)

        if (arrData.isEmpty()) {
            outDeviceParseError(timeZone, dirJournalLog, request.remoteAddr, "Empty data array")
            return
        }

        //--- время начала сессии
        val begTime = getCurrentTimeInt()
        //--- запись состояния сессии
        var status = "Init; Start;"

        //--- номер версии прошивки
        val fwVersion = "1"

        //--- first initial row in data packet array
        val headerData = arrData[0]
        if (headerData.blockID != BLOCK_ID) {
            outDeviceParseError(timeZone, dirJournalLog, request.remoteAddr, "Wrong first blockID == '${headerData.blockID}'")
            return
        }
        if (headerData.deviceID.isNullOrBlank()) {
            outDeviceParseError(timeZone, dirJournalLog, request.remoteAddr, "Empty deviceID")
            return
        }

        val serialNo: String = headerData.deviceID!!

        var dc: DeviceConfig? = null
        ApplicationService.withConnection(entityManager) { conn ->
            dc = DeviceConfig.getDeviceConfig(conn, serialNo)
        }

        val deviceConfig: DeviceConfig = dc?.let {
            status += " ID 2.0;"
            dc
        } ?: run {
            outDeviceParseError(timeZone, dirJournalLog, request.remoteAddr, "Unlinked object or unknown serialNo = '$serialNo'")
            return
        }

//--- экономный, но неправильный вариант - время точки может быть из прошлого,
//--- когда требуемые датчики ещё были или были другими
//        val sensorConfigs = mutableMapOf<Int, MutableList<SensorEntity>>()                 // portNum to sensorEntities
//        val sensorCalibrations = mutableMapOf<Int, MutableList<Pair<Double, Double>>>()    // sensorId to list of <sensorValue to dataValue> pairs
//
//        objectRepository.findByIdOrNull(deviceConfig.objectId)?.let { objectEntity ->
//            sensorRepository.findByObjAndTime(objectEntity, getCurrentTimeInt()).forEach { sensorEntity ->
//                //--- fill sensor datas
//                sensorEntity.portNum?.let { portNum ->
//                    val sensorEntitiesByPortNum = sensorConfigs.getOrPut(portNum) { mutableListOf() }
//                    sensorEntitiesByPortNum += sensorEntity
//                }
//                //--- fill sensors calibration datas
//                val sensorCalibrationPairs = mutableListOf<Pair<Double, Double>>()
//                sensorCalibrationRepository.findBySensorOrderBySensorValue(sensorEntity).forEach { sensorCalibrationEntity ->
//                    sensorCalibrationEntity.sensorValue?.let { sensorValue ->
//                        sensorCalibrationEntity.dataValue?.let { dataValue ->
//                            sensorCalibrationPairs += sensorValue to dataValue
//                        }
//                    }
//                }
//                sensorCalibrations[sensorEntity.id] = sensorCalibrationPairs
//            }
//        }

        for (i in 1..arrData.lastIndex) {
            val pulsarData = arrData[i]
            pulsarData.dateTime?.epochSeconds?.toInt()?.let { pointTime ->

                val curTime = getCurrentTimeInt()
                if (pointTime > curTime - CoreTelematicFunction.MAX_PAST_TIME && pointTime < curTime + CoreTelematicFunction.MAX_FUTURE_TIME) {

                    //--- затратный, но правильный вариант - настройки датчиков берутся для каждого конкретного времени
                    val sensorConfigs = mutableMapOf<Int, MutableList<SensorEntity>>()                 // portNum to sensorEntities
                    val sensorCalibrations = mutableMapOf<Int, MutableList<Pair<Double, Double>>>()    // sensorId to list of <sensorValue to dataValue> pairs

                    objectRepository.findByIdOrNull(deviceConfig.objectId)?.let { objectEntity ->
                        sensorRepository.findByObjAndTime(objectEntity, pointTime).forEach { sensorEntity ->
                            //--- fill sensor datas
                            sensorEntity.portNum?.let { portNum ->
                                val sensorEntitiesByPortNum = sensorConfigs.getOrPut(portNum) { mutableListOf() }
                                sensorEntitiesByPortNum += sensorEntity
                            }
                            //--- fill sensors calibration datas
                            val sensorCalibrationPairs = mutableListOf<Pair<Double, Double>>()
                            sensorCalibrationRepository.findBySensorOrderBySensorValue(sensorEntity).forEach { sensorCalibrationEntity ->
                                sensorCalibrationEntity.sensorValue?.let { sensorValue ->
                                    sensorCalibrationEntity.dataValue?.let { dataValue ->
                                        sensorCalibrationPairs += sensorValue to dataValue
                                    }
                                }
                            }
                            sensorCalibrations[sensorEntity.id] = sensorCalibrationPairs
                        }
                    }

                    pulsarData.vals?.forEach { hmData ->
                        hmData.forEach { (sId, value) ->
                            if (sId.startsWith(ID_PREFIX)) {
                                sId.substring(ID_PREFIX.length).toIntOrNull(16)?.let { id ->
                                    when (id) {
                                        in 0x0100..0x010F -> tmLLSLevel[id - 0x0100] = value
                                        in 0x0140..0x014F -> tmGalileoVoltage[id - 0x0140] = value
                                        in 0x0180..0x018F -> tmGalileoCount[id - 0x0180] = value

                                        in 0x01C1..0x01C4 -> tmEnergoCountActiveDirect[id - 0x01C1] = value
                                        in 0x0201..0x0204 -> tmEnergoCountActiveReverse[id - 0x0201] = value
                                        in 0x0241..0x0244 -> tmEnergoCountReactiveDirect[id - 0x0241] = value
                                        in 0x0281..0x0284 -> tmEnergoCountReactiveReverse[id - 0x0281] = value

                                        in 0x02C1..0x02C4 -> tmEnergoVoltageA[id - 0x02C1] = value
                                        in 0x0301..0x0304 -> tmEnergoVoltageB[id - 0x0301] = value
                                        in 0x0341..0x0344 -> tmEnergoVoltageC[id - 0x0341] = value

                                        in 0x0381..0x0384 -> tmEnergoCurrentA[id - 0x0381] = value
                                        in 0x03C1..0x03C4 -> tmEnergoCurrentB[id - 0x03C1] = value
                                        in 0x0401..0x0404 -> tmEnergoCurrentC[id - 0x0401] = value

                                        in 0x0441..0x0444 -> tmEnergoPowerKoefA[id - 0x0441] = value
                                        in 0x0481..0x0484 -> tmEnergoPowerKoefB[id - 0x0481] = value
                                        in 0x0501..0x0504 -> tmEnergoPowerKoefC[id - 0x0501] = value

                                        in 0x0510..0x0513 -> tmEnergoPowerActiveA[id - 0x0510] = value
                                        in 0x0514..0x0517 -> tmEnergoPowerActiveB[id - 0x0514] = value
                                        in 0x0518..0x051B -> tmEnergoPowerActiveC[id - 0x0518] = value

                                        in 0x051C..0x051F -> tmEnergoPowerReactiveA[id - 0x051C] = value
                                        in 0x0520..0x0523 -> tmEnergoPowerReactiveB[id - 0x0520] = value
                                        in 0x0524..0x0527 -> tmEnergoPowerReactiveC[id - 0x0524] = value

                                        in 0x0528..0x052B -> tmEnergoPowerFullA[id - 0x0528] = value
                                        in 0x052C..0x052F -> tmEnergoPowerFullB[id - 0x052C] = value
                                        in 0x0530..0x0533 -> tmEnergoPowerFullC[id - 0x0530] = value

                                        in 0x0541..0x0544 -> tmEmisMassFlow[id - 0x0541] = value

                                        in 0x0550..0x0553 -> tmEnergoTransformKoefCurrent[id - 0x0550] = value
                                        in 0x0560..0x0563 -> tmEnergoTransformKoefVoltage[id - 0x0560] = value

                                        in 0x0581..0x0584 -> tmEmisDensity[id - 0x0581] = value
                                        in 0x05C1..0x05C4 -> tmEmisTemperature[id - 0x05C1] = value
                                        in 0x0601..0x0604 -> tmEmisVolumeFlow[id - 0x0601] = value
                                        in 0x0641..0x0644 -> tmEmisAccumulatedMass[id - 0x0641] = value
                                        in 0x0681..0x0684 -> tmEmisAccumulatedVolume[id - 0x0681] = value

                                        in 0x0700..0x0703 -> tmEnergoPowerActiveABC[id - 0x0700] = value
                                        in 0x0710..0x0713 -> tmEnergoPowerReactiveABC[id - 0x0710] = value
                                        in 0x0720..0x0723 -> tmEnergoPowerFullABC[id - 0x0720] = value

                                        in 0x0800..0x080F -> tmPMPLevel[id - 0x0800] = value
                                        in 0x0810..0x081F -> tmPMPTemperature[id - 0x0810] = value
                                        in 0x0820..0x082F -> tmPMPVolume[id - 0x0820] = value
                                        in 0x0830..0x083F -> tmPMPMass[id - 0x0830] = value
                                        in 0x0840..0x084F -> tmPMPDensity[id - 0x0840] = value

                                        else -> outDataParseError(serialNo, "Unknown ID value == '$sId'")
                                    }
                                } ?: run {
                                    outDataParseError(serialNo, "Wrong ID value == '$sId'")
                                }
                            } else {
                                outDataParseError(serialNo, "Wrong ID format == '$sId'")
                            }
                        }
                    } ?: run {
                        outDataParseError(serialNo, "Vals is null")
                    }

                    ApplicationService.withConnection(entityManager) { conn ->
                        val bbData = AdvancedByteBuffer(CoreTelematicFunction.MAX_PORT_PER_DEVICE * 8)

                        MMSTelematicFunction.saveSensorData(conn, deviceConfig.deviceIndex, sensorConfigs, sensorCalibrations, pointTime, tmGalileoCount, PortNumbers.GALILEO_COUNT_110, bbData)
                        MMSTelematicFunction.saveSensorData(conn, deviceConfig.deviceIndex, sensorConfigs, sensorCalibrations, pointTime, tmLLSLevel, PortNumbers.LLS_LEVEL_120, bbData)
                        MMSTelematicFunction.saveSensorData(conn, deviceConfig.deviceIndex, sensorConfigs, sensorCalibrations, pointTime, tmGalileoVoltage, PortNumbers.GALILEO_VOLTAGE_140, bbData)

                        //--- данные по электросчётчику ---

                        //--- значения счётчиков от последнего сброса (активная/реактивная прямая/обратная)
                        MMSTelematicFunction.saveSensorData(conn, deviceConfig.deviceIndex, sensorConfigs, sensorCalibrations, pointTime, tmEnergoCountActiveDirect, PortNumbers.MERCURY_COUNT_ACTIVE_DIRECT_160, bbData)
                        MMSTelematicFunction.saveSensorData(conn, deviceConfig.deviceIndex, sensorConfigs, sensorCalibrations, pointTime, tmEnergoCountActiveReverse, PortNumbers.MERCURY_COUNT_ACTIVE_REVERSE_164, bbData)
                        MMSTelematicFunction.saveSensorData(conn, deviceConfig.deviceIndex, sensorConfigs, sensorCalibrations, pointTime, tmEnergoCountReactiveDirect, PortNumbers.MERCURY_COUNT_REACTIVE_DIRECT_168, bbData)
                        MMSTelematicFunction.saveSensorData(conn, deviceConfig.deviceIndex, sensorConfigs, sensorCalibrations, pointTime, tmEnergoCountReactiveReverse, PortNumbers.MERCURY_COUNT_REACTIVE_REVERSE_172, bbData)

                        //--- напряжение по фазам A1..4, B1..4, C1..4
                        MMSTelematicFunction.saveSensorData(conn, deviceConfig.deviceIndex, sensorConfigs, sensorCalibrations, pointTime, tmEnergoVoltageA, PortNumbers.MERCURY_VOLTAGE_A_180, bbData)
                        MMSTelematicFunction.saveSensorData(conn, deviceConfig.deviceIndex, sensorConfigs, sensorCalibrations, pointTime, tmEnergoVoltageB, PortNumbers.MERCURY_VOLTAGE_B_184, bbData)
                        MMSTelematicFunction.saveSensorData(conn, deviceConfig.deviceIndex, sensorConfigs, sensorCalibrations, pointTime, tmEnergoVoltageC, PortNumbers.MERCURY_VOLTAGE_C_188, bbData)

                        //--- ток по фазам A1..4, B1..4, C1..4
                        MMSTelematicFunction.saveSensorData(conn, deviceConfig.deviceIndex, sensorConfigs, sensorCalibrations, pointTime, tmEnergoCurrentA, PortNumbers.MERCURY_CURRENT_A_200, bbData)
                        MMSTelematicFunction.saveSensorData(conn, deviceConfig.deviceIndex, sensorConfigs, sensorCalibrations, pointTime, tmEnergoCurrentB, PortNumbers.MERCURY_CURRENT_B_204, bbData)
                        MMSTelematicFunction.saveSensorData(conn, deviceConfig.deviceIndex, sensorConfigs, sensorCalibrations, pointTime, tmEnergoCurrentC, PortNumbers.MERCURY_CURRENT_C_208, bbData)

                        //--- коэффициент мощности по фазам A1..4, B1..4, C1..4
                        MMSTelematicFunction.saveSensorData(conn, deviceConfig.deviceIndex, sensorConfigs, sensorCalibrations, pointTime, tmEnergoPowerKoefA, PortNumbers.MERCURY_POWER_KOEF_A_220, bbData)
                        MMSTelematicFunction.saveSensorData(conn, deviceConfig.deviceIndex, sensorConfigs, sensorCalibrations, pointTime, tmEnergoPowerKoefB, PortNumbers.MERCURY_POWER_KOEF_B_224, bbData)
                        MMSTelematicFunction.saveSensorData(conn, deviceConfig.deviceIndex, sensorConfigs, sensorCalibrations, pointTime, tmEnergoPowerKoefC, PortNumbers.MERCURY_POWER_KOEF_C_228, bbData)

                        //--- активная мощность по фазам A1..4, B1..4, C1..4
                        MMSTelematicFunction.saveSensorData(conn, deviceConfig.deviceIndex, sensorConfigs, sensorCalibrations, pointTime, tmEnergoPowerActiveA, PortNumbers.MERCURY_POWER_ACTIVE_A_232, bbData)
                        MMSTelematicFunction.saveSensorData(conn, deviceConfig.deviceIndex, sensorConfigs, sensorCalibrations, pointTime, tmEnergoPowerActiveB, PortNumbers.MERCURY_POWER_ACTIVE_B_236, bbData)
                        MMSTelematicFunction.saveSensorData(conn, deviceConfig.deviceIndex, sensorConfigs, sensorCalibrations, pointTime, tmEnergoPowerActiveC, PortNumbers.MERCURY_POWER_ACTIVE_C_240, bbData)

                        //--- реактивная мощность по фазам A1..4, B1..4, C1..4
                        MMSTelematicFunction.saveSensorData(conn, deviceConfig.deviceIndex, sensorConfigs, sensorCalibrations, pointTime, tmEnergoPowerReactiveA, PortNumbers.MERCURY_POWER_REACTIVE_A_244, bbData)
                        MMSTelematicFunction.saveSensorData(conn, deviceConfig.deviceIndex, sensorConfigs, sensorCalibrations, pointTime, tmEnergoPowerReactiveB, PortNumbers.MERCURY_POWER_REACTIVE_B_248, bbData)
                        MMSTelematicFunction.saveSensorData(conn, deviceConfig.deviceIndex, sensorConfigs, sensorCalibrations, pointTime, tmEnergoPowerReactiveC, PortNumbers.MERCURY_POWER_REACTIVE_C_252, bbData)

                        //--- полная мощность по фазам A1..4, B1..4, C1..4
                        MMSTelematicFunction.saveSensorData(conn, deviceConfig.deviceIndex, sensorConfigs, sensorCalibrations, pointTime, tmEnergoPowerFullA, PortNumbers.MERCURY_POWER_FULL_A_256, bbData)
                        MMSTelematicFunction.saveSensorData(conn, deviceConfig.deviceIndex, sensorConfigs, sensorCalibrations, pointTime, tmEnergoPowerFullB, PortNumbers.MERCURY_POWER_FULL_B_260, bbData)
                        MMSTelematicFunction.saveSensorData(conn, deviceConfig.deviceIndex, sensorConfigs, sensorCalibrations, pointTime, tmEnergoPowerFullC, PortNumbers.MERCURY_POWER_FULL_C_264, bbData)

                        MMSTelematicFunction.saveSensorData(conn, deviceConfig.deviceIndex, sensorConfigs, sensorCalibrations, pointTime, tmEmisMassFlow, PortNumbers.EMIS_MASS_FLOW_270, bbData)
                        MMSTelematicFunction.saveSensorData(conn, deviceConfig.deviceIndex, sensorConfigs, sensorCalibrations, pointTime, tmEmisDensity, PortNumbers.EMIS_DENSITY_280, bbData)
                        MMSTelematicFunction.saveSensorData(conn, deviceConfig.deviceIndex, sensorConfigs, sensorCalibrations, pointTime, tmEmisTemperature, PortNumbers.EMIS_TEMPERATURE_290, bbData)
                        MMSTelematicFunction.saveSensorData(conn, deviceConfig.deviceIndex, sensorConfigs, sensorCalibrations, pointTime, tmEmisVolumeFlow, PortNumbers.EMIS_VOLUME_FLOW_300, bbData)
                        MMSTelematicFunction.saveSensorData(conn, deviceConfig.deviceIndex, sensorConfigs, sensorCalibrations, pointTime, tmEmisAccumulatedMass, PortNumbers.EMIS_ACCUMULATED_MASS_310, bbData)
                        MMSTelematicFunction.saveSensorData(conn, deviceConfig.deviceIndex, sensorConfigs, sensorCalibrations, pointTime, tmEmisAccumulatedVolume, PortNumbers.EMIS_ACCUMULATED_VOLUME_320, bbData)

                        //--- мощность по трём фазам: активная, реактивная, суммарная
                        MMSTelematicFunction.saveSensorData(conn, deviceConfig.deviceIndex, sensorConfigs, sensorCalibrations, pointTime, tmEnergoPowerActiveABC, PortNumbers.MERCURY_POWER_ACTIVE_ABC_330, bbData)
                        MMSTelematicFunction.saveSensorData(conn, deviceConfig.deviceIndex, sensorConfigs, sensorCalibrations, pointTime, tmEnergoPowerReactiveABC, PortNumbers.MERCURY_POWER_REACTIVE_ABC_340, bbData)
                        MMSTelematicFunction.saveSensorData(conn, deviceConfig.deviceIndex, sensorConfigs, sensorCalibrations, pointTime, tmEnergoPowerFullABC, PortNumbers.MERCURY_POWER_FULL_ABC_350, bbData)

                        //--- коэффициент трансформации по току и напряжению
                        MMSTelematicFunction.saveSensorData(conn, deviceConfig.deviceIndex, sensorConfigs, sensorCalibrations, pointTime, tmEnergoTransformKoefCurrent, PortNumbers.MERCURY_TRANSFORM_KOEF_CURRENT_360, bbData)
                        MMSTelematicFunction.saveSensorData(conn, deviceConfig.deviceIndex, sensorConfigs, sensorCalibrations, pointTime, tmEnergoTransformKoefVoltage, PortNumbers.MERCURY_TRANSFORM_KOEF_VOLTAGE_370, bbData)

                        MMSTelematicFunction.saveSensorData(conn, deviceConfig.deviceIndex, sensorConfigs, sensorCalibrations, pointTime, tmPMPLevel, PortNumbers.PMP_LEVEL_540, bbData)
                        MMSTelematicFunction.saveSensorData(conn, deviceConfig.deviceIndex, sensorConfigs, sensorCalibrations, pointTime, tmPMPTemperature, PortNumbers.PMP_TEMPERATURE_560, bbData)
                        MMSTelematicFunction.saveSensorData(conn, deviceConfig.deviceIndex, sensorConfigs, sensorCalibrations, pointTime, tmPMPVolume, PortNumbers.PMP_VOLUME_580, bbData)
                        MMSTelematicFunction.saveSensorData(conn, deviceConfig.deviceIndex, sensorConfigs, sensorCalibrations, pointTime, tmPMPMass, PortNumbers.PMP_MASS_600, bbData)
                        MMSTelematicFunction.saveSensorData(conn, deviceConfig.deviceIndex, sensorConfigs, sensorCalibrations, pointTime, tmPMPDensity, PortNumbers.PMP_DENSITY_620, bbData)

                        MMSTelematicFunction.addPoint(conn, deviceConfig, pointTime, bbData)

                        dataCount++

                        if (firstPointTime == 0) {
                            firstPointTime = pointTime
                        }
                        lastPointTime = pointTime

                        //--- массивы данных по датчикам очищаем независимо от записываемости точек
                        tmLLSLevel.clear()
                        tmGalileoVoltage.clear()
                        tmGalileoCount.clear()

                        tmEnergoCountActiveDirect.clear()
                        tmEnergoCountActiveReverse.clear()
                        tmEnergoCountReactiveDirect.clear()
                        tmEnergoCountReactiveReverse.clear()

                        tmEnergoVoltageA.clear()
                        tmEnergoVoltageB.clear()
                        tmEnergoVoltageC.clear()

                        tmEnergoCurrentA.clear()
                        tmEnergoCurrentB.clear()
                        tmEnergoCurrentC.clear()

                        tmEnergoPowerKoefA.clear()
                        tmEnergoPowerKoefB.clear()
                        tmEnergoPowerKoefC.clear()

                        tmEnergoPowerActiveA.clear()
                        tmEnergoPowerActiveB.clear()
                        tmEnergoPowerActiveC.clear()
                        tmEnergoPowerReactiveA.clear()
                        tmEnergoPowerReactiveB.clear()
                        tmEnergoPowerReactiveC.clear()
                        tmEnergoPowerFullA.clear()
                        tmEnergoPowerFullB.clear()
                        tmEnergoPowerFullC.clear()
                        tmEnergoPowerActiveABC.clear()
                        tmEnergoPowerReactiveABC.clear()
                        tmEnergoPowerFullABC.clear()

                        tmEnergoTransformKoefCurrent.clear()
                        tmEnergoTransformKoefVoltage.clear()

                        tmEmisDensity.clear()
                        tmEmisTemperature.clear()
                        tmEmisMassFlow.clear()
                        tmEmisVolumeFlow.clear()
                        tmEmisAccumulatedMass.clear()
                        tmEmisAccumulatedVolume.clear()

                        tmPMPLevel.clear()
                        tmPMPTemperature.clear()
                        tmPMPVolume.clear()
                        tmPMPMass.clear()
                        tmPMPDensity.clear()
                    }
                } else {
                    outDataParseError(serialNo, "DateTime is very old or in future")
                }
            } ?: run {
                outDataParseError(serialNo, "DateTime is null")
            }
        }

        ApplicationService.withConnection(entityManager) { conn ->
            status += " DataRead; Ok."

            //--- данные успешно переданы - теперь можно завершить транзакцию
            MMSTelematicFunction.writeSession(
                conn = conn,
                dirSessionLog = dirSessionLog,
                timeZone = timeZone,
                deviceConfig = deviceConfig,
                fwVersion = fwVersion,
                begTime = begTime,
                address = request.remoteAddr,
                status = status,
                errorText = "",
                dataCount = dataCount,
                firstPointTime = firstPointTime,
                lastPointTime = lastPointTime,
                isOk = true,
            )
        }
    }

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    private fun outDeviceParseError(timeZone: TimeZone, dirJournalLog: File, address: String, e: String) {
        CoreTelematicFunction.writeJournal(
            dirJournalLog = dirJournalLog,
            timeZone = timeZone,
            address = address,
            errorText = e,
        )
        AdvancedLogger.error("$METHOD_NAME: $e")
    }

    private fun outDataParseError(serialNo: String?, e: String) {
        AdvancedLogger.error("$METHOD_NAME: serialNo = $serialNo: $e")
    }
}