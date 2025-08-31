package io.github.syntaxpresso.core.service.java.language;

import com.google.common.base.Strings;
import io.github.syntaxpresso.core.common.TSFile;
import java.nio.file.Files;
import java.nio.file.Path;
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
   * Retrieves the class name node from a class declaration.
   *
   * <p>Usage example:
   *
   * <pre>
   * Optional<TSNode> classNameNode = service.getClassNameNode(tsFile, classNode);
   * if (classNameNode.isPresent()) {
   *   String className = tsFile.getTextFromNode(classNameNode.get());
   *   // className = "MyClass"
   * }
   * </pre>
   *
   * Query captures: - className: The identifier node containing the class name
   *
   * @param file The {@link TSFile} containing the source code.
   * @param classNode The class declaration {@link TSNode}.
   * @return An {@link Optional} containing the class name identifier node, or empty if not found,
   *     file/tree is null, or node is not a class declaration.
   */
  public Optional<TSNode> getClassNameNode(TSFile file, TSNode classNode) {
    if (file == null
        || file.getTree() == null
        || classNode == null
        || !"class_declaration".equals(classNode.getType())) {
      return Optional.empty();
    }
    String queryString =
        """
        (class_declaration
          name: (identifier) @className)
        """;
    Optional<TSNode> classNameNode =
        file.query(queryString).returning("className").execute().firstNodeOptional();
    return classNameNode;
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
   * Query captures:
   * - className: The identifier node containing the matched class name
   * - classDeclaration: The full class declaration node to be returned
   *
   * <p>Example tree-sitter query used:
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
   * Renames a class by updating its declaration in the source code.
   *
   * <p>Usage example:
   *
   * <pre>
   * // Rename class from "OldClass" to "NewClass"
   * Optional<TSNode> renamedClass = service.renameClass(tsFile, "OldClass", "NewClass");
   * if (renamedClass.isPresent()) {
   *   String newClassDecl = tsFile.getTextFromNode(renamedClass.get());
   *   // newClassDecl = "public class NewClass {...}"
   * }
   * </pre>
   *
   * @param file The {@link TSFile} containing the source code.
   * @param oldName The current name of the class to rename.
   * @param newName The new name for the class.
   * @return An {@link Optional} containing the updated class declaration node, or empty if class
   *     not found, file/tree is null, or names are empty.
   */
  public Optional<TSNode> renameClass(TSFile file, String oldName, String newName) {
    if (file == null
        || file.getTree() == null
        || Strings.isNullOrEmpty(oldName)
        || Strings.isNullOrEmpty(newName)) {
      return Optional.empty();
    }
    Optional<TSNode> foundClass = this.findClassByName(file, oldName);
    if (foundClass.isEmpty()) {
      return Optional.empty();
    }
    Optional<TSNode> classNameNode = this.getClassNameNode(file, foundClass.get());
    if (classNameNode.isEmpty()) {
      return Optional.empty();
    }
    file.updateSourceCode(classNameNode.get(), newName);
    return this.findClassByName(file, newName);
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
   * Retrieves information about the direct superclass from a class declaration node.
   *
   * <p>Usage example:
   *
   * <pre>
   * Optional<Map<String, TSNode>> info = service.getSuperclassInfo(tsFile, classNode);
   * if (info.isPresent()) {
   *   TSNode superclass = info.get().get("superclass");     // The full superclass node
   *   TSNode superName = info.get().get("superclassName"); // The superclass name identifier
   * }
   * </pre>
   *
   * @param file The {@link TSFile} containing the class declaration.
   * @param classDeclarationNode The {@link TSNode} representing the class declaration.
   * @return An {@link Optional} map with the following capture names: - superclass: The full
   *     superclass node - superclassName: The superclass name identifier node Returns empty if no
   *     superclass is found.
   */
  public Optional<Map<String, TSNode>> getSuperclassInfo(TSFile file, TSNode classDeclarationNode) {
    String queryString =
        """
        (class_declaration
          superclass: (superclass
            (type_identifier) @superclassName) @superclass)
        """;
    return file.query(queryString)
        .within(classDeclarationNode)
        .returningAllCaptures()
        .execute()
        .firstCaptureOptional();
  }

  /**
   * Gets the direct superclass node from a class declaration.
   *
   * <p>Usage example:
   *
   * <pre>
   * Optional<TSNode> superclassNode = service.getSuperclassNode(tsFile, classNode);
   * if (superclassNode.isPresent()) {
   *   String superclassText = tsFile.getTextFromNode(superclassNode.get());
   *   // superclassText = "extends BaseClass"
   * }
   * </pre>
   *
   * @param file The {@link TSFile} containing the class declaration.
   * @param classDeclarationNode The {@link TSNode} representing the class declaration.
   * @return An {@link Optional} containing the superclass node (including the "extends" keyword),
   *     or empty if the class doesn't extend another class.
   */
  public Optional<TSNode> getSuperclassNode(TSFile file, TSNode classDeclarationNode) {
    Optional<Map<String, TSNode>> superclassInfo = getSuperclassInfo(file, classDeclarationNode);
    if (superclassInfo.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(superclassInfo.get().get("superclass"));
  }

  /**
   * Finds the simple name of the direct superclass from a class declaration node.
   *
   * <p>Usage example:
   *
   * <pre>
   * Optional<String> superName = service.getSuperclassName(tsFile, classNode);
   * </pre>
   *
   * @param file The {@link TSFile} containing the class declaration.
   * @param classDeclarationNode The {@link TSNode} representing the class declaration.
   * @return An {@link Optional} containing the superclass name as a String, or empty if the class
   *     does not explicitly extend another class.
   */
  public Optional<String> getSuperclassName(TSFile file, TSNode classDeclarationNode) {
    Optional<Map<String, TSNode>> superclassInfo = getSuperclassInfo(file, classDeclarationNode);
    if (superclassInfo.isEmpty()) {
      return Optional.empty();
    }
    TSNode superclassNameNode = superclassInfo.get().get("superclassName");
    String superclassName = file.getTextFromNode(superclassNameNode);
    return Optional.of(superclassName);
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
