package foatto.server.controller.map

import foatto.core.model.request.AppRequest
import foatto.core.model.request.MapActionRequest
import foatto.core.model.response.AppResponse
import foatto.core.model.response.MapActionResponse
import foatto.core_mms.ApiUrlMMS
import foatto.server.service.map.MapTraceService
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class MapTraceController(
    private val mapTraceService: MapTraceService,
) {

    @PostMapping(ApiUrlMMS.MAP_TRACE)
    @Transactional
    fun map(
        @RequestBody
        appRequest: AppRequest
    ): AppResponse = mapTraceService.map(
        sessionId = appRequest.sessionId,
        action = appRequest.action,
    )

    @PostMapping(ApiUrlMMS.MAP_TRACE_ACTION)
    @Transactional
    fun mapAction(
        @RequestBody
        mapActionRequest: MapActionRequest
    ): MapActionResponse = mapTraceService.mapAction(
        mapActionRequest = mapActionRequest,
    )

}