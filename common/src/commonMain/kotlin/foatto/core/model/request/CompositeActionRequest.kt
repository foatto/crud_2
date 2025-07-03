package foatto.core.model.request

import foatto.core.ApiUrl
import foatto.core.appModuleUrls
import foatto.core.model.AppAction
import kotlinx.serialization.Serializable
import kotlin.collections.get

@Serializable
class CompositeActionRequest(
    val action: AppAction,
) : BaseRequest(
    url = appModuleUrls[action.module]?.compositeActionUrl ?: ApiUrl.ERROR,
)
//{
//    val hmParam: MutableMap<String, String> = mutableMapOf()
//}