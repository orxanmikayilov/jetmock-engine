package jetmock.repository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import jetmock.entity.FlowElement;
import jetmock.entity.FlowMatchResult;
import jetmock.entity.MockFlowEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MockFlowRepository {

  private final RocksDbRepository commonRepository;


  private static final String FLOW_PREFIX = "flow:%s";
  private static final String GROUP_METHOD_PATH_KEY = "match:group:%s:method:%s:path:%s";
  private static final String GROUP_METHOD_KEY = "match:group:%s:method:%s";
  private static final String KAFKA_TRIGGER_KEY = "kafka:broker:%s:topic:%s";

  public MockFlowEntity save(MockFlowEntity flow) {
    commonRepository.save(flowKey(flow.getId()), flow);

    FlowMatchResult match = buildApiMatch(flow);
    if (match.getPath() != null) {
      commonRepository.save(matchKey(flow.getGroupId(), match.getMethod(), match.getPath()), match);
      if (match.getPath().contains(":")) {
        commonRepository.save(methodKey(flow.getGroupId(), match.getMethod()), match);
      }
    }

    buildKafkaTrigger(flow).ifPresent(kafka ->
        commonRepository.saveToList(
            kafkaTriggerKey(kafka.brokerUrl(), kafka.topic()), flow.getId(), UUID.class
        )
    );

    return flow;
  }

  public Set<MockFlowEntity> findByKafkaTrigger(String brokerId, String topic) {
    List<UUID> flowIds = commonRepository.findListByKey(
        kafkaTriggerKey(brokerId, topic),
        UUID.class
    );

    return flowIds.stream()
        .map(this::findById)
        .flatMap(Optional::stream)
        .collect(Collectors.toSet());
  }

  public Optional<FlowMatchResult> findMatchingByMethodAndPath(
      UUID groupId, String method, String path) {
    String key = matchKey(groupId, method, path);
    return commonRepository.findByKey(key, FlowMatchResult.class);
  }

  public Optional<FlowMatchResult> findMatchingByMethod(UUID groupId, String method) {
    String key = methodKey(groupId, method);
    return commonRepository.findByKey(key, FlowMatchResult.class);
  }

  public Optional<MockFlowEntity> findById(UUID id) {
    return commonRepository.findByKey(flowKey(id), MockFlowEntity.class);
  }


  public void delete(UUID id) {
    findById(id).ifPresent(flow -> {
      commonRepository.delete(flowKey(id));

      FlowMatchResult match = buildApiMatch(flow);
      if (match.getPath() != null) {
        commonRepository.delete(matchKey(flow.getGroupId(), match.getMethod(), match.getPath()),
            "");
        if (match.getPath().contains(":")) {
          commonRepository.delete(methodKey(flow.getGroupId(), match.getMethod()), "");
        }
      }

      buildKafkaTrigger(flow).ifPresent(kafka ->
          commonRepository.deleteFromList(
              kafkaTriggerKey(kafka.brokerUrl(), kafka.topic()), id.toString()
          )
      );
    });
  }
// ===================== INTERNAL =====================

  private FlowMatchResult buildApiMatch(MockFlowEntity flow) {
    Optional<FlowElement> apiTrigger = flow.getFlowElements().stream()
        .filter(e -> "API_TRIGGER_REQUEST".equals(e.getName()))
        .findFirst();

    Optional<FlowElement> condition = flow.getFlowElements().stream()
        .filter(e -> "CONDITION".equals(e.getName()))
        .findFirst();

    return FlowMatchResult.builder()
        .id(flow.getId())
        .method(apiTrigger.map(e -> attr(e, "method")).orElse(null))
        .path(apiTrigger.map(e -> attr(e, "path")).orElse(null))
        .expression(condition.map(e -> attr(e, "expression")).orElse(null))
        .build();
  }

  private Optional<KafkaTrigger> buildKafkaTrigger(MockFlowEntity flow) {
    return flow.getFlowElements().stream()
        .filter(e -> "KAFKA_TRIGGER".equals(e.getName()))
        .map(e -> new KafkaTrigger(attr(e, "broker"), attr(e, "topic")))
        .findFirst();
  }

  //todo migrate to  elementService/getAttributeValue
  private String attr(FlowElement e, String name) {
    return e.getAttributes().stream()
        .filter(a -> name.equals(a.getName()))
        .map(a -> Objects.toString(a.getValue(), null))
        .findFirst()
        .orElse(null);
  }

  private String flowKey(UUID id) {
    return String.format(FLOW_PREFIX, id);

  }

  private String matchKey(UUID groupId, String method, String path) {
    return String.format(GROUP_METHOD_PATH_KEY, groupId, method, path);
  }

  private String methodKey(UUID groupId, String method) {
    return String.format(GROUP_METHOD_KEY, groupId, method);
  }

  private String kafkaTriggerKey(String brokerUrl, String topic) {
    return String.format(KAFKA_TRIGGER_KEY, brokerUrl, topic);
  }

  private record KafkaTrigger(String brokerUrl, String topic) {
  }

}
