package luis.fluoxetina.chatwebsocket.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import luis.fluoxetina.chatwebsocket.dto.ChatMessageDto;
import luis.fluoxetina.chatwebsocket.enums.MessageType;
import luis.fluoxetina.chatwebsocket.mapper.ChatMessageMapper;
import luis.fluoxetina.chatwebsocket.model.doc.ChatMessage;
import luis.fluoxetina.chatwebsocket.model.service.ChatMessageService;
import luis.fluoxetina.chatwebsocket.model.service.RoomService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Log4j2
public class ChatController {
  private final luis.fluoxetina.chatwebsocket.messaging.MessageCommands commands;
  private final io.micrometer.core.instrument.MeterRegistry metrics;
  private final ChatMessageMapper chatMessageMapper;
  private final ChatMessageService chatMessageService;
  private final RoomService roomService;
  private final luis.fluoxetina.chatwebsocket.session.ChatAccess access;
  private final SimpMessagingTemplate messagingTemplate;

  public record ReadRequest(@jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 255) String messageId) {}

  @MessageMapping("/chat.read-message")
  public void readMessage(@Valid @Payload ReadRequest request, SimpMessageHeaderAccessor headers) {
    acknowledge(request, headers, true);
  }

  @MessageMapping("/chat.delivered-message")
  public void deliveredMessage(@Valid @Payload ReadRequest request, SimpMessageHeaderAccessor headers) {
    acknowledge(request, headers, false);
  }

  private void acknowledge(ReadRequest request, SimpMessageHeaderAccessor headers, boolean read) {
    String username = access.username(headers);
    java.util.Set<String> rooms = access.rooms(headers);
    ChatMessage message = read
      ? chatMessageService.markRead(request.messageId(), username, rooms)
      : chatMessageService.markDelivered(request.messageId(), username, rooms);
    if (username.equals(message.getUserId())) return;
    var event = new luis.fluoxetina.chatwebsocket.messaging.ReceiptEvent(
      read ? "MESSAGE_READ" : "MESSAGE_DELIVERED", message.getId(), message.getRoomId(),
      message.getUserId(), message.getRecipientId(), username,
      message.getDeliveredTo().get(username), message.getReadBy().get(username));
    messagingTemplate.convertAndSendToUser(message.getUserId(), "/queue/receipts", event);
    messagingTemplate.convertAndSendToUser(username, "/queue/receipts", event);
    metrics.counter("chat.receipts", "type", event.type()).increment();
  }

  @MessageMapping("/chat.send-message-room")
  public void sendChatMessageToRoom(@Valid @Payload ChatMessageDto chatMessageDto,
                                    SimpMessageHeaderAccessor headerAccessor) {
    if (chatMessageDto.getType() != MessageType.CHAT) {
      throw new IllegalArgumentException("JOIN and LEAVE are controlled by the subscription lifecycle");
    }
    String username = access.username(headerAccessor);
    if (username == null || username.isBlank()) {
      throw new IllegalArgumentException("A connected user is required to send messages");
    }
    if (chatMessageDto.getRoomId() == null || chatMessageDto.getRoomId().isBlank()) {
      throw new IllegalArgumentException("Room id is required");
    }
    access.requireRoom(headerAccessor, chatMessageDto.getRoomId());
    roomService.findById(chatMessageDto.getRoomId());
    publish(commands.accept(chatMessageDto, username, false));
  }

  @MessageMapping("/chat.send-direct-message")
  public void sendDirectMessage(@Valid @Payload ChatMessageDto chatMessageDto,
                                SimpMessageHeaderAccessor headerAccessor) {
    if (chatMessageDto.getType() != MessageType.CHAT) {
      throw new IllegalArgumentException("Direct messages only support CHAT messages");
    }
    String username = access.username(headerAccessor);
    if (username == null || username.isBlank()) {
      throw new IllegalArgumentException("A connected user is required to send direct messages");
    }
    publish(commands.accept(chatMessageDto, username, true));
  }

  private void publish(luis.fluoxetina.chatwebsocket.messaging.MessageCommands.Accepted accepted) {
    var message = accepted.message();
    var payload = chatMessageMapper.toDto(message);
    metrics.counter("chat.messages", "result", accepted.created() ? "created" : "replayed").increment();
    if (accepted.created()) {
      if (message.getRoomId() != null) {
        messagingTemplate.convertAndSend("/topic/chat/room/" + message.getRoomId(), payload);
        messagingTemplate.convertAndSend("/topic/chat/activity", payload);
      } else {
        messagingTemplate.convertAndSendToUser(message.getRecipientId(), "/queue/direct", payload);
        messagingTemplate.convertAndSendToUser(message.getUserId(), "/queue/direct", payload);
      }
    }
    messagingTemplate.convertAndSendToUser(message.getUserId(), "/queue/accepted", payload);
  }
}
