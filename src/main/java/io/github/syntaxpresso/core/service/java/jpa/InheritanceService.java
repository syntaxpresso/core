package io.github.syntaxpresso.core.service.java.jpa;

import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import io.github.syntaxpresso.core.service.java.language.ClassDeclarationService;
import io.github.syntaxpresso.core.service.java.language.ImportDeclarationService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.treesitter.TSNode;

@RequiredArgsConstructor
public class InheritanceService {
  private final ClassDeclarationService classDeclarationService;
  private final ImportDeclarationService importDeclarationService;

  /**
   * A container class to hold a {@link TSNode} representing a field and its associated {@link
   * TSFile}.
   */
  public static class FieldWithFile {
    public final TSNode field;
    public final TSFile file;

    public FieldWithFile(TSNode field, TSFile file) {
      this.field = field;
      this.file = file;
    }
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
   * Resolves the fully qualified name of a superclass using the context of a given file. It checks
   * for fully qualified names first, then scans import statements, considers implicit java.lang
   * imports, and finally assumes the same package if no other information is found.
   *
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
    // Check imports for the superclass
    List<TSNode> importNodes = this.importDeclarationService.findAllImportDeclarations(file);
    for (TSNode importNode : importNodes) {
      String importStatement = file.getTextFromNode(importNode);
      String importPath = importStatement.replace("import", "").replace(";", "").trim();
      if (importPath.endsWith("." + superclassName)) {
        return Optional.of(importPath);
      }
    }
    // Check if it's in java.lang (implicit imports)
    if (isJavaLangClass(superclassName)) {
      return Optional.of("java.lang." + superclassName);
    }
    // If not found in imports, assume it's in the same package
    Optional<String> packageName =
        this.importDeclarationService.getPackageDeclarationService().getPackageName(file);
    if (packageName.isPresent()) {
      return Optional.of(packageName.get() + "." + superclassName);
    }
    return Optional.empty();
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
   * Recursively finds a field annotated with {@code @Id} within the class hierarchy, starting from
   * the given class declaration. The search proceeds up the inheritance chain until an {@code @Id}
   * field is found or the top of the hierarchy is reached.
   *
   * @param projectRoot The root path of the project, used to locate source files for superclasses.
   * @param file The TSFile of the class to begin the search from.
   * @param classDeclarationNode The tree-sitter node of the class to begin the search from.
   * @return An {@link Optional} containing a {@link FieldWithFile} record for the {@code @Id} field
   *     if found, or an empty Optional otherwise.
   */
  public Optional<FieldWithFile> findIdFieldInHierarchy(
      Path projectRoot, TSFile file, TSNode classDeclarationNode) {
    // First check current class
    Optional<TSNode> idField = findIdFieldInClass(file, classDeclarationNode);
    if (idField.isPresent()) {
      return Optional.of(new FieldWithFile(idField.get(), file));
    }
    // Check superclass
    Optional<String> superclassName = getSuperclassName(file, classDeclarationNode);
    if (superclassName.isEmpty()) {
      return Optional.empty(); // No superclass, end recursion
    }
    Optional<String> fullyQualifiedSuperclass =
        getFullyQualifiedSuperclass(file, superclassName.get());
    if (fullyQualifiedSuperclass.isEmpty()) {
      return Optional.empty();
    }
    // Check if superclass is local
    if (isLocalClass(projectRoot, fullyQualifiedSuperclass.get())) {
      return findIdFieldInLocalSuperclass(projectRoot, fullyQualifiedSuperclass.get());
    } else {
      return findIdFieldInExternalSuperclass(projectRoot, fullyQualifiedSuperclass.get());
    }
  }

  /**
   * Finds a field annotated with {@code @Id} directly within the given class declaration node. This
   * method does not search the class hierarchy.
   *
   * @param file The TSFile containing the class.
   * @param classDeclarationNode The node of the class to search within.
   * @return An {@link Optional} containing the {@link TSNode} of the field if found, otherwise an
   *     empty Optional.
   */
  private Optional<TSNode> findIdFieldInClass(TSFile file, TSNode classDeclarationNode) {
    List<TSNode> allClassFields =
        this.classDeclarationService.getClassFields(file, classDeclarationNode);
    for (TSNode field : allClassFields) {
      if (hasIdAnnotation(file, field)) {
        return Optional.of(field);
      }
    }
    return Optional.empty();
  }

  /**
   * Continues the recursive search for an {@code @Id} field in a superclass that is part of the
   * local project source code.
   *
   * @param projectRoot The root path of the project.
   * @param fullyQualifiedClassName The fully qualified name of the superclass to inspect.
   * @return An {@link Optional} containing the {@link FieldWithFile} if an {@code @Id} is found in
   *     the superclass's hierarchy, otherwise an empty Optional.
   */
  private Optional<FieldWithFile> findIdFieldInLocalSuperclass(
      Path projectRoot, String fullyQualifiedClassName) {
    try {
      String relativePath = fullyQualifiedClassName.replace('.', '/') + ".java";
      Path superclassPath = projectRoot.resolve("src/main/java").resolve(relativePath);
      TSFile superclassFile = new TSFile(SupportedLanguage.JAVA, superclassPath);
      String className =
          fullyQualifiedClassName.substring(fullyQualifiedClassName.lastIndexOf('.') + 1);
      Optional<TSNode> superclassDeclaration =
          this.classDeclarationService.findClassByName(superclassFile, className);
      if (superclassDeclaration.isPresent()) {
        return findIdFieldInHierarchy(projectRoot, superclassFile, superclassDeclaration.get());
      }
    } catch (Exception e) {
      // Continue silently
    }
    return Optional.empty();
  }

  /**
   * Placeholder for finding an {@code @Id} field in an external (e.g., library) superclass. This
   * functionality is not currently supported.
   *
   * @param projectRoot The root path of the project.
   * @param fullyQualifiedClassName The fully qualified name of the external superclass.
   * @return An empty {@link Optional} as this feature is not implemented.
   */
  private Optional<FieldWithFile> findIdFieldInExternalSuperclass(
      Path projectRoot, String fullyQualifiedClassName) {
    // For now, we only support finding @Id fields in local project files
    return Optional.empty();
  }

  /**
   * Checks if a given field node has an {@code @Id} annotation.
   *
   * @param file The TSFile containing the field.
   * @param fieldNode The {@link TSNode} of the field to check.
   * @return {@code true} if the field is annotated with {@code @Id}, {@code false} otherwise.
   */
  private boolean hasIdAnnotation(TSFile file, TSNode fieldNode) {
    List<TSNode> annotations =
        file.query(fieldNode, "(marker_annotation name: (identifier) @annotation.name)");
    for (TSNode annotation : annotations) {
      String annotationText = file.getTextFromNode(annotation);
      if ("Id".equals(annotationText)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Checks if a class name corresponds to a common class in the {@code java.lang} package, which is
   * implicitly imported.
   *
   * @param className The simple name of the class.
   * @return {@code true} if the class is a known java.lang class, {@code false} otherwise.
   */
  private boolean isJavaLangClass(String className) {
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
