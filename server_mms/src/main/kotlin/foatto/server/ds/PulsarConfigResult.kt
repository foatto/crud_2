package foatto.server.ds

import kotlinx.serialization.Serializable

@Serializable
class PulsarConfigResult(
    //--- код ошибки:
    // 0 - без ошибок
    // 1 - не найден контроллер с заданным серийным номером
    // 2 - к контроллеру не привязан объект (т.е. некуда привязать датчики)
    // 3 - неправильный формат ID датчика
    // 4 - неизвестный ID датчика
    val errorCode: Int,
)
