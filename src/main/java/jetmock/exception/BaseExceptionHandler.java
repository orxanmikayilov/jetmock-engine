package jetmock.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

@Slf4j
@RestControllerAdvice
public class BaseExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex, WebRequest request) {
    log.error("Request - {}, Exception:", request.toString(), ex);

    List<ValidationError> checks = new ArrayList<>();
    ex.getBindingResult().getFieldErrors().forEach(
        fieldError -> checks.add(new ValidationError(
            fieldError.getField(), fieldError.getDefaultMessage())));

    RestErrorResponse response = RestErrorResponse.builder()
        .uuid(UUID.randomUUID().toString())
        .code("VALIDATION_EXCEPTION")
        .message("Invalid arguments")
        .checks(checks)
        .build();
    return ResponseEntity.badRequest().body(response);
  }

  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  @ExceptionHandler(Exception.class)
  public RestErrorResponse handleUnexpectedError(Exception ex) {
    log.error("Unexpected exception", ex);
    return RestErrorResponse.builder()
        .uuid(UUID.randomUUID().toString())
        .code("UNEXPECTED_ERROR")
        .message(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
        .build();
  }

  @ExceptionHandler(BaseException.class)
  public ResponseEntity<RestErrorResponse> handleBaseException(
      BaseException ex) {
    log.error("BaseException: ", ex);
    RestErrorResponse response = RestErrorResponse.builder()
        .uuid(ex.getUuid())
        .code(ex.getCode())
        .message(ex.getMessage())
        .checks(ex.getChecks())
        .build();
    return ResponseEntity.status(ex.getStatus()).body(response);
  }

  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler(ConstraintViolationException.class)
  public RestErrorResponse handleConstraintViolationException(
      ConstraintViolationException ex) {
    log.error("ValidationException: ", ex);
    Set<ConstraintViolation<?>> constraintViolations = ex.getConstraintViolations();
    List<ValidationError> checks = new ArrayList<>();

    constraintViolations.forEach(
        constraintViolation -> checks.add(new ValidationError(
            constraintViolation.getPropertyPath().toString(), constraintViolation.getMessage())));

    return RestErrorResponse.builder()
        .uuid(UUID.randomUUID().toString())
        .code("VALIDATION_EXCEPTION")
        .message(ex.getMessage())
        .checks(checks)
        .build();
  }

}
