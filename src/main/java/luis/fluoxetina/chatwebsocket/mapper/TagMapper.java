package luis.fluoxetina.chatwebsocket.mapper;

import luis.fluoxetina.chatwebsocket.dto.TagDto;
import luis.fluoxetina.chatwebsocket.model.doc.Tag;
import org.springframework.stereotype.Component;


@Component
public class TagMapper implements MapperBase<Tag, TagDto> {
  @Override
  public Tag toDocument(TagDto dto) {
    return Tag.builder()
      .id(dto.getId())
      .name(dto.getName())
      .pathIcon(dto.getPathIcon())
      .build();
  }

  @Override
  public TagDto toDto(Tag document) {
    return TagDto.builder()
      .id(document.getId())
      .name(document.getName())
      .pathIcon(document.getPathIcon())
      .build();
  }
}
