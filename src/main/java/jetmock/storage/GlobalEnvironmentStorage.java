package jetmock.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import jetmock.domain.GlobalVariable;
import org.rocksdb.RocksDB;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GlobalEnvironmentStorage {

  private final RocksDB db;
  private final ObjectMapper mapper;

  private static final String ENV_KEY = "global"; // 🔥 TƏK KEY

  @SneakyThrows
  public List<GlobalVariable> getAll() {
    byte[] value = db.get(bytes(ENV_KEY));
    if (value == null) {
      return new ArrayList<>();
    }

    return mapper.readValue(
        value,
        mapper.getTypeFactory()
            .constructCollectionType(List.class, GlobalVariable.class)
    );
  }

  @SneakyThrows
  public void saveAll(List<GlobalVariable> variables) {
    db.put(bytes(ENV_KEY), mapper.writeValueAsBytes(variables));
  }

  public void upsert(GlobalVariable variable) {
    validate(variable);

    List<GlobalVariable> vars = getAll();


    vars.removeIf(v -> v.getKey().equals(variable.getKey()));
    variable.setKey(variable.getKey());
    vars.add(variable);

    saveAll(vars);
  }

  private void validate(GlobalVariable variable) {
    if (variable == null) {
      throw new IllegalArgumentException("GlobalVariable must not be null");
    }

    if (variable.getKey() == null || variable.getKey().isBlank()) {
      throw new IllegalArgumentException("GlobalVariable.key must not be blank");
    }
  }

  private byte[] bytes(String k) {
    return k.getBytes(StandardCharsets.UTF_8);
  }

  public Optional<Object> getValueByKey(String key) {
    List<GlobalVariable> vars = getAll();
    return vars.stream()
        .filter(v -> v.getKey().equalsIgnoreCase(key))
        .map(GlobalVariable::getValue)
        .findFirst();
  }
}
