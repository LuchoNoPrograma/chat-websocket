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
  ('room-cero-contexto', 'Cero contexto', 'La general pa hablar de lo que salga: música, memes, chisme sano y preguntas random.', 0, NULL, CURRENT_TIMESTAMP),
  ('room-dev-chill', 'Dev & chill', 'Código, proyectos y bugs; cero entrevista técnica, puro compartir.', 0, NULL, CURRENT_TIMESTAMP),
  ('room-busco-parche', 'Busco parche', 'Pa conocer gente, encontrar tu misma vibra y dejar de lurkear.', 0, NULL, CURRENT_TIMESTAMP);

INSERT INTO chat_room_tags (room_id, tag_id) VALUES
  ('room-cero-contexto', 'tag-conversaciones'),
  ('room-cero-contexto', 'tag-humor'),
  ('room-dev-chill', 'tag-programacion'),
  ('room-busco-parche', 'tag-conversaciones');

INSERT INTO chat_messages (id, room_id, user_id, body, type, format, created_at) VALUES
  ('msg-cero-contexto-1', 'room-cero-contexto', 'luna', 'holaa, pregunta seria: ¿qué canción tienen pegada últimamente? 👀', 'CHAT', 'TEXT', DATEADD('MINUTE', -6, CURRENT_TIMESTAMP)),
  ('msg-cero-contexto-2', 'room-cero-contexto', 'mika', 'yo vine a mirar dos minutos y ya me quedé, clásico jsjs', 'CHAT', 'TEXT', DATEADD('MINUTE', -5, CURRENT_TIMESTAMP)),
  ('msg-dev-chill-1', 'room-dev-chill', 'nico.dev', 'ando armando una app y necesito ojos que me digan si está god o si me estoy mintiendo 😭', 'CHAT', 'TEXT', DATEADD('MINUTE', -4, CURRENT_TIMESTAMP)),
  ('msg-dev-chill-2', 'room-dev-chill', 'santi', 'pásala, acá revisamos sin entrevista técnica. yo ando peleando con Vue y Spring', 'CHAT', 'TEXT', DATEADD('MINUTE', -3, CURRENT_TIMESTAMP)),
  ('msg-busco-parche-1', 'room-busco-parche', 'vale', 'holi, soy nueva por acá. busco gente pa hablar de música, pelis y cualquier bobada', 'CHAT', 'TEXT', DATEADD('MINUTE', -2, CURRENT_TIMESTAMP)),
  ('msg-busco-parche-2', 'room-busco-parche', 'mika', 'caíste bien jaja. ¿team plan tranqui o salir a tocar pasto?', 'CHAT', 'TEXT', DATEADD('MINUTE', -1, CURRENT_TIMESTAMP));
