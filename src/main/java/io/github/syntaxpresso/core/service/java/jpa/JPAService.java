package io.github.syntaxpresso.core.service.java.jpa;

import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import io.github.syntaxpresso.core.service.java.language.ClassDeclarationService;
import io.github.syntaxpresso.core.service.java.language.ImportDeclarationService;
import io.github.syntaxpresso.core.service.java.language.JavaBasicType;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.treesitter.TSNode;

@RequiredArgsConstructor
public class JPAService {
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
   * Checks if a class is a JPA entity by looking for @Entity annotation.
   *
   * @param file The TSFile containing the class.
   * @param mainClassDeclarationNode The class declaration node.
   * @return True if the class is a JPA entity, false otherwise.
   */
  public boolean isJPAEntity(TSFile file, TSNode mainClassDeclarationNode) {
    List<TSNode> annotations =
        file.query(
            mainClassDeclarationNode, "(marker_annotation name: (identifier) @annotation.name)");
    for (TSNode annotation : annotations) {
      String annotationName = file.getTextFromNode(annotation);
      if ("jakarta.persistence.Entity".equals(annotationName)) {
        return true;
      }
      if ("Entity".equals(annotationName)) {
        List<TSNode> importNodes = this.importDeclarationService.findAllImportDeclarations(file);
        for (TSNode importNode : importNodes) {
          String importName = file.getTextFromNode(importNode);
          if (importName.equals("jakarta.persistence.Entity")
              || importName.equals("jakarta.persistence")) {
            return true;
          }
        }
      }
    }
    return false;
  }

  /**
   * Extracts the type of a field declaration.
   *
   * @param file The TSFile containing the field.
   * @param fieldNode The field declaration node.
   * @return An optional containing the field type, or empty if not found.
   */
  public Optional<String> extractFieldType(TSFile file, TSNode fieldNode) {
    if (!"field_declaration".equals(fieldNode.getType())) {
      return Optional.empty();
    }
    for (int i = 0; i < fieldNode.getChildCount(); i++) {
      TSNode child = fieldNode.getChild(i);
      String childType = child.getType();
      if ("type_identifier".equals(childType)
          || "generic_type".equals(childType)
          || "integral_type".equals(childType)
          || "floating_point_type".equals(childType)
          || "boolean_type".equals(childType)
          || "array_type".equals(childType)) {
        return Optional.of(file.getTextFromNode(child));
      }
    }
    return Optional.empty();
  }

  /**
   * Configures the repository file with JPA imports and interface extension.
   *
   * @param repositoryFile The repository TSFile to configure.
   * @param entityClassName The entity class name.
   * @param idType The ID field type.
   * @param entityPackage The entity package name.
   */
  public void configureRepositoryFile(
      TSFile repositoryFile, String entityClassName, String idType, String entityPackage) {
    this.importDeclarationService.addImport(
        repositoryFile, "org.springframework.data.jpa.repository.JpaRepository");
    Optional<String> repositoryPackage =
        this.importDeclarationService.getPackageDeclarationService().getPackageName(repositoryFile);
    if (repositoryPackage.isPresent() && !repositoryPackage.get().equals(entityPackage)) {
      this.importDeclarationService.addImport(
          repositoryFile, entityPackage + "." + entityClassName);
    }
    this.addImportForIdType(repositoryFile, idType);
    this.addExtendsClause(repositoryFile, entityClassName, idType);
  }

  /**
   * Adds the appropriate import for the ID field type if needed.
   *
   * @param repositoryFile The repository TSFile to modify.
   * @param idType The ID field type.
   */
  private void addImportForIdType(TSFile repositoryFile, String idType) {
    Optional<JavaBasicType> basicType = JavaBasicType.fromTypeName(idType);
    if (basicType.isPresent() && basicType.get().needsImport()) {
      this.importDeclarationService.addImport(
          repositoryFile, basicType.get().getFullyQualifiedName());
    }
  }

  /**
   * Adds extends JpaRepository<EntityClass, IdType> to the repository interface.
   *
   * @param repositoryFile The repository TSFile to modify.
   * @param entityClassName The entity class name.
   * @param idType The ID field type.
   */
  private void addExtendsClause(TSFile repositoryFile, String entityClassName, String idType) {
    List<TSNode> interfaceNodes = repositoryFile.query("(interface_declaration) @interface");
    if (interfaceNodes.isEmpty()) {
      return;
    }
    TSNode interfaceNode = interfaceNodes.get(0);
    TSNode nameNode = interfaceNode.getChildByFieldName("name");
    if (nameNode == null) {
      return;
    }
    String extendsClause = String.format(" extends JpaRepository<%s, %s>", entityClassName, idType);
    repositoryFile.insertTextAfterNode(nameNode, extendsClause);
  }

