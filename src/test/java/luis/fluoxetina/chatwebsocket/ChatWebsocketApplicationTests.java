package luis.fluoxetina.chatwebsocket;

import luis.fluoxetina.chatwebsocket.enums.MessageFormat;
import luis.fluoxetina.chatwebsocket.enums.MessageType;
import luis.fluoxetina.chatwebsocket.lifecycle.IdleDataResetService;
import luis.fluoxetina.chatwebsocket.model.doc.ChatMessage;
import luis.fluoxetina.chatwebsocket.model.doc.Room;
import luis.fluoxetina.chatwebsocket.model.service.ChatMessageService;
import luis.fluoxetina.chatwebsocket.model.service.RoomService;
import luis.fluoxetina.chatwebsocket.model.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.time.ZonedDateTime;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ChatWebsocketApplicationTests {
  @Autowired
  private RoomService roomService;
  @Autowired
  private ChatMessageService chatMessageService;
  @Autowired
  private IdleDataResetService idleDataResetService;
  @Autowired
  private UserService userService;

  @Test
  void contextLoads() {
  }

  @Test
  void roomCounterNeverDropsBelowZero() {
    Room room = roomService.createRoom(Room.builder()
      .name("Sala de prueba")
      .description("Valida el contador de participantes durante la sesión.")
      .tags(new ArrayList<>())
      .build());

    assertThat(roomService.leaveRoom(room.getId(), "alice").getActiveUsers()).isZero();
    assertThat(roomService.joinRoom(room.getId(), "alice").getActiveUsers()).isEqualTo(1);
    assertThat(roomService.leaveRoom(room.getId(), "alice").getActiveUsers()).isZero();
    assertThat(roomService.leaveRoom(room.getId(), "alice").getActiveUsers()).isZero();
  }

  @Test
  void messagesAreStoredForTheActiveSessionAndReturnedInOrder() {
    Room room = roomService.createRoom(Room.builder()
      .name("Historial local")
      .description("Valida mensajes almacenados durante la sesión activa.")
      .tags(new ArrayList<>())
      .build());

    ChatMessage first = chatMessageService.save(message(room.getId(), "primero"));
    ChatMessage second = chatMessageService.save(message(room.getId(), "segundo"));
    List<ChatMessage> history = chatMessageService.findAllByRoomId(room.getId());

    assertThat(first.getId()).isNotBlank();
    assertThat(second.getId()).isNotBlank();
    assertThat(history).extracting(ChatMessage::getBody).containsExactly("primero", "segundo");
  }

  @Test
  void repliesPreserveCanonicalContextAndRejectMessagesFromAnotherRoom() {
    Room room = roomService.createRoom(Room.builder()
      .name("Respuestas válidas")
      .description("Valida el contexto citado dentro de una misma conversación.")
      .tags(new ArrayList<>())
      .build());
    Room otherRoom = roomService.createRoom(Room.builder()
      .name("Otra conversación")
      .description("Impide citar mensajes que pertenecen a una sala diferente.")
      .tags(new ArrayList<>())
      .build());
    ChatMessage original = chatMessageService.save(message(room.getId(), "mensaje original"));

    ChatMessage reply = chatMessageService.save(ChatMessage.builder()
      .roomId(room.getId())
      .userId("bob")
      .body("respuesta")
      .type(MessageType.CHAT)
      .format(MessageFormat.TEXT)
      .replyToId(original.getId())
      .replyToUserId("usuario falsificado")
      .replyToBody("texto falsificado")
      .replyToCreatedAt(ZonedDateTime.now().plusDays(1))
      .build());

    assertThat(reply.getReplyToId()).isEqualTo(original.getId());
    assertThat(reply.getReplyToUserId()).isEqualTo(original.getUserId());
    assertThat(reply.getReplyToBody()).isEqualTo(original.getBody());
    assertThat(reply.getReplyToCreatedAt().toInstant().toEpochMilli())
      .isEqualTo(original.getCreatedAt().toInstant().toEpochMilli());
    assertThatThrownBy(() -> chatMessageService.save(ChatMessage.builder()
      .roomId(otherRoom.getId())
      .userId("bob")
      .body("respuesta cruzada")
      .type(MessageType.CHAT)
      .format(MessageFormat.TEXT)
      .replyToId(original.getId())
      .build()))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("another conversation");
  }

  @Test
  void roomHistoryUsesStableCursorPagesInsteadOfReturningEverything() {
    Room room = roomService.createRoom(Room.builder()
      .name("Historial paginado")
      .description("Valida la carga incremental del historial de una sala.")
      .tags(new ArrayList<>())
      .build());
    ZonedDateTime start = ZonedDateTime.now().minusHours(1);
    IntStream.range(0, 35).forEach(index -> chatMessageService.save(ChatMessage.builder()
      .id("page-message-" + room.getId() + "-" + String.format("%02d", index))
      .roomId(room.getId())
      .userId("alice")
      .body("mensaje-" + index)
      .type(MessageType.CHAT)
      .format(MessageFormat.TEXT)
      .createdAt(start.plusSeconds(index))
      .build()));

    ChatMessageService.HistoryPage latest = chatMessageService.findRoomHistory(room.getId(), null, 10);
    ChatMessageService.HistoryPage previous = chatMessageService.findRoomHistory(
      room.getId(), latest.nextCursor(), 10);

    assertThat(latest.messages()).extracting(ChatMessage::getBody)
      .containsExactly("mensaje-25", "mensaje-26", "mensaje-27", "mensaje-28", "mensaje-29",
        "mensaje-30", "mensaje-31", "mensaje-32", "mensaje-33", "mensaje-34");
    assertThat(latest.hasMore()).isTrue();
    assertThat(latest.nextCursor()).isNotBlank();
    assertThat(previous.messages()).extracting(ChatMessage::getBody)
      .containsExactly("mensaje-15", "mensaje-16", "mensaje-17", "mensaje-18", "mensaje-19",
        "mensaje-20", "mensaje-21", "mensaje-22", "mensaje-23", "mensaje-24");
    assertThat(previous.messages()).extracting(ChatMessage::getId)
      .doesNotContainAnyElementsOf(latest.messages().stream().map(ChatMessage::getId).toList());
  }

  @Test
  void directMessagesAreStoredOnceAndVisibleFromBothSides() {
    userService.connect("alice.dm");
    userService.connect("bob.dm");
    ChatMessage persisted = chatMessageService.saveDirectMessage(ChatMessage.builder()
      .userId("forged-user")
      .recipientId("forged-recipient")
      .body("  hola 👋  ")
      .type(MessageType.CHAT)
      .format(MessageFormat.TEXT)
      .build(), "alice.dm", "bob.dm");

    ChatMessageService.HistoryPage aliceHistory = chatMessageService.findDirectHistory(
      "alice.dm", "bob.dm", null, 30);
    ChatMessageService.HistoryPage bobHistory = chatMessageService.findDirectHistory(
      "bob.dm", "alice.dm", null, 30);
    ChatMessage reply = chatMessageService.saveDirectMessage(ChatMessage.builder()
      .body("respuesta directa")
      .type(MessageType.CHAT)
      .format(MessageFormat.TEXT)
      .replyToId(persisted.getId())
      .build(), "bob.dm", "alice.dm");
    aliceHistory = chatMessageService.findDirectHistory("alice.dm", "bob.dm", null, 30);
    bobHistory = chatMessageService.findDirectHistory("bob.dm", "alice.dm", null, 30);

    assertThat(persisted.getUserId()).isEqualTo("alice.dm");
    assertThat(persisted.getRecipientId()).isEqualTo("bob.dm");
    assertThat(persisted.getBody()).isEqualTo("hola 👋");
    assertThat(reply.getReplyToId()).isEqualTo(persisted.getId());
    assertThat(reply.getReplyToUserId()).isEqualTo("alice.dm");
    assertThat(reply.getReplyToBody()).isEqualTo("hola 👋");
    assertThat(aliceHistory.messages()).extracting(ChatMessage::getId)
      .containsExactly(persisted.getId(), reply.getId());
    assertThat(bobHistory.messages()).extracting(ChatMessage::getId)
      .containsExactly(persisted.getId(), reply.getId());
    assertThatThrownBy(() -> chatMessageService.saveDirectMessage(ChatMessage.builder()
      .body("mensaje propio")
      .type(MessageType.CHAT)
      .format(MessageFormat.TEXT)
      .build(), "alice.dm", "alice.dm"))
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void idleResetClearsSessionDataAndRestoresSqlSeed() {
    Room temporaryRoom = roomService.createRoom(Room.builder()
      .name("Sala temporal")
      .description("Debe desaparecer al restaurar la sesión efímera.")
      .tags(new ArrayList<>())
      .build());
    userService.connect("reset.reader");
    ChatMessage temporary = chatMessageService.save(message(temporaryRoom.getId(), "mensaje temporal"));
    chatMessageService.markRead(temporary.getId(), "reset.reader", java.util.Set.of(temporaryRoom.getId()));

    idleDataResetService.markActivity();

    assertThat(roomService.findAll("name", org.springframework.data.domain.Sort.Direction.ASC))
      .extracting(Room::getName)
      .containsExactlyInAnyOrder("Cero contexto", "Dev & chill", "¿Sale algo?");
    assertThat(roomService.findAll("name", org.springframework.data.domain.Sort.Direction.ASC))
      .allSatisfy(room -> assertThat(chatMessageService.findAllByRoomId(room.getId())).hasSize(2));
    assertThat(chatMessageService.findAllByRoomId(temporaryRoom.getId())).isEmpty();
  }

  @Test
  void readReceiptsAreIdempotentPersistedAndRequireRoomMembership() {
    userService.connect("alice");
    userService.connect("reader");
    Room room = roomService.createRoom(Room.builder().name("Lecturas")
      .description("Confirmaciones de lectura de los mensajes").tags(new ArrayList<>()).build());
    ChatMessage sent = chatMessageService.save(message(room.getId(), "hola"));
    assertThatThrownBy(() -> chatMessageService.markRead(sent.getId(), "reader", java.util.Set.of()))
      .isInstanceOf(IllegalArgumentException.class);
    var first = chatMessageService.markRead(sent.getId(), "reader", java.util.Set.of(room.getId())).getReadBy().get("reader");
    var repeated = chatMessageService.markRead(sent.getId(), "reader", java.util.Set.of(room.getId()));
    assertThat(repeated.getReadBy()).hasSize(1);
    assertThat(repeated.getReadBy().get("reader").toInstant()).isEqualTo(first.toInstant());
    assertThat(chatMessageService.markRead(sent.getId(), "alice", java.util.Set.of(room.getId())).getReadBy())
      .doesNotContainKey("alice");
    assertThat(chatMessageService.findRoomHistory(room.getId(), null, 30).messages().get(0).getReadBy())
      .containsKey("reader");
    assertThatThrownBy(() -> chatMessageService.markRead("missing", "reader", java.util.Set.of(room.getId())))
      .isInstanceOf(IllegalArgumentException.class);
    ChatMessage event = chatMessageService.save(ChatMessage.builder().roomId(room.getId()).userId("alice")
      .type(MessageType.JOIN).format(MessageFormat.TEXT).build());
    assertThatThrownBy(() -> chatMessageService.markRead(event.getId(), "reader", java.util.Set.of(room.getId())))
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void directReadReceiptsRejectThirdPartiesAndSurviveHistoryReload() {
    userService.connect("sender.read");
    userService.connect("peer.read");
    userService.connect("outsider.read");
    ChatMessage sent = chatMessageService.saveDirectMessage(ChatMessage.builder().body("hola")
      .type(MessageType.CHAT).format(MessageFormat.TEXT).build(), "sender.read", "peer.read");
    assertThatThrownBy(() -> chatMessageService.markRead(sent.getId(), "outsider.read", java.util.Set.of()))
      .isInstanceOf(IllegalArgumentException.class);
    chatMessageService.markRead(sent.getId(), "peer.read", java.util.Set.of());
    assertThat(chatMessageService.findDirectHistory("sender.read", "peer.read", null, 30)
      .messages().get(0).getReadBy()).containsKey("peer.read");
  }

  @Test
  void deliveryAndReadKeepFirstServerTimesAndRejectNonRecipients() {
    userService.connect("delivery.sender");
    userService.connect("delivery.peer");
    userService.connect("delivery.outsider");
    ChatMessage sent = chatMessageService.saveDirectMessage(ChatMessage.builder().body("entrega")
      .type(MessageType.CHAT).format(MessageFormat.TEXT).build(), "delivery.sender", "delivery.peer");
    assertThat(sent.getDeliveredTo()).isEmpty();
    assertThatThrownBy(() -> chatMessageService.markDelivered(sent.getId(), "delivery.outsider", java.util.Set.of()))
      .isInstanceOf(IllegalArgumentException.class);
    var first = chatMessageService.markDelivered(sent.getId(), "delivery.peer", java.util.Set.of());
    var deliveredAt = first.getDeliveredTo().get("delivery.peer");
    assertThat(first.getReadBy()).isEmpty();
    var read = chatMessageService.markRead(sent.getId(), "delivery.peer", java.util.Set.of());
    var readAt = read.getReadBy().get("delivery.peer");
    assertThat(readAt.toInstant()).isAfterOrEqualTo(deliveredAt.toInstant());
    chatMessageService.markDelivered(sent.getId(), "delivery.peer", java.util.Set.of());
    chatMessageService.markRead(sent.getId(), "delivery.peer", java.util.Set.of());
    var history = chatMessageService.findDirectHistory("delivery.sender", "delivery.peer", null, 30).messages().get(0);
    assertThat(history.getDeliveredTo().get("delivery.peer").toInstant()).isEqualTo(deliveredAt.toInstant());
    assertThat(history.getReadBy().get("delivery.peer").toInstant()).isEqualTo(readAt.toInstant());
    assertThat(chatMessageService.markDelivered(sent.getId(), "delivery.sender", java.util.Set.of()).getDeliveredTo())
      .doesNotContainKey("delivery.sender");
  }

  @Test
  void roomDeliveryRequiresSubscriptionAndReadImpliesDelivery() {
    userService.connect("receipt.peer");
    var room = roomService.createRoom(Room.builder().name("Entregas")
      .description("Confirmaciones en una sala pública").tags(new ArrayList<>()).build());
    var sent = chatMessageService.save(message(room.getId(), "hola"));
    assertThatThrownBy(() -> chatMessageService.markDelivered(sent.getId(), "receipt.peer", java.util.Set.of()))
      .isInstanceOf(IllegalArgumentException.class);
    var read = chatMessageService.markRead(sent.getId(), "receipt.peer", java.util.Set.of(room.getId()));
    assertThat(read.getDeliveredTo().get("receipt.peer")).isEqualTo(read.getReadBy().get("receipt.peer"));
    var event = chatMessageService.save(ChatMessage.builder().roomId(room.getId()).userId("alice")
      .type(MessageType.JOIN).format(MessageFormat.TEXT).build());
    assertThatThrownBy(() -> chatMessageService.markDelivered(event.getId(), "receipt.peer", java.util.Set.of(room.getId())))
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void longMessagesAndReplySnapshotsSurviveHistoryWithoutTruncation() {
    var room = roomService.createRoom(Room.builder().name("Textos largos")
      .description("Texto extenso y citas completas").tags(new ArrayList<>()).build());
    String body = "texto\n".repeat(1666) + "fin!";
    assertThat(body).hasSize(10000);
    var original = chatMessageService.save(message(room.getId(), body));
    var reply = message(room.getId(), "respuesta");
    reply.setReplyToId(original.getId());
    chatMessageService.save(reply);
    var history = chatMessageService.findRoomHistory(room.getId(), null, 30).messages();
    assertThat(history).anySatisfy(item -> assertThat(item.getBody()).isEqualTo(body));
    assertThat(history).anySatisfy(item -> assertThat(item.getReplyToBody()).isEqualTo(body));
    assertThatThrownBy(() -> chatMessageService.save(message(room.getId(), body + "!")))
      .isInstanceOf(IllegalArgumentException.class);
  }

  private ChatMessage message(String roomId, String body) {
    return ChatMessage.builder()
      .roomId(roomId)
      .userId("alice")
      .body(body)
      .type(MessageType.CHAT)
      .format(MessageFormat.TEXT)
      .build();
  }

}
