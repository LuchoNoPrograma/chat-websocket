package luis.fluoxetina.chatwebsocket.model.service;

import lombok.RequiredArgsConstructor;
import luis.fluoxetina.chatwebsocket.model.doc.ChatMessage;
import luis.fluoxetina.chatwebsocket.model.repository.ChatMessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatMessageService {
  private final ChatMessageRepository chatMessageRepository;

  @Transactional
  public ChatMessage save(ChatMessage chatMessage) {
    if (chatMessage.getId() == null) {
      chatMessage.setCreatedAt(ZonedDateTime.now());
    }
    return chatMessageRepository.save(chatMessage);
  }

  @Transactional(readOnly = true)
  public List<ChatMessage> findAllByRoomId(String roomId) {
    return chatMessageRepository.findAllByRoomIdOrderByCreatedAtAsc(roomId);
  }
}
