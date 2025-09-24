package io.github.syntaxpresso.core.command.dto;

import io.github.syntaxpresso.core.service.java.command.extra.JPARepositoryData;
import java.io.Serializable;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateJPARepositoryResponse implements Serializable {
  private String filePath;
  private Boolean requiresSymbolSource;
  private String symbol;
  private JPARepositoryData repositoryData;
}
