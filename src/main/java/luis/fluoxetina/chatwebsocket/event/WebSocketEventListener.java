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
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;

@Component
@RequiredArgsConstructor
@Log4j2
public class WebSocketEventListener {
  private final SimpMessagingTemplate messagingTemplate;
  private final UserService userService;
  private final RoomService roomService;
  private final ChatMessageService chatMessageService;

  private final UserMapper userMapper;
  private final RoomMapper roomMapper;
  private final ChatMessageMapper chatMessageMapper;

  @EventListener
  public void handleWebsocketConnect(SessionConnectEvent event){
    StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
    String username = headerAccessor.getLogin();

    headerAccessor.setUser(() -> username);
    headerAccessor.getSessionAttributes().put("username", username);
  }

  @EventListener
  public void handleWebSocketDisconnected(SessionDisconnectEvent event) {
    StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
    String username = (String) headerAccessor.getSessionAttributes().get("username");

    if (username != null) {
      log.info("User Disconnected: {}", username);
      User user = userService.disconnect(username);

      messagingTemplate.convertAndSend("/topic/user", userMapper.toDto(user));
      if (headerAccessor.getSessionAttributes().get("subscribedChatRooms") == null) return;

      @SuppressWarnings("unchecked")
      HashMap<String, String> subscribedRooms = (HashMap<String, String>) headerAccessor.getSessionAttributes().get("subscribedChatRooms");
      subscribedRooms.forEach((key, value) -> {
        this.processChatRoomActionType(value, user.getUsername(), MessageType.LEAVE);
        log.info(user.getUsername() + " has unsubscribed from room: {}", value);
      });
    }
  }

  @EventListener
  public void handleSubscribeEvent(SessionSubscribeEvent subscribeEvent) {
    StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(subscribeEvent.getMessage());

    if (headerAccessor.getDestination() != null && headerAccessor.getDestination().contains("/topic/chat/room/")) {
      ChatMessage chatMessageProcessed = this.processChatRoomActionType(headerAccessor, MessageType.JOIN);

      headerAccessor.getSessionAttributes().computeIfAbsent("subscribedChatRooms", k -> new HashMap<String, String>());
      @SuppressWarnings("unchecked")
      HashMap<String, String> subscribedRooms = (HashMap<String, String>) headerAccessor.getSessionAttributes().get("subscribedChatRooms");
      subscribedRooms.put(chatMessageProcessed.getRoomId(), chatMessageProcessed.getRoomId());

      log.info(headerAccessor.getSessionAttributes().get("username") + " has subscribed to room: " + chatMessageProcessed.getRoomId());
    }
  }

  @EventListener
  public void handleUnsubscribeEvent(SessionUnsubscribeEvent event) {
    StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

    if (headerAccessor.getDestination() != null && headerAccessor.getDestination().contains("/topic/chat/room/")) {
      ChatMessage chatMessageProcessed = this.processChatRoomActionType(headerAccessor, MessageType.LEAVE);

      @SuppressWarnings("unchecked")
      HashMap<String, String> subscribedRooms = (HashMap<String, String>) headerAccessor.getSessionAttributes().get("subscribedChatRooms");
      subscribedRooms.remove(chatMessageProcessed.getRoomId());

      log.info(headerAccessor.getSessionAttributes().get("username") + " has unsubscribed from room: " + chatMessageProcessed.getRoomId());
    }
  }

  private ChatMessage processChatRoomActionType(String roomId, String userId, MessageType messageType) {
    List<MessageType> allowedActions = List.of(MessageType.JOIN, MessageType.LEAVE);
    if (!allowedActions.contains(messageType))
      throw new IllegalArgumentException("Invalid message type. Only JOIN and LEAVE are allowed");

    ChatMessage chatMessagePersisted = chatMessageService.save(ChatMessage.builder()
      .type(messageType)
      .format(MessageFormat.TEXT)
      .roomId(roomId)
      .userId(userId)
      .build());
    messagingTemplate.convertAndSend("/topic/chat/room/" + roomId, chatMessageMapper.toDto(chatMessagePersisted));

    Room room = messageType.equals(MessageType.JOIN) ? roomService.joinRoom(roomId, userId) : roomService.leaveRoom(roomId, userId);
    messagingTemplate.convertAndSend("/topic/room", roomMapper.toDto(room));
    return chatMessagePersisted;
  }

  private ChatMessage processChatRoomActionType(StompHeaderAccessor headerAccessor, MessageType messageType) {
    String roomId = headerAccessor.getDestination().split("/topic/chat/room/")[1];
    String userId = (String) headerAccessor.getSessionAttributes().get("username");
    return this.processChatRoomActionType(roomId, userId, messageType);
  }
}
