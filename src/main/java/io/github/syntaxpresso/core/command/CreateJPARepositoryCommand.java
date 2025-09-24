package io.github.syntaxpresso.core.command;

import io.github.syntaxpresso.core.command.dto.CreateJPARepositoryResponse;
import io.github.syntaxpresso.core.common.DataTransferObject;
import io.github.syntaxpresso.core.common.extra.SupportedIDE;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import io.github.syntaxpresso.core.service.java.command.CreateJPARepositoryCommandService;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@RequiredArgsConstructor
@Command(
    name = "create-jpa-repository",
    description = "Create JPA Repository for the current JPA Entity.")
public class CreateJPARepositoryCommand
    implements Callable<DataTransferObject<CreateJPARepositoryResponse>> {
  private final CreateJPARepositoryCommandService createJPARepositoryCommandService;

  @Option(names = "--cwd", description = "Current Working Directory", required = true)
  private Path cwd;

  @Option(names = "--file-path", description = "The path to the file", required = true)
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
      names = "--superclass-source",
      description = "Source code for missing superclass when external symbol is required",
      required = false)
  private String superclassSource;

  @Override
  public DataTransferObject<CreateJPARepositoryResponse> call() {
    if (this.language.equals(SupportedLanguage.JAVA)) {
      return this.createJPARepositoryCommandService.run(
          this.cwd, this.filePath, this.language, this.ide, this.superclassSource);
    }
    return DataTransferObject.error("Language not supported.");
  }
}
