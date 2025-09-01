package io.github.syntaxpresso.core.service.java.language;

import com.google.common.base.Strings;
import io.github.syntaxpresso.core.common.TSFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.treesitter.TSNode;

@RequiredArgsConstructor
@Getter
public class ClassDeclarationService {

  /**
   * Gets detailed information about a class declaration, including annotations, modifiers, class
   * name, and superclass.
   *
   * <p>Usage example:
   *
   * <pre>
   * List<Map<String, TSNode>> info = service.getClassDeclarationNodeInfo(tsFile, classNode);
   * for (Map<String, TSNode> classInfo : info) {
   *   TSNode className = classInfo.get("className");           // The class name node
   *   TSNode modifiers = classInfo.get("modifiers");          // Class modifiers node (if present)
   *   TSNode annotation = classInfo.get("classAnnotation");   // Class annotation node (if present)
   *   TSNode superclass = classInfo.get("superclass");        // Superclass node (if present)
   *   TSNode superclassName = classInfo.get("superclassName"); // Superclass name identifier (if present)
   * }
   * </pre>
   *
   * @param tsFile The {@link TSFile} containing the class declaration
   * @param classDeclarationNode The class declaration node to analyze
   * @return List of maps with capture names: className, modifiers, classAnnotation, superclass,
   *     superclassName
   */
  public List<Map<String, TSNode>> getClassDeclarationNodeInfo(
      TSFile tsFile, TSNode classDeclarationNode) {
    if (tsFile == null
        || tsFile.getTree() == null
        || !classDeclarationNode.getType().equals("class_declaration")) {
      return Collections.emptyList();
    }
    String queryString =
        """
        (class_declaration
          (modifiers
            [
              (annotation)
              (marker_annotation)
            ] @classAnnotation
          )? @classModifiers
          name: (_) @className
          (superclass
            [
              (generic_type
                (type_identifier) @superclassName
              )
              (type_identifier) @superclassName
            ]
          )? @superclass
          body: (class_body
            (field_declaration
              (modifiers
                [
                  (annotation)
                  (marker_annotation)
                ] @classFieldAnnotation
              )? @classFieldModifiers
              type: (_) @classFieldType
              declarator: (variable_declarator
                name: (identifier) @classFieldName
                value: (_)? @classFieldValue
              )
            ) @classField
          ) @classBody
        ) @class
        """;
    return tsFile
        .query(queryString)
        .within(classDeclarationNode)
        .returningAllCaptures()
        .execute()
        .captures();
  }

  /**
   * Finds all class declarations in the given {@link TSFile}.
   *
   * <p>Usage example:
   *
   * <pre>
   * List<Map<String, TSNode>> classes = service.getAllClassDeclarations(tsFile);
   * for (Map<String, TSNode> classInfo : classes) {
   *   TSNode className = classInfo.get("className");         // The class name identifier node
   *   TSNode classDecl = classInfo.get("classDeclaration"); // The full class declaration node
   * }
   * </pre>
   *
   * @param file The {@link TSFile} containing the source code to search.
   * @return A list of maps with the following capture names: - className: The class name identifier
   *     node - classDeclaration: The full class declaration node Returns an empty list if the file
   *     or its tree is null.
   */
  public List<Map<String, TSNode>> getAllClassDeclarations(TSFile file) {
    if (file == null || file.getTree() == null) {
      return Collections.emptyList();
    }
    String queryString =
        """
        (class_declaration
          name: (identifier) @className) @classDeclaration
        """;
    return file.query(queryString).returningAllCaptures().execute().captures();
  }

  /**
   * Retrieves a specific node from a class declaration using a capture name.
   *
   * <p>Usage example:
   *
   * <pre>
   * Optional<TSNode> node = service.getClassDeclarationNodeByCaptureName(tsFile, "className", classNode);
   * if (node.isPresent()) {
   *   String text = tsFile.getTextFromNode(node.get());
   *   // text = "MyClass"
   * }
   * </pre>
   *
   * @param tsFile The {@link TSFile} containing the class declaration
   * @param captureName The capture name to retrieve (e.g. "className", "superclassName")
   * @param classDeclarationNode The class declaration node to analyze
   * @return Optional containing the requested node if found, empty otherwise
   */
  public Optional<TSNode> getClassDeclarationNodeByCaptureName(
      TSFile tsFile, String captureName, TSNode classDeclarationNode) {
    if (tsFile == null
        || tsFile.getTree() == null
        || !classDeclarationNode.getType().equals("class_declaration")) {
      return Optional.empty();
    }
    List<Map<String, TSNode>> classDeclarationInfo =
        this.getClassDeclarationNodeInfo(tsFile, classDeclarationNode);
    for (Map<String, TSNode> map : classDeclarationInfo) {
      TSNode node = map.get(captureName);
      if (node != null) {
        return Optional.of(node);
      }
    }
    return Optional.empty();
  }

