package foatto.server.service

import foatto.core.model.AppAction
import foatto.core.model.model.xy.GeoData
import foatto.core.model.request.FormActionData
import foatto.core.model.response.FormActionResponse
import foatto.core.model.response.ResponseCode
import foatto.core.model.response.form.cells.FormBaseCell
import foatto.core.model.response.table.TableCaption
import foatto.core.model.response.table.TablePageButton
import foatto.core.model.response.table.TableRow
import foatto.core.model.response.table.cell.TableBaseCell
import foatto.core.model.response.table.cell.TableSimpleCell
import foatto.core.util.getCurrentTimeInt
import foatto.core.util.getDateTimeDMYHMSString
import foatto.core.util.getTimeZone
import foatto.core_mms.AppModuleMMS
import foatto.server.entity.ObjectEntity
import foatto.server.model.AppModuleConfig
import foatto.server.model.ServerUserConfig
import foatto.server.model.sensor.SensorConfig
import foatto.server.repository.ActionLogRepository
import foatto.server.repository.ObjectRepository
import foatto.server.repository.SensorRepository
import foatto.server.util.AdvancedByteBuffer
import foatto.server.util.byteToHex
import jakarta.persistence.EntityManager
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.util.TreeMap

@Service
class ObjectDataService(
    private val entityManager: EntityManager,
    private val objectRepository: ObjectRepository,
    private val sensorRepository: SensorRepository,
    private val fileStoreService: FileStoreService,
    private val actionLogRepository: ActionLogRepository,
) : MMSService(
    fileStoreService = fileStoreService,
    actionLogRepository = actionLogRepository,
) {

    companion object {
        private const val PAGE_SIZE_IN_SEC = 3600    // 10_800 // 21_600 // 43_200 // 86_400
    }

    override fun isDateTimeIntervalPanelVisible(): Boolean = true

    override fun getTableColumnCaptions(action: AppAction, userConfig: ServerUserConfig): List<TableCaption> {
        val alColumnInfo = mutableListOf<Pair<String?, String>>()

        val emptyResult = getTableColumnCaptionActions(action, alColumnInfo)

        val parentObjectId = if (action.parentModule == AppModuleMMS.OBJECT) {
            action.parentId ?: return emptyResult
        } else {
            return emptyResult
        }
        val parentObjectEntity = objectRepository.findByIdOrNull(parentObjectId) ?: return emptyResult

        //--- Передавать два getCurrentTimeInt() неправильно, но определить показываемый период сейчас невозможно.
        //--- Рассчитываем на то, что после отключения старой версии Пульсара этот класс/фукционал не понадобится
        val sensorConfigs = getSensorConfigs(parentObjectEntity, getCurrentTimeInt(), getCurrentTimeInt())

        alColumnInfo += null to "Время (UTC)"
        alColumnInfo += null to "Время (местное)"
        sensorConfigs.forEach { (portNum, _) ->
            alColumnInfo += null to "$portNum"
        }
        alColumnInfo += null to "Прочие данные"

        return getTableColumnCaptionActions(
            action = action,
            columnInfos = alColumnInfo,
        )
    }

    override fun fillTableGridData(
        action: AppAction,
        userConfig: ServerUserConfig,
        moduleConfig: AppModuleConfig,
        tableCells: MutableList<TableBaseCell>,
        tableRows: MutableList<TableRow>,
        pageButtons: MutableList<TablePageButton>,
    ): Int? {
        var row = 0

        val parentObjectId = if (action.parentModule == AppModuleMMS.OBJECT) {
            action.parentId ?: return null
        } else {
            return null
        }
        val parentObjectEntity = objectRepository.findByIdOrNull(parentObjectId) ?: return null

        val zoneUTC = getTimeZone(0)
        val zoneUser = getTimeZone(userConfig.timeOffset)

        val filterBegDateTime = action.begDateTimeValue
        val filterEndDateTime = action.endDateTimeValue

        val whereClause = filterBegDateTime?.let {
            filterEndDateTime?.let {
                """
                    WHERE ontime >= $filterBegDateTime
                      AND ontime <= $filterEndDateTime 
                """
            } ?: run {
                """
                    WHERE ontime >= $filterBegDateTime 
                """
            }
        } ?: run {
            filterEndDateTime?.let {
                """
                    WHERE ontime <= $filterEndDateTime 
                """
            } ?: run {
                ""
            }
        }

        var firstTimeUTC = 0
        var lastTimeUTC = 0
        queryNativeSql(
            entityManager,
            """
                SELECT MIN(ontime), MAX(ontime)
                FROM MMS_data_$parentObjectId
                $whereClause
            """
        ) { rs ->
            if (rs.next()) {
                firstTimeUTC = rs.getInt(1)
                lastTimeUTC = rs.getInt(2)
            }
        }

        val currentTimedPageNo = lastTimeUTC / PAGE_SIZE_IN_SEC - action.pageNo
        val begPageTime = currentTimedPageNo * PAGE_SIZE_IN_SEC
        val endPageTime = begPageTime + PAGE_SIZE_IN_SEC

        val sensorConfigs = getSensorConfigs(parentObjectEntity, begPageTime, endPageTime)

        queryNativeSql(
            entityManager,
            """
                SELECT ontime , sensor_data
                FROM MMS_data_$parentObjectId
                WHERE ontime BETWEEN $begPageTime AND $endPageTime
                ORDER BY ontime DESC
            """
        ) { rs ->
            while (rs.next()) {
                var col = 0

                val ontime = rs.getInt(1)
                val bb = AdvancedByteBuffer(rs.getBytes(2))

                tableCells += TableSimpleCell(
                    row = row,
                    col = col++,
                    dataRow = row,
                    name = getDateTimeDMYHMSString(zoneUTC, ontime)
                )
                tableCells += TableSimpleCell(
                    row = row,
                    col = col++,
                    dataRow = row,
                    name = getDateTimeDMYHMSString(zoneUser, ontime)
                )

                val valuesByPortNum = mutableMapOf<Int, String>()
                var otherData = ""
                while (bb.hasRemaining()) {
                    val (portNum, dataSize) = getSensorPortNumAndDataSize(bb)

                    val sensorType = sensorConfigs[portNum]
                    //--- по каждому номеру порта - составляем визуальное представление значения
                    val sensorValue = getSensorString(sensorConfigs[portNum], dataSize, bb)

                    //--- выводим только определённые порты
                    sensorType?.let {
                        valuesByPortNum[portNum] = sensorValue
                    } ?: run {
                        otherData += "${(if (otherData.isEmpty()) "" else "  ")}$portNum=$sensorValue"
                    }
                }
                if (otherData.isEmpty()) {
                    otherData = "-"
                }

                sensorConfigs.forEach { (portNum, _) ->
                    tableCells += TableSimpleCell(row = row, col = col++, dataRow = row, name = valuesByPortNum[portNum] ?: "-")
                }
                tableCells += TableSimpleCell(row = row, col = col++, dataRow = row, name = otherData)

                tableRows += TableRow(
                    rowAction = null,
                    isRowUrlInNewTab = false,
                    tablePopups = emptyList(),
                )
                row++
            }
        }

        getTableTimedPageButtons(
            pageSizeInSec = PAGE_SIZE_IN_SEC,
            action = action,
            zoneUser = zoneUser,
            firstTimeUTC = firstTimeUTC,
            lastTimeUTC = lastTimeUTC,
            currentTimedPageNo = currentTimedPageNo,
            pageButtons = pageButtons,
        )

        return null
    }

    private fun getSensorConfigs(parentObjectEntity: ObjectEntity, begTime: Int, endTime: Int): TreeMap<Int, Int> {
        val sensorConfigs = TreeMap<Int, Int>()
        sensorRepository.findByObjAndPeriod(parentObjectEntity, begTime, endTime).forEach { sensorEntity ->
            sensorEntity.portNum?.let { portNum ->
                sensorEntity.sensorType?.let { sensorType ->
                    sensorConfigs[portNum] = sensorType
                }
            }
        }
        return sensorConfigs
    }

    override fun getFormCells(action: AppAction, userConfig: ServerUserConfig, moduleConfig: AppModuleConfig, addEnabled: Boolean, editEnabled: Boolean): List<FormBaseCell> = emptyList()
    override fun getFormActionPermissions(action: AppAction, userConfig: ServerUserConfig, moduleConfig: AppModuleConfig): Triple<Boolean, Boolean, Boolean> = Triple(false, false, false)
    override fun formActionSave(action: AppAction, userConfig: ServerUserConfig, moduleConfig: AppModuleConfig, formActionData: Map<String, FormActionData>): FormActionResponse = FormActionResponse(ResponseCode.ERROR)
    override fun formActionDelete(userId: Int, id: Int): FormActionResponse = FormActionResponse(ResponseCode.ERROR)

    private fun getSensorPortNumAndDataSize(bb: AdvancedByteBuffer): Pair<Int, Int> {
        val portNum = bb.getShort().toInt() and 0xFFFF
        val dataSize = bb.getShort() + 1
        return Pair(portNum, dataSize)
    }

    //--- defining the string form of sensor data
    private fun getSensorString(aSensorType: Int?, dataSize: Int, bb: AdvancedByteBuffer): String {
        var sensorType = aSensorType
        val sensorValue: CharSequence

        if (sensorType == null) {
            sensorType = 0
        }

        when (sensorType) {
            SensorConfig.SENSOR_GEO -> {
                val gd = GeoData(bb.getInt(), bb.getInt(), bb.getShort().toInt(), bb.getInt())
                sensorValue = "x = ${gd.wgs.x}\ny = ${gd.wgs.y}\nскорость = ${gd.speed}\nпробег = ${gd.distance}"
            }

            else -> {
                when (dataSize) {
                    1 -> sensorValue = (bb.getByte().toInt() and 0xFF).toString()
                    2 -> sensorValue = (bb.getShort().toInt() and 0xFFFF).toString()
                    3 -> sensorValue = bb.getInt3().toString()
                    4 -> sensorValue = bb.getInt().toString()
                    8 -> sensorValue = bb.getDouble().toString()
                    else -> {
                        sensorValue = if (dataSize < 0) {
                            bb.getShortString()
                        } else {
                            val sb = StringBuilder()
                            for (i in 0 until dataSize) {
                                byteToHex(bb.getByte(), sb, false)
                            }
                            sb
                        }
                    }
                }
            }
        }

        return sensorValue.toString()
    }

}

