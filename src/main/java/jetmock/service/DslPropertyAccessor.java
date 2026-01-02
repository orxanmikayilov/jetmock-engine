package jetmock.service;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;

public class DslPropertyAccessor implements PropertyAccessor {

  @Override
  public Class<?>[] getSpecificTargetClasses() {
    return new Class<?>[] {DslObject.class};
  }

  @Override
  public boolean canRead(EvaluationContext context, Object target, String name) {
    return true;
  }

  @Override
  public TypedValue read(EvaluationContext context, Object target, String name) {
    DslObject dsl = (DslObject) target;
    Object value = dsl.get(name);
    return new TypedValue(new DslObject(value));
  }

  @Override
  public boolean canWrite(EvaluationContext context, Object target, String name) {
    return false;
  }

  @Override
  public void write(EvaluationContext context, Object target, String name, Object newValue) {
    throw new UnsupportedOperationException();
  }

}
