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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Log4j2
@CrossOrigin({"*"})
public class ChatController {
  private final ChatMessageMapper chatMessageMapper;
  private final ChatMessageService chatMessageService;
  private final SimpMessagingTemplate messagingTemplate;

  @MessageMapping("/chat.send-message-room")
  public void sendChatMessageToRoom(@Valid @Payload ChatMessageDto chatMessageDto) {
    chatMessageDto.getBody().trim();
    ChatMessage chatMessagePersisted = chatMessageService.save(chatMessageMapper.toDocument(chatMessageDto));
    log.info("Message chat to room: {}", chatMessagePersisted);
    chatMessageMapper.toDto(chatMessagePersisted);
    messagingTemplate.convertAndSend("/topic/chat/room/" + chatMessageDto.getRoomId(), chatMessageMapper.toDto(chatMessagePersisted));
  }
}
