package jetmock.service;

import static lombok.AccessLevel.PRIVATE;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import jetmock.entity.KafkaBrokerEntity;
import jetmock.dto.KafkaBrokerRequest;
import jetmock.dto.KafkaBrokerResponse;
import jetmock.exception.BaseException;
import jetmock.mapper.KafkaBrokerMapper;
import jetmock.repository.KafkaBrokerRepository;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class SettingsService {

  KafkaBrokerRepository kafkaBrokerRepository;
  KafkaBrokerMapper kafkaBrokerMapper = KafkaBrokerMapper.INSTANCE;

  public List<KafkaBrokerResponse> getAll() {
    return kafkaBrokerMapper.toKafkaBrokerResponse(
        kafkaBrokerRepository.findAll()
    );
  }

  public void save(KafkaBrokerRequest request) {
    KafkaBrokerEntity broker = kafkaBrokerMapper.toKafkaBroker(request);
    kafkaBrokerRepository.save(broker);
    log.info("Kafka broker created: {}", broker);
  }

  public void delete(UUID id) {
    kafkaBrokerRepository.findById(id)
        .orElseThrow(() ->
            new BaseException(404, "KAFKA_BROKER_NOT_FOUND", "Kafka Broker not found"));

    kafkaBrokerRepository.delete(id);
    log.info("Kafka broker deleted: {}", id);
  }

  public KafkaBrokerResponse findById(UUID id) {
    KafkaBrokerEntity broker = kafkaBrokerRepository.findById(id)
        .orElseThrow(() ->
            new BaseException(404, "KAFKA_BROKER_NOT_FOUND", "Kafka Broker not found"));

    return kafkaBrokerMapper.toKafkaBrokerResponse(broker);
  }

}
