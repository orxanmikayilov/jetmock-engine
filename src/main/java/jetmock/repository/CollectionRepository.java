package jetmock.repository;

import java.util.List;
import java.util.Optional;
import jetmock.entity.CollectionNodeEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CollectionRepository {

  private final RocksDbRepository commonRepository;

  private static final String COLLECTION_ID_KEY = "collection";

  public Optional<CollectionNodeEntity> findById(String id) {
    return commonRepository.findByKey(COLLECTION_ID_KEY, id, CollectionNodeEntity.class);
  }

  public void save(CollectionNodeEntity entity) {
    commonRepository.save(COLLECTION_ID_KEY, entity.getId(), entity);
  }

  public List<CollectionNodeEntity> findAll() {
    return commonRepository.findAll(COLLECTION_ID_KEY, CollectionNodeEntity.class);
  }

  public void delete(String id) {
    commonRepository.delete(COLLECTION_ID_KEY, id);
  }

}