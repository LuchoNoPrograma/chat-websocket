package luis.fluoxetina.chatwebsocket.model.doc;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.ZonedDateTime;
import java.util.List;

@Document(collection = "users")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class User {
  @Id
  private String username;

  private boolean online;
  private ZonedDateTime createdAt;
  /*private List<String> chatMessagesId;*/
}
