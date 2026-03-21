package foatto.server.ds.request

import kotlinx.serialization.Serializable

@Serializable
class PulsarStorageSaveRequest(
    val serialNo: Int,
    val fileName: String,
    val content: String,
)