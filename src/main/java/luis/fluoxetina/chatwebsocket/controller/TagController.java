package luis.fluoxetina.chatwebsocket.controller;

import lombok.RequiredArgsConstructor;
import luis.fluoxetina.chatwebsocket.dto.TagDto;
import luis.fluoxetina.chatwebsocket.mapper.TagMapper;
import luis.fluoxetina.chatwebsocket.model.service.TagService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@CrossOrigin
public class TagController {
  private final TagService tagService;
  private final TagMapper tagMapper;

  @GetMapping("/api/v1/tag")
  public ResponseEntity<List<TagDto>> findAll() {
    return ResponseEntity.ok(tagService.findAll().stream().map(tagMapper::toDto).toList());
  }
}
