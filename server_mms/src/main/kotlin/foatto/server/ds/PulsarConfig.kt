package foatto.server.ds

import kotlinx.serialization.Serializable

@Serializable
class PulsarConfig(
    val name: String,       // наименование
    val serialNo: String,   // серийный номер
    val sensors: List<PulsarSensorConfig>,  // конфигурация датчиков
)
