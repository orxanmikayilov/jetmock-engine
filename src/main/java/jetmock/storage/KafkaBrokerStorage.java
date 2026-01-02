package jetmock.storage;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import jetmock.domain.KafkaBroker;
import org.rocksdb.RocksDB;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaBrokerStorage {

  private final RocksDB db;
  private final ObjectMapper mapper;

  private static final String BROKER_PREFIX = "kafka-broker";
  private static final String BROKER_IDS_KEY = "kafka-broker:ids";
  private static final TypeReference<Set<UUID>> UUID_SET_TYPE =
      new TypeReference<>() {
      };

  public KafkaBroker save(KafkaBroker broker) {
    if (broker.getId() == null) {
      broker.setId(UUID.randomUUID());
    }

    put(brokerKey(broker.getId()), broker);
    updateIds(ids -> ids.add(broker.getId()));

    return broker;
  }

  public Optional<KafkaBroker> findById(UUID id) {
    return get(brokerKey(id), KafkaBroker.class);
  }

  public List<KafkaBroker> findAll() {
    Set<UUID> ids = readIds();
    List<KafkaBroker> result = new ArrayList<>(ids.size());

    for (UUID id : ids) {
      findById(id).ifPresent(result::add);
    }
    return result;
  }

  public void delete(UUID id) {
    deleteKey(brokerKey(id));
    updateIds(ids -> ids.remove(id));
  }

  private Set<UUID> readIds() {
    return get(BROKER_IDS_KEY, UUID_SET_TYPE)
        .orElseGet(HashSet::new);
  }

  private void updateIds(java.util.function.Consumer<Set<UUID>> updater) {
    Set<UUID> ids = readIds();
    updater.accept(ids);
    put(BROKER_IDS_KEY, ids);
  }

  private <T> Optional<T> get(String key, Class<T> type) {
    try {
      byte[] value = db.get(bytes(key));
      if (value == null) {
        return Optional.empty();
      }
      return Optional.of(mapper.readValue(value, type));
    } catch (Exception e) {
      throw new IllegalStateException("Failed to read key=" + key, e);
    }
  }

  private <T> Optional<T> get(String key, TypeReference<T> type) {
    try {
      byte[] value = db.get(bytes(key));
      if (value == null) {
        return Optional.empty();
      }
      return Optional.of(mapper.readValue(value, type));
    } catch (Exception e) {
      throw new IllegalStateException("Failed to read key=" + key, e);
    }
  }

  private void put(String key, Object value) {
    try {
      db.put(bytes(key), mapper.writeValueAsBytes(value));
    } catch (Exception e) {
      throw new IllegalStateException("Failed to write key=" + key, e);
    }
  }

  private void deleteKey(String key) {
    try {
      db.delete(bytes(key));
    } catch (Exception e) {
      throw new IllegalStateException("Failed to delete key=" + key, e);
    }
  }

  private String brokerKey(UUID id) {
    return BROKER_PREFIX + ":" + id;
  }

  private byte[] bytes(String key) {
    return key.getBytes(StandardCharsets.UTF_8);
  }

}
