package luis.fluoxetina.chatwebsocket.messaging;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import luis.fluoxetina.chatwebsocket.session.LiveConnections;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatMetrics implements MeterBinder {
  private final LiveConnections connections;
  @Override public void bindTo(MeterRegistry registry) {
    registry.gauge("chat.connections.active", connections, LiveConnections::size);
  }
}
