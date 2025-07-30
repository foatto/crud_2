package foatto.server.ds

import kotlinx.serialization.Serializable

@Serializable
class PulsarCommandRequest(
    val serialNo: String,
)