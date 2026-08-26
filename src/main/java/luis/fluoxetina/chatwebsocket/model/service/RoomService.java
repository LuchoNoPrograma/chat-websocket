package luis.fluoxetina.chatwebsocket.model.service;

import lombok.RequiredArgsConstructor;
import luis.fluoxetina.chatwebsocket.exception.EntityNotFoundException;
import luis.fluoxetina.chatwebsocket.model.doc.Room;
import luis.fluoxetina.chatwebsocket.model.repository.RoomRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomService {
  private final RoomRepository roomRepository;

  @Transactional
  public Room createRoom(Room room) {
    room.setName(room.getName().trim());
    room.setDescription(room.getDescription().trim());
    room.setActiveUsers(0);
    if (room.getTags() == null) room.setTags(new ArrayList<>());
    room.setCreatedAt(ZonedDateTime.now());
    return roomRepository.save(room);
  }

  @Transactional(readOnly = true)
  public Room findById(String id) {
    return roomRepository.findById(id)
      .orElseThrow(() -> new EntityNotFoundException("Room not found with id: " + id));
  }

  @Transactional(readOnly = true)
  public List<Room> findAll(String property, Sort.Direction direction) {
    return roomRepository.findAll(Sort.by(direction, property));
  }

  @Transactional
  public Room joinRoom(String roomId, String username) {
    if (roomRepository.incrementActiveUsers(roomId) == 0) {
      throw new EntityNotFoundException("Room not found with id: " + roomId);
    }
    return findById(roomId);
  }

  @Transactional
  public Room leaveRoom(String roomId, String username) {
    if (roomRepository.decrementActiveUsers(roomId) == 0) {
      throw new EntityNotFoundException("Room not found with id: " + roomId);
    }
    return findById(roomId);
  }
}
