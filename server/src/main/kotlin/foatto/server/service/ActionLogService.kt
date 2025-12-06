package foatto.server.service

import foatto.core.ActionType
import foatto.core.AppModule
import foatto.core.model.AppAction
import foatto.core.model.request.FormActionData
import foatto.core.model.response.FormActionResponse
import foatto.core.model.response.ResponseCode
import foatto.core.model.response.form.FormCellVisibility
import foatto.core.model.response.form.FormDateTimeCellMode
import foatto.core.model.response.form.cells.FormBaseCell
import foatto.core.model.response.form.cells.FormBooleanCell
import foatto.core.model.response.form.cells.FormDateTimeCell
import foatto.core.model.response.form.cells.FormFileCell
import foatto.core.model.response.form.cells.FormSimpleCell
import foatto.core.model.response.table.TableCaption
import foatto.core.model.response.table.TablePageButton
import foatto.core.model.response.table.TableRow
import foatto.core.model.response.table.cell.TableBaseCell
import foatto.core.model.response.table.cell.TableSimpleCell
import foatto.core.util.getCurrentTimeInt
import foatto.core.util.getDateTimeDMYHMSString
import foatto.core.util.getTimeZone
import foatto.server.entity.ActionLogEntity
import foatto.server.model.AppModuleConfig
import foatto.server.model.ServerUserConfig
import foatto.server.repository.ActionLogRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class ActionLogService(
    private val actionLogRepository: ActionLogRepository,
    private val fileStoreService: FileStoreService,
) : ApplicationService(
    fileStoreService = fileStoreService,
    actionLogRepository = actionLogRepository,
) {

    companion object {
        private const val FIELD_USER_ID = "userId"
        private const val FIELD_ON_TIME = "onTime"
        private const val FIELD_TYPE = "type"
        private const val FIELD_MODULE = "module"
        private const val FIELD_RECORD_ID = "recordId"
        private const val FIELD_PARENT_MODULE = "parentModule"
        private const val FIELD_PARENT_RECORD_ID = "parentId"
        private const val FIELD_ACTION = "action"

        private const val FIELD_OWNER_FULL_NAME = "_ownerFullName"   // псевдополе для селектора
    }

    override fun isDateTimeIntervalPanelVisible(): Boolean = true

    override fun getTableColumnCaptions(action: AppAction, userConfig: ServerUserConfig): List<TableCaption> {
        val alColumnInfo = mutableListOf<Pair<String?, String>>()

        alColumnInfo += null to "" // userId
        alColumnInfo += FIELD_ON_TIME to "Time"
        alColumnInfo += null to "Type"
        alColumnInfo += null to "Module"
        alColumnInfo += null to "Id"
        alColumnInfo += null to "Parent Module"
        alColumnInfo += null to "Parent Id"
        alColumnInfo += null to "Action"

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

        val zoneLocal = getTimeZone(userConfig.timeOffset)

        var currentRowNo: Int? = null
        var row = 0

        val pageRequest = getTableSortedPageRequest(action, Sort.Order(Sort.Direction.DESC, FIELD_ON_TIME))
        val findText = action.findText?.trim() ?: ""

        val parentUserId = if (action.parentModule == AppModule.USER) {
            action.parentId
        } else {
            null
        }

        val page: Page<ActionLogEntity> = parentUserId?.let {
            actionLogRepository.findByParentUserIdAndFilter(
                parentUserId = parentUserId,
                findText = findText,
                timeOffset = userConfig.timeOffset,
                begDateTime = action.begDateTimeValue ?: -1,
                endDateTime = action.endDateTimeValue ?: -1,
                pageRequest = pageRequest,
            )
        } ?: run {
            actionLogRepository.findByFilter(
                findText = findText,
                timeOffset = userConfig.timeOffset,
                begDateTime = action.begDateTimeValue ?: -1,
                endDateTime = action.endDateTimeValue ?: -1,
                pageRequest = pageRequest,
            )
        }

        fillTablePageButtons(action, page.totalPages, pageButtons)
        val actionLogEntities = page.content

        for (actionLogEntity in actionLogEntities) {
            var col = 0

            val rowOwnerShortName = userConfig.shortNames[actionLogEntity.userId]
            val rowOwnerFullName = userConfig.fullNames[actionLogEntity.userId]

            tableCells += getTableUserNameCell(
                row = row,
                col = col++,
                userId = userConfig.id,
                rowUserId = actionLogEntity.userId,
                rowOwnerShortName = rowOwnerShortName,
                rowOwnerFullName = rowOwnerFullName
            )
            tableCells += TableSimpleCell(
                row = row,
                col = col++,
                dataRow = row,
                name = actionLogEntity.onTime?.let { onTime -> getDateTimeDMYHMSString(zoneLocal, onTime) } ?: "-",
            )
            tableCells += TableSimpleCell(row = row, col = col++, dataRow = row, minWidth = 100, name = actionLogEntity.type ?: "-")
            tableCells += TableSimpleCell(row = row, col = col++, dataRow = row, minWidth = 100, name = actionLogEntity.module ?: "-")
            tableCells += TableSimpleCell(row = row, col = col++, dataRow = row, minWidth = 100, name = actionLogEntity.recordId?.toString() ?: "-")
            tableCells += TableSimpleCell(row = row, col = col++, dataRow = row, minWidth = 100, name = actionLogEntity.parentModule ?: "-")
            tableCells += TableSimpleCell(row = row, col = col++, dataRow = row, minWidth = 100, name = actionLogEntity.parentId?.toString() ?: "-")
            tableCells += TableSimpleCell(row = row, col = col++, dataRow = row, minWidth = 100, name = actionLogEntity.action ?: "-")

            tableRows += TableRow(
                rowAction = action.copy(
                    type = ActionType.MODULE_FORM,
                    id = actionLogEntity.id,
                ),
            )

            if (actionLogEntity.id == action.id) {
                currentRowNo = row
            }

            row++
        }
        return currentRowNo
    }

    override fun getFormCells(
        action: AppAction,
        userConfig: ServerUserConfig,
        moduleConfig: AppModuleConfig,
        addEnabled: Boolean,
        editEnabled: Boolean,
    ): List<FormBaseCell> {
        val formCells = mutableListOf<FormBaseCell>()

        val id = action.id

        val actionLogEntity = id?.let {
            actionLogRepository.findByIdOrNull(id) ?: return emptyList()
        }

        val userId = actionLogEntity?.let {
            actionLogEntity.userId
        } ?: userConfig.id

        fillFormUserCells(
            fieldUserId = FIELD_USER_ID,
            fieldOwnerFullName = FIELD_OWNER_FULL_NAME,
            userId = userId,
            userConfig = userConfig,
            changeEnabled = false,
            formCells = formCells,
        )
        formCells += FormDateTimeCell(
            name = FIELD_ON_TIME,
            caption = "Time",
            isEditable = false,
            mode = FormDateTimeCellMode.DMYHMS,
            value = if (id == 0) {
                getCurrentTimeInt()
            } else {
                actionLogEntity?.onTime
            },
        )
        formCells += FormSimpleCell(
            name = FIELD_TYPE,
            caption = "Type",
            isEditable = false,
            value = actionLogEntity?.type ?: "",
        )
        formCells += FormSimpleCell(
            name = FIELD_MODULE,
            caption = "Module",
            isEditable = false,
            value = actionLogEntity?.module ?: "",
        )

        formCells += FormSimpleCell(
            name = FIELD_RECORD_ID,
            caption = "Id",
            isEditable = false,
            value = actionLogEntity?.recordId?.toString() ?: "",
        )
        formCells += FormSimpleCell(
            name = FIELD_PARENT_MODULE,
            caption = "Parent Module",
            isEditable = false,
            value = actionLogEntity?.parentModule ?: "",
        )
        formCells += FormSimpleCell(
            name = FIELD_PARENT_RECORD_ID,
            caption = "Parent Id",
            isEditable = false,
            value = actionLogEntity?.parentId?.toString() ?: "",
        )
        formCells += FormSimpleCell(
            name = FIELD_ACTION,
            caption = "Action",
            isEditable = false,
            value = actionLogEntity?.action ?: "",
            rows = 20,  // по мере необходимости/удобства постепенно увеличивать на 10
        )

        return formCells
    }

    override fun getFormActionPermissions(action: AppAction, userConfig: ServerUserConfig, moduleConfig: AppModuleConfig): Triple<Boolean, Boolean, Boolean> = Triple(false, false, false)
    override fun formActionSave(action: AppAction, userConfig: ServerUserConfig, moduleConfig: AppModuleConfig, formActionData: Map<String, FormActionData>): FormActionResponse = FormActionResponse(ResponseCode.ERROR)
    override fun formActionDelete(userId: Int, id: Int): FormActionResponse = FormActionResponse(ResponseCode.ERROR)
}