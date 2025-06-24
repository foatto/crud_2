package foatto.core.model.response

import foatto.core.model.AppAction
import kotlinx.serialization.Serializable

@Serializable
class ServerActionButton(
    val name: String,
    val action: AppAction,
    val inNewTab: Boolean,
    val isForWideScreenOnly: Boolean,
)
