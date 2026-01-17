package jetmock.service;

import static jetmock.constant.Constant.CONDITION_BLACKLIST_REGEX;
import static jetmock.constant.Constant.DELIMITER;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jetmock.dto.payload.ApiResponsePayload;
import jetmock.dto.payload.TriggerPayload;
import jetmock.entity.ElementAttribute;
import jetmock.entity.FlowElement;
import jetmock.entity.FlowMatchResult;
import jetmock.entity.MockFlowEntity;
import jetmock.exception.BaseException;
import jetmock.repository.MockFlowRepository;
import jetmock.util.ParserUtil;
import jetmock.util.ThreadUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
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

  public ResponseEntity<Object> getMockResponse(String groupId,
                                                HttpServletRequest request,
                                                String requestBody,
                                                Map<String, Object> headers) {
    Map<Integer, Object> context = new HashMap<>();

    TriggerPayload triggerPayload =
        TriggerPayload.builder().header(headers).body(ParserUtil.toMap(requestBody))
            .param(extractQueryParams(request)).build();

    String path = requestUrlService.getRequestPath(groupId, request.getRequestURI());
    String method = request.getMethod();
    log.info("API trigger called. group={}, method={}, path={}", groupId, method, path);

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

  private MockFlowEntity findMock(String groupId, String method, String path,
                                  TriggerPayload triggerPayload) {
    FlowMatchResult mock = findMockFlow(groupId, method, path, triggerPayload);
    return mockFlowRepository.findById(mock.getId()).orElseThrow(() ->
        new BaseException(404, "MOCK_NOT_FOUND", "Mock data not found"));
  }

  private FlowMatchResult findMockFlow(String groupId, String method, String path,
                                       TriggerPayload triggerPayload) {

    List<FlowMatchResult> candidates = new ArrayList<>();
    candidates.addAll(mockFlowRepository.findMatchingByMethodAndPath(groupId, method, path));

    if (CollectionUtils.isEmpty(candidates)) {
      candidates = findMatchingMockFlow(groupId, method, path);
    }
    if (candidates.isEmpty()) {
      throw new BaseException(404, "MOCK_NOT_FOUND", "Mock data not found");
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
      return true;
    }

    if (isCodeInjectionAttack(condition)) {
      log.warn("Code injection risk: {}", condition);
      return false;
    }

    try {
      StandardEvaluationContext context = new StandardEvaluationContext();
      context.setVariable("trigger", new DslObject(triggerPayload));
      context.addPropertyAccessor(new DslPropertyAccessor());

      ExpressionParser parser = new SpelExpressionParser();
      Expression exp = parser.parseExpression(condition);
      return Boolean.TRUE.equals(exp.getValue(context, Boolean.class));

    } catch (Exception e) {
      log.error("Condition evaluation failed. condition={}", condition, e);
      return false;
    }
  }

  private boolean isCodeInjectionAttack(String condition) {
    return condition.matches(CONDITION_BLACKLIST_REGEX);
  }

  private List<FlowMatchResult> findMatchingMockFlow(String groupId, String method,
                                                     String path) {
    String[] requestUrlParts = path.split(DELIMITER);
    return mockFlowRepository.findMatchingByMethod(groupId, method).stream()
        .filter(mock -> requestUrlService.urlMatches(path, requestUrlParts))
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

  private Map<String, Object> extractQueryParams(HttpServletRequest request) {
    Map<String, Object> result = new HashMap<>();
    request.getParameterMap().forEach((k, v) -> {
      if (v == null) {
        result.put(k, null);
      } else if (v.length == 1) {
        result.put(k, v[0]);
      } else {
        result.put(k, List.of(v));
      }
    });
    return result;
  }

}
