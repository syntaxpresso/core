package io.github.syntaxpresso.core.service.java.command;

import com.google.common.base.Strings;
import io.github.syntaxpresso.core.command.dto.CreateJPARepositoryResponse;
import io.github.syntaxpresso.core.command.dto.CreateNewFileResponse;
import io.github.syntaxpresso.core.command.extra.JavaSourceDirectoryType;
import io.github.syntaxpresso.core.common.DataTransferObject;
import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.common.extra.SupportedIDE;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import io.github.syntaxpresso.core.service.java.JavaLanguageService;
import io.github.syntaxpresso.core.service.java.command.extra.IdFieldSearchResult;
import io.github.syntaxpresso.core.service.java.command.extra.JPARepositoryData;
import io.github.syntaxpresso.core.service.java.command.extra.PrepareDataResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.treesitter.TSNode;

@RequiredArgsConstructor
public class CreateJPARepositoryCommandService {
  private final JavaLanguageService javaLanguageService;

  private JPARepositoryData buildJPARepositoryData(
      Path cwd, String entityType, String entityIdType, String entityPackageName) {
    return JPARepositoryData.builder()
        .cwd(cwd)
        .packageName(entityPackageName)
        .entityType(entityType)
        .entityIdType(entityIdType)
        .build();
  }

  private Optional<TSNode> getIdFieldFromEntity(TSFile tsFile, TSNode publicClassNode) {
    List<TSNode> publicClassFieldNodes =
        this.javaLanguageService
            .getClassDeclarationService()
            .getFieldDeclarationService()
            .getAllFieldDeclarationNodes(tsFile, publicClassNode);
    for (TSNode classFieldNode : publicClassFieldNodes) {
      Optional<TSNode> idFieldAnnotation =
          this.javaLanguageService
              .getAnnotationService()
              .findAnnotationByName(tsFile, classFieldNode, "Id");
      if (idFieldAnnotation.isPresent()) {
        return Optional.of(classFieldNode);
      }
    }
    return Optional.empty();
  }

  private Optional<TSFile> findSuperclassFile(Path cwd, TSFile entityFile, TSNode publicClassNode) {
    Optional<TSNode> superClassNameNode =
        this.javaLanguageService
            .getClassDeclarationService()
            .getClassDeclarationSuperclassNameNode(entityFile, publicClassNode);
    if (superClassNameNode.isEmpty()) {
      return Optional.empty();
    }
    String superClassName = entityFile.getTextFromNode(superClassNameNode.get());
    if (Strings.isNullOrEmpty(superClassName)) {
      return Optional.empty();
    }
    List<TSFile> allJavaFiles = this.javaLanguageService.getAllJavaFilesFromCwd(cwd);
    for (TSFile tsFile : allJavaFiles) {
      Optional<String> fileName = tsFile.getFileNameWithoutExtension();
      if (fileName.isEmpty()) {
        continue;
      }
      if (fileName.get().equals(superClassName)) {
        return Optional.of(tsFile);
      }
    }
    return Optional.empty();
  }

  private IdFieldSearchResult findIdFieldRecursively(
      Path cwd, TSFile tsFile, TSNode publicClassNode) {
    return this.findIdFieldRecursively(cwd, tsFile, publicClassNode, null);
  }

  private IdFieldSearchResult findIdFieldRecursively(
      Path cwd, TSFile tsFile, TSNode publicClassNode, TSFile externalSuperclassFile) {
    Optional<TSNode> idFieldNode = this.getIdFieldFromEntity(tsFile, publicClassNode);
    if (idFieldNode.isPresent()) {
      return IdFieldSearchResult.found(tsFile, idFieldNode.get());
    }
    Optional<TSNode> superClassNameNode =
        this.javaLanguageService
            .getClassDeclarationService()
            .getClassDeclarationSuperclassNameNode(tsFile, publicClassNode);
    if (superClassNameNode.isEmpty()) {
      return IdFieldSearchResult.notFound();
    }
    String superClassName = tsFile.getTextFromNode(superClassNameNode.get());
    if (Strings.isNullOrEmpty(superClassName)) {
      return IdFieldSearchResult.notFound();
    }

    // First check if we have an external superclass file for this superclass
    if (externalSuperclassFile != null) {
      // For external source, we can't use getPublicClass() as it requires a filename
      // Instead, find any class declaration in the external source
      Optional<TSNode> externalClassNode = this.findFirstClassDeclaration(externalSuperclassFile);
      if (externalClassNode.isPresent()) {
        Optional<String> externalClassName = this.extractEntityType(externalSuperclassFile, externalClassNode.get());
        if (externalClassName.isPresent() && externalClassName.get().equals(superClassName)) {
          // We found the matching external superclass, search within it
          return this.findIdFieldRecursively(cwd, externalSuperclassFile, externalClassNode.get(), null);
        }
      }
      // If we have external source but it doesn't match the expected class name, 
      // don't ask for the same superclass again - treat as not found
      return IdFieldSearchResult.notFound();
    }

    Optional<TSFile> superClassFile = this.findSuperclassFile(cwd, tsFile, publicClassNode);
    if (superClassFile.isEmpty()) {
      return IdFieldSearchResult.missingSuperclass(superClassName);
    }
    Optional<TSNode> superClassPublicNode =
        this.javaLanguageService.getClassDeclarationService().getPublicClass(superClassFile.get());
    if (superClassPublicNode.isEmpty()) {
      return IdFieldSearchResult.notFound();
    }
    return this.findIdFieldRecursively(
        cwd, superClassFile.get(), superClassPublicNode.get(), externalSuperclassFile);
  }

