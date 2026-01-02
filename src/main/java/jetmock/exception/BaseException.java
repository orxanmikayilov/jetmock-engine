package jetmock.exception;

import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BaseException extends RuntimeException {

  private final String uuid;
  private final Integer status;
  private final String code;
  private final String message;
  private final transient List<ValidationError> checks;

  public BaseException(Integer status, String code, String message) {
    this(status, code, message, null);
  }

  public BaseException(Integer status, String code, String message, List<ValidationError> checks) {
    this(UUID.randomUUID().toString(), status, code, message, checks);
  }

}

