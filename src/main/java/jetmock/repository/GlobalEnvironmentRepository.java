package jetmock.repository;

import java.util.List;
import java.util.Optional;
import jetmock.entity.GlobalVariableEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class GlobalEnvironmentRepository {

  private final RocksDbRepository commonRepository;

  private static final String COLLECTION_ID_KEY = "global_variable";

  public Optional<Object> findByKey(String id) {
    return commonRepository.findByKey(COLLECTION_ID_KEY, id, Object.class);
  }

  public List<Object> getAll() {
    return commonRepository.findAll(COLLECTION_ID_KEY, Object.class);
  }

  public void saveAll(List<GlobalVariableEntity> variables) {
    variables.forEach(this::save);
  }

  public void save(GlobalVariableEntity variable) {
    commonRepository.save(COLLECTION_ID_KEY, variable.getKey(), variable.getValue());
  }

}
