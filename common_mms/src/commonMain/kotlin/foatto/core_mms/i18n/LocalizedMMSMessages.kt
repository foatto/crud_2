package foatto.core_mms.i18n

import foatto.core.i18n.LanguageEnum

fun getLocalizedMMSMessage(message: LocalizedMMSMessages, lang: LanguageEnum): String =
    message.descr[lang] ?: message.descr[LanguageEnum.RU] ?: "(not defined: '$lang'='${message.name}')"

enum class LocalizedMMSMessages(
    val descr: Map<LanguageEnum, String>
) {
    OBJECT_TYPE_MOBILE(mapOf(LanguageEnum.EN to "Mobile", LanguageEnum.RU to "Мобильный")),
    OBJECT_TYPE_STATIONARY(mapOf(LanguageEnum.EN to "Stationary", LanguageEnum.RU to "Стационарный")),

    CONTROL(mapOf(LanguageEnum.EN to "Control", LanguageEnum.RU to "Контроль")),
    ACCOUNTING(mapOf(LanguageEnum.EN to "Accounting", LanguageEnum.RU to "Учёт")),
    REPORTS(mapOf(LanguageEnum.EN to "Reports", LanguageEnum.RU to "Отчёты")),
    REFERENCES(mapOf(LanguageEnum.EN to "Reference books", LanguageEnum.RU to "Справочники")),
    DEVICES(mapOf(LanguageEnum.EN to "Devices", LanguageEnum.RU to "Контроллеры")),

    ____(mapOf(LanguageEnum.EN to "____", LanguageEnum.RU to "____")),
}