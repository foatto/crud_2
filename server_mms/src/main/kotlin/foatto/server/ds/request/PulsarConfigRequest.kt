package foatto.server.ds.request

import foatto.server.ds.PulsarSensorConfig
import kotlinx.serialization.Serializable

@Serializable
class PulsarConfigRequest(
    val name: String,       // наименование
    val serialNo: String,   // серийный номер
    val sensors: List<PulsarSensorConfig>,  // конфигурация датчиков
)