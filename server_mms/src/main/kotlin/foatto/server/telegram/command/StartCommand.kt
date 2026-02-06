package foatto.server.telegram.command

import foatto.server.telegram.CallbackTypeEnum
import foatto.server.telegram.CommandEnum
import foatto.server.telegram.MMSTelegramBot
import org.springframework.stereotype.Component
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand
import org.telegram.telegrambots.meta.api.objects.Chat
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.bots.AbsSender

@Component
class StartCommand : BotCommand(CommandEnum.START.text, "") {
    override fun execute(absSender: AbsSender, user: User, chat: Chat, arguments: Array<out String>) {
        absSender.execute(MMSTelegramBot.createMessage(chat.id.toString(), "Привет, ${user.userName}!"))

        absSender.execute(
            MMSTelegramBot.createMessage(
                chat.id.toString(),
                "Выберите действие:",
            ).apply {
                replyMarkup = MMSTelegramBot.createInlineKeyboard(
                    listOf(
                        listOf(
                            CallbackTypeEnum.CONTROL.name to "Контроль",
                            CallbackTypeEnum.WORK_LOGS.name to "Журналы работ",
                        ),
                        listOf(
                            CallbackTypeEnum.OBJECT_LIST.name to "Объекты",
                            CallbackTypeEnum.DEVICES.name to "Контроллеры",
                        ),
                    ),
                )
            }
        )
    }

}

