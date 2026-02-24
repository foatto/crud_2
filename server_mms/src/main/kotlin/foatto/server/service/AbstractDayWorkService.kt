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
import foatto.core.model.response.form.cells.FormDateTimeCell
import foatto.core.model.response.form.cells.FormSimpleCell
import foatto.core.model.response.table.TablePopup
import foatto.core.util.getCurrentTimeInt
import foatto.core.util.getDateTimeYMDHMSInts
import foatto.core.util.getTimeZone
import foatto.core_mms.AppModuleMMS
import foatto.core_mms.i18n.LocalizedMMSMessages
import foatto.core_mms.i18n.getLocalizedMMSMessage
import foatto.server.checkFormAddPermission
import foatto.server.checkRowPermission
import foatto.server.entity.DateEntity
import foatto.server.entity.DayWorkEntity
import foatto.server.model.AppModuleConfig
import foatto.server.model.ServerUserConfig
import foatto.server.repository.ActionLogRepository
import foatto.server.repository.DayWorkRepository
import foatto.server.repository.ObjectRepository
import foatto.server.util.getNextId
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toInstant
import org.springframework.data.repository.findByIdOrNull
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
abstract class AbstractDayWorkService(
    private val dayWorkRepository: DayWorkRepository,
    private val objectRepository: ObjectRepository,
    private val calcService: CalcService,
    private val fileStoreService: FileStoreService,
    private val actionLogRepository: ActionLogRepository,
) : MMSService(
    fileStoreService = fileStoreService,
    actionLogRepository = actionLogRepository,
) {

    companion object {
        protected const val FIELD_USER_ID = "userId"
        protected const val FIELD_OWNER_FULL_NAME = "_ownerFullName"   // псевдополе для селектора

        protected const val FIELD_OBJECT_ID = "obj.id"
        protected const val FIELD_OBJECT_NAME = "obj.name"
        protected const val FIELD_OBJECT_MODEL = "obj.model"

        protected const val FIELD_DAY = "day"

        protected const val FIELD_DAY_YE = "day.ye"
        protected const val FIELD_DAY_MO = "day.mo"
        protected const val FIELD_DAY_DA = "day.da"
    }

    //--- на самом деле пока никому не нужно. Просто сделал, чтобы не потерять практики.
    //override fun isDateTimeIntervalPanelVisible(): Boolean = true

    protected fun getTablePopupDatas(
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
        getTableReportPopupData(userConfig, AppModuleMMS.REPORT_DAY_WORK, AppModuleMMS.ALL_OBJECT, objectId, begTime, endTime, alPopupData)

        getTableDashboardPopupData(userConfig, AppModuleMMS.OBJECT_SCHEME_DASHBOARD, AppModuleMMS.ALL_OBJECT, objectId, alPopupData)
        getTableDashboardPopupData(userConfig, AppModuleMMS.OBJECT_CHART_DASHBOARD, AppModuleMMS.ALL_OBJECT, objectId, alPopupData)

//        getTableChartPopupData(userConfig, AppModuleMMS.CHART_LIQUID_LEVEL, AppModuleMMS.OBJECT, id, begTime, endTime, alPopupData)

        getTableMapPopupData(userConfig, AppModuleMMS.MAP_TRACE, AppModuleMMS.ALL_OBJECT, objectId, begTime, endTime, alPopupData)

        getTableTablePopupData(userConfig, AppModuleMMS.SENSOR, AppModuleMMS.ALL_OBJECT, objectId, alPopupData)
        getTableTablePopupData(userConfig, AppModuleMMS.OBJECT_DATA, AppModuleMMS.ALL_OBJECT, objectId, alPopupData)
        getTableTablePopupData(userConfig, AppModuleMMS.DEVICE, AppModuleMMS.ALL_OBJECT, objectId, alPopupData)

        return alPopupData
    }

    override fun getFormCells(
        action: AppAction,
        userConfig: ServerUserConfig,
        moduleConfig: AppModuleConfig,
        addEnabled: Boolean,
        editEnabled: Boolean
    ): List<FormBaseCell> {
        val formCells = mutableListOf<FormBaseCell>()

        val id = action.id

        val changeEnabled = id?.let { editEnabled } ?: addEnabled

        val dayWorkEntity = id?.let {
            dayWorkRepository.findByIdOrNull(id) ?: return emptyList()
        }

        val parentObjectId = getParentObjectId(action) ?: dayWorkEntity?.obj?.id
        val parentObjectEntity = parentObjectId?.let {
            objectRepository.findByIdOrNull(parentObjectId)
        }

        //--- логика именно такая - если есть объект - берём его userId, даже если он null
        val userId = dayWorkEntity?.let {
            dayWorkEntity.userId
        } ?: parentObjectEntity?.let {
            parentObjectEntity.userId
        } ?: userConfig.id

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
            name = FIELD_DAY,
            caption = getLocalizedMMSMessage(LocalizedMMSMessages.DATE, userConfig.lang),
            isEditable = changeEnabled,
            mode = FormDateTimeCellMode.DMY,
            value = dayWorkEntity?.day?.let { dt ->
                LocalDateTime(dt.ye ?: 2000, dt.mo ?: 1, dt.da ?: 1, 0, 0, 0).toInstant(getTimeZone(userConfig.timeOffset)).epochSeconds.toInt()
            } ?: getCurrentTimeInt(),
        )

        return formCells
    }

    override fun getFormActionPermissions(action: AppAction, userConfig: ServerUserConfig, moduleConfig: AppModuleConfig): Triple<Boolean, Boolean, Boolean> {
        val id = action.id

        val addEnabled = checkFormAddPermission(moduleConfig, userConfig.roles)

        val dayWorkEntity = id?.let {
            dayWorkRepository.findByIdOrNull(id) ?: return Triple(addEnabled, false, false)
        }

        val editEnabled = checkRowPermission(action.module, ActionType.FORM_EDIT, userConfig.relatedUserIds[dayWorkEntity?.userId], userConfig.roles)
        val deleteEnabled = checkRowPermission(action.module, ActionType.FORM_DELETE, userConfig.relatedUserIds[dayWorkEntity?.userId], userConfig.roles)

        return Triple(addEnabled, editEnabled, deleteEnabled)
    }

    override fun formActionSave(action: AppAction, userConfig: ServerUserConfig, moduleConfig: AppModuleConfig, formActionData: Map<String, FormActionData>): FormActionResponse {
        val id = action.id

        val parentObjectId = formActionData[FIELD_OBJECT_ID]?.stringValue?.toIntOrNull() ?: return FormActionResponse(responseCode = ResponseCode.ERROR, errors = mapOf(FIELD_OBJECT_NAME to getLocalizedMMSMessage(LocalizedMMSMessages.NO_OBJECT_SELECTED, userConfig.lang)))
        val parentObjectEntity = objectRepository.findByIdOrNull(parentObjectId)

        val day = getDateTimeYMDHMSInts(getTimeZone(userConfig.timeOffset), formActionData[FIELD_DAY]?.dateTimeValue ?: getCurrentTimeInt())

        val recordId = id ?: getNextId { nextId -> dayWorkRepository.existsById(nextId) }
        val dayWorkEntity = DayWorkEntity(
            id = recordId,
            userId = formActionData[FIELD_USER_ID]?.stringValue?.toIntOrNull(),
            obj = parentObjectEntity,
            day = DateEntity(
                ye = day[0],
                mo = day[1],
                da = day[2],
            ),
        )
        dayWorkRepository.saveAndFlush(dayWorkEntity)

        return FormActionResponse(
            responseCode = ResponseCode.OK,
            nextAction = action.prevAction?.copy(id = recordId),
        )
    }

    override fun formActionDelete(userId: Int, id: Int): FormActionResponse {
        dayWorkRepository.deleteById(id)

        return FormActionResponse(responseCode = ResponseCode.OK)
    }

}
/*
        columnRun = ColumnString(modelTableName, "_run", getLocalizedMMSMessage(LocalizedMMSMessages.MILEAGE_UNITS, userConfig.lang), STRING_COLUMN_WIDTH).apply {
            isVirtual = true
            isSearchable = false
        }
        columnWork = ColumnGrid(modelTableName, "_work", "Работа оборудования [час]").apply {
            isVirtual = true
            isSearchable = false
        }
        columnEnergo = ColumnGrid(modelTableName, "_energo", "Расход/генерация э/энергии").apply {
            isVirtual = true
            isSearchable = false
        }
//        columnGroupSumEnergoName = ColumnString(tableName, "_group_energo_name", "Э/энергия", STRING_COLUMN_WIDTH).apply {
//            isVirtual = true
//            isSearchable = false
//        }
//        columnGroupSumEnergoValue = ColumnString(tableName, "_group_energo_value", "Расход/Генерация", STRING_COLUMN_WIDTH).apply {
//            isVirtual = true
//            isSearchable = false
//        }
        columnLiquid = ColumnGrid(modelTableName, "_liquid_name", "Расход топлива").apply {
            isVirtual = true
            isSearchable = false
        }
//        columnGroupSumLiquidName = ColumnString(tableName, "_group_liquid_name", "Топливо", STRING_COLUMN_WIDTH).apply {
//            isVirtual = true
//            isSearchable = false
//        }
//        columnGroupSumLiquidValue = ColumnString(tableName, "_group_liquid_value", "Расход", STRING_COLUMN_WIDTH).apply {
//            isVirtual = true
//            isSearchable = false
//        }
        columnLevel = ColumnGrid(modelTableName, "_level", "Ёмкость, Нач.остаток, Кон.остаток").apply {
            isVirtual = true
            isSearchable = false
        }
        columnLevelLiquid = ColumnGrid(modelTableName, "_level_liquid_name", "Топливо, Заправка, Слив").apply {
            isVirtual = true
            isSearchable = false
        }

        //----------------------------------------------------------------------------------------------------------------------------------------

        alTableHiddenColumn.add(columnId)
        alTableHiddenColumn.add(columnUser!!)

        alTableGroupColumn.add(columnDate)

        alFormHiddenColumn.add(columnId)
        alFormHiddenColumn.add(columnUser!!)

        //----------------------------------------------------------------------------------------------------------------------------------------

        os = ObjectSelector()
        os.fillColumns(
            model = this,
            isRequired = true,
            isSelector = true,
            alTableHiddenColumn = alTableHiddenColumn,
            alFormHiddenColumn = alFormHiddenColumn,
            alFormColumn = alFormColumn,
            hmParentColumn = hmParentColumn,
            aSingleObjectMode = false,
            addedStaticColumnCount = 0
        )

        //----------------------------------------------------------------------------------------------------------------------------------------

        addTableColumn(columnRun)
        addTableColumn(columnWork)
        addTableColumn(columnEnergo)
        addTableColumn(columnLiquid)
        addTableColumn(columnLevel)
        addTableColumn(columnLevelLiquid)

        alFormColumn.add(columnDate)
        alFormColumn.add(columnRun)
        //alFormColumn.add(columnWork)

        //alFormColumn.add(columnEnergo)
//        alFormColumn.add(columnGroupSumEnergoName)
//        alFormColumn.add(columnGroupSumEnergoValue)
        //alFormColumn.add(columnAllSumEnergo)

        //alFormColumn.add(columnLiquid)
//        alFormColumn.add(columnGroupSumLiquidName)
//        alFormColumn.add(columnGroupSumLiquidValue)
        //alFormColumn.add(columnAllSumLiquid)

        //alFormColumn.add(columnLevel)

        //alFormColumn.add(columnLevelLiquid)

        //----------------------------------------------------------------------------------------------------------------------

        addTableSort(columnDate, false)
        addTableSort(os.columnObjectName, true)

        //----------------------------------------------------------------------------------------

        hmParentColumn["system_user"] = columnUser!!

        //----------------------------------------------------------------------------------------------------------------------

        //--- define the user of mobile objects only
        val hsPermissionDW = userConfig.userPermission["mms_day_work"]
        val hsPermissionSW = userConfig.userPermission["mms_shift_work"]
        val isMovingMode = hsPermissionDW != null && hsPermissionDW.contains(cStandart.PERM_ACCESS) &&
            (hsPermissionSW == null || !hsPermissionSW.contains(cStandart.PERM_ACCESS))

        if (isMovingMode) {
            alChildData.add(ChildData("mms_show_trace", columnId, AppAction.FORM, true, true))
            alChildData.add(ChildData("mms_show_object", columnId, AppAction.FORM))
        }

        MMSFunction.fillChildDataForPeriodicReports(columnId, alChildData)
        MMSFunction.fillChildDataForLiquidIncDecReports(columnId, alChildData, withIncWaybillReport = true, newGroup = false)
        alChildData.add(ChildData("Отчёты", "mms_report_work_detail", columnId, AppAction.FORM))
        MMSFunction.fillChildDataForGeoReports(columnId, alChildData, withMovingDetailReport = true)
        MMSFunction.fillChildDataForEnergoOverReports(columnId, alChildData)
        MMSFunction.fillChildDataForOverReports(columnId, alChildData)
        alChildData.add(ChildData("Отчёты", "mms_report_data_out", columnId, AppAction.FORM))

        MMSFunction.fillAllChildDataForGraphics(columnId, alChildData)

        if (!isMovingMode) {
            alChildData.add(ChildData("mms_show_object", columnId, AppAction.FORM, true))
            alChildData.add(ChildData("mms_show_trace", columnId, AppAction.FORM))
        }
    }

 */