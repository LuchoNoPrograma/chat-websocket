package luis.fluoxetina.chatwebsocket.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {
  @Value("${app.cors.allowed-origins:http://localhost:7070,http://127.0.0.1:7070}")
  private String[] allowedOrigins;

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/api/**")
      .allowedOrigins(allowedOrigins)
      .allowedMethods("GET", "POST", "OPTIONS")
      .allowedHeaders("Authorization", "Content-Type")
      .exposedHeaders("X-Chat-Generation")
      .maxAge(3600);
  }
}
