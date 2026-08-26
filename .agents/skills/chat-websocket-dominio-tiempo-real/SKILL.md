---
name: chat-websocket-dominio-tiempo-real
description: Modificar las reglas de negocio de Chat WebSocket para identidad simple, presencia, salas, membresía, mensajes, historial y eventos JOIN/LEAVE. Usar cuando cambien invariantes o transiciones del chat en tiempo real; no usar para estilos, actualización de paquetes o transporte sin impacto funcional.
---

# Chat WebSocket Dominio de Tiempo Real

## Proposito

Proteger la semántica observable del chat mientras evolucionan presencia, salas y mensajes almacenados durante una sesión efímera en H2.

## Alcance del proyecto

- Identidad por `username`, estado `online` y ciclo de conexión/desconexión.
- Salas, etiquetas, contador `activeUsers`, suscripciones y eventos JOIN/LEAVE.
- Mensajes de sala temporales con `type`, `format`, `roomId`, `userId`, contenido y fecha.
- Restauración completa del estado inicial al arrancar o superar 30 minutos sin actividad REST, SockJS ni STOMP.

## Fuentes

- Leer entidades JPA, DTO, mappers, repositories y services de `src/main/java/luis/fluoxetina/chatwebsocket` para la entidad afectada.
- Leer `WebSocketEventListener`, `ChatController`, `RoomController` y `src/chat-frontend/src/modern/stores/realtime.ts` cuando la transición se propague en vivo.
- Contrastar `src/chat-frontend/src/modern/views/WorkspaceView.vue` para cambios en `MessageType` o `MessageFormat`.
- Leer `src/main/resources/application.yaml`, `src/main/resources/data.sql` y el paquete `lifecycle` cuando cambien ciclo de vida, datos iniciales o inactividad.

## Patrones del proyecto

- `src/main/java/luis/fluoxetina/chatwebsocket/event/WebSocketEventListener.java` - `processChatRoomActionType`: almacena JOIN/LEAVE, publica el mensaje y actualiza la sala como una sola transición observable.
- `src/main/java/luis/fluoxetina/chatwebsocket/model/service/UserService.java` - `connect` y `disconnect`: la identidad existente cambia presencia; una identidad nueva se crea en línea.
- `src/main/java/luis/fluoxetina/chatwebsocket/model/service/ChatMessageService.java` - `save`: asigna la fecha de creación solamente a mensajes nuevos.
- `src/main/java/luis/fluoxetina/chatwebsocket/model/repository/RoomRepository.java` - `incrementActiveUsers` y `decrementActiveUsers`: actualizan el contador atómicamente y el decremento nunca baja de cero.
- `src/main/java/luis/fluoxetina/chatwebsocket/model/repository/ChatMessageRepository.java` - `findByRoomIdOrderByCreatedAtAsc`: define el orden canónico del historial durante la sesión activa.
- `src/main/java/luis/fluoxetina/chatwebsocket/lifecycle/IdleDataResetService.java` - `markActivity` y `resetWhenIdle`: restauran `data.sql` antes de procesar la primera actividad posterior al límite y también mediante revisión programada.
- `src/main/java/luis/fluoxetina/chatwebsocket/lifecycle/ActivityTrackingFilter.java` - `shouldNotFilter`: limita el reloj de sesión a REST y SockJS; `WebSocketConfig.configureClientInboundChannel` añade la actividad STOMP.

## Flujo

1. Nombrar actores, estado inicial, comando, validaciones, cambio almacenado, publicaciones y estado final.
2. Localizar todos los productores y consumidores del tipo, campo o destino antes de cambiarlo.
3. Implementar la invariante en el service o listener propietario y mantener controllers y componentes delgados.
4. Cubrir repetición, desconexión, sala inexistente, payload inválido y reconexión cuando afecten la transición.

## Reglas

- Mantener JOIN y LEAVE como eventos del ciclo de suscripción, no como mensajes de chat enviados por el cliente.
- Actualizar `activeUsers` de forma atómica y evitar valores negativos o doble conteo al repetir eventos.
- Almacenar y emitir el mismo mensaje canónico; no fabricar versiones divergentes para REST y STOMP.
- Ejecutar la limpieza y la recarga de `data.sql` dentro de una sola transacción y antes de dejar avanzar la primera petición posterior al timeout.
- Considerar REST, transporte SockJS y frames STOMP como actividad; una comprobación de salud ajena al chat no debe prolongar la sesión.
- Al restaurar, eliminar usuarios, mensajes, salas y etiquetas de la sesión y volver exactamente a los datos iniciales.
- Después de una reconexión, hidratar salas desde el servidor antes de restaurar la suscripción y descartar mensajes cacheados; si una sala temporal ya no existe, volver al directorio.
- Tratar una suscripción repetida de la misma sesión como idempotente y marcar un alias offline solamente al cerrar su última conexión.
- Recortar y validar contenido e identidad en el servidor; la validación de Vue no constituye una invariante de negocio.
- No introducir autenticación fuerte, chats privados o membresía persistente sin alcance explícito, porque no existen en el flujo vigente.

## Validacion

- Ejecutar pruebas focalizadas de services, controllers o listener y luego `./mvnw test`.
- Ejecutar `npm run typecheck` si cambia un DTO, enum o evento consumido por Vue.
- Probar con dos sesiones la secuencia conectar, entrar, enviar, salir y desconectar sin duplicar presencia ni eventos.
- Reiniciar el backend y confirmar que solo reaparecen los datos de `data.sql`; usar un timeout reducido para comprobar que usuarios, salas y mensajes temporales desaparecen tras inactividad.

## Autoevaluacion

- La transición tiene un único propietario en servidor?
- Los reintentos o eventos duplicados pueden corromper presencia o contadores?
- El historial y la publicación en vivo conservan orden y forma compatibles?
