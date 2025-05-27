package foatto.compose.utils

import foatto.compose.getDefaultServerAddress
import foatto.compose.getDefaultServerPort
import foatto.compose.getDefaultServerProtocol
import io.ktor.http.*
import java.awt.Desktop
import java.net.URI

internal actual fun openFileByUrl(url: String) {
    if (Desktop.isDesktopSupported()) {
        val protocol = getDefaultServerProtocol()?.let { defaultProtocol ->
            settings.getStringOrNull(SETTINGS_SERVER_PROTOCOL)?.let { protocolName ->
                URLProtocol.byName[protocolName] ?: defaultProtocol
            } ?: defaultProtocol
        }?.name
        val host = getDefaultServerAddress()?.let { defaultAddress ->
            settings.getString(SETTINGS_SERVER_ADDRESS, defaultAddress)
        }
        val port = getDefaultServerPort()?.let { defaultPort ->
            settings.getInt(SETTINGS_SERVER_PORT, defaultPort)
        }

        val fullUrl = "$protocol://$host:$port$url"
        Desktop.getDesktop().browse(URI(fullUrl))
    }
}