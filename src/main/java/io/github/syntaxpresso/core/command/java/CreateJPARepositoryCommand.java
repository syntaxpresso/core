package io.github.syntaxpresso.core.command.java;

import io.github.syntaxpresso.core.command.java.dto.CreateNewJavaFileResponse;
import io.github.syntaxpresso.core.common.DataTransferObject;
import io.github.syntaxpresso.core.service.java.JavaService;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@RequiredArgsConstructor
@Command(
    name = "create-jpa-repository",
    description = "Create JPA Repository for the current JPA Entity.")
public class CreateJPARepositoryCommand implements Callable<DataTransferObject<CreateNewJavaFileResponse>> {
  private final JavaService javaService;

  @Option(names = "--cwd", description = "Current Working Directory", required = true)
  private Path cwd;

  @Option(names = "--file-path", description = "The path to the file", required = true)
  private Path filePath;

  @Override
  public DataTransferObject<CreateNewJavaFileResponse> call() {
    return this.javaService.createJPARepository(this.cwd, this.filePath);
  }
}
