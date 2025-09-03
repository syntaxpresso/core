package io.github.syntaxpresso.core.service.java.language;

import com.google.common.base.Strings;
import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.service.java.language.extra.ImportCapture;
import io.github.syntaxpresso.core.service.java.language.extra.ImportInsertionPoint;
import io.github.syntaxpresso.core.service.java.language.extra.ImportInsertionPoint.ImprtInsertionPosition;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.Getter;
import org.treesitter.TSNode;

/**
 * Service responsible for handling Java import declarations in source files.
 * Provides functionality to query, analyze, add, and update import statements.
 */
@Getter
public class ImportDeclarationService {
  private final PackageDeclarationService packageDeclarationService =
      new PackageDeclarationService();

  /**
   * Retrieves all import declaration nodes from a source file.
   *
   * @param tsFile The TSFile representing the source code
   * @return A list of TSNode objects representing import declarations, or an empty list if none found
   *
   * <pre>
   * Example usage:
   * TSFile javaFile = new TSFile("/path/to/MyClass.java", "Java");
   * List<TSNode> imports = importDeclarationService.getAllImportDeclarationNodes(javaFile);
   * </pre>
   */
  public List<TSNode> getAllImportDeclarationNodes(TSFile tsFile) {
    if (tsFile == null || tsFile.getTree() == null) {
      return Collections.emptyList();
    }
    String queryString =
        """
        (import_declaration) @declaration
        """;
    return tsFile.query(queryString).execute().nodes();
  }

  /**
   * Retrieves detailed information about a specific import declaration node.
   * Parses the node structure to extract scopes, class names and other elements.
   *
   * @param tsFile The TSFile representing the source code
   * @param importDeclarationNode The import declaration node to analyze
   * @return A list of maps containing the captured nodes keyed by their capture names
   *
   * <pre>
   * Example usage:
   * TSFile javaFile = new TSFile("/path/to/MyClass.java", "Java");
   * TSNode importNode = importDeclarationService.getAllImportDeclarationNodes(javaFile).get(0);
   * List<Map<String, TSNode>> info = importDeclarationService.getImportDeclarationNodeInfo(javaFile, importNode);
   * 
   * Capture names used in query:
   * - {@link ImportCapture#RELATIVE_IMPORT_SCOPE}: The relative import scope
   * - {@link ImportCapture#CLASS_NAME}: The class name being imported
   * - {@link ImportCapture#FULL_IMPORT_SCOPE}: The full import scope
   * - {@link ImportCapture#ASTERISK}: The asterisk if present (for wildcard imports)
   * - {@link ImportCapture#IMPORT}: The entire import declaration
   * </pre>
   */
  public List<Map<String, TSNode>> getImportDeclarationNodeInfo(
      TSFile tsFile, TSNode importDeclarationNode) {
    if (tsFile == null
        || tsFile.getTree() == null
        || !importDeclarationNode.getType().equals("import_declaration")) {
      return Collections.emptyList();
    }
    String queryString =
        String.format(
            """
            (import_declaration
              (scoped_identifier
                  scope: (scoped_identifier) %s
                      name: (identifier) %s
                ) %s
                (asterisk)? %s
            ) %s
            """,
            ImportCapture.RELATIVE_IMPORT_SCOPE.getCaptureWithAt(),
            ImportCapture.CLASS_NAME.getCaptureWithAt(),
            ImportCapture.FULL_IMPORT_SCOPE.getCaptureWithAt(),
            ImportCapture.ASTERISK.getCaptureWithAt(),
            ImportCapture.IMPORT.getCaptureWithAt());
    return tsFile
        .query(queryString)
        .within(importDeclarationNode)
        .returningAllCaptures()
        .execute()
        .captures();
  }

