package io.github.syntaxpresso.core.command.java;

import io.github.syntaxpresso.core.command.java.dto.GetMainClassResponse;
import io.github.syntaxpresso.core.common.DataTransferObject;
import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import io.github.syntaxpresso.core.service.JavaService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
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
  public DataTransferObject<GetMainClassResponse> call() throws Exception {
    boolean cwdExists = Files.exists(this.cwd);
    if (!cwdExists) {
      throw new IllegalArgumentException("Current working directory does not exist.");
    }
    List<TSFile> allFiles =
        this.javaService.getPathHelper().findFilesByExtention(this.cwd, SupportedLanguage.JAVA);
    for (TSFile file : allFiles) {
      boolean isMainClass = this.javaService.isMainClass(file);
      if (isMainClass) {
        Optional<String> packageName = this.javaService.getPackageName(file);
        if (packageName.isEmpty()) {
          return DataTransferObject.error(
              "Main class found, but package name couldn't be determined.");
        }
        GetMainClassResponse response = new GetMainClassResponse();
        response.setFilePath(file.getFile().getAbsolutePath());
        response.setPackageName(packageName.get());
        return DataTransferObject.success(response);
      }
    }
    return DataTransferObject.error(
        "Main class couldn't be found in the current working directory.");
  }
}
