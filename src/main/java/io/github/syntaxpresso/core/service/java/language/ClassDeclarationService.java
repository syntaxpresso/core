package io.github.syntaxpresso.core.service.java.language;

import io.github.syntaxpresso.core.common.TSFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.treesitter.TSNode;

@RequiredArgsConstructor
@Getter
public class ClassDeclarationService {
  private static final String CLASS_DECLARATION_QUERY = "(class_declaration) @class";
  private final FieldDeclarationService fieldDeclarationService;

  /**
   * Finds all class declarations in the given TSFile.
   *
   * @param file The TSFile containing the source code.
   * @return A list of all class declaration nodes.
   */
  public List<TSNode> findAllClassDeclarations(TSFile file) {
    if (file == null || file.getTree() == null) {
      return Collections.emptyList();
    }
    return file.query(CLASS_DECLARATION_QUERY);
  }

  /**
   * Gets the class name node.
   *
   * @param file The TSFile containing the source code.
   * @param classNode The class declaration node.
   * @return The class name node, or empty if not found.
   */
  public Optional<TSNode> getClassNameNode(TSFile file, TSNode classNode) {
    if (file == null || classNode == null || !"class_declaration".equals(classNode.getType())) {
      return Optional.empty();
    }
    TSNode nameNode = classNode.getChildByFieldName("name");
    if (nameNode != null) {
      return Optional.of(nameNode);
    }
    return Optional.empty();
  }

  /**
   * Gets the class name from a class declaration node.
   *
   * @param file The TSFile containing the source code.
   * @param classNode The class declaration node.
   * @return The class name, or empty if not found.
   */
  public Optional<String> getClassName(TSFile file, TSNode classNode) {
    if (file == null || classNode == null || !"class_declaration".equals(classNode.getType())) {
      return Optional.empty();
    }
    TSNode nameNode = classNode.getChildByFieldName("name");
    if (nameNode != null) {
      return Optional.of(file.getTextFromNode(nameNode));
    }
    return Optional.empty();
  }

  /**
   * Gets the name of the main class in the file.
   *
   * @param file The TSFile containing the source code.
   * @return The main class name, or empty if not found.
   */
  public Optional<String> getClassName(TSFile file) {
    Optional<TSNode> mainClassDeclarationNode = this.getMainClass(file);
    if (mainClassDeclarationNode.isEmpty()) {
      return Optional.empty();
    }
    return this.getClassName(file, mainClassDeclarationNode.get());
  }

  /**
   * Finds a class declaration by name.
   *
   * @param file The TSFile containing the source code.
   * @param className The name of the class to find.
   * @return The class declaration node, or empty if not found.
   */
  public Optional<TSNode> findClassByName(TSFile file, String className) {
    if (file == null || className == null || className.trim().isEmpty()) {
      return Optional.empty();
    }
    List<TSNode> classNodes = this.findAllClassDeclarations(file);
    for (TSNode classNode : classNodes) {
      Optional<String> name = this.getClassName(file, classNode);
      if (name.isPresent() && name.get().equals(className)) {
        return Optional.of(classNode);
      }
    }
    return Optional.empty();
  }

  /**
   * Gets all field declarations within a specific class.
   *
   * @param file The TSFile containing the source code.
   * @param classNode The class declaration node.
   * @return A list of field declaration nodes within the class.
   */
  public List<TSNode> getClassFields(TSFile file, TSNode classNode) {
    if (file == null || classNode == null || !"class_declaration".equals(classNode.getType())) {
      return Collections.emptyList();
    }
    return file.query(classNode, "(field_declaration) @field");
  }

  /**
   * Gets all method declarations within a specific class.
   *
   * @param file The TSFile containing the source code.
   * @param classNode The class declaration node.
   * @return A list of method declaration nodes within the class.
   */
  public List<TSNode> getClassMethods(TSFile file, TSNode classNode) {
    if (file == null || classNode == null || !"class_declaration".equals(classNode.getType())) {
      return Collections.emptyList();
    }
    return file.query(classNode, "(method_declaration) @method");
  }

  /**
   * Finds all methods with a specific name within a class.
   *
   * @param file The TSFile containing the source code.
   * @param classNode The class declaration node.
   * @param methodName The method name to search for.
   * @return A list of method declaration nodes with the specified name.
   */
  public List<TSNode> findMethodsByName(TSFile file, TSNode classNode, String methodName) {
    if (file == null || classNode == null || methodName == null) {
      return Collections.emptyList();
    }
    List<TSNode> classMethods = this.getClassMethods(file, classNode);
    List<TSNode> matchingMethods = new ArrayList<>();
    for (TSNode methodNode : classMethods) {
      TSNode nameNode = methodNode.getChildByFieldName("name");
      if (nameNode != null && methodName.equals(file.getTextFromNode(nameNode))) {
        matchingMethods.add(methodNode);
      }
    }
    return matchingMethods;
  }

  /**
   * Renames a class by updating its declaration.
   *
   * @param file The TSFile containing the source code.
   * @param classNode The class declaration node to rename.
   * @param newName The new name for the class.
   */
  public void renameClass(TSFile file, TSNode classNode, String newName) {
    if (file == null || classNode == null || newName == null || newName.trim().isEmpty()) {
      return;
    }
    if (!"class_declaration".equals(classNode.getType())) {
      return;
    }
    TSNode nameNode = classNode.getChildByFieldName("name");
    if (nameNode != null) {
      file.updateSourceCode(nameNode, newName);
    }
  }

