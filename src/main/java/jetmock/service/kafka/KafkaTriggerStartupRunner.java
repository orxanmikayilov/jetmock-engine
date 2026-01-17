package jetmock.service.kafka;

import java.util.List;
import jetmock.entity.FlowMatchResult;
import jetmock.repository.MockFlowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaTriggerStartupRunner {

  private final MockFlowRepository mockFlowRepository;
  private final KafkaListenerLifecycleService kafkaListenerLifecycleService;

  @EventListener(ApplicationReadyEvent.class)
  public void run() {
    log.info("Kafka trigger auto-start initialization started");

    List<FlowMatchResult> allKafkaTriggers;

    try {
      allKafkaTriggers = mockFlowRepository.findAllKafkaTrigger();

    } catch (Exception e) {
      log.error("Failed to load mock flows on startup", e);
      return;
    }
    allKafkaTriggers.forEach(flowMatchResult ->
        mockFlowRepository.findById(flowMatchResult.getId()).ifPresent(mockFlow -> {
          try {
            kafkaListenerLifecycleService.startIfExists(mockFlow);
          } catch (Exception e) {
            log.error("Failed to auto-start kafka trigger for flowId={}", mockFlow.getId(), e);
          }
        }));

    log.info("Kafka trigger auto-start initialization finished");
  }

}
