INSERT INTO room_tags (id, name, path_icon) VALUES
  ('tag-conversaciones', 'Conversaciones', 'mdi-web'),
  ('tag-videojuegos', 'Videojuegos', 'mdi-controller-classic'),
  ('tag-programacion', 'Programación', 'mdi-code-tags'),
  ('tag-arte', 'Arte', 'mdi-palette'),
  ('tag-anime-manga', 'Anime y Manga', 'mdi-animation-play-outline'),
  ('tag-series', 'Series', 'mdi-youtube-tv'),
  ('tag-humor', 'Humor', 'mdi-emoticon-excited-outline'),
  ('tag-otros', 'Otros', 'mdi-cellphone-play');

INSERT INTO chat_users (username, online, created_at) VALUES
  ('luna', FALSE, CURRENT_TIMESTAMP),
  ('mika', FALSE, CURRENT_TIMESTAMP),
  ('nico.dev', FALSE, CURRENT_TIMESTAMP),
  ('santi', FALSE, CURRENT_TIMESTAMP),
  ('vale', FALSE, CURRENT_TIMESTAMP);

INSERT INTO chat_rooms (id, name, description, active_users, img_portrait, created_at) VALUES
  ('room-cero-contexto', 'Cero contexto', '¿Esto va aquí? Sí. Música, memes y cosas que no sabías a quién contarle.', 0, NULL, CURRENT_TIMESTAMP),
  ('room-dev-chill', 'Dev & chill', 'Comparte lo que estás haciendo o ese error que ya leíste veinte veces.', 0, NULL, CURRENT_TIMESTAMP),
  ('room-busco-parche', '¿Sale algo?', 'Una partida, una peli o hablar un rato. Propón un plan y ve quién se suma.', 0, NULL, CURRENT_TIMESTAMP);

INSERT INTO chat_room_tags (room_id, tag_id) VALUES
  ('room-cero-contexto', 'tag-conversaciones'),
  ('room-cero-contexto', 'tag-humor'),
  ('room-dev-chill', 'tag-programacion'),
  ('room-busco-parche', 'tag-conversaciones');

INSERT INTO chat_messages (id, room_id, user_id, body, type, format, created_at) VALUES
  ('msg-cero-contexto-1', 'room-cero-contexto', 'luna', 'llevo todo el día con la misma canción. pasen algo antes de que la odie', 'CHAT', 'TEXT', DATEADD('MINUTE', -6, CURRENT_TIMESTAMP)),
  ('msg-cero-contexto-2', 'room-cero-contexto', 'mika', 'cuál es? necesito saber si ya estoy igual', 'CHAT', 'TEXT', DATEADD('MINUTE', -5, CURRENT_TIMESTAMP)),
  ('msg-dev-chill-1', 'room-dev-chill', 'nico.dev', 'arreglé el bug y ahora no sé por qué funciona 😭', 'CHAT', 'TEXT', DATEADD('MINUTE', -4, CURRENT_TIMESTAMP)),
  ('msg-dev-chill-2', 'room-dev-chill', 'santi', 'pasa el diff, ahora necesito saber qué cambiaste', 'CHAT', 'TEXT', DATEADD('MINUTE', -3, CURRENT_TIMESTAMP)),
  ('msg-busco-parche-1', 'room-busco-parche', 'vale', 'alguien para una partida esta noche? aviso que juego mal pero me conecto puntual', 'CHAT', 'TEXT', DATEADD('MINUTE', -2, CURRENT_TIMESTAMP)),
  ('msg-busco-parche-2', 'room-busco-parche', 'mika', 'yo estoy. con lo de puntual ya me ganaste, a qué jugamos?', 'CHAT', 'TEXT', DATEADD('MINUTE', -1, CURRENT_TIMESTAMP));
