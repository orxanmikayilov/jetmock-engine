package jetmock.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import jetmock.domain.FlowElement;
import jetmock.domain.FlowMatchResult;
import jetmock.domain.MockFlow;
import org.rocksdb.RocksDB;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MockFlowStorage {

  private final RocksDB db;
  private final ObjectMapper mapper;

  private static final String FLOW_PREFIX = "flow:";
  private static final String GROUP_FLOWS = "group:%s:flows";
  private static final String GROUP_PREFIX = "group:ids";
  private static final String MATCH_KEY = "match:%s:%s:%s"; // groupId, method, path
  private static final String METHOD_KEY = "match:%s:%s";   // groupId, method
  private static final String KAFKA_TRIGGER_KEY = "kafka:%s:%s";

  public MockFlow save(MockFlow flow) {
    try {
      if (flow.getId() == null) {
        flow.setId(UUID.randomUUID());
      }

      put(flowKey(flow.getId()), flow);
      addToSet(groupFlowsKey(flow.getGroupId()), flow.getId());
      FlowMatchResult match = buildMatch(flow);

      if (match.getMethod() != null) {
        if (match.getPath() != null) {
          put(matchKey(flow.getGroupId(), match.getMethod(), match.getPath()), match);
        }
        put(methodKey(flow.getGroupId(), match.getMethod()), match);
      }

      buildKafkaTrigger(flow).ifPresent(kafka -> {
            try {
              addToSet(
                  kafkaTriggerKey(kafka.brokerUrl, kafka.topic),
                  flow.getId()
              );
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          }
      );

      return flow;
    } catch (Exception e) {
      throw new IllegalStateException("Failed to save MockFlow", e);
    }
  }

  public int countByGroupId(UUID groupId) {
    try {
      return readSet(groupFlowsKey(groupId)).size();
    } catch (Exception e) {
      throw new IllegalStateException("Failed to count flows for groupId=" + groupId, e);
    }
  }

  public Set<MockFlow> findByKafkaTrigger(String brokerId, String topic) {
    try {
      return readSet(kafkaTriggerKey(brokerId,
          topic)).stream()
          .map(this::findById)
          .flatMap(Optional::stream)
          .collect(Collectors.toSet());
    } catch (Exception e) {
      throw new IllegalStateException("Kafka trigger lookup failed", e);
    }
  }

  public Optional<FlowMatchResult> findMatchingByMethodAndPath(
      UUID groupId, String method, String path
  ) {
    return get(matchKey(groupId, method, path), FlowMatchResult.class);
  }

  public Optional<FlowMatchResult> findMatchingByMethod(
      UUID groupId, String method
  ) {
    return get(methodKey(groupId, method), FlowMatchResult.class);
  }

  public Optional<MockFlow> findById(UUID id) {
    return get(flowKey(id), MockFlow.class);
  }

  @SneakyThrows
  public Set<MockFlow> findByGroupId(UUID groupId) {
    return readSet(groupFlowsKey(groupId)).stream()
        .map(this::findById)
        .flatMap(Optional::stream)
        .collect(Collectors.toSet());
  }

  @SneakyThrows
  public Set<MockFlow> findAll() {
    Set<MockFlow> result = new HashSet<>();
    Set<UUID> groups = readSet(GROUP_PREFIX);
    for (UUID groupId : groups) {
      result.addAll(findByGroupId(groupId));
    }
    return result;
  }

  public void delete(UUID id) {
    try {
      Optional<MockFlow> opt = findById(id);
      if (opt.isEmpty()) {
        return;
      }

      MockFlow flow = opt.get();
      db.delete(bytes(flowKey(id)));
      removeFromSet(groupFlowsKey(flow.getGroupId()), id);

      FlowMatchResult match = buildMatch(flow);

      if (match.getMethod() != null) {
        if (match.getPath() != null) {
          db.delete(bytes(matchKey(flow.getGroupId(), match.getMethod(), match.getPath())));
        }
        db.delete(bytes(methodKey(flow.getGroupId(), match.getMethod())));
      }

      // 🔴 KAFKA TRIGGER CLEANUP
      buildKafkaTrigger(flow).ifPresent(kafka ->
          {
            try {
              removeFromSet(
                  kafkaTriggerKey(kafka.brokerUrl, kafka.topic),
                  id
              );
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          }
      );

    } catch (Exception e) {
      throw new IllegalStateException("Failed to delete MockFlow id=" + id, e);
    }
  }

  // ===================== INTERNAL =====================

  private FlowMatchResult buildMatch(MockFlow flow) {
    String path = null;
    String method = null;
    String expr = null;

    for (FlowElement e : flow.getFlowElements()) {
      if ("API_TRIGGER_REQUEST".equals(e.getName())) {
        path = attr(e, "path");
        method = attr(e, "method");
      }
      if ("CONDITION".equals(e.getName())) {
        expr = attr(e, "expression");
      }
    }

    return FlowMatchResult.builder()
        .id(flow.getId())
        .path(path)
        .method(method)
        .expression(expr)
        .build();
  }

  private Optional<KafkaTrigger> buildKafkaTrigger(MockFlow flow) {
    for (FlowElement e : flow.getFlowElements()) {
      if ("KAFKA_TRIGGER".equals(e.getName())) {
        String topic = attr(e, "topic");
        String brokerUrl = attr(e, "broker"); // ⬅️ URL gözlənilir

        if (topic != null && brokerUrl != null) {
          return Optional.of(new KafkaTrigger(brokerUrl, topic));
        }
      }
    }
    return Optional.empty();
  }

  private String attr(FlowElement e, String name) {
    return e.getAttributes().stream()
        .filter(a -> name.equals(a.getName()))
        .map(a -> Objects.toString(a.getValue(), null))
        .findFirst()
        .orElse(null);
  }

  private void put(String key, Object value) throws Exception {
    db.put(bytes(key), mapper.writeValueAsBytes(value));
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

  private void addToSet(String key, UUID id) throws Exception {
    Set<UUID> set = readSet(key);
    set.add(id);
    put(key, set);
  }

  private void removeFromSet(String key, UUID id) throws Exception {
    Set<UUID> set = readSet(key);
    if (set.remove(id)) {
      put(key, set);
    }
  }

  private Set<UUID> readSet(String key) throws Exception {
    byte[] v = db.get(bytes(key));
    if (v == null) {
      return new HashSet<>();
    }
    return mapper.readValue(
        v,
        mapper.getTypeFactory()
            .constructCollectionType(Set.class, UUID.class)
    );
  }

  private String flowKey(UUID id) {
    return FLOW_PREFIX + id;
  }

  private String groupFlowsKey(UUID groupId) {
    return String.format(GROUP_FLOWS, groupId);
  }

  private String matchKey(UUID groupId, String method, String path) {
    return String.format(MATCH_KEY, groupId, method, path);
  }

  private String methodKey(UUID groupId, String method) {
    return String.format(METHOD_KEY, groupId, method);
  }

  private String kafkaTriggerKey(String brokerUrl, String topic) {
    return String.format(KAFKA_TRIGGER_KEY, brokerUrl, topic);
  }

  private byte[] bytes(String key) {
    return key.getBytes(StandardCharsets.UTF_8);
  }

  private record KafkaTrigger(String brokerUrl, String topic) {
  }

}
