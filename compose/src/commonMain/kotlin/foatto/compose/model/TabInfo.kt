package foatto.compose.model

import androidx.compose.runtime.mutableStateListOf

class TabInfo(
    val id: Int,
) {
    val texts = mutableStateListOf<String>()
}