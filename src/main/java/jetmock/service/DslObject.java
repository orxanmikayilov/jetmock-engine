package jetmock.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

public class DslObject {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private final Object value;

  public DslObject(Object value) {
    this.value = value;
  }

  public Object get(String field) {
    if (value == null) {
      return null;
    }

    if (value instanceof Map<?, ?> map) {
      return map.get(field);
    }

    try {
      var f = value.getClass().getDeclaredField(field);
      f.setAccessible(true);
      return f.get(value);
    } catch (Exception e) {
      return null;
    }
  }

  public DslObject json() {
    if (value == null) {
      return new DslObject(Map.of());
    }

    if (value instanceof Map<?, ?> map) {
      return new DslObject(map);
    }

    try {
      return new DslObject(
          MAPPER.readValue(value.toString(), Map.class)
      );
    } catch (Exception e) {
      throw new RuntimeException("Invalid JSON: " + value, e);
    }
  }

  public String upperCase() {
    return value != null ? value.toString().toUpperCase() : "";
  }

  public String lowerCase() {
    return value != null ? value.toString().toLowerCase() : "";
  }

  public String defaultIfNull(String def) {
    return value != null ? value.toString() : def;
  }

  @Override
  public String toString() {
    return value != null ? value.toString() : "";
  }

}
