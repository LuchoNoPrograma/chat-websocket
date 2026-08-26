package luis.fluoxetina.chatwebsocket.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import luis.fluoxetina.chatwebsocket.dto.RoomDto;
import luis.fluoxetina.chatwebsocket.dto.UserDto;
import luis.fluoxetina.chatwebsocket.mapper.RoomMapper;
import luis.fluoxetina.chatwebsocket.model.doc.Room;
import luis.fluoxetina.chatwebsocket.model.service.ChatMessageService;
import luis.fluoxetina.chatwebsocket.model.service.RoomService;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Log4j2
public class RoomController {
  private final RoomService roomService;
  private final ChatMessageService chatMessageService;
  private final RoomMapper roomMapper;

  @MessageMapping("/room.create-room")
  @SendTo("/topic/room")
  public RoomDto createRoom(@Valid @Payload RoomDto roomDto, SimpMessageHeaderAccessor headerAccessor) {
    Room roomPersisted = roomService.createRoom(roomMapper.toDocument(roomDto));
    log.info("Room created: {}", roomPersisted);
    return roomMapper.toDto(roomPersisted);
  }

  @MessageMapping("/room.join-room/{roomId}")
  @SendTo("/topic/room")
  public RoomDto joinRoom(@Payload UserDto userDto, @DestinationVariable String roomId){
    log.info("User joined: {}", userDto);

    Room room = roomService.joinRoom(roomId, userDto.getUsername());
    return roomMapper.toDto(room);
  }

  @GetMapping("/api/v1/room")
  public ResponseEntity<List<RoomDto>> findAll(@RequestParam(defaultValue = "createdAt") String property,
                                               @RequestParam(defaultValue = "ASC") Sort.Direction direction) {
    return ResponseEntity.ok(roomService.findAll(property, direction).stream().map(roomMapper::toDtoWithUsers).toList());
  }

  @GetMapping("/api/v1/room/{id}")
  public ResponseEntity<RoomDto> findById(@PathVariable String id) {
    Room room = roomService.findById(id);
    room.setChatMessages(chatMessageService.findAllByRoomId(id));
    return ResponseEntity.ok(roomMapper.toDtoWithChatMessagesAndUsers(room));
  }
}