  /**
   * Retrieves a specific child node from an import declaration based on the capture type.
   *
   * @param tsFile The TSFile representing the source code
   * @param importDeclarationNode The import declaration node to analyze
   * @param capture The specific capture type to retrieve
   * @return An Optional containing the node if found, or empty if not found
   *
   * <pre>
   * Example usage:
   * TSFile javaFile = new TSFile("/path/to/MyClass.java", "Java");
   * TSNode importNode = importDeclarationService.getAllImportDeclarationNodes(javaFile).get(0);
   * Optional<TSNode> classNameNode = importDeclarationService.getImportDeclarationChildByCaptureName(
   *     javaFile, importNode, ImportCapture.CLASS_NAME);
   * </pre>
   */
  public Optional<TSNode> getImportDeclarationChildByCaptureName(
      TSFile tsFile, TSNode importDeclarationNode, ImportCapture capture) {
    if (tsFile == null
        || tsFile.getTree() == null
        || !importDeclarationNode.getType().equals("import_declaration")) {
      return Optional.empty();
    }
    List<Map<String, TSNode>> nodeInfo =
        this.getImportDeclarationNodeInfo(tsFile, importDeclarationNode);
    for (Map<String, TSNode> map : nodeInfo) {
      TSNode node = map.get(capture.getCaptureName());
      if (node != null) {
        return Optional.of(node);
      }
    }
    return Optional.empty();
  }

  /**
   * Finds an import declaration node that matches the specified package and class name.
   *
   * @param tsFile The TSFile representing the source code
   * @param packageScope The package scope to search for
   * @param className The class name to search for
   * @return An Optional containing the matching import declaration node if found, or empty if not found
   *
   * <pre>
   * Example usage:
   * TSFile javaFile = new TSFile("/path/to/MyClass.java", "Java");
   * Optional<TSNode> importNode = importDeclarationService.findImportDeclarationNode(
   *     javaFile, "java.util", "List");
   * </pre>
   */
  public Optional<TSNode> findImportDeclarationNode(
      TSFile tsFile, String packageScope, String className) {
    if (tsFile == null
        || tsFile.getTree() == null
        || Strings.isNullOrEmpty(className)
        || Strings.isNullOrEmpty(packageScope)) {
      return Optional.empty();
    }
    List<TSNode> allImportDeclarationNodes = this.getAllImportDeclarationNodes(tsFile);
    for (TSNode importDeclarationNode : allImportDeclarationNodes) {
      List<Map<String, TSNode>> importDeclarationNodeInfo =
          this.getImportDeclarationNodeInfo(tsFile, importDeclarationNode);
      for (Map<String, TSNode> map : importDeclarationNodeInfo) {
        TSNode fullImportNode = map.get(ImportCapture.FULL_IMPORT_SCOPE.getCaptureName());
        TSNode classNameNode = map.get(ImportCapture.CLASS_NAME.getCaptureName());
        TSNode asteriskNode = map.get(ImportCapture.ASTERISK.getCaptureName());
        if (fullImportNode == null) {
          continue;
        }
        String fullImportText = tsFile.getTextFromNode(fullImportNode);
        if (asteriskNode != null) {
          if (packageScope.equals(fullImportText)) {
            return Optional.of(importDeclarationNode);
          }
        } else {
          if (classNameNode != null) {
            String classNameText = tsFile.getTextFromNode(classNameNode);
            if (className.equals(classNameText)) {
              return Optional.of(importDeclarationNode);
            }
          }
        }
      }
    }
    return Optional.empty();
  }

  /**
   * Checks if a specific class from a package is already imported in the source file.
   *
   * @param tsFile The TSFile representing the source code
   * @param packageScope The package scope to check
   * @param className The class name to check
   * @return True if the class is already imported, false otherwise
   *
   * <pre>
   * Example usage:
   * TSFile javaFile = new TSFile("/path/to/MyClass.java", "Java");
   * boolean isImported = importDeclarationService.isClassImported(javaFile, "java.util", "List");
   * </pre>
   */
  public Boolean isClassImported(TSFile tsFile, String packageScope, String className) {
    return this.findImportDeclarationNode(tsFile, packageScope, className).isPresent();
  }

