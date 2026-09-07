package luis.fluoxetina.chatwebsocket.model.service;

import lombok.RequiredArgsConstructor;
import luis.fluoxetina.chatwebsocket.enums.MessageType;
import luis.fluoxetina.chatwebsocket.model.doc.ChatMessage;
import luis.fluoxetina.chatwebsocket.model.repository.ChatMessageRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ChatMessageService {
  private static final int DEFAULT_PAGE_SIZE = 30;
  private static final int MAX_PAGE_SIZE = 50;
  private final ChatMessageRepository chatMessageRepository;
  private final UserService userService;

  @Transactional
  public ChatMessage save(ChatMessage chatMessage) {
    boolean roomMessage = chatMessage.getRoomId() != null && !chatMessage.getRoomId().isBlank();
    boolean directMessage = chatMessage.getConversationKey() != null && !chatMessage.getConversationKey().isBlank();
    if (roomMessage == directMessage) {
      throw new IllegalArgumentException("A message must belong to exactly one room or direct conversation");
    }
    if (chatMessage.getType() == MessageType.CHAT) {
      String body = chatMessage.getBody() == null ? "" : chatMessage.getBody().trim();
      if (body.isBlank() || body.length() > 10000) throw new IllegalArgumentException("Message must contain 1 to 10000 characters");
      chatMessage.setBody(body);
    }
    normalizeReply(chatMessage, roomMessage);
    if (chatMessage.getId() == null) {
      chatMessage.setCreatedAt(ZonedDateTime.now());
    }
    return chatMessageRepository.save(chatMessage);
  }

  @Transactional
  public ChatMessage markRead(String messageId, String username, java.util.Set<String> subscribedRooms) {
    return recordReceipt(messageId, username, subscribedRooms, true);
  }

  @Transactional
  public ChatMessage markDelivered(String messageId, String username, java.util.Set<String> subscribedRooms) {
    return recordReceipt(messageId, username, subscribedRooms, false);
  }

  private ChatMessage recordReceipt(String messageId, String username, java.util.Set<String> subscribedRooms, boolean read) {
    if (username == null || username.isBlank()) throw new IllegalArgumentException("A connected user is required");
    userService.findByUsername(username);
    ChatMessage message = chatMessageRepository.findForRead(messageId)
      .orElseThrow(() -> new IllegalArgumentException("Message does not exist"));
    if (message.getType() != MessageType.CHAT) throw new IllegalArgumentException("Only chat messages can be read");
    if (message.getRoomId() != null) {
      if (!subscribedRooms.contains(message.getRoomId())) throw new IllegalArgumentException("Join the room before reading");
    } else if (!username.equals(message.getRecipientId()) && !username.equals(message.getUserId())) {
      throw new IllegalArgumentException("Not a participant in this conversation");
    }
    if (!username.equals(message.getUserId())) {
      var now = ZonedDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
      message.getDeliveredTo().putIfAbsent(username, now);
      if (read) message.getReadBy().putIfAbsent(username, now);
    }
    return message;
  }

  private void normalizeReply(ChatMessage chatMessage, boolean roomMessage) {
    String replyToId = chatMessage.getReplyToId() == null ? "" : chatMessage.getReplyToId().trim();
    if (replyToId.isBlank()) {
      clearReply(chatMessage);
      return;
    }
    if (chatMessage.getType() != MessageType.CHAT) {
      throw new IllegalArgumentException("Only chat messages can reply to another message");
    }

    ChatMessage repliedMessage = chatMessageRepository.findById(replyToId)
      .orElseThrow(() -> new IllegalArgumentException("The replied message does not exist"));
    if (repliedMessage.getType() != MessageType.CHAT) {
      throw new IllegalArgumentException("Only chat messages can be replied to");
    }

    boolean sameConversation = roomMessage
      ? Objects.equals(chatMessage.getRoomId(), repliedMessage.getRoomId())
      : Objects.equals(chatMessage.getConversationKey(), repliedMessage.getConversationKey());
    if (!sameConversation) {
      throw new IllegalArgumentException("The replied message belongs to another conversation");
    }

    chatMessage.setReplyToId(repliedMessage.getId());
    chatMessage.setReplyToUserId(repliedMessage.getUserId());
    chatMessage.setReplyToBody(repliedMessage.getBody());
    chatMessage.setReplyToCreatedAt(repliedMessage.getCreatedAt());
  }

  private void clearReply(ChatMessage chatMessage) {
    chatMessage.setReplyToId(null);
    chatMessage.setReplyToUserId(null);
    chatMessage.setReplyToBody(null);
    chatMessage.setReplyToCreatedAt(null);
  }

  @Transactional
  public ChatMessage saveDirectMessage(ChatMessage chatMessage, String senderId, String recipientId) {
    if (senderId == null || senderId.isBlank() || recipientId == null || recipientId.isBlank()) {
      throw new IllegalArgumentException("Sender and recipient are required for a direct message");
    }
    if (senderId.equals(recipientId)) {
      throw new IllegalArgumentException("A direct message recipient must be another user");
    }
    String body = chatMessage.getBody() == null ? "" : chatMessage.getBody().trim();
    if (body.isBlank() || body.length() > 10000 || chatMessage.getType() != MessageType.CHAT
      || chatMessage.getFormat() == null) {
      throw new IllegalArgumentException("Direct message content and format are invalid");
    }
    userService.findByUsername(senderId);
    userService.findByUsername(recipientId);
    chatMessage.setRoomId(null);
    chatMessage.setUserId(senderId);
    chatMessage.setRecipientId(recipientId);
    chatMessage.setConversationKey(directConversationKey(senderId, recipientId));
    chatMessage.setBody(body);
    return save(chatMessage);
  }

  @Transactional(readOnly = true)
  public List<ChatMessage> findAllByRoomId(String roomId) {
    return chatMessageRepository.findAllByRoomIdOrderByCreatedAtAsc(roomId);
  }

  @Transactional(readOnly = true)
  public HistoryPage findRoomHistory(String roomId, String cursor, Integer requestedSize) {
    int size = normalizePageSize(requestedSize);
    MessageCursor decodedCursor = decodeCursor(cursor);
    List<ChatMessage> messages = decodedCursor == null
      ? chatMessageRepository.findByRoomIdOrderByCreatedAtDescIdDesc(roomId, PageRequest.of(0, size + 1))
      : chatMessageRepository.findRoomMessagesBefore(
        roomId, decodedCursor.createdAt(), decodedCursor.id(), PageRequest.of(0, size + 1));
    return toHistoryPage(messages, size);
  }

  @Transactional(readOnly = true)
  public HistoryPage findDirectHistory(String firstUsername,
                                       String secondUsername,
                                       String cursor,
                                       Integer requestedSize) {
    String conversationKey = directConversationKey(firstUsername, secondUsername);
    if (firstUsername.equals(secondUsername)) {
      throw new IllegalArgumentException("A direct conversation requires two different users");
    }
    userService.findByUsername(firstUsername);
    userService.findByUsername(secondUsername);
    int size = normalizePageSize(requestedSize);
    MessageCursor decodedCursor = decodeCursor(cursor);
    List<ChatMessage> messages = decodedCursor == null
      ? chatMessageRepository.findByConversationKeyOrderByCreatedAtDescIdDesc(
        conversationKey, PageRequest.of(0, size + 1))
      : chatMessageRepository.findDirectMessagesBefore(
        conversationKey, decodedCursor.createdAt(), decodedCursor.id(), PageRequest.of(0, size + 1));
    return toHistoryPage(messages, size);
  }

  private HistoryPage toHistoryPage(List<ChatMessage> newestFirst, int size) {
    boolean hasMore = newestFirst.size() > size;
    List<ChatMessage> page = new ArrayList<>(newestFirst.subList(0, Math.min(size, newestFirst.size())));
    Collections.reverse(page);
    String nextCursor = hasMore && !page.isEmpty() ? encodeCursor(page.get(0)) : null;
    return new HistoryPage(page, nextCursor, hasMore);
  }

  private int normalizePageSize(Integer requestedSize) {
    if (requestedSize == null) return DEFAULT_PAGE_SIZE;
    if (requestedSize < 1 || requestedSize > MAX_PAGE_SIZE) {
      throw new IllegalArgumentException("Message page size must be between 1 and " + MAX_PAGE_SIZE);
    }
    return requestedSize;
  }

  private String directConversationKey(String firstUsername, String secondUsername) {
    if (firstUsername == null || firstUsername.isBlank() || secondUsername == null || secondUsername.isBlank()) {
      throw new IllegalArgumentException("Both direct conversation users are required");
    }
    return firstUsername.compareTo(secondUsername) <= 0
      ? firstUsername + "\n" + secondUsername
      : secondUsername + "\n" + firstUsername;
  }

  private String encodeCursor(ChatMessage message) {
    String value = message.getCreatedAt() + "\n" + message.getId();
    return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
  }

  private MessageCursor decodeCursor(String cursor) {
    if (cursor == null || cursor.isBlank()) return null;
    try {
      String decoded = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
      String[] parts = decoded.split("\n", 2);
      if (parts.length != 2 || parts[1].isBlank()) throw new IllegalArgumentException();
      return new MessageCursor(ZonedDateTime.parse(parts[0]), parts[1]);
    } catch (RuntimeException exception) {
      throw new IllegalArgumentException("Invalid message history cursor");
    }
  }

  public record HistoryPage(List<ChatMessage> messages, String nextCursor, boolean hasMore) {}

  private record MessageCursor(ZonedDateTime createdAt, String id) {}
}
