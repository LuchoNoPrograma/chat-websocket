package luis.fluoxetina.chatwebsocket.messaging;

import java.time.ZonedDateTime;

public record ReceiptEvent(String type, String messageId, String roomId, String userId, String recipientId,
                           String username, ZonedDateTime deliveredAt, ZonedDateTime readAt) {}
