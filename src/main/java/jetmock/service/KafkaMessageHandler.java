package jetmock.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KafkaMessageHandler {

  private final KafkaTriggerService kafkaTriggerService;

  public void handle(String brokerId, String topic, String message) {
    kafkaTriggerService.processKafkaMessage(brokerId, topic, message);
  }

}
