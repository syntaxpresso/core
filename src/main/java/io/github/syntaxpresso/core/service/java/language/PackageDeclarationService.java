package io.github.syntaxpresso.core.service.java.language;

import io.github.syntaxpresso.core.common.TSFile;
import java.util.Optional;
import org.treesitter.TSNode;

public class PackageDeclarationService {
  /**
   * Gets the package declaration node from a Java file.
   *
   * @param file The TSFile to analyze.
   * @return An Optional containing the package declaration node, or empty if not found.
   */
  public Optional<TSNode> getPackageDeclarationNode(TSFile file) {
    String packageQuery = "(package_declaration) @package";
    return file.query(packageQuery).stream().findFirst();
  }

  /**
   * Gets the package name from a Java file.
   *
   * @param file The TSFile to analyze.
   * @return An Optional containing the package name, or empty if not found.
   */
  public Optional<String> getPackageName(TSFile file) {
    String packageQuery = "(package_declaration (scoped_identifier) @package_name)";
    return file.query(packageQuery).stream().findFirst().map(file::getTextFromNode);
  }
}
