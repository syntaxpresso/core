package io.github.syntaxpresso.core.service.java.language;

import com.google.common.base.Strings;
import io.github.syntaxpresso.core.common.TSFile;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.treesitter.TSNode;

public class ClassFieldDeclarationService {

  /**
   * Retrieves all field declaration nodes within a given class declaration.
   *
   * <p>Usage example:
   *
   * <pre>
   * ClassDeclarationService classService = new ClassDeclarationService();
   * TSNode classNode = classService.findClassByName(tsFile, "MyClass").get();
   * List&lt;TSNode&gt; fields = service.getAllClassFieldDeclarationNodes(tsFile, classNode);
   * for (TSNode field : fields) {
   *   String fieldText = tsFile.getTextFromNode(field);
   *   System.out.println(fieldText); // e.g., "private String name;"
   * }
   * </pre>
   *
   * @param tsFile The {@link TSFile} containing the source code.
   * @param classDeclarationNode The class declaration {@link TSNode} to search within.
   * @return A list of field declaration nodes within the class. Returns an empty list if no fields
   *     are found, the file/tree is null, or the node is not a class declaration.
   */
  public List<TSNode> getAllClassFieldDeclarationNodes(TSFile tsFile, TSNode classDeclarationNode) {
    if (tsFile == null
        || tsFile.getTree() == null
        || !classDeclarationNode.getType().equals("class_declaration")) {
      return Collections.emptyList();
    }
    String queryString = "(class_body (field_declaration) @fieldDeclaration)";
    return tsFile.query(queryString).within(classDeclarationNode).execute().nodes();
  }

  /**
   * Finds a field declaration node within a class by its name.
   *
   * <p>This method searches for a field declaration that matches the specified name within the
   * given class declaration scope. The search is case-sensitive and exact.
   *
   * <p>Usage example:
   *
   * <pre>
   * ClassDeclarationService classService = new ClassDeclarationService();
   * TSNode classNode = classService.findClassByName(tsFile, "MyClass").get();
   * Optional&lt;TSNode&gt; fieldNode = service.findClassFieldNodeByName(tsFile, "myField", classNode);
   * if (fieldNode.isPresent()) {
   *   String fieldText = tsFile.getTextFromNode(fieldNode.get());  // e.g., "private String myField;"
   * }
   * </pre>
   *
   * Query captures: - name: The field name identifier - fieldDeclaration: The entire field
   * declaration node
   *
   * @param tsFile The {@link TSFile} containing the source code.
   * @param fieldDeclaratorName The name of the field to find (case-sensitive).
   * @param classDeclarationNode The class declaration {@link TSNode} to search within.
   * @return An {@link Optional} containing the field declaration node if found, or empty if the
   *     field is not found, the file/tree is null, or the node is not a class declaration.
   */
  public Optional<TSNode> findClassFieldNodeByName(
      TSFile tsFile, String fieldDeclaratorName, TSNode classDeclarationNode) {
    if (tsFile == null
        || tsFile.getTree() == null
        || Strings.isNullOrEmpty(fieldDeclaratorName)
        || !classDeclarationNode.getType().equals("class_declaration")) {
      return Optional.empty();
    }
    String queryString =
        String.format(
            """
            ((field_declaration
              declarator: (variable_declarator
                name: (identifier) @name))
             (#eq? @name "%s")) @fieldDeclaration
            """,
            fieldDeclaratorName);
    return tsFile.query(queryString).within(classDeclarationNode).execute().firstNodeOptional();
  }

