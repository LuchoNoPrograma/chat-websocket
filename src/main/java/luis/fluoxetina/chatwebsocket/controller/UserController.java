package luis.fluoxetina.chatwebsocket.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import luis.fluoxetina.chatwebsocket.dto.UserDto;
import luis.fluoxetina.chatwebsocket.mapper.UserMapper;
import luis.fluoxetina.chatwebsocket.model.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Log4j2
@CrossOrigin({"*"})
public class UserController {
  private final UserService userService;
  private final UserMapper userMapper;

  @GetMapping("/api/v1/user-online")
  public List<UserDto> findAllOnline(@RequestParam(defaultValue = "true") boolean online) {
    return userService.findAllByOnline(online).stream().map(userMapper::toDto).toList();
  }

  @GetMapping("/api/v1/user/{userId}")
  public UserDto findByUserId(@PathVariable String userId) {
    return userMapper.toDto(userService.findByUsername(userId));
  }
}