  /**
   * Gets all nodes from a class declaration that match a specific capture name.
   *
   * <p>Usage example:
   *
   * <pre>
   * List<TSNode> nodes = service.getClassDeclarationNodesFromCaptureName(tsFile, classNode, "classField");
   * for (TSNode node : nodes) {
   *   String text = tsFile.getTextFromNode(node);
   *   // Process each matching node
   * }
   * </pre>
   *
   * @param tsFile The {@link TSFile} containing the class declaration
   * @param classDeclarationNode The class declaration node to analyze
   * @param captureName The capture name to retrieve nodes for
   * @return List of nodes matching the capture name. Returns an empty list if none found.
   */
  public List<TSNode> getClassDeclarationNodesFromCaptureName(
      TSFile tsFile, TSNode classDeclarationNode, String captureName) {
    List<Map<String, TSNode>> classDeclarationInfo =
        this.getClassDeclarationNodeInfo(tsFile, classDeclarationNode);
    List<TSNode> foundNodes = new ArrayList<>();
    for (Map<String, TSNode> map : classDeclarationInfo) {
      TSNode node = map.get(captureName);
      if (node != null) {
        foundNodes.add(node);
      }
    }
    return foundNodes;
  }

  /**
   * Gets all field declaration nodes from a class.
   *
   * <p>Usage example:
   *
   * <pre>
   * List<TSNode> fields = service.getAllClassFieldDeclarationNodes(tsFile, classNode);
   * for (TSNode field : fields) {
   *   String fieldText = tsFile.getTextFromNode(field);
   *   // fieldText = "private String name;" or similar
   * }
   * </pre>
   *
   * @param tsFile The {@link TSFile} containing the class declaration
   * @param classDeclarationNode The class declaration node to analyze
   * @return List of field declaration nodes found in the class. Returns an empty list if none
   *     found.
   */
  public List<TSNode> getAllClassFieldDeclarationNodes(TSFile tsFile, TSNode classDeclarationNode) {
    return this.getClassDeclarationNodesFromCaptureName(tsFile, classDeclarationNode, "classField");
  }

  /**
   * Gets all annotation nodes from a class declaration.
   *
   * <p>Usage example:
   *
   * <pre>
   * List<TSNode> annotations = service.getClassDeclarationAnnotationNodes(tsFile, classNode);
   * for (TSNode annotation : annotations) {
   *   String annotationText = tsFile.getTextFromNode(annotation);
   *   // annotationText = "@Entity" or "@Table(name = "my_table")"
   * }
   * </pre>
   *
   * @param tsFile The {@link TSFile} containing the class declaration
   * @param classDeclarationNode The class declaration node to analyze
   * @return List of annotation nodes found on the class declaration. Returns an empty list if no
   *     annotations are found, or if the file, tree, or node is invalid.
   */
  public List<TSNode> getClassDeclarationAnnotationNodes(
      TSFile tsFile, TSNode classDeclarationNode) {
    return this.getClassDeclarationNodesFromCaptureName(
        tsFile, classDeclarationNode, "classAnnotation");
  }

  /**
   * Gets all annotation nodes from field declarations within a class.
   *
   * <p>Usage example:
   *
   * <pre>
   * List<TSNode> annotations = service.getClassFieldAnnotationNodes(tsFile, classNode);
   * for (TSNode annotation : annotations) {
   *   String annotationText = tsFile.getTextFromNode(annotation);
   *   // annotationText = "@Column" or "@NotNull" or similar
   * }
   * </pre>
   *
   * @param tsFile The {@link TSFile} containing the class declaration
   * @param classDeclarationNode The class declaration node to analyze
   * @return List of annotation nodes found on field declarations. Returns an empty list if none
   *     found.
   */
  public List<TSNode> getClassFieldAnnotationNodes(TSFile tsFile, TSNode classDeclarationNode) {
    return this.getClassDeclarationNodesFromCaptureName(
        tsFile, classDeclarationNode, "classFieldAnnotation");
  }

