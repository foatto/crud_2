package foatto.core.model.response.table

import foatto.core.model.AppAction
import kotlinx.serialization.Serializable

@Serializable
class TablePageButton(val action: AppAction?, val text: String)