package luis.fluoxetina.chatwebsocket.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import luis.fluoxetina.chatwebsocket.dto.UserDto;
import luis.fluoxetina.chatwebsocket.mapper.UserMapper;
import luis.fluoxetina.chatwebsocket.model.doc.User;
import luis.fluoxetina.chatwebsocket.model.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;

@Log4j2
@RestController
@RequiredArgsConstructor
@CrossOrigin({"*"})
public class SimpleLoginController {
  private final SimpMessagingTemplate messagingTemplate;
  private final UserService userService;
  private final UserMapper userMapper;

  @PostMapping("/api/v1/auth")
  public ResponseEntity<UserDto> simpleLogin(@RequestParam String username){
    User userProcessed = userService.connect(username);
    messagingTemplate.convertAndSend("/topic/user", userProcessed);

    //Attribute username saved in Stomp session in WebSocketEventListener.java
    log.info("User Connected: "+username);
    return ResponseEntity.ok(userMapper.toDto(userProcessed));
  }
}
