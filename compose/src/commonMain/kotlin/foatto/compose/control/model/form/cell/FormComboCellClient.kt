package foatto.compose.control.model.form.cell

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import foatto.core.model.response.form.cells.FormComboCell

class FormComboCellClient(
    cellName: String,
    minWidth: Int,
    align: Arrangement.Horizontal,
    isVisible: MutableState<Boolean> = mutableStateOf(true),
    val data: FormComboCell,
) : FormBaseCellClient(
    cellName = cellName,
    minWidth = minWidth,
    align = align,
    isVisible = isVisible,
) {
    val values = mutableStateListOf<Pair<String, String>>()
    var current by mutableStateOf(data.value)
}