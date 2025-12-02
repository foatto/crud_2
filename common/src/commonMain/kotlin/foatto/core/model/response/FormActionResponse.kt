package foatto.core.model.response

import foatto.core.model.AppAction
import kotlinx.serialization.Serializable

@Serializable
class FormActionResponse(
    override val responseCode: ResponseCode,
    val errors: Map<String, String>? = null,
    //--- It's not currently used anywhere. I may have used it somewhere, but I stopped using it. I'll leave it as a potentially useful parameter/feature.
    val newTabAction: AppAction? = null,
    val nextAction: AppAction? = null,
) : BaseResponse()