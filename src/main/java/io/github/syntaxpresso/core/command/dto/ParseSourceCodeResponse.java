package io.github.syntaxpresso.core.command.dto;

import io.github.syntaxpresso.core.common.extra.SupportedIDE;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import java.io.Serializable;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParseSourceCodeResponse implements Serializable {
  private SupportedIDE ide;
  private SupportedLanguage language;
  private String sourceCode;
  private String filePath;
  private boolean parseSuccess;
}
