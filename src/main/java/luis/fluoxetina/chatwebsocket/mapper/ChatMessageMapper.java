package luis.fluoxetina.chatwebsocket.mapper;

import luis.fluoxetina.chatwebsocket.dto.ChatMessageDto;
import luis.fluoxetina.chatwebsocket.model.doc.ChatMessage;
import org.springframework.stereotype.Component;

@Component
public class ChatMessageMapper {
  public ChatMessage toDocument(ChatMessageDto chatMessageDto) {
    return ChatMessage.builder()
            .clientMessageId(chatMessageDto.getClientMessageId())
            .userId(chatMessageDto.getUserId())
            .roomId(chatMessageDto.getRoomId())
            .recipientId(chatMessageDto.getRecipientId())
            .replyToId(chatMessageDto.getReplyToId())
            .body(chatMessageDto.getBody())
            .type(chatMessageDto.getType())
            .format(chatMessageDto.getFormat())
            .build();
  }

  public ChatMessageDto toDto(ChatMessage chatMessage) {
    return ChatMessageDto.builder()
            .clientMessageId(chatMessage.getClientMessageId())
            .id(chatMessage.getId())
            .readBy(new java.util.HashMap<>(chatMessage.getReadBy()))
            .deliveredTo(new java.util.HashMap<>(chatMessage.getDeliveredTo()))
            .userId(chatMessage.getUserId())
            .roomId(chatMessage.getRoomId())
            .recipientId(chatMessage.getRecipientId())
            .replyToId(chatMessage.getReplyToId())
            .replyToUserId(chatMessage.getReplyToUserId())
            .replyToBody(chatMessage.getReplyToBody())
            .replyToCreatedAt(chatMessage.getReplyToCreatedAt())
            .body(chatMessage.getBody())
            .type(chatMessage.getType())
            .format(chatMessage.getFormat())
            .createdAt(chatMessage.getCreatedAt())
            .build();
  }
}
