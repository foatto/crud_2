package foatto.server.service

import foatto.core.model.AppAction
import foatto.core.model.request.FormActionData
import foatto.core.model.response.FormActionResponse
import foatto.core.model.response.ResponseCode
import foatto.core.model.response.form.cells.FormBaseCell
import foatto.core.model.response.table.TableCaption
import foatto.core.model.response.table.TablePageButton
import foatto.core.model.response.table.TableRow
import foatto.core.model.response.table.cell.TableBaseCell
import foatto.core.model.response.table.cell.TableSimpleCell
import foatto.core.util.getDateTimeDMYHMSString
import foatto.core.util.getTimeZone
import foatto.core_mms.AppModuleMMS
import foatto.core_mms.i18n.LocalizedMMSMessages
import foatto.core_mms.i18n.getLocalizedMMSMessage
import foatto.server.model.AppModuleConfig
import foatto.server.model.ServerUserConfig
import foatto.server.model.sensor.SensorConfig
import foatto.server.repository.ActionLogRepository
import foatto.server.repository.SensorRepository
import foatto.server.repository.UserRepository
import jakarta.persistence.EntityManager
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class SensorDataService(
    private val entityManager: EntityManager,
    private val sensorRepository: SensorRepository,
    private val userRepository: UserRepository,
    private val actionLogRepository: ActionLogRepository,
    private val fileStoreService: FileStoreService,
) : MMSService(
    userRepository = userRepository,
    actionLogRepository = actionLogRepository,
    fileStoreService = fileStoreService,
) {

    companion object {
        private const val PAGE_SIZE_IN_SEC_WORK = 10_800 // 21_600 // 43_200 // 86_400 - записи работы оборудования гораздо реже
        private const val PAGE_SIZE_IN_SEC_OTHER = 3600  // 10_800 // 21_600 // 43_200 // 86_400
    }

    override fun isDateTimeIntervalPanelVisible(): Boolean = true

    override fun getTableColumnCaptions(action: AppAction, userConfig: ServerUserConfig): List<TableCaption> {
        val alColumnInfo = mutableListOf<Pair<String?, String>>()

        alColumnInfo += null to getLocalizedMMSMessage(LocalizedMMSMessages.TIME_UTC, userConfig.lang)
        alColumnInfo += null to getLocalizedMMSMessage(LocalizedMMSMessages.TIME_LOCAL, userConfig.lang)
        alColumnInfo += null to getLocalizedMMSMessage(LocalizedMMSMessages.TIME_2_UTC, userConfig.lang)
        alColumnInfo += null to getLocalizedMMSMessage(LocalizedMMSMessages.TIME_2_LOCAL, userConfig.lang)
        alColumnInfo += null to getLocalizedMMSMessage(LocalizedMMSMessages.TYPE, userConfig.lang)
        alColumnInfo += null to getLocalizedMMSMessage(LocalizedMMSMessages.VALUE, userConfig.lang)
        alColumnInfo += null to getLocalizedMMSMessage(LocalizedMMSMessages.VALUE_2, userConfig.lang)
        alColumnInfo += null to getLocalizedMMSMessage(LocalizedMMSMessages.VALUE_3, userConfig.lang)
        alColumnInfo += null to getLocalizedMMSMessage(LocalizedMMSMessages.VALUE_4, userConfig.lang)

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

        val parentSensorId = if (action.parentModule == AppModuleMMS.SENSOR) {
            action.parentId ?: return null
        } else {
            return null
        }
        val parentSensorEntity = sensorRepository.findByIdOrNull(parentSensorId) ?: return null

        SensorService.checkAndCreateSensorTables(entityManager, parentSensorEntity.id)

        val zoneUTC = getTimeZone(0)
        val zoneUser = getTimeZone(userConfig.timeOffset)

        val filterBegDateTime = action.begDateTimeValue
        val filterEndDateTime = action.endDateTimeValue

        val whereClause = filterBegDateTime?.let {
            filterEndDateTime?.let {
                """
                    WHERE ontime_0 >= $filterBegDateTime
                      AND ontime_0 <= $filterEndDateTime 
                """
            } ?: run {
                """
                    WHERE ontime_0 >= $filterBegDateTime 
                """
            }
        } ?: run {
            filterEndDateTime?.let {
                """
                    WHERE ontime_0 <= $filterEndDateTime 
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
                SELECT MIN(ontime_0), MAX(ontime_0)
                FROM MMS_agg_$parentSensorId
                $whereClause
            """
        ) { rs ->
            if (rs.next()) {
                firstTimeUTC = rs.getInt(1)
                lastTimeUTC = rs.getInt(2)
            }
        }

        val currentPageSizeInSec = if (parentSensorEntity.sensorType == SensorConfig.SENSOR_WORK) {
            PAGE_SIZE_IN_SEC_WORK
        } else {
            PAGE_SIZE_IN_SEC_OTHER
        }
        val currentTimedPageNo = lastTimeUTC / currentPageSizeInSec - action.pageNo
        val begPageTime = currentTimedPageNo * currentPageSizeInSec
        val endPageTime = begPageTime + currentPageSizeInSec

        queryNativeSql(
            entityManager,
            """
                SELECT ontime_0 , ontime_1 , type_0 , value_0 , value_1 , value_2 , value_3
                FROM MMS_agg_$parentSensorId
                WHERE ontime_0 BETWEEN $begPageTime AND $endPageTime
                ORDER BY ontime_0 DESC
            """
        ) { rs ->
            while (rs.next()) {
                var pos = 1

                val ontime0 = rs.getInt(pos++)
                val ontime1 = rs.getInt(pos++)
                val type0 = rs.getInt(pos++)
                val value0 = rs.getDouble(pos++)
                val value1 = rs.getDouble(pos++)
                val value2 = rs.getDouble(pos++)
                val value3 = rs.getDouble(pos++)

                var col = 0
                tableCells += TableSimpleCell(
                    row = row,
                    col = col++,
                    dataRow = row,
                    minWidth = 100,
                    name = getDateTimeDMYHMSString(zoneUTC, ontime0)
                )
                tableCells += TableSimpleCell(
                    row = row,
                    col = col++,
                    dataRow = row,
                    minWidth = 100,
                    name = getDateTimeDMYHMSString(zoneUser, ontime0)
                )
                tableCells += TableSimpleCell(
                    row = row,
                    col = col++,
                    dataRow = row,
                    minWidth = 100,
                    name = getDateTimeDMYHMSString(zoneUTC, ontime1)
                )
                tableCells += TableSimpleCell(
                    row = row,
                    col = col++,
                    dataRow = row,
                    minWidth = 100,
                    name = getDateTimeDMYHMSString(zoneUser, ontime1)
                )
                tableCells += TableSimpleCell(row = row, col = col++, dataRow = row, minWidth = 100, name = type0.toString())
                tableCells += TableSimpleCell(row = row, col = col++, dataRow = row, minWidth = 100, name = value0.toString())
                tableCells += TableSimpleCell(row = row, col = col++, dataRow = row, minWidth = 100, name = value1.toString())
                tableCells += TableSimpleCell(row = row, col = col++, dataRow = row, minWidth = 100, name = value2.toString())
                tableCells += TableSimpleCell(row = row, col = col++, dataRow = row, minWidth = 100, name = value3.toString())

                tableRows += TableRow(
                    rowAction = null,
                    isRowUrlInNewTab = false,
                    tablePopups = emptyList(),
                )
                row++
            }
        }

        getTableTimedPageButtons(
            pageSizeInSec = currentPageSizeInSec,
            action = action,
            zoneUser = zoneUser,
            firstTimeUTC = firstTimeUTC,
            lastTimeUTC = lastTimeUTC,
            currentTimedPageNo = currentTimedPageNo,
            pageButtons = pageButtons,
        )

        return null
    }

    override fun getFormCells(action: AppAction, userConfig: ServerUserConfig, moduleConfig: AppModuleConfig, addEnabled: Boolean, editEnabled: Boolean): List<FormBaseCell> = emptyList()
    override fun getFormActionPermissions(action: AppAction, userConfig: ServerUserConfig, moduleConfig: AppModuleConfig): Triple<Boolean, Boolean, Boolean> = Triple(false, false, false)
    override fun formActionSave(action: AppAction, userConfig: ServerUserConfig, moduleConfig: AppModuleConfig, formActionData: Map<String, FormActionData>): FormActionResponse = FormActionResponse(ResponseCode.ERROR)
    override fun formActionDelete(userId: Int, id: Int): FormActionResponse = FormActionResponse(ResponseCode.ERROR)
}