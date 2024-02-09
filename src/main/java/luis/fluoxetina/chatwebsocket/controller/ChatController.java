package luis.fluoxetina.chatwebsocket.controller;

import lombok.RequiredArgsConstructor;
import luis.fluoxetina.chatwebsocket.dto.ChatMessageDto;
import luis.fluoxetina.chatwebsocket.model.doc.ChatMessage;
import luis.fluoxetina.chatwebsocket.model.service.ChatMessageService;
import luis.fluoxetina.chatwebsocket.util.ChatMessageMapper;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatController {
  private final ChatMessageMapper chatMessageMapper;
  private final ChatMessageService chatMessageService;

  @MessageMapping("/chat.send-message")
  @SendTo("/topic/public")
  public ChatMessageDto sendMessage(@Payload ChatMessageDto chatMessageDto) {
    ChatMessage chatMessagePersisted = chatMessageService.save(chatMessageMapper.toDocument(chatMessageDto));

    return chatMessageMapper.toDto(chatMessagePersisted);
  }


  @MessageMapping("/chat.add-user")
  @SendTo("/topic/public")
  public ChatMessageDto addUser(@Payload ChatMessageDto chatMessageDto, SimpMessageHeaderAccessor headerAccessor) {
    headerAccessor.getSessionAttributes().put("username", chatMessageDto.getSender());
    return chatMessageDto;
  }
}
