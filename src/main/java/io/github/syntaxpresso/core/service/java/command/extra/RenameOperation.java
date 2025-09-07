package io.github.syntaxpresso.core.service.java.command.extra;

import io.github.syntaxpresso.core.common.TSFile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.treesitter.TSNode;

@Data
@Builder
@AllArgsConstructor
public class RenameOperation {
  private final TSFile tsFile;
  private final TSNode node;
  private final String text;
}
