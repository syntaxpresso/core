package io.github.syntaxpresso.core.service.java.extra;

import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.util.StringHelper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.treesitter.TSNode;
import org.treesitter.TSQuery;
import org.treesitter.TSQueryCursor;
import org.treesitter.TSQueryMatch;

public class MethodDeclarationService {

  private static final String FORMAL_PARAMETERS_QUERY = "(formal_parameters) @params";
  private static final String LOCAL_VARIABLE_DECLARATION_QUERY =
      "(local_variable_declaration) @local_variable";

  /**
   * Finds all formal parameters of a method.
   *
   * @param methodNode The TSNode of the method declaration.
   * @param tsFile The TSFile containing the source code.
   * @return A list of all formal parameter nodes.
   */
  public List<TSNode> findAllFormalParameters(TSNode methodNode, TSFile tsFile) {
    if (methodNode == null || !"method_declaration".equals(methodNode.getType())) {
      return Collections.emptyList();
    }
    TSQuery query = new TSQuery(tsFile.getParser().getLanguage(), FORMAL_PARAMETERS_QUERY);
    TSQueryCursor cursor = new TSQueryCursor();
    cursor.exec(query, methodNode);
    TSQueryMatch match = new TSQueryMatch();
    if (cursor.nextMatch(match)) {
      return IntStream.range(0, match.getCaptures()[0].getNode().getNamedChildCount())
          .mapToObj(i -> match.getCaptures()[0].getNode().getNamedChild(i))
          .filter(node -> "formal_parameter".equals(node.getType()))
          .collect(Collectors.toList());
    }
    return Collections.emptyList();
  }

  /**
   * Gets the type identifier node of a formal parameter.
   *
   * @param formalParameterNode The TSNode of the formal parameter.
   * @return An Optional containing the type identifier node, or empty if not found.
   */
  public Optional<TSNode> getFormalParamTypeIdentifierNode(TSNode formalParameterNode) {
    if (formalParameterNode == null || !"formal_parameter".equals(formalParameterNode.getType())) {
      return Optional.empty();
    }
    return Optional.ofNullable(formalParameterNode.getChildByFieldName("type"));
  }

  /**
   * Gets the name identifier node of a formal parameter.
   *
   * @param formalParameterNode The TSNode of the formal parameter.
   * @return An Optional containing the name identifier node, or empty if not found.
   */
  public Optional<TSNode> getFormalParamNameIdentifierNode(TSNode formalParameterNode) {
    if (formalParameterNode == null || !"formal_parameter".equals(formalParameterNode.getType())) {
      return Optional.empty();
    }
    return Optional.ofNullable(formalParameterNode.getChildByFieldName("name"));
  }

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
   * Checks if an identifier node represents a parameter usage (not a local variable or field).
   *
   * @param identifierNode The identifier node to check.
   * @param methodDeclarationNode The method containing this identifier.
   * @param paramName The parameter name we're looking for.
   * @param file The TSFile for text extraction.
   * @return True if this is a parameter usage, false otherwise.
   */
  private boolean isParameterUsage(
      TSNode identifierNode, TSNode methodDeclarationNode, String paramName, TSFile file) {
    if (identifierNode == null || identifierNode.isNull()) {
      return false;
    }
    TSNode parent = identifierNode.getParent();
    // Exclude field access with 'this'
    if (parent != null && !parent.isNull() && "field_access".equals(parent.getType())) {
      TSNode objectNode = parent.getChildByFieldName("object");
      if (objectNode != null
          && !objectNode.isNull()
          && "this".equals(file.getTextFromNode(objectNode))) {
        return false; // this.paramName is a field access, not parameter
      }
    }
    // Exclude local variable declarations
    if (isLocalVariableDeclaration(identifierNode)) {
      return false;
    }
    // Exclude if it's a local variable usage
    if (isLocalVariableUsage(identifierNode, methodDeclarationNode, paramName, file)) {
      return false;
    }
    return true; // It's a parameter usage
  }

