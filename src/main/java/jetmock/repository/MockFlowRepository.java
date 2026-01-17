package jetmock.repository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
  private static final String GROUP_METHOD_PATH_KEY =
      "static_api_match:group:%s:method:%s:path:%s:flow:%s";
  private static final String GROUP_METHOD_PATH_SEARCH_KEY =
      "static_api_match:group:%s:method:%s:path:%s:";
  private static final String GROUP_METHOD_KEY = "dynamic_api_match:group:%s:method:%s:flow:%s";
  private static final String GROUP_METHOD_SEARCH_KEY = "dynamic_api_match:group:%s:method:%s:";
  private static final String KAFKA_TRIGGER_KEY = "kafka:broker:%s:topic:%s:flow:%s";
  private static final String KAFKA_TRIGGER_SEARCH_KEY = "kafka:broker:%s:topic:%s:";
  private static final String ALL_KAFKA_TRIGGER_SEARCH_KEY = "kafka:";

  public MockFlowEntity save(MockFlowEntity flow) {
    String flowId = flow.getId();
    Optional<FlowElement> condition = getFlowElement(flow, "CONDITION");
    String expression = condition.map(e -> attr(e, "expression")).orElse(null);
    FlowMatchResult flowMatch = new FlowMatchResult(flowId, expression);
    commonRepository.save(flowKey(flowId), flow);
    ApiTrigger apiTrigger = buildApiTrigger(flow);
    if (apiTrigger.path() != null) {
      commonRepository.save(
          matchKey(flow.getGroupId(), apiTrigger.method(), apiTrigger.path(), flowId),
          flowMatch);
      if (apiTrigger.path().contains(":")) {
        commonRepository.save(methodKey(flow.getGroupId(), apiTrigger.method(), flowId),
            flowMatch);
      }
    }

    buildKafkaTrigger(flow).ifPresent(kafka ->
        commonRepository.save(
            kafkaTriggerKey(kafka.brokerUrl(), kafka.topic(), flowId), flowMatch
        )
    );

    return flow;
  }

  private static Optional<FlowElement> getFlowElement(MockFlowEntity flow, String elementName) {
    return flow.getFlowElements().stream()
        .filter(e -> elementName.equals(e.getName()))
        .findFirst();
  }

  public List<FlowMatchResult> findByKafkaTrigger(String brokerId, String topic) {
    return commonRepository.findAll(kafkaTriggerKey(brokerId, topic), FlowMatchResult.class);
  }

  public List<FlowMatchResult> findAllKafkaTrigger() {
    return commonRepository.findAll(ALL_KAFKA_TRIGGER_SEARCH_KEY, FlowMatchResult.class);
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

      ApiTrigger match = buildApiTrigger(flow);
      if (match.path() != null) {
        commonRepository.deleteByPrefix(matchKey(flow.getGroupId(), match.method(), match.path()));
        if (match.path().contains(":")) {
          commonRepository.deleteByPrefix(methodKey(flow.getGroupId(), match.method()));
        }
      }

      buildKafkaTrigger(flow).ifPresent(kafka ->
          commonRepository.deleteFromList(
              kafkaTriggerKey(kafka.brokerUrl(), kafka.topic()), id.toString()
          )
      );
    });
  }

  private ApiTrigger buildApiTrigger(MockFlowEntity flow) {
    Optional<FlowElement> apiTrigger = getFlowElement(flow, "API_TRIGGER_REQUEST");

    String method = apiTrigger.map(e -> attr(e, "method")).orElse(null);
    String path = apiTrigger.map(e -> attr(e, "path")).orElse(null);
    return new ApiTrigger(method, path);
  }

  private Optional<KafkaTrigger> buildKafkaTrigger(MockFlowEntity flow) {
    Optional<FlowElement> apiTrigger = getFlowElement(flow, "KAFKA_TRIGGER");

    return apiTrigger.map(e -> new KafkaTrigger(attr(e, "broker"), attr(e, "topic")));
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

  private record ApiTrigger(String method, String path) {
  }

  private record KafkaTrigger(String brokerUrl, String topic) {
  }

}
