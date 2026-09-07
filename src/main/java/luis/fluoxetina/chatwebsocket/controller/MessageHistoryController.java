package luis.fluoxetina.chatwebsocket.controller;

import lombok.RequiredArgsConstructor;
import luis.fluoxetina.chatwebsocket.dto.MessagePageDto;
import luis.fluoxetina.chatwebsocket.mapper.ChatMessageMapper;
import luis.fluoxetina.chatwebsocket.model.service.ChatMessageService;
import luis.fluoxetina.chatwebsocket.model.service.RoomService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MessageHistoryController {
  private final ChatMessageService chatMessageService;
  private final ChatMessageMapper chatMessageMapper;
  private final RoomService roomService;

  @GetMapping("/api/v1/room/{roomId}/messages")
  public MessagePageDto findRoomMessages(@PathVariable String roomId,
                                         @RequestParam(required = false) String before,
                                         @RequestParam(defaultValue = "30") Integer size) {
    roomService.findById(roomId);
    return toDto(chatMessageService.findRoomHistory(roomId, before, size));
  }

  @GetMapping("/api/v1/dm/{peerUsername}/messages")
  public MessagePageDto findDirectMessages(@PathVariable String peerUsername,
                                           @org.springframework.web.bind.annotation.RequestAttribute("chatSession") luis.fluoxetina.chatwebsocket.session.SessionService.Session session,
                                           @RequestParam(required = false) String before,
                                           @RequestParam(defaultValue = "30") Integer size) {
    String cleanRequester = session.username();
    String cleanPeer = peerUsername.trim();
    return toDto(chatMessageService.findDirectHistory(cleanRequester, cleanPeer, before, size));
  }

  private MessagePageDto toDto(ChatMessageService.HistoryPage page) {
    return MessagePageDto.builder()
      .messages(page.messages().stream().map(chatMessageMapper::toDto).toList())
      .nextCursor(page.nextCursor())
      .hasMore(page.hasMore())
      .build();
  }
}
