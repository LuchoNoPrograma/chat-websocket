package luis.fluoxetina.chatwebsocket.model.repository;

import luis.fluoxetina.chatwebsocket.model.doc.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, String> {
  List<ChatMessage> findAllByRoomIdOrderByCreatedAtAsc(String roomId);
}
