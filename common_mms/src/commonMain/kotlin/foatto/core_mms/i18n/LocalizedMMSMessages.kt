package foatto.core_mms.i18n

import foatto.core.i18n.LanguageEnum

fun getLocalizedMMSMessage(message: LocalizedMMSMessages, lang: LanguageEnum): String =
    message.descr[lang] ?: message.descr[LanguageEnum.RU] ?: "(not defined: '$lang'='${message.name}')"

enum class LocalizedMMSMessages(
    val descr: Map<LanguageEnum, String>
) {
    CONTROL(mapOf(LanguageEnum.EN to "Control", LanguageEnum.RU to "Контроль")),
    ACCOUNTING(mapOf(LanguageEnum.EN to "Accounting", LanguageEnum.RU to "Учёт")),
    REFERENCES(mapOf(LanguageEnum.EN to "Reference books", LanguageEnum.RU to "Справочники")),
    DEVICES(mapOf(LanguageEnum.EN to "Devices", LanguageEnum.RU to "Контроллеры")),

    ____(mapOf(LanguageEnum.EN to "____", LanguageEnum.RU to "____")),
}