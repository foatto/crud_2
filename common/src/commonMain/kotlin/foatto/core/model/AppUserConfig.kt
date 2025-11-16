package foatto.core.model

import foatto.core.i18n.LanguageEnum
import kotlinx.serialization.Serializable

@Serializable
class AppUserConfig(
    val currentUserName: String,
    val isAdmin: Boolean,
    val timeOffset: Int,
    var lang: LanguageEnum,
    val userProperties: MutableMap<String, String>
)