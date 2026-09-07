package luis.fluoxetina.chatwebsocket.controller;

import luis.fluoxetina.chatwebsocket.dto.ChatMessageDto;
import luis.fluoxetina.chatwebsocket.enums.MessageFormat;
import luis.fluoxetina.chatwebsocket.enums.MessageType;
import luis.fluoxetina.chatwebsocket.mapper.ChatMessageMapper;
import luis.fluoxetina.chatwebsocket.model.doc.ChatMessage;
import luis.fluoxetina.chatwebsocket.model.doc.Room;
import luis.fluoxetina.chatwebsocket.model.service.ChatMessageService;
import luis.fluoxetina.chatwebsocket.model.service.RoomService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatControllerTests {
  @Mock
  private luis.fluoxetina.chatwebsocket.messaging.MessageCommands commands;
  @Mock
  private luis.fluoxetina.chatwebsocket.session.ChatAccess access;
  @org.mockito.Spy
  private io.micrometer.core.instrument.MeterRegistry metrics = new io.micrometer.core.instrument.simple.SimpleMeterRegistry();
  @Mock
  private ChatMessageMapper chatMessageMapper;
  @Mock
  private ChatMessageService chatMessageService;
  @Mock
  private RoomService roomService;
  @Mock
  private SimpMessagingTemplate messagingTemplate;
  @InjectMocks
  private ChatController chatController;

  @Test
  void roomMessagePublishesTheSameCanonicalPayloadToConversationAndActivityTopics() {
    ChatMessageDto incoming = ChatMessageDto.builder()
      .roomId("room-1")
      .body("  hola  ")
      .type(MessageType.CHAT)
      .format(MessageFormat.TEXT)
      .build();
    ChatMessage document = ChatMessage.builder().roomId("room-1").body("hola").build();
    ChatMessage persisted = ChatMessage.builder().id("message-1").userId("alice").roomId("room-1").body("hola").build();
    ChatMessageDto canonical = ChatMessageDto.builder()
      .id("message-1")
      .roomId("room-1")
      .userId("alice")
      .body("hola")
      .type(MessageType.CHAT)
      .format(MessageFormat.TEXT)
      .build();
    SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
    headers.setSessionAttributes(new HashMap<>(Map.of("username", "alice")));

    when(roomService.findById("room-1")).thenReturn(Room.builder().id("room-1").build());
    when(access.username(headers)).thenReturn("alice");
    when(commands.accept(incoming, "alice", false)).thenReturn(new luis.fluoxetina.chatwebsocket.messaging.MessageCommands.Accepted(persisted, true));
    when(chatMessageMapper.toDto(persisted)).thenReturn(canonical);

    chatController.sendChatMessageToRoom(incoming, headers);

    verify(messagingTemplate).convertAndSend("/topic/chat/room/room-1", canonical);
    verify(messagingTemplate).convertAndSend("/topic/chat/activity", canonical);
  }
}
