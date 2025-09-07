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
 * Service for analyzing, extracting, and manipulating import declarations in Java source code using Tree-sitter parsing.
 * <p>
 * Provides methods to query, inspect, and update import statements, including support for wildcard imports and insertion points.
 */
@Getter
public class ImportDeclarationService {
  private final PackageDeclarationService packageDeclarationService =
      new PackageDeclarationService();

  /**
   * Retrieves all import declaration nodes from the given Java source file.
   *
   * @param tsFile The parsed Java source file
   * @return List of import_declaration nodes, or empty if none found or input is invalid
   * @example
   * <pre>{@code
   * TSFile tsFile = new TSFile("import java.util.List;\nclass A {}");
   * List<TSNode> imports = service.getAllImportDeclarationNodes(tsFile);
   * // Returns nodes for all import declarations
   * }</pre>
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
   * Extracts detailed information about a specific import declaration node.
   *
   * @param tsFile The parsed Java source file
   * @param importDeclarationNode The import_declaration node to analyze
   * @return List of maps containing captured nodes with keys: "full_import_scope", "relative_import_scope", "class_name", "asterisk", "import"
   * @example
   * <pre>{@code
   * TSFile tsFile = new TSFile("import org.example.Test;\nclass A {}");
   * TSNode importNode = ... // get import_declaration node
   * List<Map<String, TSNode>> info = service.getImportDeclarationNodeInfo(tsFile, importNode);
   * // Returns map with keys for each capture in the import
   * }</pre>
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
              [
                (scoped_identifier) %s
                (scoped_identifier
                  scope: (_) %s
                  name: (_) %s)
              ]
              (asterisk)? %s
            ) %s
            """,
            ImportCapture.FULL_IMPORT_SCOPE.getCaptureWithAt(),
            ImportCapture.RELATIVE_IMPORT_SCOPE.getCaptureWithAt(),
            ImportCapture.CLASS_NAME.getCaptureWithAt(),
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
   * Retrieves a specific child node from an import declaration by capture name.
   *
   * @param tsFile The parsed Java source file
   * @param importDeclarationNode The import_declaration node to search within
   * @param capture The specific capture type to retrieve (e.g., CLASS_NAME, FULL_IMPORT_SCOPE)
   * @return Optional containing the requested node, empty if not found or invalid input
   * @example
   * <pre>{@code
   * TSFile tsFile = new TSFile("import org.example.Test;\nclass A {}");
   * TSNode importNode = ... // get import_declaration node
   * Optional<TSNode> classNameNode = service.getImportDeclarationChildByCaptureName(tsFile, importNode, ImportCapture.CLASS_NAME);
   * // Returns TSNode for "Test"
   * }</pre>
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
   * Finds the import_declaration node for a given package scope and class name.
   * Handles both regular and wildcard imports.
   *
   * @param tsFile The parsed Java source file
   * @param packageScope The package scope (e.g., "org.example")
   * @param className The class name (e.g., "Test")
   * @return Optional containing the matching import_declaration node, or empty if not found
   * @example
   * <pre>{@code
   * TSFile tsFile = new TSFile("import org.example.Test;\nclass A {}");
   * Optional<TSNode> node = service.findImportDeclarationNode(tsFile, "org.example", "Test");
   * // Returns the import_declaration node for "org.example.Test"
   * }</pre>
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
        TSNode relativeImportScopeNode =
            map.get(ImportCapture.RELATIVE_IMPORT_SCOPE.getCaptureName());
        TSNode classNameNode = map.get(ImportCapture.CLASS_NAME.getCaptureName());
        TSNode asteriskNode = map.get(ImportCapture.ASTERISK.getCaptureName());

        // Handle wildcard imports (e.g., import org.example.*;)
        if (asteriskNode != null && fullImportNode != null) {
          String fullImportText = tsFile.getTextFromNode(fullImportNode);
          if (packageScope.equals(fullImportText)) {
            return Optional.of(importDeclarationNode);
          }
        }

        // Handle regular imports (e.g., import org.example.Test;)
        if (asteriskNode == null && classNameNode != null && relativeImportScopeNode != null) {
          String scopeText = tsFile.getTextFromNode(relativeImportScopeNode);
          String classText = tsFile.getTextFromNode(classNameNode);
          if (packageScope.equals(scopeText) && className.equals(classText)) {
            return Optional.of(importDeclarationNode);
          }
        }
      }
    }
    return Optional.empty();
  }

  /**
   * Determines if the given import_declaration node is a wildcard import (e.g., import org.example.*;).
   *
   * @param tsFile The parsed Java source file
   * @param importDeclarationNode The import_declaration node to check
   * @return true if the import is a wildcard import, false otherwise
   * @example
   * <pre>{@code
   * TSFile tsFile = new TSFile("import org.example.*;\nclass A {}");
   * TSNode importNode = ... // get import_declaration node
   * boolean isWildcard = service.isWildCardImport(tsFile, importNode);
   * // Returns true
   * }</pre>
   */
  public boolean isWildCardImport(TSFile tsFile, TSNode importDeclarationNode) {
    if (tsFile == null || tsFile.getTree() == null) {
      return false;
    }
    List<Map<String, TSNode>> importDeclarationNodeInfo =
        this.getImportDeclarationNodeInfo(tsFile, importDeclarationNode);
    for (Map<String, TSNode> map : importDeclarationNodeInfo) {
      TSNode asteriskNode = map.get(ImportCapture.ASTERISK.getCaptureName());
      if (asteriskNode != null) {
        return true;
      }
    }
    return false;
  }

