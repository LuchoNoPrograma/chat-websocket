package luis.fluoxetina.chatwebsocket.mapper;

import luis.fluoxetina.chatwebsocket.dto.UserDto;
import luis.fluoxetina.chatwebsocket.model.doc.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
  public UserDto toDto(User user) {
    return UserDto.builder()
      .username(user.getUsername())
      .online(user.isOnline())
      .avatarId(user.getAvatarId())
      .build();
  }

  public User toDocument(UserDto userDto) {
    return User.builder()
      .username(userDto.getUsername())
      .online(userDto.isOnline())
      .avatarId(userDto.getAvatarId())
      .build();
  }
}
