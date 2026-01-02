package jetmock.service;

import static jetmock.contant.Constant.CONDITION_BLACKLIST_REGEX;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import jetmock.domain.FlowElement;
import jetmock.domain.FlowMatchResult;
import jetmock.domain.MockFlow;
import jetmock.dto.payload.TriggerPayload;
import jetmock.exception.BaseException;
import jetmock.storage.MockFlowStorage;
import jetmock.util.ParserUtil;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class KafkaTriggerService {

  ElementService elementService;
  MockFlowStorage mockFlowStorage;
  AsyncFlowExecutor asyncFlowExecutor;

  private static final String KAFKA_TRIGGER = "KAFKA_TRIGGER";
  private static final String CONDITION_PREFIX = "CONDITION";

  public void processKafkaMessage(String brokerId, String topic, String message) {
    TriggerPayload triggerPayload = buildTriggerPayload(topic, message, brokerId);
    log.info("Kafka trigger invoked | brokerId={} | topic={}", brokerId, topic);

    MockFlow flow = findMock(brokerId, topic, triggerPayload);

    Map<Integer, Object> context = new HashMap<>();
    setTriggerContext(flow, triggerPayload, context);

    asyncFlowExecutor.runElementsAfterTrigger(flow.getId(), KAFKA_TRIGGER,
        context);
  }

  private TriggerPayload buildTriggerPayload(String topic, String message, String brokerId) {
    Map<String, Object> messageBody = ParserUtil.toMap(message);
    Map<String, Object> headers = new java.util.HashMap<>();
    headers.put("topic", topic);
    headers.put("brokerId", brokerId);

    return TriggerPayload.builder()
        .header(headers)
        .body(messageBody)
        .build();
  }

  private void setTriggerContext(MockFlow flow,
                                 TriggerPayload payload,
                                 Map<Integer, Object> context) {

    Integer kafkaTriggerRequestOrder = flow.getFlowElements().stream()
        .filter(e -> e.getName().equals(KAFKA_TRIGGER))
        .findAny()
        .orElseThrow(() ->
            new IllegalStateException("KAFKA_TRIGGER_REQUEST element was not found in the flow"))
        .getOrderNumber();

    context.put(kafkaTriggerRequestOrder, payload);
  }

  private MockFlow findMock(String brokerId,
                            String topic,
                            TriggerPayload triggerPayload) {

    FlowMatchResult mockMatch = findMockFlow(brokerId, topic, triggerPayload);
    return mockFlowStorage.findById(mockMatch.getId())
        .orElseThrow(() ->
            new BaseException(404, "MOCK_NOT_FOUND", "Matching mock flow was not found"));
  }

  private FlowMatchResult findMockFlow(String brokerId,
                                       String topic,
                                       TriggerPayload triggerPayload) {
    Set<MockFlow> flows = mockFlowStorage.findByKafkaTrigger(brokerId, topic);

    List<FlowMatchResult> candidates = flows.stream()
        .map(this::mapToFlowMatchResult)
        .collect(Collectors.toList());

    if (CollectionUtils.isEmpty(candidates)) {
      throw new BaseException(404, "MOCK_NOT_FOUND", "No matching mock flow found");
    }

    candidates.sort(Comparator.comparing(FlowMatchResult::getId));

    for (FlowMatchResult flow : candidates) {
      if (isConditionEligible(flow, triggerPayload)) {
        log.info(
            "Mock flow selected | flowId={} | condition={}",
            flow.getId(),
            (flow.getExpression() == null || flow.getExpression().isBlank())
                ? "NO_CONDITION"
                : flow.getExpression()
        );
        return flow;
      }
    }

    throw new BaseException(
        404,
        "MOCK_NOT_FOUND",
        "No mock flow matched the evaluated conditions"
    );
  }

  private FlowMatchResult mapToFlowMatchResult(MockFlow flow) {
    FlowElement conditionElement = flow.getFlowElements().stream()
        .filter(e -> CONDITION_PREFIX.equals(e.getName()))
        .findFirst()
        .orElseThrow(() ->
            new IllegalStateException(
                "MockFlow id=" + flow.getId() + " does not contain Condition element"
            ));

    String conditionExpression =
        elementService.getAttributeValue(conditionElement, "expression");

    return FlowMatchResult.builder()
        .id(flow.getId())
        .expression(conditionExpression)
        .build();
  }

  private boolean isConditionEligible(FlowMatchResult flow,
                                      TriggerPayload triggerPayload) {
    String condition = flow.getExpression();

    if (condition == null || condition.isBlank()) {
      return true;
    }

    if (isCodeInjectionAttack(condition)) {
      log.warn("Potential code injection detected in condition: {}", condition);
      return false;
    }

    try {
      StandardEvaluationContext context = new StandardEvaluationContext();
      context.setVariable("trigger", triggerPayload);

      ExpressionParser parser = new SpelExpressionParser();
      Expression exp = parser.parseExpression(condition);

      return Boolean.TRUE.equals(exp.getValue(context, Boolean.class));

    } catch (Exception e) {
      log.error("Condition evaluation failed | flowId={}", flow.getId(), e);
    }
    return false;
  }

  private boolean isCodeInjectionAttack(String condition) {
    return condition.matches(CONDITION_BLACKLIST_REGEX);
  }

}
