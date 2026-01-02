package jetmock.service;

import static lombok.AccessLevel.PRIVATE;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import jetmock.domain.KafkaBroker;
import jetmock.dto.KafkaBrokerRequest;
import jetmock.dto.KafkaBrokerResponse;
import jetmock.exception.BaseException;
import jetmock.mapper.KafkaBrokerMapper;
import jetmock.storage.KafkaBrokerStorage;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class SettingsService {

  KafkaBrokerStorage kafkaBrokerStorage;
  KafkaBrokerMapper kafkaBrokerMapper = KafkaBrokerMapper.INSTANCE;

  public List<KafkaBrokerResponse> getAll() {
    return kafkaBrokerMapper.toKafkaBrokerResponse(
        kafkaBrokerStorage.findAll()
    );
  }

  public void save(KafkaBrokerRequest request) {
    KafkaBroker broker = kafkaBrokerMapper.toKafkaBroker(request);
    kafkaBrokerStorage.save(broker);
    log.info("Kafka broker created: {}", broker);
  }

  public void delete(UUID id) {
    kafkaBrokerStorage.findById(id)
        .orElseThrow(() ->
            new BaseException(404, "KAFKA_BROKER_NOT_FOUND", "Kafka Broker not found"));

    kafkaBrokerStorage.delete(id);
    log.info("Kafka broker deleted: {}", id);
  }

  public KafkaBrokerResponse findById(UUID id) {
    KafkaBroker broker = kafkaBrokerStorage.findById(id)
        .orElseThrow(() ->
            new BaseException(404, "KAFKA_BROKER_NOT_FOUND", "Kafka Broker not found"));

    return kafkaBrokerMapper.toKafkaBrokerResponse(broker);
  }

}
