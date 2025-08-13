package foatto.server.controller

import foatto.core.model.request.AppRequest
import foatto.core.model.request.ChartActionRequest
import foatto.core.model.response.AppResponse
import foatto.core.model.response.ChartActionResponse
import foatto.core_mms.ApiUrlMMS
import foatto.server.service.chart.ChartSensorService
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class ChartSensorController(
    private val chartSensorService: ChartSensorService,
) {

    @PostMapping(ApiUrlMMS.CHART_SENSOR)
    @Transactional
    fun chart(
        @RequestBody
        appRequest: AppRequest
    ): AppResponse {
        return chartSensorService.chart(
            sessionId = appRequest.sessionId,
            action = appRequest.action,
        )
    }

    @PostMapping(ApiUrlMMS.CHART_SENSOR_ACTION)
    @Transactional
    fun chartAction(
        @RequestBody
        chartActionRequest: ChartActionRequest,
    ): ChartActionResponse = chartSensorService.chartAction(
        chartActionRequest = chartActionRequest,
    )
}