  /**
   * Finds the first class declaration in a TSFile, regardless of its name or visibility.
   * This is useful for external source code where we don't have a filename to match against.
   */
  private Optional<TSNode> findFirstClassDeclaration(TSFile tsFile) {
    if (tsFile == null || tsFile.getTree() == null) {
      return Optional.empty();
    }
    return tsFile.query("(class_declaration) @class").returning("class").execute().firstNodeOptional();
  }

  private Optional<String> extractEntityType(TSFile tsFile, TSNode publicClassNode) {
    Optional<TSNode> classNameNode =
        this.javaLanguageService
            .getClassDeclarationService()
            .getClassDeclarationNameNode(tsFile, publicClassNode);

    if (classNameNode.isEmpty()) {
      return Optional.empty();
    }

    String className = tsFile.getTextFromNode(classNameNode.get());
    return Strings.isNullOrEmpty(className) ? Optional.empty() : Optional.of(className);
  }

  private Optional<String> extractIdType(TSFile tsFile, TSNode idFieldNode) {
    Optional<TSNode> fieldTypeNode =
        this.javaLanguageService
            .getClassDeclarationService()
            .getFieldDeclarationService()
            .getFieldDeclarationFullTypeNode(tsFile, idFieldNode);

    if (fieldTypeNode.isEmpty()) {
      return Optional.empty();
    }

    String idType = tsFile.getTextFromNode(fieldTypeNode.get());
    return Strings.isNullOrEmpty(idType) ? Optional.empty() : Optional.of(idType);
  }

  private Optional<String> extractPackageName(TSFile tsFile) {
    Optional<TSNode> packageDeclarationNode =
        this.javaLanguageService.getPackageDeclarationService().getPackageDeclarationNode(tsFile);
    if (packageDeclarationNode.isEmpty()) {
      return Optional.empty();
    }
    Optional<TSNode> packageScopeNode =
        this.javaLanguageService
            .getPackageDeclarationService()
            .getPackageScopeNode(tsFile, packageDeclarationNode.get());
    if (packageScopeNode.isEmpty()) {
      return Optional.empty();
    }
    String packageName = tsFile.getTextFromNode(packageScopeNode.get());
    return Strings.isNullOrEmpty(packageName) ? Optional.empty() : Optional.of(packageName);
  }

  private DataTransferObject<CreateNewFileResponse> createRepositoryFile(
      JPARepositoryData jpaRepositoryData) {
    if (Strings.isNullOrEmpty(jpaRepositoryData.getPackageName())
        || Strings.isNullOrEmpty(jpaRepositoryData.getEntityType())
        || Strings.isNullOrEmpty(jpaRepositoryData.getEntityIdType())) {
      return DataTransferObject.error("Entity type, ID type, and package name are required.");
    }
    String repositoryName = jpaRepositoryData.getEntityType() + "Repository";
    String template =
        String.format(
            "package %s;\n\nimport org.springframework.data.jpa.repository.JpaRepository;\nimport"
                + " org.springframework.stereotype.Repository;\n\n@Repository\npublic interface"
                + " %s extends JpaRepository<%s, %s> {}",
            jpaRepositoryData.getPackageName(),
            repositoryName,
            jpaRepositoryData.getEntityType(),
            jpaRepositoryData.getEntityIdType());
    try {
      TSFile file = new TSFile(SupportedLanguage.JAVA, template);
      Optional<Path> filePath =
          this.javaLanguageService
              .getPackageDeclarationService()
              .getFilePathFromPackageScope(
                  jpaRepositoryData.getCwd(),
                  jpaRepositoryData.getPackageName(),
                  JavaSourceDirectoryType.MAIN);
      if (filePath.isEmpty()) {
        return DataTransferObject.error("Package directory could not be determined.");
      }
      Path targetPath =
          filePath.get().resolve(repositoryName + SupportedLanguage.JAVA.getFileExtension());
      if (Files.exists(targetPath)) {
        return DataTransferObject.error("Repository file already exists: " + targetPath.toString());
      }
      file.saveAs(targetPath);
      CreateNewFileResponse response =
          CreateNewFileResponse.builder().filePath(file.getFile().getAbsolutePath()).build();
      return DataTransferObject.success(response);
    } catch (IOException e) {
      return DataTransferObject.error("Failed to create repository file: " + e.getMessage());
    } catch (Exception e) {
      return DataTransferObject.error("Unexpected error occurred: " + e.getMessage());
    }
  }

