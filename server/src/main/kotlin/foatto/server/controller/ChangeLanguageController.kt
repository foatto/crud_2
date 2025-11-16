package foatto.server.controller

import foatto.core.ApiUrl
import foatto.core.model.request.ChangeLanguageRequest
import foatto.core.model.response.ChangeLanguageResponse
import foatto.server.service.ChangeLanguageService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class ChangeLanguageController(
    private val changeLanguageService: ChangeLanguageService,
) {

    @PostMapping(ApiUrl.CHANGE_LANGUAGE)
    fun changeLanguage(
        @RequestBody
        changeLanguageRequest: ChangeLanguageRequest
    ): ChangeLanguageResponse {

        changeLanguageService.changeLanguage(
            sessionId = changeLanguageRequest.sessionId,
            lang = changeLanguageRequest.lang,
        )

        return ChangeLanguageResponse()
    }
}