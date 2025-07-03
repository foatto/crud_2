package foatto.compose.control.model

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

class CompositeListItem(
    val text: String,
    val itemId: Int,
    val itemModule: String,
    val subListDatas: List<CompositeListItem>? = null,
    var isExpanded: MutableState<Boolean> = mutableStateOf(false),
)