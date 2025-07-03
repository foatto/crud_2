package foatto.server.controller

import foatto.core.model.request.AppRequest
import foatto.core.model.request.CompositeActionRequest
import foatto.core.model.response.AppResponse
import foatto.core.model.response.CompositeActionResponse
import foatto.core_mms.ApiUrlMMS
import foatto.server.service.composite.CompositeObjectListDashboardService
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class CompositeObjectListDashboardController(
    private val compositeObjectListDashboardService: CompositeObjectListDashboardService,
) {

    @PostMapping(ApiUrlMMS.COMPOSITE_OBJECT_LIST_DASHBOARD)
    @Transactional
    fun compositeObjectDashboard(
        @RequestBody
        appRequest: AppRequest
    ): AppResponse = compositeObjectListDashboardService.composite(
        sessionId = appRequest.sessionId,
        action = appRequest.action,
    )

    @PostMapping(ApiUrlMMS.COMPOSITE_OBJECT_LIST_DASHBOARD_ACTION)
    @Transactional
    fun compositeAction(
        @RequestBody
        compositeActionRequest: CompositeActionRequest,
    ): CompositeActionResponse = compositeObjectListDashboardService.compositeAction(
        sessionId = compositeActionRequest.sessionId,
        compositeActionRequest = compositeActionRequest,
    )

}