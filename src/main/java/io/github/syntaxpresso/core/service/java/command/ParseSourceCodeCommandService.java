package io.github.syntaxpresso.core.service.java.command;

import com.google.common.base.Strings;
import io.github.syntaxpresso.core.command.dto.ParseSourceCodeResponse;
import io.github.syntaxpresso.core.common.DataTransferObject;
import io.github.syntaxpresso.core.common.extra.SupportedIDE;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ParseSourceCodeCommandService {

  private DataTransferObject<ParseSourceCodeResponse> validateArguments(
      String sourceCode, Path filePath) {
    if (Strings.isNullOrEmpty(sourceCode)) {
      return DataTransferObject.error("Invalid source code.");
    }
    if (!Files.exists(filePath)) {
      return DataTransferObject.error("File does not exist: " + filePath);
    }
    if (!filePath.toString().endsWith(".java")) {
      return DataTransferObject.error("File is not a .java file: " + filePath);
    }
    return DataTransferObject.success();
  }

  public DataTransferObject<ParseSourceCodeResponse> run(
      String sourceCode, Path filePath, SupportedLanguage language, SupportedIDE ide) {
    DataTransferObject<ParseSourceCodeResponse> validateArguments =
        this.validateArguments(sourceCode, filePath);
    if (!validateArguments.getSucceed()) {
      return validateArguments;
    }
    ParseSourceCodeResponse response =
        ParseSourceCodeResponse.builder()
            .ide(ide)
            .sourceCode(sourceCode)
            .filePath(filePath != null ? filePath.toString() : null)
            .parseSuccess(true)
            .build();
    return DataTransferObject.success(response);
  }
}
