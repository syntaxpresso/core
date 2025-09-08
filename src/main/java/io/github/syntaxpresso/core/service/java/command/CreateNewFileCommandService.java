package io.github.syntaxpresso.core.service.java.command;

import com.google.common.base.Strings;
import io.github.syntaxpresso.core.command.dto.CreateNewFileResponse;
import io.github.syntaxpresso.core.command.extra.JavaFileTemplate;
import io.github.syntaxpresso.core.command.extra.JavaSourceDirectoryType;
import io.github.syntaxpresso.core.common.DataTransferObject;
import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import io.github.syntaxpresso.core.service.java.JavaLanguageService;
import io.github.syntaxpresso.core.util.PathHelper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CreateNewFileCommandService {
  private final JavaLanguageService javaLanguageService;
  private final PathHelper pathHelper;

  private DataTransferObject<CreateNewFileResponse> validateArguments(
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
    return DataTransferObject.success();
  }

  public DataTransferObject<CreateNewFileResponse> run(
      Path cwd,
      String packageName,
      String fileName,
      JavaFileTemplate fileType,
      JavaSourceDirectoryType sourceDirectoryType) {
    DataTransferObject<CreateNewFileResponse> validateArguments =
        this.validateArguments(cwd, packageName, fileName, fileType, sourceDirectoryType);
    if (!validateArguments.getSucceed()) {
      return validateArguments;
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