  /**
   * Gets the name identifier node from a class declaration.
   *
   * <p>Usage example:
   *
   * <pre>
   * Optional<TSNode> nameNode = service.getClassDeclarationNameNode(tsFile, classNode);
   * if (nameNode.isPresent()) {
   *   String className = tsFile.getTextFromNode(nameNode.get());
   *   // className = "MyClass"
   * }
   * </pre>
   *
   * @param tsFile The {@link TSFile} containing the class declaration
   * @param classDeclarationNode The class declaration node to analyze
   * @return Optional containing the class name identifier node if found, empty otherwise
   */
  public Optional<TSNode> getClassDeclarationNameNode(TSFile tsFile, TSNode classDeclarationNode) {
    return this.getClassDeclarationNodeByCaptureName(tsFile, "className", classDeclarationNode);
  }

  /**
   * Gets the type node of a field declaration.
   *
   * <p>Usage example:
   *
   * <pre>
   * Optional<TSNode> typeNode = service.getClassFieldTypeNode(tsFile, fieldNode);
   * if (typeNode.isPresent()) {
   *   String type = tsFile.getTextFromNode(typeNode.get());
   *   // type = "String" or "List<Integer>" etc.
   * }
   * </pre>
   *
   * @param tsFile The {@link TSFile} containing the field declaration
   * @param classDeclarationNode The field declaration node to analyze
   * @return Optional containing the field type node if found, empty otherwise
   */
  public Optional<TSNode> getClassFieldTypeNode(TSFile tsFile, TSNode classDeclarationNode) {
    return this.getClassDeclarationNodeByCaptureName(
        tsFile, "classFieldType", classDeclarationNode);
  }

  /**
   * Gets the name node of a field declaration.
   *
   * <p>Usage example:
   *
   * <pre>
   * Optional<TSNode> nameNode = service.getClassFieldNameNode(tsFile, fieldNode);
   * if (nameNode.isPresent()) {
   *   String fieldName = tsFile.getTextFromNode(nameNode.get());
   *   // fieldName = "name" or "count" etc.
   * }
   * </pre>
   *
   * @param tsFile The {@link TSFile} containing the field declaration
   * @param classDeclarationNode The field declaration node to analyze
   * @return Optional containing the field name node if found, empty otherwise
   */
  public Optional<TSNode> getClassFieldNameNode(TSFile tsFile, TSNode classDeclarationNode) {
    return this.getClassDeclarationNodeByCaptureName(
        tsFile, "classFieldName", classDeclarationNode);
  }

  /**
   * Gets the initialization value node of a field declaration.
   *
   * <p>Usage example:
   *
   * <pre>
   * Optional<TSNode> valueNode = service.getClassFieldValueNode(tsFile, fieldNode);
   * if (valueNode.isPresent()) {
   *   String initialValue = tsFile.getTextFromNode(valueNode.get());
   *   // initialValue = "\"default\"" or "0" or "new ArrayList<>()" etc.
   * }
   * </pre>
   *
   * @param tsFile The {@link TSFile} containing the field declaration
   * @param classDeclarationNode The field declaration node to analyze
   * @return Optional containing the field initialization value node if found, empty otherwise
   */
  public Optional<TSNode> getClassFieldValueNode(TSFile tsFile, TSNode classDeclarationNode) {
    return this.getClassDeclarationNodeByCaptureName(
        tsFile, "classFieldValue", classDeclarationNode);
  }

