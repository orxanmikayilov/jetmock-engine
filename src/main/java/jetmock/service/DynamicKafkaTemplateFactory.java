package jetmock.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class DynamicKafkaTemplateFactory {

  private final Map<String, KafkaTemplate<String, String>> cache = new ConcurrentHashMap<>();

  public KafkaTemplate<String, String> getTemplate(String bootstrapServers) {
    if (bootstrapServers == null || bootstrapServers.isBlank()) {
      throw new IllegalArgumentException("bootstrapServers boş ola bilməz");
    }

    return cache.computeIfAbsent(bootstrapServers.trim(), bs -> {
      Map<String, Object> props = Map.of(
          ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bs,
          ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
          ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class,

          ProducerConfig.ACKS_CONFIG, "all",
          ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true,
          ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE,
          ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5
      );

      DefaultKafkaProducerFactory<String, String> pf = new DefaultKafkaProducerFactory<>(props);
      return new KafkaTemplate<>(pf);
    });
  }

}
