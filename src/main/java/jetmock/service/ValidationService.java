package jetmock.service;

import static lombok.AccessLevel.PRIVATE;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import jetmock.constant.DataType;
import jetmock.constant.ElementSchema;
import jetmock.constant.FieldRule;
import jetmock.dto.CreateMockRequest;
import jetmock.exception.BaseException;
import jetmock.exception.ValidationError;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class ValidationService {

  public void validate(CreateMockRequest request) {
    List<ValidationError> errors = new ArrayList<>();

    for (Map<String, Object> step : request.getFlowSteps()) {
      Object elementNameObj = step.get("elementName");
      if (elementNameObj == null) {
        errors.add(new ValidationError("elementName", "must not be null"));
        continue;
      }
      String elementName = elementNameObj.toString();
      Object orderNumberObj = step.get("orderNumber");
      if (orderNumberObj == null) {
        errors.add(new ValidationError(elementName + ".orderNumber", "must not be null"));
      }

      ElementSchema schema = ElementSchema.valueOf(elementName);
      Map<String, FieldRule> rules = schema.getRules();

      for (var entry : rules.entrySet()) {
        String field = entry.getKey();
        FieldRule rule = entry.getValue();
        Object value = step.get(field);

        if (rule.isNotNull() && value == null) {
          errors.add(new ValidationError(elementName + "." + field, "must not be null"));
          continue;
        }

        if (rule.isNotBlank() && (value == null || value.toString().isBlank())) {
          errors.add(new ValidationError(elementName + "." + field, "must not be blank"));
          continue;
        }

        if (value != null) {
          String typeError = validateType(value, rule.getType());
          if (typeError != null) {
            errors.add(new ValidationError(elementName + "." + field, typeError));
          }
        }
      }

      for (String key : step.keySet()) {
        if (!key.equals("elementType")
            && !key.equals("elementName")
            && !key.equals("orderNumber")
            && !rules.containsKey(key)) {
          errors.add(new ValidationError(
              key, "Field '" + key + "' is not allowed for element type " + schema.name()));
        }
      }
    }

    if (!errors.isEmpty()) {
      throw new BaseException(400, "VALIDATION_EXCEPTION", "Validation failed", errors);
    }
  }

  private String validateType(Object value, DataType expectedType) {
    return switch (expectedType) {
      case STRING -> !(value instanceof String)
          ? "data type must be STRING" : null;

      case INTEGER -> !(value instanceof Integer)
          ? "data type must be INTEGER" : null;

      case TEXT -> !(value instanceof String)
          ? "data type must be TEXT" : null;

      default -> "Unsupported data type: " + expectedType;
    };
  }

}
