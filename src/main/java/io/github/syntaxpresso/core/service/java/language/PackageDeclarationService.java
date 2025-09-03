package io.github.syntaxpresso.core.service.java.language;

import io.github.syntaxpresso.core.command.extra.JavaSourceDirectoryType;
import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.service.java.language.extra.PackageCapture;
import io.github.syntaxpresso.core.util.PathHelper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.treesitter.TSNode;

/**
 * Service for handling Java package declarations using Tree-sitter AST. Provides functionality to
 * analyze and extract information from package declarations in Java source files.
 *
 * <p>Example usage: {@code TSFile tsFile = new TSFile("path/to/file.java");
 * PackageDeclarationService service = new PackageDeclarationService();
 *
 * <p><p><p><p><p>// Get package declaration node Optional<TSNode> packageNode =
 * service.getPackageDeclarationNode(tsFile);
 *
 * <p><p><p><p><p>// Get package information with captures // Captures
 * used: @class_scope, @class_name, @package_scope, @package List<Map<String, TSNode>> info =
 * service.getPackageDeclarationInfo(tsFile, packageNode.get()); }
 */
public class PackageDeclarationService {
  private final PathHelper pathHelper = new PathHelper();

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
   * @return A list of maps containing captured nodes with the following captures: -
   *     {@code @class_scope}: The scope part of the class identifier - {@code @class_name}: The
   *     name part of the class identifier - {@code @package_scope}: The complete package scope -
   *     {@code @package}: The entire package declaration
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

  /**
   * Resolves the file system path for a given package scope within the specified source directory type.
   *
   * <p>This method finds the appropriate source directory (main or test), converts the package scope
   * (e.g., "com.example.foo") to a directory path, and ensures the directory exists.
   *
   * <p>Usage example:
   * <pre>
   * Optional<Path> packageDir = service.getFilePathFromPackageScope(
   *     projectRoot, "com.example.foo", JavaSourceDirectoryType.MAIN);
   * if (packageDir.isPresent()) {
   *   // Use packageDir.get() as the directory for new source files
   * }
   * </pre>
   *
   * @param rootDir The root directory of the project.
   * @param packageScope The package scope as a dot-separated string (e.g., "com.example.foo").
   * @param sourceDirectoryType The type of source directory (main or test).
   * @return An {@link Optional} containing the resolved package directory path, or empty if not found or on error.
   */
  public Optional<Path> getFilePathFromPackageScope(
      Path rootDir, String packageScope, JavaSourceDirectoryType sourceDirectoryType) {
    if (rootDir == null || !Files.isDirectory(rootDir)) {
      return Optional.empty();
    }
    if (packageScope == null || packageScope.isBlank()) {
      return Optional.empty();
    }
    if (sourceDirectoryType == null) {
      return Optional.empty();
    }
    final String srcDirName =
        (sourceDirectoryType == JavaSourceDirectoryType.MAIN) ? "src/main/java" : "src/test/java";
    try {
      Optional<Path> sourceDirOptional =
          this.pathHelper.findDirectoryRecursively(rootDir, srcDirName);
      Path sourceDir;
      if (sourceDirOptional.isPresent()) {
        sourceDir = sourceDirOptional.get();
      } else {
        sourceDir = rootDir.resolve(srcDirName);
      }
      Path packageAsPath = Path.of(packageScope.replace('.', '/'));
      Path fullPackageDir = sourceDir.resolve(packageAsPath);
      Files.createDirectories(fullPackageDir);
      return Optional.of(fullPackageDir);
    } catch (IOException e) {
      return Optional.empty();
    }
  }
}
