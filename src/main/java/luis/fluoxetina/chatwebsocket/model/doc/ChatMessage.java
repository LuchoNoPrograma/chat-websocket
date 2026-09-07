package luis.fluoxetina.chatwebsocket.model.doc;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.FetchType;
import java.util.Map;
import java.util.HashMap;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import luis.fluoxetina.chatwebsocket.enums.MessageFormat;
import luis.fluoxetina.chatwebsocket.enums.MessageType;
import java.time.ZonedDateTime;

@Entity
@Table(name = "chat_messages", uniqueConstraints = @jakarta.persistence.UniqueConstraint(name = "uq_message_author_client", columnNames = {"userId", "clientMessageId"}), indexes = {
  @Index(name = "idx_chat_messages_room_created", columnList = "roomId, createdAt, id"),
  @Index(name = "idx_chat_messages_direct_created", columnList = "conversationKey, createdAt, id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;
  @Column(length = 64)
  private String clientMessageId;

  @Column
  private String roomId;
  @Column(nullable = false, length = 20)
  private String userId; //sender
  @Column(length = 20)
  private String recipientId;
  @Column(length = 41)
  private String conversationKey;
  @Column
  private String replyToId;
  @Column(length = 20)
  private String replyToUserId;
  @Column(length = 10000)
  private String replyToBody;
  @Column
  private ZonedDateTime replyToCreatedAt;

  @Column(length = 10000)
  private String body;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private MessageType type;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private MessageFormat format;
  @Column(nullable = false)
  private ZonedDateTime createdAt;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "chat_message_deliveries", joinColumns = @JoinColumn(name = "message_id"))
  @MapKeyColumn(name = "username", length = 20)
  @Column(name = "delivered_at", nullable = false)
  @Builder.Default
  private Map<String, ZonedDateTime> deliveredTo = new HashMap<>();

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "chat_message_reads", joinColumns = @JoinColumn(name = "message_id"))
  @MapKeyColumn(name = "username", length = 20)
  @Column(name = "read_at", nullable = false)
  @Builder.Default
  private Map<String, ZonedDateTime> readBy = new HashMap<>();
}
