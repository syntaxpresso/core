package io.github.syntaxpresso.core.service.java.jpa;

import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.service.java.language.ImportDeclarationService;
import io.github.syntaxpresso.core.service.java.language.JavaBasicType;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.treesitter.TSNode;

@RequiredArgsConstructor
public class JPAService {
  private final ImportDeclarationService importDeclarationService;

  /**
   * Checks if a class is a JPA entity by looking for @Entity annotation.
   *
   * @param file The TSFile containing the class.
   * @param classDeclarationNode The class declaration node.
   * @return True if the class is a JPA entity, false otherwise.
   */
  public boolean isJPAEntity(TSFile file, TSNode classDeclarationNode) {
    List<TSNode> annotations =
        file.query(classDeclarationNode, "(marker_annotation name: (identifier) @annotation.name)");
    for (TSNode annotation : annotations) {
      String name = file.getTextFromNode(annotation);
      if ("Entity".equals(name)) {
        return true;
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
}
