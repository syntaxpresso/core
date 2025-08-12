package io.github.syntaxpresso.core.service.java.extra;

import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.util.StringHelper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.treesitter.TSNode;
import org.treesitter.TSQuery;
import org.treesitter.TSQueryCapture;
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
   * Renames the usage of a method parameter.
   *
   * @param file The file containing the source code.
   * @param methodDeclarationNode The node of the method declaration.
   * @param currentName The current name of the parameter.
   * @param newName The new name of the parameter.
   */
  private void renameMethodParamUsage(
      TSFile file, TSNode methodDeclarationNode, String currentName, String newName) {
    if (methodDeclarationNode == null
        || !"method_declaration".equals(methodDeclarationNode.getType())) {
      return;
    }
    TSNode paramDeclarationIdentifier = null;
    List<TSNode> formalParameters = this.findAllFormalParameters(methodDeclarationNode, file);
    for (TSNode param : formalParameters) {
      Optional<TSNode> paramNameNode = this.getFormalParamNameIdentifierNode(param);
      if (paramNameNode.isPresent()
          && file.getTextFromNode(paramNameNode.get()).equals(currentName)) {
        paramDeclarationIdentifier = paramNameNode.get();
        break;
      }
    }
    TSNode bodyNode = methodDeclarationNode.getChildByFieldName("body");
    if (bodyNode == null) {
      return;
    }
    String queryStr = "(identifier) @id";
    TSQuery query = new TSQuery(file.getParser().getLanguage(), queryStr);
    TSQueryCursor cursor = new TSQueryCursor();
    cursor.exec(query, bodyNode);
    TSQueryMatch match = new TSQueryMatch();
    List<TSNode> nodesToRename = new ArrayList<>();
    while (cursor.nextMatch(match)) {
      for (TSQueryCapture capture : match.getCaptures()) {
        TSNode idNode = capture.getNode();
        if (file.getTextFromNode(idNode).equals(StringHelper.pascalToCamel(currentName))) {
          if (idNode.equals(paramDeclarationIdentifier)) {
            continue;
          }
          TSNode parent = idNode.getParent();
          if (parent != null && "field_access".equals(parent.getType())) {
            TSNode objectNode = parent.getChildByFieldName("object");
            if (objectNode != null && "this".equals(file.getTextFromNode(objectNode))) {
              continue;
            }
          }
          nodesToRename.add(idNode);
        }
      }
    }
    nodesToRename.sort(Comparator.comparingInt(TSNode::getStartByte).reversed());
    for (TSNode node : nodesToRename) {
      if (node.getParent().getType().equals("assignment_expression")
          || node.getParent().getType().equals("expression_statement")) {
        file.updateSourceCode(node, StringHelper.pascalToCamel(newName));
      }
    }
  }

  /**
   * Renames a method parameter and all its usages within the method body.
   *
   * @param file The file containing the source code.
   * @param formalParameterNode The TSNode of the formal parameter.
   * @param currentName The current name of the parameter (PascalCase).
   * @param newName The new name for the parameter (PascalCase).
   * @return null
   */
  public Void renameMethodParam(
      TSFile file, TSNode formalParameterNode, String currentName, String newName) {
    Optional<TSNode> methodDeclarationNode =
        file.findParentNodeByType(formalParameterNode, "method_declaration");
    if (methodDeclarationNode.isEmpty()) {
      return null;
    }
    Optional<TSNode> paramNameNode = this.getFormalParamNameIdentifierNode(formalParameterNode);
    Optional<TSNode> paramTypeNode = this.getFormalParamTypeIdentifierNode(formalParameterNode);
    if (paramNameNode.isEmpty() || paramTypeNode.isEmpty()) {
      return null;
    }
    String paramName = file.getTextFromNode(paramNameNode.get());
    String paramType = file.getTextFromNode(paramTypeNode.get());
    if (!paramType.equals(currentName)) {
      return null;
    }
    if (paramType.toLowerCase().equals(paramName.toLowerCase())) {
      // Only rename usages if param variable name will be changed.
      this.renameMethodParamUsage(file, methodDeclarationNode.get(), currentName, newName);
      file.updateSourceCode(paramNameNode.get(), StringHelper.pascalToCamel(newName));
    }
    file.updateSourceCode(paramTypeNode.get(), newName);
    return null;
  }
}
