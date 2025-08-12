package io.github.syntaxpresso.core.service.java.extra;

import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.util.StringHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.treesitter.TSException;
import org.treesitter.TSNode;
import org.treesitter.TSQuery;
import org.treesitter.TSQueryCapture;
import org.treesitter.TSQueryCursor;
import org.treesitter.TSQueryMatch;

public class FieldDeclarationService {
  /**
   * Finds the type node within a field declaration that matches a given name.
   *
   * @param declarationNode The TSNode for the field_declaration.
   * @param file The TSFile containing the source code.
   * @param typeName The name of the type to find.
   * @return An Optional containing the found TSNode, or empty.
   */
  public Optional<TSNode> getFieldTypeNode(TSNode declarationNode, TSFile file, String typeName) {
    if (declarationNode == null || !"field_declaration".equals(declarationNode.getType())) {
      return Optional.empty();
    }
    TSNode typeNode = declarationNode.getChildByFieldName("type");
    if (typeNode != null) {
      String foundTypeName = file.getTextFromRange(typeNode.getStartByte(), typeNode.getEndByte());
      if (typeName.equals(foundTypeName)) {
        return Optional.of(typeNode);
      }
    }
    return Optional.empty();
  }

  /**
   * Extracts the variable name from a field_declaration node.
   *
   * @param declarationNode The TSNode representing the local_variable_declaration.
   * @param file The TSFile containing the source code.
   * @return An Optional containing the variable name node, or empty if not found.
   */
  public Optional<TSNode> getFieldNameNode(TSNode declarationNode, TSFile file) {
    if (declarationNode == null || !"field_declaration".equals(declarationNode.getType())) {
      return Optional.empty();
    }
    TSNode variableDeclaratorNode = declarationNode.getChildByFieldName("declarator");
    if (variableDeclaratorNode == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(variableDeclaratorNode.getChildByFieldName("name"));
  }

  /**
   * Extracts the instantiated class name's node from an object_creation_expression within a
   * field_declaration node.
   *
   * @param declarationNode The TSNode representing the field_declaration.
   * @param file The TSFile containing the source code.
   * @param className The name of the class being instantiated.
   * @return An Optional containing the instantiated class name node, or empty if not found.
   */
  public Optional<TSNode> getFieldInstanceNode(
      TSNode declarationNode, TSFile file, String className) {
    if (declarationNode == null || !"field_declaration".equals(declarationNode.getType())) {
      return Optional.empty();
    }
    TSNode variableDeclaratorNode = declarationNode.getChildByFieldName("declarator");
    if (variableDeclaratorNode == null) {
      return Optional.empty();
    }
    TSNode objectCreationNode = variableDeclaratorNode.getChildByFieldName("value");
    try {
      if (objectCreationNode == null
          || !"object_creation_expression".equals(objectCreationNode.getType())) {
        return Optional.empty();
      }
    } catch (TSException e) {
      return Optional.empty();
    }
    TSNode typeIdentifierNode = objectCreationNode.getChildByFieldName("type");
    if (typeIdentifierNode != null) {
      String foundClassName =
          file.getTextFromRange(typeIdentifierNode.getStartByte(), typeIdentifierNode.getEndByte());
      if (className.equals(foundClassName)) {
        return Optional.of(typeIdentifierNode);
      }
    }
    return Optional.empty();
  }

  /**
   * Finds all usages of a field within a given file.
   *
   * @param file The file to search in.
   * @param fieldName The name of the field to find usages for (camel case).
   * @return A list of all nodes representing usages of the field.
   */
  public List<TSNode> findFieldUsages(TSFile file, String fieldName) {
    List<TSNode> usages = new ArrayList<>();
    String queryString =
        "((expression_statement (assignment_expression left: (identifier) @usage)))"
            + " ((expression_statement (assignment_expression left: (field_access field:"
            + " (identifier) @usage))))((expression_statement (identifier) @usage))";
    TSQuery query = new TSQuery(file.getParser().getLanguage(), queryString);
    TSQueryCursor cursor = new TSQueryCursor();
    cursor.exec(query, file.getTree().getRootNode());

    TSQueryMatch match = new TSQueryMatch();
    while (cursor.nextMatch(match))
      // Iterate through each match found by the cursor.
      // A match can have multiple captures, so we loop through them.
      for (TSQueryCapture capture : match.getCaptures()) {
        TSNode node = capture.getNode();
        String nodeText = file.getTextFromNode(node);
        if (nodeText.equals(fieldName)) {
          usages.add(node);
        }
      }
    return usages;
  }

  /**
   * Renames a field declaration, including its type, name, and instantiation.
   *
   * @param file The file containing the source code.
   * @param declarationNode The TSNode of the field declaration.
   * @param currentName The current name of the field (PascalCase).
   * @param newName The new name for the field (PascalCase).
   */
  public void renameFieldDeclaration(
      TSFile file, TSNode declarationNode, String currentName, String newName) {
    Optional<TSNode> fieldInstantiationNode =
        this.getFieldInstanceNode(declarationNode, file, currentName);
    Optional<TSNode> fieldNameNode = this.getFieldNameNode(declarationNode, file);
    Optional<TSNode> fieldTypeNode = this.getFieldTypeNode(declarationNode, file, currentName);
    if (fieldInstantiationNode.isPresent()) {
      file.updateSourceCode(fieldInstantiationNode.get(), newName);
    }
    if (fieldNameNode.isPresent()) {
      String newVariableName = StringHelper.pascalToCamel(newName);
      file.updateSourceCode(fieldNameNode.get(), newVariableName);
    }
    if (fieldTypeNode.isPresent()) {
      file.updateSourceCode(fieldTypeNode.get(), newName);
    }
  }

  /**
   * Renames all usages of a field in a file.
   *
   * @param file The file containing the source code.
   * @param currentName The current name of the field (PascalCase).
   * @param newName The new name for the field (PascalCase).
   */
  public void renameAllFieldUsages(TSFile file, String currentName, String newName) {
    String camelCaseCurrentName = StringHelper.pascalToCamel(currentName);
    String camelCaseNewName = StringHelper.pascalToCamel(newName);
    List<TSNode> allFieldUsages = this.findFieldUsages(file, camelCaseCurrentName);
    for (TSNode usage : allFieldUsages.reversed()) {
      file.updateSourceCode(usage, camelCaseNewName);
    }
  }
}
