package luis.fluoxetina.chatwebsocket.controller;

import lombok.RequiredArgsConstructor;
import luis.fluoxetina.chatwebsocket.session.SessionService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class SimpleLoginController {
  private final SessionService sessions;

  @PostMapping("/api/v1/auth")
  public SessionService.Login simpleLogin(@RequestParam String username,
      @RequestParam(defaultValue = "claudia") String avatarId) {
    return sessions.create(username, avatarId);
  }
  @GetMapping("/api/v1/session")
  public SessionService.Login session(@RequestHeader("Authorization") String authorization) {
    return sessions.describe(authorization);
  }
  @PostMapping("/api/v1/logout")
  public void logout(@RequestHeader("Authorization") String authorization) { sessions.revoke(authorization); }
}
