package io.github.syntaxpresso.core.service.java.language;

import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.service.java.language.extra.PackageCapture;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.treesitter.TSNode;

/**
 * Service for handling Java package declarations using Tree-sitter AST.
 * Provides functionality to analyze and extract information from package declarations in Java source files.
 *
 * <p>Example usage:
 * {@code
 * TSFile tsFile = new TSFile("path/to/file.java");
 * PackageDeclarationService service = new PackageDeclarationService();
 * 
 * // Get package declaration node
 * Optional<TSNode> packageNode = service.getPackageDeclarationNode(tsFile);
 * 
 * // Get package information with captures
 * // Captures used: @class_scope, @class_name, @package_scope, @package
 * List<Map<String, TSNode>> info = service.getPackageDeclarationInfo(tsFile, packageNode.get());
 * }
 */
public class PackageDeclarationService {
  /**
   * Gets the package declaration node from a Java file.
   *
   * @param tsFile The TSFile to analyze.
   * @return An Optional containing the package declaration node, or empty if not found.
   */
  public Optional<TSNode> getPackageDeclarationNode(TSFile tsFile) {
    if (tsFile == null || tsFile.getTree() == null) {
      return Optional.empty();
    }
    String queryString = "(package_declaration) @package";
    return tsFile.query(queryString).execute().firstNodeOptional();
  }

  /**
   * Retrieves detailed information about a package declaration using Tree-sitter queries.
   *
   * @param tsFile The TSFile containing the Java source code
   * @param packageDeclarationNode The package declaration node to analyze
   * @return A list of maps containing captured nodes with the following captures:
   *         - {@code @class_scope}: The scope part of the class identifier
   *         - {@code @class_name}: The name part of the class identifier
   *         - {@code @package_scope}: The complete package scope
   *         - {@code @package}: The entire package declaration
   */
  public List<Map<String, TSNode>> getPackageDeclarationInfo(
      TSFile tsFile, TSNode packageDeclarationNode) {
    if (tsFile == null
        || tsFile.getTree() == null
        || !packageDeclarationNode.getType().equals("package_declaration")) {
      return Collections.emptyList();
    }
    String queryString =
        String.format(
            """
                  (package_declaration
                    (scoped_identifier
                        scope: (_) %s
                          name: (_) %s
                      ) %s
                  ) %s
            """,
            PackageCapture.CLASS_SCOPE.getCaptureWithAt(),
            PackageCapture.CLASS_NAME.getCaptureWithAt(),
            PackageCapture.PACKAGE_SCOPE.getCaptureWithAt(),
            PackageCapture.PACKAGE.getCaptureWithAt());
    return tsFile
        .query(queryString)
        .within(packageDeclarationNode)
        .returningAllCaptures()
        .execute()
        .captures();
  }

  private Optional<TSNode> getPackageDeclarationChildByCapture(
      TSFile tsFile, TSNode packageDeclarationNode, PackageCapture capture) {
    if (tsFile == null
        || tsFile.getTree() == null
        || !packageDeclarationNode.getType().equals("package_declaration")) {
      return Optional.empty();
    }
    List<Map<String, TSNode>> nodeInfo =
        this.getPackageDeclarationInfo(tsFile, packageDeclarationNode);
    for (Map<String, TSNode> map : nodeInfo) {
      TSNode node = map.get(capture.getCaptureName());
      if (node != null) {
        return Optional.of(node);
      }
    }
    return Optional.empty();
  }

  /**
   * Gets the class name node from a package declaration node.
   *
   * @param tsFile The TSFile containing the Java source code
   * @param packageDeclarationNode The package declaration node to analyze
   * @return An Optional containing the class name node, or empty if not found
   */
  public Optional<TSNode> getPackageClassNameNode(TSFile tsFile, TSNode packageDeclarationNode) {
    return this.getPackageDeclarationChildByCapture(
        tsFile, packageDeclarationNode, PackageCapture.CLASS_NAME);
  }

  /**
   * Gets the class scope node from a package declaration node.
   *
   * @param tsFile The TSFile containing the Java source code
   * @param packageDeclarationNode The package declaration node to analyze
   * @return An Optional containing the class scope node, or empty if not found
   */
  public Optional<TSNode> getPackageClassScopeNode(TSFile tsFile, TSNode packageDeclarationNode) {
    return this.getPackageDeclarationChildByCapture(
        tsFile, packageDeclarationNode, PackageCapture.CLASS_SCOPE);
  }

  /**
   * Gets the package scope node from a package declaration node.
   *
   * @param tsFile The TSFile containing the Java source code
   * @param packageDeclarationNode The package declaration node to analyze
   * @return An Optional containing the package scope node, or empty if not found
   */
  public Optional<TSNode> getPackageScopeNode(TSFile tsFile, TSNode packageDeclarationNode) {
    return this.getPackageDeclarationChildByCapture(
        tsFile, packageDeclarationNode, PackageCapture.PACKAGE_SCOPE);
  }
}
