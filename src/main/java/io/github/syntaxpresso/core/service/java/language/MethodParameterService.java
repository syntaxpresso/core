package io.github.syntaxpresso.core.service.java.language;

import io.github.syntaxpresso.core.common.TSFile;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.treesitter.TSNode;

public class MethodParameterService {

  public List<TSNode> getAllMethodParameterNodes(TSFile tsFile, TSNode methodDeclarationNode) {
    if (tsFile == null
        || tsFile.getTree() == null
        || methodDeclarationNode == null
        || !"method_declaration".equals(methodDeclarationNode.getType())) {
      return Collections.emptyList();
    }
    String queryString =
        """
        (method_declaration
          parameters: (formal_parameters
            (formal_parameter) @parameter
          )
        )
        """;
    return tsFile.query(queryString).within(methodDeclarationNode).execute().nodes();
  }

  public List<Map<String, TSNode>> getMethodParameterNodeInfo(
      TSFile tsFile, TSNode methodParameterNode) {
    if (tsFile == null
        || tsFile.getTree() == null
        || !methodParameterNode.getType().equals("formal_parameter")) {
      return Collections.emptyList();
    }
    String queryString =
        """
        (formal_parameter
          type: (_) @parameterType
          name: (identifier) @parameterName
        ) @parameter
        """;
    return tsFile
        .query(queryString)
        .within(methodParameterNode)
        .returningAllCaptures()
        .execute()
        .captures();
  }

  public Optional<TSNode> getMethodParameterTypeNode(TSFile tsFile, TSNode methodParameterNode) {
    if (tsFile == null
        || tsFile.getTree() == null
        || !methodParameterNode.getType().equals("formal_parameter")) {
      return Optional.empty();
    }
    List<Map<String, TSNode>> methodParameterInfo =
        this.getMethodParameterNodeInfo(tsFile, methodParameterNode);
    for (Map<String, TSNode> map : methodParameterInfo) {
      TSNode typeNode = map.get("parameterType");
      if (typeNode != null) {
        return Optional.of(typeNode);
      }
    }
    return Optional.empty();
  }

  public Optional<TSNode> getMethodParameterNameNode(TSFile tsFile, TSNode methodParameterNode) {
    if (tsFile == null
        || tsFile.getTree() == null
        || !methodParameterNode.getType().equals("formal_parameter")) {
      return Optional.empty();
    }
    List<Map<String, TSNode>> methodParameterInfo =
        this.getMethodParameterNodeInfo(tsFile, methodParameterNode);
    for (Map<String, TSNode> map : methodParameterInfo) {
      TSNode nameNode = map.get("parameterName");
      if (nameNode != null) {
        return Optional.of(nameNode);
      }
    }
    return Optional.empty();
  }

  public List<TSNode> findAllMethodParameterNodeUsages(
      TSFile tsFile, TSNode methodParameterNode, TSNode methodDeclarationNode) {
    if (tsFile == null
        || tsFile.getTree() == null
        || methodParameterNode == null
        || methodDeclarationNode == null
        || !"formal_parameter".equals(methodParameterNode.getType())
        || !"method_declaration".equals(methodDeclarationNode.getType())) {
      return Collections.emptyList();
    }
    Optional<TSNode> methodParameterNameNode =
        this.getMethodParameterNameNode(tsFile, methodParameterNode);
    if (methodParameterNameNode.isEmpty()) {
      return Collections.emptyList();
    }
    String methodParameterName = tsFile.getTextFromNode(methodParameterNameNode.get());
    String queryString =
        String.format(
            """
            [
              (method_invocation
                object: (identifier) @name
              )
              (argument_list
                (identifier) @name
              )
              (variable_declarator
                value: (identifier) @name
              )
              (assignment_expression
                left: (identifier) @name
              )
              (assignment_expression
                right: (identifier) @name
              )
              (binary_expression
                left: (identifier) @name
              )
              (binary_expression
                right: (identifier) @name
              )
              (ternary_expression
                consequence: (identifier) @name
              )
              (ternary_expression
                condition: (identifier) @name
              )
              (#eq? @name "%s")
            ]
            """,
            methodParameterName);
    return tsFile.query(queryString).within(methodDeclarationNode).execute().nodes();
  }
}

