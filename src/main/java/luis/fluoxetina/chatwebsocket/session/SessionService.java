package luis.fluoxetina.chatwebsocket.session;

import luis.fluoxetina.chatwebsocket.dto.UserDto;
import luis.fluoxetina.chatwebsocket.mapper.UserMapper;
import luis.fluoxetina.chatwebsocket.model.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

/** Opaque credentials belong to an alias for one ephemeral database generation. */
@Service
public class SessionService {
  public record Session(String username, String avatarId, String generation, Instant expiresAt) {}
  public record Login(UserDto user, String token, String generation, Instant expiresAt) {}
  private final SessionGeneration generation;
  private final UserService users;
  private final UserMapper mapper;
  private final Duration ttl;
  private final SecureRandom random = new SecureRandom();
  private final Map<String, Session> sessions = new HashMap<>();
  private final Set<String> claimedAliases = new HashSet<>();
  private String knownGeneration;

  public SessionService(SessionGeneration generation, UserService users, UserMapper mapper,
      @Value("${app.user-session.token-ttl:PT2H}") Duration ttl) {
    this.generation = generation; this.users = users; this.mapper = mapper; this.ttl = ttl;
    if (ttl.isNegative() || ttl.isZero()) throw new IllegalArgumentException("Token TTL must be positive");
  }

  private void refreshGeneration() {
    if (!generation.current().equals(knownGeneration)) {
      sessions.clear(); claimedAliases.clear(); knownGeneration = generation.current();
    }
    sessions.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(Instant.now()));
  }

  public synchronized Login create(String alias, String avatarId) {
    refreshGeneration();
    String username = alias == null ? "" : alias.trim();
    if (!username.matches("[\\p{L}\\p{N}_.-]{3,20}")) {
      throw new IllegalArgumentException("Usa entre 3 y 20 letras, números, puntos, guiones o guiones bajos.");
    }
    // Keep aliases reserved after logout/expiry so a new credential cannot inherit old direct history.
    if (claimedAliases.contains(username)) throw new SessionAccessException(HttpStatus.CONFLICT,
      "ALIAS_TAKEN", "Ese alias ya está reservado. Elige otro o vuelve desde tu sesión original.");
    byte[] bytes = new byte[32]; random.nextBytes(bytes);
    String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    var user = users.connect(username, avatarId);
    users.disconnect(username); // Presence starts only when STOMP connects.
    Session session = new Session(username, user.getAvatarId(), knownGeneration, Instant.now().plus(ttl));
    sessions.put(token, session); claimedAliases.add(username);
    return new Login(mapper.toDto(user), token, knownGeneration, session.expiresAt());
  }

  public synchronized Session require(String authorization) {
    refreshGeneration();
    String token = authorization != null && authorization.startsWith("Bearer ") ? authorization.substring(7) : "";
    Session session = sessions.get(token);
    if (session == null) throw new SessionAccessException(HttpStatus.UNAUTHORIZED,
      "SESSION_EXPIRED", "Tu sesión terminó. Vuelve a entrar con un alias.");
    return session;
  }

  public synchronized void revoke(String authorization) {
    require(authorization);
    sessions.remove(authorization.substring(7));
  }
  public Login describe(String authorization) {
    Session session = require(authorization);
    return new Login(mapper.toDto(users.findByUsername(session.username())), null,
      session.generation(), session.expiresAt());
  }
}
