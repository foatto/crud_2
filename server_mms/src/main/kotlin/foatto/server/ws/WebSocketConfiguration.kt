package foatto.server.ws

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry

@Configuration
@EnableWebSocket
class WebSocketConfiguration : WebSocketConfigurer {

    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry.addHandler(mmsWsHandler(), "/wss")
//          .setHandshakeHandler(handshakeHandler())
// 			.addInterceptors(HttpSessionHandshakeInterceptor())
//          .setAllowedOrigins("https://mydomain.com")
    }

    @Bean
    fun mmsWsHandler(): WebSocketHandler {
        return MMSWSHandler()
    }

    /*
        @Bean
        fun handshakeHandler(): DefaultHandshakeHandler {
            val strategy = JettyRequestUpgradeStrategy()
            strategy.addWebSocketConfigurer {
                it.inputBufferSize = 8192
                it.idleTimeout = Duration.ofSeconds(600)
            }
            return DefaultHandshakeHandler(strategy)
        }
    */
}

