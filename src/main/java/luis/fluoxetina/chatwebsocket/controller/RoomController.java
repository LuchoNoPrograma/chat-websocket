package luis.fluoxetina.chatwebsocket.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import luis.fluoxetina.chatwebsocket.dto.RoomDto;
import luis.fluoxetina.chatwebsocket.mapper.RoomMapper;
import luis.fluoxetina.chatwebsocket.model.doc.Room;
import luis.fluoxetina.chatwebsocket.model.service.ChatMessageService;
import luis.fluoxetina.chatwebsocket.model.service.RoomService;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Log4j2
@CrossOrigin({"*"})
public class RoomController {
  private final RoomService roomService;
  private final ChatMessageService chatMessageService;
  private final RoomMapper roomMapper;
  private final SimpMessagingTemplate messagingTemplate;

  @MessageMapping("/room.create-room")
  @SendTo("/topic/room")
  public RoomDto createRoom(@Payload RoomDto roomDto, SimpMessageHeaderAccessor headerAccessor) {
    Room roomPersisted = roomService.createRoom(roomMapper.toDocument(roomDto));
    log.info("Room created: {}", roomPersisted);
    return roomMapper.toDto(roomPersisted);
  }

  @MessageMapping("/room.send-image")
  public void sendImageBytes(@Payload String message) {
    log.info("Image sent");
    messagingTemplate.convertAndSend("/topic/room", message);
  }

  @GetMapping("/api/v1/room")
  public ResponseEntity<List<RoomDto>> findAll(@RequestParam(defaultValue = "createdAt") String property,
                                               @RequestParam(defaultValue = "ASC") Sort.Direction direction) {
    return ResponseEntity.ok(roomService.findAll(property, direction).stream().map(roomMapper::toDtoWithUsers).toList());
  }

  @GetMapping("/api/v1/room/{id}")
  public ResponseEntity<RoomDto> findById(@PathVariable String id) {
    Room room = roomService.findById(id);
    room.setChatMessages(chatMessageService.findAllByRoomId(new ObjectId(room.getId())));
    return ResponseEntity.ok(roomMapper.toDtoWithChatMessagesAndUsers(room));
  }
}
