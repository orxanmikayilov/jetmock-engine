package jetmock.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import jetmock.domain.KafkaBroker;
import jetmock.domain.MockFlow;
import jetmock.dto.ActiveKafkaListenerDto;
import jetmock.exception.BaseException;
import jetmock.storage.KafkaBrokerStorage;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class KafkaListenerLifecycleService {

  RuntimeKafkaListenerManager listenerManager;
  KafkaBrokerStorage kafkaBrokerStorage;
  ElementService elementService;

  private static final String GROUP_ID = "group-ms-mock";

  public void startIfExists(MockFlow flow) {
    extractTrigger(flow).ifPresent(t ->
        listenerManager.start(
            t.brokerUrl(),
            t.brokerId(),
            t.topic(),
            GROUP_ID
        )
    );
  }

  public void stopIfExists(MockFlow flow) {
    extractTrigger(flow).ifPresent(t ->
        listenerManager.stop(
            t.brokerUrl(),
            t.topic(),
            GROUP_ID
        )
    );
  }

  public void restart(MockFlow flow) {
    stopIfExists(flow);
    startIfExists(flow);
  }

  private Optional<TriggerInfo> extractTrigger(MockFlow flow) {
    return flow.getFlowElements().stream()
        .filter(e -> "KAFKA_TRIGGER".equals(e.getName()))
        .findFirst()
        .map(e -> {
          String brokerId = elementService.getAttributeValue(e, "broker");
          String topic = elementService.getAttributeValue(e, "topic");

          KafkaBroker broker = kafkaBrokerStorage.findById(UUID.fromString(brokerId))
              .orElseThrow(() -> new BaseException(
                  404, "KAFKA_BROKER_NOT_FOUND", "Kafka broker not found"));

          return new TriggerInfo(broker.getUrl(), brokerId, topic);
        });
  }

  private record TriggerInfo(String brokerUrl, String brokerId, String topic) {
  }

  public List<ActiveKafkaListenerDto> getAllActive() {
    return listenerManager.getActiveListeners();
  }

}