  public PrepareDataResult prepareData(TSFile tsFile, Path cwd) {
    return this.prepareData(tsFile, cwd, null);
  }

  public PrepareDataResult prepareData(TSFile tsFile, Path cwd, String superclassSource) {
    Optional<TSNode> publicClassNode =
        this.javaLanguageService.getClassDeclarationService().getPublicClass(tsFile);
    if (publicClassNode.isEmpty()) {
      return PrepareDataResult.error("No public class found in the entity file.");
    }
    Optional<String> packageName = this.extractPackageName(tsFile);
    if (packageName.isEmpty()) {
      return PrepareDataResult.error("Package name could not be extracted from the entity file.");
    }
    Optional<String> entityType = this.extractEntityType(tsFile, publicClassNode.get());
    if (entityType.isEmpty()) {
      return PrepareDataResult.error("Entity type could not be extracted from the entity file.");
    }
    TSFile externalSuperclassFile = null;
    if (!Strings.isNullOrEmpty(superclassSource)) {
      externalSuperclassFile = new TSFile(SupportedLanguage.JAVA, superclassSource);
    }
    IdFieldSearchResult searchResult =
        this.findIdFieldRecursively(cwd, tsFile, publicClassNode.get(), externalSuperclassFile);
    if (searchResult.isFound()) {
      // Id field found - extract ID type and create JPARepositoryData
      Optional<String> idType =
          this.extractIdType(searchResult.getTsFile(), searchResult.getIdFieldNode());
      if (idType.isEmpty()) {
        return PrepareDataResult.error("ID field type could not be extracted.");
      }
      JPARepositoryData repositoryData =
          this.buildJPARepositoryData(cwd, entityType.get(), idType.get(), packageName.get());
      return PrepareDataResult.success(repositoryData);
    } else if (searchResult.hasMissingSuperclass()) {
      // Superclass exists but file not found - return response requiring symbol source
      CreateJPARepositoryResponse response =
          CreateJPARepositoryResponse.builder()
              .filePath(tsFile.getFile().getAbsolutePath())
              .symbol(searchResult.getMissingSuperclassName())
              .requiresSymbolSource(true)
              .build();
      return PrepareDataResult.requiresSymbolSource(response);
    } else {
      // No Id field found in entire hierarchy
      return PrepareDataResult.error(
          "No @Id field found in the entity or its superclass hierarchy.");
    }
  }

  public void getFileFromSource(Path cwd, String sourceCode) {
    TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
    this.prepareData(tsFile, cwd);
  }

  public DataTransferObject<CreateJPARepositoryResponse> run(
      Path cwd,
      Path filePath,
      SupportedLanguage language,
      SupportedIDE ide,
      String entityType,
      String entityIdType,
      String entityPackageName,
      String superclassSource) {
    TSFile tsFile = new TSFile(language, filePath);
    JPARepositoryData jpaRepositoryData;
    if (Strings.isNullOrEmpty(entityIdType)
        && Strings.isNullOrEmpty(entityType)
        && Strings.isNullOrEmpty(entityPackageName)) {
      PrepareDataResult prepareResult = this.prepareData(tsFile, cwd, superclassSource);
      if (prepareResult.requiresSymbolSource()) {
        return DataTransferObject.success(prepareResult.getRequiresSymbolResponse());
      }
      if (prepareResult.isError()) {
        return DataTransferObject.error(prepareResult.getErrorMessage());
      }
      jpaRepositoryData = prepareResult.getRepositoryData();
    } else {
      jpaRepositoryData =
          this.buildJPARepositoryData(cwd, entityType, entityIdType, entityPackageName);
      if (jpaRepositoryData == null) {
        return DataTransferObject.error("Unable to gather necessary repository data.");
      }
    }
    DataTransferObject<CreateNewFileResponse> createFileResult =
        this.createRepositoryFile(jpaRepositoryData);
    if (!createFileResult.getSucceed()) {
      return DataTransferObject.error(createFileResult.getErrorReason());
    }
    CreateJPARepositoryResponse response =
        CreateJPARepositoryResponse.builder()
            .filePath(createFileResult.getData().getFilePath())
            .requiresSymbolSource(false)
            .build();
    return DataTransferObject.success(response);
  }
}
