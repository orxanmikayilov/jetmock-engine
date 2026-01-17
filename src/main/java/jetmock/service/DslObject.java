package jetmock.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.Map;

public class DslObject {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private final Object value;

  public DslObject(Object value) {
    this.value = value;
  }

  public Object raw() {
    return value;
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
      return new DslObject(MAPPER.readValue(value.toString(), Map.class));
    } catch (Exception e) {
      throw new RuntimeException("Invalid JSON: " + value, e);
    }
  }

  public String str() {
    return value == null ? "" : value.toString();
  }

  public Boolean bool() {
    if (value == null) {
      return false;
    }
    if (value instanceof Boolean b) {
      return b;
    }

    String s = value.toString().trim();
    if (s.isBlank()) {
      return false;
    }

    return "true".equalsIgnoreCase(s)
        || "1".equals(s)
        || "yes".equalsIgnoreCase(s)
        || "on".equalsIgnoreCase(s);
  }

  public BigDecimal num() {
    if (value == null) {
      return BigDecimal.ZERO;
    }
    if (value instanceof Number n) {
      return new BigDecimal(n.toString());
    }

    String s = value.toString().trim().replace(",", ".");
    if (s.isBlank()) {
      return BigDecimal.ZERO;
    }

    return new BigDecimal(s);
  }

  public String upperCase() {
    return str().toUpperCase();
  }

  public String lowerCase() {
    return str().toLowerCase();
  }

  public String defaultIfNull(String def) {
    return value != null ? value.toString() : def;
  }

  @Override
  public String toString() {
    return str();
  }

}
