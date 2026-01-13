package jetmock.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jetmock.constant.NodeType;

public record CreateNodeRequest(
    @NotBlank(message = "Name cannot be empty")
    String name,

    @NotNull(message = "Node type must be specified")
    NodeType type,

    String parentId
) {
}