  /**
   * Recursively finds a field annotated with {@code @Id} within the class hierarchy, starting from
   * the given class declaration. The search proceeds up the inheritance chain until an {@code @Id}
   * field is found or the top of the hierarchy is reached.
   *
   * @param inheritanceService TODO
   * @param projectRoot The root path of the project, used to locate source files for superclasses.
   * @param file The TSFile of the class to begin the search from.
   * @param classDeclarationNode The tree-sitter node of the class to begin the search from.
   * @return An {@link Optional} containing a {@link FieldWithFile} record for the {@code @Id} field
   *     if found, or an empty Optional otherwise.
   */
  public Optional<FieldWithFile> findIdFieldInHierarchy(
      ClassDeclarationService classDeclarationService,
      Path projectRoot,
      TSFile file,
      TSNode classDeclarationNode) {
    // First check current class
    Optional<TSNode> idField = this.findIdFieldInClass(classDeclarationService, file, classDeclarationNode);
    if (idField.isPresent()) {
      return Optional.of(new FieldWithFile(idField.get(), file));
    }
    // Check superclass
    Optional<String> superclassName = classDeclarationService.getSuperclassName(file, classDeclarationNode);
    if (superclassName.isEmpty()) {
      return Optional.empty(); // No superclass, end recursion
    }
    Optional<String> fullyQualifiedSuperclass =
        classDeclarationService.getFullyQualifiedSuperclass(file, superclassName.get());
    if (fullyQualifiedSuperclass.isEmpty()) {
      return Optional.empty();
    }
    // Check if superclass is local
    if (classDeclarationService.isLocalClass(projectRoot, fullyQualifiedSuperclass.get())) {
      return this.findIdFieldInLocalSuperclass(classDeclarationService, projectRoot, fullyQualifiedSuperclass.get());
    } else {
      return this.findIdFieldInExternalSuperclass(projectRoot, fullyQualifiedSuperclass.get());
    }
  }

  /**
   * Finds a field annotated with {@code @Id} directly within the given class declaration node. This
   * method does not search the class hierarchy.
   *
   * @param inheritanceService TODO
   * @param file The TSFile containing the class.
   * @param classDeclarationNode The node of the class to search within.
   * @return An {@link Optional} containing the {@link TSNode} of the field if found, otherwise an
   *     empty Optional.
   */
  Optional<TSNode> findIdFieldInClass(
      ClassDeclarationService classDeclarationService, TSFile file, TSNode classDeclarationNode) {
    List<TSNode> allClassFields = classDeclarationService.getClassFields(file, classDeclarationNode);
    for (TSNode field : allClassFields) {
      if (this.hasIdAnnotation(file, field)) {
        return Optional.of(field);
      }
    }
    return Optional.empty();
  }

  /**
   * Continues the recursive search for an {@code @Id} field in a superclass that is part of the
   * local project source code.
   *
   * @param inheritanceService TODO
   * @param projectRoot The root path of the project.
   * @param fullyQualifiedClassName The fully qualified name of the superclass to inspect.
   * @return An {@link Optional} containing the {@link FieldWithFile} if an {@code @Id} is found in
   *     the superclass's hierarchy, otherwise an empty Optional.
   */
  Optional<FieldWithFile> findIdFieldInLocalSuperclass(
      ClassDeclarationService classDeclarationService, Path projectRoot, String fullyQualifiedClassName) {
    try {
      String relativePath = fullyQualifiedClassName.replace('.', '/') + ".java";
      Path superclassPath = projectRoot.resolve("src/main/java").resolve(relativePath);
      TSFile superclassFile = new TSFile(SupportedLanguage.JAVA, superclassPath);
      String className =
          fullyQualifiedClassName.substring(fullyQualifiedClassName.lastIndexOf('.') + 1);
      Optional<TSNode> superclassDeclaration =
          classDeclarationService.findClassByName(superclassFile, className);
      if (superclassDeclaration.isPresent()) {
        return findIdFieldInHierarchy(
            classDeclarationService, projectRoot, superclassFile, superclassDeclaration.get());
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
  Optional<FieldWithFile> findIdFieldInExternalSuperclass(
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
  boolean hasIdAnnotation(TSFile file, TSNode fieldNode) {
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
}
