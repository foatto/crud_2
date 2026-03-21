package foatto.server.ds.response

import kotlinx.serialization.Serializable

@Serializable
class PulsarStorageLoadResult(
    //--- код ошибки:
    //--- 0 - нет ошибки
    //--- 1 - контроллер с заданным серийным номером не найден
    //--- 2 - файл с заданным именем не найден
    val errorCode: Int,
    val content: String? = null,
)