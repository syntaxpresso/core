package io.github.syntaxpresso.core.command.dto;

import java.io.Serializable;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateNewFileResponse implements Serializable {
  private String filePath;
}
