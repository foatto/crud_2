package foatto.server.service

import foatto.core.ActionType
import foatto.core.i18n.getLocalizedMessage
import foatto.core.model.AppAction
import foatto.core.model.request.FormActionData
import foatto.core.model.response.FormActionResponse
import foatto.core.model.response.form.cells.FormBaseCell
import foatto.core.model.response.table.TableCaption
import foatto.core.model.response.table.TablePageButton
import foatto.core.model.response.table.TablePopup
import foatto.core.model.response.table.TableRow
import foatto.core.model.response.table.cell.TableBaseCell
import foatto.core.model.response.table.cell.TableCellAlign
import foatto.core.model.response.table.cell.TableCellBackColorType
import foatto.core.model.response.table.cell.TableSimpleCell
import foatto.core.util.getCurrentTimeInt
import foatto.core.util.getSplittedDouble
import foatto.core.util.getTimeZone
import foatto.core_mms.AppModuleMMS
import foatto.server.appModuleConfigs
import foatto.server.checkAccessPermission
import foatto.server.checkRowPermission
import foatto.server.entity.DayWorkEntity
import foatto.server.getEnabledUserIds
import foatto.server.model.AppModuleConfig
import foatto.server.model.ServerUserConfig
import foatto.server.repository.DayWorkRepository
import foatto.server.repository.ObjectRepository
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toInstant
import org.springframework.data.domain.Page
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Service
class DayWorkService(
    private val dayWorkRepository: DayWorkRepository,
    private val objectRepository: ObjectRepository,
    private val calcService: CalcService,
    private val fileStoreService: FileStoreService,
) : ApplicationService(
    fileStoreService = fileStoreService,
) {

    companion object {
        //        private const val FIELD_USER_ID = "userId"
//        private const val FIELD_OBJECT = "obj"
        private const val FIELD_DAY = "day"

        //        private const val FIELD_OBJECT_ID = "obj.id"
        private const val FIELD_DAY_YE = "day.ye"
        private const val FIELD_DAY_MO = "day.mo"
        private const val FIELD_DAY_DA = "day.da"
        private const val FIELD_OBJECT_NAME = "obj.name"    // отдельное определение для сортировки в Hibernate
    }

    //--- на самом деле пока никому не нужно. Просто сделал, чтобы не потерять практики.
    //override fun isDateTimeIntervalPanelVisible(): Boolean = true

    override fun getTableColumnCaptions(action: AppAction, userConfig: ServerUserConfig): List<TableCaption> {
        val alColumnInfo = mutableListOf<Pair<String?, String>>()

        alColumnInfo += null to "" // by date group
        alColumnInfo += null to "" // userId
        alColumnInfo += null to "Объект"
        alColumnInfo += null to "Оборудование"
        alColumnInfo += null to "Работа"
        alColumnInfo += null to "Счётчики топлива"
        alColumnInfo += null to "Расход топлива"
        alColumnInfo += null to "Э/счётчики"
        alColumnInfo += null to "Э/энергия"
        alColumnInfo += null to "Уровень топлива"
        alColumnInfo += null to "Показания на начало периода"
        alColumnInfo += null to "Показания на конец периода"
        alColumnInfo += null to "Температура"
        alColumnInfo += null to "Показания на начало периода"
        alColumnInfo += null to "Показания на конец периода"
        alColumnInfo += null to "Плотность"
        alColumnInfo += null to "Показания на начало периода"
        alColumnInfo += null to "Показания на конец периода"

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
        var currentRowNo: Int? = null
        var row = 0

        val timeZone = getTimeZone(userConfig.timeOffset)

        val pageRequest = getTableSortedPageRequest(
            action,
            Sort.Order(Sort.Direction.DESC, FIELD_DAY_YE),
            Sort.Order(Sort.Direction.DESC, FIELD_DAY_MO),
            Sort.Order(Sort.Direction.DESC, FIELD_DAY_DA),
            Sort.Order(Sort.Direction.ASC, FIELD_OBJECT_NAME),
        )
        val findText = action.findText?.trim() ?: ""

        val enabledUserIds = getEnabledUserIds(action.module, action.type, userConfig.relatedUserIds, userConfig.roles)

        val parentObjectId = if (action.parentModule == AppModuleMMS.OBJECT) {
            action.parentId
        } else {
            null
        }
        val parentObjectEntity = parentObjectId?.let {
            objectRepository.findByIdOrNull(parentObjectId)
        }

        val page: Page<DayWorkEntity> = parentObjectEntity?.let {
            dayWorkRepository.findByObjAndUserIdInAndFilter(
                obj = parentObjectEntity,
                userIds = enabledUserIds,
                findText = findText,
                begDateTime = action.begDateTimeValue ?: -1,
                endDateTime = action.endDateTimeValue ?: -1,
                pageRequest = pageRequest,
            )
        } ?: run {
            dayWorkRepository.findByUserIdInAndFilter(
                userIds = enabledUserIds,
                findText = findText,
                begDateTime = action.begDateTimeValue ?: -1,
                endDateTime = action.endDateTimeValue ?: -1,
                pageRequest = pageRequest,
            )
        }
        fillTablePageButtons(action, page.totalPages, pageButtons)
        val dayWorkEntities = page.content

        val groupColSpan = getTableColumnCaptions(action, userConfig).size
        var prevGroupName: String? = null
//var worksTime = 0
//var usingsTime = 0
//var energosTime = 0
//var liquidLevelsTime = 0
//var temperaturesTime = 0
//var densitiesTime = 0
        for (dayWorkEntity in dayWorkEntities) {

            val objectEntity = dayWorkEntity.obj ?: continue
            val dayEntity = dayWorkEntity.day ?: continue
            val ye = dayEntity.ye ?: continue
            val mo = dayEntity.mo ?: continue
            val da = dayEntity.da ?: continue

            val begTime = LocalDateTime(ye, mo, da, 0, 0).toInstant(timeZone).epochSeconds.toInt()
            val endTime = begTime + 86_400

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

            val rowOwnerShortName = userConfig.shortNames[dayWorkEntity.userId]
            val rowOwnerFullName = userConfig.fullNames[dayWorkEntity.userId]

            val isFormEnabled = checkRowPermission(
                module = action.module,
                actionType = ActionType.MODULE_FORM,
                rowUserRelation = userConfig.relatedUserIds[dayWorkEntity.userId],
                userRoles = userConfig.roles
            )

            val groupName = getDateEntityDMYString(dayEntity)
            if (prevGroupName != groupName) {
                tableCells += TableSimpleCell(
                    row = row,
                    col = col,
                    colSpan = groupColSpan,
                    dataRow = row,
                    name = groupName,
                    backColorType = TableCellBackColorType.GROUP_0,
                    isBoldText = true,
                )
                prevGroupName = groupName
                tableRows += TableRow()
                row++
            }
            tableCells += TableSimpleCell(row = row, col = col++, dataRow = row, name = "", backColorType = TableCellBackColorType.GROUP_0)

            tableCells += getTableUserNameCell(
                row = row,
                col = col++,
                userId = userConfig.id,
                rowUserId = dayWorkEntity.userId,
                rowOwnerShortName = rowOwnerShortName,
                rowOwnerFullName = rowOwnerFullName
            )

            tableCells += TableSimpleCell(
                row = row,
                col = col++,
                dataRow = row,
                name = (objectEntity.name ?: "-") +
                        "\n${objectEntity.model ?: "-"}" +
                        "\n${objectEntity.department?.name ?: "-"}" +
                        "\n${objectEntity.group?.name ?: "-"}"
            )

            tableCells += TableSimpleCell(
                row = row,
                col = col++,
                dataRow = row,
                name = works.joinToString("\n") { wcd -> wcd.sensorEntity.descr?.let { descr -> "$descr = " } ?: "-" },
                align = TableCellAlign.RIGHT,
            )
            tableCells += TableSimpleCell(
                row = row,
                col = col++,
                dataRow = row,
                name = works.joinToString("\n") { wcd -> "${getSplittedDouble(wcd.onTime / 3600.0)} [час]" },
                align = TableCellAlign.LEFT,
            )

            tableCells += TableSimpleCell(
                row = row,
                col = col++,
                dataRow = row,
                name = usings.joinToString("\n") { ccd -> ccd.sensorEntity.descr?.let { descr -> "$descr = " } ?: "-" },
                align = TableCellAlign.RIGHT,
            )
            tableCells += TableSimpleCell(
                row = row,
                col = col++,
                dataRow = row,
                name = usings.joinToString("\n") { ccd -> getSplittedDouble(ccd.value) + (ccd.sensorEntity.dim ?: "") },
                align = TableCellAlign.LEFT,
            )

            tableCells += TableSimpleCell(
                row = row,
                col = col++,
                dataRow = row,
                name = energos.joinToString("\n") { ccd -> ccd.sensorEntity.descr?.let { descr -> "$descr = " } ?: "-" },
                align = TableCellAlign.RIGHT,
            )
            tableCells += TableSimpleCell(
                row = row,
                col = col++,
                dataRow = row,
                name = energos.joinToString("\n") { ccd -> getSplittedDouble(ccd.value) + (ccd.sensorEntity.dim ?: "") },
                align = TableCellAlign.LEFT,
            )

            tableCells += TableSimpleCell(
                row = row,
                col = col++,
                dataRow = row,
                name = liquidLevels.joinToString("\n") { acd -> acd.sensorEntity.descr?.let { descr -> "$descr = " } ?: "-" },
                align = TableCellAlign.RIGHT,
            )
            tableCells += TableSimpleCell(
                row = row,
                col = col++,
                dataRow = row,
                name = liquidLevels.joinToString("\n") { acd -> acd.begValue?.let { begValue -> getSplittedDouble(acd.begValue) + (acd.sensorEntity.dim ?: "") } ?: "-" },
                align = TableCellAlign.CENTER,
            )
            tableCells += TableSimpleCell(
                row = row,
                col = col++,
                dataRow = row,
                name = liquidLevels.joinToString("\n") { acd -> acd.endValue?.let { endValue -> getSplittedDouble(acd.endValue) + (acd.sensorEntity.dim ?: "") } ?: "-" },
                align = TableCellAlign.LEFT,
            )

            tableCells += TableSimpleCell(
                row = row,
                col = col++,
                dataRow = row,
                name = temperatures.joinToString("\n") { acd -> acd.sensorEntity.descr?.let { descr -> "$descr = " } ?: "-" },
                align = TableCellAlign.RIGHT,
            )
            tableCells += TableSimpleCell(
                row = row,
                col = col++,
                dataRow = row,
                name = temperatures.joinToString("\n") { acd -> acd.begValue?.let { begValue -> getSplittedDouble(acd.begValue) + (acd.sensorEntity.dim ?: "") } ?: "-" },
                align = TableCellAlign.CENTER,
            )
            tableCells += TableSimpleCell(
                row = row,
                col = col++,
                dataRow = row,
                name = temperatures.joinToString("\n") { acd -> acd.endValue?.let { endValue -> getSplittedDouble(acd.endValue) + (acd.sensorEntity.dim ?: "") } ?: "-" },
                align = TableCellAlign.LEFT,
            )

            tableCells += TableSimpleCell(
                row = row,
                col = col++,
                dataRow = row,
                name = densities.joinToString("\n") { acd -> acd.sensorEntity.descr?.let { descr -> "$descr = " } ?: "-" },
                align = TableCellAlign.RIGHT,
            )
            tableCells += TableSimpleCell(
                row = row,
                col = col++,
                dataRow = row,
                name = densities.joinToString("\n") { acd -> acd.begValue?.let { begValue -> getSplittedDouble(acd.begValue) + (acd.sensorEntity.dim ?: "") } ?: "-" },
                align = TableCellAlign.CENTER,
            )
            tableCells += TableSimpleCell(
                row = row,
                col = col++,
                dataRow = row,
                name = densities.joinToString("\n") { acd -> acd.endValue?.let { endValue -> getSplittedDouble(acd.endValue) + (acd.sensorEntity.dim ?: "") } ?: "-" },
                align = TableCellAlign.LEFT,
            )

            val formOpenAction = AppAction(
                type = ActionType.MODULE_FORM,
                module = action.module,
                id = dayWorkEntity.id,
                parentModule = action.parentModule,
                parentId = action.parentId
            )

            val popupDatas = getPopupDatas(
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

            if (dayWorkEntity.id == action.id) {
                currentRowNo = row
            }

            row++
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

    private fun getPopupDatas(
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
                text = "Открыть",
                inNewTab = false,
            )
        }

//        getTableChartPopupData(userConfig, AppModuleMMS.CHART_LIQUID_LEVEL, objectId, begTime, endTime, alPopupData)

//        getTableReportPopupData(userConfig, AppModuleMMS.REPORT_SUMMARY, objectId, begTime, endTime, alPopupData)

        if (checkAccessPermission(AppModuleMMS.MAP_TRACE, userConfig.roles)) {
            alPopupData += TablePopup(
                group = "Карты",
                action = AppAction(
                    type = ActionType.MODULE_MAP,
                    module = AppModuleMMS.MAP_TRACE,
                    id = objectId,
                    begTime = begTime,
                    endTime = endTime,
                ),
                text = appModuleConfigs[AppModuleMMS.MAP_TRACE]?.captions?.let { captions ->
                    getLocalizedMessage(captions, userConfig.lang)
                } ?: "(неизвестный тип карты)",
                inNewTab = true,
            )
        }
//        if (checkAccessPermission(AppModuleMMS.SCHEME_ANALOGUE_INDICATOR_STATE, userConfig.roles)) {
//            alPopupData += TablePopupData(
//                group = "Контрольные схемы",
//                action = AppAction(
//                    type = ActionType.MODULE_SCHEME,
//                    module = AppModuleMMS.SCHEME_ANALOGUE_INDICATOR_STATE,
//                    id = objectId,
//                ),
//                text = appModuleConfigs[AppModuleMMS.SCHEME_ANALOGUE_INDICATOR_STATE]?.caption ?: "(неизвестный тип схемы)",
//                inNewTab = true,
//            )
//        }
//        if (checkAccessPermission(AppModuleMMS.OBJECT_SCHEME_DASHBOARD, userConfig.roles)) {
//            alPopupData += TablePopup(
//                group = "Контрольные панели",
//                action = AppAction(
//                    type = ActionType.MODULE_COMPOSITE,
//                    module = AppModuleMMS.OBJECT_SCHEME_DASHBOARD,
//                    id = objectId,
//                ),
//                text = appModuleConfigs[AppModuleMMS.OBJECT_SCHEME_DASHBOARD]?.caption ?: "(неизвестный тип контрольной панели)",
//                inNewTab = true,
//            )
//        }

        return alPopupData
    }

    private fun getTableChartPopupData(
        userConfig: ServerUserConfig,
        module: String,
        objectId: Int,
        begTime: Int,
        endTime: Int,
        alPopupData: MutableList<TablePopup>
    ) {
        if (checkAccessPermission(module, userConfig.roles)) {
            alPopupData += TablePopup(
                group = "Графики",
                action = AppAction(
                    type = ActionType.MODULE_CHART,
                    module = module,
                    id = objectId,
                    timeRangeType = 0,
                    begTime = begTime,
                    endTime = endTime,
                    //!!! где-то здесь надо передавать конкретный тип аналогового датчика (пока будем выводить графики по всем аналоговым датчикам сразу)
                ),
                text = appModuleConfigs[module]?.captions?.let { captions ->
                    getLocalizedMessage(captions, userConfig.lang)
                } ?: "(неизвестный тип модуля: '$module')",
                inNewTab = true,
            )
        }
    }

    private fun getTableReportPopupData(
        userConfig: ServerUserConfig,
        module: String,
        objectId: Int,
        begTime: Int,
        endTime: Int,
        alPopupData: MutableList<TablePopup>
    ) {
        if (checkAccessPermission(module, userConfig.roles)) {
            alPopupData += TablePopup(
                group = "Отчёты",
                action = AppAction(
                    type = ActionType.MODULE_FORM,
                    module = module,
                    id = null,
                    parentModule = AppModuleMMS.OBJECT,
                    parentId = objectId,
                    begTime = begTime,
                    endTime = endTime,
                ),
                text = appModuleConfigs[module]?.captions?.let { captions ->
                    getLocalizedMessage(captions, userConfig.lang)
                } ?: "(неизвестный тип модуля: '$module')",
                inNewTab = true,
            )
        }
    }

    override fun getFormCells(action: AppAction, userConfig: ServerUserConfig, moduleConfig: AppModuleConfig, addEnabled: Boolean, editEnabled: Boolean): List<FormBaseCell> {
        TODO("Not yet implemented")
    }

    override fun getFormActionPermissions(action: AppAction, userConfig: ServerUserConfig, moduleConfig: AppModuleConfig): Triple<Boolean, Boolean, Boolean> {
        TODO("Not yet implemented")
    }

    override fun formActionSave(action: AppAction, userConfig: ServerUserConfig, moduleConfig: AppModuleConfig, formActionData: Map<String, FormActionData>): FormActionResponse {
        TODO("Not yet implemented")
    }

    override fun formActionDelete(userId: Int, id: Int): FormActionResponse {
        TODO("Not yet implemented")
    }

}
/*
        columnRun = ColumnString(modelTableName, "_run", "Пробег [км]", STRING_COLUMN_WIDTH).apply {
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