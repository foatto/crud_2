package foatto.compose.control.model.table.cell

import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color

class TableButtonCellClient(
    colSpan: Int,
    dataRow: Int?,
    minWidth: Int,
    align: Alignment,
    isStaticBackColor: Boolean,
    backColor: Color,
    textColor: Color,
    isBoldText: Boolean,

    val data: List<TableCellDataClient>,
) : TableBaseCellClient(
    colSpan = colSpan,
    dataRow = dataRow,
    minWidth = minWidth,
    align = align,
    isStaticBackColor = isStaticBackColor,
    backColor = backColor,
    textColor = textColor,
    isBoldText = isBoldText,
)
