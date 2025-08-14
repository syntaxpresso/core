package io.github.syntaxpresso.core.service.java.extra;

import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.util.StringHelper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.treesitter.TSNode;
import org.treesitter.TSQuery;
import org.treesitter.TSQueryCursor;
import org.treesitter.TSQueryMatch;

public class LocalVariableDeclarationService {

  private static final String LOCAL_VARIABLE_DECLARATION_QUERY =
      "(local_variable_declaration) @local_variable";

  /**
   * Finds all local variable declarations within a method.
   *
   * @param methodNode The TSNode of the method declaration.
   * @param tsFile The TSFile containing the source code.
   * @return A list of all local variable declaration nodes.
   */
  public List<TSNode> findAllLocalVariableDeclarations(TSNode methodNode, TSFile tsFile) {
    if (methodNode == null || !"method_declaration".equals(methodNode.getType())) {
      return Collections.emptyList();
    }
    List<TSNode> localVariables = new ArrayList<>();
    TSQuery query = new TSQuery(tsFile.getParser().getLanguage(), LOCAL_VARIABLE_DECLARATION_QUERY);
    TSQueryCursor cursor = new TSQueryCursor();
    cursor.exec(query, methodNode);
    TSQueryMatch match = new TSQueryMatch();
    while (cursor.nextMatch(match)) {
      for (var capture : match.getCaptures()) {
        localVariables.add(capture.getNode());
      }
    }
    return localVariables;
  }

  /**
   * Finds the type node within a local variable declaration that matches a given name.
   *
   * @param declarationNode The TSNode for the local_variable_declaration.
   * @param file The TSFile containing the source code.
   * @param typeName The name of the type to find.
   * @return An Optional containing the found TSNode, or empty.
   */
  public Optional<TSNode> getVariableTypeNode(
      TSNode declarationNode, TSFile file, String typeName) {
    if (declarationNode == null
        || !"local_variable_declaration".equals(declarationNode.getType())) {
      return Optional.empty();
    }
    List<TSNode> typeNodes = file.query(declarationNode, "(type_identifier) @type");
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
   * @param declarationNode The TSNode representing the local_variable_declaration.
   * @param file The TSFile containing the source code.
   * @return An Optional containing the variable name node, or empty if not found.
   */
  public Optional<TSNode> getVariableNameNode(TSNode declarationNode, TSFile file) {
    if (declarationNode == null
        || !"local_variable_declaration".equals(declarationNode.getType())) {
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
   * local_variable_declaration node.
   *
   * @param declarationNode The TSNode representing the local_variable_declaration.
   * @param file The TSFile containing the source code.
   * @param className The name of the class being instantiated.
   * @return An Optional containing the instantiated class name node, or empty if not found.
   */
  public Optional<TSNode> getVariableInstanceNode(
      TSNode declarationNode, TSFile file, String className) {
    if (declarationNode == null
        || !"local_variable_declaration".equals(declarationNode.getType())) {
      return Optional.empty();
    }
    TSNode variableDeclaratorNode = declarationNode.getChildByFieldName("declarator");
    if (variableDeclaratorNode == null) {
      return Optional.empty();
    }
    TSNode objectCreationNode = variableDeclaratorNode.getChildByFieldName("value");
    if (objectCreationNode == null
        || !"object_creation_expression".equals(objectCreationNode.getType())) {
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
   * Renames a local variable declaration, including its type, name, and instantiation.
   *
   * @param file The file containing the source code.
   * @param declarationNode The TSNode of the local variable declaration.
   * @param currentName The current name of the variable (PascalCase).
   * @param newName The new name for the variable (PascalCase).
   */
  public void renameLocalVariableDeclaration(
      TSFile file, TSNode declarationNode, String currentName, String newName) {
    // Rename in reverse order for the same reason.
    Optional<TSNode> classDeclarationInstantiationNode =
        this.getVariableInstanceNode(declarationNode, file, currentName);
    Optional<TSNode> classDeclarationVariableNode = this.getVariableNameNode(declarationNode, file);
    Optional<TSNode> classDeclarationTypeNode =
        this.getVariableTypeNode(declarationNode, file, currentName);
    if (classDeclarationInstantiationNode.isPresent()) {
      file.updateSourceCode(classDeclarationInstantiationNode.get(), newName);
    }
    if (classDeclarationInstantiationNode.isPresent()) {
      String newVariableName = StringHelper.pascalToCamel(newName);
      file.updateSourceCode(classDeclarationVariableNode.get(), newVariableName);
    }
    if (classDeclarationTypeNode.isPresent()) {
      file.updateSourceCode(classDeclarationTypeNode.get(), newName);
    }
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
        // Check if this identifier comes after the local variable declaration
        if (identifierNode.getStartByte() > localVar.getEndByte()) {
          return true; // It's a local variable usage
        }
      }
    }
    return false;
  }
}
