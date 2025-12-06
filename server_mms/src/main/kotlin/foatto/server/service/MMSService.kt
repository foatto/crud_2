package foatto.server.service

import foatto.core.ActionType
import foatto.core.i18n.getLocalizedMessage
import foatto.core.model.AppAction
import foatto.core.model.response.table.TablePopup
import foatto.server.appModuleConfigs
import foatto.server.checkAccessPermission
import foatto.server.model.ServerUserConfig
import foatto.server.repository.ActionLogRepository

abstract class MMSService(
    private val fileStoreService: FileStoreService,
    private val actionLogRepository: ActionLogRepository,
) : ApplicationService(
    fileStoreService = fileStoreService,
    actionLogRepository = actionLogRepository,
) {

    protected fun getTableTablePopupData(
        userConfig: ServerUserConfig,
        childModule: String,
        parentModule: String,
        parentId: Int,
        alPopupData: MutableList<TablePopup>,
    ) {
        getTablePopupData(
            userConfig = userConfig,
            actionType = ActionType.MODULE_TABLE,
            childModule = childModule,
            parentModule = parentModule,
            parentId = parentId,
            alPopupData = alPopupData,
        )
    }

    protected fun getTableReportPopupData(
        userConfig: ServerUserConfig,
        childModule: String,
        parentModule: String,
        parentId: Int,
        begTime: Int,
        endTime: Int,
        alPopupData: MutableList<TablePopup>,
    ) {
        getTablePopupData(
            userConfig = userConfig,
            group = "Отчёты",
            actionType = ActionType.MODULE_FORM,
            childModule = childModule,
            parentModule = parentModule,
            parentId = parentId,
            begTime = begTime,
            endTime = endTime,
            alPopupData = alPopupData,
        )
    }

    protected fun getTableDashboardPopupData(
        userConfig: ServerUserConfig,
        childModule: String,
        parentModule: String,
        parentId: Int,
        alPopupData: MutableList<TablePopup>
    ) {
        getTablePopupData(
            userConfig = userConfig,
            group = "Контрольные панели",
            actionType = ActionType.MODULE_COMPOSITE,
            childModule = childModule,
            parentModule = parentModule,
            parentId = parentId,
            alPopupData = alPopupData,
        )
    }

    protected fun getTableChartPopupData(
        userConfig: ServerUserConfig,
        childModule: String,
        parentModule: String,
        parentId: Int,
        begTime: Int,
        endTime: Int,
        alPopupData: MutableList<TablePopup>
    ) {
        getTablePopupData(
            userConfig = userConfig,
            group = "Графики",
            actionType = ActionType.MODULE_CHART,
            childModule = childModule,
            parentModule = parentModule,
            parentId = parentId,
            begTime = begTime,
            endTime = endTime,
            alPopupData = alPopupData,
        )
    }

    protected fun getTableMapPopupData(
        userConfig: ServerUserConfig,
        childModule: String,
        parentModule: String,
        parentId: Int,
        begTime: Int,
        endTime: Int,
        alPopupData: MutableList<TablePopup>
    ) {
        getTablePopupData(
            userConfig = userConfig,
            group = "Карты",
            actionType = ActionType.MODULE_MAP,
            childModule = childModule,
            parentModule = parentModule,
            parentId = parentId,
            begTime = begTime,
            endTime = endTime,
            alPopupData = alPopupData,
        )
    }

    private fun getTablePopupData(
        userConfig: ServerUserConfig,
        group: String? = null,
        actionType: String,
        childModule: String,
        parentModule: String? = null,
        parentId: Int? = null,
        begTime: Int? = null,
        endTime: Int? = null,
        alPopupData: MutableList<TablePopup>,
    ) {
        if (checkAccessPermission(childModule, userConfig.roles)) {
            alPopupData += TablePopup(
                group = group,
                action = AppAction(
                    type = actionType,
                    module = childModule,
                    parentModule = parentModule,
                    parentId = parentId,
                    begTime = begTime,
                    endTime = endTime,
                ),
                text = appModuleConfigs[childModule]?.captions?.let { captions ->
                    getLocalizedMessage(captions, userConfig.lang)
                } ?: "(неизвестный тип модуля: '$childModule')",
                inNewTab = true,
            )
        }
    }

}