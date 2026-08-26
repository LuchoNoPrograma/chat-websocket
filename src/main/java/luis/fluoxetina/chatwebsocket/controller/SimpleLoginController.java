package luis.fluoxetina.chatwebsocket.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import luis.fluoxetina.chatwebsocket.dto.UserDto;
import luis.fluoxetina.chatwebsocket.mapper.UserMapper;
import luis.fluoxetina.chatwebsocket.model.doc.User;
import luis.fluoxetina.chatwebsocket.model.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Log4j2
@RestController
@RequiredArgsConstructor
public class SimpleLoginController {
  private final SimpMessagingTemplate messagingTemplate;
  private final UserService userService;
  private final UserMapper userMapper;

  @PostMapping("/api/v1/auth")
  public ResponseEntity<UserDto> simpleLogin(@RequestParam String username){
    String cleanUsername = username.trim();
    if (cleanUsername.length() < 3 || cleanUsername.length() > 20) {
      throw new IllegalArgumentException("Username must contain between 3 and 20 characters");
    }
    User userProcessed = userService.connect(cleanUsername);
    messagingTemplate.convertAndSend("/topic/user", userProcessed);

    //Attribute username saved in Stomp session in WebSocketEventListener.java
    log.info("User Connected: {}", cleanUsername);
    return ResponseEntity.ok(userMapper.toDto(userProcessed));
  }
}
