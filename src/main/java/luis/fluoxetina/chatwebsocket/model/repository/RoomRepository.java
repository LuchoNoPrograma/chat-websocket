package luis.fluoxetina.chatwebsocket.model.repository;

import luis.fluoxetina.chatwebsocket.model.doc.Room;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;

//Atomic operations with MongoDB: keyword $inc
//References: https://docs.spring.io/spring-data/mongodb/reference/mongodb/repositories/modifying-methods.html
public interface RoomRepository extends MongoRepository<Room, String> {
  @Query("{'_id': ?0}")
  @Update("{'$inc': {'activeUsers': 1}}")
  void findAndIncrementActiveUsersById(ObjectId roomId);
}