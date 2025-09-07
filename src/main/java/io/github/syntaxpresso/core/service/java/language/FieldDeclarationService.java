package io.github.syntaxpresso.core.service.java.language;

import com.google.common.base.Strings;
import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.service.java.language.extra.FieldCapture;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.treesitter.TSNode;

/**
 * Service for analyzing and manipulating field declarations in Java source code using tree-sitter.
 *
 * <p>This service provides comprehensive functionality for working with field declarations within
 * Java classes, including extraction of field information, finding field usages, and performing
 * field-related transformations. It leverages tree-sitter queries to accurately parse and analyze
 * field declarations at the AST level.
 *
 * <p>Key capabilities include:
 *
 * <ul>
 *   <li>Extracting field type, name, and initialization values
 *   <li>Finding all field declarations within a class
 *   <li>Locating field annotations
 *   <li>Searching for field usages across the class
 *   <li>Finding fields by name or type
 *   <li>Renaming field declarations and their usages
 * </ul>
 *
 * <p>Usage example:
 *
 * <pre>
 * FieldDeclarationService fieldService = new FieldDeclarationService();
 * ClassDeclarationService classService = new ClassDeclarationService();
 *
 * // Find a class and get all its fields
 * TSNode classNode = classService.findClassByName(tsFile, "User").get();
 * List&lt;TSNode&gt; fieldNodes = fieldService.getAllFieldDeclarationNodes(tsFile, classNode);
 *
 * // Analyze each field
 * for (TSNode fieldNode : fieldNodes) {
 *   List&lt;Map&lt;String, TSNode&gt;&gt; fieldInfo = fieldService.getFieldDeclarationNodeInfo(tsFile, fieldNode);
 *   for (Map&lt;String, TSNode&gt; info : fieldInfo) {
 *     String type = tsFile.getTextFromNode(info.get("fieldType"));
 *     String name = tsFile.getTextFromNode(info.get("fieldName"));
 *     System.out.println("Field: " + type + " " + name);
 *   }
 * }
 * </pre>
 *
 * @see TSFile
 * @see FieldCapture
 */
@Getter
@RequiredArgsConstructor
public class FieldDeclarationService {

  /**
   * Extracts field declaration information from a field node using tree-sitter queries.
   *
   * <p>This method analyzes a field declaration node to extract information about its type, name,
   * and optional initialization value using tree-sitter queries.
   *
   * <p>Usage example:
   *
   * <pre>
   * List&lt;TSNode&gt; fieldNodes = service.getAllFieldDeclarationNodes(tsFile, classNode);
   * List&lt;Map&lt;String, TSNode&gt;&gt; fieldInfo = service.getFieldDeclarationNodeInfo(tsFile, fieldNodes.get(0));
   * for (Map&lt;String, TSNode&gt; info : fieldInfo) {
   *   String fieldType = tsFile.getTextFromNode(info.get("fieldType"));  // e.g., "String"
   *   String fieldName = tsFile.getTextFromNode(info.get("fieldName"));  // e.g., "name"
   * }
   * </pre>
   *
   * Query captures: - fieldType: The type node of the field - fieldName: The name identifier of the
   * field - fieldValue: The initialization value if present - field: The entire field declaration
   *
   * @param tsFile The {@link TSFile} containing the source code
   * @param fieldDeclarationNode The field declaration {@link TSNode} to analyze
   * @return A list of maps containing the captured nodes, or an empty list if invalid input
   */
  public List<Map<String, TSNode>> getFieldDeclarationNodeInfo(
      TSFile tsFile, TSNode fieldDeclarationNode) {
    if (tsFile == null
        || tsFile.getTree() == null
        || !fieldDeclarationNode.getType().equals("field_declaration")) {
      return Collections.emptyList();
    }
    String queryString =
        String.format(
            """
            (field_declaration
              type: [
                (type_identifier) %s
                (generic_type
                  (type_identifier) %s
                  (type_arguments
                    [
                      (type_identifier) %s
                      (generic_type) %s
                    ]
                  )
                )
              ]
              declarator: (variable_declarator
                name: (identifier) %s
                (_)? %s)) %s
            """,
            FieldCapture.FIELD_TYPE.getCaptureWithAt(),
            FieldCapture.FIELD_TYPE.getCaptureWithAt(),
            FieldCapture.FIELD_TYPE_ARGUMENT.getCaptureWithAt(),
            FieldCapture.FIELD_TYPE_ARGUMENT.getCaptureWithAt(),
            FieldCapture.FIELD_NAME.getCaptureWithAt(),
            FieldCapture.FIELD_VALUE.getCaptureWithAt(),
            FieldCapture.FIELD.getCaptureWithAt());
    return tsFile
        .query(queryString)
        .within(fieldDeclarationNode)
        .returningAllCaptures()
        .execute()
        .captures();
  }

