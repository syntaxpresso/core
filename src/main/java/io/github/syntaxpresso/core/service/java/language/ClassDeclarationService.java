package io.github.syntaxpresso.core.service.java.language;

import com.google.common.base.Strings;
import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.service.java.language.extra.ClassCapture;
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

/**
 * Service for analyzing and manipulating class declarations in Java source code using tree-sitter.
 *
 * <p>This service provides comprehensive functionality for working with class declarations within
 * Java source files, including extraction of class information, finding classes by name, resolving
 * superclass relationships, and performing class-related transformations. It leverages tree-sitter
 * queries to accurately parse and analyze class declarations at the AST level.
 *
 * <p>Key capabilities include:
 *
 * <ul>
 *   <li>Extracting class name, annotations, modifiers, and superclass information
 *   <li>Finding all class declarations within a file
 *   <li>Locating classes by name or file-based public class lookup
 *   <li>Resolving superclass fully qualified names
 *   <li>Checking for local vs external class references
 *   <li>Renaming class declarations and associated files
 * </ul>
 *
 * <p>Usage example:
 *
 * <pre>
 * ClassDeclarationService classService = new ClassDeclarationService(fieldService, methodService);
 *
 * // Find a specific class by name
 * Optional&lt;TSNode&gt; classNode = classService.findClassByName(tsFile, "UserService");
 * if (classNode.isPresent()) {
 *   // Get detailed class information
 *   List&lt;Map&lt;String, TSNode&gt;&gt; classInfo = classService.getClassDeclarationNodeInfo(tsFile, classNode.get());
 *   for (Map&lt;String, TSNode&gt; info : classInfo) {
 *     String className = tsFile.getTextFromNode(info.get("className"));
 *     TSNode superclass = info.get("superclass");
 *     if (superclass != null) {
 *       String superclassName = tsFile.getTextFromNode(superclass);
 *       System.out.println("Class " + className + " extends " + superclassName);
 *     }
 *   }
 * }
 *
 * // Find the main public class of a file
 * Optional&lt;TSNode&gt; publicClass = classService.getPublicClass(tsFile);
 * if (publicClass.isPresent()) {
 *   String className = tsFile.getTextFromNode(classService.getClassDeclarationNameNode(tsFile, publicClass.get()).get());
 *   System.out.println("Public class: " + className);
 * }
 * </pre>
 *
 * @see TSFile
 * @see ClassCapture
 * @see FieldDeclarationService
 * @see MethodDeclarationService
 */
@Getter
@RequiredArgsConstructor
public class ClassDeclarationService {
  private final FieldDeclarationService fieldDeclarationService;
  private final MethodDeclarationService methodDeclarationService;

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
        String.format(
            """
               (class_declaration
                 (modifiers
                   [
                     (annotation)
                     (marker_annotation)
                   ] %s
                 )? %s
                 name: (_) %s
                 interfaces: (_)? %s
                  (superclass
                    [
                      (generic_type
                        (type_identifier) %s)
                      (type_identifier) %s
                    ])? %s
                  body: (class_body
                   (constructor_declaration)? %s
                  ) %s
                ) %s
            """,
            ClassCapture.CLASS_ANNOTATION.getCaptureWithAt(),
            ClassCapture.MODIFIERS.getCaptureWithAt(),
            ClassCapture.CLASS_NAME.getCaptureWithAt(),
            ClassCapture.CLASS_INTERFACES.getCaptureWithAt(),
            ClassCapture.SUPERCLASS_NAME.getCaptureWithAt(),
            ClassCapture.SUPERCLASS_NAME.getCaptureWithAt(),
            ClassCapture.SUPERCLASS.getCaptureWithAt(),
            ClassCapture.CONSTRUCTOR_DECLARATION.getCaptureWithAt(),
            ClassCapture.CLASS_BODY.getCaptureWithAt(),
            ClassCapture.CLASS.getCaptureWithAt());
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
   * @param classDeclarationNode The class declaration node to analyze
   * @param capture The capture name to retrieve (e.g. "className", "superclassName")
   * @return Optional containing the requested node if found, empty otherwise
   */
  public Optional<TSNode> getClassDeclarationNodeByCaptureName(
      TSFile tsFile, TSNode classDeclarationNode, ClassCapture capture) {
    if (tsFile == null
        || tsFile.getTree() == null
        || !classDeclarationNode.getType().equals("class_declaration")) {
      return Optional.empty();
    }
    List<Map<String, TSNode>> classDeclarationInfo =
        this.getClassDeclarationNodeInfo(tsFile, classDeclarationNode);
    for (Map<String, TSNode> map : classDeclarationInfo) {
      TSNode node = map.get(capture.getCaptureName());
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
   * @param capture The capture name to retrieve nodes for
   * @return List of nodes matching the capture name. Returns an empty list if none found.
   */
  public List<TSNode> getClassDeclarationNodesFromCaptureName(
      TSFile tsFile, TSNode classDeclarationNode, ClassCapture capture) {
    List<Map<String, TSNode>> classDeclarationInfo =
        this.getClassDeclarationNodeInfo(tsFile, classDeclarationNode);
    List<TSNode> foundNodes = new ArrayList<>();
    for (Map<String, TSNode> map : classDeclarationInfo) {
      TSNode node = map.get(capture.getCaptureName());
      if (node != null) {
        foundNodes.add(node);
      }
    }
    return foundNodes;
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
        tsFile, classDeclarationNode, ClassCapture.CLASS_ANNOTATION);
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
    return this.getClassDeclarationNodeByCaptureName(
        tsFile, classDeclarationNode, ClassCapture.CLASS_NAME);
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
        tsFile, classDeclarationNode, ClassCapture.SUPERCLASS_NAME);
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
   * @param tsFile {@link TSFile} containing the source code
   * @param className Name of the class to search for
   * @return {@link Optional} containing the class declaration node, or empty if not found,
   *     file/tree is null, or className is empty
   */
  public Optional<TSNode> findClassByName(TSFile tsFile, String className) {
    if (tsFile == null || tsFile.getTree() == null || Strings.isNullOrEmpty(className)) {
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
    return tsFile.query(queryString).returning("classDeclaration").execute().firstNodeOptional();
  }

  /**
   * Gets the public class of a file, defined as the public class whose name matches the file name.
   *
   * <p>Usage example:
   *
   * <pre>
   * // For a file named "MyClass.java"
   * Optional<TSNode> publicClass = service.getPublicClass(tsFile);
   * if (publicClass.isPresent()) {
   *   String publicClass = tsFile.getTextFromNode(publicClass.get());
   *   // className = "public class MyClass ..."
   * }
   * </pre>
   *
   * @param tsFile The {@link TSFile} to analyze.
   * @return An {@link Optional} containing the public class declaration node, or empty if not
   *     found, file/tree is null, or file name is missing.
   */
  public Optional<TSNode> getPublicClass(TSFile tsFile) {
    if (tsFile == null || tsFile.getTree() == null) {
      return Optional.empty();
    }
    Optional<String> fileName = tsFile.getFileNameWithoutExtension();
    if (fileName.isEmpty()) {
      return Optional.empty();
    }
    return this.findClassByName(tsFile, fileName.get());
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
