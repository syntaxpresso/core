package io.github.syntaxpresso.core.command.dto;

import java.io.Serializable;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GetMainClassResponse implements Serializable {
  private String filePath;
  private String packageName;
  private String className;
}
