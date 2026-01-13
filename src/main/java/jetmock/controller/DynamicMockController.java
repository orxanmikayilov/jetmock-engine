package jetmock.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import jetmock.service.MockService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DynamicMockController {

  private final MockService mockService;

  @RequestMapping(path = "/{groupName:^(?!swagger-ui|v3|api-docs).+}/**")
  public ResponseEntity<Object> getMockResponse(@PathVariable String groupName,
                                                HttpServletRequest request,
                                                @RequestHeader Map<String, Object> headers,
                                                @RequestBody(required = false)
                                                String requestBody) {
    return mockService.getMockResponse(groupName, request, requestBody, headers);
  }

}
