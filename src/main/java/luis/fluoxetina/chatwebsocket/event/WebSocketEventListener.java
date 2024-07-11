package luis.fluoxetina.chatwebsocket.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import luis.fluoxetina.chatwebsocket.enums.MessageFormat;
import luis.fluoxetina.chatwebsocket.enums.MessageType;
import luis.fluoxetina.chatwebsocket.mapper.ChatMessageMapper;
import luis.fluoxetina.chatwebsocket.mapper.RoomMapper;
import luis.fluoxetina.chatwebsocket.mapper.UserMapper;
import luis.fluoxetina.chatwebsocket.model.doc.ChatMessage;
import luis.fluoxetina.chatwebsocket.model.doc.Room;
import luis.fluoxetina.chatwebsocket.model.doc.User;
import luis.fluoxetina.chatwebsocket.model.service.ChatMessageService;
import luis.fluoxetina.chatwebsocket.model.service.RoomService;
import luis.fluoxetina.chatwebsocket.model.service.UserService;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.util.HashMap;
import java.util.Objects;

@Component
@RequiredArgsConstructor
@Log4j2
public class WebSocketEventListener {
  private final SimpMessageSendingOperations messageTemplate;
  private final UserService userService;
  private final RoomService roomService;
  private final ChatMessageService chatMessageService;

  private final UserMapper userMapper;
  private final RoomMapper roomMapper;
  private final ChatMessageMapper chatMessageMapper;

  @EventListener
  public void handleWebSocketDisconnect(SessionDisconnectEvent event) {
    StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
    String username = (String) headerAccessor.getSessionAttributes().get("username");

    if (username != null) {
      log.info("User Disconnected: {}", username);
      User user = userService.disconnect(username);

      messageTemplate.convertAndSend("/topic/user", userMapper.toDto(user));
      if(headerAccessor.getSessionAttributes().get("subscribedChatRooms") == null) return;

      @SuppressWarnings("unchecked")
      HashMap<String, String> subscribedRooms = (HashMap<String, String>) headerAccessor.getSessionAttributes().get("subscribedChatRooms");
      subscribedRooms.forEach((key, value) -> {
        Room room = roomService.leaveRoom(value, user.getUsername());
        messageTemplate.convertAndSend("/topic/room", roomMapper.toDto(room));

        ChatMessage chatMessage = ChatMessage.builder()
                        .type(MessageType.LEAVE)
                        .format(MessageFormat.TEXT)
                        .roomId(value)
                        .userId(user.getUsername())
                        .build();

        ChatMessage chatMessagePersisted = chatMessageService.save(chatMessage);
        log.info(user.getUsername()+" unsubscribed from room: {}", value);
        messageTemplate.convertAndSend("/topic/chat/room/" + value, chatMessageMapper.toDto(chatMessagePersisted));
      });

    }
  }

  @EventListener
  public void handleSubscribeEvent(SessionSubscribeEvent subscribeEvent) {
    StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(subscribeEvent.getMessage());

    if (headerAccessor.getDestination() != null && headerAccessor.getDestination().contains("/topic/chat/room/")) {
      String roomId = headerAccessor.getDestination().split("/topic/chat/room/")[1];
      headerAccessor.getSessionAttributes().computeIfAbsent("subscribedChatRooms", k -> new HashMap<String, String>());
      @SuppressWarnings("unchecked")
      HashMap<String, String> subscribedRooms = (HashMap<String, String>) headerAccessor.getSessionAttributes().get("subscribedChatRooms");
      subscribedRooms.put(roomId, roomId);

      log.info("User subscribed to room: {}", roomId);
    }
  }

  @EventListener
  public void handleUnsubscribeEvent(SessionUnsubscribeEvent event) {
    StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

    if (headerAccessor.getDestination() != null && headerAccessor.getDestination().contains("/topic/chat/room/")) {
      String roomId = headerAccessor.getDestination().split("/topic/chat/room/")[1];
      @SuppressWarnings("unchecked")
      HashMap<String, String> subscribedRooms = (HashMap<String, String>) headerAccessor.getSessionAttributes().get("subscribedChatRooms");
      subscribedRooms.remove(roomId);
      log.info("User unsubscribed from room: {}", roomId);
    }
  }
}
