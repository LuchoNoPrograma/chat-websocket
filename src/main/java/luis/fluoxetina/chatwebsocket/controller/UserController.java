package luis.fluoxetina.chatwebsocket.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import luis.fluoxetina.chatwebsocket.dto.UserDto;
import luis.fluoxetina.chatwebsocket.mapper.UserMapper;
import luis.fluoxetina.chatwebsocket.model.service.UserService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Log4j2
@CrossOrigin({"*"})
public class UserController {
  private final UserService userService;
  private final UserMapper userMapper;

  @MessageMapping("/user.connect/{username}")
  @SendTo("/topic/user")
  public UserDto connect(@DestinationVariable String username, StompHeaderAccessor headerAccessor) {
    log.info("User Connected: {}", username);
    headerAccessor.getSessionAttributes().put("username", username);

    return userMapper.toDto(userService.connect(username));
  }

  /*@MessageMapping("/user.disconnect/{username}")
  @SendTo("/topic/user")
  public UserDto disconnect(@DestinationVariable String username, StompHeaderAccessor headerAccessor) {
    log.info("User Disconnected: {}", username);
    return userMapper.toDto(userService.disconnect(username));
  }*/

  @GetMapping("/api/v1/user-online")
  public List<UserDto> findAllOnline(@RequestParam(defaultValue = "true") boolean online) {
    return userService.findAllByOnline(online).stream().map(userMapper::toDto).toList();
  }
}