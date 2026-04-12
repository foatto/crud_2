package foatto.server.model

import kotlinx.serialization.Serializable

@Serializable
class DevicesStatusResponse(
    val errorMessage: String? = null,
    val devices: List<DevicesStatusResponseDeviceInfo>? = null,
)

@Serializable
class DevicesStatusResponseDeviceInfo(
    val serialNo: String?,
    val objectName: String?,
    val lastSessionTime: String,
    val lastSessionStatus: String?,
    val lastSessionError: String?,
)
