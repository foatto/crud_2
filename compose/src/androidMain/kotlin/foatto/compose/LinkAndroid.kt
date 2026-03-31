package foatto.compose

import io.ktor.http.*

internal actual fun getDefaultServerProtocol(): URLProtocol? = URLProtocol.HTTPS
internal actual fun getDefaultServerAddress(): String? = "pulsar.report"
internal actual fun getDefaultServerPort(): Int? = 444
