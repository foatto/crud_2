package foatto.core.model

import kotlinx.serialization.Serializable

@Serializable
class AppModuleUrl(
    val appUrl: String,
    val chartActionUrl: String? = null,
    val compositeActionUrl: String? = null,
    val formActionUrl: String? = null,
    val mapActionUrl: String? = null,
    val schemeActionUrl: String? = null,
)