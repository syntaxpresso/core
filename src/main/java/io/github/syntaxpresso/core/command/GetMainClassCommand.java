// package io.github.syntaxpresso.core.command;
//
// import io.github.syntaxpresso.core.command.dto.GetMainClassResponse;
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
// @Command(name = "get-main-class", description = "Get Main class")
// public class GetMainClassCommand implements Callable<DataTransferObject<GetMainClassResponse>> {
//   private final JavaCommandService javaService;
//
//   @Option(names = "--cwd", description = "Current Working Directory", required = true)
//   private Path cwd;
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
//   public DataTransferObject<GetMainClassResponse> call() {
//     if (this.language.equals(SupportedLanguage.JAVA)) {
//       return this.javaService.getMainClass(this.cwd);
//     }
//     return DataTransferObject.error("Language not supported.");
//   }
// }
