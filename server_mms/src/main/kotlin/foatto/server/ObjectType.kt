package foatto.server

import foatto.core.i18n.LanguageEnum
import foatto.core_mms.i18n.LocalizedMMSMessages
import foatto.core_mms.i18n.getLocalizedMMSMessage
import kotlinx.serialization.Serializable

@Serializable
enum class ObjectType {
    MOBILE,
    STATIONARY;

    fun getDescr(lang: LanguageEnum): String = when (this) {
        MOBILE -> getLocalizedMMSMessage(LocalizedMMSMessages.OBJECT_TYPE_MOBILE, lang)
        STATIONARY -> getLocalizedMMSMessage(LocalizedMMSMessages.OBJECT_TYPE_STATIONARY, lang)
    }

}