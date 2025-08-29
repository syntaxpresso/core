package io.github.syntaxpresso.core.command.dto;

import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.common.extra.SupportedIDE;
import java.io.Serializable;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParseSourceCodeResponse implements Serializable {
  TSFile file;
  private SupportedIDE ide;
}
