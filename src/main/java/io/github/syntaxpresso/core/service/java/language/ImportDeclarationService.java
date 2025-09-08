package io.github.syntaxpresso.core.service.java.language;

import com.google.common.base.Strings;
import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.service.java.language.extra.ImportCapture;
import io.github.syntaxpresso.core.service.java.language.extra.ImportInsertionPoint;
import io.github.syntaxpresso.core.service.java.language.extra.ImportInsertionPoint.ImportInsertionPosition;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.Getter;
import org.treesitter.TSNode;

/**
 * Service for analyzing and manipulating import declarations in Java source code using tree-sitter.
 *
 * <p>This service provides comprehensive functionality for working with import declarations within
 * Java source files, including extraction of import information, finding import statements,
 * checking for existing imports, and performing import-related transformations. It leverages
 * tree-sitter queries to accurately parse and analyze import declarations at the AST level.
 *
 * <p>Key capabilities include:
 *
 * <ul>
 *   <li>Extracting import scope, class name, and wildcard information
 *   <li>Finding all import declarations within a file
 *   <li>Locating specific imports by package and class name
 *   <li>Checking for wildcard imports vs specific class imports
 *   <li>Determining optimal insertion points for new imports
 *   <li>Adding and updating import statements
 * </ul>
 *
 * <p>Usage example:
 *
 * <pre>
 * ImportDeclarationService importService = new ImportDeclarationService();
 *
 * // Check if a class is already imported
 * boolean imported = importService.isClassImported(tsFile, "java.util", "List");
 * if (!imported) {
 *   // Add the import
 *   TSNode packageNode = packageService.getPackageDeclarationNode(tsFile).orElse(null);
 *   importService.addImport(tsFile, "java.util", "List", packageNode);
 * }
 *
 * // Find all imports in the file
 * List&lt;TSNode&gt; imports = importService.getAllImportDeclarationNodes(tsFile);
 * for (TSNode importNode : imports) {
 *   List&lt;Map&lt;String, TSNode&gt;&gt; info = importService.getImportDeclarationNodeInfo(tsFile, importNode);
 *   for (Map&lt;String, TSNode&gt; infoMap : info) {
 *     TSNode classNode = infoMap.get("class_name");
 *     if (classNode != null) {
 *       String className = tsFile.getTextFromNode(classNode);
 *       System.out.println("Imported class: " + className);
 *     }
 *   }
 * }
 * </pre>
 *
 * @see TSFile
 * @see ImportCapture
 * @see ImportInsertionPoint
 * @see PackageDeclarationService
 */
@Getter
public class ImportDeclarationService {
  private final PackageDeclarationService packageDeclarationService =
      new PackageDeclarationService();

