package io.github.syntaxpresso.core.service.java.command;

import static io.github.syntaxpresso.core.service.java.language.extra.AnnotationInsertionPoint.AnnotationInsertionPosition.ABOVE_SCOPE_DECLARATION;

import com.google.common.base.Strings;
import io.github.syntaxpresso.core.command.dto.CreateNewFileResponse;
import io.github.syntaxpresso.core.command.dto.CreateNewJPAEntityResponse;
import io.github.syntaxpresso.core.command.extra.JavaFileTemplate;
import io.github.syntaxpresso.core.command.extra.JavaSourceDirectoryType;
import io.github.syntaxpresso.core.common.DataTransferObject;
import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import io.github.syntaxpresso.core.service.java.JavaLanguageService;
import io.github.syntaxpresso.core.service.java.language.extra.AnnotationArgument;
import io.github.syntaxpresso.core.service.java.language.extra.AnnotationInsertionPoint;
import io.github.syntaxpresso.core.util.StringHelper;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.treesitter.TSNode;

@RequiredArgsConstructor
public class CreateNewJPAEntityCommandService {
  private final JavaLanguageService javaLanguageService;
  private final CreateNewFileCommandService createNewFileCommandService;

  public DataTransferObject<CreateNewJPAEntityResponse> createNewJPAEntity(
      final Path cwd, final String packageName, final String fileName) {
    if (!fileName.endsWith(".java")) {
      return DataTransferObject.error("File is not a .java file: " + fileName);
    }
    String fileNameWithoutExtension = fileName.replace(".java", "");
    String snakeName = StringHelper.pascalToSnake(fileNameWithoutExtension);
    String tableAnnotation = "@Table(name = \"" + snakeName + "\")";
    List<TSFile> files =
        this.javaLanguageService.getAllJavaFilesFromCwd(cwd, JavaSourceDirectoryType.MAIN);
    Optional<DataTransferObject<CreateNewJPAEntityResponse>> conflict =
        files.parallelStream()
            .filter(
                file ->
                    this.javaLanguageService
                        .getClassDeclarationService()
                        .getPublicClass(file)
                        .isPresent())
            .map(file -> validateEntityConflict(file, snakeName))
            .filter(result -> !result.getSucceed())
            .findAny();
    if (conflict.isPresent()) {
      return conflict.get();
    }
    DataTransferObject<CreateNewFileResponse> response =
        this.createNewFileCommandService.run(
            cwd, packageName, fileName, JavaFileTemplate.CLASS, JavaSourceDirectoryType.MAIN);
    if (!response.getSucceed()) {
      return DataTransferObject.error("Unable to create file");
    }
    TSFile tsFile = new TSFile(SupportedLanguage.JAVA, Paths.get(response.getData().getFilePath()));
    Optional<TSNode> packageDeclarationNode =
        this.javaLanguageService.getPackageDeclarationService().getPackageDeclarationNode(tsFile);
    if (packageDeclarationNode.isEmpty()) {
      return DataTransferObject.error("Package declaration is empty");
    }
    Optional<TSNode> classNode =
        this.javaLanguageService.getClassDeclarationService().getPublicClass(tsFile);
    if (classNode.isEmpty()) {
      return DataTransferObject.error("Public class not found");
    }
    AnnotationInsertionPoint entityAnnotationPoint =
        this.javaLanguageService
            .getAnnotationService()
            .getAnnotationInsertionPosition(tsFile, classNode.get(), ABOVE_SCOPE_DECLARATION);
    this.javaLanguageService
        .getAnnotationService()
        .addAnnotation(tsFile, classNode.get(), entityAnnotationPoint, "@Entity");
    AnnotationInsertionPoint tableAnnotationPoint =
        this.javaLanguageService
            .getAnnotationService()
            .getAnnotationInsertionPosition(tsFile, classNode.get(), ABOVE_SCOPE_DECLARATION);
    this.javaLanguageService
        .getAnnotationService()
        .addAnnotation(tsFile, classNode.get(), tableAnnotationPoint, tableAnnotation);
    this.javaLanguageService
        .getImportDeclarationService()
        .addImport(tsFile, "jakarta.persistence", "Entity", packageDeclarationNode.get());
    this.javaLanguageService
        .getImportDeclarationService()
        .addImport(tsFile, "jakarta.persistence", "Table", packageDeclarationNode.get());
    try {
      tsFile.save();
    } catch (IOException e) {
      return DataTransferObject.error("Could not save file: " + e.getMessage());
    }
    return DataTransferObject.success(
        new CreateNewJPAEntityResponse(tsFile.getFile().getAbsolutePath()));
  }

  private DataTransferObject<CreateNewJPAEntityResponse> validateEntityConflict(
      TSFile file, String snakeName) {
    Optional<TSNode> publicClass =
        this.javaLanguageService.getClassDeclarationService().getPublicClass(file);

    if (publicClass.isEmpty()) {
      return DataTransferObject.success();
    }
    String packageScope = "jakarta.persistence";
    String entityAnnotationName = "Entity";
    String tableAnnotationName = "Table";
    Optional<TSNode> entityImportDeclarationNode =
        this.javaLanguageService
            .getImportDeclarationService()
            .findImportDeclarationNode(file, packageScope, entityAnnotationName);
    Optional<TSNode> tableImportDeclarationNode =
        this.javaLanguageService
            .getImportDeclarationService()
            .findImportDeclarationNode(file, packageScope, tableAnnotationName);
    if (entityImportDeclarationNode.isPresent()) {
      Optional<TSNode> entityAnnotationNode =
          this.javaLanguageService
              .getAnnotationService()
              .findAnnotationByName(file, publicClass.get(), entityAnnotationName);
      if (entityAnnotationNode.isPresent()) {
        DataTransferObject<CreateNewJPAEntityResponse> entityExists =
            verifyArgumentName(file, entityAnnotationNode.get(), snakeName);
        if (!entityExists.getSucceed()) return entityExists;
      }
    }
    if (tableImportDeclarationNode.isPresent()) {
      Optional<TSNode> tableAnnotationNode =
          this.javaLanguageService
              .getAnnotationService()
              .findAnnotationByName(file, publicClass.get(), tableAnnotationName);
      if (tableAnnotationNode.isPresent()) {
        DataTransferObject<CreateNewJPAEntityResponse> tableExists =
            verifyArgumentName(file, tableAnnotationNode.get(), snakeName);
        if (!tableExists.getSucceed()) return tableExists;
      }
    }
    return DataTransferObject.success();
  }

  private DataTransferObject<CreateNewJPAEntityResponse> verifyArgumentName(
      TSFile file, TSNode tableAnnotationNode, String snakeName) {
    Map<String, AnnotationArgument> annotationArgument =
        this.javaLanguageService
            .getAnnotationService()
            .getAnnotationArguments(file, tableAnnotationNode);
    AnnotationArgument nameArgumentNode = annotationArgument.get("name");
    if (nameArgumentNode != null) {
      String argValue = nameArgumentNode.getValue(file).replace("\"", "");
      if (Strings.isNullOrEmpty(argValue)) {
        return DataTransferObject.error("Annotation does not exist");
      }
      if (argValue.equals(snakeName)) {
        return DataTransferObject.error("JPA Entity already exist");
      }
    }
    return DataTransferObject.success();
  }
}
