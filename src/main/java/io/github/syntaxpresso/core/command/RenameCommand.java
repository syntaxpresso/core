package io.github.syntaxpresso.core.command;

import io.github.syntaxpresso.core.command.dto.RenameResponse;
import io.github.syntaxpresso.core.command.extra.SourceDirectoryType;
import io.github.syntaxpresso.core.common.DataTransferObject;
import io.github.syntaxpresso.core.service.java.JavaService;
import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine;
import picocli.CommandLine.Option;

@CommandLine.Command(
    name = "rename",
    description = "Rename a Java class/interface/enum and its file.",
    mixinStandardHelpOptions = true)
@RequiredArgsConstructor
public class RenameCommand implements Callable<DataTransferObject<RenameResponse>> {
  private final JavaService javaService;

  @Option(names = "--cwd", description = "Current Working Directory", required = true)
  private Path cwd;

  @CommandLine.Option(
      names = {"-f", "--file-path"},
      description = "The absolute path to the .java file.",
      required = true)
  private File filePath;

  @CommandLine.Option(
      names = {"-n", "--new-name"},
      description = "The new name for the class/interface/enum.",
      required = true)
  private String newName;

  @CommandLine.Option(
      names = {"-l", "--line"},
      description = "The cursor line",
      required = true)
  private Integer line;

  @CommandLine.Option(
      names = {"-c", "--column"},
      description = "The cursor column",
      required = true)
  private Integer column;

  @CommandLine.Option(
      names = {"-s", "--source-directory"},
      description = "The source directory type (e.g., MAIN, TEST).",
      defaultValue = "MAIN")
  private SourceDirectoryType sourceDirectoryType;

  @Override
  public DataTransferObject<RenameResponse> call() {
    return this.javaService.rename(this.filePath.toPath(), this.newName);
  }
}
