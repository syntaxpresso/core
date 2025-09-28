package io.github.syntaxpresso.core.command;

import io.github.syntaxpresso.core.command.dto.GetAllFilesResponse;
import io.github.syntaxpresso.core.command.extra.JavaFileTemplate;
import io.github.syntaxpresso.core.common.DataTransferObject;
import io.github.syntaxpresso.core.common.extra.SupportedIDE;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import io.github.syntaxpresso.core.service.java.command.GetAllFilesCommandService;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@RequiredArgsConstructor
@Command(
    name = "get-all-files",
    description = "Get a list of all files in the current working directory by it's type.")
public class GetAllFilesCommand implements Callable<DataTransferObject<GetAllFilesResponse>> {
  private final GetAllFilesCommandService getAllFilesCommandService;

  @Option(
      names = {"--cwd"},
      description = "The current working directory.",
      required = true)
  private Path cwd;

  @Option(
      names = {"--file-type"},
      description = "The type of the files (CLASS, INTERFACE, ENUM, RECORD, ANNOTATION).",
      required = true)
  private JavaFileTemplate fileType;

  @Option(
      names = "--language",
      description = "The language related to the command execution.",
      required = false)
  private SupportedLanguage language = SupportedLanguage.JAVA;

  @Option(
      names = "--ide",
      description = "The IDE the command is being called from.",
      required = false)
  private SupportedIDE ide = SupportedIDE.NONE;

  @Override
  public DataTransferObject<GetAllFilesResponse> call() {
    if (this.language != null && this.language.equals(SupportedLanguage.JAVA)) {
      return this.getAllFilesCommandService.run(this.cwd, this.fileType);
    }
    return DataTransferObject.error("Language not supported.");
  }
}
