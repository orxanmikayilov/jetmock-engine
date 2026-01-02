package jetmock.mapper;

import java.util.List;
import jetmock.domain.MockGroup;
import jetmock.dto.GroupRequest;
import jetmock.dto.GroupResponse;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface GroupMapper {

  GroupMapper INSTANCE = Mappers.getMapper(GroupMapper.class);

  MockGroup toEntity(GroupRequest groupRequest);

  default List<GroupResponse> toGroupResponses(List<MockGroup> groupEntities) {
    return groupEntities.stream()
        .map(this::toGroupResponseWithMockCount)
        .toList();
  }

  //    @Mapping(target = "mockCount", expression =
  //      "java(groupEntity.getMockFlows() != null ? groupEntity.getMockFlows().size() : 0)")
  GroupResponse toGroupResponseWithMockCount(MockGroup groupEntity);

  GroupResponse toGroupResponse(MockGroup groupEntity);

}
