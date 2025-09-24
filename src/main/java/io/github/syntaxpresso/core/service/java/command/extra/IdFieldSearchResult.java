package io.github.syntaxpresso.core.service.java.command.extra;

import io.github.syntaxpresso.core.common.TSFile;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.treesitter.TSNode;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class IdFieldSearchResult {
  private final TSFile tsFile;
  private final TSNode idFieldNode;
  private final String missingSuperclassName;
  private final boolean found;

  public static IdFieldSearchResult found(TSFile tsFile, TSNode idFieldNode) {
    return new IdFieldSearchResult(tsFile, idFieldNode, null, true);
  }

  public static IdFieldSearchResult missingSuperclass(String superclassName) {
    return new IdFieldSearchResult(null, null, superclassName, false);
  }

  public static IdFieldSearchResult notFound() {
    return new IdFieldSearchResult(null, null, null, false);
  }

  public boolean hasMissingSuperclass() {
    return missingSuperclassName != null;
  }
}
