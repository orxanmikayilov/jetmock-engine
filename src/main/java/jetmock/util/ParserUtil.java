package jetmock.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ParserUtil {

  private static final ObjectMapper objectMapper = new ObjectMapper();
  public static final TypeReference<Map<String, Object>> MAP_STRING_OBJECT = new TypeReference<>() {
  };
  public static final TypeReference<Map<String, String>> MAP_STRING_STRING = new TypeReference<>() {
  };

  @SneakyThrows
  public JsonNode toJsonNode(String payload) {
    if (payload == null) {
      return null;
    }

    return objectMapper.readTree(payload);

  }

  @SneakyThrows
  public static <T> T parseTo(String json, TypeReference<T> typeClass) {
    if (json == null) {
      return null;
    }
    return objectMapper.readValue(json, typeClass);
  }

  public static Map<String, Object> toMap(String json) {
    if (json == null) {
      return null;
    }
    try {
      return new ObjectMapper().readValue(json, new TypeReference<>() {
      });
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static Map<String, String> toStringMap(String json) {
    if (json == null) {
      return null;
    }
    try {
      return new ObjectMapper().readValue(json, new TypeReference<>() {
      });
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

}
