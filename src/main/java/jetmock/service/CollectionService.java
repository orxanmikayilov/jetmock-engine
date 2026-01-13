package jetmock.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import jetmock.entity.CollectionNodeEntity;
import jetmock.dto.CollectionNodeResponse;
import jetmock.dto.CreateNodeRequest;
import jetmock.dto.UpdateNodeRequest;
import jetmock.exception.BaseException;
import jetmock.repository.CollectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CollectionService {

  private final CollectionRepository collectionRepository;

  //check nested validation package, group, flow

  public void createNode(CreateNodeRequest request) {
    String id = UUID.randomUUID().toString();

    CollectionNodeEntity node = new CollectionNodeEntity(
        id, request.name(), request.type(), request.parentId());

    collectionRepository.save(node);
  }

  public List<CollectionNodeResponse> getFullTree() {
    List<CollectionNodeEntity> allNodes = collectionRepository.findAll();

    Map<String, CollectionNodeResponse> nodeMap = allNodes.stream()
        .collect(Collectors.toMap(
            CollectionNodeEntity::getId,
            node -> new CollectionNodeResponse(
                node.getId(), node.getName(), node.getType(),
                node.getParentId(), new ArrayList<>()
            )
        ));

    List<CollectionNodeResponse> rootNodes = new ArrayList<>();

    for (CollectionNodeEntity entity : allNodes) {
      CollectionNodeResponse current = nodeMap.get(entity.getId());
      if (entity.getParentId() == null || entity.getParentId().isEmpty()) {
        rootNodes.add(current);
      } else {
        CollectionNodeResponse parent = nodeMap.get(entity.getParentId());
        if (parent != null) {
          parent.children().add(current);
        }
      }
    }
    return rootNodes;
  }

  public void deleteNode(String id) {
    collectionRepository.delete(id);
  }

  public void updateNode(String id, UpdateNodeRequest request) {
    CollectionNodeEntity existingNode = collectionRepository.findById(id).orElseThrow(
        () -> new BaseException(404, "NODE_NOT_FOUND", "Node not found with id: " + id));

    CollectionNodeEntity updatedNode = new CollectionNodeEntity(
        existingNode.getId(),
        request.getName(),
        existingNode.getType(),
        existingNode.getParentId()
    );

    collectionRepository.save(updatedNode);
  }

}