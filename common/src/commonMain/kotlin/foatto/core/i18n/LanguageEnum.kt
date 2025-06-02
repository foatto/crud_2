package foatto.core.i18n

import kotlinx.serialization.Serializable

@Serializable
enum class LanguageEnum(
    val descr: String
) {
    EN("en"),
    RU("ru"),
}