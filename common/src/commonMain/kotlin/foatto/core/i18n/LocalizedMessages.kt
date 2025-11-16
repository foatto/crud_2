package foatto.core.i18n

fun getLocalizedMessage(message: LocalizedMessages, lang: LanguageEnum): String =
    message.descr[lang] ?: message.descr[LanguageEnum.RU] ?: "(not defined: '$lang'='${message.name}')"

fun getLocalizedMessage(messages: Map<LanguageEnum, String>, lang: LanguageEnum): String =
    messages[lang] ?: messages[LanguageEnum.RU] ?: "(not defined: '$lang'='${messages.entries.joinToString()}')"

enum class LocalizedMessages(
    val descr: Map<LanguageEnum, String>
) {
    ERROR_LOGIN_NOT_ENTERED(mapOf(LanguageEnum.EN to "Login not entered", LanguageEnum.RU to "Не введён логин")),
    ERROR_FULL_NAME_NOT_ENTERED(mapOf(LanguageEnum.EN to "Full name not entered", LanguageEnum.RU to "Не введёно полное имя")),
    ERROR_SUCH_LOGIN_ALREADY_EXISTS(mapOf(LanguageEnum.EN to "Such login already exists", LanguageEnum.RU to "Такой логин уже существует")),
    ERROR_PASSWORD_NOT_ENTERED(mapOf(LanguageEnum.EN to "Password not entered", LanguageEnum.RU to "Не введён пароль")),

    ADD(mapOf(LanguageEnum.EN to "Add", LanguageEnum.RU to "Добавить")),
    //--- используется в дочерних проектах
    ADDITIONAL(mapOf(LanguageEnum.EN to "Additional", LanguageEnum.RU to "Дополнительно")),
    AGAIN(mapOf(LanguageEnum.EN to "Again", LanguageEnum.RU to "Ещё раз")),
    AUTHORIZATION(mapOf(LanguageEnum.EN to "Login", LanguageEnum.RU to "Авторизация")),
    BLOCKED(mapOf(LanguageEnum.EN to "Blocked", LanguageEnum.RU to "Заблокирован")),
    BLOCK_TYPE_NOT_SPECIFIED(mapOf(LanguageEnum.EN to "Block type not specified", LanguageEnum.RU to "Не задан тип блока")),
    CANCEL(mapOf(LanguageEnum.EN to "Cancel", LanguageEnum.RU to "Отмена")),
    //--- используется в дочерних проектах
    CHANGE_PASSWORD(mapOf(LanguageEnum.EN to "Change password", LanguageEnum.RU to "Сменить пароль")),
//    CLOSE(mapOf(LanguageEnum.EN to "Close", LanguageEnum.RU to "Закрыть")),
    CONTACT_INFO(mapOf(LanguageEnum.EN to "Contact info", LanguageEnum.RU to "Контактная информация")),
//    DELETE(mapOf(LanguageEnum.EN to "Delete", LanguageEnum.RU to "Удалить")),
    DIFFERENT_PASSWORD(mapOf(LanguageEnum.EN to "You have entered different passwords. Please try again.", LanguageEnum.RU to "Вы ввели разные пароли. Попробуйте ввести ещё раз.")),
    EMPTY_LOGIN(mapOf(LanguageEnum.EN to "Empty username!", LanguageEnum.RU to "Пустой логин!")),
    EMPTY_PASSWORD(mapOf(LanguageEnum.EN to "Empty password!", LanguageEnum.RU to "Пустой пароль!")),
    ENTER_INTEGER(mapOf(LanguageEnum.EN to "Enter an integer", LanguageEnum.RU to "Введите целое число")),
    ENTER_NEW_PASSWORD(mapOf(LanguageEnum.EN to "Enter a new password", LanguageEnum.RU to "Введите новый пароль")),
    ENTER_SHORT_LINK_LIFETIME(mapOf(LanguageEnum.EN to "Enter the short link lifetime [hours]", LanguageEnum.RU to "Введите время жизни короткой ссылки [час]")),
    FILE_SELECT(mapOf(LanguageEnum.EN to "Select file", LanguageEnum.RU to "Выбор файла")),
    FULL_NAME(mapOf(LanguageEnum.EN to "Full name", LanguageEnum.RU to "Полное имя")),
    INCORRECT_LOGIN_OR_PASSWORD(mapOf(LanguageEnum.EN to "Incorrect username or password!", LanguageEnum.RU to "Неправильное имя или пароль!")),
    LANG(mapOf(LanguageEnum.EN to "Language", LanguageEnum.RU to "Язык")),
    LOGIN(mapOf(LanguageEnum.EN to "Login", LanguageEnum.RU to "Логин")),
    LOGON(mapOf(LanguageEnum.EN to "Login", LanguageEnum.RU to "Вход")),
    //--- используется в дочерних проектах
    LOGOUT(mapOf(LanguageEnum.EN to "Logout", LanguageEnum.RU to "Выход из системы")),
    NEW_PASSWORD(mapOf(LanguageEnum.EN to "New password", LanguageEnum.RU to "Новый пароль")),
    NEW_URL_COPIED_TO_CLIPBOARD(mapOf(LanguageEnum.EN to "The new link has been copied to the clipboard", LanguageEnum.RU to "Новая ссылка скопирована в буфер обмена")),
    NONE(mapOf(LanguageEnum.EN to "none", LanguageEnum.RU to "нет")),
    OK(mapOf(LanguageEnum.EN to "OK", LanguageEnum.RU to "OK")),
    OPEN(mapOf(LanguageEnum.EN to "Open", LanguageEnum.RU to "Открыть")),
    PASSWORD(mapOf(LanguageEnum.EN to "Password", LanguageEnum.RU to "Пароль")),
    PASSWORD_CHANGED_SUCCESSFULLY(mapOf(LanguageEnum.EN to "Password changed successfully.", LanguageEnum.RU to "Пароль успешно сменён.")),
    PREPARED(mapOf(LanguageEnum.EN to "Prepared", LanguageEnum.RU to "Подготовлено")),
    PRINT(mapOf(LanguageEnum.EN to "Print", LanguageEnum.RU to "Печать")),
    REFRESH(mapOf(LanguageEnum.EN to "Refresh", LanguageEnum.RU to "Обновить")),
    REMEMBER_ME(mapOf(LanguageEnum.EN to "Remember me", LanguageEnum.RU to "Запомнить меня")),
    SAVE(mapOf(LanguageEnum.EN to "Save", LanguageEnum.RU to "Сохранить")),
    SEARCH(mapOf(LanguageEnum.EN to "Search", LanguageEnum.RU to "Поиск")),
    SELECT(mapOf(LanguageEnum.EN to "Select", LanguageEnum.RU to "Выбор")),
    SYSTEM(mapOf(LanguageEnum.EN to "System", LanguageEnum.RU to "Система")),
    TIME_OFFSET(mapOf(LanguageEnum.EN to "Time offset [sec]", LanguageEnum.RU to "Сдвиг часового пояса [сек]")),
    TOO_SHORT_PASSWORD(mapOf(LanguageEnum.EN to "Your password is too short. Please try again.", LanguageEnum.RU to "Слишком короткий пароль. Попробуйте ввести ещё раз.")),
    UNKNOWN_MODULE(mapOf(LanguageEnum.EN to "unknown module", LanguageEnum.RU to "неизвестный модуль")),
    UNKNOWN_VALUE(mapOf(LanguageEnum.EN to "unknown value", LanguageEnum.RU to "неизвестное значение")),
    USER(mapOf(LanguageEnum.EN to "User", LanguageEnum.RU to "Пользователь")),
    USER_BLOCKED_BY_ADMIN(mapOf(LanguageEnum.EN to "The user has been blocked by the administrator.", LanguageEnum.RU to "Пользователь заблокирован администратором.")),
    USER_BLOCKED_BY_SYSTEM(mapOf(LanguageEnum.EN to "The user has been blocked by the system.", LanguageEnum.RU to "Пользователь заблокирован системой.")),

    ____(mapOf(LanguageEnum.EN to "____", LanguageEnum.RU to "____")),
}
