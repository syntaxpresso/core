package io.github.syntaxpresso.core.service.java.command.extra;

import java.nio.file.Path;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JPARepositoryData {
  private Path path;
  private Path cwd;
  private String packageName;
  private String entityIdType;
  private String entityType;
}
