package foatto.server.controller

import foatto.server.ds.PulsarData
import foatto.server.ds.request.PulsarCommandRequest
import foatto.server.ds.request.PulsarConfigRequest
import foatto.server.ds.request.PulsarStorageLoadRequest
import foatto.server.ds.request.PulsarStorageSaveRequest
import foatto.server.ds.response.PulsarCommandResponse
import foatto.server.ds.response.PulsarConfigResponse
import foatto.server.ds.response.PulsarStorageLoadResult
import foatto.server.ds.response.PulsarStorageSaveResult
import foatto.server.service.pulsar.MMSPulsarConfigService
import foatto.server.service.pulsar.MMSPulsarDataService
import foatto.server.service.pulsar.MMSPulsarManageService
import foatto.server.service.pulsar.MMSPulsarStorageService
import foatto.server.util.AdvancedLogger
import jakarta.servlet.http.HttpServletRequest
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@RestController
class MMSPulsarController(
    private val mmsPulsarDataService: MMSPulsarDataService,
    private val mmsPulsarConfigService: MMSPulsarConfigService,
    private val mmsPulsarManageService: MMSPulsarManageService,
    private val mmsPulsarStorageService: MMSPulsarStorageService,
) {
    //--- /{fileName} - на время Юриной отладки
    @PostMapping(value = ["/data/pulsar/{fileName}"])
    @Transactional
    fun storePulsarData(
        @PathVariable fileName: String,
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
        pulsarConfigRequest: PulsarConfigRequest,
    ): PulsarConfigResponse = mmsPulsarConfigService.storePulsarConfig(
        pulsarConfigRequest = pulsarConfigRequest,
    )

    @PostMapping(value = ["/manage/pulsar/v1"])
    @Transactional
    fun getPulsarCommand(
        @RequestBody
        pulsarCommandRequest: PulsarCommandRequest,
    ): PulsarCommandResponse = mmsPulsarManageService.getPulsarCommand(
        pulsarCommandRequest = pulsarCommandRequest,
    )

    @PostMapping(value = ["/storage/pulsar/save/v1"])
    @Transactional
    fun saveToPulsarStorage(
        @RequestBody
        pulsarStorageSaveRequest: PulsarStorageSaveRequest,
    ): PulsarStorageSaveResult = mmsPulsarStorageService.saveToStorage(
        serialNo = pulsarStorageSaveRequest.serialNo,
        fileName = pulsarStorageSaveRequest.fileName,
        content = pulsarStorageSaveRequest.content,
    )

    @PostMapping(value = ["/storage/pulsar/load/v1"])
    @Transactional
    fun loadFromPulsarStorage(
        @RequestBody
        pulsarStorageLoadRequest: PulsarStorageLoadRequest,
    ): PulsarStorageLoadResult = mmsPulsarStorageService.loadFromStorage(
        serialNo = pulsarStorageLoadRequest.serialNo,
        fileName = pulsarStorageLoadRequest.fileName,
    )

}

/*
    У меня предложение: можем сделать такой урл //pulsar/v1
    Я бы там хранил контекст конкретного прибора, и некоторые настройки.
    А потом при замене контроллера просто вычитал что было, и в ручную вводить не нужно повторно.
    Таблицы точно не нужно, я предлагаю это использовать как файловую систему с одним уровнем.
    Там будет 2-3 json файла с моими настройками, содержимое их тебе до лампочки, размеры до 100 кб. Они мне только нужны. Как тебе такой вариант ?
    /storage/pulsar/v1/sn_1014 думаю так удобнее. Можно как прочитать, так и записать. Что скажешь ?
    sn_1014 - серийник.
    тогда с моей стороны выглядит так PUT /storage/pulsar/v1/sn_1014/contex.json
    и на чтение GET /storage/pulsar/v1/sn_1014/contex.json
    Получается кусок памяти моего прибора, но в облаке нестираемом. Как тебе ? */