package luis.fluoxetina.chatwebsocket.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(luis.fluoxetina.chatwebsocket.session.SessionAccessException.class)
  public ResponseEntity<ApiError> handleSession(luis.fluoxetina.chatwebsocket.session.SessionAccessException exception) {
    return error(exception.status(), exception.code(), exception.getMessage(), Map.of());
  }


  @ExceptionHandler(EntityNotFoundException.class)
  public ResponseEntity<ApiError> handleNotFound(EntityNotFoundException exception) {
    return error(HttpStatus.NOT_FOUND, "NOT_FOUND", exception.getMessage(), Map.of());
  }

  @ExceptionHandler({IllegalArgumentException.class, ConstraintViolationException.class})
  public ResponseEntity<ApiError> handleBadRequest(RuntimeException exception) {
    return error(HttpStatus.BAD_REQUEST, "BAD_REQUEST", exception.getMessage(), Map.of());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception) {
    Map<String, String> fields = new LinkedHashMap<>();
    exception.getBindingResult().getFieldErrors()
      .forEach(fieldError -> fields.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage()));
    return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request validation failed", fields);
  }

  private ResponseEntity<ApiError> error(HttpStatus status, String code, String message,
                                         Map<String, String> fields) {
    return ResponseEntity.status(status)
      .body(new ApiError(Instant.now(), status.value(), code, message, fields));
  }
}