  protected Optional<TSNode> getFieldDeclarationNodeByCaptureName(
      TSFile tsFile, TSNode fieldDeclarationNode, FieldCapture capture) {
    if (tsFile == null
        || tsFile.getTree() == null
        || !fieldDeclarationNode.getType().equals("field_declaration")) {
      return Optional.empty();
    }
    List<Map<String, TSNode>> fieldInfo =
        this.getFieldDeclarationNodeInfo(tsFile, fieldDeclarationNode);
    for (Map<String, TSNode> map : fieldInfo) {
      TSNode node = map.get(capture.getCaptureName());
      if (node != null) {
        return Optional.of(node);
      }
    }
    return Optional.empty();
  }

  protected List<TSNode> getFieldDeclarationNodesFromCaptureName(
      TSFile tsFile, TSNode fieldDeclarationNode, FieldCapture capture) {
    List<Map<String, TSNode>> fieldInfo =
        this.getFieldDeclarationNodeInfo(tsFile, fieldDeclarationNode);
    List<TSNode> foundNodes = new ArrayList<>();
    for (Map<String, TSNode> map : fieldInfo) {
      TSNode node = map.get(capture.getCaptureName());
      if (node != null) {
        foundNodes.add(node);
      }
    }
    return foundNodes;
  }

  /**
   * Retrieves all field declaration nodes within a class declaration.
   *
   * <p>This method finds all field declarations within the specified class scope, regardless of
   * their visibility modifiers or types.
   *
   * <p>Usage example:
   *
   * <pre>
   * ClassDeclarationService classService = new ClassDeclarationService();
   * TSNode classNode = classService.findClassByName(tsFile, "MyClass").get();
   * List&lt;TSNode&gt; fieldNodes = service.getAllFieldDeclarationNodes(tsFile, classNode);
   * for (TSNode field : fieldNodes) {
   *   String fieldText = tsFile.getTextFromNode(field);  // e.g., "private String name;"
   * }
   * </pre>
   *
   * @param tsFile The {@link TSFile} containing the source code
   * @param classDeclarationNode The class declaration {@link TSNode} to search within
   * @return A list of all field declaration nodes in the class
   */
  public List<TSNode> getAllFieldDeclarationNodes(TSFile tsFile, TSNode classDeclarationNode) {
    if (tsFile == null
        || tsFile.getTree() == null
        || !classDeclarationNode.getType().equals("class_declaration")) {
      return Collections.emptyList();
    }
    String queryString =
        """
          (class_declaration
            body: (class_body
              (field_declaration) @fieldDeclaration
            )
          )
        """;
    return tsFile.query(queryString).within(classDeclarationNode).execute().nodes();
  }

