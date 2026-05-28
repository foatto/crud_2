package foatto.core.i18n

fun getLocalizedMessage(message: LocalizedMessages, lang: LanguageEnum): String =
    message.descr[lang] ?: message.descr[LanguageEnum.RU] ?: "(not defined: '$lang'='${message.name}')"

fun getLocalizedMessage(messages: Map<LanguageEnum, String>, lang: LanguageEnum): String =
    messages[lang] ?: messages[LanguageEnum.RU] ?: "(not defined: '$lang'='${messages.entries.joinToString()}')"

enum class LocalizedMessages(
    val descr: Map<LanguageEnum, String>
) {
    ERROR_LOGIN_NOT_ENTERED(
        mapOf(
            LanguageEnum.EN to "Login not entered",
            LanguageEnum.RU to "Не введён логин",
            LanguageEnum.KZ to "Кіру енгізілмеді",
        )
    ),
    ERROR_FULL_NAME_NOT_ENTERED(
        mapOf(
            LanguageEnum.EN to "Full name not entered",
            LanguageEnum.RU to "Не введёно полное имя",
            LanguageEnum.KZ to "Толық аты-жөні енгізілмеген",
        )
    ),
    ERROR_SUCH_LOGIN_ALREADY_EXISTS(
        mapOf(
            LanguageEnum.EN to "Such login already exists",
            LanguageEnum.RU to "Такой логин уже существует",
            LanguageEnum.KZ to "Бұл логин бұрыннан бар",
        )
    ),
    ERROR_PASSWORD_NOT_ENTERED(
        mapOf(
            LanguageEnum.EN to "Password not entered",
            LanguageEnum.RU to "Не введён пароль",
            LanguageEnum.KZ to "Құпия сөз енгізілмеді",
        )
    ),

    ADD(
        mapOf(
            LanguageEnum.EN to "Add",
            LanguageEnum.RU to "Добавить",
            LanguageEnum.KZ to "Қосу",
        )
    ),

    //--- используется в дочерних проектах
    ADDITIONAL(
        mapOf(
            LanguageEnum.EN to "Additional",
            LanguageEnum.RU to "Дополнительно",
            LanguageEnum.KZ to "Қосымша",
        )
    ),
    AGAIN(
        mapOf(
            LanguageEnum.EN to "Again",
            LanguageEnum.RU to "Ещё раз",
            LanguageEnum.KZ to "Тағы да",
        )
    ),
    AUTHORIZATION(
        mapOf(
            LanguageEnum.EN to "Login",
            LanguageEnum.RU to "Авторизация",
            LanguageEnum.KZ to "Авторизация",
        )
    ),
    BLOCKED(
        mapOf(
            LanguageEnum.EN to "Blocked",
            LanguageEnum.RU to "Заблокирован",
            LanguageEnum.KZ to "Блокталған",
        )
    ),
    BLOCK_TYPE_NOT_SPECIFIED(
        mapOf(
            LanguageEnum.EN to "Block type not specified",
            LanguageEnum.RU to "Не задан тип блока",
            LanguageEnum.KZ to "Блок түрі көрсетілмеген",
        )
    ),
    CANCEL(
        mapOf(
            LanguageEnum.EN to "Cancel",
            LanguageEnum.RU to "Отмена",
            LanguageEnum.KZ to "Бас тарту",
        )
    ),

    //--- используется в дочерних проектах
    CHANGE_PASSWORD(
        mapOf(
            LanguageEnum.EN to "Change password",
            LanguageEnum.RU to "Сменить пароль",
            LanguageEnum.KZ to "Құпия сөзді өзгерту",
        )
    ),

    //    CLOSE(mapOf(LanguageEnum.EN to "Close", LanguageEnum.RU to "Закрыть", LanguageEnum.KZ to "Жабу")),
    CONTACT_INFO(
        mapOf(
            LanguageEnum.EN to "Contact info",
            LanguageEnum.RU to "Контактная информация",
            LanguageEnum.KZ to "Байланыс ақпараты",
        )
    ),
    DATE_TIME_OF_LAST_LOGIN(
        mapOf(
            LanguageEnum.EN to "Date/time of last login",
            LanguageEnum.RU to "Дата/время последнего входа",
            LanguageEnum.KZ to "Соңғы кіру күні/уақыты",
        )
    ),
    DATE_TIME_OF_LAST_PASSWORD_CHANGE(
        mapOf(
            LanguageEnum.EN to "Date/time of last password change",
            LanguageEnum.RU to "Дата/время последней смены пароля",
            LanguageEnum.KZ to "Құпия сөздің соңғы өзгертілген күні/уақыты",
        )
    ),

    //    DELETE(mapOf(LanguageEnum.EN to "Delete", LanguageEnum.RU to "Удалить", LanguageEnum.KZ to "Жою")),
    DEPARTMENT(
        mapOf(
            LanguageEnum.EN to "Department",
            LanguageEnum.RU to "Подразделение",
            LanguageEnum.KZ to "Бөлімше",
        )
    ),
    DIFFERENT_PASSWORD(
        mapOf(
            LanguageEnum.EN to "You have entered different passwords. Please try again.",
            LanguageEnum.RU to "Вы ввели разные пароли. Попробуйте ввести ещё раз.",
            LanguageEnum.KZ to "Сіз басқа құпия сөздерді енгіздіңіз. Қайталап көріңіз.",
        )
    ),
    EMPLOYEE(
        mapOf(
            LanguageEnum.EN to "Employee",
            LanguageEnum.RU to "Работник",
            LanguageEnum.KZ to "Жұмысшы",
        )
    ),
    EMPTY_LOGIN(
        mapOf(
            LanguageEnum.EN to "Empty username!",
            LanguageEnum.RU to "Пустой логин!",
            LanguageEnum.KZ to "Бос кіру!",
        )
    ),
    EMPTY_PASSWORD(
        mapOf(
            LanguageEnum.EN to "Empty password!",
            LanguageEnum.RU to "Пустой пароль!",
            LanguageEnum.KZ to "Бос құпия сөз!",
        )
    ),
    ENTER_INTEGER(
        mapOf(
            LanguageEnum.EN to "Enter an integer",
            LanguageEnum.RU to "Введите целое число",
            LanguageEnum.KZ to "Бүтін санды енгізіңіз",
        )
    ),
    ENTER_MESSAGE(
        mapOf(
            LanguageEnum.EN to "Enter a message text",
            LanguageEnum.RU to "Введите текст сообщения",
            LanguageEnum.KZ to "Хабарлама мәтінін енгізіңіз",
        )
    ),
    ENTER_NEW_PASSWORD(
        mapOf(
            LanguageEnum.EN to "Enter a new password",
            LanguageEnum.RU to "Введите новый пароль",
            LanguageEnum.KZ to "Жаңа құпия сөзді енгізіңіз",
        )
    ),
    ENTER_SHORT_LINK_LIFETIME(
        mapOf(
            LanguageEnum.EN to "Enter the short link lifetime [hours]",
            LanguageEnum.RU to "Введите время жизни короткой ссылки [час]",
            LanguageEnum.KZ to "Қысқа сілтеменің өмір сүру ұзақтығын енгізіңіз [сағат]",
        )
    ),
    FILES(
        mapOf(
            LanguageEnum.EN to "Files",
            LanguageEnum.RU to "Файлы",
            LanguageEnum.KZ to "Файлдар",
        )
    ),
    FILE_SELECT(
        mapOf(
            LanguageEnum.EN to "Select file",
            LanguageEnum.RU to "Выбор файла",
            LanguageEnum.KZ to "Файлды таңдау",
        )
    ),
    FRACTIONAL_SEPARATOR(
        mapOf(
            LanguageEnum.EN to "Fractional separator",
            LanguageEnum.RU to "Разделитель дробной части",
            LanguageEnum.KZ to "Бөлшек бөлгіш",
        )
    ),
    FULL_NAME(
        mapOf(
            LanguageEnum.EN to "Full name",
            LanguageEnum.RU to "Полное имя",
            LanguageEnum.KZ to "Толық аты-жөні",
        )
    ),
    INCORRECT_LOGIN_OR_PASSWORD(
        mapOf(
            LanguageEnum.EN to "Incorrect username or password!",
            LanguageEnum.RU to "Неправильное имя или пароль!",
            LanguageEnum.KZ to "Дұрыс емес пайдаланушы аты немесе құпия сөз!",
        )
    ),
    LANG(
        mapOf(
            LanguageEnum.EN to "Language",
            LanguageEnum.RU to "Язык",
            LanguageEnum.KZ to "Тіл",
        )
    ),
    LOGIN(
        mapOf(
            LanguageEnum.EN to "Login",
            LanguageEnum.RU to "Логин",
            LanguageEnum.KZ to "Кіру",
        )
    ),
    LOGON(
        mapOf(
            LanguageEnum.EN to "Login",
            LanguageEnum.RU to "Вход",
            LanguageEnum.KZ to "Кіру",
        )
    ),
    LOGOUT(
        mapOf(
            LanguageEnum.EN to "Logout",
            LanguageEnum.RU to "Выход из системы",
            LanguageEnum.KZ to "Шығу",
        )
    ),
    MANAGER(
        mapOf(
            LanguageEnum.EN to "Manager",
            LanguageEnum.RU to "Руководитель",
            LanguageEnum.KZ to "Супервайзер",
        )
    ),
    MANUAL_BLOCK_PLACEMENT_CLEARED(
        mapOf(
            LanguageEnum.EN to "Manual block placement has been cleared",
            LanguageEnum.RU to "Ручное расположение блоков очищено",
            LanguageEnum.KZ to "Блокты қолмен орналастыру тазартылды",
        )
    ),
    MANUAL_BLOCK_PLACEMENT_SAVED(
        mapOf(
            LanguageEnum.EN to "Manual block placement is preserved",
            LanguageEnum.RU to "Ручное расположение блоков сохранено",
            LanguageEnum.KZ to "Блокты қолмен орналастыру сақталған",
        )
    ),
    MESSAGE(
        mapOf(
            LanguageEnum.EN to "Message",
            LanguageEnum.RU to "Сообщение",
            LanguageEnum.KZ to "Хабарлама",
        )
    ),
    NEW_PASSWORD(
        mapOf(
            LanguageEnum.EN to "New password",
            LanguageEnum.RU to "Новый пароль",
            LanguageEnum.KZ to "Жаңа құпия сөз",
        )
    ),
    NEW_URL_COPIED_TO_CLIPBOARD(
        mapOf(
            LanguageEnum.EN to "The new link has been copied to the clipboard",
            LanguageEnum.RU to "Новая ссылка скопирована в буфер обмена",
            LanguageEnum.KZ to "Жаңа сілтеме алмасу буферіне көшірілді",
        )
    ),
    NONE(
        mapOf(
            LanguageEnum.EN to "none",
            LanguageEnum.RU to "нет",
            LanguageEnum.KZ to "жоқ",
        )
    ),
    NUMBER_OF_LOGIN_ATTEMPTS(
        mapOf(
            LanguageEnum.EN to "Number of login attempts",
            LanguageEnum.RU to "Кол-во попыток входа",
            LanguageEnum.KZ to "Жүйеге кіру әрекеттерінің саны",
        )
    ),
    OK(
        mapOf(
            LanguageEnum.EN to "OK",
            LanguageEnum.RU to "OK",
            LanguageEnum.KZ to "OK",
        )
    ),
    OPEN(
        mapOf(
            LanguageEnum.EN to "Open",
            LanguageEnum.RU to "Открыть",
            LanguageEnum.KZ to "Ашық",
        )
    ),
    OWNER(
        mapOf(
            LanguageEnum.EN to "Owner",
            LanguageEnum.RU to "Владелец",
            LanguageEnum.KZ to "Иесі",
        )
    ),
    PASSWORD(
        mapOf(
            LanguageEnum.EN to "Password",
            LanguageEnum.RU to "Пароль",
            LanguageEnum.KZ to "Құпия сөз",
        )
    ),
    PASSWORD_CHANGED_SUCCESSFULLY(
        mapOf(
            LanguageEnum.EN to "Password changed successfully",
            LanguageEnum.RU to "Пароль успешно сменён",
            LanguageEnum.KZ to "Құпия сөз сәтті өзгертілді",
        )
    ),
    PREPARED(
        mapOf(
            LanguageEnum.EN to "Prepared",
            LanguageEnum.RU to "Подготовлено",
            LanguageEnum.KZ to "Дайындалған",
        )
    ),
    PRINT(
        mapOf(
            LanguageEnum.EN to "Print",
            LanguageEnum.RU to "Печать",
            LanguageEnum.KZ to "Басып шығару",
        )
    ),
    REFRESH(
        mapOf(
            LanguageEnum.EN to "Refresh",
            LanguageEnum.RU to "Обновить",
            LanguageEnum.KZ to "Жаңарту",
        )
    ),
    REMEMBER_ME(
        mapOf(
            LanguageEnum.EN to "Remember me",
            LanguageEnum.RU to "Запомнить меня",
            LanguageEnum.KZ to "Мені ұмытпаңыз",
        )
    ),
    ROLE(
        mapOf(
            LanguageEnum.EN to "Role",
            LanguageEnum.RU to "Роль",
            LanguageEnum.KZ to "Рөл",
        )
    ),
    ROLES(
        mapOf(
            LanguageEnum.EN to "Roles",
            LanguageEnum.RU to "Роли",
            LanguageEnum.KZ to "Рөлдер",
        )
    ),
    SAVE(
        mapOf(
            LanguageEnum.EN to "Save",
            LanguageEnum.RU to "Сохранить",
            LanguageEnum.KZ to "Сақтау",
        )
    ),
    SEARCH(
        mapOf(
            LanguageEnum.EN to "Search",
            LanguageEnum.RU to "Поиск",
            LanguageEnum.KZ to "Іздеу",
        )
    ),
    SELECT(
        mapOf(
            LanguageEnum.EN to "Select",
            LanguageEnum.RU to "Выбор",
            LanguageEnum.KZ to "Таңдау",
        )
    ),
    SEND_MESSAGE(
        mapOf(
            LanguageEnum.EN to "Send message",
            LanguageEnum.RU to "Отправить сообщение",
            LanguageEnum.KZ to "Хабарлама жіберу",
        )
    ),
    SEPARATE_THOUSANDS_WITH_SPACES(
        mapOf(
            LanguageEnum.EN to "Separate thousands with spaces",
            LanguageEnum.RU to "Разделять тысячи пробелами",
            LanguageEnum.KZ to "Мыңдықтарды бос орындармен бөліңіз",
        )
    ),
    SHORT_NAME(
        mapOf(
            LanguageEnum.EN to "Short name",
            LanguageEnum.RU to "Короткое имя",
            LanguageEnum.KZ to "Қысқа атауы",
        )
    ),
    SYSTEM(
        mapOf(
            LanguageEnum.EN to "System",
            LanguageEnum.RU to "Система",
            LanguageEnum.KZ to "Жүйе",
        )
    ),
    THIS_FULL_NAME_ALREADY_EXISTS(
        mapOf(
            LanguageEnum.EN to "This full name already exists",
            LanguageEnum.RU to "Такое полное имя уже существует",
            LanguageEnum.KZ to "Бұл толық атау бұрыннан бар",
        )
    ),
    TIME_OFFSET(
        mapOf(
            LanguageEnum.EN to "Time offset",
            LanguageEnum.RU to "Сдвиг часового пояса",
            LanguageEnum.KZ to "Уақыт белдеуінің ауысуы",
        )
    ),
    TOO_SHORT_PASSWORD(
        mapOf(
            LanguageEnum.EN to "Your password is too short. Please try again.",
            LanguageEnum.RU to "Слишком короткий пароль. Попробуйте ввести ещё раз.",
            LanguageEnum.KZ to "Құпия сөзіңіз тым қысқа. Қайталап көріңіз.",
        )
    ),
    TYPE(
        mapOf(
            LanguageEnum.EN to "Type",
            LanguageEnum.RU to "Тип",
            LanguageEnum.KZ to "Түрі",
        )
    ),
    UNKNOWN(
        mapOf(
            LanguageEnum.EN to "(unknown)",
            LanguageEnum.RU to "(неизвестно)",
            LanguageEnum.KZ to "(белгісіз)",
        )
    ),
    UNKNOWN_MODULE(
        mapOf(
            LanguageEnum.EN to "unknown module",
            LanguageEnum.RU to "неизвестный модуль",
            LanguageEnum.KZ to "белгісіз модуль",
        )
    ),
    UNKNOWN_MODULE_TYPE(
        mapOf(
            LanguageEnum.EN to "unknown module type",
            LanguageEnum.RU to "неизвестный тип модуля",
            LanguageEnum.KZ to "белгісіз модуль түрі",
        )
    ),
    UNKNOWN_USER(
        mapOf(
            LanguageEnum.EN to "(unknown user)",
            LanguageEnum.RU to "(неизвестный пользователь)",
            LanguageEnum.KZ to "(белгісіз пайдаланушы)",
        )
    ),
    UNKNOWN_VALUE(
        mapOf(
            LanguageEnum.EN to "unknown value",
            LanguageEnum.RU to "неизвестное значение",
            LanguageEnum.KZ to "белгісіз мағына",
        )
    ),
    UPPER_DIVISION(
        mapOf(
            LanguageEnum.EN to "Upper division",
            LanguageEnum.RU to "Верхнее подразделение",
            LanguageEnum.KZ to "Жоғарғы дивизион",
        )
    ),
    USER(
        mapOf(
            LanguageEnum.EN to "User",
            LanguageEnum.RU to "Пользователь",
            LanguageEnum.KZ to "Пайдаланушы",
        )
    ),
    USER_BLOCKED_BY_ADMIN(
        mapOf(
            LanguageEnum.EN to "The user has been blocked by the administrator",
            LanguageEnum.RU to "Пользователь заблокирован администратором",
            LanguageEnum.KZ to "Пайдаланушы әкімші тарапынан бұғатталған",
        )
    ),
    USER_BLOCKED_BY_SYSTEM(
        mapOf(
            LanguageEnum.EN to "The user has been blocked by the system",
            LanguageEnum.RU to "Пользователь заблокирован системой",
            LanguageEnum.KZ to "Пайдаланушы жүйемен бұғатталған",
        )
    ),

    ____(
        mapOf(
            LanguageEnum.EN to "____",
            LanguageEnum.RU to "____",
            LanguageEnum.KZ to "____"
        )
    ),
}
