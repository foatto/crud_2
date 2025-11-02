package foatto.server.ds

import kotlinx.serialization.Serializable

//--- outer JSON-class, don't rename fields!
@Serializable
data class PulsarData(
    //--- kotlinx.serialization при обновлениях версий иногда внезапно перестаёт работать.
    //--- надёжнее будет парсить вручную.
    //--- 2025-10-20T07:08:40.010Z
    val dateTime: String? = null,

    val deviceID: String? = null,
    val blockID: String? = null,

    val idx: Int? = null,
    val vals: List<Map<String, Double>>? = null,
)

