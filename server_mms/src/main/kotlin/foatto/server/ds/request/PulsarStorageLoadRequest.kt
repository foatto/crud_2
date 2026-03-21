package foatto.server.ds.request

import kotlinx.serialization.Serializable

@Serializable
class PulsarStorageLoadRequest(
    val serialNo: Int,
    val fileName: String,
)