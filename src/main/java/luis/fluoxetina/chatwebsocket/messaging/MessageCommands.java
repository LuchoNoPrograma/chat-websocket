package luis.fluoxetina.chatwebsocket.messaging;

import lombok.RequiredArgsConstructor;
import luis.fluoxetina.chatwebsocket.dto.ChatMessageDto;
import luis.fluoxetina.chatwebsocket.mapper.ChatMessageMapper;
import luis.fluoxetina.chatwebsocket.model.doc.ChatMessage;
import luis.fluoxetina.chatwebsocket.model.repository.ChatMessageRepository;
import luis.fluoxetina.chatwebsocket.model.repository.UserRepository;
import luis.fluoxetina.chatwebsocket.model.service.ChatMessageService;
import luis.fluoxetina.chatwebsocket.enums.MessageType;
import luis.fluoxetina.chatwebsocket.enums.MessageFormat;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class MessageCommands {
  public record Accepted(ChatMessage message, boolean created) {}
  private final UserRepository users;
  private final ChatMessageRepository messages;
  private final ChatMessageMapper mapper;
  private final ChatMessageService service;

  @Transactional
  public Accepted accept(ChatMessageDto command, String username, boolean direct) {
    if (command.getClientMessageId() == null || !command.getClientMessageId().matches("[a-zA-Z0-9-]{16,64}"))
      throw new IllegalArgumentException("clientMessageId is required (16–64 letters, digits or hyphens)");
    if (command.getType() != MessageType.CHAT || command.getFormat() != MessageFormat.TEXT)
      throw new IllegalArgumentException("Only text chat messages are supported");
    // Serialize only this author's commands. The database constraint is a second line of defense.
    users.lockForMessage(username).orElseThrow(() -> new IllegalArgumentException("Unknown sender"));
    String body = command.getBody() == null ? "" : command.getBody().trim();
    String peer = direct && command.getRecipientId() != null ? command.getRecipientId().trim() : null;
    String room = direct ? null : command.getRoomId();
    var existing = messages.findByUserIdAndClientMessageId(username, command.getClientMessageId());
    if (existing.isPresent()) {
      var previous = existing.get();
      if (!Objects.equals(body, previous.getBody()) || !Objects.equals(peer, previous.getRecipientId())
          || !Objects.equals(room, previous.getRoomId()) || !Objects.equals(command.getReplyToId(), previous.getReplyToId()))
        throw new IllegalArgumentException("A clientMessageId cannot be reused for different content");
      return new Accepted(previous, false);
    }
    var message = mapper.toDocument(command);
    message.setUserId(username); message.setRoomId(room); message.setRecipientId(peer);
    message.setBody(body);
    return new Accepted(direct ? service.saveDirectMessage(message, username, peer) : service.save(message), true);
  }
}
