package jetmock.repository;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import org.rocksdb.WriteBatch;
import org.rocksdb.WriteOptions;
import org.springframework.stereotype.Repository;

@Repository
@Slf4j
@RequiredArgsConstructor
public class RocksDbRepository {

  private final RocksDB db;
  private final ObjectMapper objectMapper;

  private String buildKey(String prefix, String key) {
    return prefix + ":" + key;
  }

  public <T> List<T> findListByKey(String key, Class<T> contentClass) {
    JavaType type = objectMapper.getTypeFactory().constructCollectionType(List.class, contentClass);

    return this.<List<T>>findByKey(key, type)
        .orElse(new ArrayList<>());
  }

  public <T> Optional<T> findByKey(String prefix, String key, Class<T> entityClass) {
    JavaType type = objectMapper.getTypeFactory().constructType(entityClass);
    return findByKey(buildKey(prefix, key), type);
  }

  public <T> Optional<T> findByKey(String key, Class<T> entityClass) {
    JavaType type = objectMapper.getTypeFactory().constructType(entityClass);
    return findByKey(key, type);
  }

  public <T> Optional<T> findByKey(String key, JavaType type) {
    try {
      byte[] bytes = db.get(key.getBytes());
      if (bytes == null) {
        return Optional.empty();
      }
      Optional<T> t = Optional.ofNullable(objectMapper.readValue(bytes, type));
      return t;
    } catch (Exception e) {
      log.error("RocksDB findByKey error for key: {}", key, e);
      return Optional.empty();
    }
  }

  public <T> void save(String prefix, String key, T value) {
    save(buildKey(prefix, key), value);
  }

  public <T> void save(String key, T value) {
    try {
      db.put(key.getBytes(), objectMapper.writeValueAsBytes(value));
    } catch (Exception e) {
      log.error("RocksDB save error for key: {}", key, e);
      throw new RuntimeException("RocksDB save error", e);
    }
  }

  public <T> void saveToList(String key, T newValue, Class<T> contentClass) {
    List<T> all = findListByKey(key, contentClass);
    all.removeIf(item -> item.equals(newValue));
    all.add(newValue);
    save(key, all);
  }

  public <T> List<T> findAll(String prefix, Class<T> entityClass) {
    List<T> results = new ArrayList<>();

    try (RocksIterator iterator = db.newIterator()) {
      for (iterator.seek(prefix.getBytes());
           iterator.isValid() && new String(iterator.key()).startsWith(prefix);
           iterator.next()) {
        results.add(objectMapper.readValue(iterator.value(), entityClass));
      }
    } catch (Exception e) {
      log.error("RocksDB findAll error for prefix: {}", prefix, e);
      throw new RuntimeException("RocksDB read error", e);
    }
    return results;
  }

  public void deleteFromList(String key, String newValue) {
    List<String> all = findListByKey(key, String.class);
    if (all.remove(newValue)) {
      save(key, all);
    }
  }

  public void delete(String prefix, String key) {
    delete(buildKey(prefix, key));
  }

  public void delete(String key) {
    try {
      db.delete(key.getBytes());
    } catch (RocksDBException e) {
      log.error("RocksDB delete error for key: {}", key, e);
      throw new RuntimeException("RocksDB delete error", e);
    }
  }

  public int deleteByPrefix(String prefix) {
    byte[] prefixBytes = prefix.getBytes();

    try (RocksIterator it = db.newIterator();
         WriteBatch batch = new WriteBatch();
         WriteOptions options = new WriteOptions()) {
      int deleted = 0;

      for (it.seek(prefixBytes);
           it.isValid() && new String(it.key()).startsWith(prefix);
           it.next()) {
        batch.delete(it.key());
        deleted++;
      }

      if (deleted > 0) {
        db.write(options, batch);
      }
      return deleted;

    } catch (Exception e) {
      log.error("RocksDB deleteByPrefix error for prefix: {}", prefix, e);
      throw new RuntimeException("RocksDB delete error", e);
    }
  }

}