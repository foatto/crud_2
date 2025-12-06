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
import foatto.server.model.AppModuleConfig
import foatto.server.model.ServerUserConfig
import foatto.server.repository.ActionLogRepository
import foatto.server.repository.SensorRepository
import jakarta.persistence.EntityManager
import kotlinx.datetime.TimeZone
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class SensorDataService(
    private val entityManager: EntityManager,
    private val sensorRepository: SensorRepository,
    private val fileStoreService: FileStoreService,
    private val actionLogRepository: ActionLogRepository,
) : MMSService(
    fileStoreService = fileStoreService,
    actionLogRepository = actionLogRepository,
) {

    companion object {
        private const val PAGE_SIZE_IN_SEC = 3600  // 10_800 // 21_600 // 43_200 // 86_400
    }

    override fun isDateTimeIntervalPanelVisible(): Boolean = true

    override fun getTableColumnCaptions(action: AppAction, userConfig: ServerUserConfig): List<TableCaption> {
        val alColumnInfo = mutableListOf<Pair<String?, String>>()

        alColumnInfo += null to "Время (UTC)"
        alColumnInfo += null to "Время (местное)"
        alColumnInfo += null to "Время 2 (UTC)"
        alColumnInfo += null to "Время 2 (местное)"
        alColumnInfo += null to "Тип"
        alColumnInfo += null to "Значение"
        alColumnInfo += null to "Значение 2"
        alColumnInfo += null to "Значение 3"
        alColumnInfo += null to "Значение 4"

        return getTableColumnCaptionActions(
            action = action,
            alColumnInfo = alColumnInfo,
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

        val currentTimedPageNo = lastTimeUTC / PAGE_SIZE_IN_SEC - action.pageNo
        val begPageTime = currentTimedPageNo * PAGE_SIZE_IN_SEC
        val endPageTime = begPageTime + PAGE_SIZE_IN_SEC

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
                    name = getDateTimeDMYHMSString(zoneUTC, ontime0)
                )
                tableCells += TableSimpleCell(
                    row = row,
                    col = col++,
                    dataRow = row,
                    name = getDateTimeDMYHMSString(zoneUser, ontime0)
                )
                tableCells += TableSimpleCell(
                    row = row,
                    col = col++,
                    dataRow = row,
                    name = getDateTimeDMYHMSString(zoneUTC, ontime1)
                )
                tableCells += TableSimpleCell(
                    row = row,
                    col = col++,
                    dataRow = row,
                    name = getDateTimeDMYHMSString(zoneUser, ontime1)
                )
                tableCells += TableSimpleCell(row = row, col = col++, dataRow = row, name = type0.toString())
                tableCells += TableSimpleCell(row = row, col = col++, dataRow = row, name = value0.toString())
                tableCells += TableSimpleCell(row = row, col = col++, dataRow = row, name = value1.toString())
                tableCells += TableSimpleCell(row = row, col = col++, dataRow = row, name = value2.toString())
                tableCells += TableSimpleCell(row = row, col = col++, dataRow = row, name = value3.toString())

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

    override fun getFormCells(action: AppAction, userConfig: ServerUserConfig, moduleConfig: AppModuleConfig, addEnabled: Boolean, editEnabled: Boolean): List<FormBaseCell> = emptyList()
    override fun getFormActionPermissions(action: AppAction, userConfig: ServerUserConfig, moduleConfig: AppModuleConfig): Triple<Boolean, Boolean, Boolean> = Triple(false, false, false)
    override fun formActionSave(action: AppAction, userConfig: ServerUserConfig, moduleConfig: AppModuleConfig, formActionData: Map<String, FormActionData>): FormActionResponse = FormActionResponse(ResponseCode.ERROR)
    override fun formActionDelete(userId: Int, id: Int): FormActionResponse = FormActionResponse(ResponseCode.ERROR)
}