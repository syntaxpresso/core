package io.github.syntaxpresso.core.service.java.extra;

import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.util.StringHelper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.treesitter.TSNode;

public class FieldDeclarationService {

  private static final String FIELD_DECLARATION_QUERY = "(field_declaration) @field";

  /**
   * Finds all field declarations with a specific type name.
   *
   * @param file The TSFile containing the source code.
   * @param typeName The type name to search for.
   * @return A list of field declaration nodes that have the specified type.
   */
  public List<TSNode> findAllFieldDeclarations(TSFile file, String typeName) {
    if (file == null || typeName == null) {
      return Collections.emptyList();
    }
    List<TSNode> foundFields = new ArrayList<>();
    List<TSNode> allFields = file.query(FIELD_DECLARATION_QUERY);
    for (TSNode field : allFields) {
      List<TSNode> typeNodes = file.query(field, "(type_identifier) @type");
      for (TSNode typeNode : typeNodes) {
        String typeNodeName = file.getTextFromNode(typeNode);
        if (typeName.equals(typeNodeName)) {
          foundFields.add(field);
          break; // Found a match for this field, no need to check other type nodes
        }
      }
    }
    return foundFields;
  }

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
    // Query for type identifiers within this field declaration
    List<TSNode> typeNodes = file.query(declarationNode, "(type_identifier) @type");
    for (TSNode typeNode : typeNodes) {
      String foundTypeName = file.getTextFromNode(typeNode);
      if (typeName.equals(foundTypeName)) {
        return Optional.of(typeNode);
      }
    }
    return Optional.empty();
  }

  /**
   * Extracts the variable name from a field_declaration node.
   *
   * @param declarationNode The TSNode representing the field_declaration.
   * @param file The TSFile containing the source code.
   * @return An Optional containing the variable name node, or empty if not found.
   */
  public Optional<TSNode> getFieldNameNode(TSNode declarationNode, TSFile file) {
    if (declarationNode == null || !"field_declaration".equals(declarationNode.getType())) {
      return Optional.empty();
    }
    // Query for the field name identifier within the declarator
    List<TSNode> nameNodes =
        file.query(declarationNode, "(variable_declarator name: (identifier) @name)");
    return nameNodes.isEmpty() ? Optional.empty() : Optional.of(nameNodes.get(0));
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
    // Query for type identifiers in object creation expressions within this field declaration
    List<TSNode> typeNodes =
        file.query(declarationNode, "(object_creation_expression type: (type_identifier) @type)");
    for (TSNode typeNode : typeNodes) {
      String foundClassName = file.getTextFromNode(typeNode);
      if (className.equals(foundClassName)) {
        return Optional.of(typeNode);
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
    // Query for different patterns of field usage
    String[] queries = {
      "(assignment_expression left: (identifier) @usage)",
      "(assignment_expression left: (field_access field: (identifier) @usage))",
      "(expression_statement (identifier) @usage)"
    };
    for (String queryString : queries) {
      List<TSNode> nodes = file.query(queryString);
      for (TSNode node : nodes) {
        String nodeText = file.getTextFromNode(node);
        if (fieldName.equals(nodeText)) {
          usages.add(node);
        }
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
  public void renameClassField(TSFile file, String currentName, String newName) {
    String camelCaseCurrentName = StringHelper.pascalToCamel(currentName);
    String camelCaseNewName = StringHelper.pascalToCamel(newName);
    List<TSNode> allFieldUsages = this.findFieldUsages(file, camelCaseCurrentName);
    for (TSNode usage : allFieldUsages.reversed()) {
      file.updateSourceCode(usage, camelCaseNewName);
    }
  }

  /**
   * Renames all field declarations and their usages for a specific type.
   *
   * @param file The file containing the source code.
   * @param currentName The current type name (PascalCase).
   * @param newName The new type name (PascalCase).
   */
  public void renameClassFields(TSFile file, String currentName, String newName) {
    // Find all field declarations with the specified type
    List<TSNode> fieldDeclarations = findAllFieldDeclarations(file, currentName);
    // Rename field declarations in reverse order to preserve byte positions
    for (TSNode fieldDeclaration : fieldDeclarations.reversed()) {
      renameFieldDeclaration(file, fieldDeclaration, currentName, newName);
    }
    // Rename field usages
    renameClassField(file, currentName, newName);
  }
}
