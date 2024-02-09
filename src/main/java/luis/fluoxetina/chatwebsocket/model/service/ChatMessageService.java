package luis.fluoxetina.chatwebsocket.model.service;

import lombok.RequiredArgsConstructor;
import luis.fluoxetina.chatwebsocket.dto.ChatMessageDto;
import luis.fluoxetina.chatwebsocket.model.doc.ChatMessage;
import luis.fluoxetina.chatwebsocket.model.repository.ChatMessageRepository;
import luis.fluoxetina.chatwebsocket.util.ChatMessageMapper;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;

@Service
@RequiredArgsConstructor
public class ChatMessageService {
  private final ChatMessageRepository chatMessageRepository;
  private final ChatMessageMapper chatMessageMapper;

  public ChatMessage save(ChatMessage chatMessage) {
    if (chatMessage.getId() == null) {
      chatMessage.setSendDate(ZonedDateTime.now());
    }
    return chatMessageRepository.save(chatMessage);
  }
}
