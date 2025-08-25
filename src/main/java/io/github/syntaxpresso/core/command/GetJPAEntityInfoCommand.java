package io.github.syntaxpresso.core.command;

import io.github.syntaxpresso.core.command.dto.GetJPAEntityInfoResponse;
import io.github.syntaxpresso.core.common.DataTransferObject;
import io.github.syntaxpresso.core.common.extra.SupportedIDE;
import io.github.syntaxpresso.core.service.java.JavaCommandService;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@RequiredArgsConstructor
@Command(
    name = "get-jpa-entity-info",
    description = "Get necessary info of an specific Entity to create a JPA repository.")
public class GetJPAEntityInfoCommand
    implements Callable<DataTransferObject<GetJPAEntityInfoResponse>> {
  private final JavaCommandService javaCommandService;

  @Option(names = "--cwd", description = "Current Working Directory", required = true)
  private Path cwd;

  @Option(
      names = {"--file-path"},
      description = "The absolute path to the .java file.",
      required = true)
  private Path filePath;

  @Option(
      names = "--ide",
      description = "The IDE the command is being called from.",
      required = true)
  private SupportedIDE ide = SupportedIDE.NONE;

  @Override
  public DataTransferObject<GetJPAEntityInfoResponse> call() {
    return this.javaCommandService.getJPAEntityInfo(this.cwd, this.filePath, this.ide);
  }
}
