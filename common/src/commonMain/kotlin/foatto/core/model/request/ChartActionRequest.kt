package foatto.core.model.request

import foatto.core.ApiUrl
import foatto.core.appModuleUrls
import foatto.core.model.AppAction
import kotlinx.serialization.Serializable

@Serializable
class ChartActionRequest(
    val action: AppAction,

    val times: Pair<Int, Int>,
    val viewSize: Pair<Float, Float>,
) : BaseRequest(
    url = appModuleUrls[action.module]?.chartActionUrl ?: ApiUrl.ERROR,
)