  /**
   * Gets the superclass name identifier node from a class declaration.
   *
   * <p>Usage example:
   *
   * <pre>
   * Optional<TSNode> superclassNode = service.getClassDeclarationSuperclassNameNode(tsFile, classNode);
   * if (superclassNode.isPresent()) {
   *   String superclassName = tsFile.getTextFromNode(superclassNode.get());
   *   // superclassName = "BaseClass"
   * }
   * </pre>
   *
   * @param tsFile The {@link TSFile} containing the class declaration
   * @param classDeclarationNode The class declaration node to analyze
   * @return Optional containing the superclass name identifier node if found, empty otherwise
   */
  public Optional<TSNode> getClassDeclarationSuperclassNameNode(
      TSFile tsFile, TSNode classDeclarationNode) {
    return this.getClassDeclarationNodeByCaptureName(
        tsFile, "superclassName", classDeclarationNode);
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
   * Finds a class declaration node by its name.
   *
   * <p>Usage example:
   *
   * <pre>
   * Optional<TSNode> classNode = service.findClassByName(tsFile, "MyClass");
   * if (classNode.isPresent()) {
   *   String classDecl = tsFile.getTextFromNode(classNode.get());
   *   // classDecl = "public class MyClass extends BaseClass { private String name; }"
   * }
   * </pre>
   *
   * Query captures: - className: The identifier node containing the matched class name -
   * classDeclaration: The full class declaration node to be returned
   *
   * <p>Example tree-sitter query used:
   *
   * <pre>
   * (class_declaration @classDeclaration
   *   name: (identifier) @className
   *   (#eq? @className "MyClass"))
   * </pre>
   *
   * @param file {@link TSFile} containing the source code
   * @param className Name of the class to search for
   * @return {@link Optional} containing the class declaration node, or empty if not found,
   *     file/tree is null, or className is empty
   */
  public Optional<TSNode> findClassByName(TSFile file, String className) {
    if (file == null || file.getTree() == null || Strings.isNullOrEmpty(className)) {
      return Optional.empty();
    }
    String queryString =
        String.format(
            """
            (class_declaration
              name: (identifier) @className
            (#eq? @className "%s")) @classDeclaration
            """,
            className);
    return file.query(queryString).returning("classDeclaration").execute().firstNodeOptional();
  }

  /**
   * Gets the main class of a file, defined as the public class whose name matches the file name.
   *
   * <p>Usage example:
   *
   * <pre>
   * // For a file named "MyClass.java"
   * Optional<TSNode> mainClass = service.getMainClass(tsFile);
   * if (mainClass.isPresent()) {
   *   String className = tsFile.getTextFromNode(mainClass.get());
   *   // className = "public class MyClass ..."
   * }
   * </pre>
   *
   * @param file The {@link TSFile} to analyze.
   * @return An {@link Optional} containing the main class declaration node, or empty if not found,
   *     file/tree is null, or file name is missing.
   */
  public Optional<TSNode> getMainClass(TSFile file) {
    if (file == null || file.getTree() == null) {
      return Optional.empty();
    }
    Optional<String> fileName = file.getFileNameWithoutExtension();
    if (fileName.isEmpty()) {
      return Optional.empty();
    }
    return this.findClassByName(file, fileName.get());
  }

  /**
   * Checks if a class, identified by its fully qualified name, exists as a local source file within
   * the project.
   *
   * <p>Usage example:
   *
   * <pre>
   * boolean exists = service.isLocalClass(projectRoot, "com.example.MyClass");
   * </pre>
   *
   * @param projectRoot The root {@link Path} of the project.
   * @param fullyQualifiedClassName The fully qualified name of the class to check.
   * @return {@code true} if the corresponding .java file exists in the project's main source
   *     directory, {@code false} otherwise.
   */
  public Boolean isLocalClass(Path projectRoot, String fullyQualifiedClassName) {
    String relativePath = fullyQualifiedClassName.replace('.', '/') + ".java";
    Path sourcePath = projectRoot.resolve("src/main/java").resolve(relativePath);
    return Files.exists(sourcePath);
  }

  /**
   * Resolves the fully qualified name of a superclass using the context of a given file.
   *
   * <p>The method checks for fully qualified names, implicit {@code java.lang} imports, and assumes
   * the same package if not found elsewhere.
   *
   * <p>Usage example:
   *
   * <pre>
   * Optional<String> fqName = service.getFullyQualifiedSuperclass(tsFile, "BaseClass");
   * </pre>
   *
   * @param file The {@link TSFile} where the superclass is referenced, providing context for
   *     imports and package.
   * @param superclassName The simple or partially qualified name of the superclass.
   * @return An {@link Optional} containing the fully qualified name of the superclass, or the
   *     simple name if it cannot be resolved.
   */
  public Optional<String> getFullyQualifiedSuperclass(TSFile file, String superclassName) {
    // First check if it's already fully qualified
    if (superclassName.contains(".")) {
      return Optional.of(superclassName);
    }
    // Check if it's in java.lang (implicit imports)
    if (this.isJavaLangClass(superclassName)) {
      return Optional.of("java.lang." + superclassName);
    }
    // If not found in imports and not in java.lang, assume it's in the same package
    // Note: Import resolution would require ImportDeclarationService, but for now
    // we'll assume same package for simplicity
    return Optional.of(superclassName); // Simplified - just return the simple name
  }

  /**
   * Checks if a class name corresponds to a common class in the {@code java.lang} package, which is
   * implicitly imported in Java.
   *
   * <p>Usage example:
   *
   * <pre>
   * boolean isLang = service.isJavaLangClass("String");
   * </pre>
   *
   * @param className The simple name of the class.
   * @return {@code true} if the class is a known {@code java.lang} class, {@code false} otherwise.
   */
  public boolean isJavaLangClass(String className) {
    return List.of(
            "Object",
            "String",
            "Integer",
            "Long",
            "Double",
            "Float",
            "Boolean",
            "Character",
            "Byte",
            "Short")
        .contains(className);
  }
}
