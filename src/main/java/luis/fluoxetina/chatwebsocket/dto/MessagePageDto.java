package luis.fluoxetina.chatwebsocket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MessagePageDto {
  private List<ChatMessageDto> messages;
  private String nextCursor;
  private boolean hasMore;
}
