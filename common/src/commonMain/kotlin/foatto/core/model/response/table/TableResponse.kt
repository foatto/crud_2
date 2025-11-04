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

    val isFindPanelVisible: Boolean,
    val findText: String,

    val isDateTimeIntervalPanelVisible: Boolean,
    val withTime: Boolean,
    val begDateTimeValue: Int?,
    val endDateTimeValue: Int?,

    val serverActionButtons: List<ServerActionButton>,
    val clientActionButtons: List<ClientActionButton>,

    val isRefreshEnabled: Boolean,

    val columnCaptions: List<TableCaption>,

    val tableCells: List<TableBaseCell>,
    val tableRows: List<TableRow>,
    val selectedRowNo: Int?,

    val pageButtons: List<TablePageButton>
)
