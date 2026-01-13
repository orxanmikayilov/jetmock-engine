package jetmock.dto;

import java.util.List;
import jetmock.constant.NodeType;

public record CollectionNodeResponse(
    String id,
    String name,
    NodeType type,
    String parentId,
    List<CollectionNodeResponse> children
) {
}