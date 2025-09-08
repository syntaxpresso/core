package io.github.syntaxpresso.core.command;

import io.github.syntaxpresso.core.command.dto.CreateNewFileResponse;
import io.github.syntaxpresso.core.command.extra.JavaFileTemplate;
import io.github.syntaxpresso.core.command.extra.JavaSourceDirectoryType;
import io.github.syntaxpresso.core.common.DataTransferObject;
import io.github.syntaxpresso.core.common.extra.SupportedIDE;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import io.github.syntaxpresso.core.service.java.command.CreateNewFileCommandService;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@RequiredArgsConstructor
@Command(name = "create-new-file", description = "Create a new Java file")
public class CreateNewFileCommand implements Callable<DataTransferObject<CreateNewFileResponse>> {
  private final CreateNewFileCommandService createNewFileCommandService;

  @Option(names = "--cwd", description = "Current Working Directory", required = true)
  private Path cwd;

  @Option(
      names = "--language",
      description = "The language related to the command execution.",
      required = true)
  private SupportedLanguage language;

  @Option(
      names = "--ide",
      description = "The IDE the command is being called from.",
      required = true)
  private SupportedIDE ide = SupportedIDE.NONE;

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
      names = "--source-directory",
      description =
          "Defines if the file should be created in the main or in the test directory (MAIN, TEST)",
      required = false)
  private JavaSourceDirectoryType sourceDirectoryType = JavaSourceDirectoryType.MAIN;

  @Override
  public DataTransferObject<CreateNewFileResponse> call() {
    if (this.language != null && this.language.equals(SupportedLanguage.JAVA)) {
      return this.createNewFileCommandService.run(
          this.cwd, this.packageName, this.fileName, this.fileType, this.sourceDirectoryType);
    }
    return DataTransferObject.error("Language not supported.");
  }
}
