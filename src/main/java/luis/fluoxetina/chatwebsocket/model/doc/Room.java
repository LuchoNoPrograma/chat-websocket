package luis.fluoxetina.chatwebsocket.model.doc;

import lombok.*;
import org.bson.types.Binary;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.ZonedDateTime;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Document(collection = "rooms")
public class Room {
  @Id
  private String id;

  private String name;
  private String description;

  @ToString.Exclude
  private String imgPortrait;

  private ZonedDateTime createdAt;

  @DBRef
  private List<Tag> tags;

  @DBRef(lazy = true)
  private List<User> users;

  @DBRef(lazy = true)
  private List<ChatMessage> chatMessages;
}
