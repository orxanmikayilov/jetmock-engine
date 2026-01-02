package jetmock.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

public class SpelFunctions {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @SuppressWarnings("unchecked")
  public static Map<String, Object> json(Object body) {
    if (body == null) {
      return Map.of();
    }

    if (body instanceof Map<?, ?> map) {
      return (Map<String, Object>) map;
    }

    try {
      return MAPPER.readValue(body.toString(), Map.class);
    } catch (Exception e) {
      throw new RuntimeException("Invalid JSON: " + body, e);
    }
  }

}
