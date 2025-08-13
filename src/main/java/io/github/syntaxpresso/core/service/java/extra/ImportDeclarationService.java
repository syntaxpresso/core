package io.github.syntaxpresso.core.service.java.extra;

import io.github.syntaxpresso.core.common.TSFile;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import org.treesitter.TSNode;
import org.treesitter.TSQuery;
import org.treesitter.TSQueryCapture;
import org.treesitter.TSQueryCursor;
import org.treesitter.TSQueryMatch;

@Getter
public class ImportDeclarationService {
  private final PackageDeclarationService packageDeclarationService =
      new PackageDeclarationService();

  /**
   * Gets the scoped identifier from an import declaration node.
   *
   * @param importDeclarationNode The import declaration node.
   * @return An optional containing the scoped identifier node, or empty if not found.
   */
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

  /**
   * Finds all import declarations in a file.
   *
   * @param file The file to search in.
   * @return A list of all import declaration nodes.
   */
  public List<TSNode> findAllImportDeclarations(TSFile file) {
    List<TSNode> importNodes = new ArrayList<>();
    String importQuery = "(import_declaration) @import";
    TSQuery query = new TSQuery(file.getParser().getLanguage(), importQuery);
    TSQueryCursor cursor = new TSQueryCursor();
    cursor.exec(query, file.getTree().getRootNode());
    TSQueryMatch match = new TSQueryMatch();
    while (cursor.nextMatch(match)) {
      for (TSQueryCapture capture : match.getCaptures()) {
        importNodes.add(capture.getNode());
      }
    }
    return importNodes;
  }

  /**
   * Checks if a class is imported in a file.
   *
   * @param file The file to check.
   * @param className The name of the class.
   * @param packageName The name of the package.
   * @return True if the class is imported, false otherwise.
   */
  public Optional<TSNode> getImportDeclarationNode(
      TSFile file, String className, String packageName) {
    List<TSNode> allImportDeclarationNodes = this.findAllImportDeclarations(file);
    for (TSNode importDeclarationNode : allImportDeclarationNodes) {
      Optional<TSNode> scopedIdentifier =
          file.findChildNodeByType(importDeclarationNode, "scoped_identifier");
      if (scopedIdentifier.isEmpty()) {
        continue;
      }
      String scopeName = file.getTextFromNode(scopedIdentifier.get());
      String importPackage = packageName + "." + className;
      if (scopeName.equals(importPackage) || scopeName.equals(packageName)) {
        return Optional.of(importDeclarationNode);
      }
    }
    return Optional.empty();
  }
}
