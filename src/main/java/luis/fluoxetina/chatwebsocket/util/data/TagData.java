package luis.fluoxetina.chatwebsocket.util.data;

import luis.fluoxetina.chatwebsocket.model.doc.Tag;

import java.util.List;

public class TagData {
  public static List<Tag> tags() {
    return List.of(
      Tag.builder().name("Conversaciónes").pathIcon("mdi-web").build(),
      Tag.builder().name("Videojuegos").pathIcon("mdi-controller-classic").build(),
      Tag.builder().name("Arte").pathIcon("mdi-palette").build(),
      Tag.builder().name("Anime y Manga").pathIcon("mdi-animation-play-outline").build(),
      Tag.builder().name("Series").pathIcon("mdi-youtube-tv").build(),
      Tag.builder().name("XD").pathIcon("mdi-emoticon-excited-outline").build(),
      Tag.builder().name("Otros").pathIcon("mdi-cellphone-play").build()
    );
  }
}
