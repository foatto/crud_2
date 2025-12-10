package foatto.server.service

import foatto.core.ActionType
import foatto.core.i18n.getLocalizedMessage
import foatto.core.model.AppAction
import foatto.core.model.response.table.TablePageButton
import foatto.core.model.response.table.TablePopup
import foatto.core.util.getDateTimeDMYHMSString
import foatto.server.appModuleConfigs
import foatto.server.checkAccessPermission
import foatto.server.model.ServerUserConfig
import foatto.server.repository.ActionLogRepository
import kotlinx.datetime.FixedOffsetTimeZone
import kotlinx.datetime.TimeZone

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

    protected fun getTableTimedPageButtons(
        pageSizeInSec: Int,
        action: AppAction,
        zoneUser: FixedOffsetTimeZone,
        firstTimeUTC: Int,
        lastTimeUTC: Int,
        currentTimedPageNo: Int,
        pageButtons: MutableList<TablePageButton>,
    ) {
        //--- из-за возможного статического меню слева пришлось уменьшить с 4 до 3
        val NEAR_PAGE_BUTTON_COUNT = 3
        val pageNo = action.pageNo
        val pageCount = lastTimeUTC / pageSizeInSec - firstTimeUTC / pageSizeInSec + 1

        //--- current page
        pageButtons += getTableTimedPageButton(
            action = null,
            timeZone = zoneUser,
            pageNo = pageNo,
            time = currentTimedPageNo * pageSizeInSec,
        )

        //--- previous pages
        for (i in 1..NEAR_PAGE_BUTTON_COUNT) {
            val prevPageNo = pageNo - i
            if (prevPageNo >= 0) {
                pageButtons.add(
                    0,
                    getTableTimedPageButton(
                        action = action,
                        timeZone = zoneUser,
                        pageNo = prevPageNo,
                        time = (currentTimedPageNo + i) * pageSizeInSec,
                    )
                )
            } else {
                break
            }
        }

        //--- previous pages / left side
        val firstPageButton = getTableTimedPageButton(
            action = action,
            timeZone = zoneUser,
            pageNo = 0,
            time = lastTimeUTC / pageSizeInSec * pageSizeInSec,
        )
        val secondPageButton = getTableTimedPageButton(
            action = action,
            timeZone = zoneUser,
            pageNo = 1,
            time = lastTimeUTC / pageSizeInSec * pageSizeInSec - pageSizeInSec,
        )
        if (pageNo - NEAR_PAGE_BUTTON_COUNT > 2) {
            pageButtons.add(0, TablePageButton(null, "..."))
            pageButtons.add(0, firstPageButton)
        } else if (pageNo - NEAR_PAGE_BUTTON_COUNT > 1) {
            pageButtons.add(0, secondPageButton)
            pageButtons.add(0, firstPageButton)
        } else if (pageNo - NEAR_PAGE_BUTTON_COUNT > 0) {
            pageButtons.add(0, firstPageButton)
        }

        //--- next pages
        for (i in 1..NEAR_PAGE_BUTTON_COUNT) {
            val nextPageNo = pageNo + i
            if (nextPageNo <= pageCount - 1) {
                pageButtons += getTableTimedPageButton(
                        action = action,
                        timeZone = zoneUser,
                        pageNo = nextPageNo,
                        time = (currentTimedPageNo - i) * pageSizeInSec,
                    )
            } else {
                break
            }
        }

        //--- next pages / right side
        val lastPageButton = getTableTimedPageButton(
            action = action,
            timeZone = zoneUser,
            pageNo = pageCount - 1,
            time = firstTimeUTC / pageSizeInSec * pageSizeInSec,
        )
        val prevLastPageButton = getTableTimedPageButton(
            action = action,
            timeZone = zoneUser,
            pageNo = pageCount - 2,
            time = firstTimeUTC / pageSizeInSec * pageSizeInSec + pageSizeInSec,
        )
        if (pageNo + NEAR_PAGE_BUTTON_COUNT < pageCount - 3) {
            pageButtons += TablePageButton(null, "...")
            pageButtons += lastPageButton
        } else if (pageNo + NEAR_PAGE_BUTTON_COUNT < pageCount - 2) {
            pageButtons += prevLastPageButton
            pageButtons += lastPageButton
        } else if (pageNo + NEAR_PAGE_BUTTON_COUNT < pageCount - 1) {
            pageButtons += lastPageButton
        }
    }

    private fun getTableTimedPageButton(
        action: AppAction? = null,
        timeZone: TimeZone,
        pageNo: Int,
        time: Int,
    ): TablePageButton {
        var pageCaption = getDateTimeDMYHMSString(timeZone, time)
        //--- remove last seconds digits
        pageCaption = pageCaption.substring(0, pageCaption.length - 3)

        return action?.let {
            TablePageButton(action.copy(pageNo = pageNo), pageCaption)
        } ?: TablePageButton(null, pageCaption)
    }

}