  /**
   * Gets the relative import scope node from an import declaration.
   * This represents the part of the import before the class name.
   *
   * @param tsFile The TSFile representing the source code
   * @param importDeclarationNode The import declaration node to analyze
   * @return An Optional containing the relative import scope node if found, or empty if not found
   *
   * <pre>
   * Example usage:
   * TSFile javaFile = new TSFile("/path/to/MyClass.java", "Java");
   * TSNode importNode = importDeclarationService.getAllImportDeclarationNodes(javaFile).get(0);
   * Optional<TSNode> relativeScope = importDeclarationService.getImportDeclarationRelativeImportScopeNode(
   *     javaFile, importNode);
   * // For import java.util.List, the relative scope would be "java.util"
   * </pre>
   */
  public Optional<TSNode> getImportDeclarationRelativeImportScopeNode(
      TSFile tsFile, TSNode importDeclarationNode) {
    return this.getImportDeclarationChildByCaptureName(
        tsFile, importDeclarationNode, ImportCapture.RELATIVE_IMPORT_SCOPE);
  }

  /**
   * Gets the class name node from an import declaration.
   *
   * @param tsFile The TSFile representing the source code
   * @param importDeclarationNode The import declaration node to analyze
   * @return An Optional containing the class name node if found, or empty if not found
   *
   * <pre>
   * Example usage:
   * TSFile javaFile = new TSFile("/path/to/MyClass.java", "Java");
   * TSNode importNode = importDeclarationService.getAllImportDeclarationNodes(javaFile).get(0);
   * Optional<TSNode> classNameNode = importDeclarationService.getImportDeclarationClassNameNode(
   *     javaFile, importNode);
   * // For import java.util.List, the class name would be "List"
   * </pre>
   */
  public Optional<TSNode> getImportDeclarationClassNameNode(
      TSFile tsFile, TSNode importDeclarationNode) {
    return this.getImportDeclarationChildByCaptureName(
        tsFile, importDeclarationNode, ImportCapture.CLASS_NAME);
  }

  /**
   * Gets the full import scope node from an import declaration.
   * This represents the entire qualified name in the import statement.
   *
   * @param tsFile The TSFile representing the source code
   * @param importDeclarationNode The import declaration node to analyze
   * @return An Optional containing the full import scope node if found, or empty if not found
   *
   * <pre>
   * Example usage:
   * TSFile javaFile = new TSFile("/path/to/MyClass.java", "Java");
   * TSNode importNode = importDeclarationService.getAllImportDeclarationNodes(javaFile).get(0);
   * Optional<TSNode> fullScope = importDeclarationService.getImportDeclarationFullImportScopeNode(
   *     javaFile, importNode);
   * // For import java.util.List, the full scope would be "java.util.List"
   * </pre>
   */
  public Optional<TSNode> getImportDeclarationFullImportScopeNode(
      TSFile tsFile, TSNode importDeclarationNode) {
    return this.getImportDeclarationChildByCaptureName(
        tsFile, importDeclarationNode, ImportCapture.FULL_IMPORT_SCOPE);
  }

  /**
   * Determines the best position to insert a new import statement in the source file.
   *
   * @param tsFile The TSFile representing the source code
   * @param packageDeclarationNode The package declaration node, if present
   * @return An ImportInsertionPoint containing the position information for the new import
   */
  private ImportInsertionPoint getImportInsertionPoint(
      TSFile tsFile, TSNode packageDeclarationNode) {
    if (tsFile == null || tsFile.getTree() == null) {
      return null;
    }
    ImportInsertionPoint insertPoint = new ImportInsertionPoint();
    List<TSNode> existingImports = this.getAllImportDeclarationNodes(tsFile);
    if (existingImports.isEmpty()) {
      insertPoint.setInsertByte(packageDeclarationNode.getEndByte());
      insertPoint.setPosition(ImprtInsertionPosition.AFTER_PACKAGE_DECLARATION);
      return insertPoint;
    } else if (packageDeclarationNode == null) {
      insertPoint.setInsertByte(0);
      insertPoint.setPosition(ImprtInsertionPosition.BEGINNING);
      return insertPoint;
    }
    TSNode lastImportNode = existingImports.get(existingImports.size() - 1);
    insertPoint.setInsertByte(lastImportNode.getEndByte());
    insertPoint.setPosition(ImprtInsertionPosition.AFTER_LAST_IMPORT);
    return insertPoint;
  }

