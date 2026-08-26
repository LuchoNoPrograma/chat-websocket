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
  private final ChatMessageMapper chatMessageMapper;
  private final ChatMessageService chatMessageService;
  private final RoomService roomService;
  private final SimpMessagingTemplate messagingTemplate;

  @MessageMapping("/chat.send-message-room")
  public void sendChatMessageToRoom(@Valid @Payload ChatMessageDto chatMessageDto,
                                    SimpMessageHeaderAccessor headerAccessor) {
    if (chatMessageDto.getType() != MessageType.CHAT) {
      throw new IllegalArgumentException("JOIN and LEAVE are controlled by the subscription lifecycle");
    }
    String username = (String) headerAccessor.getSessionAttributes().get("username");
    if (username == null || username.isBlank()) {
      throw new IllegalArgumentException("A connected user is required to send messages");
    }
    roomService.findById(chatMessageDto.getRoomId());
    chatMessageDto.setUserId(username);
    chatMessageDto.setBody(chatMessageDto.getBody().trim());
    ChatMessage chatMessagePersisted = chatMessageService.save(chatMessageMapper.toDocument(chatMessageDto));
    log.info("Message chat to room: {}", chatMessagePersisted);
    messagingTemplate.convertAndSend("/topic/chat/room/" + chatMessageDto.getRoomId(), chatMessageMapper.toDto(chatMessagePersisted));
  }
}
