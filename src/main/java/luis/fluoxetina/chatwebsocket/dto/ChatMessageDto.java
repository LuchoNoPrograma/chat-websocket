package luis.fluoxetina.chatwebsocket.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
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
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private java.util.Map<String, ZonedDateTime> readBy;
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private java.util.Map<String, ZonedDateTime> deliveredTo;
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private String id;
  @Size(max = 64)
  private String clientMessageId;
  private String userId; //sender
  private String roomId; //destination room
  private String recipientId; //destination user for a direct message
  @Size(max = 255)
  private String replyToId;
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private String replyToUserId;
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private String replyToBody;
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private ZonedDateTime replyToCreatedAt;

  @NotBlank
  @Size(max = 10000)
  private String body;

  @NotNull
  private MessageType type;
  @NotNull
  private MessageFormat format;
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private ZonedDateTime createdAt;
}
