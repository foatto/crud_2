package foatto.server.model

import kotlinx.serialization.Serializable

@Serializable
class ObjectEventsRequest(
    val token: String,

    val name: String,
    val start: String? = null,
    val duration: Int? = null,
)