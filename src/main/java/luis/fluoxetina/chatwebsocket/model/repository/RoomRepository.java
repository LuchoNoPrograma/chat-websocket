package luis.fluoxetina.chatwebsocket.model.repository;

import luis.fluoxetina.chatwebsocket.model.doc.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomRepository extends JpaRepository<Room, String> {
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("update Room room set room.activeUsers = coalesce(room.activeUsers, 0) + 1 where room.id = :roomId")
  int incrementActiveUsers(@Param("roomId") String roomId);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("""
    update Room room
    set room.activeUsers = case
      when coalesce(room.activeUsers, 0) > 0 then room.activeUsers - 1
      else 0
    end
    where room.id = :roomId
    """)
  int decrementActiveUsers(@Param("roomId") String roomId);
}
