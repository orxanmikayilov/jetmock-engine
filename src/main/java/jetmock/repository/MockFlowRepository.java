package jetmock.repository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
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
  private static final String GROUP_METHOD_PATH_KEY = "static_api_match:group:%s:method:%s:path:%s:flow:%s";
  private static final String GROUP_METHOD_PATH_SEARCH_KEY = "static_api_match:group:%s:method:%s:path:%s:";
  private static final String GROUP_METHOD_KEY = "dynamic_api_match:group:%s:method:%s:flow:%s";
  private static final String GROUP_METHOD_SEARCH_KEY = "dynamic_api_match:group:%s:method:%s:";
  private static final String KAFKA_TRIGGER_KEY = "kafka:broker:%s:topic:%s:flow:%s";
  private static final String KAFKA_TRIGGER_SEARCH_KEY = "kafka:broker:%s:topic:%s:";

  public MockFlowEntity save(MockFlowEntity flow) {
    String flowId = flow.getId();
    commonRepository.save(flowKey(flowId), flow);

    FlowMatchResult match = buildApiMatch(flow);
    if (match.getPath() != null) {
      commonRepository.save(matchKey(flow.getGroupId(), match.getMethod(), match.getPath(), flowId),
          match);
      if (match.getPath().contains(":")) {
        commonRepository.save(methodKey(flow.getGroupId(), match.getMethod(), flowId), match);
      }
    }

    buildKafkaTrigger(flow).ifPresent(kafka ->
        commonRepository.saveToList(
            kafkaTriggerKey(kafka.brokerUrl(), kafka.topic(), flowId), flowId, String.class
        )
    );

    return flow;
  }

  public List<MockFlowEntity> findByKafkaTrigger(String brokerId, String topic) {
    List<String> flowIds = commonRepository.findListByKey(
        kafkaTriggerKey(brokerId, topic),
        String.class
    );

    return flowIds.stream()
        .map(this::findById)
        .flatMap(Optional::stream)
        .toList();
  }

  public List<FlowMatchResult> findMatchingByMethodAndPath(
      String groupId, String method, String path) {
    String key = matchKey(groupId, method, path);
    return commonRepository.findAll(key, FlowMatchResult.class);
  }

  public List<FlowMatchResult> findMatchingByMethod(String groupId, String method) {
    String key = methodKey(groupId, method);
    return commonRepository.findAll(key, FlowMatchResult.class);
  }

  public Optional<MockFlowEntity> findById(String id) {
    return commonRepository.findByKey(flowKey(id), MockFlowEntity.class);
  }


  public void delete(String id) {
    findById(id).ifPresent(flow -> {
      commonRepository.delete(flowKey(id));

      FlowMatchResult match = buildApiMatch(flow);
      if (match.getPath() != null) {
        commonRepository.deleteByPrefix(matchKey(flow.getGroupId(), match.getMethod(), match.getPath()));
        if (match.getPath().contains(":")) {
          commonRepository.deleteByPrefix(methodKey(flow.getGroupId(), match.getMethod()));
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

  private String flowKey(String id) {
    return String.format(FLOW_PREFIX, id);
  }

  private String matchKey(String groupId, String method, String path, String flowId) {
    return String.format(GROUP_METHOD_PATH_KEY, groupId, method, path, flowId);
  }

  private String matchKey(String groupId, String method, String path) {
    return String.format(GROUP_METHOD_PATH_SEARCH_KEY, groupId, method, path);
  }

  private String methodKey(String groupId, String method, String flowId) {
    return String.format(GROUP_METHOD_KEY, groupId, method, flowId);
  }

  private String methodKey(String groupId, String method) {
    return String.format(GROUP_METHOD_SEARCH_KEY, groupId, method);
  }

  private String kafkaTriggerKey(String brokerUrl, String topic, String flowId) {
    return String.format(KAFKA_TRIGGER_KEY, brokerUrl, topic, flowId);
  }

  private String kafkaTriggerKey(String brokerUrl, String topic) {
    return String.format(KAFKA_TRIGGER_SEARCH_KEY, brokerUrl, topic);
  }

  private record KafkaTrigger(String brokerUrl, String topic) {
  }

}
