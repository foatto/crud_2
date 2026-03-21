package foatto.server.service.pulsar

import foatto.server.ds.response.PulsarStorageLoadResult
import foatto.server.ds.response.PulsarStorageSaveResult
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File

@Service
class MMSPulsarStorageService {

    @Value("\${pulsar_storage_dir}")
    private val pulsarStorageDir: String = ""

    fun saveToStorage(
        serialNo: Int,
        fileName: String,
        content: String,
    ): PulsarStorageSaveResult {

        val controllerRoot = File(pulsarStorageDir, serialNo.toString())
        controllerRoot.mkdirs()

        val contentFile = File(controllerRoot, fileName)
        contentFile.writeText(content)

        return PulsarStorageSaveResult(0)
    }

    fun loadFromStorage(
        serialNo: Int,
        fileName: String,
    ): PulsarStorageLoadResult {

        val controllerRoot = File(pulsarStorageDir, serialNo.toString())
        if (!controllerRoot.exists()) {
            return PulsarStorageLoadResult(1)
        }

        val contentFile = File(controllerRoot, fileName)
        if (!contentFile.exists()) {
            return PulsarStorageLoadResult(2)
        }

        return PulsarStorageLoadResult(0, contentFile.readText())
    }
}