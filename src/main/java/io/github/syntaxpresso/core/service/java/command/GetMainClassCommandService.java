package io.github.syntaxpresso.core.service.java.command;

import io.github.syntaxpresso.core.command.dto.GetMainClassResponse;
import io.github.syntaxpresso.core.command.extra.JavaSourceDirectoryType;
import io.github.syntaxpresso.core.common.DataTransferObject;
import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.service.java.JavaLanguageService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.treesitter.TSNode;

@Getter
@RequiredArgsConstructor
public class GetMainClassCommandService {
  private final JavaLanguageService javaLanguageService;

  private DataTransferObject<GetMainClassResponse> validateArguments(Path cwd) {
    if (cwd == null || !Files.exists(cwd)) {
      return DataTransferObject.error("Current working directory does not exist.");
    }
    return DataTransferObject.success();
  }

  public DataTransferObject<GetMainClassResponse> run(Path cwd) {
    DataTransferObject<GetMainClassResponse> validateArguments = this.validateArguments(cwd);
    if (!validateArguments.getSucceed()) {
      return validateArguments;
    }
    List<TSFile> allTSFiles =
        this.javaLanguageService.getAllJavaFilesFromCwd(cwd, JavaSourceDirectoryType.MAIN);
    Optional<DataTransferObject<GetMainClassResponse>> mainClassResult =
        allTSFiles.stream()
            .map(tsFile -> this.processFileForMainClass(tsFile))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .findAny();
    return mainClassResult.orElse(
        DataTransferObject.error("Couldn't find the main class of this project."));
  }

  private Optional<DataTransferObject<GetMainClassResponse>> processFileForMainClass(
      TSFile tsFile) {
    Optional<TSNode> publicClassNode =
        this.getJavaLanguageService().getClassDeclarationService().getPublicClass(tsFile);
    if (publicClassNode.isEmpty()) {
      return Optional.empty();
    }
    List<TSNode> allClassMethodNodes =
        this.getJavaLanguageService()
            .getClassDeclarationService()
            .getMethodDeclarationService()
            .getAllMethodDeclarationNodes(tsFile, publicClassNode.get());
    for (TSNode methodNode : allClassMethodNodes) {
      boolean isMainMethod =
          this.getJavaLanguageService()
              .getClassDeclarationService()
              .getMethodDeclarationService()
              .isMainMethod(tsFile, methodNode);
      if (isMainMethod) {
        return Optional.of(this.createMainClassResponse(tsFile, publicClassNode.get()));
      }
    }
    return Optional.empty();
  }

  private DataTransferObject<GetMainClassResponse> createMainClassResponse(
      TSFile tsFile, TSNode publicClassNode) {
    Optional<TSNode> mainClassNameNode =
        this.getJavaLanguageService()
            .getClassDeclarationService()
            .getClassDeclarationNameNode(tsFile, publicClassNode);
    if (mainClassNameNode.isEmpty()) {
      return DataTransferObject.error(
          "Couldn't obtain public class' name from "
              + tsFile.getFile().getAbsolutePath().toString()
              + ".");
    }
    String mainClassName = tsFile.getTextFromNode(mainClassNameNode.get());
    Optional<TSNode> mainClassPackageNode =
        this.getJavaLanguageService()
            .getPackageDeclarationService()
            .getPackageDeclarationNode(tsFile);
    if (mainClassPackageNode.isEmpty()) {
      return DataTransferObject.error(
          "Couldn't obtain public class' package name from "
              + tsFile.getFile().getAbsolutePath().toString()
              + ".");
    }
    Optional<TSNode> mainClassPackageScopeNode =
        this.getJavaLanguageService()
            .getPackageDeclarationService()
            .getPackageScopeNode(tsFile, mainClassPackageNode.get());
    if (mainClassPackageScopeNode.isEmpty()) {
      return DataTransferObject.error(
          "Couldn't obtain public class' package scope from "
              + tsFile.getFile().getAbsolutePath().toString()
              + ".");
    }
    String mainClassPackageScopeName = tsFile.getTextFromNode(mainClassPackageScopeNode.get());
    GetMainClassResponse response =
        GetMainClassResponse.builder()
            .filePath(tsFile.getFile().getAbsolutePath())
            .className(mainClassName)
            .packageName(mainClassPackageScopeName)
            .build();
    return DataTransferObject.success(response);
  }
}
