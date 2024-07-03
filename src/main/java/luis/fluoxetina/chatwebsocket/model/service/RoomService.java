package luis.fluoxetina.chatwebsocket.model.service;

import lombok.RequiredArgsConstructor;
import luis.fluoxetina.chatwebsocket.exception.EntityNotFoundException;
import luis.fluoxetina.chatwebsocket.model.doc.Room;
import luis.fluoxetina.chatwebsocket.model.repository.ChatMessageRepository;
import luis.fluoxetina.chatwebsocket.model.repository.RoomRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomService {
  private final RoomRepository roomRepository;
  private final ChatMessageRepository chatMessageRepository;

  public Room createRoom(Room room) {
    room.setCreatedAt(ZonedDateTime.now());
    return roomRepository.save(room);
  }

  public Room findById(String id) {
    return roomRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Room not found with id: " + id));
  }

  public List<Room> findAll(String property, Sort.Direction direction) {
    return roomRepository.findAll(Sort.by(direction, property));
  }
}
