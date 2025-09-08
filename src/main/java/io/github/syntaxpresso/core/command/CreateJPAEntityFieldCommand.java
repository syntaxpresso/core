// package io.github.syntaxpresso.core.command;
//
// import io.github.syntaxpresso.core.command.dto.CreateNewFileResponse;
// import io.github.syntaxpresso.core.common.DataTransferObject;
// import io.github.syntaxpresso.core.common.extra.SupportedIDE;
// import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
// import io.github.syntaxpresso.core.service.java.JavaCommandService;
// import java.nio.file.Path;
// import java.util.concurrent.Callable;
// import lombok.RequiredArgsConstructor;
// import picocli.CommandLine.Command;
// import picocli.CommandLine.Option;
//
// @RequiredArgsConstructor
// @Command(
//     name = "create-jpa-entity-field",
//     description = "Create JPA Repository for the current JPA Entity.")
// public class CreateJPAEntityFieldCommand
//     implements Callable<DataTransferObject<CreateNewFileResponse>> {
//   private final JavaCommandService javaCommandService;
//
//   @Option(names = "--cwd", description = "Current Working Directory", required = true)
//   private Path cwd;
//
//   @Option(names = "--file-path", description = "The path to the file", required = true)
//   private Path filePath;
//
//   @Option(
//       names = "--language",
//       description = "The language related to the command execution.",
//       required = true)
//   private SupportedLanguage language;
//
//   @Option(
//       names = "--ide",
//       description = "The IDE the command is being called from.",
//       required = true)
//   private SupportedIDE ide = SupportedIDE.NONE;
//
//   @Override
//   public DataTransferObject<CreateNewFileResponse> call() {
//     if (this.language.equals(SupportedLanguage.JAVA)) {
//       return this.javaCommandService.createJPARepository(this.cwd, this.filePath);
//     }
//     return DataTransferObject.error("Language not supported.");
//   }
// }
