# Chat WebSocket

## Skills del proyecto

- Usa `.agents/skills/chat-websocket-feature-integral/SKILL.md` para funcionalidades que crucen frontend, REST/STOMP, backend o H2.
- Usa `.agents/skills/chat-websocket-dominio-tiempo-real/SKILL.md` cuando cambien reglas de identidad, presencia, salas, suscripciones, mensajes o historial.
- Para un cambio que active ambas, lee primero la skill de dominio y después la skill integral.

## Estructura

- Backend Spring Boot: `src/main/java/luis/fluoxetina/chatwebsocket`.
- Frontend Vue: `src/chat-frontend`.
- Estado efímero embebido: H2 en memoria mediante JPA, inicializado por `src/main/resources/data.sql` en cada arranque.
- Después de 30 minutos sin actividad REST, SockJS o STOMP, la siguiente comprobación o petición elimina los datos de sesión y restaura `data.sql` antes de continuar.
- El contrato del tiempo real se reparte entre DTO y enums Java, tipos TypeScript, destinos STOMP y topics.
- La interfaz activa usa siempre REST/STOMP contra el backend; no mantengas recorridos alternativos con datos simulados ni mensajes que presenten la aplicación como muestra personal.

## Comprobaciones

- Backend: `bash ./mvnw test`.
- Frontend: `npm run typecheck`, `npm run lint` y `npm run build` desde `src/chat-frontend`.
- Verifica en navegador los cambios visibles en desktop y móvil.

Conserva cambios concurrentes y no incluyas `.env`, credenciales ni artefactos generados.
