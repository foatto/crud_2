//package foatto.server.telegram
//
//import foatto.core.SESSION_EXPIRE_TIME
//import foatto.core.util.getCurrentTimeInt
//import foatto.core_mms.AppModuleMMS
//import foatto.server.model.SessionData
//import kotlinx.serialization.json.Json
//import org.springframework.beans.factory.annotation.Value
//import org.springframework.stereotype.Component
//import org.telegram.telegrambots.extensions.bots.commandbot.TelegramLongPollingCommandBot
//import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand
//import org.telegram.telegrambots.meta.api.methods.send.SendMessage
//import org.telegram.telegrambots.meta.api.objects.Update
//import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
//import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
//import java.util.concurrent.ConcurrentHashMap
//
//@Component
//class MMSTelegramBot(
//    commands: Set<BotCommand>,
//    @Value("\${telegram.token}")
//    token: String,
//) : TelegramLongPollingCommandBot(token) {
//
//    companion object {
//
//        private val sessionDataTime = ConcurrentHashMap<String, Int>()
//        private val sessionDataStore = ConcurrentHashMap<String, SessionData>()
//
//        fun getSessionData(userName: String): SessionData? {
//            val sessionExpireTime = sessionDataTime[userName] ?: 0
//            return if (getCurrentTimeInt() < sessionExpireTime) {
//                sessionDataTime[userName] = getCurrentTimeInt() + SESSION_EXPIRE_TIME
//                sessionDataStore[userName]
//            } else {
//                removeSessionData(userName)
//                null
//            }
//        }
//
//        fun putSessionData(userName: String, sessionData: SessionData) {
//            sessionDataTime[userName] = getCurrentTimeInt() + SESSION_EXPIRE_TIME
//            sessionDataStore[userName] = sessionData
//        }
//
//        fun removeSessionData(userName: String) {
//            sessionDataTime.remove(userName)
//            sessionDataStore.remove(userName)
//        }
//
//        fun createMessage(chatId: String, text: String) =
//            SendMessage(chatId, text)
//                .apply {
//                    enableMarkdown(true)
//                    disableWebPagePreview()
//                }
//
//        //--- ограничения на InlineKeyboard (01.08.2018):
//        //--- callBackData = 64 байта
//        //--- кнопок в ряд = 8
//        //--- кнопок всего = 100
//        fun createInlineKeyboard(allButtons: List<List<Pair<String, String>>>): InlineKeyboardMarkup =
//            InlineKeyboardMarkup().apply {
//                keyboard = allButtons.map { rowButtons ->
//                    rowButtons.map { (data, buttonText) ->
//                        InlineKeyboardButton().apply {
//                            callbackData = data
//                            text = buttonText
//                        }
//                    }
//                }
//            }
//    }
//
//    @Value("\${telegram.botName}")
//    private val botName: String = ""
//
//    init {
//        registerAll(*commands.toTypedArray())
//    }
//
//    override fun getBotUsername(): String = botName
//
//    private val json = Json { prettyPrint = true }
//
//    override fun processNonCommandUpdate(update: Update) {
//        if (update.hasMessage()) {
//            val chatId = update.message.chatId
//            if (update.message.hasText()) {
//                val messageText = update.message.text
//                // ...
//                execute(createMessage(chatId.toString(), "Echo: $messageText"))
//            } else {
//                execute(createMessage(chatId.toString(), "Text only enabled!"))
//            }
//        } else if (update.hasCallbackQuery()) {
//            val callbackQuery = update.callbackQuery
//
////            val chatId = callbackQuery.message.chatId
////            val callbackQueryId = callbackQuery.id
//            val callbackQueryData = callbackQuery.data
//
//            //--- disable wait-progress in chat
////            execute(AnswerCallbackQuery(callbackQueryId))
//
//            val action = json.decodeFromString<TAction>(callbackQueryData)
//
//            //--- remove (replace with empty list) command buttons
////            execute(
////                EditMessageReplyMarkup(
////                    chatId.toString(),
////                    callbackQuery.message.messageId,
////                    callbackQuery.inlineMessageId,
////                    createInlineKeyboard(emptyList())
////                )
////            )
//
//            when (action.module) {
//                AppModuleMMS.T_OBJECT -> {
//
//                }
//
//                AppModuleMMS.T_DEVICE -> {
//
//                }
//            }
//        }
//    }
//}
