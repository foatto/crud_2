package foatto.compose

import com.darkrockstudios.libraries.mpfilepicker.PlatformFile
import foatto.compose.control.model.form.cell.FormFileCellClient
import foatto.compose.utils.applicationDispatcher
import foatto.core.ApiUrl
import foatto.core.model.emptyAction
import foatto.core.model.response.form.FormFileUploadParams
import foatto.core.model.response.form.FormFileUploadResponse
import foatto.core.model.response.form.cells.FormFileData
import foatto.core.util.getRandomInt
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

internal actual fun getDefaultServerProtocol(): URLProtocol? = URLProtocol.HTTP
internal actual fun getDefaultServerAddress(): String? = "192.168.0.44"
internal actual fun getDefaultServerPort(): Int? = 19998

@OptIn(DelicateCoroutinesApi::class)
internal actual fun invokeUploadFormFile(
    gridData: FormFileCellClient,
    platformFiles: List<PlatformFile>,
    onSuccess: (responseData: FormFileUploadResponse) -> Unit,
) {
    GlobalScope.launch(applicationDispatcher) {
        val responseData: FormFileUploadResponse = httpClient.post {
            url(ApiUrl.UPLOAD_FORM_FILE)
            setBody(
                MultiPartFormDataContent(
                    parts = formData {
                        for (platformFile in platformFiles) {
                            val id = -getRandomInt()

                            gridData.files.add(FormFileData(id, 0, emptyAction(), platformFile.getName()))
                            gridData.addFiles[id] = platformFile.getName()

                            append(
                                key = FormFileUploadParams.FORM_FILE_IDS,
                                value = id.toString(),
                            )
                            append(
                                key = FormFileUploadParams.FORM_FILE_BLOBS,
                                value = platformFile.readBytes(),   //.toString(),
                                headers = Headers.build {
                                    append(HttpHeaders.ContentType, ContentType.Application.OctetStream)//"application/octet-stream")
                                    append(HttpHeaders.ContentDisposition, "filename=\"${platformFile.getName()}\"")
                                }
                            )
                        }
                    },
//                append(
//                    "document",
//                    ChannelProvider(
//                        size = some.length()) { some.readChannel() },
//                        Headers.build { ...
                )
            )
        }.body()

        onSuccess(responseData)
    }
}
