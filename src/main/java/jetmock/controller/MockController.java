package jetmock.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import jetmock.dto.CreateMockRequest;
import jetmock.dto.MockDetailResponse;
import jetmock.dto.MockResponse;
import jetmock.service.CreateMockService;
import jetmock.service.MockFlowService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/mocks")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MockController {

  MockFlowService mockFlowService;
  CreateMockService createMockService;

  @GetMapping
  List<MockResponse> getByGroupId(@RequestParam UUID groupId) {
    return mockFlowService.getByGroupId(groupId);
  }

  @GetMapping("/{mockId}")
  MockDetailResponse getByMockId(@PathVariable UUID mockId) {
    return mockFlowService.getByMockId(mockId);
  }

  @DeleteMapping("/{mockId}")
  void delete(@PathVariable UUID mockId) {
    mockFlowService.delete(mockId);
  }

  @PostMapping
  void save(@RequestBody @Valid CreateMockRequest request) {
    createMockService.save(request);
  }

  @PutMapping("/{mockId}")
  void update(@PathVariable UUID mockId, @RequestBody @Valid CreateMockRequest request) {
    createMockService.update(mockId, request);
  }

}
