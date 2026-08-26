package luis.fluoxetina.chatwebsocket.event;

import luis.fluoxetina.chatwebsocket.dto.ChatMessageDto;
import luis.fluoxetina.chatwebsocket.dto.RoomDto;
import luis.fluoxetina.chatwebsocket.enums.MessageFormat;
import luis.fluoxetina.chatwebsocket.enums.MessageType;
import luis.fluoxetina.chatwebsocket.mapper.ChatMessageMapper;
import luis.fluoxetina.chatwebsocket.mapper.RoomMapper;
import luis.fluoxetina.chatwebsocket.mapper.UserMapper;
import luis.fluoxetina.chatwebsocket.model.doc.ChatMessage;
import luis.fluoxetina.chatwebsocket.model.doc.Room;
import luis.fluoxetina.chatwebsocket.model.service.ChatMessageService;
import luis.fluoxetina.chatwebsocket.model.service.RoomService;
import luis.fluoxetina.chatwebsocket.model.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebSocketEventListenerTests {
  @Mock
  private SimpMessagingTemplate messagingTemplate;
  @Mock
  private UserService userService;
  @Mock
  private RoomService roomService;
  @Mock
  private ChatMessageService chatMessageService;
  @Mock
  private UserMapper userMapper;
  @Mock
  private RoomMapper roomMapper;
  @Mock
  private ChatMessageMapper chatMessageMapper;

  private WebSocketEventListener listener;

  @BeforeEach
  void setUp() {
    listener = new WebSocketEventListener(
      messagingTemplate,
      userService,
      roomService,
      chatMessageService,
      userMapper,
      roomMapper,
      chatMessageMapper
    );
  }

  @Test
  void repeatedSubscriptionProducesOneJoinTransition() {
    Room room = Room.builder()
      .id("room-1")
      .name("General")
      .description("Conversación general")
      .activeUsers(1)
      .tags(new ArrayList<>())
      .build();
    when(roomService.findById("room-1")).thenReturn(room);
    when(roomService.joinRoom("room-1", "alice")).thenReturn(room);
    when(chatMessageService.save(any(ChatMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(chatMessageMapper.toDto(any(ChatMessage.class))).thenReturn(ChatMessageDto.builder()
      .type(MessageType.JOIN)
      .format(MessageFormat.TEXT)
      .build());
    when(roomMapper.toDto(room)).thenReturn(RoomDto.builder().tags(new ArrayList<>()).build());

    Map<String, Object> sessionAttributes = new HashMap<>();
    sessionAttributes.put("username", "alice");
    listener.handleSubscribeEvent(subscribeEvent(sessionAttributes));
    listener.handleSubscribeEvent(subscribeEvent(sessionAttributes));

    verify(roomService, times(1)).joinRoom("room-1", "alice");
    verify(chatMessageService, times(1)).save(any(ChatMessage.class));
  }

  private SessionSubscribeEvent subscribeEvent(Map<String, Object> sessionAttributes) {
    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
    accessor.setSessionId("session-1");
    accessor.setSubscriptionId("subscription-1");
    accessor.setDestination("/topic/chat/room/room-1");
    accessor.setSessionAttributes(sessionAttributes);
    Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    return new SessionSubscribeEvent(this, message);
  }
}
