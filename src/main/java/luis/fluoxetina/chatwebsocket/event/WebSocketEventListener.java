package luis.fluoxetina.chatwebsocket.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import luis.fluoxetina.chatwebsocket.enums.MessageType;
import luis.fluoxetina.chatwebsocket.model.doc.ChatMessage;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
@RequiredArgsConstructor
@Log4j2
public class WebSocketEventListener {
  private final SimpMessageSendingOperations messageTemplate;
  public void handleWebSocketDisconnectListener(SessionDisconnectEvent event){
    StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
    String username = (String) headerAccessor.getSessionAttributes().get("username");


    if(username != null){
      log.info("User Disconnected: {}", username);
      ChatMessage chatMessage = ChatMessage.builder()
        .sender(username)
        .type(MessageType.LEAVE)
        .build();

      messageTemplate.convertAndSend("/topic/public", chatMessage);
    }
  }
}
