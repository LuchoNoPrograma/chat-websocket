package luis.fluoxetina.chatwebsocket.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import luis.fluoxetina.chatwebsocket.dto.ChatMessageDto;
import luis.fluoxetina.chatwebsocket.mapper.ChatMessageMapper;
import luis.fluoxetina.chatwebsocket.mapper.RoomMapper;
import luis.fluoxetina.chatwebsocket.model.doc.ChatMessage;
import luis.fluoxetina.chatwebsocket.model.service.ChatMessageService;
import luis.fluoxetina.chatwebsocket.model.service.RoomService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Log4j2
public class ChatController {
  private final ChatMessageMapper chatMessageMapper;
  private final ChatMessageService chatMessageService;
  private final SimpMessagingTemplate messagingTemplate;
  private final RoomService roomService;
  private final RoomMapper roomMapper;

  @MessageMapping("/chat.send-message-room")
  public void sendChatMessage(@Valid @Payload ChatMessageDto chatMessageDto) {
    chatMessageDto.getBody().trim();
    ChatMessage chatMessagePersisted = chatMessageService.save(chatMessageMapper.toDocument(chatMessageDto));
    log.info("Message chat: {}", chatMessagePersisted);
    chatMessageMapper.toDto(chatMessagePersisted);
    messagingTemplate.convertAndSend("/topic/chat/room/" + chatMessageDto.getRoomId(), chatMessageMapper.toDto(chatMessagePersisted));
  }


  @MessageMapping("/chat.add-user")
  @SendTo("/topic/chat")
  public ChatMessageDto addUser(@Payload ChatMessageDto chatMessageDto, SimpMessageHeaderAccessor headerAccessor) {
    log.info("Adding user: {}", chatMessageDto.getUserId());
    headerAccessor.getSessionAttributes().put("username", chatMessageDto.getUserId());
    return chatMessageDto;
  }
}
