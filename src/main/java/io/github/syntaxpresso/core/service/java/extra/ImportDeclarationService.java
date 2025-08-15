package io.github.syntaxpresso.core.service.java.extra;

import io.github.syntaxpresso.core.common.TSFile;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import org.treesitter.TSNode;

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
    return file.query("(import_declaration) @import");
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

  /**
   * Checks if an import already exists in the file.
   *
   * @param file The file to check.
   * @param importStatement The full import statement to check for.
   * @return True if the import exists, false otherwise.
   */
  private boolean importExists(TSFile file, String importStatement) {
    List<TSNode> allImportDeclarationNodes = findAllImportDeclarations(file);
    for (TSNode importDeclarationNode : allImportDeclarationNodes) {
      String importText = file.getTextFromNode(importDeclarationNode);
      if (importStatement.endsWith(".*")) {
        // For wildcard imports, check if the import text contains the wildcard
        if (importText.contains(importStatement)) {
          return true;
        }
      } else {
        // For regular imports, check scoped identifier
        Optional<TSNode> scopedIdentifier =
            file.findChildNodeByType(importDeclarationNode, "scoped_identifier");
        if (scopedIdentifier.isPresent()) {
          String scopeName = file.getTextFromNode(scopedIdentifier.get());
          if (scopeName.equals(importStatement)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  /**
   * Adds an import statement to a Java file.
   *
   * @param file The file to add the import to.
   * @param packageName The package name.
   * @param className The class name.
   */
  public void addImport(TSFile file, String packageName, String className) {
    String fullImport = packageName + "." + className;
    String wildcardImport = packageName + ".*";
    if (importExists(file, fullImport) || importExists(file, wildcardImport)) {
      return;
    }
    List<TSNode> existingImports = findAllImportDeclarations(file);
    Optional<TSNode> packageNode = this.packageDeclarationService.getPackageDeclarationNode(file);
    if (existingImports.isEmpty() && packageNode.isPresent()) {
      // Package exists but no imports - insert after package with proper spacing
      TSNode packageDeclaration = packageNode.get();
      int packageEnd = packageDeclaration.getEndByte();
      String sourceCode = file.getSourceCode();
      // Find position right after the first newline (start of blank line or content)
      int firstNewline = sourceCode.indexOf('\n', packageEnd);
      if (firstNewline == -1) firstNewline = sourceCode.length();
      // Insert import preserving the blank line structure
      String importStatement = "\nimport " + fullImport + ";\n";
      file.updateSourceCode(firstNewline + 1, firstNewline + 1, importStatement);
    } else if (existingImports.isEmpty()) {
      // No package, no imports - insert at start
      String importStatement = "import " + fullImport + ";\n\n";
      file.updateSourceCode(0, 0, importStatement);
    } else {
      // After existing imports
      TSNode lastImport = existingImports.get(existingImports.size() - 1);
      int insertionPoint = lastImport.getEndByte();
      String importStatement = "\nimport " + fullImport + ";";
      file.updateSourceCode(insertionPoint, insertionPoint, importStatement);
    }
  }

  /**
   * Adds an import statement using a full package name.
   *
   * @param file The file to add the import to.
   * @param fullPackageName The full package name (e.g., "java.util.List").
   */
  public void addImport(TSFile file, String fullPackageName) {
    int lastDotIndex = fullPackageName.lastIndexOf('.');
    if (lastDotIndex == -1) {
      throw new IllegalArgumentException("Invalid full package name: " + fullPackageName);
    }
    String packageName = fullPackageName.substring(0, lastDotIndex);
    String className = fullPackageName.substring(lastDotIndex + 1);
    addImport(file, packageName, className);
  }

  /**
   * Adds a wildcard import statement to a Java file.
   *
   * @param file The file to add the import to.
   * @param packageName The package name to import with wildcard.
   */
  public void addImportWildcard(TSFile file, String packageName) {
    String wildcardImport = packageName + ".*";
    if (importExists(file, wildcardImport)) {
      return;
    }
    List<TSNode> existingImports = findAllImportDeclarations(file);
    Optional<TSNode> packageNode = this.packageDeclarationService.getPackageDeclarationNode(file);
    if (existingImports.isEmpty() && packageNode.isPresent()) {
      // Package exists but no imports - insert after package with proper spacing
      TSNode packageDeclaration = packageNode.get();
      int packageEnd = packageDeclaration.getEndByte();
      String sourceCode = file.getSourceCode();
      // Find position right after the first newline (start of blank line or content)
      int firstNewline = sourceCode.indexOf('\n', packageEnd);
      if (firstNewline == -1) firstNewline = sourceCode.length();
      // Insert import preserving the blank line structure
      String importStatement = "\nimport " + wildcardImport + ";\n";
      file.updateSourceCode(firstNewline + 1, firstNewline + 1, importStatement);
    } else if (existingImports.isEmpty()) {
      // No package, no imports - insert at start
      String importStatement = "import " + wildcardImport + ";\n\n";
      file.updateSourceCode(0, 0, importStatement);
    } else {
      // After existing imports
      TSNode lastImport = existingImports.get(existingImports.size() - 1);
      int insertionPoint = lastImport.getEndByte();
      String importStatement = "\nimport " + wildcardImport + ";";
      file.updateSourceCode(insertionPoint, insertionPoint, importStatement);
    }
  }

  /**
   * Updates an existing import declaration to use a new class name. If a wildcard import exists for
   * the same package, no update is performed.
   *
   * @param file The file to update the import in.
   * @param oldFullImport The current full import (e.g., "org.example.Test").
   * @param newFullImport The new full import (e.g., "org.example.NewName").
   * @return True if the import was updated, false if no update was needed or possible.
   */
  public boolean updateImport(TSFile file, String oldFullImport, String newFullImport) {
    // Parse old import
    int oldLastDotIndex = oldFullImport.lastIndexOf('.');
    if (oldLastDotIndex == -1) {
      throw new IllegalArgumentException("Invalid old import: " + oldFullImport);
    }
    String oldPackage = oldFullImport.substring(0, oldLastDotIndex);
    // Parse new import
    int newLastDotIndex = newFullImport.lastIndexOf('.');
    if (newLastDotIndex == -1) {
      throw new IllegalArgumentException("Invalid new import: " + newFullImport);
    }
    // Check if wildcard import exists for old package
    String wildcardImport = oldPackage + ".*";
    if (importExists(file, wildcardImport)) {
      // Wildcard import exists, no need to update
      return false;
    }
    // Find the specific import to update
    List<TSNode> allImportDeclarationNodes = this.findAllImportDeclarations(file);
    for (TSNode importDeclarationNode : allImportDeclarationNodes) {
      Optional<TSNode> scopedIdentifier =
          file.findChildNodeByType(importDeclarationNode, "scoped_identifier");
      if (scopedIdentifier.isPresent()) {
        String importText = file.getTextFromNode(scopedIdentifier.get());
        if (importText.equals(oldFullImport)) {
          // Found the import to update
          file.updateSourceCode(scopedIdentifier.get(), newFullImport);
          return true;
        }
      }
    }
    // Import not found
    return false;
  }
}
