package foatto.server.ds.response

import kotlinx.serialization.Serializable

@Serializable
class PulsarStorageSaveResult(
    //--- код ошибки:
    //--- 0 - нет ошибки
    val errorCode: Int,
)