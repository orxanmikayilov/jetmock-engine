package jetmock.service;

import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import jetmock.repository.GlobalEnvironmentRepository;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PlaceholderService {

  private static final Pattern PATTERN = Pattern.compile("\\{\\{(.+?)}}");
  private static final ExpressionParser PARSER = new SpelExpressionParser();
  GlobalEnvironmentRepository globalEnvironmentRepository;


  public String resolvePlaceholders(String template, Map<Integer, Object> context) {
    if (template == null) {
      return null;
    }

    Matcher matcher = PATTERN.matcher(template);
    StringBuffer sb = new StringBuffer();

    while (matcher.find()) {
      String expr = matcher.group(1).trim();
      String resolved = resolve(expr, context);
      matcher.appendReplacement(sb, Matcher.quoteReplacement(resolved));
    }

    matcher.appendTail(sb);
    return sb.toString();
  }

  private String resolve(String expr, Map<Integer, Object> context) {
    if ("random.uuid".equalsIgnoreCase(expr)) {
      return UUID.randomUUID().toString();
    }
    if (expr.startsWith("global.")) {
      return resolveGlobal(expr);
    }

    //    if (expr.startsWith("cache.")) {
    //      return dataCache.get(expr.substring(6)).orElse("");
    //    }

    // 🔥 MAIN DSL ENTRY
    return resolveDsl(expr, context);
  }

  private String resolveDsl(String expr, Map<Integer, Object> context) {
    try {
      int dot = expr.indexOf('.');
      if (dot == -1) {
        return "";
      }

      int order = Integer.parseInt(expr.substring(0, dot));
      Object raw = context.get(order);
      if (raw == null) {
        return "";
      }

      DslObject root = new DslObject(raw);

      StandardEvaluationContext spelContext =
          new StandardEvaluationContext(root);

      spelContext.addPropertyAccessor(new DslPropertyAccessor());

      Object value =
          PARSER.parseExpression(expr.substring(dot + 1))
              .getValue(spelContext);

      return value != null ? value.toString() : "";

    } catch (Exception e) {
      log.error("DSL resolve error: {}", expr, e);
      return "";
    }
  }

  private String resolveGlobal(String expr) {
    try {
      String key = expr.substring("global.".length()).trim();

      if (key.isEmpty()) {
        return "";
      }

      return globalEnvironmentRepository
          .findByKey(key)
          .map(Object::toString)
          .orElse("");

    } catch (Exception e) {
      log.error("Global resolve error: {}", expr, e);
      return "";
    }
  }


}
