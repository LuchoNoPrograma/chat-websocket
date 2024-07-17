package luis.fluoxetina.chatwebsocket.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;
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
  @NotNull
  private String userId; //sender
  private String roomId; //destination room

  private String body;

  @NotNull
  private MessageType type;
  @NotNull
  private MessageFormat format;
  private ZonedDateTime createdAt;
}
