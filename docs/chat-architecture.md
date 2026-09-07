# Arquitectura de Chatty

Chat de texto con Vue/Pinia, Spring Boot, REST/STOMP y H2 en memoria. El alcance es un único proceso y una sesión efímera; no se añaden archivos, imágenes ni servicios distribuidos.

## Identidad y permisos

`POST /api/v1/auth?username=…&avatarId=…` entrega `{user, token, generation, expiresAt}`. El token opaco contiene 256 bits aleatorios; se transmite como `Authorization: Bearer …` en REST y en CONNECT STOMP. El alias nunca autentica una petición. El navegador conserva la credencial en sessionStorage, por pestaña. En despliegues públicos se requiere HTTPS/WSS.

`GET /api/v1/session` valida la credencial y devuelve usuario, generación y expiración. `POST /api/v1/logout` revoca la credencial. Los tokens vencen a las dos horas por defecto (`app.user-session.token-ttl`). El alias permanece reservado hasta el reinicio de datos, incluso tras cerrar sesión: evita que una credencial nueva herede el historial directo de ese alias. No hay recuperación de cuentas ni contraseña.

El interceptor HTTP exige credencial para `/api/**`, excepto apertura de sesión y política de sesión. El historial directo toma al solicitante de la sesión autenticada, nunca del parámetro `requester`. Las salas son públicas para las sesiones autenticadas. En STOMP se permiten únicamente los comandos y suscripciones declarados: no se puede publicar directamente a topics del broker ni suscribirse a colas de otro usuario. Enviar a una sala o confirmar sus mensajes exige la suscripción de esa conexión.

Las conexiones vencidas/revocadas se cierran mediante la revisión de sesiones, como máximo en el siguiente segundo. Una generación nueva invalida credenciales anteriores. La recuperación vuelve al acceso en lugar de apropiarse automáticamente del mismo alias.

## Envío y reintentos

Cada comando lleva `clientMessageId`, generado por el navegador una única vez. La clave única `(userId, clientMessageId)` y un bloqueo de la fila del autor serializan los comandos concurrentes de esa persona. Repetir el mismo comando devuelve el mensaje original; reutilizar el ID con otro cuerpo, destino o cita se rechaza.

El servidor valida texto y destino, guarda el mensaje, confirma la transacción y publica. `/user/queue/accepted` devuelve el mensaje canónico, incluido su `clientMessageId`. El frontend relaciona confirmaciones por ID, nunca por coincidencia de texto. El componente conserva el borrador hasta esa aceptación.

El outbox de la pestaña conserva los comandos pendientes en sessionStorage, asociados a generación y usuario. La reconexión reintenta con los IDs originales; un pendiente de sala se reenvía al restaurar/abrir esa sala. También existe reintento manual tras ocho segundos sin confirmación. Cambiar la identidad o invalidar la generación elimina los pendientes anteriores.

La garantía es **un registro por comando**, no entrega distribuida exactamente una vez. Un fallo del proceso entre commit y publicación no se resuelve mediante un broker externo: el cliente recupera el estado desde el historial. Al ser H2 efímero, un reinicio pierde toda la sesión. Esta limitación es deliberada y comprobable.

## Entrega y lectura

`/ws/chat.delivered-message` y `/ws/chat.read-message` reciben `{messageId}`. La sesión determina la persona y el servidor fija las primeras fechas. Leer implica entrega si aún no se había confirmado. Las repeticiones conservan ambas fechas.

Los eventos `MESSAGE_DELIVERED` y `MESSAGE_READ` viajan por `/user/queue/receipts`, solo al autor y a quien confirma. Incluyen mensaje, conversación, persona y fechas; no retransmiten el cuerpo ni disparan notificaciones de mensaje nuevo. REST incluye los mapas completos para reconstruir el estado. El cliente mezcla confirmaciones sin perder una lectura al recibir una entrega atrasada.

Lectura significa visualización con la pestaña activa, no prueba de comprensión. Los textos largos requieren expansión y mostrar su final. En salas no existe «todos leyeron»: son públicas y no tienen una lista inmutable de destinatarios.

## Recuperación y estado efímero

La reconexión valida primero sesión/generación, suscribe eventos, hidrata directorios y recupera el historial paginado de la conversación. REST se combina con eventos recibidos durante la petición y se deduplica por ID. Este diseño usa una instantánea reciente para reconciliar también recibos; no afirma disponer de un log durable ni de un cursor incremental global.

El reinicio y el timeout de datos limpian mensajes, confirmaciones, usuarios y salas temporales antes de restaurar `data.sql`. La generación cambia después de completar la transacción. El cliente invalida cachés y pendientes al detectar la sesión caducada. El backend rechaza comandos de conexiones de la generación anterior.

## Responsabilidades

- `session/`: emisión y validación de credenciales, generación, autorización HTTP, conexiones y caducidad.
- `messaging/MessageCommands`: aceptación idempotente, transacciones y validación del comando.
- `ChatMessageService`: contenido, citas, historial y confirmaciones almacenadas.
- `ChatController`: adaptación STOMP y publicación tras la transacción.
- `services/session.ts`: credenciales y HTTP; `services/outbox.ts`: pendientes; `services/messages.ts`: reconciliación; store Pinia: coordinación reactiva y transporte.
- Componentes Vue: presentación, borrador e interacción accesible.

## Observabilidad y verificación

Micrometer registra `chat.messages{result=created|replayed}`, `chat.receipts{type=…}`, `chat.commands.rejected` y `chat.connections.active`, sin aliases, tokens ni cuerpos como etiquetas. Los endpoints públicos de Actuator siguen limitados a salud/info. Las métricas pueden exportarse mediante configuración de operación sin publicar credenciales.

Ejecutar `bash ./mvnw test` y, desde `src/chat-frontend`, `npm run typecheck`, `npm run lint`, `npm run build`. Las pruebas comprueban permisos REST, alias reservado, revocación, generación, seis envíos concurrentes con el mismo ID, conflicto de contenido, historial, recibos y restauración H2.

Verificación de navegador: dos sesiones con aliases distintos; enviar en sala/directo; abrir detalles y observar entrega antes de lectura; desconectar al emisor y reconectar; verificar un único mensaje y confirmación; reiniciar el servidor y comprobar que se vuelve al acceso. Repetir en escritorio/móvil y claro/oscuro.

## Evidencia de validación

- Suite Java: 19 pruebas aprobadas con `bash ./mvnw test`, incluidas las pruebas de arquitectura con servidor real y las pruebas de dominio existentes.
- Frontend: typecheck, lint y build aprobados.
- `scripts/verify-chat.py`: protocolo REST/STOMP real, conexión sin token rechazada, consulta directa sin identidad rechazada, aceptación canónica, repetición del mismo comando antes/después de reconectar con un único registro, entrega/lectura sin retransmitir el cuerpo, publicación al broker y suscripción a cola cruda rechazadas, revocación con cierre de socket.
- Navegador: dos contextos independientes, corte de red de uno, mensaje enviado durante el corte recuperado una sola vez, nuevo envío confirmado una sola vez y lectura del otro usuario actualizada en detalles. Desktop y móvil de 390 px sin desbordamiento horizontal; claro y oscuro.
- Reinicio real: el cliente rechaza la generación previa, borra credencial y outbox y vuelve al acceso. Timeout de datos reducido a cinco segundos: token anterior rechazado y nueva generación con las tres salas iniciales.

El evento `offline` del navegador fuerza el cierre del transporte para iniciar la recuperación, ya que algunos navegadores mantienen un WebSocket aparentemente abierto al cambiar el estado de red.
