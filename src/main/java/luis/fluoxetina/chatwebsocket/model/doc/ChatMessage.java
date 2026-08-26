package luis.fluoxetina.chatwebsocket.model.doc;

import jakarta.persistence.Column;
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
@Table(name = "chat_messages", indexes = {
  @Index(name = "idx_chat_messages_room_created", columnList = "roomId, createdAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;

  @Column(nullable = false)
  private String roomId;
  @Column(nullable = false, length = 20)
  private String userId; //sender

  @Column(length = 1000)
  private String body;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private MessageType type;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private MessageFormat format;
  @Column(nullable = false)
  private ZonedDateTime createdAt;
}
