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

  public Optional<GlobalVariableEntity> findByKey(String id) {
    return commonRepository.findByKey(COLLECTION_ID_KEY, id, GlobalVariableEntity.class);
  }

  public List<GlobalVariableEntity> getAll() {
    return commonRepository.findAll(COLLECTION_ID_KEY, GlobalVariableEntity.class);
  }

  public void saveAll(List<GlobalVariableEntity> variables) {
    variables.forEach(this::save);
  }

  public void save(GlobalVariableEntity variable) {
    commonRepository.save(variable.getKey(), variable.getValue());
  }

}
