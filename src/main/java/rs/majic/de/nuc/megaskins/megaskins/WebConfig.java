package rs.majic.de.nuc.megaskins.megaskins;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Restrict CORS to localhost (any port) and nuc.de.majic.rs
        registry.addMapping("/api/**")
                // allow localhost with any port and both http/https for the public host
                .allowedOriginPatterns("http://localhost:*", "https://localhost:*", "https://nuc.de.majic.rs")
                .allowedMethods("GET", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false)
                .maxAge(3600);
    }
}
