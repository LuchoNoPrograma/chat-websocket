package luis.fluoxetina.chatwebsocket.model.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import luis.fluoxetina.chatwebsocket.model.doc.Tag;
import luis.fluoxetina.chatwebsocket.model.repository.TagRepository;
import luis.fluoxetina.chatwebsocket.util.data.TagData;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Service
@RequiredArgsConstructor
@Log4j2
public class TagService {
  private final TagRepository tagRepository;

  @PostConstruct
  public void initData() {
    if (tagRepository.count() == 0) {
      log.info("Creating default tags...");
      tagRepository.saveAll(TagData.tags());
    }
  }

  public List<Tag> findAll() {
    return tagRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
  }
}
