package foatto.compose_mms

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import foatto.compose.control.filePickerDialogSettings
import foatto.compose.utils.applicationDispatcher
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@OptIn(DelicateCoroutinesApi::class)
fun main() = application {
    val root = MMSRoot()
    Window(
        onCloseRequest = ::exitApplication,
        title = "Система контроля технологического оборудования и транспорта \"Пульсар\"",
    ) {
        filePickerDialogSettings = FileKitDialogSettings(this.window)
        root.Content()
    }
    GlobalScope.launch(applicationDispatcher) {
        root.start()
    }
}
