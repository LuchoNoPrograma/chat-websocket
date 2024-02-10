package luis.fluoxetina.chatwebsocket.model.doc;

import lombok.*;
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

  private String sender;
  private String content;
  private MessageType type;

  private ZonedDateTime sendDate;
}
