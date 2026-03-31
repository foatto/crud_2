package foatto.compose

import foatto.compose.utils.SETTINGS_SERVER_ADDRESS
import foatto.compose.utils.SETTINGS_SERVER_PORT
import foatto.compose.utils.SETTINGS_SERVER_PROTOCOL
import foatto.compose.utils.settings
import foatto.core.model.AppMessage
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import kotlinx.coroutines.channels.Channel

val wsMessageStore = Channel<AppMessage>(capacity = Channel.UNLIMITED)

val wsClient = HttpClient /* (CIO) - не работает в wasm-target */ {
    install(WebSockets) {

        // maxFrameSize = 1024
        // contentConverter
        // pingIntervalMillis = 20_000
        // pingInterval
        //contentConverter = KotlinxWebsocketSerializationConverter(Json) - будем конвертировать сами
    }
    defaultRequest {
        getDefaultServerProtocol()?.let { defaultProtocol ->
            url.protocol = settings.getStringOrNull(SETTINGS_SERVER_PROTOCOL)?.let { protocolName ->
                URLProtocol.byName[protocolName] ?: defaultProtocol
            } ?: defaultProtocol
        }
        getDefaultServerAddress()?.let { defaultAddress ->
            host = settings.getString(SETTINGS_SERVER_ADDRESS, defaultAddress)
        }
        getDefaultServerPort()?.let { defaultPort ->
            port = settings.getInt(SETTINGS_SERVER_PORT, defaultPort)
        }
    }
}

/* OkHtpp version

val client = HttpClient(OkHttp) {
    engine {
        preconfigured = OkHttpClient.Builder()
            .pingInterval(20, TimeUnit.SECONDS)
            .build()
    }
}
*/