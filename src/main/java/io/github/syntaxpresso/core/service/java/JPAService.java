package io.github.syntaxpresso.core.service.java;

import io.github.syntaxpresso.core.command.dto.CreateNewFileResponse;
import io.github.syntaxpresso.core.command.extra.JavaFileTemplate;
import io.github.syntaxpresso.core.common.DataTransferObject;
import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import java.nio.file.Path;
import java.util.Optional;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.treesitter.TSNode;

@Data
@RequiredArgsConstructor
public class JPAService {
  private final JavaLanguageService javaLanguageService;
  private final io.github.syntaxpresso.core.service.java.jpa.JPAService jpaOperations;

  public JPAService(JavaLanguageService javaLanguageService) {
    this.javaLanguageService = javaLanguageService;
    io.github.syntaxpresso.core.service.java.jpa.JPAEntityService jpaEntityService = 
        new io.github.syntaxpresso.core.service.java.jpa.JPAEntityService(this.javaLanguageService);
    this.jpaOperations =
        new io.github.syntaxpresso.core.service.java.jpa.JPAService(
            this.javaLanguageService.getImportDeclarationService(), jpaEntityService); // TODO: REMOVE DEPENDENCY
  }

  /**
   * Creates a JPA Repository interface for an entity class.
   *
   * @param cwd The current working directory.
   * @param entityFilePath The path to the entity file.
   * @return A DataTransferObject containing the new repository file information or error.
   */
  public DataTransferObject<CreateNewFileResponse> createJPARepository(
      Path cwd, Path entityFilePath) {
    if (!entityFilePath.toString().endsWith(".java")) {
      return DataTransferObject.error("File is not a .java file: " + entityFilePath);
    }
    try {
      TSFile entityFile = new TSFile(SupportedLanguage.JAVA, entityFilePath);
      Optional<String> className = entityFile.getFileNameWithoutExtension();
      if (className.isEmpty()) {
        return DataTransferObject.error("Unable to get entity class name");
      }
      Optional<String> packageName =
          this.javaLanguageService.getPackageDeclarationService().getPackageName(entityFile);
      if (packageName.isEmpty()) {
        return DataTransferObject.error("Unable to get package name");
      }
      Optional<TSNode> classDeclarationNode =
          this.javaLanguageService
              .getClassDeclarationService()
              .findClassByName(entityFile, className.get());
      if (classDeclarationNode.isEmpty()) {
        return DataTransferObject.error("No class declaration found in file");
      }
      if (!this.jpaOperations
          .getJpaEntityService()
          .isJPAEntity(entityFile)) {
        return DataTransferObject.error("Class is not a JPA entity (@Entity annotation not found)");
      }
      Optional<io.github.syntaxpresso.core.service.java.jpa.JPAService.FieldWithFile>
          idFieldWithFile =
              this.jpaOperations.findIdFieldInHierarchy(
                  this.javaLanguageService.getClassDeclarationService(),
                  cwd,
                  entityFile,
                  classDeclarationNode.get());
      if (idFieldWithFile.isEmpty()) {
        return DataTransferObject.error(
            "No @Id field found in entity hierarchy. Note: Only local project classes are currently"
                + " supported.");
      }
      Optional<String> idType =
          this.jpaOperations.extractFieldType(
              idFieldWithFile.get().file, idFieldWithFile.get().field);
      if (idType.isEmpty()) {
        return DataTransferObject.error("Unable to determine @Id field type");
      }
      String repositoryName = className.get() + "Repository";

      // Use JavaLanguageService to create the file
      String repositoryClassName = repositoryName.trim();
      repositoryClassName = com.google.common.io.Files.getNameWithoutExtension(repositoryClassName);
      String template =
          JavaFileTemplate.INTERFACE.getSourceContent(packageName.get(), repositoryClassName);
      TSFile file = new TSFile(SupportedLanguage.JAVA, template);

      // Find the file path using JavaLanguageService path helper
      final String srcDirName = "src/main/java";
      Optional<Path> sourceDirOptional =
          this.javaLanguageService.getPathHelper().findDirectoryRecursively(cwd, srcDirName);
      Path sourceDir;
      if (sourceDirOptional.isPresent()) {
        sourceDir = sourceDirOptional.get();
      } else {
        sourceDir = cwd.resolve(srcDirName);
      }
      Path packageAsPath = Path.of(packageName.get().replace('.', '/'));
      Path fullPackageDir = sourceDir.resolve(packageAsPath);
      java.nio.file.Files.createDirectories(fullPackageDir);

      Path targetPath = fullPackageDir.resolve(repositoryClassName + ".java");
      if (java.nio.file.Files.exists(targetPath)) {
        return DataTransferObject.error("File already exists: " + targetPath.toString());
      }
      file.saveAs(targetPath);
      DataTransferObject<CreateNewFileResponse> createResult =
          DataTransferObject.success(
              CreateNewFileResponse.builder().filePath(file.getFile().getAbsolutePath()).build());
      if (!createResult.getSucceed()) {
        return createResult;
      }
      TSFile repositoryFile =
          new TSFile(SupportedLanguage.JAVA, Path.of(createResult.getData().getFilePath()));
      this.jpaOperations.configureRepositoryFile(
          repositoryFile, className.get(), idType.get(), packageName.get());
      repositoryFile.save();
      return createResult;
    } catch (Exception e) {
      return DataTransferObject.error("Failed to create repository: " + e.getMessage());
    }
  }
}
