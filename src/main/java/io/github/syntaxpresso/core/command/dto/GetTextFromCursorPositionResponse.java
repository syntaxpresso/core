package io.github.syntaxpresso.core.command.dto;

import java.io.Serializable;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GetTextFromCursorPositionResponse implements Serializable {
  private String filePath;
  private String node;
  private String text;
}
