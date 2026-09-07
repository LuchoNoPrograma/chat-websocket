package luis.fluoxetina.chatwebsocket.session;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SessionExpiry {
  private final SessionService sessions;
  private final LiveConnections connections;
  @Scheduled(fixedDelay = 1000)
  public void closeExpiredConnections() {
    connections.credentials().forEach((id, authorization) -> {
      try { sessions.require(authorization); } catch (SessionAccessException expired) { connections.close(id); }
    });
  }
}
