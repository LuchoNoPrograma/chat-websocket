package luis.fluoxetina.chatwebsocket.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
  @NotBlank
  @Size(min = 3, max = 30)
  private String name;
  @NotBlank
  @Size(min = 8, max = 255)
  private String description;
  private Integer activeUsers;

  //Handle img in base64
  private String imgPortrait;
  private ZonedDateTime createdAt;

  @NotNull
  private List<TagDto> tags;
  private List<UserDto> users;
}
