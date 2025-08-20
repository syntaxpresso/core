package io.github.syntaxpresso.core.command.java;

import io.github.syntaxpresso.core.command.java.dto.RenameResponse;
import io.github.syntaxpresso.core.command.java.extra.SourceDirectoryType;
import io.github.syntaxpresso.core.common.DataTransferObject;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import io.github.syntaxpresso.core.service.java.JavaService;
import java.io.File;
import java.util.concurrent.Callable;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine;

@CommandLine.Command(
    name = "rename",
    description = "Rename a Java class/interface/enum and its file.",
    mixinStandardHelpOptions = true)
@RequiredArgsConstructor
public class RenameCommand implements Callable<DataTransferObject<RenameResponse>> {
  private final JavaService javaService;

  @CommandLine.Option(
      names = {"-f", "--file-path"},
      description = "The absolute path to the .java file.",
      required = true)
  private File filePath;

  @Option(
      names = "--language",
      description = "The language related to the command execution.",
      required = true)
  private SupportedLanguage language;

  @CommandLine.Option(
      names = {"-n", "--new-name"},
      description = "The new name for the class/interface/enum.",
      required = true)
  private String newName;

  @CommandLine.Option(
      names = {"-s", "--source-directory"},
      description = "The source directory type (e.g., MAIN, TEST).",
      defaultValue = "MAIN")
  private SourceDirectoryType sourceDirectoryType;

  @Override
  public DataTransferObject<RenameResponse> call() {
    if (this.language.equals(SupportedLanguage.JAVA)) {
      return this.javaService.rename(this.filePath.toPath(), this.newName);
    }
    return DataTransferObject.error("Language not supported.");
  }
}
