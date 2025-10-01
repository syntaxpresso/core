package io.github.syntaxpresso.core.service.java.command;

import static io.github.syntaxpresso.core.service.java.language.extra.AnnotationInsertionPoint.AnnotationInsertionPosition.ABOVE_SCOPE_DECLARATION;

import com.google.common.base.Strings;
import io.github.syntaxpresso.core.command.dto.CreateJPARepositoryResponse;
import io.github.syntaxpresso.core.command.dto.CreateNewFileResponse;
import io.github.syntaxpresso.core.command.extra.JavaBasicType;
import io.github.syntaxpresso.core.command.extra.JavaFileTemplate;
import io.github.syntaxpresso.core.command.extra.JavaSourceDirectoryType;
import io.github.syntaxpresso.core.common.DataTransferObject;
import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.common.extra.SupportedIDE;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import io.github.syntaxpresso.core.service.java.JavaLanguageService;
import io.github.syntaxpresso.core.service.java.command.extra.IdFieldSearchResult;
import io.github.syntaxpresso.core.service.java.command.extra.JPARepositoryData;
import io.github.syntaxpresso.core.service.java.command.extra.PrepareDataResult;
import io.github.syntaxpresso.core.service.java.language.extra.AnnotationInsertionPoint;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.treesitter.TSNode;

@RequiredArgsConstructor
public class CreateJPARepositoryCommandService {
  private final JavaLanguageService javaLanguageService;
  private final CreateNewFileCommandService createNewFileCommandService;
  private final GetJPAEntityInfoCommandService getJPAEntityInfoCommandService;

  private JPARepositoryData buildJPARepositoryData(
      Path cwd, String entityType, String entityIdType, String entityPackageName) {
    return JPARepositoryData.builder()
        .cwd(cwd)
        .packageName(entityPackageName)
        .entityType(entityType)
        .entityIdType(entityIdType)
        .build();
  }

  private DataTransferObject<CreateNewFileResponse> createRepositoryFile(
      JPARepositoryData jpaRepositoryData) {
    if (Strings.isNullOrEmpty(jpaRepositoryData.getPackageName())
        || Strings.isNullOrEmpty(jpaRepositoryData.getEntityType())
        || Strings.isNullOrEmpty(jpaRepositoryData.getEntityIdType())) {
      return DataTransferObject.error("Entity type, ID type, and package name are required.");
    }
    String repositoryName = jpaRepositoryData.getEntityType() + "Repository";
    DataTransferObject<CreateNewFileResponse> createNewFileResponse =
        this.createNewFileCommandService.run(
            jpaRepositoryData.getCwd(),
            jpaRepositoryData.getPackageName(),
            repositoryName,
            JavaFileTemplate.INTERFACE,
            JavaSourceDirectoryType.MAIN);
    if (!createNewFileResponse.getSucceed()) {
      return DataTransferObject.error("Unable to create file.");
    }
    TSFile tsFile =
        new TSFile(
            SupportedLanguage.JAVA, Paths.get(createNewFileResponse.getData().getFilePath()));
    Optional<TSNode> packageDeclarationNode =
        this.javaLanguageService.getPackageDeclarationService().getPackageDeclarationNode(tsFile);
    if (packageDeclarationNode.isEmpty()) {
      return DataTransferObject.error(
          "Unable to get the package declaration node of the newly created file.");
    }
    this.javaLanguageService
        .getImportDeclarationService()
        .addImport(
            tsFile,
            "org.springframework.data.jpa.repository",
            "JpaRepository",
            packageDeclarationNode.get());
    this.javaLanguageService
        .getImportDeclarationService()
        .addImport(
            tsFile, "org.springframework.stereotype", "Repository", packageDeclarationNode.get());
    Optional<JavaBasicType> typeToImport =
        JavaBasicType.getByTypeName(jpaRepositoryData.getEntityIdType());
    if (typeToImport.isPresent()) {
      if (typeToImport.get().getPackageName().isPresent()) {
        this.javaLanguageService
            .getImportDeclarationService()
            .addImport(
                tsFile,
                typeToImport.get().getPackageName().get(),
                typeToImport.get().getTypeName(),
                packageDeclarationNode.get());
      }
    }
    Optional<TSNode> publicInterfaceNode =
        this.javaLanguageService.getInterfaceDeclarationService().getPublicInterface(tsFile);
    if (publicInterfaceNode.isEmpty()) {
      return DataTransferObject.error("Unable to get the public interface node of the repository.");
    }
    Optional<TSNode> publicInterfaceNameNode =
        this.javaLanguageService
            .getInterfaceDeclarationService()
            .getInterfaceNameNode(tsFile, publicInterfaceNode.get());
    if (publicInterfaceNameNode.isEmpty()) {
      return DataTransferObject.error(
          "Unable to get the public interface name node of the repository.");
    }
    String template =
        String.format(
            " extends JpaRepository<%s, %s> ",
            jpaRepositoryData.getEntityType(), jpaRepositoryData.getEntityIdType());
    tsFile.updateSourceCode(
        publicInterfaceNameNode.get().getEndByte(),
        publicInterfaceNameNode.get().getEndByte(),
        template);
    AnnotationInsertionPoint repositoryAnnotationPoint =
        this.javaLanguageService
            .getAnnotationService()
            .getAnnotationInsertionPosition(
                tsFile, publicInterfaceNode.get(), ABOVE_SCOPE_DECLARATION);
    this.javaLanguageService
        .getAnnotationService()
        .addAnnotation(tsFile, publicInterfaceNode.get(), repositoryAnnotationPoint, "@Repository");
    try {
      tsFile.save();
    } catch (IOException e) {
      return DataTransferObject.error("Failed to create repository file: " + e.getMessage());
    }
    CreateNewFileResponse response =
        CreateNewFileResponse.builder().filePath(tsFile.getFile().getAbsolutePath()).build();
    return DataTransferObject.success(response);
  }