  /**
   * Retrieves all import declaration nodes from the given Java source file.
   *
   * <p>This method finds all import statements within the source file, including both regular
   * imports (e.g., {@code import java.util.List;}) and wildcard imports (e.g., {@code import
   * java.util.*;}).
   *
   * <p>Usage example:
   *
   * <pre>
   * List&lt;TSNode&gt; imports = service.getAllImportDeclarationNodes(tsFile);
   * for (TSNode importNode : imports) {
   *   String importText = tsFile.getTextFromNode(importNode);
   *   // importText = "import java.util.List;"
   * }
   * </pre>
   *
   * @param tsFile The {@link TSFile} containing the Java source code
   * @return List of import declaration nodes, empty if none found or file/tree is null
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
   * <p>This method analyzes an import declaration node to extract information about its scope,
   * class name, and whether it's a wildcard import using tree-sitter queries.
   *
   * <p>Usage example:
   *
   * <pre>
   * List&lt;TSNode&gt; imports = service.getAllImportDeclarationNodes(tsFile);
   * List&lt;Map&lt;String, TSNode&gt;&gt; info = service.getImportDeclarationNodeInfo(tsFile, imports.get(0));
   * for (Map&lt;String, TSNode&gt; infoMap : info) {
   *   TSNode fullScope = infoMap.get("full_import_scope");     // e.g., "java.util.List"
   *   TSNode relativeScope = infoMap.get("relative_import_scope"); // e.g., "java.util"
   *   TSNode className = infoMap.get("class_name");            // e.g., "List"
   *   TSNode asterisk = infoMap.get("asterisk");               // present for wildcard imports
   * }
   * </pre>
   *
   * Query captures: - full_import_scope: The complete import path - relative_import_scope: The
   * package portion of the import - class_name: The class name portion - asterisk: Present for
   * wildcard imports - import: The entire import declaration
   *
   * @param tsFile The {@link TSFile} containing the Java source code
   * @param importDeclarationNode The import declaration {@link TSNode} to analyze
   * @return List of maps containing captured nodes, empty if invalid input
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
   * <p>This method extracts a specific component of an import declaration based on the provided
   * capture type. It can retrieve the import scope, class name, or other components.
   *
   * <p>Usage example:
   *
   * <pre>
   * List&lt;TSNode&gt; imports = service.getAllImportDeclarationNodes(tsFile);
   * Optional&lt;TSNode&gt; classNameNode = service.getImportDeclarationChildByCaptureName(
   *     tsFile, imports.get(0), ImportCapture.CLASS_NAME);
   * if (classNameNode.isPresent()) {
   *   String className = tsFile.getTextFromNode(classNameNode.get());
   *   // className = "List" from "import java.util.List;"
   * }
   * </pre>
   *
   * @param tsFile The {@link TSFile} containing the Java source code
   * @param importDeclarationNode The import declaration {@link TSNode} to search within
   * @param capture The specific {@link ImportCapture} type to retrieve
   * @return Optional containing the requested node, empty if not found or invalid input
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
   * Finds the import declaration node for a given package scope and class name.
   *
   * <p>This method searches for an import declaration that matches the specified package scope and
   * class name. It handles both regular imports (exact class match) and wildcard imports (package
   * scope match with asterisk).
   *
   * <p>Usage example:
   *
   * <pre>
   * Optional&lt;TSNode&gt; importNode = service.findImportDeclarationNode(tsFile, "java.util", "List");
   * if (importNode.isPresent()) {
   *   String importText = tsFile.getTextFromNode(importNode.get());
   *   // importText = "import java.util.List;" or covered by "import java.util.*;"
   * }
   * </pre>
   *
   * @param tsFile The {@link TSFile} containing the Java source code
   * @param packageScope The package scope to search for (e.g., "java.util")
   * @param className The class name to search for (e.g., "List")
   * @return Optional containing the matching import declaration node, empty if not found or invalid
   *     input
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
   * Determines if the given import declaration node is a wildcard import.
   *
   * <p>This method checks whether an import declaration uses the wildcard syntax (asterisk) to
   * import all classes from a package, such as {@code import java.util.*;}.
   *
   * <p>Usage example:
   *
   * <pre>
   * List&lt;TSNode&gt; imports = service.getAllImportDeclarationNodes(tsFile);
   * for (TSNode importNode : imports) {
   *   boolean isWildcard = service.isWildCardImport(tsFile, importNode);
   *   if (isWildcard) {
   *     System.out.println("Found wildcard import");
   *   }
   * }
   * </pre>
   *
   * @param tsFile The {@link TSFile} containing the Java source code
   * @param importDeclarationNode The import declaration {@link TSNode} to check
   * @return true if the import is a wildcard import, false otherwise
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
   * <p>This method determines whether a specific class from a given package is already imported,
   * either through a direct import statement or through a wildcard import of the package.
   *
   * <p>Usage example:
   *
   * <pre>
   * boolean imported = service.isClassImported(tsFile, "java.util", "List");
   * if (!imported) {
   *   // Need to add import for java.util.List
   *   service.addImport(tsFile, "java.util", "List", packageNode);
   * }
   * </pre>
   *
   * @param tsFile The {@link TSFile} containing the Java source code
   * @param packageScope The package scope to check (e.g., "java.util")
   * @param className The class name to check (e.g., "List")
   * @return true if the class is imported (directly or via wildcard), false otherwise
   */
  public Boolean isClassImported(TSFile tsFile, String packageScope, String className) {
    return this.findImportDeclarationNode(tsFile, packageScope, className).isPresent();
  }

