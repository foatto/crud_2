package foatto.compose.i18n

import foatto.core.i18n.LanguageEnum

fun getLocalizedText(messageCode: MessageCodeEnum, lang: LanguageEnum): String =
    texts[messageCode]?.let { langTexts ->
        langTexts[lang] ?: langTexts[LanguageEnum.EN] ?: "(text for message code '$messageCode' and language '$lang' not defined)"
    } ?: "(texts for message code '$messageCode' not defined)"

fun addLocalizedText(messageCode: MessageCodeEnum, lang: LanguageEnum, text: String) {
    val langTexts = texts.getOrPut(messageCode) { mutableMapOf() }
    langTexts[lang] = text
}


private val texts: MutableMap<MessageCodeEnum, MutableMap<LanguageEnum, String>> = mutableMapOf(

    MessageCodeEnum.LOGIN to mutableMapOf(
        LanguageEnum.EN to "Login",
        LanguageEnum.RU to "Вход",
    ),
)