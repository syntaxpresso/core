package io.github.syntaxpresso.core.service.java.command.extra;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.nio.file.Path;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY, setterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY)
public class JPARepositoryData {
  @JsonProperty("path")
  private Path path;
  
  @JsonProperty("cwd")
  private Path cwd;
  
  @JsonProperty("packageName")
  private String packageName;
  
  @JsonProperty("entityIdType")
  private String entityIdType;
  
  @JsonProperty("entityType")
  private String entityType;
}
