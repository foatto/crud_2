import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import foatto.compose.utils.applicationDispatcher
import foatto.compose_mms.MMSRoot
import kotlinx.browser.document
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class, DelicateCoroutinesApi::class)
fun main() {
    val root = MMSRoot()
    ComposeViewport(document.body!!) {
        root.Content()
    }
    GlobalScope.launch(applicationDispatcher) {
        root.start()
    }
}