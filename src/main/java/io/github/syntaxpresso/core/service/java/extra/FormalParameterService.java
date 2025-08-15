package io.github.syntaxpresso.core.service.java.extra;

import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.util.StringHelper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.treesitter.TSNode;

@RequiredArgsConstructor
public class FormalParameterService {

  private final LocalVariableDeclarationService localVariableDeclarationService;

  private static final String FORMAL_PARAMETER_QUERY = "(formal_parameter) @param";

  /**
   * Finds all formal parameters of a method.
   *
   * @param methodNode The TSNode of the method declaration.
   * @param tsFile The TSFile containing the source code.
   * @return A list of all formal parameter nodes.
   */
  public List<TSNode> findAllFormalParameters(TSFile file, TSNode methodNode, String typeName) {
    if (methodNode == null || !"method_declaration".equals(methodNode.getType())) {
      return Collections.emptyList();
    }
    List<TSNode> foundParams = new ArrayList<>();
    List<TSNode> allParams = file.query(methodNode, FORMAL_PARAMETER_QUERY);
    for (TSNode param : allParams) {
      List<TSNode> typeNodes = file.query(param, "(type_identifier) @type");
      for (TSNode typeNode : typeNodes) {
        String typeNodeName = file.getTextFromNode(typeNode);
        if (typeName.equals(typeNodeName)) {
          foundParams.add(param);
        }
      }
    }
    return foundParams;
  }

  /**
   * Finds the type node within a formal parameter that matches a given name.
   *
   * @param formalParameterNode The TSNode for the formal_parameter.
   * @param file The TSFile containing the source code.
   * @param typeName The name of the type to find.
   * @return An Optional containing the found TSNode, or empty.
   */
  public Optional<TSNode> getParameterTypeNode(
      TSNode formalParameterNode, TSFile file, String typeName) {
    if (formalParameterNode == null || !"formal_parameter".equals(formalParameterNode.getType())) {
      return Optional.empty();
    }
    List<TSNode> typeNodes = file.query(formalParameterNode, "(type_identifier) @type");
    for (TSNode typeNode : typeNodes) {
      String typeNodeName = file.getTextFromNode(typeNode);
      if (typeName.equals(typeNodeName)) {
        return Optional.of(typeNode);
      }
    }
    return Optional.empty();
  }

  /**
   * Extracts the parameter name from a formal_parameter node.
   *
   * @param formalParameterNode The TSNode representing the formal_parameter.
   * @param file The TSFile containing the source code.
   * @return An Optional containing the parameter name node, or empty if not found.
   */
  public Optional<TSNode> getParameterNameNode(TSNode formalParameterNode, TSFile file) {
    if (formalParameterNode == null || !"formal_parameter".equals(formalParameterNode.getType())) {
      return Optional.empty();
    }
    return Optional.ofNullable(formalParameterNode.getChildByFieldName("name"));
  }

  /**
   * Checks if an identifier is part of a formal parameter declaration.
   *
   * @param identifierNode The identifier node to check.
   * @return True if this identifier is declaring a formal parameter.
   */
  public boolean isFormalParameterDeclaration(TSNode identifierNode) {
    if (identifierNode == null || identifierNode.isNull()) {
      return false;
    }
    TSNode parent = identifierNode.getParent();
    return parent != null && !parent.isNull() && "formal_parameter".equals(parent.getType());
  }

  /**
   * Checks if an identifier represents usage of a formal parameter.
   *
   * @param identifierNode The identifier node to check.
   * @param methodDeclarationNode The method declaration node.
   * @param paramName The parameter name to check.
   * @param file The TSFile for text extraction.
   * @return True if this is a formal parameter usage.
   */
  public boolean isFormalParameterUsage(
      TSFile file,
      TSNode methodDeclarationNode,
      TSNode identifierNode,
      String paramName,
      String currentName) {
    if (identifierNode == null
        || identifierNode.isNull()
        || methodDeclarationNode == null
        || methodDeclarationNode.isNull()) {
      return false;
    }

    // Check if it's a field access (this.paramName)
    TSNode parent = identifierNode.getParent();
    if (parent != null && "field_access".equals(parent.getType())) {
      TSNode objectNode = parent.getChildByFieldName("object");
      if (objectNode != null && "this".equals(file.getTextFromNode(objectNode))) {
        return false; // this.paramName is a field access, not parameter usage
      }
    }

    // Exclude if it's a local variable declaration or usage
    if (localVariableDeclarationService.isLocalVariableDeclaration(identifierNode)) {
      return false;
    }
    if (localVariableDeclarationService.isLocalVariableUsage(
        identifierNode, methodDeclarationNode, paramName, file)) {
      return false;
    }

    // Check if it matches any formal parameter name
    List<TSNode> formalParams = findAllFormalParameters(file, methodDeclarationNode, currentName);
    for (TSNode param : formalParams) {
      Optional<TSNode> nameNode = getParameterNameNode(param, file);
      if (nameNode.isPresent()
          && paramName.equals(file.getTextFromNode(nameNode.get()))
          && identifierNode.getStartByte() >= param.getEndByte()) {
        return true; // It's after the parameter declaration, so it's a usage
      }
    }

    return false;
  }

