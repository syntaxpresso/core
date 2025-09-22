package io.github.syntaxpresso.core.command;

import io.github.syntaxpresso.core.command.dto.CreateNewJPAEntityResponse;
import io.github.syntaxpresso.core.common.DataTransferObject;
import io.github.syntaxpresso.core.service.java.command.CreateNewJPAEntityCommandService;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.concurrent.Callable;

@RequiredArgsConstructor
@CommandLine.Command(name = "create-new-jpa-entity", description = "Create New JPA Entity")
public class CreateNewJPAEntityCommand
    implements Callable<DataTransferObject<CreateNewJPAEntityResponse>> {
  private final CreateNewJPAEntityCommandService createNewJPAEntityCommandService;

    @CommandLine.Option(names = "--cwd", description = "Current Working Directory", required = true)
    private Path cwd;

    @CommandLine.Option(
            names = "--package-name",
            description = "The package name for the new file",
            required = true)
    private String packageName;

    @CommandLine.Option(names = "--file-name", description = "The name of the new file", required = true)
    private String fileName;

  @Override
  public DataTransferObject<CreateNewJPAEntityResponse> call() throws Exception {
    return this.createNewJPAEntityCommandService.createNewJPAEntity(cwd, packageName, fileName);
  }
}
