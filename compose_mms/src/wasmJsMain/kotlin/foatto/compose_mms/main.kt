package foatto.compose_mms

import androidx.compose.material3.SnackbarDuration
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import foatto.compose.utils.applicationDispatcher
import foatto.compose.wsClient
import foatto.compose.wsMessageStore
import foatto.core.model.AppMessage
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.browser.document
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

private val json = Json { prettyPrint = true }

@OptIn(ExperimentalComposeUiApi::class, DelicateCoroutinesApi::class)
fun main() {
    val root = MMSRoot()
    ComposeViewport(document.body!!) {
        root.Content()
    }
    GlobalScope.launch(applicationDispatcher) {
        root.start()
    }
    GlobalScope.launch(applicationDispatcher) {
        wsClient.webSocket(urlString = "/wss") { // this: DefaultClientWebSocketSession
            while (true) {
                delay(1000)

                while (true) {
                    incoming.tryReceive().getOrNull()?.let { frame ->
                        (frame as? Frame.Text)?.let { textFrame ->
                            val appMessage = try {
                                json.decodeFromString<AppMessage>(textFrame.readText())
                            } catch (_: Throwable) {
                                null
                            }
                            appMessage?.let {
                                root.snackbarHostState.showSnackbar(
                                    message = appMessage.message,
                                    withDismissAction = true,
                                    duration = SnackbarDuration.Long,
                                )
                                delay(11_000)   // чтобы SnackbarDuration.Long == 10 sec полностью прошёл
                            }
                        }
                    } ?: break
                }

                while (true) {
                    wsMessageStore.tryReceive().getOrNull()?.let { appMessage ->
                        send(json.encodeToString(appMessage))
                    } ?: break
                }
            }
//            wsClient.close()
        }
    }
}