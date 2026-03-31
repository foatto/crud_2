package foatto.server.ws

import foatto.core.model.AppMessage
import kotlinx.serialization.json.Json
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

class MMSWSHandler : TextWebSocketHandler() {   // BinaryWebSocketHandler

    companion object {
        private val messageStore = ConcurrentHashMap<Int, ConcurrentLinkedQueue<AppMessage>>()
    }

    private val json = Json { prettyPrint = true }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        val incomeMessage: AppMessage? = try {
            json.decodeFromString(message.payload)
        } catch (_: Throwable) {
            null
        }

        incomeMessage?.let {
            messageStore.getOrPut(incomeMessage.toUserId) { ConcurrentLinkedQueue() }.offer(incomeMessage)

            messageStore[incomeMessage.fromUserId]?.let { queueMessages ->
                while (true) {
                    queueMessages.poll()?.let { outcomeMessage ->
                        session.sendMessage(TextMessage(json.encodeToString(outcomeMessage)))
                    } ?: break
                }
            }
        }
    }
}
