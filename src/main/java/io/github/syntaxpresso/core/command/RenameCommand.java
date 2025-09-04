package io.github.syntaxpresso.core.command;

import io.github.syntaxpresso.core.command.dto.RenameResponse;
import io.github.syntaxpresso.core.command.extra.JavaSourceDirectoryType;
import io.github.syntaxpresso.core.common.DataTransferObject;
import io.github.syntaxpresso.core.common.extra.SupportedIDE;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import io.github.syntaxpresso.core.service.java.JavaCommandService;
import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    name = "rename",
    description = "Rename a Java class/interface/enum and its file.",
    mixinStandardHelpOptions = true)
@RequiredArgsConstructor
public class RenameCommand implements Callable<DataTransferObject<RenameResponse>> {
  private final JavaCommandService javaCommandService;

  @Option(names = "--cwd", description = "Current Working Directory", required = true)
  private Path cwd;

  @Option(
      names = {"--file-path"},
      description = "The absolute path to the .java file.",
      required = true)
  private File filePath;

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
      names = {"--new-name"},
      description = "The new name for the class/interface/enum.",
      required = true)
  private String newName;

  @Option(
      names = {"--line"},
      description = "The cursor line",
      required = true)
  private Integer line;

  @Option(
      names = {"--column"},
      description = "The cursor column",
      required = true)
  private Integer column;

  @Option(
      names = {"--source-directory"},
      description = "The source directory type (e.g., MAIN, TEST).",
      defaultValue = "MAIN")
  private JavaSourceDirectoryType sourceDirectoryType;

  @Override
  public DataTransferObject<RenameResponse> call() {
    if (this.language.equals(SupportedLanguage.JAVA)) {
      return this.javaCommandService.rename(
          this.cwd, this.filePath.toPath(), this.ide, this.line, this.column, this.newName);
    }
    return DataTransferObject.error("Language not supported.");
  }
}
