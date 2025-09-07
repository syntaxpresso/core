package io.github.syntaxpresso.core.service.java.command.extra;

import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.service.extra.JavaIdentifierType;
import java.nio.file.Path;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.treesitter.TSNode;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RenameSourceFileData {
  private Path cwd;
  private TSFile sourceFile;
  private TSNode sourcePackageNode;
  private TSNode sourcePackageScopeNode;
  private String sourcePackageScopeText;
  private TSNode sourceCursorPositionNode;
  private String sourceCursorPositionText;
  private JavaIdentifierType sourceCursorPositionType;
}
