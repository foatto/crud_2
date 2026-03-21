package foatto.server.ds.request

import kotlinx.serialization.Serializable

@Serializable
class PulsarCommandRequest(
    val serialNo: String,
)