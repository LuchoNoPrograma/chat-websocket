package luis.fluoxetina.chatwebsocket.config;

import lombok.RequiredArgsConstructor;
import luis.fluoxetina.chatwebsocket.lifecycle.IdleDataResetService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
  private final luis.fluoxetina.chatwebsocket.session.LiveConnections liveConnections;
  private final IdleDataResetService idleDataResetService;
  private final luis.fluoxetina.chatwebsocket.session.SessionService sessions;
  private final luis.fluoxetina.chatwebsocket.model.service.RoomService rooms;

  @Value("${app.cors.allowed-origins:http://localhost:7070,http://127.0.0.1:7070}")
  private String[] allowedOrigins;

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry
      .addEndpoint("/ws-chatapp")
      .setAllowedOrigins(allowedOrigins)
      .withSockJS();
  }

  @Override
  public void configureMessageBroker(MessageBrokerRegistry config) {
    config.setApplicationDestinationPrefixes("/ws");
    config.enableSimpleBroker("/topic", "/queue");
    config.setUserDestinationPrefix("/user");
  }

  @Override
  public void configureClientInboundChannel(ChannelRegistration registration) {
    registration.interceptors(new ChannelInterceptor() {
      @Override
      public Message<?> preSend(Message<?> message, MessageChannel channel) {
        idleDataResetService.markActivity();
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) return message;
        var command = accessor.getCommand();
        if (command == StompCommand.CONNECT) {
          String authorization = accessor.getFirstNativeHeader("Authorization");
          var session = sessions.require(authorization);
          accessor.setUser(() -> session.username());
          liveConnections.authenticated(accessor.getSessionId(), authorization);
          var attributes = accessor.getSessionAttributes();
          if (attributes == null) throw new IllegalArgumentException("Session attributes are required");
          attributes.put("username", session.username());
          attributes.put("authorization", authorization);
          attributes.put("generation", session.generation());
        } else if (command != StompCommand.DISCONNECT) {
          var attributes = accessor.getSessionAttributes();
          sessions.require(attributes == null ? null : (String) attributes.get("authorization"));
        }
        String destination = accessor.getDestination();
        if (command == StompCommand.SEND && !java.util.Set.of(
            "/ws/chat.send-message-room", "/ws/chat.send-direct-message", "/ws/chat.read-message",
            "/ws/chat.delivered-message", "/ws/room.create-room").contains(destination == null ? "" : destination)) {
          throw new IllegalArgumentException("Unsupported command destination");
        }
        if (command == StompCommand.SUBSCRIBE) {
          if (destination != null && destination.startsWith("/topic/chat/room/")) {
            rooms.findById(destination.substring("/topic/chat/room/".length()));
          } else if (!java.util.Set.of("/topic/user", "/topic/room", "/topic/chat/activity",
              "/user/queue/direct", "/user/queue/errors", "/user/queue/receipts", "/user/queue/accepted")
              .contains(destination == null ? "" : destination)) {
            throw new IllegalArgumentException("Unsupported subscription destination");
          }
        }
        return message;
      }
    });
  }

  @Bean
  public ServletServerContainerFactoryBean createWebSocketContainer() {
    ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
    container.setMaxTextMessageBufferSize(1024 * 1024);
    container.setMaxBinaryMessageBufferSize(1024 * 1024);
    return container;
  }

  public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
    registration.addDecoratorFactory(handler -> new org.springframework.web.socket.handler.WebSocketHandlerDecorator(handler) {
      @Override
      public void afterConnectionEstablished(org.springframework.web.socket.WebSocketSession session) throws Exception {
        liveConnections.opened(session);
        super.afterConnectionEstablished(session);
      }
      @Override
      public void afterConnectionClosed(org.springframework.web.socket.WebSocketSession session, org.springframework.web.socket.CloseStatus status) throws Exception {
        liveConnections.removed(session.getId());
        super.afterConnectionClosed(session, status);
      }
    });
    registration.setMessageSizeLimit(5000 * 1024);
    registration.setSendBufferSizeLimit(10240 * 1024);
    registration.setSendTimeLimit(5 * 60 * 1000);
  }
}
