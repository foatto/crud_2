package foatto.server.controller

import foatto.core.model.request.AppRequest
import foatto.core.model.request.FormActionRequest
import foatto.core.model.response.AppResponse
import foatto.core.model.response.FormActionResponse
import foatto.core_mms.ApiUrlMMS
import foatto.server.service.DeviceManageService
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class DeviceManageController(
    private val deviceManageService: DeviceManageService,
) {

    @PostMapping(ApiUrlMMS.DEVICE_MANAGE)
    @Transactional
    fun app(
        @RequestBody
        appRequest: AppRequest
    ): AppResponse {
        return deviceManageService.app(
            sessionId = appRequest.sessionId,
            action = appRequest.action,
        )
    }

    @PostMapping(ApiUrlMMS.DEVICE_MANAGE_FORM_ACTION)
    @Transactional
    fun formAction(
        @RequestBody
        formActionRequest: FormActionRequest
    ): FormActionResponse {
        return deviceManageService.formAction(
            sessionId = formActionRequest.sessionId,
            action = formActionRequest.action,
            formActionData = formActionRequest.formActionData,
        )
    }

}
