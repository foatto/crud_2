package foatto.compose_mms

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import foatto.compose.control.filePickerDialogSettings
import foatto.compose.utils.applicationDispatcher
import foatto.core_mms.i18n.LocalizedMMSMessages
import foatto.core_mms.i18n.getLocalizedMMSMessage
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@OptIn(DelicateCoroutinesApi::class)
fun main() = application {
    val root = MMSRoot()
    Window(
        onCloseRequest = ::exitApplication,
        title = getLocalizedMMSMessage(LocalizedMMSMessages.TITLE, root.appUserConfig.lang),
    ) {
        filePickerDialogSettings = FileKitDialogSettings(this.window)
        root.Content()
    }
    GlobalScope.launch(applicationDispatcher) {
        root.start()
    }
}
