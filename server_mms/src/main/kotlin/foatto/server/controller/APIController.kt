package foatto.server.controller

import foatto.server.model.DevicesStatusRequest
import foatto.server.model.DevicesStatusResponse
import foatto.server.model.ObjectDataRequest
import foatto.server.model.ObjectDataResponse
import foatto.server.model.ObjectEventsRequest
import foatto.server.model.ObjectEventsResponse
import foatto.server.service.APIService
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class APIController(
    private val apiService: APIService,
) {

    @PostMapping(value = ["/ext/object_data/v1"])
    @Transactional
    fun getObjectData(
        @RequestBody
        request: ObjectDataRequest,
    ): ObjectDataResponse = apiService.getObjectData(
        token = request.token,
        objectName = request.name,
        start = request.start,
        duration = request.duration,
    )

    @PostMapping(value = ["/ext/object_events/v1"])
    @Transactional
    fun getObjectEvents(
        @RequestBody
        request: ObjectEventsRequest,
    ): ObjectEventsResponse = apiService.getObjectEvents(
        token = request.token,
        objectName = request.name,
        start = request.start,
        duration = request.duration,
    )

    @PostMapping(value = ["/ext/devices_status/v1"])
    @Transactional
    fun getDevicesStatus(
        @RequestBody
        request: DevicesStatusRequest,
    ): DevicesStatusResponse = apiService.getDevicesStatus(
        token = request.token,
    )
}
