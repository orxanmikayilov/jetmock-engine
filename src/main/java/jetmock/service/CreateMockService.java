package jetmock.service;

import static lombok.AccessLevel.PRIVATE;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import jetmock.constant.ElementSchema;
import jetmock.constant.FieldRule;
import jetmock.dto.CreateMockRequest;
import jetmock.entity.ElementAttribute;
import jetmock.entity.FlowElement;
import jetmock.entity.MockFlowEntity;
import jetmock.repository.MockFlowRepository;
import jetmock.service.kafka.KafkaListenerLifecycleService;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class CreateMockService {

  MockFlowRepository mockFlowRepository;
  ValidationService validationService;
  KafkaListenerLifecycleService kafkaListenerLifecycleService;

  public void upsert(UUID mockId, CreateMockRequest request) {
    validationService.validate(request);

    MockFlowEntity flow = mockFlowRepository.findById(mockId)
        .orElseGet(() -> MockFlowEntity.builder()
            .id(mockId)
            .groupId(UUID.fromString(request.getGroupId()))
            .flowElements(new ArrayList<>())
            .build());

    List<FlowElement> elements = request.getFlowSteps().stream()
        .map(this::toFlowElement)
        .toList();
    flow.setFlowElements(elements);

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

}
