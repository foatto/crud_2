package foatto.core.model.response.composite

import foatto.core.model.AppAction
import kotlinx.serialization.Serializable

@Serializable
class CompositeResponse(
    val tabCaption: String,
    val action: AppAction,
    val items: List<CompositeListItemData>?,
)
