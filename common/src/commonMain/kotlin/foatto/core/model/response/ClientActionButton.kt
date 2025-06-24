package foatto.core.model.response

import foatto.core.model.AppAction
import kotlinx.serialization.Serializable

@Serializable
class ClientActionButton(
    val name: String,
    val action: AppAction,
    val isForWideScreenOnly: Boolean,
)