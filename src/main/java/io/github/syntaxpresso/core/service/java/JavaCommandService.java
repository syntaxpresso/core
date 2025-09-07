package io.github.syntaxpresso.core.service.java;

import com.google.common.base.Strings;
import io.github.syntaxpresso.core.command.dto.CreateNewFileResponse;
import io.github.syntaxpresso.core.command.dto.GetCursorPositionInfoResponse;
import io.github.syntaxpresso.core.command.dto.GetMainClassResponse;
import io.github.syntaxpresso.core.command.dto.ParseSourceCodeResponse;
import io.github.syntaxpresso.core.command.dto.RenameResponse;
import io.github.syntaxpresso.core.command.extra.JavaFileTemplate;
import io.github.syntaxpresso.core.command.extra.JavaSourceDirectoryType;
import io.github.syntaxpresso.core.common.DataTransferObject;
import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.common.extra.SupportedIDE;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import io.github.syntaxpresso.core.service.extra.JavaIdentifierType;
import io.github.syntaxpresso.core.service.java.command.RenameCommandService;
import io.github.syntaxpresso.core.service.java.language.VariableNamingService;
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
  private final VariableNamingService variableNamingService;
  private final RenameCommandService renameCommandService;

  public DataTransferObject<ParseSourceCodeResponse> parseSourceCommand(
      final String sourceCode,
      final Path filePath,
      final SupportedLanguage language,
      final SupportedIDE ide) {
    ParseSourceCodeResponse response =
        ParseSourceCodeResponse.builder()
            .ide(ide)
            .sourceCode(sourceCode)
            .filePath(filePath != null ? filePath.toString() : null)
            .parseSuccess(true)
            .build();
    return DataTransferObject.success(response);
  }

  public DataTransferObject<GetCursorPositionInfoResponse> getTextFromCursorPosition(
      final Path filePath,
      final SupportedLanguage language,
      final SupportedIDE ide,
      final Integer line,
      final Integer column) {
    if (!Files.exists(filePath)) {
      return DataTransferObject.error("File does not exist: " + filePath);
    }
    if (!filePath.toString().endsWith(".java")) {
      return DataTransferObject.error("File is not a .java file: " + filePath);
    }
    TSFile file = new TSFile(language, filePath);
    TSNode node = file.getNodeFromPosition(line, column, ide);
    if (node == null) {
      return DataTransferObject.error("No symbol found at the specified position.");
    }
    JavaIdentifierType identifierType = this.javaLanguageService.getIdentifierType(node, ide);
    if (identifierType == null) {
      return DataTransferObject.error(
          "Unable to determine symbol type at cursor position. Node type: "
              + node.getType()
              + ", Node text: '"
              + file.getTextFromNode(node)
              + "'");
    }
    String text;
    try {
      text = file.getTextFromRange(node.getStartByte(), node.getEndByte());
    } catch (Exception e) {
      return DataTransferObject.error("Error getting text from node: " + e.getMessage());
    }
    if (Strings.isNullOrEmpty(text)) {
      return DataTransferObject.error("Unable to determine current symbol name.");
    }
    GetCursorPositionInfoResponse response =
        GetCursorPositionInfoResponse.builder()
            .filePath(filePath.toString())
            .language(SupportedLanguage.JAVA)
            .node(node.toString())
            .nodeText(text)
            .nodeType(identifierType)
            .build();
    return DataTransferObject.success(response);
  }

  public DataTransferObject<GetMainClassResponse> getMainClass(final Path cwd) {
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
      final Path cwd,
      final String packageName,
      final String fileName,
      final JavaFileTemplate fileType,
      final JavaSourceDirectoryType sourceDirectoryType) {
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

  public DataTransferObject<RenameResponse> rename(
      final Path cwd,
      final Path filePath,
      final SupportedIDE ide,
      final int line,
      final int column,
      final String newName) {
    return this.renameCommandService.rename(cwd, filePath, ide, line, column, newName);
  }
}
