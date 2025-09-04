package io.github.syntaxpresso.core.service.java;

import com.google.common.base.Strings;
import io.github.syntaxpresso.core.command.dto.CreateNewFileResponse;
import io.github.syntaxpresso.core.command.dto.GetMainClassResponse;
import io.github.syntaxpresso.core.command.extra.JavaFileTemplate;
import io.github.syntaxpresso.core.command.extra.JavaSourceDirectoryType;
import io.github.syntaxpresso.core.common.DataTransferObject;
import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import io.github.syntaxpresso.core.util.PathHelper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.treesitter.TSNode;

@Getter
@RequiredArgsConstructor
public class JavaCommandService {
  private final PathHelper pathHelper;
  private final JavaLanguageService javaLanguageService;

  public DataTransferObject<GetMainClassResponse> getMainClass(Path cwd) {
    if (cwd == null || !Files.exists(cwd)) {
      return DataTransferObject.error("Current working directory does not exist.");
    }
    List<TSFile> allTSFiles = this.javaLanguageService.getAllJavaFilesFromCwd(cwd);
    for (TSFile tsFile : allTSFiles) {
      Optional<TSNode> publicClassNode =
          this.getJavaLanguageService().getClassDeclarationService().getPublicClass(tsFile);
      if (publicClassNode.isEmpty()) {
        continue;
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
          Optional<TSNode> mainClassNameNode =
              this.getJavaLanguageService()
                  .getClassDeclarationService()
                  .getClassDeclarationNameNode(tsFile, publicClassNode.get());
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
          String mainClassPackageScopeName =
              tsFile.getTextFromNode(mainClassPackageScopeNode.get());
          GetMainClassResponse response =
              GetMainClassResponse.builder()
                  .filePath(tsFile.getFile().getAbsolutePath())
                  .className(mainClassName)
                  .packageName(mainClassPackageScopeName)
                  .build();
          return DataTransferObject.success(response);
        }
      }
    }
    return DataTransferObject.error("Couldn't find the main class of this project.");
  }

  public DataTransferObject<CreateNewFileResponse> createNewFile(
      Path cwd,
      String packageName,
      String fileName,
      JavaFileTemplate fileType,
      JavaSourceDirectoryType sourceDirectoryType) {
    if (cwd == null || !Files.exists(cwd)) {
      return DataTransferObject.error("Current working directory does not exist.");
    }
    if (Strings.isNullOrEmpty(packageName)) {
      return DataTransferObject.error("Package name invalid.");
    }
    if (Strings.isNullOrEmpty(fileName)) {
      return DataTransferObject.error("File name invalid.");
    }
    if (fileType == null) {
      return DataTransferObject.error("File type is required.");
    }
    if (sourceDirectoryType == null) {
      return DataTransferObject.error("Source directory type is required.");
    }
    final String srcDirName =
        (sourceDirectoryType == JavaSourceDirectoryType.MAIN) ? "src/main/java" : "src/test/java";
    try {
      Optional<Path> sourceDirOptional = this.pathHelper.findDirectoryRecursively(cwd, srcDirName);
      if (sourceDirOptional.isEmpty()) {
        if (!Files.exists(cwd.resolve(srcDirName))) {
          return DataTransferObject.error("Cannot find source directory.");
        }
      }
    } catch (IOException e) {
      return DataTransferObject.error("Failed to search for source directory: " + e.getMessage());
    }
    try {
      String className = fileName.trim();
      className = com.google.common.io.Files.getNameWithoutExtension(className);
      String template = fileType.getSourceContent(packageName, className);
      TSFile file = new TSFile(SupportedLanguage.JAVA, template);
      Optional<Path> filePath =
          this.getJavaLanguageService()
              .getPackageDeclarationService()
              .getFilePathFromPackageScope(cwd, packageName, sourceDirectoryType);
      if (filePath.isEmpty()) {
        return DataTransferObject.error("Package name couldn't be determined.");
      }
      Path targetPath =
          filePath.get().resolve(className.concat(SupportedLanguage.JAVA.getFileExtension()));
      if (Files.exists(targetPath)) {
        return DataTransferObject.error("File already exists: " + targetPath.toString());
      }
      file.saveAs(targetPath);
      CreateNewFileResponse response =
          CreateNewFileResponse.builder().filePath(file.getFile().getAbsolutePath()).build();
      return DataTransferObject.success(response);
    } catch (IOException e) {
      return DataTransferObject.error("Failed to create file: " + e.getMessage());
    } catch (Exception e) {
      return DataTransferObject.error("Unexpected error occurred: " + e.getMessage());
    }
  }
}
