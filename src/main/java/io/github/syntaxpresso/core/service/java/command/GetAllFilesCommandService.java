package io.github.syntaxpresso.core.service.java.command;

import io.github.syntaxpresso.core.command.dto.FileResponse;
import io.github.syntaxpresso.core.command.dto.GetAllFilesResponse;
import io.github.syntaxpresso.core.command.extra.JavaFileTemplate;
import io.github.syntaxpresso.core.common.DataTransferObject;
import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.service.java.JavaLanguageService;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.treesitter.TSNode;

@Getter
@RequiredArgsConstructor
public class GetAllFilesCommandService {
  private final JavaLanguageService javaLanguageService;

  private Optional<TSNode> getPublicNodeByFileType(TSFile tsFile, JavaFileTemplate fileType) {
    if (fileType.equals(JavaFileTemplate.ENUM)) {
      return this.javaLanguageService.getEnumDeclarationService().getPublicEnum(tsFile);
    } else if (fileType.equals(JavaFileTemplate.CLASS)) {
      return this.javaLanguageService.getClassDeclarationService().getPublicClass(tsFile);
    } else if (fileType.equals(JavaFileTemplate.ANNOTATION)) {
      return this.javaLanguageService.getAnnotationDeclarationService().getPublicAnnotation(tsFile);
    } else if (fileType.equals(JavaFileTemplate.RECORD)) {
      return this.javaLanguageService.getRecordDeclarationService().getPublicRecord(tsFile);
    } else if (fileType.equals(JavaFileTemplate.INTERFACE)) {
      return this.javaLanguageService.getInterfaceDeclarationService().getPublicInterface(tsFile);
    }
    return Optional.empty();
  }

  private Optional<String> extractPackageScope(TSFile tsFile) {
    Optional<TSNode> packageDeclarationNode =
        this.javaLanguageService.getPackageDeclarationService().getPackageDeclarationNode(tsFile);
    if (packageDeclarationNode.isEmpty()) {
      return Optional.empty();
    }
    Optional<TSNode> packageScopeNode =
        this.javaLanguageService
            .getPackageDeclarationService()
            .getPackageClassScopeNode(tsFile, packageDeclarationNode.get());
    if (packageScopeNode.isPresent()) {
      String packageScope = tsFile.getTextFromNode(packageScopeNode.get());
      return Optional.of(packageScope);
    }
    return Optional.empty();
  }

  private Optional<FileResponse> createFileResponse(TSFile tsFile) {
    Optional<String> fileName = tsFile.getFileNameWithoutExtension();
    if (fileName.isEmpty()) {
      return Optional.empty();
    }
    FileResponse fileResponse = new FileResponse();
    fileResponse.setType(fileName.get());
    String filePath = tsFile.getFile().getAbsolutePath();
    fileResponse.setFilePath(filePath);
    return Optional.of(fileResponse);
  }

  public DataTransferObject<GetAllFilesResponse> run(Path cwd, JavaFileTemplate fileType) {
    GetAllFilesResponse response = new GetAllFilesResponse();
    List<TSFile> allJavaFiles = this.javaLanguageService.getAllJavaFilesFromCwd(cwd);
    for (TSFile tsFile : allJavaFiles) {
      Optional<FileResponse> fileResponseOpt = createFileResponse(tsFile);
      if (fileResponseOpt.isEmpty()) {
        continue;
      }
      FileResponse fileResponse = fileResponseOpt.get();
      Optional<TSNode> publicNode = getPublicNodeByFileType(tsFile, fileType);
      if (publicNode.isEmpty()) {
        continue;
      }
      Optional<String> packageScope = extractPackageScope(tsFile);
      if (packageScope.isEmpty()) {
        continue;
      }
      fileResponse.setPackagePath(packageScope.get());
      response.getResponse().add(fileResponse);
    }
    return DataTransferObject.success(response);
  }
}
