package io.github.syntaxpresso.core.command.java;

import io.github.syntaxpresso.core.command.java.dto.CreateNewJavaFileResponse;
import io.github.syntaxpresso.core.command.java.extra.JavaFileTemplate;
import io.github.syntaxpresso.core.command.java.extra.SourceDirectoryType;
import io.github.syntaxpresso.core.common.DataTransferObject;
import io.github.syntaxpresso.core.service.java.JavaService;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@RequiredArgsConstructor
@Command(name = "create-new-file", description = "Create a new Java file")
public class CreateNewFileCommand
    implements Callable<DataTransferObject<CreateNewJavaFileResponse>> {
  private final JavaService javaService;

  @Option(names = "--cwd", description = "Current Working Directory", required = true)
  private Path cwd;

  @Option(
      names = "--package-name",
      description = "The package name for the new file",
      required = true)
  private String packageName;

  @Option(names = "--file-name", description = "The name of the new file", required = true)
  private String fileName;

  @Option(
      names = "--file-type",
      description = "The type of the new file (CLASS, INTERFACE, RECORD, ENUM, ANNOTATION)",
      required = true)
  private JavaFileTemplate fileType;

  @Option(
      names = "--source-directory-type",
      description =
          "Defines if the file should be created in the main or in the test directory (MAIN, TEST)",
      required = false)
  private SourceDirectoryType sourceDirectoryType = SourceDirectoryType.MAIN;

  @Override
  public DataTransferObject<CreateNewJavaFileResponse> call() {
    return this.javaService.createNewFile(
        this.cwd, this.packageName, this.fileName, this.fileType, this.sourceDirectoryType);
  }
}
