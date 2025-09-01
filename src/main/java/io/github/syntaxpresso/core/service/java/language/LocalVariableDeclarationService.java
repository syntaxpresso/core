package io.github.syntaxpresso.core.service.java.language;

import com.google.common.base.Strings;
import io.github.syntaxpresso.core.common.TSFile;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.treesitter.TSNode;

public class LocalVariableDeclarationService {
  public List<TSNode> getAllMethodParameterNodes(TSFile tsFile) {
    if (tsFile == null || tsFile.getTree() == null) {
      return Collections.emptyList();
    }
    String queryString =
        """
        [
          (local_variable_declaration
            type: (_)
            declarator: (variable_declarator
              name: (identifier)
            )
          ) @node
          (formal_parameter
            type: (_)
            name: (identifier)
          ) @node
          (field_declaration
            (modifiers)
            type: (_)
            declarator: (variable_declarator
              name: (identifier)
            )
          ) @node
        ]
        """;
    return tsFile.query(queryString).execute().nodes();
  }

  public List<Map<String, TSNode>> getLocalVariableDeclarationNodeInfo(
      TSFile tsFile, TSNode localVariableDeclarationNode) {
    if (tsFile == null || tsFile.getTree() == null) {
      return Collections.emptyList();
    }
    String queryString =
        """
        [
          (local_variable_declaration
            (modifiers)? @modifiers
            type: (_) @type
            declarator: (variable_declarator
              name: (identifier) @name
              value: (_)? @value
            )
          ) @node
          (formal_parameter
            (modifiers)? @modifiers
            type: (_) @type
            name: (identifier) @name
          ) @node
          (field_declaration
            (modifiers)? @modifiers
            type: (_) @type
            declarator: (variable_declarator
              name: (identifier) @name
              value: (_)? @value
            )
          ) @node
        ]
        """;
    return tsFile
        .query(queryString)
        .within(localVariableDeclarationNode)
        .returningAllCaptures()
        .execute()
        .captures();
  }

  public Optional<TSNode> getLocalVariableDeclarationNodeByCaptureName(
      TSFile tsFile, String captureName, TSNode localVariableDeclarationNode) {
    if (tsFile == null
        || tsFile.getTree() == null
        || (!localVariableDeclarationNode.getType().equals("field_declaration")
        && !localVariableDeclarationNode.getType().equals("formal_parameter")
        && !localVariableDeclarationNode.getType().equals("local_variable_declaration"))) {
      return Optional.empty();
    }
    List<Map<String, TSNode>> localVariableDeclarationInfo =
        this.getLocalVariableDeclarationNodeInfo(tsFile, localVariableDeclarationNode);
    for (Map<String, TSNode> map : localVariableDeclarationInfo) {
      TSNode node = map.get(captureName);
      if (node != null) {
        return Optional.of(node);
      }
    }
    return Optional.empty();
  }

  public Optional<TSNode> getLocalVariableDeclarationNameNode(
      TSFile tsFile, TSNode localVariableDeclarationNode) {
    return this.getLocalVariableDeclarationNodeByCaptureName(
        tsFile, "name", localVariableDeclarationNode);
  }

  public Optional<TSNode> getLocalVariableDeclarationValueNode(
      TSFile tsFile, TSNode localVariableDeclarationNode) {
    return this.getLocalVariableDeclarationNodeByCaptureName(
        tsFile, "type", localVariableDeclarationNode);
  }

  public Optional<TSNode> getLocalVariableDeclarationModifiersNode(
      TSFile tsFile, TSNode localVariableDeclarationNode) {
    return this.getLocalVariableDeclarationNodeByCaptureName(
        tsFile, "modifiers", localVariableDeclarationNode);
  }

  public List<TSNode> findLocalVariableDeclarationByType(TSFile tsFile, String type) {
    if (tsFile == null || tsFile.getTree() == null || Strings.isNullOrEmpty(type)) {
      return Collections.emptyList();
    }
    String queryString =
        String.format(
            """
            [
              (formal_parameter
                type: (_) @type
              ) @node
              (#eq? @type "%s")
              (local_variable_declaration
                type: (_) @type
              ) @node
              (#eq? @type "%s")
              (field_declaration
                type: (_) @type
              ) @node
              (#eq? @type "%s")
            ]
            """,
            type, type, type);
    return tsFile.query(queryString).execute().nodes();
  }
}
