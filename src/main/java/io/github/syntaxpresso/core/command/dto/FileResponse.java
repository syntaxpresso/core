package io.github.syntaxpresso.core.command.dto;

import java.io.Serializable;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileResponse implements Serializable {
  private String type;
  private String packagePath;
  private String filePath;
}
