package io.github.syntaxpresso.core.command.dto;

import java.io.Serializable;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RenameResponse implements Serializable {
  private String filePath;
  private int renamedNodes;
  private String newName;
}