  /**
   * Finds all field declaration nodes within a class that match a specific type.
   *
   * <p>This method searches for field declarations that have the specified type within the given
   * class declaration scope. The type matching is case-sensitive and exact. This includes both
   * primitive types and reference types.
   *
   * <p>Usage example:
   *
   * <pre>
   * ClassDeclarationService classService = new ClassDeclarationService();
   * TSNode classNode = classService.findClassByName(tsFile, "MyClass").get();
   * List&lt;TSNode&gt; stringFields = service.findClassFieldNodesByType(tsFile, "String", classNode);
   * for (TSNode field : stringFields) {
   *   String fieldText = tsFile.getTextFromNode(field);  // e.g., "private String name;"
   * }
   * </pre>
   *
   * Query captures: - type: The field type node - fieldDeclaration: The entire field declaration
   * node
   *
   * @param tsFile The {@link TSFile} containing the source code.
   * @param fieldDeclaratorType The type to search for (case-sensitive).
   * @param classDeclarationNode The class declaration {@link TSNode} to search within.
   * @return A list of field declaration nodes that have the specified type. Returns an empty list
   *     if no matching fields are found, the file/tree is null, or the node is not a class
   *     declaration.
   */
  public List<TSNode> findClassFieldNodesByType(
      TSFile tsFile, String fieldDeclaratorType, TSNode classDeclarationNode) {
    if (tsFile == null
        || tsFile.getTree() == null
        || Strings.isNullOrEmpty(fieldDeclaratorType)
        || !classDeclarationNode.getType().equals("class_declaration")) {
      return Collections.emptyList();
    }
    String queryString =
        String.format(
            """
            ((field_declaration
              type: (_) @type)
             (#eq? @type "%s")) @fieldDeclaration
            """,
            fieldDeclaratorType);
    return tsFile.query(queryString).within(classDeclarationNode).execute().nodes();
  }

  /**
   * Extracts detailed information from a field declaration node, including type, name, and optional
   * initialization value.
   *
   * <p>This method parses a field declaration and returns information about each variable
   * declarator within the field. For fields with multiple declarators (e.g., {@code int x, y;}), it
   * returns separate entries for each variable.
   *
   * <p>Usage example:
   *
   * <pre>
   * List&lt;TSNode&gt; fieldNodes = service.getAllClassFieldDeclarationNodes(tsFile, classNode);
   * List&lt;Map&lt;String, TSNode&gt;&gt; fieldInfo = service.getClassFieldNodeInfo(tsFile, fieldNodes.get(0));
   * for (Map&lt;String, TSNode&gt; info : fieldInfo) {
   *   TSNode typeNode = info.get("type");        // The field type node
   *   TSNode nameNode = info.get("name");        // The field name identifier node
   *   TSNode valueNode = info.get("value");      // The initialization value node (if present)
   *
   *   String typeName = tsFile.getTextFromNode(typeNode);  // e.g., "String"
   *   String fieldName = tsFile.getTextFromNode(nameNode); // e.g., "name"
   *   String initValue = valueNode != null ? tsFile.getTextFromNode(valueNode) : null; // e.g., "\"test\""
   * }
   * </pre>
   *
   * Query captures: - type: The field type node - name: The field name identifier node - value: The
   * field initialization value node (optional, may be null)
   *
   * @param tsFile The {@link TSFile} containing the source code.
   * @param fieldDeclarationNode The field declaration {@link TSNode} to analyze.
   * @return A list of maps containing field information for each variable declarator. Each map
   *     contains "type", "name", and optionally "value" keys. Returns an empty list if the file is
   *     null, tree is null, or the node is not a field declaration.
   */
  public List<Map<String, TSNode>> getClassFieldNodeInfo(TSFile tsFile, TSNode fieldDeclarationNode) {
    if (tsFile == null
        || tsFile.getTree() == null
        || !fieldDeclarationNode.getType().equals("field_declaration")) {
      return Collections.emptyList();
    }
    String queryString =
        """
        (field_declaration
          type: (_) @type
          declarator: (variable_declarator
            name: (identifier) @name
            (_)? @value))
        """;
    return tsFile
        .query(queryString)
        .within(fieldDeclarationNode)
        .returningAllCaptures()
        .execute()
        .captures();
  }

