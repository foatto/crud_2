package foatto.core.model.response.composite

import kotlinx.serialization.Serializable

@Serializable
class CompositeListItemData(
    val text: String,
    val itemId: Int,
    val itemModule: String,
    val itemStatus: Boolean,
    val subListDatas: List<CompositeListItemData>? = null,
)