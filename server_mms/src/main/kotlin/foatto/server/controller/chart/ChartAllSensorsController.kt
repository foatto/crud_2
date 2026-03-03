package foatto.server.controller.chart

import foatto.core.model.request.AppRequest
import foatto.core.model.request.ChartActionRequest
import foatto.core.model.response.AppResponse
import foatto.core.model.response.ChartActionResponse
import foatto.core_mms.ApiUrlMMS
import foatto.server.service.chart.ChartAllSensorsService
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class ChartAllSensorsController(
    private val chartAllSensorsService: ChartAllSensorsService,
) {

    @PostMapping(ApiUrlMMS.CHART_ALL_SENSORS)
    @Transactional
    fun chart(
        @RequestBody
        appRequest: AppRequest
    ): AppResponse {
        return chartAllSensorsService.chart(
            sessionId = appRequest.sessionId,
            action = appRequest.action,
        )
    }

    @PostMapping(ApiUrlMMS.CHART_ALL_SENSORS_ACTION)
    @Transactional
    fun chartAction(
        @RequestBody
        chartActionRequest: ChartActionRequest,
    ): ChartActionResponse = chartAllSensorsService.chartAction(
        chartActionRequest = chartActionRequest,
    )
}