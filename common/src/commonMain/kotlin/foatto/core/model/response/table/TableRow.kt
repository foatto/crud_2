package foatto.core.model.response.table

import foatto.core.model.AppAction
import foatto.core.model.response.ClientActionButton
import foatto.core.model.response.ServerActionButton
import kotlinx.serialization.Serializable

@Serializable
class TableRow(
    val formAction: AppAction? = null,
    val rowAction: AppAction? = null,
    val isRowUrlInNewTab: Boolean = false,
    val gotoAction: AppAction? = null,
    val isGotoUrlInNewTab: Boolean = false,
    val tablePopups: List<TablePopup> = listOf(),
    val serverActionButtons: List<ServerActionButton> = listOf(),
    val clientActionButtons: List<ClientActionButton> = listOf(),
)
