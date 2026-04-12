package foatto.server.model

import kotlinx.serialization.Serializable

@Serializable
class DevicesStatusRequest(
    val token: String,
)