package jetmock.service;

import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jetmock.domain.MockFlow;
import jetmock.storage.MockFlowStorage;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaTriggerStartupRunner implements CommandLineRunner {

  private final MockFlowStorage mockFlowStorage;
  private final KafkaListenerLifecycleService kafkaListenerLifecycleService;

  @Override
  public void run(String... args) {
    log.info("Kafka trigger auto-start initialization started");

    Set<MockFlow> allFlows;

    try {
      allFlows = mockFlowStorage.findAll();
    } catch (Exception e) {
      log.error("Failed to load mock flows on startup", e);
      return;
    }

    for (MockFlow flow : allFlows) {
      try {
        kafkaListenerLifecycleService.startIfExists(flow);
      } catch (Exception e) {
        log.error("Failed to auto-start kafka trigger for flowId={}", flow.getId(), e);
      }
    }

    log.info("Kafka trigger auto-start initialization finished");
  }
}
