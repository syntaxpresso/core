package io.github.syntaxpresso.core.service.java.extra;

import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.util.StringHelper;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.treesitter.TSNode;
import org.treesitter.TSQuery;
import org.treesitter.TSQueryCursor;
import org.treesitter.TSQueryMatch;

public class LocalVariableDeclarationService {

  private static final String LOCAL_VARIABLE_DECLARATION_QUERY =
      "(local_variable_declaration) @local_variable";

  /**
   * Finds all local variable declarations within a method. Results are unique and ordered by node's
   * start byte.
   *
   * @param methodDeclarationNode The TSNode of the method declaration.
   * @param file The TSFile containing the source code.
   * @return A list of unique local variable declaration nodes ordered by start byte.
   */
  public List<TSNode> findAllLocalVariableDeclarations(TSNode methodDeclarationNode, TSFile file) {
    if (methodDeclarationNode == null
        || !"method_declaration".equals(methodDeclarationNode.getType())) {
      return Collections.emptyList();
    }
    LinkedHashSet<TSNode> localVariables = new LinkedHashSet<>();
    TSQuery query = new TSQuery(file.getParser().getLanguage(), LOCAL_VARIABLE_DECLARATION_QUERY);
    TSQueryCursor cursor = new TSQueryCursor();
    cursor.exec(query, methodDeclarationNode);
    TSQueryMatch match = new TSQueryMatch();
    while (cursor.nextMatch(match)) {
      for (var capture : match.getCaptures()) {
        localVariables.add(capture.getNode());
      }
    }
    return localVariables.stream()
        .sorted(Comparator.comparingInt(TSNode::getStartByte))
        .collect(Collectors.toList());
  }

  /**
   * Finds the type node within a local variable declaration that matches a given name.
   *
   * @param localVariableDeclarationNode The TSNode for the local_variable_declaration.
   * @param file The TSFile containing the source code.
   * @param typeName The name of the type to find.
   * @return An Optional containing the found TSNode, or empty.
   */
  public Optional<TSNode> getVariableTypeNode(
      TSNode localVariableDeclarationNode, TSFile file, String typeName) {
    if (localVariableDeclarationNode == null
        || !"local_variable_declaration".equals(localVariableDeclarationNode.getType())) {
      return Optional.empty();
    }
    List<TSNode> typeNodes = file.query(localVariableDeclarationNode, "(type_identifier) @type");
    for (TSNode typeNode : typeNodes) {
      String typeNodeName = file.getTextFromNode(typeNode);
      if (typeName.equals(typeNodeName)) {
        return Optional.of(typeNode);
      }
    }
    return Optional.empty();
  }

