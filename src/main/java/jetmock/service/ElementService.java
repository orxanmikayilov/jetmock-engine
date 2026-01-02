package jetmock.service;

import static lombok.AccessLevel.PRIVATE;

import com.fasterxml.jackson.databind.JsonNode;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import jetmock.domain.ElementAttribute;
import jetmock.domain.FlowElement;
import jetmock.domain.GlobalVariable;
import jetmock.dto.payload.CallbackApiPayload;
import jetmock.dto.payload.GlobalVariablePayload;
import jetmock.dto.payload.KafkaPublisherPayload;
import jetmock.storage.GlobalEnvironmentStorage;
import jetmock.util.ParserUtil;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class ElementService {

  KafkaPublishService kafkaPublishService;
  CallbackService callbackService;
  GlobalEnvironmentStorage globalEnvironmentStorage;
  PlaceholderService placeholderService;

  public void executeElementAction(FlowElement fe, Map<Integer, Object> context) {
    String type = fe.getName();
    switch (type) {
      case "CALLBACK_API" -> executeCallbackAttributes(fe, context);
      case "KAFKA_PUBLISHER" -> executeKafkaPublisherAttributes(fe, context);
      case "GLOBAL_VARIABLE" -> executeGlobalVariableAttributes(fe, context);
      default -> log.warn("Unknown element type: {}", type);
    }
  }

  public <T> T mapAttributes(List<ElementAttribute> attributes, Class<T> clazz) {
    try {
      T dto = clazz.getDeclaredConstructor().newInstance();

      for (ElementAttribute attr : attributes) {
        Field field = getField(clazz, attr.getName());
        if (field == null) {
          continue;
        }

        field.setAccessible(true);
        Object value = convertValue(attr.getValue(), field.getType());
        field.set(dto, value);
      }

      return dto;
    } catch (Exception e) {
      throw new RuntimeException("Failed to map attributes to DTO: " + clazz.getSimpleName(), e);
    }
  }

  private Object convertValue(Object value, Class<?> targetType) {
    if (value == null) {
      return null;
    }

    if (targetType == Integer.class) {
      return Integer.valueOf(value.toString());
    }
    if (targetType == Double.class) {
      return Double.valueOf(value.toString());
    }
    if (targetType == Boolean.class) {
      return Boolean.valueOf(value.toString());
    }
    return value.toString();
  }

  private Field getField(Class<?> clazz, String fieldName) {
    try {
      return clazz.getDeclaredField(fieldName);
    } catch (Exception e) {
      return null;
    }
  }

  public int findIndex(List<FlowElement> elements, String type) {
    for (int i = 0; i < elements.size(); i++) {
      if (type.equals(elements.get(i).getName())) {
        return i;
      }
    }
    return -1;
  }

  private void executeKafkaPublisherAttributes(FlowElement element, Map<Integer, Object> context) {
    KafkaPublisherPayload payload =
        mapAttributes(element.getAttributes(), KafkaPublisherPayload.class);

    String resolvedBody =
        placeholderService.resolvePlaceholders(payload.getBody(), context);

    payload.setBody(resolvedBody);
    kafkaPublishService.publishToKafka(payload);

    context.put(element.getOrderNumber(), payload);
  }

  private void executeCallbackAttributes(FlowElement element, Map<Integer, Object> context) {
    CallbackApiPayload payload =
        mapAttributes(element.getAttributes(), CallbackApiPayload.class);

    String resolvedBody =
        placeholderService.resolvePlaceholders(payload.getBody(), context);

    payload.setBody(resolvedBody);
    callbackService.callbackToClient(payload);

    context.put(element.getOrderNumber(), payload);
  }

  private void executeGlobalVariableAttributes(FlowElement element, Map<Integer, Object> context) {
    GlobalVariablePayload payload =
        mapAttributes(element.getAttributes(), GlobalVariablePayload.class);

    String resolvedValue =
        placeholderService.resolvePlaceholders(payload.getVariable(), context);
    JsonNode jsonNode = ParserUtil.toJsonNode(resolvedValue);

    for (JsonNode node : jsonNode) {

      JsonNode keyNode = node.get("key");
      JsonNode valueNode = node.get("value");

      if (keyNode == null || keyNode.isNull()) {
        continue;
      }

      String key = keyNode.asText();
      Object value = valueNode != null && !valueNode.isNull()
          ? valueNode.asText()
          : null;

      globalEnvironmentStorage.upsert(new GlobalVariable(key, value));

    }
    payload.setVariable(resolvedValue);

    context.put(element.getOrderNumber(), payload);
  }

  public String getAttributeValue(FlowElement e, String path) {
    return e.getAttributes().stream().filter(a -> a.getName().equals(path)).findFirst()
        .map(ElementAttribute::getValue)
        .map(String.class::cast)
        .orElse(null);
  }

}
