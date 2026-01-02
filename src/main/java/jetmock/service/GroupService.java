package jetmock.service;

import static lombok.AccessLevel.PRIVATE;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import jetmock.domain.MockGroup;
import jetmock.dto.GroupRequest;
import jetmock.dto.GroupResponse;
import jetmock.dto.UpdateGroupStatusRequest;
import jetmock.exception.BaseException;
import jetmock.mapper.GroupMapper;
import jetmock.storage.GroupStorage;
import jetmock.storage.MockFlowStorage;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class GroupService {

  GroupStorage groupStorage;
  MockFlowStorage mockFlowStorage;

  GroupMapper groupMapper = GroupMapper.INSTANCE;

  public void createGroup(GroupRequest groupRequest) {
    groupStorage.findByName(groupRequest.getName())
        .ifPresent(existing -> {
          throw new BaseException(400, "GROUP_ALREADY_EXISTS", "Group has already exists: ");
        });

    MockGroup entity = groupMapper.toEntity(groupRequest);
    entity.setIsActive(true);
    groupStorage.save(entity);
    log.info("Group created successfully: {}", entity.getName());
  }

  public List<GroupResponse> getAll() {
    List<MockGroup> groups = groupStorage.findAllWithMocks();
    return groups.stream()
        .map(g -> {
          GroupResponse r = groupMapper.toGroupResponse(g);
          r.setMockCount(mockFlowStorage.countByGroupId(g.getId()));
          return r;
        })
        .toList();
  }

  public void updateGroupStatus(UUID id, UpdateGroupStatusRequest request) {
    MockGroup entity = groupStorage.findById(id)
        .orElseThrow(() -> new BaseException(404, "GROUP_NOT_FOUND", "Group not found"));

    entity.setIsActive(request.getIsActive());
    groupStorage.save(entity);
  }

  public GroupResponse findById(UUID id) {
    return groupStorage.findById(id).map(groupMapper::toGroupResponse)
        .orElseThrow(() -> new BaseException(404, "GROUP_NOT_FOUND", "Group not found"));
  }

  public void deleteGroup(UUID id) {
    MockGroup entity = groupStorage.findById(id)
        .orElseThrow(() -> new BaseException(404, "GROUP_NOT_FOUND", "Group not found"));

    groupStorage.delete(entity.getId());

    log.info("Group deleted: {}", id);
  }

}
