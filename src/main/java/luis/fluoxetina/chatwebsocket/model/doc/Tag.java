package luis.fluoxetina.chatwebsocket.model.doc;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "tags")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Tag {
  private String id;
  private String name;
  private String pathIcon;
}
