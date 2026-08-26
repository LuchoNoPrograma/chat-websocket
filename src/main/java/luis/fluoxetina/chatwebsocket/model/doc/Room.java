package luis.fluoxetina.chatwebsocket.model.doc;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "chat_rooms")
public class Room {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;

  @Column(nullable = false, length = 30)
  private String name;
  @Column(nullable = false, length = 255)
  private String description;
  @Builder.Default
  @Column(nullable = false)
  private Integer activeUsers = 0;

  @ToString.Exclude
  @Lob
  private String imgPortrait;

  @Column(nullable = false)
  private ZonedDateTime createdAt;

  @Builder.Default
  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(
    name = "chat_room_tags",
    joinColumns = @JoinColumn(name = "room_id"),
    inverseJoinColumns = @JoinColumn(name = "tag_id")
  )
  private List<Tag> tags = new ArrayList<>();

  @Transient
  private List<User> users;

  @Transient
  private List<ChatMessage> chatMessages;
}
