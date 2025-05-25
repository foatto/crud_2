package foatto.compose

import com.darkrockstudios.libraries.mpfilepicker.PlatformFile
import foatto.compose.control.model.form.cell.FormFileCellClient
import foatto.core.ApiUrl
import foatto.core.model.emptyAction
import foatto.core.model.response.form.FormFileUploadParams
import foatto.core.model.response.form.FormFileUploadResponse
import foatto.core.model.response.form.cells.FormFileData
import foatto.core.util.getRandomInt
import kotlinx.browser.window
import kotlinx.serialization.json.Json
import org.w3c.xhr.XMLHttpRequest

internal actual fun getDefaultServerAddress(): String? = null
internal actual fun getDefaultServerPort(): Int? = null

internal actual fun invokeUploadFormFile(
    gridData: FormFileCellClient,
    platformFiles: List<PlatformFile>,
    onSuccess: (responseData: FormFileUploadResponse) -> Unit,
) {
    val formData = org.w3c.xhr.FormData().also { formData_ ->
        for (platformFile in platformFiles) {
            val id = -getRandomInt()

            gridData.files.add(FormFileData(id, 0, emptyAction(), platformFile.file.name))
            gridData.addFiles[id] = platformFile.file.name

            formData_.append(FormFileUploadParams.FORM_FILE_IDS, id.toString())
            formData_.append(FormFileUploadParams.FORM_FILE_BLOBS, platformFile.file)
        }
    }

    XMLHttpRequest().apply {
        open("POST", ApiUrl.UPLOAD_FORM_FILE, true)
        onreadystatechange = {
            if (this.readyState == XMLHttpRequest.DONE) {
                if (this.status == 200.toShort()) {
                    if (responseText.isNotBlank()) {
                        val formFileUploadResponse = Json.decodeFromString<FormFileUploadResponse>(responseText)
                        onSuccess(formFileUploadResponse)
                    }
                } else {
                    httpDefaultErrorHandler(this)
                }
            }
        }
        setRequestHeader("contentType", "application/octet-stream")
        send(formData)
    }
}

private fun httpDefaultErrorHandler(xmlHttpRequest: XMLHttpRequest) {
    val status = xmlHttpRequest.status.toInt()
    when {
        status == 400 -> window.location.reload()
        status >= 500 -> println("Ошибка сервера: $status \r\n Сообщение для разработчиков: ${xmlHttpRequest.responseText} ")
        else -> println("Неизвестная ошибка: $status")
    }
}
