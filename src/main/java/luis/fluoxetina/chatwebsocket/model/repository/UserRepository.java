package luis.fluoxetina.chatwebsocket.model.repository;

import luis.fluoxetina.chatwebsocket.model.doc.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRepository extends JpaRepository<User, String> {
  @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @org.springframework.data.jpa.repository.Query("select u from User u where u.username = :username")
  java.util.Optional<User> lockForMessage(@org.springframework.data.repository.query.Param("username") String username);
  List<User> findAllByOnline(Boolean online);
}
