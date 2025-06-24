package foatto.core.model.response.table

import foatto.core.model.AppAction
import kotlinx.serialization.Serializable

@Serializable
class TableCaption(
    val name: String,
    val action: AppAction?
)