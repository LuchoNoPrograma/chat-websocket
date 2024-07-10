package luis.fluoxetina.chatwebsocket.model.service;

import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import lombok.RequiredArgsConstructor;
import luis.fluoxetina.chatwebsocket.exception.EntityNotFoundException;
import luis.fluoxetina.chatwebsocket.model.doc.Room;
import luis.fluoxetina.chatwebsocket.model.repository.ChatMessageRepository;
import luis.fluoxetina.chatwebsocket.model.repository.RoomRepository;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomService {
  private final MongoTemplate mongoTemplate;
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

  //References for findAndUpdate in MongoDB: https://docs.spring.io/spring-data/mongodb/reference/mongodb/template-crud-operations.html
  public Room joinRoom(String roomId, String username) {
    Query query = new Query(Criteria.where("_id").is(new ObjectId(roomId)));
    Update update = new Update().inc("activeUsers", 1);
    return mongoTemplate.update(Room.class)
            .matching(query)
            .apply(update)
            .withOptions(FindAndModifyOptions.options().returnNew(true))
            .findAndModifyValue();
  }
}