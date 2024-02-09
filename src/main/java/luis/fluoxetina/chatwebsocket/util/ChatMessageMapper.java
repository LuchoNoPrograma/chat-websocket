package luis.fluoxetina.chatwebsocket.util;

import luis.fluoxetina.chatwebsocket.dto.ChatMessageDto;
import luis.fluoxetina.chatwebsocket.model.doc.ChatMessage;
import org.springframework.stereotype.Component;

@Component
public class ChatMessageMapper {
  public ChatMessage toDocument(ChatMessageDto chatMessageDto) {
    return ChatMessage.builder()
      .sender(chatMessageDto.getSender())
      .content(chatMessageDto.getContent())
      .sendDate(chatMessageDto.getSendDate())
      .type(chatMessageDto.getType())
      .build();
  }

  public ChatMessageDto toDto(ChatMessage chatMessage) {
    return ChatMessageDto.builder()
      .sender(chatMessage.getSender())
      .content(chatMessage.getContent())
      .sendDate(chatMessage.getSendDate())
      .type(chatMessage.getType())
      .build();
  }
}
