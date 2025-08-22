package io.github.syntaxpresso.core.command.dto;

import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import io.github.syntaxpresso.core.service.extra.JavaIdentifierType;
import java.io.Serializable;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GetCursorPositionInfoResponse implements Serializable {
  private String filePath;
  private SupportedLanguage language;
  private String node;
  private JavaIdentifierType nodeType;
  private String nodeText;
}
