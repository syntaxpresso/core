package io.github.syntaxpresso.core.service.java.extra;

import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
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

  /** Container class to hold both the field node and its associated file. */
  public static class FieldWithFile {
    public final TSNode field;
    public final TSFile file;

    public FieldWithFile(TSNode field, TSFile file) {
      this.field = field;
      this.file = file;
    }
  }

  /** Finds the superclass name from a class declaration node. */
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

  /** Resolves the fully qualified name of a superclass using imports. */
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

  /** Checks if a superclass exists in the project's source code. */
  public boolean isLocalClass(Path projectRoot, String fullyQualifiedClassName) {
    String relativePath = fullyQualifiedClassName.replace('.', '/') + ".java";
    Path sourcePath = projectRoot.resolve("src/main/java").resolve(relativePath);
    return Files.exists(sourcePath);
  }

  /** Recursively finds a field with @Id annotation in the class hierarchy. */
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

  /** Finds @Id field in the current class. */
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

  /** Finds @Id field in a local superclass. */
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

  /** Finds @Id field in an external superclass - returns empty (not supported for now). */
  private Optional<FieldWithFile> findIdFieldInExternalSuperclass(
      Path projectRoot, String fullyQualifiedClassName) {
    // For now, we only support finding @Id fields in local project files
    return Optional.empty();
  }

  /** Checks if a field has @Id annotation. */
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

  /** Checks if a class is in java.lang package. */
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