  /**
   * Retrieves the relative import scope node from an import declaration.
   *
   * <p>This method extracts the package portion of an import statement. For example, in {@code
   * import java.util.List;}, it would return the node representing "java.util".
   *
   * <p>Usage example:
   *
   * <pre>
   * List&lt;TSNode&gt; imports = service.getAllImportDeclarationNodes(tsFile);
   * Optional&lt;TSNode&gt; scopeNode = service.getImportDeclarationRelativeImportScopeNode(tsFile, imports.get(0));
   * if (scopeNode.isPresent()) {
   *   String scope = tsFile.getTextFromNode(scopeNode.get());
   *   // scope = "java.util" from "import java.util.List;"
   * }
   * </pre>
   *
   * @param tsFile The {@link TSFile} containing the Java source code
   * @param importDeclarationNode The import declaration {@link TSNode}
   * @return Optional containing the relative import scope node, empty if not found or invalid input
   */
  public Optional<TSNode> getImportDeclarationRelativeImportScopeNode(
      TSFile tsFile, TSNode importDeclarationNode) {
    return this.getImportDeclarationChildByCaptureName(
        tsFile, importDeclarationNode, ImportCapture.RELATIVE_IMPORT_SCOPE);
  }

  /**
   * Retrieves the class name node from an import declaration.
   *
   * <p>This method extracts the class name portion of an import statement. For example, in {@code
   * import java.util.List;}, it would return the node representing "List".
   *
   * <p>Usage example:
   *
   * <pre>
   * List&lt;TSNode&gt; imports = service.getAllImportDeclarationNodes(tsFile);
   * Optional&lt;TSNode&gt; classNameNode = service.getImportDeclarationClassNameNode(tsFile, imports.get(0));
   * if (classNameNode.isPresent()) {
   *   String className = tsFile.getTextFromNode(classNameNode.get());
   *   // className = "List" from "import java.util.List;"
   * }
   * </pre>
   *
   * @param tsFile The {@link TSFile} containing the Java source code
   * @param importDeclarationNode The import declaration {@link TSNode}
   * @return Optional containing the class name node, empty if not found or invalid input
   */
  public Optional<TSNode> getImportDeclarationClassNameNode(
      TSFile tsFile, TSNode importDeclarationNode) {
    return this.getImportDeclarationChildByCaptureName(
        tsFile, importDeclarationNode, ImportCapture.CLASS_NAME);
  }

