package foatto.compose.control

import androidx.compose.runtime.Composable
import foatto.compose.Root

class EmptyControl(
    root: Root,
) : AbstractControl(
    root = root,
    tabId = 0,
) {

    @Composable
    override fun Body() {}

    override suspend fun start() {}
}