//package foatto.server.telegram.command
//
//import foatto.server.telegram.Commands
//import foatto.server.telegram.TelegramBot
//import org.springframework.stereotype.Component
//import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand
//import org.telegram.telegrambots.meta.api.objects.Chat
//import org.telegram.telegrambots.meta.api.objects.User
//import org.telegram.telegrambots.meta.bots.AbsSender
//
//@Component
//class LogonCommand : BotCommand(Commands.LOGON.text, "")  {
//
//    override fun execute(absSender: AbsSender, user: User, chat: Chat, arguments: Array<out String>) {
//        val numbers = arguments.map { it.toInt() }
//        val sum = numbers.sum()
//        absSender.execute(
//            TelegramBot.createMessage(
//                chat.id.toString(),
//                numbers.joinToString(separator = " + ", postfix = " = $sum"),
//            )
//        )
//    }
//}