  /**
   * Retrieves the type node from a field declaration.
   *
   * <p>This method extracts the first available type node from the field declaration. For fields
   * with multiple declarators, all declarators share the same type, so this returns the type for
   * all of them.
   *
   * <p>Usage example:
   *
   * <pre>
   * List&lt;TSNode&gt; fieldNodes = service.getAllClassFieldDeclarationNodes(tsFile, classNode);
   * Optional&lt;TSNode&gt; typeNode = service.getClassFieldTypeNode(tsFile, fieldNodes.get(0));
   * if (typeNode.isPresent()) {
   *   String typeName = tsFile.getTextFromNode(typeNode.get());  // e.g., "String", "List&lt;Integer&gt;"
   *   String nodeType = typeNode.get().getType();                // e.g., "type_identifier", "generic_type"
   * }
   * </pre>
   *
   * @param tsFile The {@link TSFile} containing the source code.
   * @param fieldDeclarationNode The field declaration {@link TSNode} to analyze.
   * @return An {@link Optional} containing the type node, or empty if no type is found, the file is
   *     null, tree is null, or the node is not a field declaration.
   */
  public Optional<TSNode> getClassFieldTypeNode(TSFile tsFile, TSNode fieldDeclarationNode) {
    if (tsFile == null
        || tsFile.getTree() == null
        || !fieldDeclarationNode.getType().equals("field_declaration")) {
      return Optional.empty();
    }
    List<Map<String, TSNode>> fieldDeclarationInfo =
        this.getClassFieldNodeInfo(tsFile, fieldDeclarationNode);
    for (Map<String, TSNode> map : fieldDeclarationInfo) {
      TSNode typeNode = map.get("type");
      if (typeNode != null) {
        return Optional.of(typeNode);
      }
    }
    return Optional.empty();
  }

  /**
   * Retrieves the first field name node from a field declaration.
   *
   * <p>This method extracts the first available field name identifier from the field declaration.
   * For fields with multiple declarators (e.g., {@code int x, y;}), this returns only the first
   * name node. Use {@link #getClassFieldNodeInfo(TSFile, TSNode)} to get all field names.
   *
   * <p>Usage example:
   *
   * <pre>
   * List&lt;TSNode&gt; fieldNodes = service.getAllClassFieldDeclarationNodes(tsFile, classNode);
   * Optional&lt;TSNode&gt; nameNode = service.getClassFieldNameNode(tsFile, fieldNodes.get(0));
   * if (nameNode.isPresent()) {
   *   String fieldName = tsFile.getTextFromNode(nameNode.get());  // e.g., "name", "count"
   *   String nodeType = nameNode.get().getType();                 // "identifier"
   * }
   * </pre>
   *
   * @param tsFile The {@link TSFile} containing the source code.
   * @param fieldDeclarationNode The field declaration {@link TSNode} to analyze.
   * @return An {@link Optional} containing the first field name identifier node, or empty if no
   *     name is found, the file is null, tree is null, or the node is not a field declaration.
   */
  public Optional<TSNode> getClassFieldNameNode(TSFile tsFile, TSNode fieldDeclarationNode) {
    if (tsFile == null
        || tsFile.getTree() == null
        || !fieldDeclarationNode.getType().equals("field_declaration")) {
      return Optional.empty();
    }
    List<Map<String, TSNode>> fieldDeclarationInfo =
        this.getClassFieldNodeInfo(tsFile, fieldDeclarationNode);
    for (Map<String, TSNode> map : fieldDeclarationInfo) {
      TSNode nameNode = map.get("name");
      if (nameNode != null) {
        return Optional.of(nameNode);
      }
    }
    return Optional.empty();
  }

