package foatto.compose

import io.ktor.http.*

internal actual fun getDefaultServerProtocol(): URLProtocol? = URLProtocol.HTTP
internal actual fun getDefaultServerAddress(): String? = "192.168.0.44"
internal actual fun getDefaultServerPort(): Int? = 19998
