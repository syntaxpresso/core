// package io.github.syntaxpresso.core.command;
//
// import io.github.syntaxpresso.core.command.dto.ParseSourceCodeResponse;
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
//     name = "parse-source-code",
//     description = "Parse source code. Usefull to provide external library files to the code.")
// public class ParseSourceCodeCommand
//     implements Callable<DataTransferObject<ParseSourceCodeResponse>> {
//   private final JavaCommandService javaCommandService;
//
//   @Option(
//       names = {"--source-code"},
//       description = "The absolute path to the .java file.",
//       required = true)
//   private String sourceCode;
//
//   @Option(
//       names = {"--file-path"},
//       description = "The absolute path to the .java file.",
//       required = true)
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
//   public DataTransferObject<ParseSourceCodeResponse> call() {
//     if (this.language.equals(SupportedLanguage.JAVA)) {
//       DataTransferObject<ParseSourceCodeResponse> response =
//           this.javaCommandService.parseSourceCommand(sourceCode, filePath, language, ide);
//       if (this.ide.equals(SupportedIDE.NONE)) {
//         System.out.println(response.getData().getSourceCode());
//         return response;
//       } else {
//         return DataTransferObject.success();
//       }
//     }
//     return DataTransferObject.error("Unable to parse source code.");
//   }
// }
