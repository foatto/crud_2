package foatto.core.model.response.table

import foatto.core.model.response.ClientActionButton
import foatto.core.model.response.HeaderData
import foatto.core.model.response.ServerActionButton
import foatto.core.model.response.table.cell.TableBaseCell
import kotlinx.serialization.Serializable

@Serializable
class TableResponse(
    val tabCaption: String,

    val headerData: HeaderData,

    val findText: String,

    val serverActionButtons: List<ServerActionButton>,
    val clientActionButtons: List<ClientActionButton>,

    val columnCaptions: List<TableCaption>,

    val tableCells: List<TableBaseCell>,
    val tableRows: List<TableRow>,
    val selectedRowNo: Int?,

    val tablePageButtonData: List<TablePageButton>
)
