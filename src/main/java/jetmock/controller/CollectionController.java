package jetmock.controller;

import java.util.List;
import jetmock.dto.CollectionNodeResponse;
import jetmock.dto.CreateNodeRequest;
import jetmock.dto.UpdateNodeRequest;
import jetmock.service.CollectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/collections")
public class CollectionController {

  private final CollectionService collectionService;

  @GetMapping("/tree")
  public List<CollectionNodeResponse> getFullTree() {
    return collectionService.getFullTree();
  }

  @PostMapping("/nodes")
  public void createNode(@RequestBody CreateNodeRequest request) {
    collectionService.createNode(request);
  }

  @DeleteMapping("/nodes/{id}")
  public void deleteNode(@PathVariable String id) {
    collectionService.deleteNode(id);
  }

  @PutMapping("/nodes/{id}")
  public void updateNode(@PathVariable String id, @RequestBody UpdateNodeRequest request) {
    collectionService.updateNode(id, request);
  }

}
