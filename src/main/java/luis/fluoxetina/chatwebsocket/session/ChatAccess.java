package luis.fluoxetina.chatwebsocket.session;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ChatAccess {
  private final SessionService sessions;
  public String username(SimpMessageHeaderAccessor headers) {
    var attributes = headers.getSessionAttributes();
    if (attributes == null) throw new IllegalArgumentException("A connected session is required");
    return sessions.require((String) attributes.get("authorization")).username();
  }
  public Set<String> rooms(SimpMessageHeaderAccessor headers) {
    var attributes = headers.getSessionAttributes();
    if (attributes != null && attributes.get("subscribedChatRooms") instanceof Set<?> values) {
      return values.stream().map(Object::toString).collect(java.util.stream.Collectors.toSet());
    }
    return Set.of();
  }
  public void requireRoom(SimpMessageHeaderAccessor headers, String roomId) {
    username(headers);
    if (!rooms(headers).contains(roomId)) throw new IllegalArgumentException("Entra a la sala antes de enviar mensajes.");
  }
}
