package foatto.server.service

import foatto.core.ActionType
import foatto.core.i18n.LocalizedMessages
import foatto.core.i18n.getLocalizedMessage
import foatto.core.model.AppAction
import foatto.core.model.request.FormActionData
import foatto.core.model.response.FormActionResponse
import foatto.core.model.response.ResponseCode
import foatto.core.model.response.form.FormDateTimeCellMode
import foatto.core.model.response.form.cells.FormBaseCell
import foatto.core.model.response.form.cells.FormBooleanCell
import foatto.core.model.response.form.cells.FormDateTimeCell
import foatto.core.model.response.form.cells.FormSimpleCell
import foatto.core.model.response.table.TableCaption
import foatto.core.model.response.table.TablePageButton
import foatto.core.model.response.table.TablePopup
import foatto.core.model.response.table.TableRow
import foatto.core.model.response.table.cell.TableBaseCell
import foatto.core.model.response.table.cell.TableCellAlign
import foatto.core.model.response.table.cell.TableCellBackColorType
import foatto.core.model.response.table.cell.TableSimpleCell
import foatto.core.util.getCurrentTimeInt
import foatto.core.util.getDateTimeDMYHMString
import foatto.core.util.getSplittedDouble
import foatto.core_mms.AppModuleMMS
import foatto.core_mms.i18n.LocalizedMMSMessages
import foatto.core_mms.i18n.getLocalizedMMSMessage
import foatto.server.checkFormAddPermission
import foatto.server.checkRowPermission
import foatto.server.entity.WorkShiftEntity
import foatto.server.getEnabledUserIds
import foatto.server.model.AppModuleConfig
import foatto.server.model.ServerUserConfig
import foatto.server.repository.ActionLogRepository
import foatto.server.repository.ObjectRepository
import foatto.server.repository.UserRepository
import foatto.server.repository.WorkShiftRepository
import foatto.server.util.getNextId
import org.springframework.data.domain.Page
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Service
class WorkShiftService(
    private val workShiftRepository: WorkShiftRepository,
    private val objectRepository: ObjectRepository,
    private val calcService: CalcService,
    private val userRepository: UserRepository,
    private val actionLogRepository: ActionLogRepository,
    private val fileStoreService: FileStoreService,
) : MMSService(
    userRepository = userRepository,
    actionLogRepository = actionLogRepository,
    fileStoreService = fileStoreService,
) {
    companion object {
        private const val FIELD_USER_ID = "userId"
        private const val FIELD_OWNER_FULL_NAME = "_ownerFullName"   // псевдополе для селектора

        private const val FIELD_OBJECT_ID = "obj.id"
        private const val FIELD_OBJECT_NAME = "obj.name"
        private const val FIELD_OBJECT_MODEL = "obj.model"

        private const val FIELD_BEG_TIME = "begTime"
        private const val FIELD_END_TIME = "endTime"

        private const val FIELD_IS_AUTO_WORK_SHIFT_ENABLED = "isAutoWorkShiftEnabled"
    }

    //--- на самом деле пока никому не нужно. Просто сделал, чтобы не потерять практики.
    //override fun isDateTimeIntervalPanelVisible(): Boolean = true

    override fun getTableColumnCaptions(action: AppAction, userConfig: ServerUserConfig): List<TableCaption> {
        val alColumnInfo = mutableListOf<Pair<String?, String>>()

        alColumnInfo += null to "" // by begTime group
        alColumnInfo += null to "" // userId
        alColumnInfo += null to getLocalizedMMSMessage(LocalizedMMSMessages.THE_END, userConfig.lang)
        alColumnInfo += null to getLocalizedMMSMessage(LocalizedMMSMessages.OBJECT, userConfig.lang)
        alColumnInfo += null to getLocalizedMMSMessage(LocalizedMMSMessages.EQUIPMENT, userConfig.lang)
        alColumnInfo += null to getLocalizedMMSMessage(LocalizedMMSMessages.OPERATION, userConfig.lang)
        alColumnInfo += null to getLocalizedMMSMessage(LocalizedMMSMessages.FUEL_METERS, userConfig.lang)
        alColumnInfo += null to getLocalizedMMSMessage(LocalizedMMSMessages.FUEL_CONSUMPTION, userConfig.lang)
        alColumnInfo += null to getLocalizedMMSMessage(LocalizedMMSMessages.ELECTRICITY_METERS, userConfig.lang)
        alColumnInfo += null to getLocalizedMMSMessage(LocalizedMMSMessages.ELECTRICITY, userConfig.lang)
        alColumnInfo += null to getLocalizedMMSMessage(LocalizedMMSMessages.FUEL_LEVEL, userConfig.lang)
        alColumnInfo += null to getLocalizedMMSMessage(LocalizedMMSMessages.START_OF_PERIOD, userConfig.lang)
        alColumnInfo += null to getLocalizedMMSMessage(LocalizedMMSMessages.END_OF_PERIOD, userConfig.lang)
        alColumnInfo += null to getLocalizedMMSMessage(LocalizedMMSMessages.TEMPERATURE, userConfig.lang)
        alColumnInfo += null to getLocalizedMMSMessage(LocalizedMMSMessages.START_OF_PERIOD, userConfig.lang)
        alColumnInfo += null to getLocalizedMMSMessage(LocalizedMMSMessages.END_OF_PERIOD, userConfig.lang)
        alColumnInfo += null to getLocalizedMMSMessage(LocalizedMMSMessages.DENSITY, userConfig.lang)
        alColumnInfo += null to getLocalizedMMSMessage(LocalizedMMSMessages.START_OF_PERIOD, userConfig.lang)
        alColumnInfo += null to getLocalizedMMSMessage(LocalizedMMSMessages.END_OF_PERIOD, userConfig.lang)

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
        var currentRowNo: Int? = null
        var row = 0
        var dataRow = 0

        val pageRequest = getTableSortedPageRequest(
            action,
            Sort.Order(Sort.Direction.DESC, FIELD_BEG_TIME),
            Sort.Order(Sort.Direction.DESC, FIELD_END_TIME),
            Sort.Order(Sort.Direction.ASC, FIELD_OBJECT_NAME),
        )
        val findText = action.findText?.trim() ?: ""

        val parentUserIds = getParentUserIds(action)

        val parentObjectId = getParentObjectId(action)
        val parentObjectEntity = parentObjectId?.let {
            objectRepository.findByIdOrNull(parentObjectId)
        }

        val enabledUserIds = getEnabledUserIds(
            module = action.module,
            actionType = action.type,
            relatedUserIds = userConfig.relatedUserIds,
            roles = userConfig.roles
        )

        val page: Page<WorkShiftEntity> = workShiftRepository.findByParentUserIdAndObjAndUserIdInAndFilter(
            parentUserIds = parentUserIds,
            obj = parentObjectEntity,
            userIds = enabledUserIds,
            findText = findText,
            timeOffset = userConfig.timeOffset,
            begDateTime = action.begDateTimeValue ?: -1,
            endDateTime = action.endDateTimeValue ?: -1,
            pageRequest = pageRequest,
        )

        fillTablePageButtons(userConfig, action, page.totalPages, pageButtons)
        val workShiftEntities = page.content

        val groupColSpan = getTableColumnCaptions(action, userConfig).size
        var prevGroupName: String? = null
//var worksTime = 0
//var usingsTime = 0
//var energosTime = 0
//var liquidLevelsTime = 0
//var temperaturesTime = 0
//var densitiesTime = 0
        for (workShiftEntity in workShiftEntities) {
            val objectEntity = workShiftEntity.obj ?: continue

            val begTime = workShiftEntity.begTime ?: continue
            val endTime = workShiftEntity.endTime ?: continue

//var bt = getCurrentTimeInt()
            val works = calcService.calcWorks(objectEntity, begTime, endTime).sortedBy { wcd -> wcd.sensorEntity.descr ?: "-" }
//worksTime += getCurrentTimeInt() - bt
//bt = getCurrentTimeInt()
            val usings = calcService.calcUsings(objectEntity, begTime, endTime).sortedBy { ccd -> ccd.sensorEntity.descr ?: "-" }
//usingsTime += getCurrentTimeInt() - bt
//bt = getCurrentTimeInt()
            val energos = calcService.calcEnergos(objectEntity, begTime, endTime).sortedBy { ccd -> ccd.sensorEntity.descr ?: "-" }
//energosTime += getCurrentTimeInt() - bt
//bt = getCurrentTimeInt()
            val liquidLevels = calcService.calcLiquidLevels(objectEntity, begTime, endTime).sortedBy { acd -> acd.sensorEntity.descr ?: "-" }
//liquidLevelsTime += getCurrentTimeInt() - bt
//bt = getCurrentTimeInt()
            val temperatures = calcService.calcTemperatures(objectEntity, begTime, endTime).sortedBy { acd -> acd.sensorEntity.descr ?: "-" }
//temperaturesTime += getCurrentTimeInt() - bt
//bt = getCurrentTimeInt()
            val densities = calcService.calcDensities(objectEntity, begTime, endTime).sortedBy { acd -> acd.sensorEntity.descr ?: "-" }
//densitiesTime += getCurrentTimeInt() - bt

            var col = 0

            val rowOwnerShortName = userConfig.shortNames[workShiftEntity.userId]
            val rowOwnerFullName = userConfig.fullNames[workShiftEntity.userId]

            val isFormEnabled = checkRowPermission(
                module = action.module,
                actionType = ActionType.MODULE_FORM,
                rowUserRelation = userConfig.relatedUserIds[workShiftEntity.userId],
                userRoles = userConfig.roles
            )

            val groupName = getDateTimeDMYHMString(timeOffset = userConfig.timeOffset, seconds = begTime)
            if (prevGroupName != groupName) {
                tableCells += TableSimpleCell(
                    row = row,
                    col = col,
                    colSpan = groupColSpan,
                    dataRow = dataRow,
                    name = groupName,
                    backColorType = TableCellBackColorType.GROUP_0,
                    isBoldText = true,
                )
                prevGroupName = groupName
                tableRows += TableRow()
                row++
            }
            tableCells += TableSimpleCell(row = row, col = col++, dataRow = dataRow, name = "", backColorType = TableCellBackColorType.GROUP_0)

            tableCells += getTableUserNameCell(
                row = row,
                col = col++,
                dataRow = dataRow,
                userConfig = userConfig,
                rowUserId = workShiftEntity.userId,
                rowOwnerShortName = rowOwnerShortName,
                rowOwnerFullName = rowOwnerFullName
            )

            tableCells += TableSimpleCell(
                row = row,
                col = col++,
                dataRow = dataRow,
                name = getDateTimeDMYHMString(timeOffset = userConfig.timeOffset, seconds = endTime)
            )

            tableCells += TableSimpleCell(
                row = row,
                col = col++,
                dataRow = dataRow,
                name = (objectEntity.name ?: "-") +
                        "\n${objectEntity.model ?: "-"}" +
                        "\n${objectEntity.department?.name ?: "-"}" +
                        "\n${objectEntity.group?.name ?: "-"}"
            )

            tableCells += TableSimpleCell(
                row = row,
                col = col++,
                dataRow = dataRow,
                name = works.joinToString("\n") { wcd -> wcd.sensorEntity.descr?.let { descr -> "$descr = " } ?: "-" },
                align = TableCellAlign.RIGHT,
            )
            tableCells += TableSimpleCell(
                row = row,
                col = col++,
                dataRow = dataRow,
                name = works.joinToString("\n") { wcd -> "${getSplittedDouble(wcd.onTime / 3600.0)} ${getLocalizedMMSMessage(LocalizedMMSMessages.UNIT_HOUR, userConfig.lang)}" },
                align = TableCellAlign.LEFT,
            )

            tableCells += TableSimpleCell(
                row = row,
                col = col++,
                dataRow = dataRow,
                name = usings.joinToString("\n") { ccd -> ccd.sensorEntity.descr?.let { descr -> "$descr = " } ?: "-" },
                align = TableCellAlign.RIGHT,
            )
            tableCells += TableSimpleCell(
                row = row,
                col = col++,
                dataRow = dataRow,
                name = usings.joinToString("\n") { ccd -> getSplittedDouble(ccd.value) + (ccd.sensorEntity.dim ?: "") },
                align = TableCellAlign.LEFT,
            )

            tableCells += TableSimpleCell(
                row = row,
                col = col++,
                dataRow = dataRow,
                name = energos.joinToString("\n") { ccd -> ccd.sensorEntity.descr?.let { descr -> "$descr = " } ?: "-" },
                align = TableCellAlign.RIGHT,
            )
            tableCells += TableSimpleCell(
                row = row,
                col = col++,
                dataRow = dataRow,
                name = energos.joinToString("\n") { ccd -> getSplittedDouble(ccd.value) + (ccd.sensorEntity.dim ?: "") },
                align = TableCellAlign.LEFT,
            )

            tableCells += TableSimpleCell(
                row = row,
                col = col++,
                dataRow = dataRow,
                name = liquidLevels.joinToString("\n") { acd -> acd.sensorEntity.descr?.let { descr -> "$descr = " } ?: "-" },
                align = TableCellAlign.RIGHT,
            )
            tableCells += TableSimpleCell(
                row = row,
                col = col++,
                dataRow = dataRow,
                name = liquidLevels.joinToString("\n") { acd -> acd.begValue?.let { begValue -> getSplittedDouble(begValue) + (acd.sensorEntity.dim ?: "") } ?: "-" },
                align = TableCellAlign.CENTER,
            )
            tableCells += TableSimpleCell(
                row = row,
                col = col++,
                dataRow = dataRow,
                name = liquidLevels.joinToString("\n") { acd -> acd.endValue?.let { endValue -> getSplittedDouble(endValue) + (acd.sensorEntity.dim ?: "") } ?: "-" },
                align = TableCellAlign.LEFT,
            )

            tableCells += TableSimpleCell(
                row = row,
                col = col++,
                dataRow = dataRow,
                name = temperatures.joinToString("\n") { acd -> acd.sensorEntity.descr?.let { descr -> "$descr = " } ?: "-" },
                align = TableCellAlign.RIGHT,
            )
            tableCells += TableSimpleCell(
                row = row,
                col = col++,
                dataRow = dataRow,
                name = temperatures.joinToString("\n") { acd -> acd.begValue?.let { begValue -> getSplittedDouble(begValue) + (acd.sensorEntity.dim ?: "") } ?: "-" },
                align = TableCellAlign.CENTER,
            )
            tableCells += TableSimpleCell(
                row = row,
                col = col++,
                dataRow = dataRow,
                name = temperatures.joinToString("\n") { acd -> acd.endValue?.let { endValue -> getSplittedDouble(endValue) + (acd.sensorEntity.dim ?: "") } ?: "-" },
                align = TableCellAlign.LEFT,
            )

            tableCells += TableSimpleCell(
                row = row,
                col = col++,
                dataRow = dataRow,
                name = densities.joinToString("\n") { acd -> acd.sensorEntity.descr?.let { descr -> "$descr = " } ?: "-" },
                align = TableCellAlign.RIGHT,
            )
            tableCells += TableSimpleCell(
                row = row,
                col = col++,
                dataRow = dataRow,
                name = densities.joinToString("\n") { acd -> acd.begValue?.let { begValue -> getSplittedDouble(begValue) + (acd.sensorEntity.dim ?: "") } ?: "-" },
                align = TableCellAlign.CENTER,
            )
            tableCells += TableSimpleCell(
                row = row,
                col = col++,
                dataRow = dataRow,
                name = densities.joinToString("\n") { acd -> acd.endValue?.let { endValue -> getSplittedDouble(endValue) + (acd.sensorEntity.dim ?: "") } ?: "-" },
                align = TableCellAlign.LEFT,
            )

            val formOpenAction = action.copy(
                type = ActionType.MODULE_FORM,
                id = workShiftEntity.id,
            )

            val popupDatas = getTablePopupDatas(
                userConfig = userConfig,
                objectId = objectEntity.id,
                begTime = begTime,
                endTime = endTime,
                isFormEnabled = isFormEnabled,
                formOpenAction = formOpenAction,
            )

            tableRows += TableRow(
                rowAction = if (isFormEnabled) {
                    formOpenAction
                } else {
                    null
                },
                isRowUrlInNewTab = false,
                tablePopups = popupDatas,
            )

            if (workShiftEntity.id == action.id) {
                currentRowNo = row
            }

            //if (userConfig.isWideScreen) {
                row++
            //}
            dataRow++
        }
//println("works = $worksTime")
//println("usings = $usingsTime")
//println("energos = $energosTime")
//println("liquidLevels = $liquidLevelsTime")
//println("temperatures = $temperaturesTime")
//println("densities = $densitiesTime")
        /*
        works = 12 / 4 = 3
        usings = 8 / 4 = 2
        energos = 2 / 0 = ?
        liquidLevels = 16 / 6 = 2.7
        temperatures = 6 / 2 = 3
        densities = 5 / 2 = 2.5
        */
        return currentRowNo
    }

    private fun getTablePopupDatas(
        userConfig: ServerUserConfig,
        objectId: Int,
        begTime: Int,
        endTime: Int,
        isFormEnabled: Boolean,
        formOpenAction: AppAction,
    ): List<TablePopup> {
        val alPopupData = mutableListOf<TablePopup>()

        if (isFormEnabled) {
            alPopupData += TablePopup(
                action = formOpenAction,
                text = getLocalizedMessage(LocalizedMessages.OPEN, userConfig.lang),
                inNewTab = false,
            )
        }

        getTableReportPopupData(userConfig, AppModuleMMS.REPORT_SUMMARY, AppModuleMMS.ALL_OBJECT, objectId, begTime, endTime, alPopupData)
        getTableReportPopupData(userConfig, AppModuleMMS.REPORT_WORK_SHIFT, AppModuleMMS.ALL_OBJECT, objectId, begTime, endTime, alPopupData)

        getTableDashboardPopupData(userConfig, AppModuleMMS.OBJECT_SCHEME_DASHBOARD, AppModuleMMS.ALL_OBJECT, objectId, alPopupData)
//        getTableDashboardPopupData(userConfig, AppModuleMMS.OBJECT_CHART_DASHBOARD, AppModuleMMS.ALL_OBJECT, objectId, alPopupData)

        getTableChartPopupData(userConfig, AppModuleMMS.CHART_LIQUID_LEVEL, AppModuleMMS.ALL_OBJECT, objectId, begTime, endTime, alPopupData)
        getTableChartPopupData(userConfig, AppModuleMMS.CHART_ENERGO_SENSORS, AppModuleMMS.ALL_OBJECT, objectId, begTime, endTime, alPopupData)
        getTableChartPopupData(userConfig, AppModuleMMS.CHART_ALL_SENSORS, AppModuleMMS.ALL_OBJECT, objectId, begTime, endTime, alPopupData)

        //--- maps for shift works for static objects is not exists
        //getTableMapPopupData(userConfig, AppModuleMMS.MAP_TRACE, AppModuleMMS.OBJECT, objectId, begTime, endTime, alPopupData)

        getTableTablePopupData(userConfig, AppModuleMMS.SENSOR, AppModuleMMS.ALL_OBJECT, objectId, alPopupData)
        getTableTablePopupData(userConfig, AppModuleMMS.OBJECT_DATA, AppModuleMMS.ALL_OBJECT, objectId, alPopupData)
        getTableTablePopupData(userConfig, AppModuleMMS.DEVICE, AppModuleMMS.ALL_OBJECT, objectId, alPopupData)

        return alPopupData
    }

    override fun getFormCells(action: AppAction, userConfig: ServerUserConfig, moduleConfig: AppModuleConfig, addEnabled: Boolean, editEnabled: Boolean): List<FormBaseCell> {
        val formCells = mutableListOf<FormBaseCell>()

        val id = action.id

        val changeEnabled = id?.let { editEnabled } ?: addEnabled

        val workShiftEntity = id?.let {
            workShiftRepository.findByIdOrNull(id) ?: return emptyList()
        }

        val parentObjectId = getParentObjectId(action) ?: workShiftEntity?.obj?.id
        val parentObjectEntity = parentObjectId?.let {
            objectRepository.findByIdOrNull(parentObjectId)
        }

        //--- логика именно такая - если есть объект - берём его userId, даже если он null
        val userId = workShiftEntity?.let {
            workShiftEntity.userId
        } ?: parentObjectEntity?.let {
            parentObjectEntity.userId
        } ?: getParentUserIds(action)?.singleOrNull() ?: userConfig.id

        fillFormUserCells(
            fieldUserId = FIELD_USER_ID,
            fieldOwnerFullName = FIELD_OWNER_FULL_NAME,
            userId = userId,
            userConfig = userConfig,
            changeEnabled = changeEnabled,
            formCells = formCells,
        )

        formCells += FormSimpleCell(
            name = FIELD_OBJECT_ID,
            caption = "",
            isEditable = false,
            value = parentObjectEntity?.id?.toString() ?: "",
        )
        formCells += FormSimpleCell(
            name = FIELD_OBJECT_NAME,
            caption = getLocalizedMMSMessage(LocalizedMMSMessages.NAME, userConfig.lang),
            isEditable = false,
            value = parentObjectEntity?.name ?: "",
            selectorAction = AppAction(
                type = ActionType.MODULE_TABLE,
                module = AppModuleMMS.ALL_OBJECT,
                isSelectorMode = true,
                selectorPath = mapOf(
                    AbstractObjectService.FIELD_ID to FIELD_OBJECT_ID,
                    AbstractObjectService.FIELD_NAME to FIELD_OBJECT_NAME,
                    AbstractObjectService.FIELD_MODEL to FIELD_OBJECT_MODEL,
                ),
                selectorClear = mapOf(
                    FIELD_OBJECT_ID to "",
                    FIELD_OBJECT_NAME to "",
                    FIELD_OBJECT_MODEL to "",
                ),
            ),
        )
        formCells += FormSimpleCell(
            name = FIELD_OBJECT_MODEL,
            caption = getLocalizedMMSMessage(LocalizedMMSMessages.MODEL, userConfig.lang),
            isEditable = changeEnabled,
            value = parentObjectEntity?.model ?: "",
        )

        formCells += FormDateTimeCell(
            name = FIELD_BEG_TIME,
            caption = "Дата/время начала смены",
            isEditable = changeEnabled,
            mode = FormDateTimeCellMode.DMYHMS,
            value = workShiftEntity?.begTime ?: getCurrentTimeInt(),
        )
        formCells += FormDateTimeCell(
            name = FIELD_END_TIME,
            caption = "Дата/время окончания смены",
            isEditable = changeEnabled,
            mode = FormDateTimeCellMode.DMYHMS,
            value = workShiftEntity?.endTime ?: getCurrentTimeInt(),
        )

        formCells += FormBooleanCell(
            name = FIELD_IS_AUTO_WORK_SHIFT_ENABLED,
            caption = "Автосоздание рабочих смен",
            isEditable = changeEnabled,
            value = parentObjectEntity?.isAutoWorkShiftEnabled ?: true,
        )

        return formCells
    }

    override fun getFormActionPermissions(
        action: AppAction,
        userConfig: ServerUserConfig,
        moduleConfig: AppModuleConfig,
    ): Triple<Boolean, Boolean, Boolean> {
        val id = action.id

        val addEnabled = checkFormAddPermission(moduleConfig, userConfig.roles)

        val workShiftEntity = id?.let {
            workShiftRepository.findByIdOrNull(id) ?: return Triple(addEnabled, false, false)
        }

        val editEnabled = checkRowPermission(action.module, ActionType.FORM_EDIT, userConfig.relatedUserIds[workShiftEntity?.userId], userConfig.roles)
        val deleteEnabled = checkRowPermission(action.module, ActionType.FORM_DELETE, userConfig.relatedUserIds[workShiftEntity?.userId], userConfig.roles)

        return Triple(addEnabled, editEnabled, deleteEnabled)
    }


    override fun formActionSave(action: AppAction, userConfig: ServerUserConfig, moduleConfig: AppModuleConfig, formActionData: Map<String, FormActionData>): FormActionResponse {
        val id = action.id

        val parentObjectId = formActionData[FIELD_OBJECT_ID]?.stringValue?.toIntOrNull() ?: return FormActionResponse(responseCode = ResponseCode.ERROR, errors = mapOf(FIELD_OBJECT_NAME to getLocalizedMMSMessage(LocalizedMMSMessages.NO_OBJECT_SELECTED, userConfig.lang)))
        val parentObjectEntity = objectRepository.findByIdOrNull(parentObjectId)

        val begTime = formActionData[FIELD_BEG_TIME]?.dateTimeValue ?: return FormActionResponse(responseCode = ResponseCode.ERROR, errors = mapOf(FIELD_BEG_TIME to "Не задано время начала смены"))
        val endTime = formActionData[FIELD_END_TIME]?.dateTimeValue ?: return FormActionResponse(responseCode = ResponseCode.ERROR, errors = mapOf(FIELD_END_TIME to "Не задано время окончания смены"))

        if (endTime == begTime) {
            return FormActionResponse(responseCode = ResponseCode.ERROR, errors = mapOf(FIELD_END_TIME to "Время окончания смены совпадает с временем начала смены"))
        } else if (endTime < begTime) {
            return FormActionResponse(responseCode = ResponseCode.ERROR, errors = mapOf(FIELD_END_TIME to "Время окончания смены раньше, чем время начала смены"))
        }

        val recordId = id ?: getNextId { nextId -> workShiftRepository.existsById(nextId) }
        val workShiftEntity = WorkShiftEntity(
            id = recordId,
            userId = formActionData[FIELD_USER_ID]?.stringValue?.toIntOrNull(),
            obj = parentObjectEntity,
            begTime = begTime,
            endTime = endTime,
            begTimeFact = begTime,
            endTimeFact = endTime,
        )
        workShiftRepository.saveAndFlush(workShiftEntity)

        parentObjectEntity?.let {
            parentObjectEntity.isAutoWorkShiftEnabled = formActionData[FIELD_IS_AUTO_WORK_SHIFT_ENABLED]?.booleanValue ?: false
            objectRepository.saveAndFlush(parentObjectEntity)
        }

        return FormActionResponse(
            responseCode = ResponseCode.OK,
            nextAction = action.prevAction?.copy(id = recordId),
        )
    }

    override fun formActionDelete(userId: Int, id: Int): FormActionResponse {
        workShiftRepository.deleteById(id)

        return FormActionResponse(responseCode = ResponseCode.OK)
    }

}