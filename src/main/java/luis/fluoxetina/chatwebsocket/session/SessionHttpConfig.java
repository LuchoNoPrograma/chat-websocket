package luis.fluoxetina.chatwebsocket.session;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class SessionHttpConfig implements WebMvcConfigurer {
  private final SessionService sessions;
  private final SessionGeneration generation;
  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(new HandlerInterceptor() {
      @Override
      public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        response.setHeader("X-Chat-Generation", generation.current());
        response.setHeader("Cache-Control", "no-store");
        String path = request.getRequestURI();
        if ("OPTIONS".equals(request.getMethod()) || path.equals("/api/v1/session-policy")
            || (path.equals("/api/v1/auth") && "POST".equals(request.getMethod()))) return true;
        var session = sessions.require(request.getHeader("Authorization"));
        request.setAttribute("chatSession", session);
        return true;
      }
    }).addPathPatterns("/api/**");
  }
}
