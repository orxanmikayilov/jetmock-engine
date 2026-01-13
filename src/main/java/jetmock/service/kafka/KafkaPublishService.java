package jetmock.service.kafka;

import java.util.UUID;
import jetmock.dto.payload.KafkaPublisherPayload;
import jetmock.entity.KafkaBrokerEntity;
import jetmock.exception.BaseException;
import jetmock.repository.KafkaBrokerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaPublishService {

  private final KafkaBrokerRepository kafkaBrokerRepository;
  private final DynamicKafkaTemplateFactory templateFactory;

  public void publishToKafka(KafkaPublisherPayload payload) {
    log.info("Publish to kafka started: {}", payload);
    try {
      String brokerId = payload.getBroker();

      KafkaBrokerEntity broker = kafkaBrokerRepository
          .findById(UUID.fromString(brokerId))
          .orElseThrow(() -> new BaseException(
              404, "KAFKA_BROKER_NOT_FOUND", "Kafka broker not found: " + brokerId
          ));
      KafkaTemplate<String, String> kafkaTemplate =
          templateFactory.getTemplate(broker.getUrl());
      kafkaTemplate.send(payload.getTopic(), payload.getBody());
      log.info("Publish to kafka completed");
    } catch (Exception ex) {
      log.info("Publish to kafka process failed", ex);
    }
  }

}
