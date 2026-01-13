package jetmock.service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import jetmock.service.kafka.KafkaListenerLifecycleService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import jetmock.entity.FlowElement;
import jetmock.entity.MockFlowEntity;
import jetmock.dto.MockDetailResponse;
import jetmock.exception.BaseException;
import jetmock.repository.MockFlowRepository;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MockFlowService {

  MockFlowRepository mockFlowRepository;
  KafkaListenerLifecycleService kafkaListenerLifecycleService;

  public MockDetailResponse getByMockId(UUID mockId) {
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

  public void delete(UUID id) {
    MockFlowEntity flow = mockFlowRepository.findById(id)
        .orElseThrow(() ->
            new BaseException(404, "MOCK_NOT_FOUND", "Mock not found"));

    kafkaListenerLifecycleService.stopIfExists(flow);

    mockFlowRepository.delete(id);

    log.info("Mock deleted and kafka listener stopped | flowId={}", id);
  }

}