  /**
   * Retrieves the full import scope node from an import declaration.
   *
   * <p>This method extracts the complete import path from an import statement. For regular imports
   * like {@code import java.util.List;}, it returns the entire "java.util.List". For wildcard
   * imports like {@code import java.util.*;}, it returns "java.util".
   *
   * <p>Usage example:
   *
   * <pre>
   * List&lt;TSNode&gt; imports = service.getAllImportDeclarationNodes(tsFile);
   * Optional&lt;TSNode&gt; fullScopeNode = service.getImportDeclarationFullImportScopeNode(tsFile, imports.get(0));
   * if (fullScopeNode.isPresent()) {
   *   String fullScope = tsFile.getTextFromNode(fullScopeNode.get());
   *   // fullScope = "java.util.List" or "java.util" for wildcard
   * }
   * </pre>
   *
   * @param tsFile The {@link TSFile} containing the Java source code
   * @param importDeclarationNode The import declaration {@link TSNode}
   * @return Optional containing the full import scope node, empty if not found or invalid input
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

    if (!existingImports.isEmpty()) {
      TSNode lastImportNode = existingImports.get(existingImports.size() - 1);
      insertPoint.setInsertByte(lastImportNode.getEndByte());
      insertPoint.setPosition(ImportInsertionPosition.AFTER_LAST_IMPORT);
    } else { // No existing imports
      if (packageDeclarationNode != null) {
        insertPoint.setInsertByte(packageDeclarationNode.getEndByte());
        insertPoint.setPosition(ImportInsertionPosition.AFTER_PACKAGE_DECLARATION);
      } else {
        insertPoint.setInsertByte(0);
        insertPoint.setPosition(ImportInsertionPosition.BEGINNING);
      }
    }
    return insertPoint;
  }

  /**
   * Adds an import statement for the specified class to the Java source file, if not already
   * imported.
   *
   * <p>This method intelligently inserts a new import statement at the appropriate location in the
   * file. It determines the insertion point based on existing package declarations and imports, and
   * only adds the import if the class is not already imported.
   *
   * <p>The insertion follows these rules:
   *
   * <ul>
   *   <li>After package declaration (with proper spacing) if no imports exist
   *   <li>At the beginning of file if no package or imports exist
   *   <li>After the last existing import if imports already exist
   * </ul>
   *
   * <p>Usage example:
   *
   * <pre>
   * // Check if import is needed and add it
   * if (!service.isClassImported(tsFile, "java.util", "List")) {
   *   TSNode packageNode = packageService.getPackageDeclarationNode(tsFile).orElse(null);
   *   service.addImport(tsFile, "java.util", "List", packageNode);
   *   // Adds "import java.util.List;" at the appropriate location
   * }
   * </pre>
   *
   * @param tsFile The {@link TSFile} containing the Java source code
   * @param packageScope The package scope to import from (e.g., "java.util")
   * @param className The class name to import (e.g., "List")
   * @param packageDeclarationNode The package declaration {@link TSNode} for determining insertion
   *     point, can be null
   */
  public void addImport(
      TSFile tsFile, String packageScope, String className, TSNode packageDeclarationNode) {
    if (this.isClassImported(tsFile, packageScope, className)) {
      return;
    }
    ImportInsertionPoint insertionPoint =
        this.getImportInsertionPoint(tsFile, packageDeclarationNode);
    if (insertionPoint == null) {
      return; // Should not happen with valid inputs, but good practice
    }
    String fullImportStatement = packageScope + "." + className;
    String importStatement;
    if (insertionPoint.getPosition().equals(ImportInsertionPosition.BEGINNING)) {
      // No package, no imports - insert at start
      importStatement = "import " + fullImportStatement + ";\n\n";
    } else if (insertionPoint
        .getPosition()
        .equals(ImportInsertionPosition.AFTER_PACKAGE_DECLARATION)) {
      importStatement = "\n\nimport " + fullImportStatement + ";";
    } else {
      // After existing imports
      importStatement = "\nimport " + fullImportStatement + ";";
    }
    tsFile.updateSourceCode(
        insertionPoint.getInsertByte(), insertionPoint.getInsertByte(), importStatement);
  }

  /**
   * Updates an existing import statement in the Java source file, changing the package scope and/or
   * class name.
   *
   * <p>This method modifies an existing import declaration by updating its package scope and class
   * name components. It's useful for refactoring operations where classes are moved between
   * packages or renamed.
   *
   * <p>Usage example:
   *
   * <pre>
   * // Update an import from old package/class to new package/class
   * boolean updated = service.updateImport(
   *     tsFile,
   *     "com.old.package", "com.new.package",
   *     "OldClass", "NewClass");
   * if (updated) {
   *   // Import was successfully updated from "import com.old.package.OldClass;"
   *   // to "import com.new.package.NewClass;"
   * }
   * </pre>
   *
   * @param tsFile The {@link TSFile} containing the Java source code
   * @param oldPackageScope The current package scope to replace
   * @param newPackageScope The new package scope to use
   * @param oldClassName The current class name to replace
   * @param newClassName The new class name to use
   * @return true if the import was successfully updated, false if not found or already exists
   */
  public boolean updateImport(
      TSFile tsFile,
      String oldPackageScope,
      String newPackageScope,
      String oldClassName,
      String newClassName) {
    if (this.isClassImported(tsFile, newPackageScope, newClassName)) {
      return false;
    }
    Optional<TSNode> existingImport =
        this.findImportDeclarationNode(tsFile, oldPackageScope, oldClassName);
    if (existingImport.isEmpty()) {
      return false;
    }

    TSNode importNode = existingImport.get();
    String newImportStatement = "import " + newPackageScope + "." + newClassName + ";";
    tsFile.updateSourceCode(importNode, newImportStatement);

    return true;
  }
}
