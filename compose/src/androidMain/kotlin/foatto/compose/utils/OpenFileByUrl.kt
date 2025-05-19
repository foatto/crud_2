package foatto.compose.utils

import android.content.Intent
import android.net.Uri
import foatto.compose.CoreMainApplication
import io.ktor.http.*

internal actual fun openFileByUrl(url: String) {
    val protocol = settings.getStringOrNull(SETTINGS_SERVER_PROTOCOL)?.let { protocolName ->
        URLProtocol.byName[protocolName]
    } ?: URLProtocol.HTTP
    val host = settings.getString(SETTINGS_SERVER_ADDRESS, "")
    val port = settings.getInt(SETTINGS_SERVER_PORT, 80)

    val fullUrl = "$protocol://$host:$port$url"
    val urlIntent = Intent(Intent.ACTION_VIEW, Uri.parse(fullUrl))
    CoreMainApplication.instance.startActivity(urlIntent)
}