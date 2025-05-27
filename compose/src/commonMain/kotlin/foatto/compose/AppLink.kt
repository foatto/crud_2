package foatto.compose

import foatto.compose.control.model.form.cell.FormFileCellClient
import foatto.compose.utils.SETTINGS_SERVER_ADDRESS
import foatto.compose.utils.SETTINGS_SERVER_PORT
import foatto.compose.utils.SETTINGS_SERVER_PROTOCOL
import foatto.compose.utils.applicationDispatcher
import foatto.compose.utils.settings
import foatto.core.ApiUrl
import foatto.core.model.emptyAction
import foatto.core.model.request.BaseRequest
import foatto.core.model.response.BaseResponse
import foatto.core.model.response.form.FormFileUploadParams
import foatto.core.model.response.form.FormFileUploadResponse
import foatto.core.model.response.form.cells.FormFileData
import foatto.core.util.getRandomInt
import foatto.core.util.getRandomLong
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

private val sessionId = getRandomLong()

internal val httpClient = HttpClient /* (CIO) - не работает в wasm-target */ {
    install(HttpTimeout)
    install(ContentNegotiation) {
        json()
    }
    defaultRequest {
        getDefaultServerProtocol()?.let { defaultProtocol ->
            url.protocol = settings.getStringOrNull(SETTINGS_SERVER_PROTOCOL)?.let { protocolName ->
                URLProtocol.byName[protocolName] ?: defaultProtocol
            } ?: defaultProtocol
        }
        getDefaultServerAddress()?.let { defaultAddress ->
            host = settings.getString(SETTINGS_SERVER_ADDRESS, defaultAddress)
        }
        getDefaultServerPort()?.let { defaultPort ->
            port = settings.getInt(SETTINGS_SERVER_PORT, defaultPort)
        }
    }
}

internal expect fun getDefaultServerProtocol(): URLProtocol?
internal expect fun getDefaultServerAddress(): String?
internal expect fun getDefaultServerPort(): Int?

@OptIn(DelicateCoroutinesApi::class)
internal inline fun <reified IN : BaseRequest, reified OUT : BaseResponse> invokeRequest(
    requestData: IN,
    crossinline onSuccess: suspend (responseData: OUT) -> Unit,
) {
    GlobalScope.launch(applicationDispatcher) {
        requestData.sessionId = sessionId

        val responseData: OUT = httpClient.post {
            url(requestData.url)
            contentType(ContentType.Application.Json)
            setBody(requestData)
            timeout {
                socketTimeoutMillis = 60_000
            }
        }.body()

        onSuccess(responseData)
    }
}

@OptIn(DelicateCoroutinesApi::class)
internal fun invokeUploadFormFile(
    gridData: FormFileCellClient,
    platformFiles: List<PlatformFile>,
    onSuccess: (responseData: FormFileUploadResponse) -> Unit,
) {
    GlobalScope.launch(applicationDispatcher) {
        val files = mutableListOf<Pair<String, ByteArray>>()
        for (platformFile in platformFiles) {
            GlobalScope.launch(applicationDispatcher) {
                files += platformFile.name to platformFile.readBytes()
            }.join()
        }
        val responseData: FormFileUploadResponse = httpClient.post {
            url(ApiUrl.UPLOAD_FORM_FILE)
            setBody(
                MultiPartFormDataContent(
                    parts = formData {
                        for ((fileName, fileContent) in files) {
                            val id = -getRandomInt()

                            gridData.files.add(FormFileData(id, 0, emptyAction(), fileName))
                            gridData.addFiles[id] = fileName

                            append(
                                key = FormFileUploadParams.FORM_FILE_IDS,
                                value = id.toString(),
                            )
                            append(
                                key = FormFileUploadParams.FORM_FILE_BLOBS,
                                value = fileContent,
                                headers = Headers.build {
                                    append(HttpHeaders.ContentType, ContentType.Application.OctetStream)
                                    append(HttpHeaders.ContentDisposition, "filename=\"${fileName}\"")
                                }
                            )
                        }
                    },
//                append(
//                    "document",
//                    ChannelProvider(
//                        size = some.length()) { some.readChannel() },
//                        Headers.build { ...
// Read large files with streaming API
//file.source().buffered().use { source ->
//    // Process chunks of data
//}
                )
            )
        }.body()

        onSuccess(responseData)
    }
}