  /**
   * Extracts the variable name from a local_variable_declaration node.
   *
   * @param localVariableDeclarationNode The TSNode representing the local_variable_declaration.
   * @param file The TSFile containing the source code.
   * @return An Optional containing the variable name node, or empty if not found.
   */
  public Optional<TSNode> getVariableNameNode(TSNode localVariableDeclarationNode, TSFile file) {
    if (localVariableDeclarationNode == null
        || !"local_variable_declaration".equals(localVariableDeclarationNode.getType())) {
      return Optional.empty();
    }
    TSNode variableDeclaratorNode = localVariableDeclarationNode.getChildByFieldName("declarator");
    if (variableDeclaratorNode == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(variableDeclaratorNode.getChildByFieldName("name"));
  }

  /**
   * Extracts the instantiated class name's node from an object_creation_expression within a
   * local_variable_declaration node.
   *
   * @param localVariableDeclarationNode The TSNode representing the local_variable_declaration.
   * @param file The TSFile containing the source code.
   * @param typeName The name of the class being instantiated.
   * @return An Optional containing the instantiated class name node, or empty if not found.
   */
  public Optional<TSNode> getVariableInstanceNode(
      TSNode localVariableDeclarationNode, TSFile file, String typeName) {
    if (localVariableDeclarationNode == null
        || !"local_variable_declaration".equals(localVariableDeclarationNode.getType())) {
      return Optional.empty();
    }
    TSNode variableDeclaratorNode = localVariableDeclarationNode.getChildByFieldName("declarator");
    if (variableDeclaratorNode == null) {
      return Optional.empty();
    }
    List<TSNode> typeNodes = file.query(variableDeclaratorNode, "(type_identifier) @type");
    for (TSNode typeNode : typeNodes) {
      String typeNodeName = file.getTextFromNode(typeNode);
      if (typeName.equals(typeNodeName)) {
        return Optional.of(typeNode);
      }
    }
    return Optional.empty();
  }

  /**
   * Checks if an identifier is part of a local variable declaration.
   *
   * @param identifierNode The identifier node to check.
   * @return True if this identifier is declaring a local variable.
   */
  public boolean isLocalVariableDeclaration(TSNode identifierNode) {
    if (identifierNode == null || identifierNode.isNull()) {
      return false;
    }
    TSNode parent = identifierNode.getParent();
    while (parent != null && !parent.isNull()) {
      if ("local_variable_declaration".equals(parent.getType())) {
        TSNode declarator = parent.getChildByFieldName("declarator");
        if (declarator != null && !declarator.isNull()) {
          TSNode nameNode = declarator.getChildByFieldName("name");
          if (nameNode != null && !nameNode.isNull() && nameNode.equals(identifierNode)) {
            return true;
          }
        }
      }
      parent = parent.getParent();
    }
    return false;
  }

  /**
   * Checks if an identifier represents usage of a local variable.
   *
   * @param identifierNode The identifier node to check.
   * @param methodDeclarationNode The method declaration node.
   * @param varName The variable name to check.
   * @param file The TSFile for text extraction.
   * @return True if this is a local variable usage.
   */
  public boolean isLocalVariableUsage(
      TSNode identifierNode, TSNode methodDeclarationNode, String varName, TSFile file) {
    if (identifierNode == null
        || identifierNode.isNull()
        || methodDeclarationNode == null
        || methodDeclarationNode.isNull()) {
      return false;
    }
    List<TSNode> localVars = findAllLocalVariableDeclarations(methodDeclarationNode, file);
    for (TSNode localVar : localVars) {
      if (localVar == null || localVar.isNull()) {
        continue;
      }
      Optional<TSNode> nameNode = getVariableNameNode(localVar, file);
      if (nameNode.isPresent()
          && !nameNode.get().isNull()
          && varName.equals(file.getTextFromNode(nameNode.get()))) {
        // Check if this identifier comes before the local variable declaration
        if (identifierNode.getStartByte() < localVar.getStartByte()) {
          return false; // It's before the declaration, so not a usage
        }
        return true; // It's after or at the declaration, so it's a usage
      }
    }
    return false;
  }

  /**
   * Checks if a type represents a collection (List, Set, ArrayList, etc.).
   *
   * @param typeText The type text to check.
   * @return true if the type is a collection type, false otherwise.
   */
  private boolean isCollectionType(String typeText) {
    if (typeText == null) {
      return false;
    }
    return typeText.startsWith("List<")
        || typeText.startsWith("Set<")
        || typeText.startsWith("ArrayList<")
        || typeText.startsWith("LinkedList<")
        || typeText.startsWith("HashSet<")
        || typeText.startsWith("LinkedHashSet<")
        || typeText.startsWith("TreeSet<")
        || typeText.startsWith("Collection<");
  }

  /**
   * Renames a local variable declaration, including its type, name, and instantiation.
   *
   * @param file The file containing the source code.
   * @param methodDeclarationNode The TSNode of the local variable declaration.
   * @param currentName The current name of the variable (PascalCase).
   * @param newName The new name for the variable (PascalCase).
   */
  public void renameLocalVariables(
      TSFile file, TSNode methodDeclarationNode, String currentName, String newName) {
    // Rename in reverse order for the same reason.
    List<TSNode> localVariableNodes =
        this.findAllLocalVariableDeclarations(methodDeclarationNode, file);
    for (TSNode localVariableNode : localVariableNodes.reversed()) {
      Optional<TSNode> localVariableTypeNode =
          this.getVariableTypeNode(localVariableNode, file, currentName);
      if (localVariableTypeNode.isEmpty()) {
        continue;
      }
      Optional<TSNode> localVariableNameNode = this.getVariableNameNode(localVariableNode, file);
      Optional<TSNode> localVariableInstanceNode =
          this.getVariableInstanceNode(localVariableNode, file, currentName);
      if (localVariableInstanceNode.isPresent()) {
        file.updateSourceCode(localVariableInstanceNode.get(), newName);
      }
      if (localVariableNameNode.isPresent()) {
        boolean isCollectionType = this.isCollectionType(file.getTextFromNode(localVariableNode));
        String currentLocalVariableName = file.getTextFromNode(localVariableNameNode.get());
        if (isCollectionType) {
          String pluralizedCurrentName = StringHelper.pluralizeCamelCase(currentName);
          if (currentLocalVariableName.equals(StringHelper.pascalToCamel(pluralizedCurrentName))) {
            String pluralizedNewName = StringHelper.pluralizeCamelCase(newName);
            file.updateSourceCode(
                localVariableNameNode.get(), StringHelper.pascalToCamel(pluralizedNewName));
          }
        }
        if (currentLocalVariableName.equals(StringHelper.pascalToCamel(currentName))) {
          file.updateSourceCode(localVariableNameNode.get(), StringHelper.pascalToCamel(newName));
        }
      }
      file.updateSourceCode(localVariableTypeNode.get(), newName);
    }
  }
}
