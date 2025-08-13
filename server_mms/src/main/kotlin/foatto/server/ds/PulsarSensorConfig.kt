package foatto.server.ds

import kotlinx.serialization.Serializable

@Serializable
class PulsarSensorConfig(
    val id: String,         // IDxxxx - тип датчика в hex

    val descr: String,      // описание датчика

    //--- для всех типов датчиков

    //--- игнорировать значения менее, чем
    val minIgnore: Double? = null,
    //--- игнорировать значения более, чем
    val maxIgnore: Double? = null,
    //--- единица измерения
    val dim: String? = null,

    //--- для датчиков работы оборудования

    //--- рабочее состояние
    //"true" to "если > граничного значения",
    //"false" to "если < граничного значения",
    val isAboveBorder: Boolean? = null,
    //--- граница вкл/выкл оборудования
    val onOffBorder: Double? = null,
    //--- граница холостого хода
    val idleBorder: Double? = null,
    //--- граница перегрузки
    val limitBorder: Double? = null,
    //--- минимальное время простоя (сек)
    val minOffTime: Int? = null,
    //--- минимальное время работы (сек)
    val minOnTime: Int? = null,
    //--- минимальное время холостого хода (сек)
    val minIdleTime: Int? = null,
    //--- минимальное время перегрузки (сек)
    val minOverTime: Int? = null,

    //--- для датчиков контрольных значений

    //--- минимально отображаемое значение
    val minView: Double? = null,
    //--- максимальное отображаемое значение
    val maxView: Double? = null,
    //--- значение нижней границы допустимых значений
    val minLimit: Double? = null,
    //--- значение верхней границы допустимых значений
    val maxLimit: Double? = null,
    //--- период сглаживания (сек)
    val smoothTime: Int? = null,

    //--- для счётных/накопительных датчиков

    //--- абсолютный счётчик
    val isAbsoluteCount: Boolean? = null,
    //--- тип счётчика:
    //--- 0 - входящий счётчик
    //--- 1 - исходящий счётчик
    val inOutType: Int? = null,

    //--- для энерго-датчиков

    //--- фаза:
    //--- 0 - сумма фаз/все фазы
    //--- 1 - фаза A
    //--- 2 - фаза B
    //--- 3 - фаза C
    val phase: Int? = null,

    //--- тарировка
    val sensorValues: List<Double> = emptyList(),     // значения датчика
    val dataValues: List<Double> = emptyList(),       // реальные значения
)
