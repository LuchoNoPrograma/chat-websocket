package luis.fluoxetina.chatwebsocket.mapper;

import luis.fluoxetina.chatwebsocket.dto.ChatMessageDto;
import luis.fluoxetina.chatwebsocket.model.doc.ChatMessage;
import org.springframework.stereotype.Component;

@Component
public class ChatMessageMapper {
  public ChatMessage toDocument(ChatMessageDto chatMessageDto) {
    return ChatMessage.builder()
      .userId(chatMessageDto.getUserId())
      .roomId(chatMessageDto.getRoomId())
      .body(chatMessageDto.getBody())
      .type(chatMessageDto.getType())
      .format(chatMessageDto.getFormat())
      .createdAt(chatMessageDto.getCreatedAt())
      .build();
  }

  public ChatMessageDto toDto(ChatMessage chatMessage) {
    return ChatMessageDto.builder()
      .userId(chatMessage.getUserId())
      .roomId(chatMessage.getRoomId())
      .body(chatMessage.getBody())
      .type(chatMessage.getType())
      .format(chatMessage.getFormat())
      .createdAt(chatMessage.getCreatedAt())
      .build();
  }
}
