package io.github.syntaxpresso.core.service.java.extra;

import io.github.syntaxpresso.core.common.TSFile;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.treesitter.TSNode;

@RequiredArgsConstructor
public class LocalVariableDeclarationService {

  private final VariableNamingService variableNamingService;

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
    return file.query(methodDeclarationNode, LOCAL_VARIABLE_DECLARATION_QUERY);
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
        boolean isCollectionType =
            this.variableNamingService.isCollectionType(file.getTextFromNode(localVariableNode));
        String currentLocalVariableName = file.getTextFromNode(localVariableNameNode.get());
        String newLocalVariableName =
            this.variableNamingService.generateNewVariableName(
                currentLocalVariableName, currentName, newName, isCollectionType);
        if (!currentLocalVariableName.equals(newLocalVariableName)) {
          file.updateSourceCode(localVariableNameNode.get(), newLocalVariableName);
        }
      }
      file.updateSourceCode(localVariableTypeNode.get(), newName);
    }
  }

  /**
   * Renames all local variables of the specified type across all methods in a file.
   *
   * @param file The file containing the source code.
   * @param currentName The current name of the variable type (PascalCase).
   * @param newName The new name for the variable type (PascalCase).
   */
  public void renameLocalVariablesInFile(TSFile file, String currentName, String newName) {
    List<TSNode> methodDeclarationNodes = file.query("(method_declaration) @method");
    for (TSNode methodDeclarationNode : methodDeclarationNodes) {
      this.renameLocalVariables(file, methodDeclarationNode, currentName, newName);
    }
  }
}
