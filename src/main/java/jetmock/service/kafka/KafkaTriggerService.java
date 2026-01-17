package jetmock.service.kafka;

import static jetmock.constant.Constant.CONDITION_BLACKLIST_REGEX;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jetmock.constant.ElementSchema;
import jetmock.dto.payload.TriggerPayload;
import jetmock.entity.FlowMatchResult;
import jetmock.entity.MockFlowEntity;
import jetmock.exception.BaseException;
import jetmock.repository.MockFlowRepository;
import jetmock.service.AsyncFlowExecutor;
import jetmock.service.DslObject;
import jetmock.service.DslPropertyAccessor;
import jetmock.util.ParserUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class KafkaTriggerService {

  MockFlowRepository mockFlowRepository;
  AsyncFlowExecutor asyncFlowExecutor;

  public void processKafkaMessage(String brokerId, String topic, String message) {
    TriggerPayload triggerPayload = buildTriggerPayload(topic, message, brokerId);
    log.info("Kafka trigger invoked | brokerId={} | topic={}", brokerId, topic);

    MockFlowEntity flow = findMock(brokerId, topic, triggerPayload);

    Map<Integer, Object> context = new HashMap<>();
    setTriggerContext(flow, triggerPayload, context);

    asyncFlowExecutor.runElementsAfterTrigger(flow.getId(), ElementSchema.KAFKA_TRIGGER.name(),
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

  private void setTriggerContext(MockFlowEntity flow,
                                 TriggerPayload payload,
                                 Map<Integer, Object> context) {

    Integer kafkaTriggerRequestOrder = flow.getFlowElements().stream()
        .filter(e -> e.getName().equals(ElementSchema.KAFKA_TRIGGER.name()))
        .findAny()
        .orElseThrow(() ->
            new IllegalStateException("KAFKA_TRIGGER_REQUEST element was not found in the flow"))
        .getOrderNumber();

    context.put(kafkaTriggerRequestOrder, payload);
  }

  private MockFlowEntity findMock(String brokerId,
                                  String topic,
                                  TriggerPayload triggerPayload) {

    FlowMatchResult mockMatch = findMockFlow(brokerId, topic, triggerPayload);
    return mockFlowRepository.findById(mockMatch.getId())
        .orElseThrow(() ->
            new BaseException(404, "MOCK_NOT_FOUND", "Matching mock flow was not found"));
  }

  private FlowMatchResult findMockFlow(String brokerId,
                                       String topic,
                                       TriggerPayload triggerPayload) {
    List<FlowMatchResult> candidates = mockFlowRepository.findByKafkaTrigger(brokerId, topic);

    if (candidates.isEmpty()) {
      throw new BaseException(404, "MOCK_NOT_FOUND", "Mock data not found");
    }

    for (FlowMatchResult flow : candidates) {
      if (isConditionEligible(flow, triggerPayload)) {
        return flow;
      }
    }

    throw new BaseException(
        404,
        "MOCK_NOT_FOUND",
        "No mock flow matched the evaluated conditions"
    );
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

}
