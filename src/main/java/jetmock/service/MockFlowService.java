package jetmock.service;

import static jetmock.contant.ElementSchema.API_TRIGGER_REQUEST;
import static jetmock.contant.ElementSchema.KAFKA_TRIGGER;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import jetmock.contant.ElementSchema;
import jetmock.domain.FlowElement;
import jetmock.domain.MockFlow;
import jetmock.dto.MockDetailResponse;
import jetmock.dto.MockResponse;
import jetmock.exception.BaseException;
import jetmock.storage.MockFlowStorage;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MockFlowService {

  MockFlowStorage mockFlowStorage;
  KafkaListenerLifecycleService kafkaListenerLifecycleService;

  public List<MockResponse> getByGroupId(UUID groupId) {
    Set<MockFlow> flows = mockFlowStorage.findByGroupId(groupId);
    List<MockResponse> responses = new ArrayList<>();

    for (MockFlow mockFlow : flows) {
      FlowElement triggerElement = mockFlow.getFlowElements().stream()
          .filter(element -> List.of(API_TRIGGER_REQUEST, KAFKA_TRIGGER)
              .contains(ElementSchema.valueOf(element.getName())))
          .findFirst()
          .orElse(null);

      if (triggerElement == null) {
        continue;
      }

      String trigger = "UNKNOWN";
      String description = null;

      ElementSchema elementSchema = ElementSchema.valueOf(triggerElement.getName());

      switch (elementSchema) {
        case API_TRIGGER_REQUEST -> {
          trigger = "API";
          String method = getStringAttr(triggerElement, "method");
          String path = getStringAttr(triggerElement, "path");
          description = method + " " + path;
        }

        case KAFKA_TRIGGER -> {
          trigger = "KAFKA";
          String broker = getStringAttr(triggerElement, "broker");
          String topic = getStringAttr(triggerElement, "topic");
          description = broker + " : " + topic;
        }

        default -> log.warn("Unsupported trigger {}", elementSchema);
      }

      responses.add(
          MockResponse.builder()
              .id(mockFlow.getId())
              .name(mockFlow.getName())
              .trigger(trigger)
              .description(description)
              .build()
      );
    }

    return responses;
  }

  private String getStringAttr(FlowElement element, String name) {
    return element.getAttributes().stream()
        .filter(a -> a.getName().equals(name))
        .findFirst()
        .map(elementAttribute -> Objects.toString(elementAttribute.getValue(), null))
        .orElse(null);
  }

  public MockDetailResponse getByMockId(UUID mockId) {
    MockFlow flow = mockFlowStorage.findById(mockId)
        .orElseThrow(() -> new BaseException(404, "MOCK_NOT_FOUND", "Mock not found"));

    MockDetailResponse detailResponse = new MockDetailResponse();
    detailResponse.setName(flow.getName());

    List<Map<String, Object>> flowSteps = flow.getFlowElements().stream()
        .sorted(Comparator.comparing(FlowElement::getOrderNumber))
        .map(element -> {
          Map<String, Object> flowStep = new HashMap<>();
          flowStep.put("elementName", element.getName());
          flowStep.put("orderNumber", element.getOrderNumber());
          element.getAttributes().forEach(attr -> flowStep.put(attr.getName(), attr.getValue()));
          return flowStep;
        })
        .toList();
    detailResponse.setFlowSteps(flowSteps);
    return detailResponse;
  }

  public void delete(UUID id) {
    MockFlow flow = mockFlowStorage.findById(id)
        .orElseThrow(() ->
            new BaseException(404, "MOCK_NOT_FOUND", "Mock not found"));

    kafkaListenerLifecycleService.stopIfExists(flow);

    mockFlowStorage.delete(id);

    log.info("Mock deleted and kafka listener stopped | flowId={}", id);
  }

}
