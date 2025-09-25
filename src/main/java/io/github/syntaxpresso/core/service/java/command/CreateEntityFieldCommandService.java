package io.github.syntaxpresso.core.service.java.command;

import io.github.syntaxpresso.core.common.DataTransferObject;
import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.common.extra.SupportedIDE;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import io.github.syntaxpresso.core.service.java.JavaLanguageService;
import java.nio.file.Path;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.treesitter.TSNode;

@RequiredArgsConstructor
public class CreateEntityFieldCommandService {
  private final JavaLanguageService javaLanguageService;

  public DataTransferObject<Void> run(
      Path cwd, Path filePath, SupportedLanguage language, SupportedIDE ide) {
    TSFile tsFile = new TSFile(language, filePath);
    Optional<TSNode> publicClassNode =
        this.javaLanguageService.getClassDeclarationService().getPublicClass(tsFile);
    if (publicClassNode.isEmpty()) {
      return DataTransferObject.error("Unable to find public class node.");
    }

    return DataTransferObject.success();
  }
}
