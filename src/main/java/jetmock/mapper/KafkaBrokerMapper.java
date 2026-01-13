package jetmock.mapper;

import java.util.List;
import jetmock.entity.KafkaBrokerEntity;
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

  KafkaBrokerEntity toKafkaBroker(KafkaBrokerRequest request);

  KafkaBrokerResponse toKafkaBrokerResponse(KafkaBrokerEntity broker);

  List<KafkaBrokerResponse> toKafkaBrokerResponse(List<KafkaBrokerEntity> brokers);

}
