package luis.fluoxetina.chatwebsocket.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoomDto {
  private String id;
  private String name;
  private String description;
  private Integer activeUsers;

  //Handle img in base64
  private String imgPortrait;
  private ZonedDateTime createdAt;

  private List<TagDto> tags;
  private List<UserDto> users;
  private List<ChatMessageDto> chatMessages;
}