  /**
   * Adds a new import statement to the source file if it doesn't already exist.
   * Determines the appropriate position to insert the import based on existing imports
   * and package declaration.
   *
   * @param tsFile The TSFile representing the source code
   * @param packageScope The package scope to import from
   * @param className The class name to import
   * @param packageDeclarationNode The package declaration node, if present
   *
   * <pre>
   * Example usage:
   * TSFile javaFile = new TSFile("/path/to/MyClass.java", "Java");
   * PackageDeclarationService packageService = new PackageDeclarationService();
   * TSNode packageNode = packageService.getPackageDeclarationNode(javaFile).orElse(null);
   * importDeclarationService.addImport(javaFile, "java.util", "ArrayList", packageNode);
   * // This will add "import java.util.ArrayList;" to the file
   * </pre>
   */
  public void addImport(
      TSFile tsFile, String packageScope, String className, TSNode packageDeclarationNode) {
    if (this.isClassImported(tsFile, packageScope, className)) {
      return;
    }
    ImportInsertionPoint insertionPoint =
        this.getImportInsertionPoint(tsFile, packageDeclarationNode);
    String fullImportStatement = packageScope + "." + className;
    String importStatement = null;
    if (insertionPoint.getPosition().equals(ImprtInsertionPosition.AFTER_PACKAGE_DECLARATION)) {
      importStatement = "\n\nimport " + fullImportStatement + ";";
      tsFile.updateSourceCode(
          insertionPoint.getInsertByte(), insertionPoint.getInsertByte(), importStatement);
    } else if (insertionPoint.getPosition().equals(ImprtInsertionPosition.BEGINNING)) {
      // No package, no imports - insert at start
      importStatement = "import " + fullImportStatement + ";\n";
      tsFile.updateSourceCode(
          insertionPoint.getInsertByte(), insertionPoint.getInsertByte(), importStatement);
    } else {
      // After existing imports
      importStatement = "\nimport " + fullImportStatement + ";";
      tsFile.updateSourceCode(
          insertionPoint.getInsertByte(), insertionPoint.getInsertByte(), importStatement);
    }
  }

  /**
   * Updates an existing import statement in the source file.
   * Can modify both the package scope and class name parts of the import.
   *
   * @param tsFile The TSFile representing the source code
   * @param oldPackageScope The current package scope to find
   * @param newPackageScope The new package scope to replace with
   * @param oldClassName The current class name to find
   * @param newClassName The new class name to replace with
   * @return True if the import was successfully updated, false otherwise
   *
   * <pre>
   * Example usage:
   * TSFile javaFile = new TSFile("/path/to/MyClass.java", "Java");
   * boolean updated = importDeclarationService.updateImport(
   *     javaFile, "java.util", "java.util.concurrent", "List", "ConcurrentList");
   * // This will update "import java.util.List;" to "import java.util.concurrent.ConcurrentList;"
   * </pre>
   */
  public boolean updateImport(
      TSFile tsFile,
      String oldPackageScope,
      String newPackageScope,
      String oldClassName,
      String newClassName) {
    if (this.isClassImported(tsFile, oldPackageScope, newClassName)) {
      return false;
    }
    Optional<TSNode> existingImport =
        this.findImportDeclarationNode(tsFile, oldPackageScope, newClassName);
    if (existingImport.isEmpty()) {
      return false;
    }
    List<Map<String, TSNode>> existingImportInfo =
        this.getImportDeclarationNodeInfo(tsFile, existingImport.get());
    boolean isModified = false;
    for (Map<String, TSNode> map : existingImportInfo) {
      TSNode fullImportScopeNode = map.get(ImportCapture.FULL_IMPORT_SCOPE.getCaptureName());
      TSNode classNameNode = map.get(ImportCapture.CLASS_NAME.getCaptureName());
      TSNode asteriskNode = map.get(ImportCapture.ASTERISK.getCaptureName());
      if (fullImportScopeNode != null) {
        tsFile.updateSourceCode(fullImportScopeNode, newPackageScope);
        isModified = true;
      }
      if (asteriskNode != null && classNameNode != null) {
        tsFile.updateSourceCode(classNameNode, newClassName);
        isModified = true;
      }
    }
    return isModified;
  }
}
