package jetmock.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import jetmock.entity.FlowElement;
import jetmock.entity.MockFlowEntity;
import jetmock.repository.MockFlowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncFlowExecutor {

  private final ElementService elementService;
  private final MockFlowRepository mockFlowRepository;

  //todo config
  @Async
  public void runElementsAfterTrigger(String flowId,
                                      String triggerElementName,
                                      Map<Integer, Object> context) {
    MockFlowEntity flow = mockFlowRepository.findById(flowId)
        .orElseThrow(() -> new IllegalStateException("MockFlow not found: " + flowId));

    List<FlowElement> elements = flow.getFlowElements();
    elements.sort(Comparator.comparing(FlowElement::getOrderNumber));
    int start = elementService.findIndex(elements, triggerElementName);

    for (int i = start + 1; i < elements.size(); i++) {
      elementService.executeElementAction(elements.get(i), context);
    }
    log.info("Finished async execution | flow={} | trigger={}",
        flowId, triggerElementName);
  }

}
