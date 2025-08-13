package foatto.server.controller

import foatto.core.model.request.AppRequest
import foatto.core.model.request.CompositeActionRequest
import foatto.core.model.response.AppResponse
import foatto.core.model.response.CompositeActionResponse
import foatto.core_mms.ApiUrlMMS
import foatto.server.service.composite.ObjectChartDashboardService
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class ObjectChartDashboardController(
    private val objectChartDashboardService: ObjectChartDashboardService,
) {

    @PostMapping(ApiUrlMMS.OBJECT_CHART_DASHBOARD)
    @Transactional
    fun compositeObjectDashboard(
        @RequestBody
        appRequest: AppRequest
    ): AppResponse = objectChartDashboardService.composite(
        sessionId = appRequest.sessionId,
        action = appRequest.action,
    )

    @PostMapping(ApiUrlMMS.OBJECT_CHART_DASHBOARD_ACTION)
    @Transactional
    fun compositeAction(
        @RequestBody
        compositeActionRequest: CompositeActionRequest,
    ): CompositeActionResponse = objectChartDashboardService.compositeAction(
        sessionId = compositeActionRequest.sessionId,
        compositeActionRequest = compositeActionRequest,
    )

}