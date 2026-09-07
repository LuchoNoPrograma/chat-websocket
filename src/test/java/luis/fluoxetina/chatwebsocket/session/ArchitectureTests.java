package luis.fluoxetina.chatwebsocket.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import luis.fluoxetina.chatwebsocket.dto.ChatMessageDto;
import luis.fluoxetina.chatwebsocket.enums.MessageType;
import luis.fluoxetina.chatwebsocket.enums.MessageFormat;
import luis.fluoxetina.chatwebsocket.messaging.MessageCommands;
import luis.fluoxetina.chatwebsocket.model.service.ChatMessageService;
import luis.fluoxetina.chatwebsocket.model.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.Callable;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {"app.data-reset.idle-timeout=PT30M", "spring.datasource.url=jdbc:h2:mem:architecture;DB_CLOSE_DELAY=-1"})
@AutoConfigureMockMvc
class ArchitectureTests {
  @Autowired MockMvc mvc;
  @Autowired SessionService sessions;
  @Autowired SessionGeneration generation;
  @Autowired MessageCommands commands;
  @Autowired ChatMessageService messages;
  @Autowired UserService users;
  @Autowired ObjectMapper json;

  @Test void restRequiresCredentialAndCannotImpersonateRequester() throws Exception {
    var alice = sessions.create("secure.alice", "sol");
    var bob = sessions.create("secure.bob", "kimi");
    var outsider = sessions.create("secure.outsider", "luna");
    commands.accept(command("secret", "secure.bob", UUID.randomUUID().toString()), "secure.alice", true);
    mvc.perform(get("/api/v1/dm/secure.bob/messages").param("requester", "secure.alice"))
      .andExpect(status().isUnauthorized());
    mvc.perform(get("/api/v1/dm/secure.bob/messages").param("requester", "secure.alice")
      .header("Authorization", "Bearer " + outsider.token()))
      .andExpect(status().isOk()).andExpect(jsonPath("$.messages").isEmpty());
    mvc.perform(get("/api/v1/dm/secure.alice/messages").header("Authorization", "Bearer " + bob.token()))
      .andExpect(status().isOk()).andExpect(jsonPath("$.messages[0].body").value("secret"));
    assertThat(alice.token()).hasSizeGreaterThanOrEqualTo(40);
  }

  @Test void aliasesCannotBeReclaimedAndRevokedTokensFail() {
    var issued = sessions.create("reserved.alias", "sol");
    assertThatThrownBy(() -> sessions.create("reserved.alias", "kimi")).isInstanceOf(SessionAccessException.class);
    sessions.revoke("Bearer " + issued.token());
    assertThatThrownBy(() -> sessions.require("Bearer " + issued.token())).isInstanceOf(SessionAccessException.class);
    assertThatThrownBy(() -> sessions.create("reserved.alias", "kimi")).isInstanceOf(SessionAccessException.class);
  }

  @Test void generationInvalidatesPreviousCredentials() {
    var issued = sessions.create("old.generation", "sol");
    generation.rotate();
    assertThatThrownBy(() -> sessions.require("Bearer " + issued.token())).isInstanceOf(SessionAccessException.class);
    assertThat(sessions.create("old.generation", "sol").generation()).isNotEqualTo(issued.generation());
  }

  @Test void concurrentRetriesInsertExactlyOneMessageAndRejectChangedPayload() throws Exception {
    users.connect("retry.sender"); users.connect("retry.peer");
    String id = UUID.randomUUID().toString();
    var executor = Executors.newFixedThreadPool(6);
    try {
      Callable<MessageCommands.Accepted> send = () -> commands.accept(command("hola", "retry.peer", id), "retry.sender", true);
      var results = executor.invokeAll(List.of(send, send, send, send, send, send));
      var accepted = new java.util.ArrayList<MessageCommands.Accepted>();
      for (var result : results) accepted.add(result.get());
      assertThat(accepted.stream().filter(MessageCommands.Accepted::created)).hasSize(1);
      assertThat(accepted.stream().map(item -> item.message().getId()).distinct()).hasSize(1);
      assertThat(messages.findDirectHistory("retry.sender", "retry.peer", null, 30).messages()).hasSize(1);
      assertThatThrownBy(() -> commands.accept(command("different", "retry.peer", id), "retry.sender", true))
        .isInstanceOf(IllegalArgumentException.class);
    } finally { executor.shutdownNow(); }
  }

  @Test void transportFieldsCannotOverwriteServerIdentityOrTimestamps() throws Exception {
    var dto = json.readValue("{\"id\":\"forged\",\"createdAt\":\"2000-01-01T00:00:00Z\",\"readBy\":{\"x\":\"2000-01-01T00:00:00Z\"}}", ChatMessageDto.class);
    assertThat(dto.getId()).isNull(); assertThat(dto.getCreatedAt()).isNull(); assertThat(dto.getReadBy()).isNull();
  }

  private ChatMessageDto command(String body, String peer, String id) {
    return ChatMessageDto.builder().body(body).recipientId(peer).clientMessageId(id)
      .type(MessageType.CHAT).format(MessageFormat.TEXT).build();
  }
}
