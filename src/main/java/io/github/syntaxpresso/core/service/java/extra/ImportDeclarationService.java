package io.github.syntaxpresso.core.service.java.extra;

import java.util.Optional;
import org.treesitter.TSNode;

public class ImportDeclarationService {
  public Optional<TSNode> getImportScopedIdentifier(TSNode importDeclarationNode) {
    if (importDeclarationNode == null
        || !"import_declaration".equals(importDeclarationNode.getType())) {
      return Optional.empty();
    }
    for (int i = 0; i < importDeclarationNode.getChildCount(); i++) {
      TSNode child = importDeclarationNode.getChild(i);
      if ("scoped_identifier".equals(child.getType())) {
        return Optional.of(child);
      }
    }
    return Optional.empty();
  }
}
