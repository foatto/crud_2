package foatto.server.ds

import kotlinx.serialization.Serializable

@Serializable
class PulsarCommandResult(
    //--- код ошибки:
    //--- 0 - нет ошибки
    //--- 1 - контроллер с заданным серийным номером не найден
    val errorCode: Int,
    //--- текст команды
    val command: String? = null,
)