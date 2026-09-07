package luis.fluoxetina.chatwebsocket.model.repository;

import luis.fluoxetina.chatwebsocket.model.doc.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.ZonedDateTime;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, String> {
  java.util.Optional<ChatMessage> findByUserIdAndClientMessageId(String userId, String clientMessageId);

  @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @Query("select m from ChatMessage m where m.id = :id")
  java.util.Optional<ChatMessage> findForRead(@Param("id") String id);

  List<ChatMessage> findAllByRoomIdOrderByCreatedAtAsc(String roomId);

  List<ChatMessage> findByRoomIdOrderByCreatedAtDescIdDesc(String roomId, Pageable pageable);

  @Query("""
    select message from ChatMessage message
    where message.roomId = :roomId
      and (message.createdAt < :createdAt
        or (message.createdAt = :createdAt and message.id < :id))
    order by message.createdAt desc, message.id desc
    """)
  List<ChatMessage> findRoomMessagesBefore(@Param("roomId") String roomId,
                                           @Param("createdAt") ZonedDateTime createdAt,
                                           @Param("id") String id,
                                           Pageable pageable);

  List<ChatMessage> findByConversationKeyOrderByCreatedAtDescIdDesc(String conversationKey, Pageable pageable);

  @Query("""
    select message from ChatMessage message
    where message.conversationKey = :conversationKey
      and (message.createdAt < :createdAt
        or (message.createdAt = :createdAt and message.id < :id))
    order by message.createdAt desc, message.id desc
    """)
  List<ChatMessage> findDirectMessagesBefore(@Param("conversationKey") String conversationKey,
                                             @Param("createdAt") ZonedDateTime createdAt,
                                             @Param("id") String id,
                                             Pageable pageable);
}
