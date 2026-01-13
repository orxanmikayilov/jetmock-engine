package jetmock.service;

import static jetmock.constant.Constant.CONDITION_BLACKLIST_REGEX;
import static jetmock.constant.Constant.DELIMITER;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import jetmock.entity.ElementAttribute;
import jetmock.entity.FlowElement;
import jetmock.entity.FlowMatchResult;
import jetmock.entity.MockFlowEntity;
import jetmock.dto.payload.ApiResponsePayload;
import jetmock.dto.payload.TriggerPayload;
import jetmock.exception.BaseException;
import jetmock.repository.MockFlowRepository;
import jetmock.util.ParserUtil;
import jetmock.util.ThreadUtil;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MockService {

  ElementService elementService;
  MockFlowRepository mockFlowRepository;
  RequestUrlService requestUrlService;
  AsyncFlowExecutor asyncFlowExecutor;
  PlaceholderService placeholderService;

  public ResponseEntity<Object> getMockResponse(String groupName,
                                                HttpServletRequest request,
                                                String requestBody,
                                                Map<String, Object> headers) {
    UUID groupId = null;
    Map<Integer, Object> context = new HashMap<>();

    TriggerPayload triggerPayload =
        TriggerPayload.builder().header(headers).body(ParserUtil.toMap(requestBody)).build();

    String path = requestUrlService.getRequestPath(groupName, request.getRequestURI());
    String method = request.getMethod();
    log.info("API trigger called. group={}, method={}, path={}", groupName, method, path);

    MockFlowEntity flow = findMock(groupId, method, path, triggerPayload);

    Map<String, String> pathVariables =
        requestUrlService.extractPathVariables(getTriggerPath(flow), path);
    triggerPayload.setPath(pathVariables);

    setTriggerContext(flow, triggerPayload, context);

    runElementsBeforeResponse(flow, context);

    ResponseEntity<Object> responseEntity = returnResponse(flow, context);
    asyncFlowExecutor.runElementsAfterTrigger(
        flow.getId(),
        "API_TRIGGER_RESPONSE",
        context
    );
    return responseEntity;
  }

  private void setTriggerContext(MockFlowEntity flow, TriggerPayload payload,
                                 Map<Integer, Object> context) {
    Integer apiTriggerRequestOrder = flow.getFlowElements().stream()
        .filter(e -> e.getName().equals("API_TRIGGER_REQUEST"))
        .findAny()
        .orElseThrow()
        .getOrderNumber();

    context.put(apiTriggerRequestOrder, payload);
  }

  private ResponseEntity<Object> returnResponse(MockFlowEntity flow,
                                                Map<Integer, Object> context) {
    FlowElement flowElement = flow.getFlowElements().stream()
        .filter(e -> e.getName().equals("API_TRIGGER_RESPONSE"))
        .findFirst()
        .orElseThrow();
    List<ElementAttribute> apiTriggerAttributes = flowElement.getAttributes();

    ApiResponsePayload apiResponsePayload =
        elementService.mapAttributes(apiTriggerAttributes, ApiResponsePayload.class);
    ThreadUtil.sleep(apiResponsePayload.getLatency());

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    if (apiResponsePayload.getHeader() != null) {
      ParserUtil.toStringMap(apiResponsePayload.getHeader())
          .forEach(headers::add);
    }

    String body = placeholderService.resolvePlaceholders(apiResponsePayload.getBody(), context);
    context.put(flowElement.getOrderNumber(), apiResponsePayload);

    return ResponseEntity
        .status(apiResponsePayload.getStatus())
        .headers(headers)
        .body(body);
  }

  private MockFlowEntity findMock(UUID groupId, String method, String path,
                                  TriggerPayload triggerPayload) {
    FlowMatchResult mock = findMockFlow(groupId, method, path, triggerPayload);
    return mockFlowRepository.findById(mock.getId()).orElseThrow(() ->
        new BaseException(404, "MOCK_NOT_FOUND", "Mock data not found"));
  }

  private FlowMatchResult findMockFlow(UUID groupId, String method, String path,
                                       TriggerPayload triggerPayload) {

    List<FlowMatchResult> candidates = new ArrayList<>();
    mockFlowRepository.findMatchingByMethodAndPath(groupId, method, path).ifPresent(candidates::add);

    if (CollectionUtils.isEmpty(candidates)) {
      candidates = findMatchingMockFlow(groupId, method, path);
    }
    if (candidates.isEmpty()) {
      throw new BaseException(404, "MOCK_NOT_FOUND", "Mock data not found");
    }

    if (candidates.size() == 1) {
      return candidates.get(0);
    }

    for (FlowMatchResult flow : candidates) {
      if (isConditionEligible(flow, triggerPayload)) {
        return flow;
      }
    }
    throw new BaseException(404, "MOCK_NOT_FOUND", "Mock data not found");
  }

  private boolean isConditionEligible(FlowMatchResult flow, TriggerPayload triggerPayload) {
    String condition = flow.getExpression();

    if (condition == null || condition.isBlank()) {
      log.info("condition is blank");
      return false;
    }

    if (isCodeInjectionAttack(condition)) {
      log.warn("Code injection risk: {}", condition);
      return false;
    }

    try {
      StandardEvaluationContext context = new StandardEvaluationContext();
      context.setVariable("trigger", triggerPayload);

      ExpressionParser parser = new SpelExpressionParser();
      Expression exp = parser.parseExpression(condition);
      return Boolean.TRUE.equals(exp.getValue(context, Boolean.class));

    } catch (Exception e) {
      log.error("Condition evaluation failed", e);
    }
    return false;
  }

  private boolean isCodeInjectionAttack(String condition) {
    return condition.matches(CONDITION_BLACKLIST_REGEX);
  }

  private List<FlowMatchResult> findMatchingMockFlow(UUID groupId, String method,
                                                     String requestUrl) {
    String[] requestUrlParts = requestUrl.split(DELIMITER);
    return mockFlowRepository.findMatchingByMethod(groupId, method).stream()
        .filter(mock -> requestUrlService.urlMatches(mock.getPath(), requestUrlParts))
        .toList();
  }

  private String getTriggerPath(MockFlowEntity flow) {
    return flow.getFlowElements().stream()
        .filter(e -> e.getName().equals("API_TRIGGER_REQUEST"))
        .map(e -> elementService.getAttributeValue(e, "path"))
        .findFirst()
        .orElse(null);
  }

  private void runElementsBeforeResponse(MockFlowEntity flow, Map<Integer, Object> context) {
    List<FlowElement> elements = new ArrayList<>(flow.getFlowElements());
    elements.sort(Comparator.comparing(FlowElement::getOrderNumber));

    int start = elementService.findIndex(elements, "API_TRIGGER_REQUEST");
    int end = elementService.findIndex(elements, "API_TRIGGER_RESPONSE");

    for (int i = start + 1; i < end; i++) {
      elementService.executeElementAction(elements.get(i), context);
    }
  }

}
