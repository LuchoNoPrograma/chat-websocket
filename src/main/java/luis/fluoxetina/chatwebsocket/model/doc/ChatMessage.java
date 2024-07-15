package luis.fluoxetina.chatwebsocket.model.doc;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import luis.fluoxetina.chatwebsocket.enums.MessageFormat;
import luis.fluoxetina.chatwebsocket.enums.MessageType;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.ZonedDateTime;

@Document(collection = "chatMessages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
  @Id
  private String id;

  private String roomId;
  private String userId; //sender
  private String userRecipientId; //recipient

  private String body;
  private MessageType type;
  private MessageFormat format;
  private ZonedDateTime createdAt;
}