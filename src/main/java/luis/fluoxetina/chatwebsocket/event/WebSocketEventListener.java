package luis.fluoxetina.chatwebsocket.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import luis.fluoxetina.chatwebsocket.enums.MessageFormat;
import luis.fluoxetina.chatwebsocket.enums.MessageType;
import luis.fluoxetina.chatwebsocket.exception.EntityNotFoundException;
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

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
@Log4j2
public class WebSocketEventListener {
  private final SimpMessagingTemplate messagingTemplate;
  private final UserService userService;
  private final luis.fluoxetina.chatwebsocket.session.SessionGeneration generation;
  private final RoomService roomService;
  private final ChatMessageService chatMessageService;

  private final UserMapper userMapper;
  private final RoomMapper roomMapper;
  private final ChatMessageMapper chatMessageMapper;
  private final ConcurrentHashMap<String, String> connectedSessions = new ConcurrentHashMap<>();

  @EventListener
  public void handleWebsocketConnect(SessionConnectEvent event){
    StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
    if (headerAccessor.getUser() == null) throw new IllegalArgumentException("Authenticated session required");
    String username = headerAccessor.getUser().getName();
    if (headerAccessor.getSessionId() == null) return;
    if (connectedSessions.putIfAbsent(headerAccessor.getSessionId(), username) == null) {
      var existing = userService.findByUsername(username);
      var user = userService.connect(username, existing.getAvatarId());
      messagingTemplate.convertAndSend("/topic/user", userMapper.toDto(user));
    }
  }

  @EventListener
  public void handleWebSocketDisconnected(SessionDisconnectEvent event) {
    StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
    if (headerAccessor.getSessionId() == null) return;
    String username = connectedSessions.remove(headerAccessor.getSessionId());
    var attributes = headerAccessor.getSessionAttributes();
    if (attributes == null || !generation.current().equals(attributes.get("generation"))) return;
    if (username != null) {
      log.info("User Disconnected: {}", username);
      if (!connectedSessions.containsValue(username)) {
        try {
          User user = userService.disconnect(username);
          messagingTemplate.convertAndSend("/topic/user", userMapper.toDto(user));
        } catch (EntityNotFoundException exception) {
          log.debug("Ignoring disconnect for user that did not finish login: {}", username);
        }
      }
      Set<String> subscribedRooms = getSubscribedRooms(headerAccessor, false);
      if (subscribedRooms == null) return;

      Set.copyOf(subscribedRooms).forEach(roomId -> {
        this.processChatRoomActionType(roomId, username, MessageType.LEAVE);
        log.info("{} has unsubscribed from room: {}", username, roomId);
      });
      subscribedRooms.clear();
    }
  }

  @EventListener
  public void handleSubscribeEvent(SessionSubscribeEvent subscribeEvent) {
    StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(subscribeEvent.getMessage());

    if (headerAccessor.getDestination() != null && headerAccessor.getDestination().contains("/topic/chat/room/")) {
      String roomId = extractRoomId(headerAccessor);
      Set<String> subscribedRooms = getSubscribedRooms(headerAccessor, true);
      if (subscribedRooms == null) return;
      if (subscribedRooms.contains(roomId)) {
        log.debug("Ignoring duplicate subscription to room {}", roomId);
        return;
      }

      ChatMessage chatMessageProcessed = this.processChatRoomActionType(headerAccessor, MessageType.JOIN);
      subscribedRooms.add(chatMessageProcessed.getRoomId());

      log.info(headerAccessor.getSessionAttributes().get("username") + " has subscribed to room: " + chatMessageProcessed.getRoomId());
    }
  }

  @EventListener
  public void handleUnsubscribeEvent(SessionUnsubscribeEvent event) {
    StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

    if (headerAccessor.getDestination() != null && headerAccessor.getDestination().contains("/topic/chat/room/")) {
      String roomId = extractRoomId(headerAccessor);
      Set<String> subscribedRooms = getSubscribedRooms(headerAccessor, false);
      if (subscribedRooms == null || !subscribedRooms.contains(roomId)) {
        log.debug("Ignoring duplicate unsubscribe from room {}", roomId);
        return;
      }

      ChatMessage chatMessageProcessed = this.processChatRoomActionType(headerAccessor, MessageType.LEAVE);
      subscribedRooms.remove(chatMessageProcessed.getRoomId());

      log.info(headerAccessor.getSessionAttributes().get("username") + " has unsubscribed from room: " + chatMessageProcessed.getRoomId());
    }
  }

  private ChatMessage processChatRoomActionType(String roomId, String userId, MessageType messageType) {
    List<MessageType> allowedActions = List.of(MessageType.JOIN, MessageType.LEAVE);
    if (!allowedActions.contains(messageType))
      throw new IllegalArgumentException("Invalid message type. Only JOIN and LEAVE are allowed");
    if (userId == null || userId.isBlank())
      throw new IllegalArgumentException("A connected user is required for room membership events");

    roomService.findById(roomId);

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
    String roomId = extractRoomId(headerAccessor);
    String userId = (String) headerAccessor.getSessionAttributes().get("username");
    return this.processChatRoomActionType(roomId, userId, messageType);
  }

  private String extractRoomId(StompHeaderAccessor headerAccessor) {
    String destination = headerAccessor.getDestination();
    if (destination == null || !destination.startsWith("/topic/chat/room/")) {
      throw new IllegalArgumentException("Invalid chat room destination");
    }
    String roomId = destination.substring("/topic/chat/room/".length());
    if (roomId.isBlank()) throw new IllegalArgumentException("Room id is required");
    return roomId;
  }

  @SuppressWarnings("unchecked")
  private Set<String> getSubscribedRooms(StompHeaderAccessor headerAccessor, boolean create) {
    if (headerAccessor.getSessionAttributes() == null) return null;
    if (create) {
      headerAccessor.getSessionAttributes().computeIfAbsent("subscribedChatRooms", key -> ConcurrentHashMap.<String>newKeySet());
    }
    return (Set<String>) headerAccessor.getSessionAttributes().get("subscribedChatRooms");
  }
}