  /**
   * Retrieves all field annotation nodes within a class declaration.
   *
   * <p>This method finds all annotations attached to field declarations within the specified class
   * scope. This includes both single annotations and annotation arrays.
   *
   * <p>Usage example:
   *
   * <pre>
   * ClassDeclarationService classService = new ClassDeclarationService();
   * TSNode classNode = classService.findClassByName(tsFile, "MyClass").get();
   * List&lt;TSNode&gt; annotationNodes = service.getAllFieldDeclarationAnnotationNodes(tsFile, classNode);
   * for (TSNode annotation : annotationNodes) {
   *   String annotationText = tsFile.getTextFromNode(annotation);  // e.g., "@NotNull"
   * }
   * </pre>
   *
   * @param tsFile The {@link TSFile} containing the source code
   * @param classDeclarationNode The class declaration {@link TSNode} to search within
   * @return A list of all field annotation nodes in the class
   */
  public List<TSNode> getAllFieldDeclarationAnnotationNodes(
      TSFile tsFile, TSNode classDeclarationNode) {
    if (tsFile == null
        || tsFile.getTree() == null
        || !classDeclarationNode.getType().equals("class_declaration")) {
      return Collections.emptyList();
    }
    String queryString =
        """
        (class_declaration
          body: (class_body
            (field_declaration
              (modifiers
                [
                  (annotation)
                  (marker_annotation)
                ]? @fieldAnnotation
              )?
            )
          )
        )
        """;
    return tsFile.query(queryString).within(classDeclarationNode).execute().nodes();
  }

  /**
   * Retrieves the complete type node of a field declaration, including generic type information if
   * present.
   *
   * <p>This method extracts the full type node from a field declaration, which includes the base
   * type and any generic type arguments. For example, in {@code List<String> items}, it would
   * return the entire {@code List<String>} type node.
   *
   * <p>Usage example:
   *
   * <pre>
   * List&lt;TSNode&gt; fieldNodes = service.getAllFieldDeclarationNodes(tsFile, classNode);
   * Optional&lt;TSNode&gt; fullTypeNode = service.getFieldDeclarationFullTypeNode(tsFile, fieldNodes.get(0));
   * if (fullTypeNode.isPresent()) {
   *   String type = tsFile.getTextFromNode(fullTypeNode.get());  // e.g., "List&lt;String&gt;"
   * }
   * </pre>
   *
   * @param tsFile The {@link TSFile} containing the source code
   * @param fieldDeclarationNode The field declaration {@link TSNode} to analyze
   * @return An {@link Optional} containing the complete type node if found
   */
  public Optional<TSNode> getFieldDeclarationFullTypeNode(
      TSFile tsFile, TSNode fieldDeclarationNode) {
    return this.getFieldDeclarationNodeByCaptureName(
        tsFile, fieldDeclarationNode, FieldCapture.FIELD_TYPE);
  }

  /**
   * Retrieves the base type node of a field declaration, excluding generic type arguments.
   *
   * <p>This method extracts the base type node from a field declaration. For generic types, it
   * returns only the type argument. For example, in {@code List<String> items}, it would return
   * just the {@code String} type node.
   *
   * <p>Usage example:
   *
   * <pre>
   * List&lt;TSNode&gt; fieldNodes = service.getAllFieldDeclarationNodes(tsFile, classNode);
   * Optional&lt;TSNode&gt; typeNode = service.getFieldDeclarationTypeNode(tsFile, fieldNodes.get(0));
   * if (typeNode.isPresent()) {
   *   String type = tsFile.getTextFromNode(typeNode.get());  // e.g., "String" from List&lt;String&gt;
   * }
   * </pre>
   *
   * @param tsFile The {@link TSFile} containing the source code
   * @param fieldDeclarationNode The field declaration {@link TSNode} to analyze
   * @return An {@link Optional} containing the base type node if found
   */
  public Optional<TSNode> getFieldDeclarationTypeNode(TSFile tsFile, TSNode fieldDeclarationNode) {
    Optional<TSNode> fieldTypeArgumentNode =
        this.getFieldDeclarationNodeByCaptureName(
            tsFile, fieldDeclarationNode, FieldCapture.FIELD_TYPE_ARGUMENT);
    if (fieldTypeArgumentNode.isPresent()) {
      return fieldTypeArgumentNode;
    }
    return this.getFieldDeclarationNodeByCaptureName(
        tsFile, fieldDeclarationNode, FieldCapture.FIELD_TYPE);
  }

