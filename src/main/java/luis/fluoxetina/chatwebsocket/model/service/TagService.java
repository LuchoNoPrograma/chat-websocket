package luis.fluoxetina.chatwebsocket.model.service;

import lombok.RequiredArgsConstructor;
import luis.fluoxetina.chatwebsocket.model.doc.Tag;
import luis.fluoxetina.chatwebsocket.model.repository.TagRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TagService {
  private final TagRepository tagRepository;

  public List<Tag> findAll() {
    return tagRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
  }
}