  public PrepareDataResult prepareData(TSFile tsFile, Path cwd) {
    return this.prepareData(tsFile, cwd, null);
  }

  public PrepareDataResult prepareData(TSFile tsFile, Path cwd, String superclassSource) {
    Optional<TSNode> entityPublicClassNode =
        this.javaLanguageService.getClassDeclarationService().getPublicClass(tsFile);
    if (entityPublicClassNode.isEmpty()) {
      return PrepareDataResult.error("No public class found in the entity file.");
    }
    Optional<String> packageName = this.getJPAEntityInfoCommandService.extractPackageName(tsFile);
    if (packageName.isEmpty()) {
      return PrepareDataResult.error("Package name could not be extracted from the entity file.");
    }
    Optional<String> entityType =
        this.getJPAEntityInfoCommandService.extractEntityType(tsFile, entityPublicClassNode.get());
    if (entityType.isEmpty()) {
      return PrepareDataResult.error("Entity type could not be extracted from the entity file.");
    }
    IdFieldSearchResult searchResult;
    TSFile externalSuperclassFile = null;
    if (!Strings.isNullOrEmpty(superclassSource)) {
      externalSuperclassFile = new TSFile(SupportedLanguage.JAVA, superclassSource);
      Optional<TSNode> externalSuperclassFilePublicClassNode =
          this.javaLanguageService
              .getClassDeclarationService()
              .getPublicClass(externalSuperclassFile);
      if (externalSuperclassFilePublicClassNode.isEmpty()) {
        return PrepareDataResult.error("No public class found in the entity file.");
      }
      searchResult =
          this.getJPAEntityInfoCommandService.findIdFieldRecursively(
              cwd, externalSuperclassFile, externalSuperclassFilePublicClassNode.get());
    } else {
      searchResult =
          this.getJPAEntityInfoCommandService.findIdFieldRecursively(
              cwd, tsFile, entityPublicClassNode.get());
    }
    if (searchResult.isFound()) {
      // Id field found - extract ID type and create JPARepositoryData
      Optional<String> idType =
          this.getJPAEntityInfoCommandService.extractIdType(
              searchResult.getTsFile(), searchResult.getIdFieldNode());
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
    PrepareDataResult prepareResult = this.prepareData(tsFile, cwd, decodedSuperclassSource);
    if (prepareResult.requiresSymbolSource()) {
      return DataTransferObject.success(prepareResult.getRequiresSymbolResponse());
    }
    if (prepareResult.isError()) {
      return DataTransferObject.error(prepareResult.getErrorMessage());
    }
    JPARepositoryData jpaRepositoryData = prepareResult.getRepositoryData();
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
