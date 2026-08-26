package luis.fluoxetina.chatwebsocket.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import luis.fluoxetina.chatwebsocket.enums.MessageFormat;
import luis.fluoxetina.chatwebsocket.enums.MessageType;
import luis.fluoxetina.chatwebsocket.model.doc.Tag;

import java.time.ZonedDateTime;

/**
 * DTO for {@link luis.fluoxetina.chatwebsocket.model.doc.ChatMessage}
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatMessageDto {
  private String id;
  @NotNull
  private String userId; //sender
  @NotBlank
  private String roomId; //destination room

  @NotBlank
  @Size(max = 1000)
  private String body;

  @NotNull
  private MessageType type;
  @NotNull
  private MessageFormat format;
  private ZonedDateTime createdAt;
}
