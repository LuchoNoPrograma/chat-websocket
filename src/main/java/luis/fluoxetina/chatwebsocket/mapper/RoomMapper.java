package luis.fluoxetina.chatwebsocket.mapper;

import lombok.RequiredArgsConstructor;
import luis.fluoxetina.chatwebsocket.dto.RoomDto;
import luis.fluoxetina.chatwebsocket.model.doc.Room;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoomMapper {
  private final UserMapper userMapper;
  private final ChatMessageMapper chatMessageMapper;
  private final TagMapper topicMapper;

  public RoomDto toDto(Room room) {
    return RoomDto.builder()
      .id(room.getId())
      .name(room.getName())
      .description(room.getDescription())
      .activeUsers(room.getActiveUsers())
      .imgPortrait(room.getImgPortrait())
      .createdAt(room.getCreatedAt())
      .tags(room.getTags().stream().map(topicMapper::toDto).toList())
      .build();
  }

  public Room toDocument(RoomDto roomDto) {
    return Room.builder()
      .id(roomDto.getId())
      .name(roomDto.getName())
      .description(roomDto.getDescription())
      .activeUsers(roomDto.getActiveUsers())
      .imgPortrait(roomDto.getImgPortrait())
      .createdAt(roomDto.getCreatedAt())
      .tags(roomDto.getTags().stream().map(topicMapper::toDocument).toList())
      .build();
  }

  public RoomDto toDtoWithUsers(Room room) {
    RoomDto roomDto = toDto(room);
    if (room.getUsers() != null) {
      roomDto.setUsers(room.getUsers().stream().map(userMapper::toDto).toList());
    }
    return roomDto;
  }

  public RoomDto toDtoWithChatMessages(Room room) {
    RoomDto roomDto = toDto(room);
    if (room.getChatMessages() != null) {
      roomDto.setChatMessages(room.getChatMessages().stream().map(chatMessageMapper::toDto).toList());
    }

    return roomDto;
  }

  public RoomDto toDtoWithChatMessagesAndUsers(Room room) {
    RoomDto roomDto = toDtoWithChatMessages(room);
    if (room.getUsers() != null) {
      roomDto.setUsers(room.getUsers().stream().map(userMapper::toDto).toList());
    }
    if(room.getChatMessages() != null) {
      roomDto.setChatMessages(room.getChatMessages().stream().map(chatMessageMapper::toDto).toList());
    }

    return roomDto;
  }

  public Room toDocumentWithUsers(RoomDto roomDto) {
    Room room = toDocument(roomDto);
    room.setUsers(roomDto.getUsers().stream().map(userMapper::toDocument).toList());
    return room;
  }
}
