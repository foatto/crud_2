package foatto.server.service

import foatto.core.ActionType
import foatto.core.model.AppAction
import foatto.core.model.request.FormActionData
import foatto.core.model.response.FormActionResponse
import foatto.core.model.response.HeaderData
import foatto.core.model.response.ResponseCode
import foatto.core.model.response.TitleData
import foatto.core.model.response.form.FormDateTimeCellMode
import foatto.core.model.response.form.cells.FormBaseCell
import foatto.core.model.response.form.cells.FormDateTimeCell
import foatto.core.model.response.form.cells.FormFileCell
import foatto.core.model.response.form.cells.FormSimpleCell
import foatto.core.model.response.table.TableCaption
import foatto.core.model.response.table.TablePageButton
import foatto.core.model.response.table.TablePopup
import foatto.core.model.response.table.TableRow
import foatto.core.model.response.table.cell.TableBaseCell
import foatto.core.model.response.table.cell.TableButtonCell
import foatto.core.model.response.table.cell.TableSimpleCell
import foatto.core.util.getCurrentTimeInt
import foatto.core.util.getDateTimeDMYHMSString
import foatto.core.util.getTimeZone
import foatto.core_mms.AppModuleMMS
import foatto.server.UserRelationEnum
import foatto.server.checkFormAddPermission
import foatto.server.checkRowPermission
import foatto.server.entity.DeviceEntity
import foatto.server.entity.DeviceManageEntity
import foatto.server.model.AppModuleConfig
import foatto.server.model.ServerUserConfig
import foatto.server.repository.DeviceManageRepository
import foatto.server.repository.DeviceRepository
import foatto.server.util.getNextId
import org.springframework.data.domain.Page
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Service
class DeviceManageService(
    private val deviceManageRepository: DeviceManageRepository,
    private val deviceRepository: DeviceRepository,
    private val fileStoreService: FileStoreService,
) : ApplicationService(
    fileStoreService = fileStoreService,
) {
    companion object {
        private const val FIELD_USER_ID = "userId"

        private const val FIELD_DESCR = "descr"
        private const val FIELD_COMMAND = "command"
        private const val FIELD_FILE = "fileId"

        private const val FIELD_CREATE_TIME = "createTime"
        private const val FIELD_EDIT_TIME = "editTime"
        private const val FIELD_SEND_TIME = "sendTime"

        private const val FIELD_OWNER_FULL_NAME = "_ownerFullName"   // псевдополе для селектора
    }

    override fun getTableHeaderData(
        action: AppAction,
        userConfig: ServerUserConfig,
        moduleConfig: AppModuleConfig
    ): HeaderData {
        val rows = mutableListOf<Pair<String, String>>()

        getParentDeviceEntity(action)?.let { parentDeviceEntity ->
            rows += "Серийный номер контроллера" to (parentDeviceEntity.serialNo ?: "-")
            rows += "Наименование контроллера" to (parentDeviceEntity.name ?: "-")
        }

        return HeaderData(
            titles = listOf(
                TitleData(
                    action = null,
                    text = moduleConfig.caption,
                    isBold = true,
                )
            ),
            rows = rows,
        )
    }

    override fun getTableColumnCaptions(action: AppAction, userConfig: ServerUserConfig): List<TableCaption> {
        val alColumnInfo = mutableListOf<Pair<String?, String>>()

        alColumnInfo += null to "" // userId
        alColumnInfo += null to "Примечание"
        alColumnInfo += null to "Команда"
        alColumnInfo += null to "Файл"
        alColumnInfo += null to "Время создания"
        alColumnInfo += null to "Время редактирования"
        alColumnInfo += null to "Время отправки"

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

        val pageRequest = getTableSortedPageRequest(action, Sort.Order(Sort.Direction.DESC, FIELD_CREATE_TIME))
        val findText = action.findText?.trim() ?: ""

        val parentDeviceEntity = getParentDeviceEntity(action) ?: return null

        val page: Page<DeviceManageEntity> = deviceManageRepository.findByDeviceAndFilter(parentDeviceEntity, findText, pageRequest)
        fillTablePageButtons(action, page.totalPages, pageButtons)
        val deviceManageEntities = page.content

        for (deviceManageEntity in deviceManageEntities) {
            var col = 0

            val rowOwnerShortName = userConfig.shortNames[deviceManageEntity.userId]
            val rowOwnerFullName = userConfig.fullNames[deviceManageEntity.userId]

            val isFormEnabled = checkRowPermission(
                module = action.module,
                actionType = ActionType.MODULE_FORM,
                rowUserRelation = UserRelationEnum.NOBODY,
                userRoles = userConfig.roles
            )

            tableCells += getTableUserNameCell(
                row = row,
                col = col++,
                userId = userConfig.id,
                rowUserId = deviceManageEntity.userId,
                rowOwnerShortName = rowOwnerShortName,
                rowOwnerFullName = rowOwnerFullName
            )
            tableCells += TableSimpleCell(row = row, col = col++, dataRow = row, name = deviceManageEntity.descr ?: "-", minWidth = 200)
            tableCells += TableSimpleCell(row = row, col = col++, dataRow = row, name = deviceManageEntity.command ?: "-", minWidth = 200)
            tableCells += TableButtonCell(
                row = row,
                col = col++,
                dataRow = row,
                values = getTableFileButtonCellData(deviceManageEntity.fileId),
            )

            tableCells += TableSimpleCell(
                row = row,
                col = col++,
                dataRow = row,
                name = deviceManageEntity.createTime?.let { time -> getDateTimeDMYHMSString(zoneLocal, time) } ?: "-",
                minWidth = 200,
            )
            tableCells += TableSimpleCell(
                row = row,
                col = col++,
                dataRow = row,
                name = deviceManageEntity.editTime?.let { time -> getDateTimeDMYHMSString(zoneLocal, time) } ?: "-",
                minWidth = 200,
            )
            tableCells += TableSimpleCell(
                row = row,
                col = col++,
                dataRow = row,
                name = deviceManageEntity.sendTime?.let { time -> getDateTimeDMYHMSString(zoneLocal, time) } ?: "-",
                minWidth = 200,
            )

            val formOpenAction = AppAction(
                type = ActionType.MODULE_FORM,
                module = action.module,
                id = deviceManageEntity.id,
                parentModule = action.parentModule,
                parentId = action.parentId
            )

            val popupDatas = mutableListOf<TablePopup>()

            if (isFormEnabled) {
                popupDatas += TablePopup(
                    action = formOpenAction,
                    text = "Открыть",
                    inNewTab = false,
                )
            }

            tableRows += TableRow(
                rowAction = if (isFormEnabled) {
                    formOpenAction
                } else {
                    null
                },
                isRowUrlInNewTab = false,
                tablePopups = popupDatas,
            )

            if (deviceManageEntity.id == action.id) {
                currentRowNo = row
            }

            row++
        }
        return currentRowNo
    }

    override fun getFormHeaderData(
        action: AppAction,
        userConfig: ServerUserConfig,
        moduleConfig: AppModuleConfig
    ): HeaderData {
        val rows = mutableListOf<Pair<String, String>>()

        getParentDeviceEntity(action)?.let { parentDeviceEntity ->
            rows += "Серийный номер контроллера" to (parentDeviceEntity.serialNo ?: "-")
            rows += "Наименование контроллера" to (parentDeviceEntity.name ?: "-")
        }

        return HeaderData(
            titles = listOf(
                TitleData(
                    action = null,
                    text = moduleConfig.caption,
                    isBold = true,
                )
            ),
            rows = rows,
        )
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

        val deviceManageEntity = id?.let {
            deviceManageRepository.findByIdOrNull(id) ?: return emptyList()
        }

        val changeEnabled = id?.let { editEnabled && deviceManageEntity?.sendTime == null } ?: addEnabled

        val userId = deviceManageEntity?.let {
            deviceManageEntity.userId
        } ?: userConfig.id

        fillFormUserCells(
            fieldUserId = FIELD_USER_ID,
            fieldOwnerFullName = FIELD_OWNER_FULL_NAME,
            userId = userId,
            userConfig = userConfig,
            changeEnabled = false,
            formCells = formCells,
        )
        formCells += FormSimpleCell(
            name = FIELD_DESCR,
            caption = "Примечание",
            isEditable = changeEnabled,
            value = deviceManageEntity?.descr ?: "",
        )
        formCells += FormSimpleCell(
            name = FIELD_COMMAND,
            caption = "Команда",
            isEditable = changeEnabled,
            value = deviceManageEntity?.command ?: "",
            rows = 5,
        )
        formCells += FormFileCell(
            name = FIELD_FILE,
            caption = "Файл для отправки",
            isEditable = false, //!!! changeEnabled,
            fileId = deviceManageEntity?.fileId,
            files = getFormFileCellData(deviceManageEntity?.fileId),
            maxFileCount = 1,
        )

        formCells += FormDateTimeCell(
            name = FIELD_CREATE_TIME,
            caption = "Время создания",
            isEditable = false,
            mode = FormDateTimeCellMode.DMYHMS,
            value = deviceManageEntity?.createTime,
        )
        formCells += FormDateTimeCell(
            name = FIELD_EDIT_TIME,
            caption = "Время редактирования",
            isEditable = false,
            mode = FormDateTimeCellMode.DMYHMS,
            value = deviceManageEntity?.editTime,
        )
        formCells += FormDateTimeCell(
            name = FIELD_SEND_TIME,
            caption = "Время отправки",
            isEditable = false,
            mode = FormDateTimeCellMode.DMYHMS,
            value = deviceManageEntity?.sendTime,
        )

        return formCells
    }

    override fun getFormActionPermissions(
        action: AppAction,
        userConfig: ServerUserConfig,
        moduleConfig: AppModuleConfig,
    ): Triple<Boolean, Boolean, Boolean> {
        val deviceManageEntity = action.id?.let { id ->
            deviceManageRepository.findByIdOrNull(id)
        }

        val addEnabled = checkFormAddPermission(moduleConfig, userConfig.roles)
        val editEnabled = checkRowPermission(action.module, ActionType.FORM_EDIT, UserRelationEnum.NOBODY, userConfig.roles) && deviceManageEntity?.sendTime == null
        val deleteEnabled = checkRowPermission(action.module, ActionType.FORM_DELETE, UserRelationEnum.NOBODY, userConfig.roles) && deviceManageEntity?.sendTime == null

        return Triple(addEnabled, editEnabled, deleteEnabled)
    }

    override fun formActionSave(
        action: AppAction,
        userConfig: ServerUserConfig,
        moduleConfig: AppModuleConfig,
        formActionData: Map<String, FormActionData>
    ): FormActionResponse {
        val id = action.id

        val recordUserId = formActionData[FIELD_USER_ID]?.stringValue?.toIntOrNull() ?: 0

        val command = formActionData[FIELD_COMMAND]?.stringValue?.trim() ?: return FormActionResponse(responseCode = ResponseCode.ERROR, errors = mapOf(FIELD_COMMAND to "Не введёна команда"))
        if (command.isEmpty()) {
            return FormActionResponse(responseCode = ResponseCode.ERROR, errors = mapOf(FIELD_COMMAND to "Не введёна команда"))
        }

        val oldDeviceManageEntity = id?.let {
            deviceManageRepository.findByIdOrNull(id)
        }

        val deviceManageEntity = DeviceManageEntity(
            id = id ?: getNextId { nextId -> deviceManageRepository.existsById(nextId) },
            userId = recordUserId,
            device = getParentDeviceEntity(action),
            descr = formActionData[FIELD_DESCR]?.stringValue?.trim(),
            command = command,
            fileId = formActionData[FIELD_FILE]?.let { fad ->
                formActionSaveFiles(fad)
            },
            createTime = oldDeviceManageEntity?.createTime ?: getCurrentTimeInt(),
            editTime = getCurrentTimeInt(),
            sendTime = oldDeviceManageEntity?.sendTime,
        )
        deviceManageRepository.saveAndFlush(deviceManageEntity)

        return FormActionResponse(responseCode = ResponseCode.OK)
    }

    override fun formActionDelete(userId: Int, id: Int): FormActionResponse {
        deviceManageRepository.deleteById(id)

        return FormActionResponse(responseCode = ResponseCode.OK)
    }

    private fun getParentDeviceEntity(action: AppAction): DeviceEntity? =
        if (action.parentModule == AppModuleMMS.DEVICE) {
            action.parentId?.let { parentId ->
                deviceRepository.findByIdOrNull(parentId)
            }
        } else {
            null
        }

}
