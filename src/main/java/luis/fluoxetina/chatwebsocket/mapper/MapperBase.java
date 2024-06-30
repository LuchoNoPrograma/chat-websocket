package luis.fluoxetina.chatwebsocket.mapper;

import org.springframework.stereotype.Component;

public interface MapperBase <D, T> {
  D toDocument(T document);
  T toDto(D document);
}