  /**
   * Finds all formal parameter usages by currentName within a method body.
   *
   * @param file The TSFile containing the source code.
   * @param methodDeclarationNode The TSNode of the method declaration.
   * @param currentName The current name of the parameter type to find usages for.
   * @return A list of TSNode identifiers representing formal parameter usages.
   */
  public List<TSNode> findAllFormalParameterUsages(
      TSFile file, TSNode methodDeclarationNode, String currentName) {
    if (methodDeclarationNode == null
        || !"method_declaration".equals(methodDeclarationNode.getType())) {
      return Collections.emptyList();
    }
    List<TSNode> parameterUsages = new ArrayList<>();
    // Get all formal parameters with the specified type
    List<TSNode> formalParameters =
        findAllFormalParameters(file, methodDeclarationNode, currentName);
    // For each formal parameter, find its usages in the method body
    for (TSNode formalParameter : formalParameters) {
      Optional<TSNode> parameterNameNode = getParameterNameNode(formalParameter, file);
      if (parameterNameNode.isEmpty()) {
        continue;
      }
      String parameterName = file.getTextFromNode(parameterNameNode.get());

      // Find usages in method body
      TSNode bodyNode = methodDeclarationNode.getChildByFieldName("body");
      if (bodyNode != null && "block".equals(bodyNode.getType())) {
        List<TSNode> identifiers = file.query(bodyNode, "(identifier) @id");
        for (TSNode identifier : identifiers) {
          if (parameterName.equals(file.getTextFromNode(identifier))
              && isFormalParameterUsage(
                  file, methodDeclarationNode, identifier, parameterName, currentName)) {
            parameterUsages.add(identifier);
          }
        }
      }
    }
    return parameterUsages.stream()
        .sorted(Comparator.comparingInt(TSNode::getStartByte))
        .collect(Collectors.toList());
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
   * Renames formal parameters, including their types, names, and usages.
   *
   * @param file The file containing the source code.
   * @param methodDeclarationNode The TSNode of the method declaration.
   * @param currentName The current name of the parameter type (PascalCase).
   * @param newName The new name for the parameter type (PascalCase).
   */
  public void renameFormalParameters(
      TSFile file, TSNode methodDeclarationNode, String currentName, String newName) {
    List<TSNode> formalParameterNodes =
        this.findAllFormalParameters(file, methodDeclarationNode, currentName);
    for (TSNode formalParameterNode : formalParameterNodes.reversed()) {
      Optional<TSNode> parameterTypeNode =
          this.getParameterTypeNode(formalParameterNode, file, currentName);
      if (parameterTypeNode.isEmpty()) {
        continue;
      }
      Optional<TSNode> parameterNameNode = this.getParameterNameNode(formalParameterNode, file);
      if (parameterNameNode.isPresent()) {
        boolean isCollectionType = this.isCollectionType(file.getTextFromNode(formalParameterNode));
        String currentParameterName = file.getTextFromNode(parameterNameNode.get());
        List<TSNode> paramUsages =
            this.findAllFormalParameterUsages(file, methodDeclarationNode, currentName);
        for (TSNode usage : paramUsages.reversed()) {
          String usageText = file.getTextFromNode(usage);
          if (isCollectionType) {
            String pluralizedCurrentName = StringHelper.pluralizeCamelCase(currentName);
            if (usageText.equals(StringHelper.pascalToCamel(pluralizedCurrentName))) {
              String pluralizedNewName = StringHelper.pluralizeCamelCase(newName);
              file.updateSourceCode(usage, StringHelper.pascalToCamel(pluralizedNewName));
            }
          }
          if (usageText.equals(StringHelper.pascalToCamel(currentName))) {
            file.updateSourceCode(usage, StringHelper.pascalToCamel(newName));
          }
        }
        if (isCollectionType) {
          String pluralizedCurrentName = StringHelper.pluralizeCamelCase(currentName);
          if (currentParameterName.equals(StringHelper.pascalToCamel(pluralizedCurrentName))) {
            String pluralizedNewName = StringHelper.pluralizeCamelCase(newName);
            file.updateSourceCode(
                parameterNameNode.get(), StringHelper.pascalToCamel(pluralizedNewName));
          }
        }
        if (currentParameterName.equals(StringHelper.pascalToCamel(currentName))) {
          file.updateSourceCode(parameterNameNode.get(), StringHelper.pascalToCamel(newName));
        }
      }
      file.updateSourceCode(parameterTypeNode.get(), newName);
    }
  }
}
