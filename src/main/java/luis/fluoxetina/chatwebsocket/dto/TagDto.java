package luis.fluoxetina.chatwebsocket.dto;

import lombok.Builder;
import lombok.Value;
import luis.fluoxetina.chatwebsocket.model.doc.Tag;

import java.io.Serializable;

/**
 * DTO for {@link Tag}
 */
@Value
@Builder
public class TagDto implements Serializable {
  String id;
  String name;
  String pathIcon;
}