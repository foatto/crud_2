package foatto.server.model

import kotlinx.serialization.Serializable

@Serializable
class ObjectEventsResponse(
    val errorMessage: String? = null,
    val sensors: List<ObjectEventsResponseSensorInfo>? = null,
)

@Serializable
class ObjectEventsResponseSensorInfo(
    val id: String?,
    val name: String?,
    val type: Int?,
    val data: List<ObjectEventsResponseEventInfo>?,
)

@Serializable
class ObjectEventsResponseEventInfo(
    val begTime: String?,
    val endTime: String?,
    val type: Int?,
    val code: Int?,
)
