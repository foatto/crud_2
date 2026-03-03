package foatto.server.controller.chart

import foatto.core.model.request.AppRequest
import foatto.core.model.request.ChartActionRequest
import foatto.core.model.response.AppResponse
import foatto.core.model.response.ChartActionResponse
import foatto.core_mms.ApiUrlMMS
import foatto.server.service.chart.ChartEnergoSensorsService
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class ChartEnergoSensorsController(
    private val chartEnergoSensorsService: ChartEnergoSensorsService,
) {

    @PostMapping(ApiUrlMMS.CHART_ENERGO_SENSORS)
    @Transactional
    fun chart(
        @RequestBody
        appRequest: AppRequest
    ): AppResponse {
        return chartEnergoSensorsService.chart(
            sessionId = appRequest.sessionId,
            action = appRequest.action,
        )
    }

    @PostMapping(ApiUrlMMS.CHART_ENERGO_SENSORS_ACTION)
    @Transactional
    fun chartAction(
        @RequestBody
        chartActionRequest: ChartActionRequest,
    ): ChartActionResponse = chartEnergoSensorsService.chartAction(
        chartActionRequest = chartActionRequest,
    )
}