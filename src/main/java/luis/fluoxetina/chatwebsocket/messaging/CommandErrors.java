package luis.fluoxetina.chatwebsocket.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.web.bind.annotation.ControllerAdvice;

@ControllerAdvice
@RequiredArgsConstructor
public class CommandErrors {
  public record Rejected(String code, String message, String clientMessageId) {}
  private final ObjectMapper mapper;
  private final io.micrometer.core.instrument.MeterRegistry metrics;

  @MessageExceptionHandler(Exception.class)
  @SendToUser(value = "/queue/errors", broadcast = false)
  public Rejected reject(Exception error, Message<?> frame) {
    String clientMessageId = null;
    try {
      if (frame.getPayload() instanceof byte[] bytes) clientMessageId = mapper.readTree(bytes).path("clientMessageId").asText(null);
    } catch (Exception ignored) { /* Invalid JSON has no correlation id. */ }
    metrics.counter("chat.commands.rejected").increment();
    String message = error instanceof IllegalArgumentException ? error.getMessage() : "No se pudo completar la operación.";
    return new Rejected("COMMAND_REJECTED", message, clientMessageId);
  }
}
