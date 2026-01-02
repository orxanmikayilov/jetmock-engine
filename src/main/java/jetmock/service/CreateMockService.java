package jetmock.service;

import static lombok.AccessLevel.PRIVATE;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import jetmock.contant.ElementSchema;
import jetmock.contant.FieldRule;
import jetmock.domain.ElementAttribute;
import jetmock.domain.FlowElement;
import jetmock.domain.MockFlow;
import jetmock.domain.MockGroup;
import jetmock.dto.CreateMockRequest;
import jetmock.exception.BaseException;
import jetmock.storage.GroupStorage;
import jetmock.storage.MockFlowStorage;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class CreateMockService {

  GroupStorage groupStorage;
  MockFlowStorage mockFlowStorage;
  ValidationService validationService;
  KafkaListenerLifecycleService kafkaListenerLifecycleService;

  public void save(CreateMockRequest request) {
    validationService.validate(request);

    MockGroup group = groupStorage.findById(UUID.fromString(request.getGroupId()))
        .orElseThrow(() -> new RuntimeException("Group not found"));

    MockFlow flow = MockFlow.builder()
        .id(UUID.randomUUID())
        .name(request.getName())
        .groupId(group.getId())
        .flowElements(new ArrayList<>())
        .build();

    List<FlowElement> elements = request.getFlowSteps().stream()
        .map(this::toFlowElement)
        .toList();

    flow.setFlowElements(elements);

    mockFlowStorage.save(flow);
    kafkaListenerLifecycleService.startIfExists(flow);

    log.info("Mock flow '{}' created with {} elements", flow.getId(), elements.size());
  }

  public void update(UUID mockId, CreateMockRequest request) {
    validationService.validate(request);

    MockFlow flow = mockFlowStorage.findById(mockId)
        .orElseThrow(() ->
            new BaseException(404, "MOCK_NOT_FOUND", "MockFlow not found: " + mockId));

    kafkaListenerLifecycleService.startIfExists(flow);

    List<FlowElement> elements = request.getFlowSteps().stream()
        .map(this::toFlowElement)
        .toList();

    flow.setName(request.getName());
    flow.setFlowElements(elements);

    mockFlowStorage.save(flow);

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

}
