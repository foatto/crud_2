package foatto.core.model.request

import foatto.core.ApiUrl
import kotlinx.serialization.Serializable

@Serializable
class LogonRequest(
    val login: String,
    val password: String,
    val isWideScreen: Boolean,
) : BaseRequest(
    url = ApiUrl.LOGON,
)