  /**
   * Checks if a class is imported in the given Java source file.
   *
   * @param tsFile The parsed Java source file
   * @param packageScope The package scope (e.g., "org.example")
   * @param className The class name (e.g., "Test")
   * @return true if the class is imported, false otherwise
   * @example
   * <pre>{@code
   * TSFile tsFile = new TSFile("import org.example.Test;\nclass A {}");
   * boolean imported = service.isClassImported(tsFile, "org.example", "Test");
   * // Returns true
   * }</pre>
   */
  public Boolean isClassImported(TSFile tsFile, String packageScope, String className) {
    return this.findImportDeclarationNode(tsFile, packageScope, className).isPresent();
  }

  /**
   * Retrieves the relative import scope node from an import_declaration (e.g., "org.example" in "import org.example.Test;").
   *
   * @param tsFile The parsed Java source file
   * @param importDeclarationNode The import_declaration node
   * @return Optional containing the relative import scope node, or empty if not found
   */
  public Optional<TSNode> getImportDeclarationRelativeImportScopeNode(
      TSFile tsFile, TSNode importDeclarationNode) {
    return this.getImportDeclarationChildByCaptureName(
        tsFile, importDeclarationNode, ImportCapture.RELATIVE_IMPORT_SCOPE);
  }

  /**
   * Retrieves the class name node from an import_declaration (e.g., "Test" in "import org.example.Test;").
   *
   * @param tsFile The parsed Java source file
   * @param importDeclarationNode The import_declaration node
   * @return Optional containing the class name node, or empty if not found
   */
  public Optional<TSNode> getImportDeclarationClassNameNode(
      TSFile tsFile, TSNode importDeclarationNode) {
    return this.getImportDeclarationChildByCaptureName(
        tsFile, importDeclarationNode, ImportCapture.CLASS_NAME);
  }

  /**
   * Retrieves the full import scope node from an import_declaration (e.g., "org.example" in "import org.example.*;" or "import org.example.Test;").
   *
   * @param tsFile The parsed Java source file
   * @param importDeclarationNode The import_declaration node
   * @return Optional containing the full import scope node, or empty if not found
   */
  public Optional<TSNode> getImportDeclarationFullImportScopeNode(
      TSFile tsFile, TSNode importDeclarationNode) {
    return this.getImportDeclarationChildByCaptureName(
        tsFile, importDeclarationNode, ImportCapture.FULL_IMPORT_SCOPE);
  }

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
   * Adds an import statement for the specified class to the Java source file, if not already imported.
   *
   * @param tsFile The parsed Java source file
   * @param packageScope The package scope (e.g., "org.example")
   * @param className The class name (e.g., "Test")
   * @param packageDeclarationNode The package_declaration node (for determining insertion point)
   * @example
   * <pre>{@code
   * TSFile tsFile = new TSFile("package foo;\nclass A {}");
   * TSNode pkgNode = ... // get package_declaration node
   * service.addImport(tsFile, "org.example", "Test", pkgNode);
   * // Adds "import org.example.Test;" after the package declaration
   * }</pre>
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
   * Updates an existing import statement in the Java source file, changing the package scope and/or class name.
   *
   * @param tsFile The parsed Java source file
   * @param oldPackageScope The old package scope
   * @param newPackageScope The new package scope
   * @param oldClassName The old class name
   * @param newClassName The new class name
   * @return true if the import was updated, false otherwise
   * @example
   * <pre>{@code
   * TSFile tsFile = new TSFile("import org.example.Test;\nclass A {}");
   * boolean updated = service.updateImport(tsFile, "org.example", "org.new", "Test", "NewTest");
   * // Updates the import to "import org.new.NewTest;"
   * }</pre>
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
