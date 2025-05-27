package foatto.compose.utils

import android.content.Intent
import androidx.core.net.toUri
import foatto.compose.CoreMainApplication
import foatto.compose.getDefaultServerAddress
import foatto.compose.getDefaultServerPort
import foatto.compose.getDefaultServerProtocol
import io.ktor.http.*

internal actual fun openFileByUrl(url: String) {
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
    val urlIntent = Intent(Intent.ACTION_VIEW, fullUrl.toUri())
    CoreMainApplication.instance.startActivity(urlIntent)
}