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
import jetmock.domain.MockGroup;
import org.rocksdb.RocksDB;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GroupStorage {

  private final RocksDB db;
  private final ObjectMapper mapper;

  private static final String GROUP_PREFIX = "group";
  private static final String GROUP_IDS_KEY = "group:ids";
  private static final String GROUP_NAME_PREFIX = "group:name";
  private static final String GROUP_FLOWS = "group:%s:flows";

  private static final TypeReference<Set<UUID>> UUID_SET = new TypeReference<>() {
  };

  public MockGroup save(MockGroup group) {
    if (group.getId() == null) {
      group.setId(UUID.randomUUID());
    }

    put(groupKey(group.getId()), group);
    put(groupNameKey(group.getName()), group.getId());
    updateIds(ids -> ids.add(group.getId()));

    return group;
  }

  public void delete(UUID id) {
    findById(id).ifPresent(group -> {
      deleteKey(groupKey(id));
      deleteKey(groupNameKey(group.getName()));
      updateIds(ids -> ids.remove(id));
    });
  }

  public Optional<MockGroup> findById(UUID id) {
    return get(groupKey(id), MockGroup.class);
  }

  public Optional<MockGroup> findByName(String name) {
    return get(groupNameKey(name), UUID.class)
        .flatMap(this::findById);
  }

  public List<MockGroup> findAll() {
    Set<UUID> ids = readIds();
    List<MockGroup> result = new ArrayList<>(ids.size());

    for (UUID id : ids) {
      findById(id).ifPresent(result::add);
    }
    return result;
  }

  public List<MockGroup> findAllWithMocks() {
    return findAll();
  }

  private Set<UUID> readIds() {
    return get(GROUP_IDS_KEY, UUID_SET)
        .orElseGet(HashSet::new);
  }

  private void updateIds(java.util.function.Consumer<Set<UUID>> updater) {
    Set<UUID> ids = readIds();
    updater.accept(ids);
    put(GROUP_IDS_KEY, ids);
  }

  private <T> Optional<T> get(String key, Class<T> type) {
    try {
      byte[] v = db.get(bytes(key));
      if (v == null) {
        return Optional.empty();
      }
      return Optional.of(mapper.readValue(v, type));
    } catch (Exception e) {
      throw new IllegalStateException("Read failed: " + key, e);
    }
  }

  private <T> Optional<T> get(String key, TypeReference<T> type) {
    try {
      byte[] v = db.get(bytes(key));
      if (v == null) {
        return Optional.empty();
      }
      return Optional.of(mapper.readValue(v, type));
    } catch (Exception e) {
      throw new IllegalStateException("Read failed: " + key, e);
    }
  }

  private void put(String key, Object value) {
    try {
      db.put(bytes(key), mapper.writeValueAsBytes(value));
    } catch (Exception e) {
      throw new IllegalStateException("Write failed: " + key, e);
    }
  }

  private void deleteKey(String key) {
    try {
      db.delete(bytes(key));
    } catch (Exception e) {
      throw new IllegalStateException("Delete failed: " + key, e);
    }
  }

  private String groupKey(UUID id) {
    return GROUP_PREFIX + ":" + id;
  }

  private String groupNameKey(String name) {
    return GROUP_NAME_PREFIX + ":" + name.toLowerCase();
  }

  private byte[] bytes(String key) {
    return key.getBytes(StandardCharsets.UTF_8);
  }

}
