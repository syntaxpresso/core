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
  private List<TSNode> findAllImportDeclarations(TSFile file) {
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
  public boolean isClassImported(TSFile file, String className, String packageName) {
    Optional<String> currentFilePackageName =
        this.getPackageDeclarationService().getPackageName(file);
    // Checks if the class belongs to the same package. In this case,
    // It's automatically imported and could be used.
    if (currentFilePackageName.isPresent()) {
      if (packageName.equals(currentFilePackageName.get())) {
        return true;
      }
    }
    List<TSNode> allImportNodes = this.findAllImportDeclarations(file);
    for (TSNode importNode : allImportNodes) {
      Optional<TSNode> scopedIdentifier = this.getImportScopedIdentifier(importNode);
      if (scopedIdentifier.isPresent()) {
        String importScope = file.getTextFromNode(scopedIdentifier.get());
        if (importScope.contains(packageName)) {
          return true;
        }
      }
    }
    return false;
  }
}
