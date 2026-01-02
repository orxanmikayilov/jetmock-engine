package jetmock.contant;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ElementSchema {

  CONDITION(Map.of(
      "expression", FieldRule.builder().type(DataType.STRING).isNotBlank(true).build())),
  API_TRIGGER_REQUEST(Map.of(
      "method", FieldRule.builder().type(DataType.STRING).isNotBlank(true).build(),
      "path", FieldRule.builder().type(DataType.STRING).isNotBlank(true).build())),
  API_TRIGGER_RESPONSE(Map.of(
      "status", FieldRule.builder().type(DataType.INTEGER).isNotNull(true).build(),
      "latency", FieldRule.builder().type(DataType.INTEGER).isNotNull(true).build(),
      "header", FieldRule.builder().type(DataType.TEXT).isNotBlank(true).build(),
      "body", FieldRule.builder().type(DataType.TEXT).isNotBlank(true).build())),
  KAFKA_TRIGGER(Map.of(
      "topic", FieldRule.builder().type(DataType.STRING).isNotBlank(true).build(),
      "broker", FieldRule.builder().type(DataType.STRING).isNotBlank(true).build())),
  CALLBACK_API(Map.of(
      "path", FieldRule.builder().type(DataType.STRING).isNotBlank(true).build(),
      "method", FieldRule.builder().type(DataType.STRING).isNotBlank(true).build(),
      "latency", FieldRule.builder().type(DataType.INTEGER).isNotNull(true).build(),
      "header", FieldRule.builder().type(DataType.TEXT).isNotBlank(true).build(),
      "param", FieldRule.builder().type(DataType.TEXT).isNotBlank(true).build(),
      "body", FieldRule.builder().type(DataType.TEXT).isNotBlank(true).build())),
  KAFKA_PUBLISHER(Map.of(
      "topic", FieldRule.builder().type(DataType.STRING).isNotBlank(true).build(),
      "broker", FieldRule.builder().type(DataType.STRING).isNotBlank(true).build(),
      "body", FieldRule.builder().type(DataType.TEXT).isNotBlank(true).build())),
  GLOBAL_VARIABLE(Map.of(
      "variable", FieldRule.builder().type(DataType.TEXT).isNotBlank(true).build()));
  private final Map<String, FieldRule> rules;

}
