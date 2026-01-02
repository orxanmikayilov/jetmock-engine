package jetmock.mapper;

import java.util.List;
import jetmock.domain.KafkaBroker;
import jetmock.dto.KafkaBrokerRequest;
import jetmock.dto.KafkaBrokerResponse;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(
    unmappedSourcePolicy = ReportingPolicy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface KafkaBrokerMapper {

  KafkaBrokerMapper INSTANCE = Mappers.getMapper(KafkaBrokerMapper.class);

  KafkaBroker toKafkaBroker(KafkaBrokerRequest request);

  KafkaBrokerResponse toKafkaBrokerResponse(KafkaBroker broker);

  List<KafkaBrokerResponse> toKafkaBrokerResponse(List<KafkaBroker> brokers);

}
