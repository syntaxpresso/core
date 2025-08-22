package io.github.syntaxpresso.core.command;

import io.github.syntaxpresso.core.command.dto.GetTextFromCursorPositionResponse;
import io.github.syntaxpresso.core.common.DataTransferObject;
import io.github.syntaxpresso.core.common.extra.SupportedIDE;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import io.github.syntaxpresso.core.service.java.JavaService;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@RequiredArgsConstructor
@Command(name = "get-text", description = "Get text of an specific node based on cursor position.")
public class GetTextFromCursorPositionCommand
    implements Callable<DataTransferObject<GetTextFromCursorPositionResponse>> {
  private final JavaService javaService;

  @Option(
      names = {"--file-path"},
      description = "The absolute path to the .java file.",
      required = true)
  private Path filePath;

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
      names = {"--line"},
      description = "The cursor line",
      required = true)
  private Integer line;

  @Option(
      names = {"--column"},
      description = "The cursor column",
      required = true)
  private Integer column;

  @Override
  public DataTransferObject<GetTextFromCursorPositionResponse> call() {
    if (this.language.equals(SupportedLanguage.JAVA)) {
      return this.javaService.getTextFromCursorPosition(
          this.filePath, this.language, this.ide, this.line, this.column);
    }
    return DataTransferObject.error("Language not supported.");
  }
}