  /**
   * Gets the main class of a file, which is the public class that has the same name as the file.
   *
   * @param file The TSFile to analyze.
   * @return An Optional containing the main class declaration node, or empty if not found.
   */
  public Optional<TSNode> getMainClass(TSFile file) {
    if (file == null) {
      return Optional.empty();
    }
    try {
      if (file.getFile() == null) {
        return Optional.empty();
      }
    } catch (IllegalStateException e) {
      return Optional.empty();
    }
    Optional<String> fileName = file.getFileNameWithoutExtension();
    if (fileName.isEmpty()) {
      return Optional.empty();
    }
    List<TSNode> classNodes = findAllClassDeclarations(file);
    for (TSNode classNode : classNodes) {
      Optional<String> className = getClassName(file, classNode);
      if (className.isPresent() && fileName.get().equals(className.get())) {
        return Optional.of(classNode);
      }
    }
    return Optional.empty();
  }

  /**
   * Get field declaration name.
   *
   * @param file The TSFile to analyze.
   * @param fieldDeclarationNode THe field declaration node.
   * @return An Optional containing the main field name node, or empty if not found.
   */
  public Optional<TSNode> getFieldNameNode(TSFile file, TSNode fieldDeclarationNode) {
    if (file == null || fieldDeclarationNode == null) {
      return Optional.empty();
    }
    if (!"field_declaration".equals(fieldDeclarationNode.getType())) {
      return Optional.empty();
    }
    try {
      TSNode fieldDeclaratorNode = fieldDeclarationNode.getChildByFieldName("declarator");
      if (fieldDeclaratorNode == null) {
        return Optional.empty();
      }
      TSNode fieldNameNode = fieldDeclaratorNode.getChildByFieldName("name");
      if (fieldNameNode == null) {
        return Optional.empty();
      }
      return Optional.of(fieldNameNode);
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  /**
   * Get field declaration type.
   *
   * @param file The TSFile to analyze.
   * @param fieldDeclarationNode THe field declaration node.
   * @return An Optional containing the main field type node, or empty if not found.
   */
  public Optional<TSNode> getFieldTypeNode(TSFile file, TSNode fieldDeclarationNode) {
    if (file == null || fieldDeclarationNode == null) {
      return Optional.empty();
    }
    // Validate that this is actually a field declaration node
    if (!"field_declaration".equals(fieldDeclarationNode.getType())) {
      return Optional.empty();
    }
    TSNode fieldTypeNode = fieldDeclarationNode.getChildByFieldName("type");
    if (fieldTypeNode == null) {
      return Optional.empty();
    }
    return Optional.of(fieldTypeNode);
  }

  /**
   * Checks if a class, identified by its fully qualified name, exists as a source file within the
   * project. It constructs the expected file path from the project root.
   *
   * @param projectRoot The root path of the project.
   * @param fullyQualifiedClassName The fully qualified name of the class to check.
   * @return {@code true} if the corresponding .java file exists in the project's main source
   *     directory, {@code false} otherwise.
   */
  public boolean isLocalClass(Path projectRoot, String fullyQualifiedClassName) {
    String relativePath = fullyQualifiedClassName.replace('.', '/') + ".java";
    Path sourcePath = projectRoot.resolve("src/main/java").resolve(relativePath);
    return Files.exists(sourcePath);
  }

  /**
   * Resolves the fully qualified name of a superclass using the context of a given file. It checks
   * for fully qualified names first, then scans import statements, considers implicit java.lang
   * imports, and finally assumes the same package if no other information is found.
   *
   * @param inheritanceService
   * @param file The TSFile where the superclass is referenced, providing context for imports and
   *     package.
   * @param superclassName The simple or partially qualified name of the superclass.
   * @return An {@link Optional} containing the fully qualified name of the superclass, or an empty
   *     Optional if it cannot be resolved.
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
   * Finds the simple name of the direct superclass from a class declaration node.
   *
   * @param file The TSFile containing the class declaration.
   * @param classDeclarationNode The tree-sitter node representing the class declaration.
   * @return An {@link Optional} containing the superclass name as a String, or an empty Optional if
   *     the class does not explicitly extend another class.
   */
  public Optional<String> getSuperclassName(TSFile file, TSNode classDeclarationNode) {
    List<TSNode> superclassNodes =
        file.query(
            classDeclarationNode,
            "(class_declaration superclass: (superclass (type_identifier) @superclass.name))");
    if (!superclassNodes.isEmpty()) {
      return Optional.of(file.getTextFromNode(superclassNodes.get(0)));
    }
    return Optional.empty();
  }

  /**
   * Checks if a class name corresponds to a common class in the {@code java.lang} package, which is
   * implicitly imported.
   *
   * @param className The simple name of the class.
   * @return {@code true} if the class is a known java.lang class, {@code false} otherwise.
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

  /**
   * Gets a list of all class annotation nodes.
   *
   * @param file The file the holds the class object of the query.
   * @param classDeclarationNode The class declaration node object of the query.
   * @return List of class annotation nodes.
   */
  public List<TSNode> getAllClassAnnotations(TSFile file, TSNode classDeclarationNode) {
    List<TSNode> markerAnnotationNodes =
        file.query(
            classDeclarationNode, "(class_declaration (modifiers (marker_annotation)) @annotation");
    List<TSNode> annotationNodes =
        file.query(classDeclarationNode, "(class_declaration (modifiers (annotation)) @annotation");
    annotationNodes.addAll(markerAnnotationNodes);
    return annotationNodes;
  }

  /**
   * Gets all annotation nodes for the main class in the file.
   *
   * @param file The TSFile containing the source code.
   * @return List of annotation nodes for the main class.
   */
  public List<TSNode> getAllClassAnnotations(TSFile file) {
    Optional<TSNode> mainClassNode = this.getMainClass(file);
    if (mainClassNode.isEmpty()) {
      return List.of();
    }
    return this.getAllClassAnnotations(file, mainClassNode.get());
  }
}
