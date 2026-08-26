package luis.fluoxetina.chatwebsocket.model.repository;

import luis.fluoxetina.chatwebsocket.model.doc.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TagRepository extends JpaRepository<Tag, String> {
}
