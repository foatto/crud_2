package foatto.server.ds.response

import kotlinx.serialization.Serializable

@Serializable
class PulsarCommandResponse(
    //--- код ошибки:
    //--- 0 - нет ошибки
    //--- 1 - контроллер с заданным серийным номером не найден
    val errorCode: Int,
    //--- текст команды
    val command: String? = null,
)