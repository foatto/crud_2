package foatto.core.model.request

import foatto.core.ApiUrl
import foatto.core.i18n.LanguageEnum
import kotlinx.serialization.Serializable

@Serializable
class ChangeLanguageRequest(
    val lang: LanguageEnum,
) : BaseRequest(
    url = ApiUrl.CHANGE_LANGUAGE,
)
