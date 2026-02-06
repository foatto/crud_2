package foatto.server.telegram

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.telegram.telegrambots.extensions.bots.commandbot.TelegramLongPollingCommandBot
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton

@Component
class MMSTelegramBot(
    commands: Set<BotCommand>,
    @Value("\${telegram.token}")
    token: String,
) : TelegramLongPollingCommandBot(token) {

    companion object {
        const val CALLBACK_QUERY_DATA_DIVIDER: String = "|"

        fun createMessage(chatId: String, text: String) =
            SendMessage(chatId, text)
                .apply {
                    enableMarkdown(true)
                    disableWebPagePreview()
                }

        //--- ограничения на InlineKeyboard (01.08.2018):
        //--- callBackData = 64 байта
        //--- кнопок в ряд = 8
        //--- кнопок всего = 100
        fun createInlineKeyboard(allButtons: List<List<Pair<String, String>>>): InlineKeyboardMarkup =
            InlineKeyboardMarkup().apply {
                keyboard = allButtons.map { rowButtons ->
                    rowButtons.map { (data, buttonText) ->
                        InlineKeyboardButton().apply {
                            callbackData = data
                            text = buttonText
                        }
                    }
                }
            }
    }

    @Value("\${telegram.botName}")
    private val botName: String = ""

    init {
        registerAll(*commands.toTypedArray())
    }

    override fun getBotUsername(): String = botName

    override fun processNonCommandUpdate(update: Update) {
        if (update.hasMessage()) {
            val chatId = update.message.chatId
            if (update.message.hasText()) {
                val messageText = update.message.text
                // ...
                execute(createMessage(chatId.toString(), "Эхо: $messageText"))
            } else {
                execute(createMessage(chatId.toString(), "Я понимаю только текст!"))
            }
        } else if (update.hasCallbackQuery()) {
            val callbackQuery = update.callbackQuery

            val chatId = callbackQuery.message.chatId
            val callbackQueryId = callbackQuery.id
            val callbackQueryData = callbackQuery.data

            //--- disable wait-progress in chat
            execute(AnswerCallbackQuery(callbackQueryId))

            val callbackArguments = callbackQueryData.split(CALLBACK_QUERY_DATA_DIVIDER)
            val callbackType = try {
                CallbackTypeEnum.valueOf(callbackArguments.first())
            } catch (_: IllegalArgumentException) {
                null
            }
            val callbackParams = callbackArguments.drop(1)

            //--- remove (replace with empty list) command buttons
            execute(
                EditMessageReplyMarkup(
                    chatId.toString(),
                    callbackQuery.message.messageId,
                    callbackQuery.inlineMessageId,
                    createInlineKeyboard(emptyList())
                )
            )
//            when(callbackType) {
//
//            }
        }
    }
}
