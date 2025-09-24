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
    Optional<TSFile> superClassFile = this.findSuperclassFile(cwd, tsFile, publicClassNode);
    if (superClassFile.isEmpty()) {
      return IdFieldSearchResult.missingSuperclass(superClassName);
    }
    Optional<TSNode> superClassPublicNode =
        this.javaLanguageService.getClassDeclarationService().getPublicClass(superClassFile.get());
    if (superClassPublicNode.isEmpty()) {
      return IdFieldSearchResult.notFound();
    }
    return this.findIdFieldRecursively(cwd, superClassFile.get(), superClassPublicNode.get());
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

  public JPARepositoryData prepareData(TSFile tsFile, Path cwd) {
    Optional<TSNode> publicClassNode =
        this.javaLanguageService.getClassDeclarationService().getPublicClass(tsFile);
    if (publicClassNode.isEmpty()) {
      return null;
    }
    Optional<String> packageName = this.extractPackageName(tsFile);
    if (packageName.isEmpty()) {
      return null;
    }
    Optional<String> entityType = this.extractEntityType(tsFile, publicClassNode.get());
    if (entityType.isEmpty()) {
      return null;
    }
    IdFieldSearchResult searchResult =
        this.findIdFieldRecursively(cwd, tsFile, publicClassNode.get());
    if (searchResult.isFound()) {
      // Id field found - extract ID type and create JPARepositoryData
      Optional<String> idType = this.extractIdType(tsFile, searchResult.getIdFieldNode());
      if (idType.isEmpty()) {
        return null;
      }

      JPARepositoryData repositoryData =
          this.buildJPARepositoryData(cwd, entityType.get(), idType.get(), packageName.get());
      return repositoryData;
    } else if (searchResult.hasMissingSuperclass()) {
      // Superclass exists but file not found - return response requiring symbol source
      CreateJPARepositoryResponse response =
          CreateJPARepositoryResponse.builder()
              .filePath(tsFile.getFile().getAbsolutePath())
              .symbol(searchResult.getMissingSuperclassName())
              .requiresSymbolSource(true)
              .build();
      return null;
    } else {
      // No Id field found in entire hierarchy
      return null;
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
      String entityPackageName) {
    TSFile tsFile = new TSFile(language, filePath);
    JPARepositoryData jpaRepositoryData = new JPARepositoryData();
    if (Strings.isNullOrEmpty(entityIdType)
        && Strings.isNullOrEmpty(entityType)
        && Strings.isNullOrEmpty(entityPackageName)) {
      jpaRepositoryData = this.prepareData(tsFile, cwd);
      if (jpaRepositoryData == null) {
        return DataTransferObject.error("Could not extract repository data from entity file.");
      }
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
