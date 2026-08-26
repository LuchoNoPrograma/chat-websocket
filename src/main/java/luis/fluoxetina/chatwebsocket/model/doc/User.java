package luis.fluoxetina.chatwebsocket.model.doc;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.ZonedDateTime;

@Entity
@Table(name = "chat_users")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class User {
  @Id
  @Column(length = 20)
  private String username;

  private boolean online;
  private ZonedDateTime createdAt;
}
