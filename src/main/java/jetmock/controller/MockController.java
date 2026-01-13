package jetmock.controller;

import jakarta.validation.Valid;
import java.util.UUID;
import jetmock.dto.CreateMockRequest;
import jetmock.dto.MockDetailResponse;
import jetmock.service.CreateMockService;
import jetmock.service.MockFlowService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/mocks")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MockController {

  MockFlowService mockFlowService;
  CreateMockService createMockService;

  @GetMapping("/{mockId}")
  MockDetailResponse getByMockId(@PathVariable UUID mockId) {
    return mockFlowService.getByMockId(mockId);
  }

  @DeleteMapping("/{mockId}")
  void delete(@PathVariable UUID mockId) {
    mockFlowService.delete(mockId);
  }

  @PutMapping("/{mockId}")
  void upsert(@PathVariable UUID mockId, @RequestBody @Valid CreateMockRequest request) {
    createMockService.upsert(mockId, request);
  }

}
