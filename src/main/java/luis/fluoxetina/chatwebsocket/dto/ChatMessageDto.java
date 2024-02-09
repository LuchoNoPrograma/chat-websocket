package luis.fluoxetina.chatwebsocket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import luis.fluoxetina.chatwebsocket.enums.MessageType;

import java.time.ZonedDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessageDto {
  private String sender;
  private String content;
  private MessageType type;

  private ZonedDateTime sendDate;
}
