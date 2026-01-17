package jetmock.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksIterator;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DebugRocksService {

  private final RocksDB db;
  private final ObjectMapper mapper;

  public Map<String, JsonNode> dumpAll() {
    Map<String, JsonNode> result = new LinkedHashMap<>();

    try (RocksIterator it = db.newIterator()) {
      it.seekToFirst();

      while (it.isValid()) {
        String key = key(it.key());
        JsonNode value = readValue(it.value());
        result.put(key, value);
        it.next();
      }
    }
    return result;
  }

  public Map<String, JsonNode> dumpByPrefix(String prefix) {
    Map<String, JsonNode> result = new LinkedHashMap<>();

    byte[] prefixBytes = bytes(prefix);

    try (RocksIterator it = db.newIterator()) {
      it.seek(prefixBytes);

      while (it.isValid()) {
        String key = key(it.key());
        if (!key.startsWith(prefix)) {
          break;
        }

        result.put(key, readValue(it.value()));
        it.next();
      }
    }
    return result;
  }

  public Map<String, Object> deleteByPrefix(String prefix) {
    byte[] prefixBytes = bytes(prefix);

    List<byte[]> keysToDelete = new ArrayList<>();
    int scanned = 0;

    try (RocksIterator it = db.newIterator()) {
      it.seek(prefixBytes);

      while (it.isValid()) {
        String k = key(it.key());
        if (!k.startsWith(prefix)) {
          break;
        }
        scanned++;
        keysToDelete.add(it.key());
        it.next();
      }
    }

    int deleted = 0;
    for (byte[] k : keysToDelete) {
      try {
        db.delete(k);
        deleted++;
      } catch (Exception e) {
        throw new IllegalStateException("Failed to delete key=" + key(k), e);
      }
    }
    return Map.of("prefix", prefix, "scanned", scanned, "deleted", deleted);
  }

  public JsonNode getByKey(String key) {
    try {
      byte[] value = db.get(bytes(key));
      if (value == null) {
        return null;
      }
      return readValue(value);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to read key=" + key, e);
    }
  }

  private JsonNode readValue(byte[] bytes) {
    try {
      return mapper.readTree(bytes);
    } catch (Exception e) {
      return mapper.getNodeFactory().textNode(
          "[BINARY] size=" + bytes.length
      );
    }
  }

  private String key(byte[] bytes) {
    return new String(bytes, StandardCharsets.UTF_8);
  }

  private byte[] bytes(String key) {
    return key.getBytes(StandardCharsets.UTF_8);
  }

}
