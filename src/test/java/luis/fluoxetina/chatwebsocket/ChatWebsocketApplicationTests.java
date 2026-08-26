package luis.fluoxetina.chatwebsocket;

import luis.fluoxetina.chatwebsocket.enums.MessageFormat;
import luis.fluoxetina.chatwebsocket.enums.MessageType;
import luis.fluoxetina.chatwebsocket.lifecycle.IdleDataResetService;
import luis.fluoxetina.chatwebsocket.model.doc.ChatMessage;
import luis.fluoxetina.chatwebsocket.model.doc.Room;
import luis.fluoxetina.chatwebsocket.model.service.ChatMessageService;
import luis.fluoxetina.chatwebsocket.model.service.RoomService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ChatWebsocketApplicationTests {
  @Autowired
  private RoomService roomService;
  @Autowired
  private ChatMessageService chatMessageService;
  @Autowired
  private IdleDataResetService idleDataResetService;

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
  void idleResetClearsSessionDataAndRestoresSqlSeed() {
    Room temporaryRoom = roomService.createRoom(Room.builder()
      .name("Sala temporal")
      .description("Debe desaparecer al restaurar la sesión efímera.")
      .tags(new ArrayList<>())
      .build());
    chatMessageService.save(message(temporaryRoom.getId(), "mensaje temporal"));

    idleDataResetService.markActivity();

    assertThat(roomService.findAll("name", org.springframework.data.domain.Sort.Direction.ASC))
      .extracting(Room::getName)
      .containsExactlyInAnyOrder("Cero contexto", "Dev & chill", "Busco parche");
    assertThat(roomService.findAll("name", org.springframework.data.domain.Sort.Direction.ASC))
      .allSatisfy(room -> assertThat(chatMessageService.findAllByRoomId(room.getId())).hasSize(2));
    assertThat(chatMessageService.findAllByRoomId(temporaryRoom.getId())).isEmpty();
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
