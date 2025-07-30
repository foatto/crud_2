package foatto.server.controller

import foatto.server.ds.PulsarCommandRequest
import foatto.server.ds.PulsarCommandResult
import foatto.server.ds.PulsarConfig
import foatto.server.ds.PulsarConfigResult
import foatto.server.ds.PulsarData
import foatto.server.service.MMSPulsarConfigService
import foatto.server.service.MMSPulsarDataService
import foatto.server.service.MMSPulsarManageService
import foatto.server.util.AdvancedLogger
import jakarta.servlet.http.HttpServletRequest
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class MMSPulsarController(
    private val mmsPulsarDataService: MMSPulsarDataService,
    private val mmsPulsarConfigService: MMSPulsarConfigService,
    private val mmsPulsarManageService: MMSPulsarManageService,
) {
    //--- /{fileName} - на время Юриной отладки
    @PostMapping(value = ["/data/pulsar/{fileName}"])
    @Transactional
    fun storePulsarData(
        @PathVariable("fileName")
        fileName: String,
        @RequestBody
        arrData: Array<PulsarData>,
        request: HttpServletRequest,
    ) {
        AdvancedLogger.debug(
            message = arrData.joinToString(separator = "\n") { (dateTime, deviceID, blockID, idx, vals) ->
                "\ndateTime = ${dateTime}\n" +
                    "deviceID = ${deviceID}\n" +
                    "blockID = ${blockID}\n" +
                    "idx = ${idx}\n" +
                    vals?.joinToString(separator = "\n") { valuesMap ->
                        valuesMap.entries.joinToString(separator = "\n")
                    }
            },
            subDir = arrData.firstOrNull()?.deviceID ?: "0"
        )

        mmsPulsarDataService.storePulsarData(
            request = request,
            arrData = arrData,
        )
    }

    @PostMapping(value = ["/config/pulsar/v1"])
    @Transactional
    fun storePulsarConfig(
        @RequestBody
        pulsarConfig: PulsarConfig,
    ): PulsarConfigResult {
        return mmsPulsarConfigService.storePulsarConfig(
            pulsarConfig = pulsarConfig,
        )
    }

    @PostMapping(value = ["/manage/pulsar/v1"])
    @Transactional
    fun getPulsarCommand(
        @RequestBody
        pulsarCommandRequest: PulsarCommandRequest,
    ): PulsarCommandResult {
        return mmsPulsarManageService.getPulsarCommand(
            pulsarCommandRequest = pulsarCommandRequest,
        )
    }
}
