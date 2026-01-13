package jetmock.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import jetmock.constant.ElementSchema;
import jetmock.constant.FieldRule;
import jetmock.dto.CreateMockRequest;
import jetmock.dto.MockDetailResponse;
import jetmock.entity.ElementAttribute;
import jetmock.entity.FlowElement;
import jetmock.entity.MockFlowEntity;
import jetmock.exception.BaseException;
import jetmock.repository.MockFlowRepository;
import jetmock.service.kafka.KafkaListenerLifecycleService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MockFlowService {

  ValidationService validationService;
  MockFlowRepository mockFlowRepository;
  KafkaListenerLifecycleService kafkaListenerLifecycleService;

  public void upsert(UUID mockId, CreateMockRequest request) {
    validationService.validate(request);

    MockFlowEntity existing = mockFlowRepository.findById(String.valueOf(mockId)).orElse(null);

    MockFlowEntity flow = existing != null ? existing : MockFlowEntity.builder()
        .id(mockId.toString())
        .groupId(request.getGroupId())
        .flowElements(new ArrayList<>())
        .build();

    List<FlowElement> elements = request.getFlowSteps().stream()
        .map(this::toFlowElement)
        .toList();
    flow.setFlowElements(elements);

    if (existing != null) {
      delete(String.valueOf(mockId), existing);
    }

    mockFlowRepository.save(flow);
    kafkaListenerLifecycleService.startIfExists(flow);

    log.info("MockFlow updated and kafka listener restarted | flowId={}", flow.getId());
  }

  private FlowElement toFlowElement(Map<String, Object> step) {
    String elementName = step.get("elementName").toString();
    Integer orderNumber = Integer.valueOf(step.get("orderNumber").toString());

    ElementSchema schema = ElementSchema.valueOf(elementName);
    Map<String, FieldRule> rules = schema.getRules();

    List<ElementAttribute> attributes = new ArrayList<>();

    for (String key : step.keySet()) {
      if (key.equals("elementName") || key.equals("orderNumber")) {
        continue;
      }

      FieldRule rule = rules.get(key);
      Object value = step.get(key);

      attributes.add(
          ElementAttribute.builder()
              .name(key)
              .dataType(rule.getType().name())
              .value(value)
              .build()
      );
    }

    return FlowElement.builder()
        .id(UUID.randomUUID())
        .name(elementName)
        .orderNumber(orderNumber)
        .attributes(attributes)
        .build();
  }

  public MockDetailResponse getByMockId(String mockId) {
    MockFlowEntity flow = mockFlowRepository.findById(mockId)
        .orElseThrow(() -> new BaseException(404, "MOCK_NOT_FOUND", "Mock not found"));

    MockDetailResponse detailResponse = new MockDetailResponse();

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

  public void delete(String id) {
    MockFlowEntity flow = mockFlowRepository.findById(id)
        .orElseThrow(() ->
            new BaseException(404, "MOCK_NOT_FOUND", "Mock not found"));

    delete(id, flow);
  }

  private void delete(String id, MockFlowEntity flow) {
    kafkaListenerLifecycleService.stopIfExists(flow);

    mockFlowRepository.delete(id);

    log.info("Mock deleted and kafka listener stopped | flowId={}", id);
  }

}
