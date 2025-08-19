package io.github.syntaxpresso.core.command;

import io.github.syntaxpresso.core.command.dto.GetMainClassResponse;
import io.github.syntaxpresso.core.common.DataTransferObject;
import io.github.syntaxpresso.core.service.java.JavaService;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@RequiredArgsConstructor
@Command(name = "get-main-class", description = "Get Main class")
public class GetMainClassCommand implements Callable<DataTransferObject<GetMainClassResponse>> {
  private final JavaService javaService;

  @Option(names = "--cwd", description = "Current Working Directory", required = true)
  private Path cwd;

  @Override
  public DataTransferObject<GetMainClassResponse> call() {
    return this.javaService.getMainClass(this.cwd);
  }
}
