package jetmock.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jetmock.entity.KafkaBrokerEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaBrokerRepository {

  private final RocksDbRepository commonRepository;

  private static final String BROKER_ID_KEY = "kafka-broker";

  public void save(KafkaBrokerEntity broker) {
    if (broker.getId() == null) {
      broker.setId(UUID.randomUUID());
    }
    commonRepository.save(BROKER_ID_KEY, broker.getId().toString(), broker);
  }

  public Optional<KafkaBrokerEntity> findById(UUID id) {
    return commonRepository.findByKey(BROKER_ID_KEY, id.toString(), KafkaBrokerEntity.class);
  }

  public List<KafkaBrokerEntity> findAll() {
    return commonRepository.findAll(BROKER_ID_KEY, KafkaBrokerEntity.class);
  }

  public void delete(UUID id) {
    commonRepository.delete(BROKER_ID_KEY, id.toString());
  }

}
