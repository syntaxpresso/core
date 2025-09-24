package io.github.syntaxpresso.core.command.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.syntaxpresso.core.service.java.command.extra.JPARepositoryData;
import java.io.Serializable;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY, setterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY)
public class CreateJPARepositoryResponse implements Serializable {
  @JsonProperty("filePath")
  private String filePath;
  
  @JsonProperty("requiresSymbolSource")
  private Boolean requiresSymbolSource;
  
  @JsonProperty("symbol")
  private String symbol;
  
  @JsonProperty("repositoryData")
  private JPARepositoryData repositoryData;
}
