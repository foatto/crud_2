package foatto.core_mms.i18n

import foatto.core.i18n.LanguageEnum

fun getLocalizedMMSMessage(message: LocalizedMMSMessages, lang: LanguageEnum): String =
    message.descr[lang] ?: message.descr[LanguageEnum.RU] ?: "(not defined: '$lang'='${message.name}')"

enum class LocalizedMMSMessages(
    val descr: Map<LanguageEnum, String>
) {
    TITLE(
        mapOf(
            LanguageEnum.EN to "Control system for technological equipment and transport",
            LanguageEnum.RU to "Система контроля технологического оборудования и транспорта",
            LanguageEnum.KZ to "Технологиялық жабдықтар мен көлікті басқару жүйесі",
        )
    ),

    OBJECT_TYPE_MOBILE(
        mapOf(
            LanguageEnum.EN to "Mobile",
            LanguageEnum.RU to "Мобильный",
            LanguageEnum.KZ to "Мобильді",
        )
    ),
    OBJECT_TYPE_STATIONARY(
        mapOf(
            LanguageEnum.EN to "Stationary",
            LanguageEnum.RU to "Стационарный",
            LanguageEnum.KZ to "Стационарлық",
        )
    ),

    //--- module desriptions
    CONTROL(
        mapOf(
            LanguageEnum.EN to "Control",
            LanguageEnum.RU to "Контроль",
            LanguageEnum.KZ to "Басқару",
        )
    ),
    DEVICES(
        mapOf(
            LanguageEnum.EN to "Devices",
            LanguageEnum.RU to "Контроллеры",
            LanguageEnum.KZ to "Контроллерлер",
        )
    ),
    OBJECTS(
        mapOf(
            LanguageEnum.EN to "Objects",
            LanguageEnum.RU to "Объекты",
            LanguageEnum.KZ to "Нысандар",
        )
    ),
    REFERENCES(
        mapOf(
            LanguageEnum.EN to "Reference books",
            LanguageEnum.RU to "Справочники",
            LanguageEnum.KZ to "Анықтамалық әдебиеттер",
        )
    ),
    REPORTS(
        mapOf(
            LanguageEnum.EN to "Reports",
            LanguageEnum.RU to "Отчёты",
            LanguageEnum.KZ to "Есептер",
        )
    ),
    WORK_LOGS(
        mapOf(
            LanguageEnum.EN to "Work logs",
            LanguageEnum.RU to "Рабочие журналы",
            LanguageEnum.KZ to "Жұмыс күнделіктері",
        )
    ),

    ABSOLUTE_MILEAGE(
        mapOf(
            LanguageEnum.EN to "Absolute mileage",
            LanguageEnum.RU to "Абсолютный пробег",
            LanguageEnum.KZ to "Абсолютті жүріс",
        )
    ),
    ACCUMULATIVE_METER(
        mapOf(
            LanguageEnum.EN to "Accumulative meter",
            LanguageEnum.RU to "Накопительный счётчик",
            LanguageEnum.KZ to "Жинақтаушы есептегіш",
        )
    ),
    AUTOMATIC_CREATION_OF_WORK_SHIFTS(
        mapOf(
            LanguageEnum.EN to "Automatic creation of work shifts",
            LanguageEnum.RU to "Автосоздание рабочих смен",
            LanguageEnum.KZ to "Жұмыс ауысымдарын автоматты түрде құру",
        )
    ),
    AUTO_CREATE_SENSORS(
        mapOf(
            LanguageEnum.EN to "Auto-create sensors",
            LanguageEnum.RU to "Автосоздание датчиков",
            LanguageEnum.KZ to "Сенсорларды автоматты түрде жасау",
        )
    ),
    BY_DATES(
        mapOf(
            LanguageEnum.EN to "By dates",
            LanguageEnum.RU to "По датам",
            LanguageEnum.KZ to "Күні бойынша",
        )
    ),
    BY_OBJECTS(
        mapOf(
            LanguageEnum.EN to "By objects",
            LanguageEnum.RU to "По объектам",
            LanguageEnum.KZ to "Объектілер бойынша",
        )
    ),
    CHARTS(
        mapOf(
            LanguageEnum.EN to "Charts",
            LanguageEnum.RU to "Графики",
            LanguageEnum.KZ to "Диаграммалар",
        )
    ),
    COMMAND(
        mapOf(
            LanguageEnum.EN to "Command",
            LanguageEnum.RU to "Команда",
            LanguageEnum.KZ to "Команда",
        )
    ),
    CONTROLLER_NAME(
        mapOf(
            LanguageEnum.EN to "Controller name",
            LanguageEnum.RU to "Наименование контроллера",
            LanguageEnum.KZ to "Контроллердің атауы",
        )
    ),
    CONTROLLER_SERIAL_NUMBER(
        mapOf(
            LanguageEnum.EN to "Controller serial number",
            LanguageEnum.RU to "Серийный номер контроллера",
            LanguageEnum.KZ to "Контроллердің сериялық нөмірі",
        )
    ),
    COPY_SENSORS(
        mapOf(
            LanguageEnum.EN to "Copy sensors when changing an object",
            LanguageEnum.RU to "Копировать датчики при смене объекта",
            LanguageEnum.KZ to "Нысанды өзгерткен кезде сенсорларды көшіру",
        )
    ),
    CREATION_TIME(
        mapOf(
            LanguageEnum.EN to "Creation Time",
            LanguageEnum.RU to "Время создания",
            LanguageEnum.KZ to "Жасалу уақыты",
        )
    ),
    DASHBOARDS(
        mapOf(
            LanguageEnum.EN to "Dashboards",
            LanguageEnum.RU to "Контрольные панели",
            LanguageEnum.KZ to "Басқару панельдері",
        )
    ),
    DATE(
        mapOf(
            LanguageEnum.EN to "Date",
            LanguageEnum.RU to "Дата",
            LanguageEnum.KZ to "Күні",
        )
    ),
    DENSITY(
        mapOf(
            LanguageEnum.EN to "Density",
            LanguageEnum.RU to "Плотность",
            LanguageEnum.KZ to "Тығыздық",
        )
    ),
    DEPARTMENT(
        mapOf(
            LanguageEnum.EN to "Subdivision",
            LanguageEnum.RU to "Подразделение",
            LanguageEnum.KZ to "Бөлімше",
        )
    ),
    DESCRIPTION(
        mapOf(
            LanguageEnum.EN to "Description",
            LanguageEnum.RU to "Описание",
            LanguageEnum.KZ to "Сипаттама",
        )
    ),
    EDIT_TIME(
        mapOf(
            LanguageEnum.EN to "Edit Time",
            LanguageEnum.RU to "Время редактирования",
            LanguageEnum.KZ to "Өңдеу уақыты",
        )
    ),
    ELECTRICITY(
        mapOf(
            LanguageEnum.EN to "Electricity",
            LanguageEnum.RU to "Э/энергия",
            LanguageEnum.KZ to "Электр қуаты",
        )
    ),
    ELECTRICITY_METERS(
        mapOf(
            LanguageEnum.EN to "Electricity Meters",
            LanguageEnum.RU to "Э/счётчики",
            LanguageEnum.KZ to "Электр есептегіштері",
        )
    ),
    END_OF_PERIOD(
        mapOf(
            LanguageEnum.EN to "End of Period",
            LanguageEnum.RU to "Конец периода",
            LanguageEnum.KZ to "Кезеңнің соңы",
        )
    ),
    EQUIPMENT(
        mapOf(
            LanguageEnum.EN to "Equipment",
            LanguageEnum.RU to "Оборудование",
            LanguageEnum.KZ to "Жабдық",
        )
    ),
    FILE(
        mapOf(
            LanguageEnum.EN to "File",
            LanguageEnum.RU to "Файл",
            LanguageEnum.KZ to "Файл",
        )
    ),
    FILE_TO_SEND(
        mapOf(
            LanguageEnum.EN to "File to send",
            LanguageEnum.RU to "Файл для отправки",
            LanguageEnum.KZ to "Жіберілетін файл",
        )
    ),
    FINAL_DENSITY(
        mapOf(
            LanguageEnum.EN to "Final density",
            LanguageEnum.RU to "Плотность конечная",
            LanguageEnum.KZ to "Соңғы тығыздық",
        )
    ),
    FINAL_LEVEL(
        mapOf(
            LanguageEnum.EN to "Final level",
            LanguageEnum.RU to "Уровень конечный",
            LanguageEnum.KZ to "Соңғы деңгей",
        )
    ),
    FINAL_TEMPERATURE(
        mapOf(
            LanguageEnum.EN to "Final temperature",
            LanguageEnum.RU to "Температура конечная",
            LanguageEnum.KZ to "Соңғы температура",
        )
    ),
    FIRMWARE_VERSION(
        mapOf(
            LanguageEnum.EN to "Firmware Version",
            LanguageEnum.RU to "Версия прошивки",
            LanguageEnum.KZ to "Микробағдарлама нұсқасы",
        )
    ),
    FLOW(
        mapOf(
            LanguageEnum.EN to "Flow",
            LanguageEnum.RU to "Расход",
            LanguageEnum.KZ to "Тұтыну",
        )
    ),
    FLOW_GENERATION(
        mapOf(
            LanguageEnum.EN to "Flow/Generation",
            LanguageEnum.RU to "Расход/Генерация",
            LanguageEnum.KZ to "Тұтыну/Өндіріс",
        )
    ),
    FOR_THE_LAST(
        mapOf(
            LanguageEnum.EN to "for the last",
            LanguageEnum.RU to "за последние",
            LanguageEnum.KZ to "соңғысына",
        )
    ),
    FOR_THE_PERIOD_FROM(
        mapOf(
            LanguageEnum.EN to "for the period from",
            LanguageEnum.RU to "за период с",
            LanguageEnum.KZ to "бастап кезең үшін",
        )
    ),
    FUEL_CONSUMPTION(
        mapOf(
            LanguageEnum.EN to "Fuel Consumption",
            LanguageEnum.RU to "Расход топлива",
            LanguageEnum.KZ to "Отын шығыны",
        )
    ),
    FUEL_LEVEL(
        mapOf(
            LanguageEnum.EN to "Fuel Level",
            LanguageEnum.RU to "Уровень топлива",
            LanguageEnum.KZ to "Отын деңгейі",
        )
    ),
    FUEL_METERS(
        mapOf(
            LanguageEnum.EN to "Fuel Meters",
            LanguageEnum.RU to "Счётчики топлива",
            LanguageEnum.KZ to "Отын өлшегіштері",
        )
    ),
    GEOFENCE(
        mapOf(
            LanguageEnum.EN to "Geofence",
            LanguageEnum.RU to "Геозона",
            LanguageEnum.KZ to "Геоқоршау",
        )
    ),
    GROUP(
        mapOf(
            LanguageEnum.EN to "Group",
            LanguageEnum.RU to "Группа",
            LanguageEnum.KZ to "Топ",
        )
    ),
    GROUPING(
        mapOf(
            LanguageEnum.EN to "Grouping",
            LanguageEnum.RU to "Группировка",
            LanguageEnum.KZ to "Топтастыру",
        )
    ),
    HOUR_S(
        mapOf(
            LanguageEnum.EN to "hour(s)",
            LanguageEnum.RU to "час(а,ов)",
            LanguageEnum.KZ to "сағат(тар)",
        )
    ),
    IDLE_LIMIT(
        mapOf(
            LanguageEnum.EN to "Idle limit",
            LanguageEnum.RU to "Граница холостого хода",
            LanguageEnum.KZ to "Бос жүрістегі жылдамдық шегі",
        )
    ),
    IGNORE_SENSOR_READINGS_GREATER_THAN(
        mapOf(
            LanguageEnum.EN to "Ignore sensor readings greater than",
            LanguageEnum.RU to "Игнорировать показания датчика более чем",
            LanguageEnum.KZ to "Сенсор көрсеткіштерін келесіден артық елемеңіз",
        )
    ),
    IGNORE_SENSOR_READINGS_LESS_THAN(
        mapOf(
            LanguageEnum.EN to "Ignore sensor readings less than",
            LanguageEnum.RU to "Игнорировать показания датчика менее чем",
            LanguageEnum.KZ to "Сенсор көрсеткіштерін ескермеу",
        )
    ),
    INCOMING_FILLING_METER(
        mapOf(
            LanguageEnum.EN to "Incoming/filling meter",
            LanguageEnum.RU to "Входящий/заправочный счётчик",
            LanguageEnum.KZ to "Кіріс/жанармай құю өлшегіші",
        )
    ),
    INCOME(
        mapOf(
            LanguageEnum.EN to "Income",
            LanguageEnum.RU to "Приход",
            LanguageEnum.KZ to "Келуі",
        )
    ),
    INCOMING_METERS(
        mapOf(
            LanguageEnum.EN to "Incoming meters [units]",
            LanguageEnum.RU to "Входящие счётчики [ед.изм.]",
            LanguageEnum.KZ to "Кіріс есептегіштері [бірлік]",
        )
    ),
    INDEX_MUST_BE_BETWEEN_0_AND(
        mapOf(
            LanguageEnum.EN to "Index must be between 0 and",
            LanguageEnum.RU to "Индекс должен быть в диапазоне от 0 до",
            LanguageEnum.KZ to "Индекс 0 мен аралықта болуы керек",
        )
    ),
    INDEX_NOT_ENTERED(
        mapOf(
            LanguageEnum.EN to "Index not entered",
            LanguageEnum.RU to "Не введён индекс",
            LanguageEnum.KZ to "Индекс енгізілмеген",
        )
    ),
    INDEX_ON_OBJECT(
        mapOf(
            LanguageEnum.EN to "Index on the object",
            LanguageEnum.RU to "Индекс на объекте",
            LanguageEnum.KZ to "Нысан бойынша индекс",
        )
    ),
    INFORMATION(
        mapOf(
            LanguageEnum.EN to "Information",
            LanguageEnum.RU to "Информация",
            LanguageEnum.KZ to "Ақпарат",
        )
    ),
    INITIAL_DENSITY(
        mapOf(
            LanguageEnum.EN to "Initial density",
            LanguageEnum.RU to "Плотность начальная",
            LanguageEnum.KZ to "Бастапқы тығыздық",
        )
    ),
    INITIAL_LEVEL(
        mapOf(
            LanguageEnum.EN to "Initial level",
            LanguageEnum.RU to "Уровень начальный",
            LanguageEnum.KZ to "Бастауыш деңгей",
        )
    ),
    INITIAL_TEMPERATURE(
        mapOf(
            LanguageEnum.EN to "Initial temperature",
            LanguageEnum.RU to "Температура начальная",
            LanguageEnum.KZ to "Бастапқы температура",
        )
    ),
    INPUT_NUMBER(
        mapOf(
            LanguageEnum.EN to "Input Number",
            LanguageEnum.RU to "Номер входа",
            LanguageEnum.KZ to "Кіру нөмірі",
        )
    ),
    INTEGRATION_ID(
        mapOf(
            LanguageEnum.EN to "Integration ID",
            LanguageEnum.RU to "Интеграционный идентификатор",
            LanguageEnum.KZ to "Интеграция идентификаторы",
        )
    ),
    IS_MISSING_FROM_THE_SYSTEM(
        mapOf(
            LanguageEnum.EN to "is missing from the system",
            LanguageEnum.RU to "отсутствует в системе",
            LanguageEnum.KZ to "жүйеде жоқ",
        )
    ),
    LAST_SESSION_ERROR(
        mapOf(
            LanguageEnum.EN to "Last Session Error",
            LanguageEnum.RU to "Ошибка последней сессии",
            LanguageEnum.KZ to "Соңғы сессия қатесі",
        )
    ),
    LAST_SESSION_STATUS(
        mapOf(
            LanguageEnum.EN to "Last Session Status",
            LanguageEnum.RU to "Статус последней сессии",
            LanguageEnum.KZ to "Соңғы сессияның мәртебесі",
        )
    ),
    LAST_SESSION_TIME(
        mapOf(
            LanguageEnum.EN to "Last Session Time",
            LanguageEnum.RU to "Время последней сессии",
            LanguageEnum.KZ to "Соңғы сессия уақыты",
        )
    ),
    LOCKED(
        mapOf(
            LanguageEnum.EN to "Locked",
            LanguageEnum.RU to "Заблокирован",
            LanguageEnum.KZ to "Блокталған",
        )
    ),
    MAIN_TANK(
        mapOf(
            LanguageEnum.EN to "Main tank",
            LanguageEnum.RU to "Основная ёмкость",
            LanguageEnum.KZ to "Негізгі сыйымдылық",
        )
    ),
    MAPS(
        mapOf(
            LanguageEnum.EN to "Maps",
            LanguageEnum.RU to "Карты",
            LanguageEnum.KZ to "Карталар",
        )
    ),
    MAXIMUM_DISPLAY_VALUE(
        mapOf(
            LanguageEnum.EN to "Maximum display value",
            LanguageEnum.RU to "Максимальное отображаемое значение",
            LanguageEnum.KZ to "Максималды көрсету мәні",
        )
    ),
    MAXIMUM_OPERATING_VALUE(
        mapOf(
            LanguageEnum.EN to "Maximum operating value",
            LanguageEnum.RU to "Максимальное рабочее значение",
            LanguageEnum.KZ to "Максималды жұмыс мәні",
        )
    ),
    MEASURED_VALUE(
        mapOf(
            LanguageEnum.EN to "Measured value",
            LanguageEnum.RU to "Значение измеряемой величины",
            LanguageEnum.KZ to "Өлшенген шаманың мәні",
        )
    ),
    MEASURED_VALUE_NOT_ENTERED(
        mapOf(
            LanguageEnum.EN to "Measured value not entered",
            LanguageEnum.RU to "Не введёно значение измеряемой величины",
            LanguageEnum.KZ to "Өлшенген шаманың мәні енгізілмеген",
        )
    ),
    METERING_TYPE(
        mapOf(
            LanguageEnum.EN to "Metering type",
            LanguageEnum.RU to "Тип учёта",
            LanguageEnum.KZ to "Есеп түрі",
        )
    ),
    MILEAGE(
        mapOf(
            LanguageEnum.EN to "Mileage",
            LanguageEnum.RU to "Пробег",
            LanguageEnum.KZ to "Жүріп өткен жол",
        )
    ),
    MILEAGE_UNITS(
        mapOf(
            LanguageEnum.EN to "Mileage [km]",
            LanguageEnum.RU to "Пробег [км]",
            LanguageEnum.KZ to "Жүріп өткен жол [км]",
        )
    ),
    MINIMUM_DISPLAY_VALUE(
        mapOf(
            LanguageEnum.EN to "Minimum display value",
            LanguageEnum.RU to "Минимальное отображаемое значение",
            LanguageEnum.KZ to "Ең аз дисплей мәні",
        )
    ),
    MINIMUM_DRIVING_TIME(
        mapOf(
            LanguageEnum.EN to "Minimum driving time [sec]",
            LanguageEnum.RU to "Минимальное время движения [сек]",
            LanguageEnum.KZ to "Ең аз саяхат уақыты [сек]",
        )
    ),
    MINIMUM_IDLE_TIME_1(
        mapOf(
            LanguageEnum.EN to "Minimum idle time [sec]",
            LanguageEnum.RU to "Минимальное время простоя [сек]",
            LanguageEnum.KZ to "Ең аз тоқтап қалу уақыты [сек]",
        )
    ),
    MINIMUM_IDLE_TIME_2(
        mapOf(
            LanguageEnum.EN to "Minimum idle time [sec]",
            LanguageEnum.RU to "Минимальное время холостого хода [сек]",
            LanguageEnum.KZ to "Ең аз бос тұру уақыты [сек]",
        )
    ),
    MINIMUM_OPERATING_TIME(
        mapOf(
            LanguageEnum.EN to "Minimum operating time [sec]",
            LanguageEnum.RU to "Минимальное время работы [сек]",
            LanguageEnum.KZ to "Ең аз жұмыс уақыты [сек]",
        )
    ),
    MINIMUM_OPERATING_VALUE(
        mapOf(
            LanguageEnum.EN to "Minimum operating value",
            LanguageEnum.RU to "Минимальное рабочее значение",
            LanguageEnum.KZ to "Ең төменгі жұмыс мәні",
        )
    ),
    MINIMUM_OVERLOAD_TIME(
        mapOf(
            LanguageEnum.EN to "Minimum overload time [sec]",
            LanguageEnum.RU to "Минимальное время перегрузки [сек]",
            LanguageEnum.KZ to "Ең аз шамадан тыс жүктеме уақыты [сек]",
        )
    ),
    MINIMUM_PARKING_TIME(
        mapOf(
            LanguageEnum.EN to "Minimum parking time [sec]",
            LanguageEnum.RU to "Минимальное время стоянки [сек]",
            LanguageEnum.KZ to "Тұраққа қоюдың ең аз уақыты [сек]",
        )
    ),
    MINIMUM_SPEEDING_TIME(
        mapOf(
            LanguageEnum.EN to "Minimum speeding time [sec]",
            LanguageEnum.RU to "Минимальное время превышения скорости [сек]",
            LanguageEnum.KZ to "Жылдамдықты арттырудың ең аз уақыты [сек]",
        )
    ),
    MINUTES(
        mapOf(
            LanguageEnum.EN to "minutes",
            LanguageEnum.RU to "минут",
            LanguageEnum.KZ to "минуттар",
        )
    ),
    MOBILE_OPERATOR(
        mapOf(
            LanguageEnum.EN to "Mobile Operator",
            LanguageEnum.RU to "Мобильный оператор",
            LanguageEnum.KZ to "Ұялы байланыс операторы",
        )
    ),
    MODEL(
        mapOf(
            LanguageEnum.EN to "Model",
            LanguageEnum.RU to "Модель",
            LanguageEnum.KZ to "Модель",
        )
    ),
    NAME(
        mapOf(
            LanguageEnum.EN to "Name",
            LanguageEnum.RU to "Наименование",
            LanguageEnum.KZ to "Аты",
        )
    ),
    NAME_OF_MAIN_TANK(
        mapOf(
            LanguageEnum.EN to "Name of main tank [units]",
            LanguageEnum.RU to "Наименование основной ёмкости [ед.изм.]",
            LanguageEnum.KZ to "Негізгі сыйымдылықтың атауы [бірлік]",
        )
    ),
    NAME_OF_WORKING_FLOW_TANK(
        mapOf(
            LanguageEnum.EN to "Name of working/flow tank [units]",
            LanguageEnum.RU to "Наименование рабочей/расходной ёмкости [ед.изм.]",
            LanguageEnum.KZ to "Жұмыс/тұтыну қуатының атауы [бірлік]",
        )
    ),
    NAME_UNITS(
        mapOf(
            LanguageEnum.EN to "Name [units]",
            LanguageEnum.RU to "Наименование [ед.изм.]",
            LanguageEnum.KZ to "Аты-жөні [бірлік]",
        )
    ),
    NOTE(
        mapOf(
            LanguageEnum.EN to "Note",
            LanguageEnum.RU to "Примечание",
            LanguageEnum.KZ to "Ескерту",
        )
    ),
    NO_COMMAND_ENTERED(
        mapOf(
            LanguageEnum.EN to "No command entered",
            LanguageEnum.RU to "Не введёна команда",
            LanguageEnum.KZ to "Ешқандай команда енгізілмеді",
        )
    ),
    NO_DESCRIPTION_ENTERED(
        mapOf(
            LanguageEnum.EN to "No description entered",
            LanguageEnum.RU to "Не введёно описание",
            LanguageEnum.KZ to "Сипаттама енгізілмеді",
        )
    ),
    NO_ENTRY_NUMBER_ENTERED(
        mapOf(
            LanguageEnum.EN to "No entry number entered",
            LanguageEnum.RU to "Не введён номер входа",
            LanguageEnum.KZ to "Кіру нөмірі енгізілмеген",
        )
    ),
    NO_NAME_ENTERED(
        mapOf(
            LanguageEnum.EN to "No name entered",
            LanguageEnum.RU to "Не введёно наименование",
            LanguageEnum.KZ to "Аты-жөні енгізілмеген",
        )
    ),
    NO_OBJECT_SELECTED(
        mapOf(
            LanguageEnum.EN to "No object selected",
            LanguageEnum.RU to "Не выбран объект",
            LanguageEnum.KZ to "Ешқандай нысан таңдалмады",
        )
    ),
    OBJECT(
        mapOf(
            LanguageEnum.EN to "Object",
            LanguageEnum.RU to "Объект",
            LanguageEnum.KZ to "Нысан",
        )
    ),
    OBJECT_MODEL(
        mapOf(
            LanguageEnum.EN to "Object model",
            LanguageEnum.RU to "Модель объекта",
            LanguageEnum.KZ to "Нысан моделі",
        )
    ),
    OBJECT_NAME(
        mapOf(
            LanguageEnum.EN to "Object Name",
            LanguageEnum.RU to "Наименование объекта",
            LanguageEnum.KZ to "Нысанның атауы",
        )
    ),
    OBJECT_SCHEMA_FILE(
        mapOf(
            LanguageEnum.EN to "Object schema file",
            LanguageEnum.RU to "Файл схемы объекта",
            LanguageEnum.KZ to "Нысан схемасының файлы",
        )
    ),
    OBJECT_TYPE(
        mapOf(
            LanguageEnum.EN to "Object Type",
            LanguageEnum.RU to "Тип объекта",
            LanguageEnum.KZ to "Нысан түрі",
        )
    ),
    ON_LIMIT(
        mapOf(
            LanguageEnum.EN to "On limit",
            LanguageEnum.RU to "Граница включения",
            LanguageEnum.KZ to "Қосу шекарасы",
        )
    ),
    OPERATION(
        mapOf(
            LanguageEnum.EN to "Operation",
            LanguageEnum.RU to "Работа",
            LanguageEnum.KZ to "Жұмыс",
        )
    ),
    OPERATING_STATE_ABOVE_LIMIT(
        mapOf(
            LanguageEnum.EN to "Operating state above limit",
            LanguageEnum.RU to "Рабочее состояние выше граничного",
            LanguageEnum.KZ to "Жұмыс жағдайы шектен жоғары",
        )
    ),
    OPERATING_TIME(
        mapOf(
            LanguageEnum.EN to "Operating time [hours]",
            LanguageEnum.RU to "Время работы [час]",
            LanguageEnum.KZ to "Жұмыс уақыты [сағат]",
        )
    ),
    OTHER_DATA(
        mapOf(
            LanguageEnum.EN to "Other data",
            LanguageEnum.RU to "Прочие данные",
            LanguageEnum.KZ to "Басқа деректер",
        )
    ),
    OUTGOING_FLOW_METER(
        mapOf(
            LanguageEnum.EN to "Outgoing/flow meter",
            LanguageEnum.RU to "Исходящий/расходный счётчик",
            LanguageEnum.KZ to "Шығыс/шығын есептегіші",
        )
    ),
    OUTGOING_METERS(
        mapOf(
            LanguageEnum.EN to "Outgoing meters [units]",
            LanguageEnum.RU to "Исходящие счётчики [ед.изм.]",
            LanguageEnum.KZ to "Шығыс есептегіштері [бірлік]",
        )
    ),
    OVERLOAD_LIMIT(
        mapOf(
            LanguageEnum.EN to "Overload limit",
            LanguageEnum.RU to "Граница перегрузки",
            LanguageEnum.KZ to "Шамадан тыс жүктеме шегі",
        )
    ),
    PERIOD(
        mapOf(
            LanguageEnum.EN to "Period",
            LanguageEnum.RU to "Период",
            LanguageEnum.KZ to "Кезең",
        )
    ),
    PERIOD_DEFINED(
        mapOf(
            LanguageEnum.EN to "For a given period",
            LanguageEnum.RU to "За заданный период",
            LanguageEnum.KZ to "Белгілі бір кезең үшін",
        )
    ),
    PERIOD_5_MINUTES(
        mapOf(
            LanguageEnum.EN to "In the last 5 minutes",
            LanguageEnum.RU to "За последние 5 минут",
            LanguageEnum.KZ to "Соңғы 5 минутта",
        )
    ),
    PERIOD_15_MINUTES(
        mapOf(
            LanguageEnum.EN to "In the last 15 minutes",
            LanguageEnum.RU to "За последние 15 минут",
            LanguageEnum.KZ to "Соңғы 15 минутта",
        )
    ),
    PERIOD_1_HOUR(
        mapOf(
            LanguageEnum.EN to "In the last hour",
            LanguageEnum.RU to "За последний час",
            LanguageEnum.KZ to "Соңғы сағатта",
        )
    ),
    PERIOD_6_HOUR(
        mapOf(
            LanguageEnum.EN to "In the last 6 hours",
            LanguageEnum.RU to "За последние 6 часов",
            LanguageEnum.KZ to "Соңғы 6 сағатта",
        )
    ),
    PERIOD_1_DAY(
        mapOf(
            LanguageEnum.EN to "Over the past 24 hours",
            LanguageEnum.RU to "За последние сутки",
            LanguageEnum.KZ to "Соңғы 24 сағат ішінде",
        )
    ),
    PERIOD_7_DAY(
        mapOf(
            LanguageEnum.EN to "Over the past week",
            LanguageEnum.RU to "За последнюю неделю",
            LanguageEnum.KZ to "Соңғы аптада",
        )
    ),
    PHASE(
        mapOf(
            LanguageEnum.EN to "Phase",
            LanguageEnum.RU to "Фаза",
            LanguageEnum.KZ to "Фаза",
        )
    ),
    PHASE_SUM(
        mapOf(
            LanguageEnum.EN to "Phase sum",
            LanguageEnum.RU to "По сумме фаз",
            LanguageEnum.KZ to "Фазалардың қосындысы бойынша",
        )
    ),
    PHONE_NUMBER(
        mapOf(
            LanguageEnum.EN to "Phone Number",
            LanguageEnum.RU to "Номер телефона",
            LanguageEnum.KZ to "Телефон нөмірі",
        )
    ),
    REASON_FOR_BLOCKING(
        mapOf(
            LanguageEnum.EN to "Reason for Blocking",
            LanguageEnum.RU to "Причина блокировки",
            LanguageEnum.KZ to "Блоктау себебі",
        )
    ),
    SCALE_MULTIPLIER(
        mapOf(
            LanguageEnum.EN to "Scale: Multiplier",
            LanguageEnum.RU to "Шкала: множитель",
            LanguageEnum.KZ to "Масштаб: Көбейткіш",
        )
    ),
    SCALE_NUMBER_DIVISIONS(
        mapOf(
            LanguageEnum.EN to "Scale: Number of divisions",
            LanguageEnum.RU to "Шкала: количество делений",
            LanguageEnum.KZ to "Масштаб: бөлімдер саны",
        )
    ),
    SECONDS(
        mapOf(
            LanguageEnum.EN to "seconds",
            LanguageEnum.RU to "секунд",
            LanguageEnum.KZ to "секундтар",
        )
    ),
    SELECT_AN_ACTION(
        mapOf(
            LanguageEnum.EN to "Select an action:",
            LanguageEnum.RU to "Выберите действие:",
            LanguageEnum.KZ to "Әрекетті таңдаңыз:",
        )
    ),
    SENSOR_CALIBRATION(
        mapOf(
            LanguageEnum.EN to "Sensor calibration",
            LanguageEnum.RU to "Тарировка датчика",
            LanguageEnum.KZ to "Сенсорды калибрлеу",
        )
    ),
    SENSOR_DESCRIPTION(
        mapOf(
            LanguageEnum.EN to "Sensor Description",
            LanguageEnum.RU to "Описание датчика",
            LanguageEnum.KZ to "Сенсордың сипаттамасы",
        )
    ),
    SENSOR_GROUP_NAME(
        mapOf(
            LanguageEnum.EN to "Sensor group name",
            LanguageEnum.RU to "Наименование группы датчиков",
            LanguageEnum.KZ to "Сенсор тобының атауы",
        )
    ),
    SENSOR_NAME(
        mapOf(
            LanguageEnum.EN to "Sensor name",
            LanguageEnum.RU to "Наименование датчика",
            LanguageEnum.KZ to "Сенсор атауы",
        )
    ),
    SENSOR_NAME_POSTFIX(
        mapOf(
            LanguageEnum.EN to "Sensor name postfix",
            LanguageEnum.RU to "Постфикс наименования датчика",
            LanguageEnum.KZ to "Сенсор атауының постфиксі",
        )
    ),
    SENSOR_NAME_PREFIX(
        mapOf(
            LanguageEnum.EN to "Sensor name prefix",
            LanguageEnum.RU to "Префикс наименования датчика",
            LanguageEnum.KZ to "Сенсор атауының префиксі",
        )
    ),
    SENSOR_TYPE(
        mapOf(
            LanguageEnum.EN to "Sensor Type",
            LanguageEnum.RU to "Тип датчика",
            LanguageEnum.KZ to "Сенсор түрі",
        )
    ),
    SENSOR_VALUE(
        mapOf(
            LanguageEnum.EN to "Sensor value",
            LanguageEnum.RU to "Значение датчика",
            LanguageEnum.KZ to "Сенсор мәні",
        )
    ),
    SENSOR_VALUE_NOT_ENTERED(
        mapOf(
            LanguageEnum.EN to "Sensor value not entered",
            LanguageEnum.RU to "Не введёно значение датчика",
            LanguageEnum.KZ to "Сенсор мәні енгізілмеген",
        )
    ),
    SENSOR_WITHOUT_DESCRIPTION(
        mapOf(
            LanguageEnum.EN to "(sensor without description)",
            LanguageEnum.RU to "(датчик без описания)",
            LanguageEnum.KZ to "(сипаттамасыз сенсор)",
        )
    ),
    SERIAL_NUMBER(
        mapOf(
            LanguageEnum.EN to "Serial Number",
            LanguageEnum.RU to "Серийный номер",
            LanguageEnum.KZ to "Сериялық нөмір",
        )
    ),
    SIM_CARD_OWNER(
        mapOf(
            LanguageEnum.EN to "SIM Card Owner",
            LanguageEnum.RU to "Владелец сим-карты",
            LanguageEnum.KZ to "SIM карта иесі",
        )
    ),
    SMOOTHING_PERIOD(
        mapOf(
            LanguageEnum.EN to "Smoothing period [min]",
            LanguageEnum.RU to "Период сглаживания [мин]",
            LanguageEnum.KZ to "Тегістеу кезеңі [мин]",
        )
    ),
    SPEED(
        mapOf(
            LanguageEnum.EN to "Speed",
            LanguageEnum.RU to "Скорость",
            LanguageEnum.KZ to "Жылдамдық",
        )
    ),
    SPEED_LIMIT(
        mapOf(
            LanguageEnum.EN to "Speed limit [km/h]",
            LanguageEnum.RU to "Ограничение скорости [км/ч]",
            LanguageEnum.KZ to "Жылдамдық шегі [км/сағ]",
        )
    ),
    START_OF_PERIOD(
        mapOf(
            LanguageEnum.EN to "Beginning of Period",
            LanguageEnum.RU to "Начало периода",
            LanguageEnum.KZ to "Кезеңнің басы",
        )
    ),
    SUBMISSION_TIME(
        mapOf(
            LanguageEnum.EN to "Submission Time",
            LanguageEnum.RU to "Время отправки",
            LanguageEnum.KZ to "Жөнелту уақыты",
        )
    ),
    TANK_TYPE(
        mapOf(
            LanguageEnum.EN to "Tank type",
            LanguageEnum.RU to "Тип ёмкости",
            LanguageEnum.KZ to "Контейнер түрі",
        )
    ),
    TEMPERATURE(
        mapOf(
            LanguageEnum.EN to "Temperature",
            LanguageEnum.RU to "Температура",
            LanguageEnum.KZ to "Температура",
        )
    ),
    THE_END(
        mapOf(
            LanguageEnum.EN to "The end",
            LanguageEnum.RU to "Окончание",
            LanguageEnum.KZ to "Соңы",
        )
    ),
    THE_ENTRY_NUMBER_MUST_BE_BETWEEN_0_AND_65535(
        mapOf(
            LanguageEnum.EN to "The entry number must be between 0 and 65535",
            LanguageEnum.RU to "Номер входа должен быть в диапазоне от 0 до 65535",
            LanguageEnum.KZ to "Енгізілген нөмір 0-ден 65535-ке дейінгі диапазонда болуы керек",
        )
    ),
    THIS_NAME_ALREADY_EXISTS(
        mapOf(
            LanguageEnum.EN to "This name already exists",
            LanguageEnum.RU to "Такое наименование уже существует",
            LanguageEnum.KZ to "Бұл атау бұрыннан бар",
        )
    ),
    TIME_2_LOCAL(
        mapOf(
            LanguageEnum.EN to "Time 2 (Local)",
            LanguageEnum.RU to "Время 2 (местное)",
            LanguageEnum.KZ to "Уақыт 2 (жергілікті)",
        )
    ),
    TIME_2_UTC(
        mapOf(
            LanguageEnum.EN to "Time 2 (UTC)",
            LanguageEnum.RU to "Время 2 (UTC)",
            LanguageEnum.KZ to "Уақыт 2 (UTC)",
        )
    ),
    TIME_LOCAL(
        mapOf(
            LanguageEnum.EN to "Time (Local)",
            LanguageEnum.RU to "Время (местное)",
            LanguageEnum.KZ to "Уақыт (жергілікті)",
        )
    ),
    TIME_UTC(
        mapOf(
            LanguageEnum.EN to "Time (UTC)",
            LanguageEnum.RU to "Время (UTC)",
            LanguageEnum.KZ to "Уақыт (UTC)",
        )
    ),
    TO(
        mapOf(
            LanguageEnum.EN to "to",
            LanguageEnum.RU to "по",
            LanguageEnum.KZ to "бастап",
        )
    ),
    TOTAL(
        mapOf(
            LanguageEnum.EN to "TOTAL",
            LanguageEnum.RU to "ИТОГО",
            LanguageEnum.KZ to "ЖАЛПЫ",
        )
    ),
    TYPE(
        mapOf(
            LanguageEnum.EN to "Type",
            LanguageEnum.RU to "Тип",
            LanguageEnum.KZ to "Түрі",
        )
    ),
    UNIT_HOUR(
        mapOf(
            LanguageEnum.EN to "[hour]",
            LanguageEnum.RU to "[час]",
            LanguageEnum.KZ to "[сағат]",
        )
    ),
    UNIT_KM(
        mapOf(
            LanguageEnum.EN to "[km]",
            LanguageEnum.RU to "[км]",
            LanguageEnum.KZ to "[км]",
        )
    ),
    UNIT_OF_MEASUREMENT(
        mapOf(
            LanguageEnum.EN to "Unit of measurement",
            LanguageEnum.RU to "Единица измерения",
            LanguageEnum.KZ to "Өлшем бірлігі",
        )
    ),
    UNKNOWN_ERROR(
        mapOf(
            LanguageEnum.EN to "(unknown error)",
            LanguageEnum.RU to "(неизвестная ошибка)",
            LanguageEnum.KZ to "(белгісіз қате)",
        )
    ),
    UNKNOWN_SENSOR_TYPE(
        mapOf(
            LanguageEnum.EN to "(unknown sensor type)",
            LanguageEnum.RU to "(неизвестный тип датчика)",
            LanguageEnum.KZ to "(сенсор түрі белгісіз)",
        )
    ),
    USER(
        mapOf(
            LanguageEnum.EN to "User",
            LanguageEnum.RU to "Пользователь",
            LanguageEnum.KZ to "Пайдаланушы",
        )
    ),
    USING_END(
        mapOf(
            LanguageEnum.EN to "End of Operation",
            LanguageEnum.RU to "Окончание эксплуатации",
            LanguageEnum.KZ to "Қызмет көрсетудің аяқталуы",
        )
    ),
    USING_START(
        mapOf(
            LanguageEnum.EN to "Start of Operation",
            LanguageEnum.RU to "Начало эксплуатации",
            LanguageEnum.KZ to "Жұмыстың басталуы",
        )
    ),
    VALUE(
        mapOf(
            LanguageEnum.EN to "Value",
            LanguageEnum.RU to "Значение",
            LanguageEnum.KZ to "Мағынасы",
        )
    ),
    VALUE_2(
        mapOf(
            LanguageEnum.EN to "Value 2",
            LanguageEnum.RU to "Значение 2",
            LanguageEnum.KZ to "Мағынасы 2",
        )
    ),
    VALUE_3(
        mapOf(
            LanguageEnum.EN to "Value 3",
            LanguageEnum.RU to "Значение 3",
            LanguageEnum.KZ to "Мағынасы 3",
        )
    ),
    VALUE_4(
        mapOf(
            LanguageEnum.EN to "Value 4",
            LanguageEnum.RU to "Значение 4",
            LanguageEnum.KZ to "Мағынасы 4",
        )
    ),
    WITHOUT_NAME(
        mapOf(
            LanguageEnum.EN to "(without name)",
            LanguageEnum.RU to "(без наименования)",
            LanguageEnum.KZ to "(аты жоқ)",
        )
    ),
    WITHOUT_NAME_SERIAL_NUMBER_OR_INDEX(
        mapOf(
            LanguageEnum.EN to "(without name, serial number, or index)",
            LanguageEnum.RU to "(без наименования, серийного номера и индекса)",
            LanguageEnum.KZ to "(атауын, сериялық нөмірін және индексінсіз)",
        )
    ),
    WORKING_FLOW_TANK(
        mapOf(
            LanguageEnum.EN to "Working/flow tank",
            LanguageEnum.RU to "Рабочая/расходная ёмкость",
            LanguageEnum.KZ to "Жұмыс/тұтыну сыйымдылығы",
        )
    ),

    ____(mapOf(LanguageEnum.EN to "____", LanguageEnum.RU to "____", LanguageEnum.KZ to "______")),
}
