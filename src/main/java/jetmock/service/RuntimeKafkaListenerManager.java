package jetmock.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import jetmock.dto.ActiveKafkaListenerDto;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RuntimeKafkaListenerManager {

  KafkaMessageHandler messageHandler;

  Map<String, KafkaMessageListenerContainer<String, String>> containers =
      new ConcurrentHashMap<>();

  public void start(String brokerUrl, String brokerId, String topic, String groupId) {
    String key = key(brokerUrl, topic, groupId);
    if (containers.containsKey(key)) {
      return;
    }

    Map<String, Object> props = listenerProps(brokerUrl, groupId);
    var consumerFactory = new DefaultKafkaConsumerFactory<String, String>(props);

    var containerProps = new ContainerProperties(topic);
    containerProps.setMessageListener(
        (MessageListener<String, String>) record ->
            messageHandler.handle(
                brokerId,
                record.topic(),
                record.value()
            )
    );

    var container =
        new KafkaMessageListenerContainer<>(consumerFactory, containerProps);

    container.setBeanName("runtime-kafka-" + key);
    container.start();
    containers.put(key, container);

    log.info("Kafka listener STARTED | {}", key);
  }

  public void stop(String brokerUrl, String topic, String groupId) {
    String key = key(brokerUrl, topic, groupId);
    var container = containers.remove(key);

    if (container == null) {
      log.info("Kafka listener already stopped or not found | {}", key);
      return;
    }

    container.stop();
    log.info("Kafka listener STOPPED | {}", key);
  }

  private Map<String, Object> listenerProps(String brokerUrl, String groupId) {
    Map<String, Object> props = new HashMap<>();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, brokerUrl);
    props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
    return props;
  }

  private String key(String brokerUrl, String topic, String groupId) {
    return brokerUrl + "|" + topic + "|" + groupId;
  }

  public List<ActiveKafkaListenerDto> getActiveListeners() {
    return containers.entrySet().stream()
        .map(entry -> {
          String key = entry.getKey();
          KafkaMessageListenerContainer<?, ?> container = entry.getValue();

          String[] parts = key.split("\\|", 3);

          return ActiveKafkaListenerDto.builder()
              .brokerUrl(parts[0])
              .topic(parts[1])
              .groupId(parts[2])
              .running(container.isRunning())
              .build();
        })
        .toList();
  }

}
