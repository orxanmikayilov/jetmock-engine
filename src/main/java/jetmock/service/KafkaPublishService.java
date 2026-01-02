package jetmock.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jetmock.domain.KafkaBroker;
import jetmock.dto.payload.KafkaPublisherPayload;
import jetmock.exception.BaseException;
import jetmock.storage.KafkaBrokerStorage;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaPublishService {

  private final KafkaBrokerStorage kafkaBrokerStorage;
  private final DynamicKafkaTemplateFactory templateFactory;

  public void publishToKafka(KafkaPublisherPayload payload) {
    log.info("Publish to kafka started: {}", payload);
    try {
      String brokerId = payload.getBroker();

      KafkaBroker broker = kafkaBrokerStorage
          .findById(UUID.fromString(brokerId))
          .orElseThrow(() -> new BaseException(
              404, "NOT_FOUND",
              "Kafka broker tapılmadı: " + brokerId
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