  /**
   * Checks if an identifier is part of a local variable declaration.
   *
   * @param identifierNode The identifier node to check.
   * @return True if this identifier is declaring a local variable.
   */
  private boolean isLocalVariableDeclaration(TSNode identifierNode) {
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
  private boolean isLocalVariableUsage(
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

  /**
   * Checks if a node is within the bounds of another node.
   *
   * @param node The node to check.
   * @param container The container node.
   * @return True if node is within container's bounds.
   */
  private boolean isNodeWithin(TSNode node, TSNode container) {
    if (node == null || node.isNull() || container == null || container.isNull()) {
      return false;
    }
    return node.getStartByte() >= container.getStartByte()
        && node.getEndByte() <= container.getEndByte();
  }

  /**
   * Finds all parameter usages within a method body. This ONLY searches within the method body
   * block, not the entire method declaration.
   *
   * @param methodDeclarationNode The method declaration node.
   * @param paramName The parameter name to find.
   * @param file The TSFile for queries and text extraction.
   * @return List of identifier nodes that represent parameter usages.
   */
  private List<TSNode> findParameterUsagesInMethodBody(
      TSNode methodDeclarationNode, String paramName, TSFile file) {
    if (methodDeclarationNode == null || methodDeclarationNode.isNull()) {
      return Collections.emptyList();
    }
    // Get the method body (block node)
    TSNode bodyNode = methodDeclarationNode.getChildByFieldName("body");
    if (bodyNode == null || bodyNode.isNull() || !"block".equals(bodyNode.getType())) {
      return Collections.emptyList();
    }
    List<TSNode> identifierNodes = file.query(bodyNode, "(identifier) @id");
    List<TSNode> usages = new ArrayList<>();
    for (TSNode idNode : identifierNodes) {
      if (idNode == null || idNode.isNull()) {
        continue;
      }
      // Ensure the node is actually within the method body
      if (!isNodeWithin(idNode, bodyNode)) {
        continue;
      }
      // Check if this identifier matches our parameter name
      String idText = file.getTextFromNode(idNode);
      if (!idText.equals(paramName)) {
        continue;
      }
      // Verify this is actually a parameter usage, not a field or local variable
      if (isParameterUsage(idNode, methodDeclarationNode, paramName, file)) {
        usages.add(idNode);
      }
    }
    return usages;
  }

  /**
   * Renames a method parameter and all its usages within the method body. This method ONLY renames:
   * 1. The parameter type in the method signature 2. The parameter name in the method signature (if
   * it matches naming convention or force is true) 3. Parameter usages within the method body It
   * does NOT rename field declarations or field accesses.
   *
   * @param file The file containing the source code.
   * @param formalParameterNode The TSNode of the formal parameter.
   * @param currentName The current name of the parameter type (PascalCase).
   * @param newName The new name for the parameter type (PascalCase).
   * @param force If true, renames variable even if it doesn't follow type naming convention.
   * @return True if any renaming occurred, false otherwise.
   */
  public boolean renameMethodParam(
      TSFile file, TSNode formalParameterNode, String currentName, String newName, boolean force) {
    // 1. Validation
    if (formalParameterNode == null
        || formalParameterNode.isNull()
        || !"formal_parameter".equals(formalParameterNode.getType())) {
      return false;
    }
    Optional<TSNode> methodDeclarationNode =
        file.findParentNodeByType(formalParameterNode, "method_declaration");
    if (methodDeclarationNode.isEmpty()) {
      return false;
    }
    Optional<TSNode> paramNameNode = getFormalParamNameIdentifierNode(formalParameterNode);
    Optional<TSNode> paramTypeNode = getFormalParamTypeIdentifierNode(formalParameterNode);
    if (paramNameNode.isEmpty() || paramTypeNode.isEmpty()) {
      return false;
    }
    // 2. Get current values
    String paramName = file.getTextFromNode(paramNameNode.get());
    String paramType = file.getTextFromNode(paramTypeNode.get());
    // Check if the parameter type matches what we're trying to rename
    if (!paramType.equals(currentName)) {
      return false;
    }
    // 3. Determine if we should rename the parameter variable
    String expectedVariableName = StringHelper.pascalToCamel(currentName);
    boolean shouldRenameVariable = paramName.equals(expectedVariableName) || force;
    // 4. Collect nodes to rename
    List<TSNode> nodesToRename = new ArrayList<>();
    // Always rename the parameter type
    nodesToRename.add(paramTypeNode.get());
    // Only rename parameter name and usages if appropriate
    if (shouldRenameVariable) {
      // Add the parameter name node
      nodesToRename.add(paramNameNode.get());
      // Find and add parameter usages in the method body
      List<TSNode> usages =
          findParameterUsagesInMethodBody(methodDeclarationNode.get(), paramName, file);
      nodesToRename.addAll(usages);
    }
    // 5. Sort nodes in reverse order by start byte to avoid position shifts
    nodesToRename.sort(Comparator.comparingInt(TSNode::getStartByte).reversed());
    // 6. Remove any duplicates (in case the same node was added twice)
    List<TSNode> uniqueNodes = new ArrayList<>();
    Set<TSNode> seen = new HashSet<>();
    for (TSNode node : nodesToRename) {
      if (seen.add(node)) {
        uniqueNodes.add(node);
      }
    }
    // 7. Perform the renaming
    String newVariableName = StringHelper.pascalToCamel(newName);
    for (TSNode node : uniqueNodes) {
      if (node.equals(paramTypeNode.get())) {
        // Type node gets PascalCase
        file.updateSourceCode(node, newName);
      } else {
        // Variable name and usages get camelCase
        file.updateSourceCode(node, newVariableName);
      }
    }
    return !uniqueNodes.isEmpty();
  }

  /**
   * Renames a method parameter and all its usages within the method body. Uses default behavior
   * (only renames variable if it follows type naming convention).
   *
   * @param file The file containing the source code.
   * @param formalParameterNode The TSNode of the formal parameter.
   * @param currentName The current name of the parameter (PascalCase).
   * @param newName The new name for the parameter (PascalCase).
   * @return True if any renaming occurred, false otherwise.
   */
  public boolean renameMethodParam(
      TSFile file, TSNode formalParameterNode, String currentName, String newName) {
    return renameMethodParam(file, formalParameterNode, currentName, newName, false);
  }
}
