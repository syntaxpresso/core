package io.github.syntaxpresso.core.service.java.extra;

import io.github.syntaxpresso.core.command.java.dto.CreateNewJavaFileResponse;
import io.github.syntaxpresso.core.command.java.extra.JavaFileTemplate;
import io.github.syntaxpresso.core.command.java.extra.SourceDirectoryType;
import io.github.syntaxpresso.core.common.DataTransferObject;
import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import io.github.syntaxpresso.core.service.java.JavaService;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.treesitter.TSNode;

@RequiredArgsConstructor
public class JPAService {
  private final JavaService javaService;
  private final ClassDeclarationService classDeclarationService;
  private final InheritanceService inheritanceService;
  private final ImportDeclarationService importDeclarationService;

  /**
   * Creates a JPA Repository interface for an entity class.
   */
  public DataTransferObject<CreateNewJavaFileResponse> createJPARepository(Path cwd, Path entityFilePath) {
    if (!entityFilePath.toString().endsWith(".java")) {
      return DataTransferObject.error("File is not a .java file: " + entityFilePath);
    }

    try {
      TSFile entityFile = new TSFile(SupportedLanguage.JAVA, entityFilePath);
      
      // Get entity class name
      Optional<String> className = entityFile.getFileNameWithoutExtension();
      if (className.isEmpty()) {
        return DataTransferObject.error("Unable to get entity class name");
      }

      // Get package name
      Optional<String> packageName = this.importDeclarationService.getPackageDeclarationService().getPackageName(entityFile);
      if (packageName.isEmpty()) {
        return DataTransferObject.error("Unable to get package name");
      }

      // Find class declaration
      Optional<TSNode> classDeclarationNode = this.classDeclarationService.findClassByName(entityFile, className.get());
      if (classDeclarationNode.isEmpty()) {
        return DataTransferObject.error("No class declaration found in file");
      }

      // Check if it's a JPA entity
      if (!this.isJPAEntity(entityFile, classDeclarationNode.get())) {
        return DataTransferObject.error("Class is not a JPA entity (@Entity annotation not found)");
      }

      // Find @Id field in inheritance hierarchy (local files only)
      Optional<InheritanceService.FieldWithFile> idFieldWithFile = 
          this.inheritanceService.findIdFieldInHierarchy(cwd, entityFile, classDeclarationNode.get());
      if (idFieldWithFile.isEmpty()) {
        return DataTransferObject.error("No @Id field found in entity hierarchy. Note: Only local project classes are currently supported.");
      }

      // Get ID field type
      Optional<String> idType = this.extractFieldType(idFieldWithFile.get().file, idFieldWithFile.get().field);
      if (idType.isEmpty()) {
        return DataTransferObject.error("Unable to determine @Id field type");
      }

      // Create repository interface
      String repositoryName = className.get() + "Repository";
      DataTransferObject<CreateNewJavaFileResponse> createResult = this.javaService.createNewFile(
          cwd, packageName.get(), repositoryName, JavaFileTemplate.INTERFACE, SourceDirectoryType.MAIN);
      
      if (!createResult.getSucceed()) {
        return createResult;
      }

      // Configure the repository file with JPA imports and interface extension
      TSFile repositoryFile = new TSFile(SupportedLanguage.JAVA, Path.of(createResult.getData().getFilePath()));
      this.configureRepositoryFile(repositoryFile, className.get(), idType.get(), packageName.get());
      repositoryFile.save();
      
      return createResult;
      
    } catch (Exception e) {
      return DataTransferObject.error("Failed to create repository: " + e.getMessage());
    }
  }

  /**
   * Checks if a class is a JPA entity by looking for @Entity annotation.
   */
  public boolean isJPAEntity(TSFile file, TSNode classDeclarationNode) {
    List<TSNode> annotations = file.query(classDeclarationNode, "(marker_annotation name: (identifier) @annotation.name)");
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
   */
  private Optional<String> extractFieldType(TSFile file, TSNode fieldNode) {
    if (!"field_declaration".equals(fieldNode.getType())) {
      return Optional.empty();
    }
    
    // Find type node among children
    for (int i = 0; i < fieldNode.getChildCount(); i++) {
      TSNode child = fieldNode.getChild(i);
      String childType = child.getType();
      
      if ("type_identifier".equals(childType) || "generic_type".equals(childType) ||
          "integral_type".equals(childType) || "floating_point_type".equals(childType) || 
          "boolean_type".equals(childType) || "array_type".equals(childType)) {
        return Optional.of(file.getTextFromNode(child));
      }
    }
    
    return Optional.empty();
  }

  /**
   * Configures the repository file with JPA imports and interface extension.
   */
  private void configureRepositoryFile(TSFile repositoryFile, String entityClassName, String idType, String entityPackage) {
    // Add JpaRepository import
    this.importDeclarationService.addImport(repositoryFile, "org.springframework.data.jpa.repository.JpaRepository");
    
    // Add entity import if needed (if in different package)
    Optional<String> repositoryPackage = this.importDeclarationService.getPackageDeclarationService().getPackageName(repositoryFile);
    if (repositoryPackage.isPresent() && !repositoryPackage.get().equals(entityPackage)) {
      this.importDeclarationService.addImport(repositoryFile, entityPackage + "." + entityClassName);
    }
    
    // Add ID type import if needed
    if ("UUID".equals(idType)) {
      this.importDeclarationService.addImport(repositoryFile, "java.util.UUID");
    }
    
    // TODO: Update interface declaration to extend JpaRepository<Entity, IdType>
    // This would require complex tree modification, so leaving as basic interface for now
  }
}