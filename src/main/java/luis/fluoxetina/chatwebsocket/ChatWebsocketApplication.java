package luis.fluoxetina.chatwebsocket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ChatWebsocketApplication {

  public static void main(String[] args) {
    SpringApplication.run(ChatWebsocketApplication.class, args);
  }

}
