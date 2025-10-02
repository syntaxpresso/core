package io.github.syntaxpresso.core.service.java.command;

import com.google.common.base.Strings;
import io.github.syntaxpresso.core.command.dto.GetJPAEntityInfoResponse;
import io.github.syntaxpresso.core.command.extra.JavaSourceDirectoryType;
import io.github.syntaxpresso.core.common.DataTransferObject;
import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.common.extra.SupportedIDE;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import io.github.syntaxpresso.core.service.java.JavaLanguageService;
import io.github.syntaxpresso.core.service.java.command.extra.IdFieldSearchResult;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.treesitter.TSNode;

@RequiredArgsConstructor
public class GetJPAEntityInfoCommandService {
  private final JavaLanguageService javaLanguageService;

  public Optional<TSNode> getIdFieldFromEntity(TSFile tsFile, TSNode publicClassNode) {
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

  public Optional<TSFile> findSuperclassFile(Path cwd, TSFile entityFile, TSNode publicClassNode) {
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
    List<TSFile> allJavaFiles =
        this.javaLanguageService.getAllJavaFilesFromCwd(cwd, JavaSourceDirectoryType.MAIN);
    return allJavaFiles.parallelStream()
        .filter(tsFile -> tsFile.getFileNameWithoutExtension().isPresent())
        .filter(tsFile -> tsFile.getFileNameWithoutExtension().get().equals(superClassName))
        .findAny();
  }

  public IdFieldSearchResult findIdFieldRecursively(
      Path cwd, TSFile tsFile, TSNode publicClassNode) {
    return this.findIdFieldRecursively(cwd, tsFile, publicClassNode, null);
  }

  public IdFieldSearchResult findIdFieldRecursively(
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
      Optional<TSNode> externalClassNode =
          this.javaLanguageService
              .getClassDeclarationService()
              .getPublicClass(externalSuperclassFile);
      if (externalClassNode.isPresent()) {
        Optional<String> externalClassName =
            this.extractEntityType(externalSuperclassFile, externalClassNode.get());
        if (externalClassName.isPresent() && externalClassName.get().equals(superClassName)) {
          // We found the matching external superclass, search within it
          return this.findIdFieldRecursively(
              cwd, externalSuperclassFile, externalClassNode.get(), null);
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

  public Optional<String> extractEntityType(TSFile tsFile, TSNode publicClassNode) {
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

  public Optional<String> extractIdType(TSFile tsFile, TSNode idFieldNode) {
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

  public Optional<String> extractPackageName(TSFile tsFile) {
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

  public boolean isJPAEntity(TSFile tsFile, TSNode publicClassNode) {
    Optional<TSNode> entityAnnotation =
        this.javaLanguageService
            .getAnnotationService()
            .findAnnotationByName(tsFile, publicClassNode, "Entity");
    return entityAnnotation.isPresent();
  }

  public DataTransferObject<GetJPAEntityInfoResponse> run(
      Path cwd,
      Path filePath,
      SupportedLanguage language,
      SupportedIDE ide,
      String superclassSource) {
    String decodedSuperclassSource = null;
    if (!Strings.isNullOrEmpty(superclassSource)) {
      try {
        decodedSuperclassSource = new String(Base64.getDecoder().decode(superclassSource));
      } catch (IllegalArgumentException e) {
        return DataTransferObject.error(
            "Invalid base64 encoding in superclass source: " + e.getMessage());
      }
    }
    TSFile tsFile = new TSFile(language, filePath);
    Optional<TSNode> entityPublicClassNode =
        this.javaLanguageService.getClassDeclarationService().getPublicClass(tsFile);
    if (entityPublicClassNode.isEmpty()) {
      return DataTransferObject.error("No public class found in the entity file.");
    }
    Optional<String> packageName = this.extractPackageName(tsFile);
    if (packageName.isEmpty()) {
      return DataTransferObject.error("Package name could not be extracted from the entity file.");
    }
    Optional<String> entityType = this.extractEntityType(tsFile, entityPublicClassNode.get());
    if (entityType.isEmpty()) {
      return DataTransferObject.error("Entity type could not be extracted from the entity file.");
    }
    boolean isJPAEntity = this.isJPAEntity(tsFile, entityPublicClassNode.get());
    // Search for @Id field
    IdFieldSearchResult searchResult;
    TSFile externalSuperclassFile = null;
    if (!Strings.isNullOrEmpty(decodedSuperclassSource)) {
      externalSuperclassFile = new TSFile(SupportedLanguage.JAVA, decodedSuperclassSource);
      Optional<TSNode> externalSuperclassFilePublicClassNode =
          this.javaLanguageService
              .getClassDeclarationService()
              .getPublicClass(externalSuperclassFile);
      if (externalSuperclassFilePublicClassNode.isEmpty()) {
        return DataTransferObject.error("No public class found in the superclass source.");
      }
      searchResult =
          this.findIdFieldRecursively(
              cwd, externalSuperclassFile, externalSuperclassFilePublicClassNode.get());
    } else {
      searchResult = this.findIdFieldRecursively(cwd, tsFile, entityPublicClassNode.get());
    }
    String idFieldType = null;
    String idFieldPackageName = null;
    if (searchResult.isFound()) {
      Optional<String> idType =
          this.extractIdType(searchResult.getTsFile(), searchResult.getIdFieldNode());
      if (idType.isPresent()) {
        idFieldType = idType.get();
        // Try to extract package name for the ID field type
        Optional<String> idPackage = this.extractPackageName(searchResult.getTsFile());
        idFieldPackageName = idPackage.orElse(null);
      }
    }
    GetJPAEntityInfoResponse response =
        GetJPAEntityInfoResponse.builder()
            .isJPAEntity(isJPAEntity)
            .entityType(entityType.get())
            .entityPackageName(packageName.get())
            .entityPath(tsFile.getFile().getAbsolutePath())
            .idFieldType(idFieldType)
            .idFieldPackageName(idFieldPackageName)
            .build();
    return DataTransferObject.success(response);
  }
}
