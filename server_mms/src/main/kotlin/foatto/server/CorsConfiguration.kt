package foatto.server

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class CorsConfiguration : WebMvcConfigurer {

    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/**")
            .allowedOrigins("http://localhost:3000")
//            .allowedMethods("GET", "PUT")
            .allowedHeaders("*")
//            .exposedHeaders("header1", "header2")
//            .allowCredentials(true)
//            .maxAge(3600)
    }
}