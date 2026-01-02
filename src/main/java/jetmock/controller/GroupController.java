package jetmock.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import jetmock.dto.GroupRequest;
import jetmock.dto.GroupResponse;
import jetmock.dto.UpdateGroupStatusRequest;
import jetmock.service.GroupService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/groups")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GroupController {

  GroupService groupService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public void createGroup(@RequestBody @Valid GroupRequest groupRequest) {
    groupService.createGroup(groupRequest);
  }

  @GetMapping
  public List<GroupResponse> getAll() {
    return groupService.getAll();
  }

  @GetMapping("/{id}")
  public GroupResponse findById(@PathVariable UUID id) {
    return groupService.findById(id);
  }

  @PatchMapping("/{id}/status")
  public void updateGroupStatus(@PathVariable UUID id,
                                @RequestBody @Valid UpdateGroupStatusRequest request) {
    groupService.updateGroupStatus(id, request);
  }

  @DeleteMapping("/{id}")
  public void deleteGroup(@PathVariable UUID id) {
    groupService.deleteGroup(id);
  }

}
