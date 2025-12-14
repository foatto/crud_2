package foatto.core.model.response

import foatto.core.model.AppAction
import kotlinx.serialization.Serializable

@Serializable
class MenuData(
    val iconUrl: String? = null,
    val iconSize: Int = 16,
    val caption: String,
    val action: AppAction? = null,
    val subMenuDatas: List<MenuData>? = null,
)
