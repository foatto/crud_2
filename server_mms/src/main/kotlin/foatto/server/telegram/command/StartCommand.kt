//package foatto.server.telegram.command
//
//import foatto.core.i18n.LanguageEnum
//import foatto.core.i18n.LocalizedMessages
//import foatto.core.i18n.getLocalizedMessage
//import foatto.core_mms.AppModuleMMS
//import foatto.core_mms.i18n.LocalizedMMSMessages
//import foatto.core_mms.i18n.getLocalizedMMSMessage
//import foatto.server.OrgType
//import foatto.server.SpringApp
//import foatto.server.appModuleConfigs
//import foatto.server.checkAccessPermission
//import foatto.server.model.ServerUserConfig
//import foatto.server.model.SessionData
//import foatto.server.repository.UserPropertyRepository
//import foatto.server.repository.UserRepository
//import foatto.server.service.LogonService
//import foatto.server.telegram.CommandEnum
//import foatto.server.telegram.MMSTelegramBot
//import foatto.server.telegram.TAction
//import kotlinx.serialization.encodeToString
//import kotlinx.serialization.json.Json
//import org.springframework.stereotype.Component
//import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand
//import org.telegram.telegrambots.meta.api.objects.Chat
//import org.telegram.telegrambots.meta.api.objects.User
//import org.telegram.telegrambots.meta.bots.AbsSender
//
//@Component
//class StartCommand(
//    private val logonService: LogonService,
//    private val userRepository: UserRepository,
//    private val userPropertyRepository: UserPropertyRepository,
//) : BotCommand(CommandEnum.START.text, "") {
//
//    private val json = Json { prettyPrint = true }
//
//    override fun execute(absSender: AbsSender, user: User, chat: Chat, arguments: Array<out String>) {
//
//        val userName = user.userName
//
//        val sessionData: SessionData = MMSTelegramBot.getSessionData(userName) ?: run {
//            userRepository.findByTelegram(userName).firstOrNull()?.let { userEntity ->
//                val lang = userEntity.lang ?: SpringApp.defaultLang
//
//                val serverUserConfig = ServerUserConfig(
//                    id = userEntity.id,
//                    currentUserName = userEntity.fullName ?: getLocalizedMessage(LocalizedMessages.UNKNOWN, lang),
//                    roles = userEntity.roles,
//                    timeOffset = userEntity.timeOffset ?: (3 * 3600),
//                    lang = userEntity.lang ?: LanguageEnum.RU,
//                    fullNames = logonService.loadFullUserNames(lang),
//                    shortNames = logonService.loadShortUserNames(lang),
//                    relatedUserIds = logonService.loadRelatedUserIds(
//                        userId = userEntity.id,
//                        parentId = userEntity.parentId ?: 0,
//                        orgType = userEntity.orgType ?: OrgType.ORG_TYPE_WORKER
//                    ),
//                    userProperties = userPropertyRepository.findByUserId(userEntity.id)
//                        .filter { userPropertyEntity ->
//                            !userPropertyEntity.name.isNullOrBlank() && !userPropertyEntity.value.isNullOrBlank()
//                        }.associate { userPropertyEntity ->
//                            (userPropertyEntity.name ?: "") to (userPropertyEntity.value ?: "")
//                        }.toMutableMap(),
//                )
//                val sd = SessionData(serverUserConfig = serverUserConfig)
//                MMSTelegramBot.putSessionData(userName, sd)
//                sd
//            } ?: run {
//                absSender.execute(MMSTelegramBot.createMessage(chat.id.toString(), "${getLocalizedMMSMessage(LocalizedMMSMessages.USER, SpringApp.defaultLang)} '$userName' ${getLocalizedMMSMessage(LocalizedMMSMessages.IS_MISSING_FROM_THE_SYSTEM, SpringApp.defaultLang)}"))
//                return
//            }
//        }
//
//        val serverUserConfig = sessionData.serverUserConfig ?: run {
//            absSender.execute(MMSTelegramBot.createMessage(chat.id.toString(), "${getLocalizedMMSMessage(LocalizedMMSMessages.USER, SpringApp.defaultLang)} '$userName' ${getLocalizedMMSMessage(LocalizedMMSMessages.IS_MISSING_FROM_THE_SYSTEM, SpringApp.defaultLang)}"))
//            return
//        }
//
//        val allButtons = mutableListOf<MutableList<Pair<String, String>>>()
//
//        val buttonRow = mutableListOf<Pair<String, String>>()
//        getStartButton(serverUserConfig, AppModuleMMS.T_OBJECT)?.let { button ->
//            buttonRow += button
//        }
//        getStartButton(serverUserConfig, AppModuleMMS.T_DEVICE)?.let { button ->
//            buttonRow += button
//        }
//        allButtons += buttonRow
//
//        absSender.execute(
//            MMSTelegramBot.createMessage(
//                chat.id.toString(),
//                getLocalizedMMSMessage(LocalizedMMSMessages.SELECT_AN_ACTION, SpringApp.defaultLang),
//            ).apply {
//                replyMarkup = MMSTelegramBot.createInlineKeyboard(allButtons)
//            }
//        )
//    }
//
//    private fun getStartButton(serverUserConfig: ServerUserConfig, module: String): Pair<String, String>? =
//        if (checkAccessPermission(module, serverUserConfig.roles)) {
//            json.encodeToString(
//                TAction(
//                    module = module,
//                )
//            ) to (appModuleConfigs[module]?.captions[serverUserConfig.lang] ?: "<Unknown: $module>")
//        } else {
//            null
//        }
//
//}