  /**
   * Retrieves the first initialization value node from a field declaration.
   *
   * <p>This method extracts the first available initialization value from the field declaration.
   * For fields with multiple declarators, this returns only the first value node (if any). Use
   * {@link #getClassFieldNodeInfo(TSFile, TSNode)} to get all field values.
   *
   * <p>Usage example:
   *
   * <pre>
   * List&lt;TSNode&gt; fieldNodes = service.getAllClassFieldDeclarationNodes(tsFile, classNode);
   * Optional&lt;TSNode&gt; valueNode = service.getClassFieldValueNode(tsFile, fieldNodes.get(0));
   * if (valueNode.isPresent()) {
   *   String initValue = tsFile.getTextFromNode(valueNode.get());  // e.g., "\"test\"", "42", "new ArrayList&lt;&gt;()"
   *   String nodeType = valueNode.get().getType();                 // e.g., "string_literal", "decimal_integer_literal"
   * } else {
   *   // Field has no initialization value
   * }
   * </pre>
   *
   * @param tsFile The {@link TSFile} containing the source code.
   * @param fieldDeclarationNode The field declaration {@link TSNode} to analyze.
   * @return An {@link Optional} containing the first initialization value node, or empty if no
   *     value is found, the field is not initialized, the file is null, tree is null, or the node
   *     is not a field declaration.
   */
  public Optional<TSNode> getClassFieldValueNode(TSFile tsFile, TSNode fieldDeclarationNode) {
    if (tsFile == null
        || tsFile.getTree() == null
        || !fieldDeclarationNode.getType().equals("field_declaration")) {
      return Optional.empty();
    }
    List<Map<String, TSNode>> fieldDeclarationInfo =
        this.getClassFieldNodeInfo(tsFile, fieldDeclarationNode);
    for (Map<String, TSNode> map : fieldDeclarationInfo) {
      TSNode valueNode = map.get("value");
      if (valueNode != null) {
        return Optional.of(valueNode);
      }
    }
    return Optional.empty();
  }

  /**
   * Finds all usages of a field within a specified class declaration scope.
   *
   * <p>This method searches for all field access patterns (e.g., {@code this.fieldName}, {@code
   * object.fieldName}) that match the name of the specified field within the given class
   * declaration scope.
   *
   * <p><strong>Important Limitation:</strong> This method finds usages based on field name matching
   * within the class scope. If there are multiple fields with the same name in nested classes, it
   * will find usages of all fields with that name, not just the specific field declaration
   * provided.
   *
   * <p>Usage example:
   *
   * <pre>
   * // Find all usages of a specific field
   * List&lt;TSNode&gt; fieldNodes = service.getAllClassFieldDeclarationNodes(tsFile, classNode);
   * List&lt;TSNode&gt; usages = service.getAllClassFieldUsageNodes(tsFile, fieldNodes.get(0), classNode);
   * for (TSNode usage : usages) {
   *   String usageText = tsFile.getTextFromNode(usage);  // e.g., "fieldName"
   *   int line = usage.getStartPoint().getRow() + 1;     // Line number of usage
   *   System.out.println("Usage: " + usageText + " at line " + line);
   * }
   * </pre>
   *
   * Query captures: - usage: The field name identifier within field access expressions
   *
   * @param tsFile The {@link TSFile} containing the source code.
   * @param fieldDeclarationNode The field declaration {@link TSNode} to find usages for.
   * @param classDeclarationNode The class declaration {@link TSNode} to search within.
   * @return A list of identifier nodes representing field usages. Returns an empty list if no
   *     usages are found, the file/tree is null, or the nodes are not valid declarations.
   */
  public List<TSNode> getAllClassFieldUsageNodes(
      TSFile tsFile, TSNode fieldDeclarationNode, TSNode classDeclarationNode) {
    if (tsFile == null
        || tsFile.getTree() == null
        || !fieldDeclarationNode.getType().equals("field_declaration")
        || !classDeclarationNode.getType().equals("class_declaration")) {
      return Collections.emptyList();
    }
    Optional<TSNode> fieldNameNode = this.getClassFieldNameNode(tsFile, fieldDeclarationNode);
    if (fieldNameNode.isEmpty()) {
      return Collections.emptyList();
    }
    String fieldName = tsFile.getTextFromNode(fieldNameNode.get());
    String queryString =
        String.format(
            """
            ((field_access
              field: (identifier) @usage)
             (#eq? @usage "%s"))
            """,
            fieldName);
    return tsFile.query(queryString).within(classDeclarationNode).execute().nodes();
  }
}
