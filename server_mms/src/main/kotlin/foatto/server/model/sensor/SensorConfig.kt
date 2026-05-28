package foatto.server.model.sensor

import foatto.core.i18n.LanguageEnum

class SensorConfig {

    companion object {

        //--- критичный период отсутствия связи и/или данных
        const val CRITICAL_OFF_PERIOD: Int = 3600

        //--- предопределённый номер порта для гео-датика
        const val GEO_PORT_NUM = 19

        //--- предопределённый размер гео-данных (wgsX + wgsY + speed + distance = 4 + 4 + 2 + 4 = 14)
        const val GEO_DATA_SIZE = 14

        //--- логические входы с датчиков

        //--- особые величины - сигнал с устройства
        //const val SENSOR_SIGNAL = -2   // Есть/нет - устаревшие тип датчика, не используется

        //--- составной датчик - гео-данных ( координаты,скорость,пробег )
        const val SENSOR_GEO = -1           // координаты, км/ч, м или км

        //--- учётные величины - отчёты
        const val SENSOR_WORK = 1           // мото * час

        //--- контрольные величины - графики
        //const val SENSOR_LIQUID_FLOW_CALC = 3 - устаревший тип псевдодатчика, сейчас считается автоматически при записи данных со счётчика расхода
        const val SENSOR_LIQUID_LEVEL = 4
        const val SENSOR_WEIGHT = 5
        const val SENSOR_TURN = 6
        const val SENSOR_PRESSURE = 7
        const val SENSOR_TEMPERATURE = 8
        const val SENSOR_VOLTAGE = 9
        const val SENSOR_POWER = 10

        //--- датчики, подключенные через массомер ЭМИС

        //--- контрольные величины - графики
        const val SENSOR_DENSITY = 11
        const val SENSOR_MASS_FLOW = 12
        const val SENSOR_VOLUME_FLOW = 13

        //--- учётные величины - отчёты

        //--- Исторически сложилось так, что это три разных датчика (трёх разных производителей и возможностей)
        //--- но используются они пока одинаково.
        //--- Возможно, когда-нибудь их удастся объединить.
        const val SENSOR_MASS_ACCUMULATED = 14
        const val SENSOR_VOLUME_ACCUMULATED = 15
        const val SENSOR_LIQUID_USING = 16

        //--- сигнальная величина - состояние расходомера/счётчика
        //const val SENSOR_LIQUID_USING_COUNTER_STATE = 17 - вместо этого записываем все данные в MMS_text_XXX

        //--- electro energo counters

        //--- учётные величины - отчёты
        const val SENSOR_ENERGO_COUNT_AD = 20   // Active Direct - активная прямая электроэнергия
        const val SENSOR_ENERGO_COUNT_AR = 21   // Active Reverse - активная обратная электроэнергия
        const val SENSOR_ENERGO_COUNT_RD = 22   // Reactive Direct - реактивная прямая электроэнергия
        const val SENSOR_ENERGO_COUNT_RR = 23   // Reactive Reverse - реактивная обратная электроэнергия

        //--- контрольные величины - графики
        const val SENSOR_ENERGO_VOLTAGE = 30        // voltage by phase
        const val SENSOR_ENERGO_CURRENT = 31        // current by phase
        const val SENSOR_ENERGO_POWER_KOEF = 32     // power koeff by phase
        const val SENSOR_ENERGO_POWER_ACTIVE = 33   // active power by phase
        const val SENSOR_ENERGO_POWER_REACTIVE = 34 // reactive power by phase
        const val SENSOR_ENERGO_POWER_FULL = 35     // full/summary power by phase
        const val SENSOR_ENERGO_TRANSFORM_KOEF_CURRENT = 36 // transformation koeff by current
        const val SENSOR_ENERGO_TRANSFORM_KOEF_VOLTAGE = 37  // transformation koeff by voltage

        //--- отдельный список аналоговых датчиков, чтобы не перечислять их каждый раз (и не пропустить в очередном перечислении)
        val analogueSensorTypes: Set<Int> = setOf(
            SENSOR_LIQUID_LEVEL,
            SENSOR_WEIGHT,
            SENSOR_TURN,
            SENSOR_PRESSURE,
            SENSOR_TEMPERATURE,
            SENSOR_VOLTAGE,
            SENSOR_POWER,
            SENSOR_DENSITY,
            SENSOR_MASS_FLOW,
            SENSOR_VOLUME_FLOW,
            SENSOR_ENERGO_VOLTAGE,
            SENSOR_ENERGO_CURRENT,
            SENSOR_ENERGO_POWER_KOEF,
            SENSOR_ENERGO_POWER_ACTIVE,
            SENSOR_ENERGO_POWER_REACTIVE,
            SENSOR_ENERGO_POWER_FULL,
            SENSOR_ENERGO_TRANSFORM_KOEF_CURRENT,
            SENSOR_ENERGO_TRANSFORM_KOEF_VOLTAGE,
        )

        //--- отдельный список счётных датчиков, чтобы не перечислять их каждый раз (и не пропустить в очередном перечислении)
        val counterSensorTypes: Set<Int> = setOf(
            SENSOR_MASS_ACCUMULATED,
            SENSOR_VOLUME_ACCUMULATED,
            SENSOR_LIQUID_USING,
            SENSOR_ENERGO_COUNT_AD,
            SENSOR_ENERGO_COUNT_AR,
            SENSOR_ENERGO_COUNT_RD,
            SENSOR_ENERGO_COUNT_RR,
        )

        //--- перевёрнутые графики для датчиков
        private val reversedChartSensorTypes = setOf<Int>(/* SensorConfig.SENSOR_DEPTH */)
        fun isReversedChart(sensorType: Int?): Boolean = reversedChartSensorTypes.contains(sensorType)

        //--- названия датчиков
        val hmSensorDescr: Map<Int, Map<LanguageEnum, String>> = mapOf(
            SENSOR_GEO to mapOf(
                LanguageEnum.EN to "Geo-data",
                LanguageEnum.RU to "Гео-данные",
                LanguageEnum.KZ to "Геодеректер",
            ),
            SENSOR_WORK to mapOf(
                LanguageEnum.EN to "Equipment operation",
                LanguageEnum.RU to "Работа оборудования",
                LanguageEnum.KZ to "Жабдықты пайдалану",
            ),
            SENSOR_LIQUID_LEVEL to mapOf(
                LanguageEnum.EN to "Fuel level",
                LanguageEnum.RU to "Уровень топлива",
                LanguageEnum.KZ to "Отын деңгейі",
            ),
            SENSOR_WEIGHT to mapOf(
                LanguageEnum.EN to "Weight",
                LanguageEnum.RU to "Вес",
                LanguageEnum.KZ to "Салмақ",
            ),
            SENSOR_TURN to mapOf(
                LanguageEnum.EN to "Rotational speed",
                LanguageEnum.RU to "Обороты вращения",
                LanguageEnum.KZ to "Айналу жылдамдығы",
            ),
            SENSOR_PRESSURE to mapOf(
                LanguageEnum.EN to "Pressure",
                LanguageEnum.RU to "Давление",
                LanguageEnum.KZ to "Қысым",
            ),
            SENSOR_TEMPERATURE to mapOf(
                LanguageEnum.EN to "Temperature",
                LanguageEnum.RU to "Температура",
                LanguageEnum.KZ to "Температура",
            ),
            SENSOR_VOLTAGE to mapOf(
                LanguageEnum.EN to "Voltage",
                LanguageEnum.RU to "Напряжение",
                LanguageEnum.KZ to "Вольтаж",
            ),
            SENSOR_POWER to mapOf(
                LanguageEnum.EN to "Power",
                LanguageEnum.RU to "Мощность",
                LanguageEnum.KZ to "Қуат",
            ),
            SENSOR_DENSITY to mapOf(
                LanguageEnum.EN to "Density",
                LanguageEnum.RU to "Плотность",
                LanguageEnum.KZ to "Тығыздық",
            ),
            SENSOR_MASS_FLOW to mapOf(
                LanguageEnum.EN to "Mass flow rate",
                LanguageEnum.RU to "Массовый расход",
                LanguageEnum.KZ to "Масса ағынының жылдамдығы",
            ),
            SENSOR_VOLUME_FLOW to mapOf(
                LanguageEnum.EN to "Volumetric flow rate",
                LanguageEnum.RU to "Объёмный расход",
                LanguageEnum.KZ to "Көлемдік ағын жылдамдығы",
            ),
            SENSOR_MASS_ACCUMULATED to mapOf(
                LanguageEnum.EN to "Accumulated mass",
                LanguageEnum.RU to "Накопленная масса",
                LanguageEnum.KZ to "Жиналған масса",
            ),
            SENSOR_VOLUME_ACCUMULATED to mapOf(
                LanguageEnum.EN to "Accumulated volume",
                LanguageEnum.RU to "Накопленный объём",
                LanguageEnum.KZ to "Жинақталған көлем",
            ),
            SENSOR_LIQUID_USING to mapOf(
                LanguageEnum.EN to "Fuel consumption",
                LanguageEnum.RU to "Расход топлива",
                LanguageEnum.KZ to "Отын шығыны",
            ),
            SENSOR_ENERGO_COUNT_AD to mapOf(
                LanguageEnum.EN to "Direct active electrical energy",
                LanguageEnum.RU to "Электроэнергия активная прямая",
                LanguageEnum.KZ to "Тікелей белсенді электр энергиясы",
            ),
            SENSOR_ENERGO_COUNT_AR to mapOf(
                LanguageEnum.EN to "Active reverse electrical energy",
                LanguageEnum.RU to "Электроэнергия активная обратная",
                LanguageEnum.KZ to "Белсенді кері электр энергиясы",
            ),
            SENSOR_ENERGO_COUNT_RD to mapOf(
                LanguageEnum.EN to "Direct reactive electrical energy",
                LanguageEnum.RU to "Электроэнергия реактивная прямая",
                LanguageEnum.KZ to "Тікелей реактивті электр энергиясы",
            ),
            SENSOR_ENERGO_COUNT_RR to mapOf(
                LanguageEnum.EN to "Reactive reverse electrical energy",
                LanguageEnum.RU to "Электроэнергия реактивная обратная",
                LanguageEnum.KZ to "Реактивті кері электр энергиясы",
            ),
            SENSOR_ENERGO_VOLTAGE to mapOf(
                LanguageEnum.EN to "Phase electric voltage",
                LanguageEnum.RU to "Электрическое напряжение фазное",
                LanguageEnum.KZ to "Фазалық электр кернеуі",
            ),
            SENSOR_ENERGO_CURRENT to mapOf(
                LanguageEnum.EN to "Phase electric current",
                LanguageEnum.RU to "Электрический ток фазный",
                LanguageEnum.KZ to "Фазалық электр тогы",
            ),
            SENSOR_ENERGO_POWER_KOEF to mapOf(
                LanguageEnum.EN to "Phase power factor",
                LanguageEnum.RU to "Коэффициент мощности фазный",
                LanguageEnum.KZ to "Фазалық қуат коэффициенті",
            ),
            SENSOR_ENERGO_POWER_ACTIVE to mapOf(
                LanguageEnum.EN to "Active phase power",
                LanguageEnum.RU to "Активная мощность фазная",
                LanguageEnum.KZ to "Белсенді фазалық қуат",
            ),
            SENSOR_ENERGO_POWER_REACTIVE to mapOf(
                LanguageEnum.EN to "Phase reactive power",
                LanguageEnum.RU to "Реактивная мощность фазная",
                LanguageEnum.KZ to "Фазалық реактивті қуат",
            ),
            SENSOR_ENERGO_POWER_FULL to mapOf(
                LanguageEnum.EN to "Full phase power",
                LanguageEnum.RU to "Полная мощность фазная",
                LanguageEnum.KZ to "Толық фазалық қуат",
            ),
            SENSOR_ENERGO_TRANSFORM_KOEF_CURRENT to mapOf(
                LanguageEnum.EN to "Current transformation ratio",
                LanguageEnum.RU to "Коэффициент трансформации по току",
                LanguageEnum.KZ to "Ағымдағы трансформация коэффициенті",
            ),
            SENSOR_ENERGO_TRANSFORM_KOEF_VOLTAGE to mapOf(
                LanguageEnum.EN to "Voltage transformation ratio",
                LanguageEnum.RU to "Коэффициент трансформации по напряжению",
                LanguageEnum.KZ to "Кернеу түрлендіру коэффициенті",
            ),
        )

        val hmPhaseDescr: Map<Int, String> = mapOf(
            0 to "ABC",
            1 to "A",
            2 to "B",
            3 to "C",
        )

    }
}