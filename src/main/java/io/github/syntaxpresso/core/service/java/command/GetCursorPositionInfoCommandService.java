package io.github.syntaxpresso.core.service.java.command;

import com.google.common.base.Strings;
import io.github.syntaxpresso.core.command.dto.GetCursorPositionInfoResponse;
import io.github.syntaxpresso.core.common.DataTransferObject;
import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.common.extra.SupportedIDE;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import io.github.syntaxpresso.core.service.extra.JavaIdentifierType;
import io.github.syntaxpresso.core.service.java.JavaLanguageService;
import io.github.syntaxpresso.core.util.PathHelper;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.treesitter.TSNode;

@Getter
@RequiredArgsConstructor
public class GetCursorPositionInfoCommandService {
  private final JavaLanguageService javaLanguageService;
  private final PathHelper pathHelper;

  private DataTransferObject<GetCursorPositionInfoResponse> validateArguments(
      Path filePath, Integer line, Integer column) {
    if (line == null || column == null) {
      return DataTransferObject.error("Invalind line or column.");
    }
    if (!Files.exists(filePath)) {
      return DataTransferObject.error("File does not exist: " + filePath);
    }
    if (!filePath.toString().endsWith(".java")) {
      return DataTransferObject.error("File is not a .java file: " + filePath);
    }
    return DataTransferObject.success();
  }

  public DataTransferObject<GetCursorPositionInfoResponse> run(
      Path filePath, SupportedLanguage language, SupportedIDE ide, Integer line, Integer column) {
    DataTransferObject<GetCursorPositionInfoResponse> validateArguments =
        this.validateArguments(filePath, line, column);
    if (!validateArguments.getSucceed()) {
      return validateArguments;
    }
    TSFile file = new TSFile(language, filePath);
    TSNode node = file.getNodeFromPosition(line, column, ide);
    if (node == null) {
      return DataTransferObject.error("No symbol found at the specified position.");
    }
    JavaIdentifierType identifierType = this.javaLanguageService.getIdentifierType(node, ide);
    if (identifierType == null) {
      return DataTransferObject.error(
          "Unable to determine symbol type at cursor position. Node type: "
              + node.getType()
              + ", Node text: '"
              + file.getTextFromNode(node)
              + "'");
    }
    String text;
    try {
      text = file.getTextFromRange(node.getStartByte(), node.getEndByte());
    } catch (Exception e) {
      return DataTransferObject.error("Error getting text from node: " + e.getMessage());
    }
    if (Strings.isNullOrEmpty(text)) {
      return DataTransferObject.error("Unable to determine current symbol name.");
    }
    GetCursorPositionInfoResponse response =
        GetCursorPositionInfoResponse.builder()
            .filePath(filePath.toString())
            .language(SupportedLanguage.JAVA)
            .node(node.toString())
            .nodeText(text)
            .nodeType(identifierType)
            .build();
    return DataTransferObject.success(response);
  }
}
