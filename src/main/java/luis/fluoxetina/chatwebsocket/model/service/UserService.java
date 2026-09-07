package luis.fluoxetina.chatwebsocket.model.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import luis.fluoxetina.chatwebsocket.exception.EntityNotFoundException;
import luis.fluoxetina.chatwebsocket.model.doc.User;
import luis.fluoxetina.chatwebsocket.model.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Log4j2
public class UserService {
  private static final String DEFAULT_AVATAR_ID = "claudia";
  private static final Set<String> AVATAR_IDS = Set.of(DEFAULT_AVATAR_ID, "sol", "luna", "jimenez", "dickson", "kimi");

  private final UserRepository userRepository;

  public User findByUsername(String username) throws EntityNotFoundException{
    return userRepository.findById(username).orElseThrow(() -> new EntityNotFoundException("User not found with username: " + username));
  }

  public User connect(User user) {
    User storedUser;
    try {
      storedUser = findByUsername(user.getUsername());
      storedUser.setOnline(true);
      storedUser.setAvatarId(normalizeAvatarId(user.getAvatarId()));
      userRepository.save(storedUser);
    } catch (EntityNotFoundException e) {
      storedUser = createUser(user);
    }
    return storedUser;
  }

  public User connect(String username) {
    return connect(username, DEFAULT_AVATAR_ID);
  }

  public User connect(String username, String avatarId) {
    User storedUser;
    String cleanAvatarId = normalizeAvatarId(avatarId);
    try {
      storedUser = findByUsername(username);
      storedUser.setOnline(true);
      storedUser.setAvatarId(cleanAvatarId);
      userRepository.save(storedUser);
    } catch (EntityNotFoundException e) {
      storedUser = createUser(User.builder()
        .username(username)
        .online(true)
        .avatarId(cleanAvatarId)
        .createdAt(ZonedDateTime.now())
        .build());
    }
    return storedUser;
  }

  public User disconnect(String username) {
    User storedUser = findByUsername(username);
    try {
      storedUser.setOnline(false);
      userRepository.save(storedUser);
    } catch (Exception e) {
      log.error(e.getMessage());
    }
    return storedUser;
  }

  public User createUser(User user) {
    user.setOnline(true);
    user.setAvatarId(normalizeAvatarId(user.getAvatarId()));
    return userRepository.save(user);
  }

  private String normalizeAvatarId(String avatarId) {
    return AVATAR_IDS.contains(avatarId) ? avatarId : DEFAULT_AVATAR_ID;
  }

  public List<User> findAllByOnline(boolean online) {
    return userRepository.findAllByOnline(online);
  }
}