  /**
   * Retrieves the name node of a field declaration.
   *
   * <p>This method extracts the identifier node that represents the field's name from a field
   * declaration.
   *
   * <p>Usage example:
   *
   * <pre>
   * List&lt;TSNode&gt; fieldNodes = service.getAllFieldDeclarationNodes(tsFile, classNode);
   * Optional&lt;TSNode&gt; nameNode = service.getFieldDeclarationNameNode(tsFile, fieldNodes.get(0));
   * if (nameNode.isPresent()) {
   *   String name = tsFile.getTextFromNode(nameNode.get());  // e.g., "firstName"
   * }
   * </pre>
   *
   * @param tsFile The {@link TSFile} containing the source code
   * @param fieldDeclarationNode The field declaration {@link TSNode} to analyze
   * @return An {@link Optional} containing the name node if found
   */
  public Optional<TSNode> getFieldDeclarationNameNode(TSFile tsFile, TSNode fieldDeclarationNode) {
    return this.getFieldDeclarationNodeByCaptureName(
        tsFile, fieldDeclarationNode, FieldCapture.FIELD_NAME);
  }

  /**
   * Retrieves the initialization value node of a field declaration.
   *
   * <p>This method extracts the node representing the initialization value of a field, if one
   * exists. The value can be any valid Java expression.
   *
   * <p>Usage example:
   *
   * <pre>
   * List&lt;TSNode&gt; fieldNodes = service.getAllFieldDeclarationNodes(tsFile, classNode);
   * Optional&lt;TSNode&gt; valueNode = service.getFieldDeclarationValueNode(tsFile, fieldNodes.get(0));
   * if (valueNode.isPresent()) {
   *   String value = tsFile.getTextFromNode(valueNode.get());  // e.g., "\"John\""
   * }
   * </pre>
   *
   * @param tsFile The {@link TSFile} containing the source code
   * @param fieldDeclarationNode The field declaration {@link TSNode} to analyze
   * @return An {@link Optional} containing the value node if found
   */
  public Optional<TSNode> getFieldDeclarationValueNode(TSFile tsFile, TSNode fieldDeclarationNode) {
    return this.getFieldDeclarationNodeByCaptureName(
        tsFile, fieldDeclarationNode, FieldCapture.FIELD_VALUE);
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
   * List&lt;TSNode&gt; fieldNodes = service.getAllFieldDeclarationNodes(tsFile, classNode);
   * List&lt;TSNode&gt; usages = service.getAllFieldDeclarationUsageNodes(tsFile, fieldNodes.get(0), classNode);
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
  public List<TSNode> getAllFieldDeclarationUsageNodes(
      TSFile tsFile, TSNode fieldDeclarationNode, TSNode classDeclarationNode) {
    if (tsFile == null
        || tsFile.getTree() == null
        || !fieldDeclarationNode.getType().equals("field_declaration")
        || !classDeclarationNode.getType().equals("class_declaration")) {
      return Collections.emptyList();
    }
    Optional<TSNode> fieldNameNode = this.getFieldDeclarationNameNode(tsFile, fieldDeclarationNode);
    if (fieldNameNode.isEmpty()) {
      return Collections.emptyList();
    }
    String fieldName = tsFile.getTextFromNode(fieldNameNode.get());
    String queryString =
        String.format(
            """
            (field_access
              field: (identifier) @usage)
            (#eq? @usage "%s")
            """,
            fieldName);

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
   * Optional&lt;TSNode&gt; fieldNode = service.findFieldDeclarationNodeByName(tsFile, "myField", classNode);
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
  public Optional<TSNode> findFieldDeclarationNodeByName(
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
   * List&lt;TSNode&gt; stringFields = service.findFieldDeclarationNodesByType(tsFile, "String", classNode);
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
  public List<TSNode> findFieldDeclarationNodesByType(
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
            (field_declaration
              type: [
                (type_identifier) @type
                (generic_type
                  (type_arguments
                    (type_identifier) @type
                  )
                )
              ]
            ) @fieldDeclaration
            (#eq? @type "%s")
            """,
            fieldDeclaratorType);
    return tsFile.query(queryString).within(classDeclarationNode).execute().nodes();
  }

  /**
   * Renames a field declaration's type and optionally its variable name.
   *
   * <p>This method updates both the type and optionally the variable name of a field declaration.
   * It supports renaming both primitive types and reference types. When renaming a field, you can
   * choose whether to also rename the variable name or just the type.
   *
   * <p>Usage example:
   *
   * <pre>
   * List&lt;TSNode&gt; fieldNodes = service.getAllFieldDeclarationNodes(tsFile, classNode);
   * // Rename both type and variable: "String name" to "Username userName"
   * boolean success = service.renameFieldDeclarationNode(
   *     tsFile, fieldNodes.get(0), "Username", "userName", true);
   *
   * // Rename only type: "String name" to "Username name"
   * success = service.renameFieldDeclarationNode(
   *     tsFile, fieldNodes.get(0), "Username", "", false);
   * </pre>
   *
   * @param tsFile The {@link TSFile} containing the source code
   * @param fieldDeclarationNode The field declaration {@link TSNode} to rename
   * @param newTypeName The new type name for the field
   * @param newVariableName The new variable name (used only if shouldRenameVariable is true)
   * @param shouldRenameVariable Whether to rename the variable in addition to the type
   * @return true if the rename operation was successful, false otherwise
   */
  public boolean renameFieldDeclarationNode(
      TSFile tsFile,
      TSNode fieldDeclarationNode,
      String newTypeName,
      String newVariableName,
      Boolean shouldRenameVariable) {
    if (tsFile == null
        || tsFile.getTree() == null
        || Strings.isNullOrEmpty(newTypeName)
        || !fieldDeclarationNode.getType().equals("field_declaration")) {
      return false;
    }
    Optional<TSNode> fieldDeclarationTypeNode =
        this.getFieldDeclarationTypeNode(tsFile, fieldDeclarationNode);
    Optional<TSNode> fieldDeclarationNameNode =
        this.getFieldDeclarationNameNode(tsFile, fieldDeclarationNode);
    if (fieldDeclarationNameNode.isEmpty() || fieldDeclarationTypeNode.isEmpty()) {
      return false;
    }
    if (shouldRenameVariable) {
      tsFile.updateSourceCode(fieldDeclarationNameNode.get(), newVariableName);
    }
    tsFile.updateSourceCode(fieldDeclarationTypeNode.get(), newTypeName);
    return true;
  }

  /**
   * Renames a field usage node to match a new type name, converting it to camelCase.
   *
   * <p>This method updates a field usage identifier to match a new type name, automatically
   * converting the new name to camelCase format. This is typically used in conjunction with {@link
   * #renameFieldDeclarationNode} to update all references to a renamed field.
   *
   * <p>For example, if a field type is renamed from "UserName" to "PersonName", this method would
   * update references from "userName" to "personName".
   *
   * <p>Usage example:
   *
   * <pre>
   * // After renaming a field's type
   * List&lt;TSNode&gt; usages = service.getAllFieldDeclarationUsageNodes(tsFile, fieldNode, classNode);
   * for (TSNode usage : usages) {
   *   boolean success = service.renamedFieldDeclarationUsageNode(tsFile, usage, "PersonName");
   *   // Updates "userName" to "personName"
   * }
   * </pre>
   *
   * @param tsFile The {@link TSFile} containing the source code
   * @param fieldDeclarationUsageNode The field usage {@link TSNode} to rename
   * @param newTypeName The new type name to convert to camelCase and apply
   * @return true if the rename operation was successful, false otherwise
   */
  public boolean renameFieldDeclarationUsageNode(
      TSFile tsFile, TSNode fieldDeclarationUsageNode, String newTypeName) {
    if (tsFile == null
        || tsFile.getTree() == null
        || Strings.isNullOrEmpty(newTypeName)
        || !fieldDeclarationUsageNode.getType().equals("identifier")) {
      return false;
    }
    tsFile.updateSourceCode(fieldDeclarationUsageNode, newTypeName);
    return true;
  }
}
