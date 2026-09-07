package luis.fluoxetina.chatwebsocket.session;

import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class SessionGeneration {
  private volatile String id = UUID.randomUUID().toString();
  public String current() { return id; }
  public void rotate() { id = UUID.randomUUID().toString(); }
}
