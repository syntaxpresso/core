package io.github.syntaxpresso.core.service.java.command.extra;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.treesitter.TSNode;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class IdFieldSearchResult {
  private final TSNode idFieldNode;
  private final String missingSuperclassName;
  private final boolean found;

  public static IdFieldSearchResult found(TSNode idFieldNode) {
    return new IdFieldSearchResult(idFieldNode, null, true);
  }

  public static IdFieldSearchResult missingSuperclass(String superclassName) {
    return new IdFieldSearchResult(null, superclassName, false);
  }

  public static IdFieldSearchResult notFound() {
    return new IdFieldSearchResult(null, null, false);
  }

  public boolean hasMissingSuperclass() {
    return missingSuperclassName != null;
  }
}