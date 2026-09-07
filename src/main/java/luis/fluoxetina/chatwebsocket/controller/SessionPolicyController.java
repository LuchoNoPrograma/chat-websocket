package luis.fluoxetina.chatwebsocket.controller;

import luis.fluoxetina.chatwebsocket.dto.SessionPolicyDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
public class SessionPolicyController {
  private final Duration idleTimeout;
  private final Duration warningBefore;

  public SessionPolicyController(
    @Value("${app.user-session.idle-timeout:PT15M}") Duration idleTimeout,
    @Value("${app.user-session.warning-before:PT2M}") Duration warningBefore
  ) {
    if (idleTimeout.isZero() || idleTimeout.isNegative()) {
      throw new IllegalArgumentException("User session idle timeout must be positive");
    }
    if (warningBefore.isNegative() || warningBefore.compareTo(idleTimeout) >= 0) {
      throw new IllegalArgumentException("User session warning must be shorter than the idle timeout");
    }
    this.idleTimeout = idleTimeout;
    this.warningBefore = warningBefore;
  }

  @GetMapping("/api/v1/session-policy")
  public SessionPolicyDto getPolicy() {
    return new SessionPolicyDto(idleTimeout.toSeconds(), warningBefore.toSeconds());
  }
}
