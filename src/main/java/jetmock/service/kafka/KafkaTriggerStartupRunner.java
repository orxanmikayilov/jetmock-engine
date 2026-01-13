package jetmock.service.kafka;

import java.util.Set;
import jetmock.entity.MockFlowEntity;
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

    Set<MockFlowEntity> allFlows;

    try {
      //todo use findKafkaTriggers instrad of find all
      allFlows = null;
    } catch (Exception e) {
      log.error("Failed to load mock flows on startup", e);
      return;
    }

//    for (MockFlowEntity flow : allFlows) {
//      try {
//        kafkaListenerLifecycleService.startIfExists(flow);
//      } catch (Exception e) {
//        log.error("Failed to auto-start kafka trigger for flowId={}", flow.getId(), e);
//      }
//    }

    log.info("Kafka trigger auto-start initialization finished");
  }
}
