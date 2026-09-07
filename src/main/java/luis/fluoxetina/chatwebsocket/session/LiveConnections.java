package luis.fluoxetina.chatwebsocket.session;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.CloseStatus;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LiveConnections {
  private final ConcurrentHashMap<String, WebSocketSession> sockets = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, String> credentials = new ConcurrentHashMap<>();
  public void opened(WebSocketSession socket) { sockets.put(socket.getId(), socket); }
  public void authenticated(String id, String authorization) { credentials.put(id, authorization); }
  public void removed(String id) { sockets.remove(id); credentials.remove(id); }
  public java.util.Map<String, String> credentials() { return java.util.Map.copyOf(credentials); }
  public int size() { return sockets.size(); }
  public void close(String id) {
    var socket = sockets.get(id);
    if (socket != null) try { socket.close(CloseStatus.POLICY_VIOLATION); } catch (java.io.IOException ignored) { }
  }
}
