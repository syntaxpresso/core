package io.github.syntaxpresso.core.command.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GetAllFilesResponse implements Serializable {
  @Builder.Default
  private List<FileResponse> response = new ArrayList<>();
}
