---
name: chat-websocket-feature-integral
description: Implementar funcionalidades verticales de Chat WebSocket que crucen Vue, Pinia, REST, STOMP/WebSocket, Spring Boot o H2 en memoria. Usar al agregar o cambiar un recorrido completo de usuario; no usar para ajustes visuales aislados, mantenimiento de dependencias ni cambios confinados a una sola capa.
---

# Chat WebSocket Feature Integral

## Proposito

Coordinar features completas manteniendo alineados contratos HTTP/STOMP, estado reactivo, reglas del servidor y almacenamiento efímero.

## Alcance del proyecto

- Frontend Vue en `src/chat-frontend/src`, con rutas, componentes y stores Pinia.
- Backend Spring Boot en `src/main/java/luis/fluoxetina/chatwebsocket`, con endpoints REST, destinos STOMP y estado JPA efímero en H2.
- Contratos compartidos en DTO Java y tipos TypeScript; configuración de transporte en `WebSocketConfig` y estado de conexión en `src/chat-frontend/src/modern/stores/realtime.ts`.

## Fuentes

- Leer `AGENTS.md`, `pom.xml` y `src/chat-frontend/package.json` antes de delimitar el cambio.
- Seguir la ruta o componente de entrada, el store propietario, el controller, el service, la entidad JPA y el repository afectados.
- Leer `src/main/java/luis/fluoxetina/chatwebsocket/config/WebSocketConfig.java` y `src/main/java/luis/fluoxetina/chatwebsocket/event/WebSocketEventListener.java` cuando cambien conexión, suscripción o presencia.
- Leer `src/main/resources/application.yaml`, `src/main/resources/data.sql` y `src/main/java/luis/fluoxetina/chatwebsocket/lifecycle` cuando cambien datos iniciales, actividad o ciclo de vida.

## Patrones del proyecto

- `src/chat-frontend/src/modern/stores/realtime.ts` - `useRealtimeStore`: concentra REST, conexión STOMP, suscripción de sala, reconciliación de historial y errores de operación.
- `src/main/java/luis/fluoxetina/chatwebsocket/controller/RoomController.java` - `RoomController`: separa consultas REST de comandos STOMP y delega las reglas a servicios inyectados por constructor.
- `src/main/java/luis/fluoxetina/chatwebsocket/model/service/RoomService.java` - `RoomService`: posee almacenamiento y actualización atómica de contadores de sala.
- `src/main/resources/application.yaml` - `spring.datasource` y `app.data-reset`: crean H2 en memoria, inicializan SQL y fijan el límite de inactividad en 30 minutos.
- `src/main/java/luis/fluoxetina/chatwebsocket/lifecycle/IdleDataResetService.java` - `restoreInitialData`: limpia las tablas en orden seguro y reutiliza `data.sql` dentro de una transacción.

## Flujo

1. Recorrer primero el flujo actual de UI a almacenamiento y anotar destinos, payloads, respuestas y eventos derivados.
2. Definir criterios observables, capas afectadas, compatibilidad de contratos y exclusiones antes de editar.
3. Cambiar primero el contrato y la regla propietaria del servidor; después adaptar store y UI consumidores.
4. Conservar estados de carga, vacío, error, desconexión y responsive que apliquen al recorrido.

## Reglas

- No inventar endpoints, destinos STOMP, campos, tipos de mensaje ni estados; verificar ambos extremos del contrato.
- No mover reglas de sala, presencia o almacenamiento a componentes Vue.
- No conservar código del template MaterialPro si no es alcanzable ni requerido por el chat.
- No agregar recorridos con datos simulados ni textos que presenten la aplicación como muestra personal; la interfaz activa usa el backend real.
- No introducir un volumen ni un servicio de base externo sin alcance explícito; los datos de usuarios son deliberadamente efímeros.
- No prometer durabilidad en UI o contratos: el reinicio del proceso y el timeout restauran el estado inicial.
- No conservar en Pinia mensajes o salas temporales que el servidor ya eliminó; la hidratación posterior a una reconexión es la fuente de verdad.
- No ampliar orígenes CORS/WebSocket a comodines; usar la lista explícita configurable por entorno.

## Validacion

- Ejecutar `npm run typecheck`, `npm run lint` y `npm run build` en `src/chat-frontend`.
- Ejecutar `./mvnw test` en la raíz.
- Verificar en navegador el recorrido afectado en desktop y móvil, incluidos error y desconexión cuando correspondan.
- Para cambios de datos o tiempo real, verificar con dos clientes, reiniciar el backend y probar un timeout reducido para confirmar la restauración de `data.sql`.

## Autoevaluacion

- Los tipos TypeScript y DTO Java describen el mismo payload?
- El store y el service conservan ownership claro sin duplicar la regla?
- Las suscripciones se crean y liberan exactamente una vez?
