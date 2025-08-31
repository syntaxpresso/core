// package io.github.syntaxpresso.core.service.java.language;
//
// import io.github.syntaxpresso.core.common.TSFile;
// import io.github.syntaxpresso.core.service.java.language.extra.ImportInsertionPoint;
// import
// io.github.syntaxpresso.core.service.java.language.extra.ImportInsertionPoint.ImprtInsertionPosition;
// import java.util.ArrayList;
// import java.util.LinkedHashMap;
// import java.util.List;
// import java.util.Map;
// import java.util.Optional;
// import lombok.Getter;
// import org.treesitter.TSNode;
//
// @Getter
// public class ImportDeclarationService {
//   private final PackageDeclarationService packageDeclarationService =
//       new PackageDeclarationService();
//
//   /**
//    * Checks if a wildcard import already exists for the given package.
//    *
//    * <p>This helper method searches through all import declarations to determine if there's
// already
//    * a wildcard import (e.g., {@code import java.util.*;}) for the specified package. This is
// used
//    * to avoid adding redundant wildcard imports.
//    *
//    * <p>Example usage:
//    *
//    * <pre>{@code
//    * TSFile javaFile = new TSFile("Test.java", "import java.util.*;");
//    * ImportDeclarationService service = new ImportDeclarationService();
//    *
//    * boolean hasWildcard = service.hasWildcardImport(javaFile, "java.util"); // true
//    * boolean hasIO = service.hasWildcardImport(javaFile, "java.io"); // false
//    * }</pre>
//    *
//    * @param file The file to check for wildcard imports
//    * @param packageName The package name to search for (e.g., "java.util")
//    * @return true if a wildcard import exists for the package, false otherwise
//    */
//   private boolean hasWildcardImport(TSFile file, String packageName) {
//     return this.getAllImportDeclarations(file).stream()
//         .anyMatch(
//             map ->
//                 map.containsKey("isWildCard")
//                     && file.getTextFromNode(map.get("package")).equals(packageName));
//   }
//
//   /**
//    * Determines the optimal insertion point for new import statements in a Java file.
//    *
//    * <p>This method analyzes the current file structure to determine where a new import statement
//    * should be inserted according to Java conventions:
//    *
//    * <ul>
//    *   <li>After the package declaration (if present) and before any existing imports
//    *   <li>At the beginning of the file if no package declaration exists
//    *   <li>After the last existing import statement
//    * </ul>
//    *
//    * <p>Example usage:
//    *
//    * <pre>{@code
//    * TSFile javaFile = new TSFile("Test.java",
//    *     "package com.example;\n\nimport java.util.List;\n\nclass Test {}");
//    * ImportDeclarationService service = new ImportDeclarationService();
//    *
//    * ImportInsertionPoint point = service.getImportInsertionPoint(javaFile);
//    * // point.getPosition() == AFTER_LAST_IMPORT
//    * // point.getInsertByte() == byte position after "import java.util.List;"
//    * }</pre>
//    *
//    * @param file The file to analyze for insertion point
//    * @return An ImportInsertionPoint containing the byte position and context information
//    */
//   private ImportInsertionPoint getImportInsertionPoint(TSFile file) {
//     ImportInsertionPoint insertPoint = new ImportInsertionPoint();
//     List<Map<String, TSNode>> existingImports = this.getAllImportDeclarations(file);
//     Optional<TSNode> packageNode =
// this.packageDeclarationService.getPackageDeclarationNode(file);
//     if (existingImports.isEmpty() && packageNode.isPresent()) {
//       TSNode packageDeclaration = packageNode.get();
//       insertPoint.setInsertByte(packageDeclaration.getEndByte());
//       insertPoint.setPosition(ImprtInsertionPosition.AFTER_PACKAGE_DECLARATION);
//       return insertPoint;
//     } else if (existingImports.isEmpty()) {
//       insertPoint.setInsertByte(0);
//       insertPoint.setPosition(ImprtInsertionPosition.BEGINNING);
//       return insertPoint;
//     }
//     Map<String, TSNode> lastImportMap = existingImports.get(existingImports.size() - 1);
//     TSNode lastImportNode = lastImportMap.get("importDeclaration");
//     insertPoint.setInsertByte(lastImportNode.getEndByte());
//     insertPoint.setPosition(ImprtInsertionPosition.AFTER_LAST_IMPORT);
//     return insertPoint;
//   }
//
//   /**
//    * Retrieves all import declarations from a Java source file using Tree-sitter query patterns.
//    * This method captures both regular class imports and wildcard imports by parsing the syntax
//    * tree.
//    *
//    * <p>The method uses a Tree-sitter query to find import declaration nodes and their
// components:
//    *
//    * <ul>
//    *   <li>For regular imports: captures the package scope and class name separately
//    *   <li>For wildcard imports: captures the package and the wildcard asterisk
//    * </ul>
//    *
//    * <p>Example usage:
//    *
//    * <pre>{@code
//    * TSFile javaFile = new TSFile("MyClass.java", sourceCode);
//    * ImportDeclarationService service = new ImportDeclarationService();
//    * List<Map<String, TSNode>> importNodes = service.getAllImportDeclarations(javaFile);
//    *
//    * for (Map<String, TSNode> map : importNodes) {
//    *   boolean isWildCardImport = map.containsKey("isWildCard");
//    *   if (isWildCardImport) {
//    *     TSNode scopeNode = map.get("package");
//    *     String scopeText = javaFile.getTextFromNode(scopeNode);
//    *     System.out.printf("Wildcard Import -> Package: %s%n", scopeText);
//    *   } else {
//    *     TSNode scopeNode = map.get("package");
//    *     TSNode nameNode = map.get("class");
//    *     String scopeText = javaFile.getTextFromNode(scopeNode);
//    *     String nameText = javaFile.getTextFromNode(nameNode);
//    *     System.out.printf("Single Import   -> Package: %s, Class: %s%n", scopeText, nameText);
//    *   }
//    * }
//    * }</pre>
//    *
//    * @param file The TSFile containing the Java source code to analyze
//    * @return A list of capture maps, where each map contains TSNode captures for import
// components.
//    *     Keys include "package", "class" (for regular imports), and "isWildCard" (for wildcard
//    *     imports). Returns empty list if no import declarations are found.
//    */
//   public List<Map<String, TSNode>> getAllImportDeclarations(TSFile file) {
//     String regularImportsQuery =
//         "(import_declaration"
//             + "  (scoped_identifier"
//             + "    scope: (_) @package"
//             + "    name: (identifier) @class"
//             + "  )"
//             + ") @importDeclaration";
//     String wildcardImportsQuery =
//         "(import_declaration"
//             + "  (scoped_identifier) @package"
//             + "  (asterisk) @isWildCard"
//             + ") @importDeclaration";
//     List<Map<String, TSNode>> regularImports =
// file.query(regularImportsQuery).executeForCaptures();
//     List<Map<String, TSNode>> wildcardImports =
//         file.query(wildcardImportsQuery).executeForCaptures();
//     Map<String, Map<String, TSNode>> deduplicatedImports = new LinkedHashMap<>();
//     for (Map<String, TSNode> regularImport : regularImports) {
//       TSNode importDeclNode = regularImport.get("importDeclaration");
//       String key = importDeclNode.getStartByte() + "-" + importDeclNode.getEndByte();
//       deduplicatedImports.put(key, regularImport);
//     }
//     for (Map<String, TSNode> wildcardImport : wildcardImports) {
//       TSNode importDeclNode = wildcardImport.get("importDeclaration");
//       String key = importDeclNode.getStartByte() + "-" + importDeclNode.getEndByte();
//       deduplicatedImports.put(key, wildcardImport);
//     }
//     return new ArrayList<>(deduplicatedImports.values());
//   }
//
//   /**
//    * Finds an import declaration map for a specific class and package combination.
//    *
//    * <p>This method searches through all import declarations to find either a specific class
// import
//    * or a wildcard import that would cover the requested class. It returns the map containing the
//    * Tree-sitter nodes for the matching import.
//    *
//    * <p>The method will match:
//    *
//    * <ul>
//    *   <li>Exact class imports: {@code import java.util.List;} matches className="List",
//    *       packageName="java.util"
//    *   <li>Wildcard imports: {@code import java.util.*;} matches any className with
//    *       packageName="java.util"
//    * </ul>
//    *
//    * <p>Example usage:
//    *
//    * <pre>{@code
//    * TSFile javaFile = new TSFile("Test.java",
//    *     "import java.util.List;\nimport java.io.*;");
//    * ImportDeclarationService service = new ImportDeclarationService();
//    *
//    * // Find specific import
//    * Optional<Map<String, TSNode>> listImport =
//    *     service.getImportDeclarationMap(javaFile, "List", "java.util");
//    * if (listImport.isPresent()) {
//    *   // Found import java.util.List
//    * }
//    *
//    * // Find via wildcard
//    * Optional<Map<String, TSNode>> fileImport =
//    *     service.getImportDeclarationMap(javaFile, "File", "java.io");
//    * if (fileImport.isPresent()) {
//    *   // Found via import java.io.*
//    * }
//    * }</pre>
//    *
//    * @param file The file to search for imports
//    * @param className The name of the class to find
//    * @param packageName The name of the package containing the class
//    * @return An optional containing the import declaration map if found, empty otherwise
//    */
//   public Optional<Map<String, TSNode>> getImportDeclarationMap(
//       TSFile file, String className, String packageName) {
//     List<Map<String, TSNode>> importDeclarationMaps = this.getAllImportDeclarations(file);
//     for (Map<String, TSNode> map : importDeclarationMaps) {
//       // Check if it's a wildcard import first
//       if (map.containsKey("isWildCard")) {
//         TSNode packageNode = map.get("package");
//         String packageText = file.getTextFromNode(packageNode);
//         if (packageText.equals(packageName)) {
//           // Found a matching wildcard import
//           return Optional.of(map);
//         }
//       }
//       // If it's NOT a wildcard, it must be a single-class import
//       else {
//         TSNode packageNode = map.get("package");
//         TSNode classNode = map.get("class");
//         // This check prevents a NullPointerException if the query somehow fails
//         if (packageNode != null && classNode != null) {
//           String packageText = file.getTextFromNode(packageNode);
//           String classText = file.getTextFromNode(classNode);
//           if (packageText.equals(packageName) && classText.equals(className)) {
//             // Found an exact match for the class
//             return Optional.of(map);
//           }
//         }
//       }
//     }
//     return Optional.empty();
//   }
//
//   /**
//    * Adds an import statement to a Java file using separate package and class names.
//    *
//    * <p>This method adds a new import statement for the specified class if it doesn't already
// exist.
//    * It checks for both direct imports and wildcard imports that would cover the class. The
// import
//    * is inserted at the appropriate location according to Java conventions.
//    *
//    * <p>Behavior:
//    *
//    * <ul>
//    *   <li>Does nothing if the class is already imported (directly or via wildcard)
//    *   <li>Inserts the import with proper formatting and positioning
//    *   <li>Handles files with/without package declarations and existing imports
//    * </ul>
//    *
//    * <p>Example usage:
//    *
//    * <pre>{@code
//    * TSFile javaFile = new TSFile("Test.java", "package com.example;\n\nclass Test {}");
//    * ImportDeclarationService service = new ImportDeclarationService();
//    *
//    * service.addImport(javaFile, "java.util", "List");
//    * // Result: "package com.example;\n\nimport java.util.List;\n\nclass Test {}"
//    *
//    * service.addImport(javaFile, "java.util", "List"); // No-op, already exists
//    * }</pre>
//    *
//    * @param file The file to add the import to
//    * @param packageName The package name (e.g., "java.util")
//    * @param className The class name (e.g., "List")
//    */
//   public void addImport(TSFile file, String packageName, String className) {
//     Optional<Map<String, TSNode>> existingImport =
//         this.getImportDeclarationMap(file, className, packageName);
//     if (existingImport.isPresent()) {
//       return;
//     }
//     ImportInsertionPoint insertionPoint = this.getImportInsertionPoint(file);
//     String fullImportStatement = packageName + "." + className;
//     String importStatement = null;
//     if (insertionPoint.getPosition().equals(ImprtInsertionPosition.AFTER_PACKAGE_DECLARATION)) {
//       importStatement = "\n\nimport " + fullImportStatement + ";";
//       file.updateSourceCode(
//           insertionPoint.getInsertByte(), insertionPoint.getInsertByte(), importStatement);
//     } else if (insertionPoint.getPosition().equals(ImprtInsertionPosition.BEGINNING)) {
//       // No package, no imports - insert at start
//       importStatement = "import " + fullImportStatement + ";\n";
//       file.updateSourceCode(
//           insertionPoint.getInsertByte(), insertionPoint.getInsertByte(), importStatement);
//     } else {
//       // After existing imports
//       importStatement = "\nimport " + fullImportStatement + ";";
//       file.updateSourceCode(
//           insertionPoint.getInsertByte(), insertionPoint.getInsertByte(), importStatement);
//     }
//   }
//
//   /**
//    * Adds an import statement using a fully qualified class name.
//    *
//    * <p>This is a convenience overload that parses the fully qualified class name and delegates
// to
//    * the main {@link #addImport(TSFile, String, String)} method. The full package name is split
// at
//    * the last dot to separate the package from the class name.
//    *
//    * <p>Example usage:
//    *
//    * <pre>{@code
//    * TSFile javaFile = new TSFile("Test.java", "package com.example;\n\nclass Test {}");
//    * ImportDeclarationService service = new ImportDeclarationService();
//    *
//    * service.addImport(javaFile, "java.util.List");
//    * // Equivalent to: service.addImport(javaFile, "java.util", "List");
//    * // Result: "package com.example;\n\nimport java.util.List;\n\nclass Test {}"
//    * }</pre>
//    *
//    * @param file The file to add the import to
//    * @param fullPackageName The fully qualified class name (e.g., "java.util.List")
//    * @throws IllegalArgumentException if fullPackageName doesn't contain a dot separator
//    */
//   public void addImport(TSFile file, String fullPackageName) {
//     int lastDotIndex = fullPackageName.lastIndexOf('.');
//     if (lastDotIndex == -1) {
//       throw new IllegalArgumentException("Invalid full package name: " + fullPackageName);
//     }
//     String packageName = fullPackageName.substring(0, lastDotIndex);
//     String className = fullPackageName.substring(lastDotIndex + 1);
//     this.addImport(file, packageName, className);
//   }
//
//   /**
//    * Adds a wildcard import statement to a Java file.
//    *
//    * <p>This method adds a wildcard import (e.g., {@code import java.util.*;}) for the specified
//    * package if one doesn't already exist. Wildcard imports allow access to all public classes in
// a
//    * package without explicitly importing each class.
//    *
//    * <p>Behavior:
//    *
//    * <ul>
//    *   <li>Does nothing if a wildcard import for the package already exists
//    *   <li>Inserts the wildcard import with proper formatting and positioning
//    *   <li>Follows the same insertion rules as regular imports
//    * </ul>
//    *
//    * <p>Example usage:
//    *
//    * <pre>{@code
//    * TSFile javaFile = new TSFile("Test.java",
//    *     "package com.example;\n\nimport java.util.List;\n\nclass Test {}");
//    * ImportDeclarationService service = new ImportDeclarationService();
//    *
//    * service.addImportWildcard(javaFile, "java.io");
//    * // Result: "package com.example;\n\nimport java.util.List;\nimport java.io.*;\n\nclass Test
// {}"
//    *
//    * service.addImportWildcard(javaFile, "java.io"); // No-op, already exists
//    * }</pre>
//    *
//    * @param file The file to add the wildcard import to
//    * @param packageName The package name to import with wildcard (e.g., "java.util")
//    */
//   public void addImportWildcard(TSFile file, String packageName) {
//     if (hasWildcardImport(file, packageName)) {
//       return;
//     }
//     ImportInsertionPoint insertionPoint = this.getImportInsertionPoint(file);
//     String wildcardImportStatement = packageName + ".*";
//     String importStatement = null;
//     if (insertionPoint.getPosition().equals(ImprtInsertionPosition.AFTER_PACKAGE_DECLARATION)) {
//       importStatement = "\n\nimport " + wildcardImportStatement + ";";
//     } else if (insertionPoint.getPosition().equals(ImprtInsertionPosition.BEGINNING)) {
//       importStatement = "import " + wildcardImportStatement + ";\n";
//     } else {
//       importStatement = "\nimport " + wildcardImportStatement + ";";
//     }
//     file.updateSourceCode(
//         insertionPoint.getInsertByte(), insertionPoint.getInsertByte(), importStatement);
//   }
//
//   /**
//    * Updates an existing import declaration to use a new class name within the same package.
//    *
//    * <p>This method is specifically for class renaming scenarios where the package remains
//    * unchanged. It updates only the class identifier part of the import statement, making it
// ideal
//    * for refactoring operations like class renaming within the same package.
//    *
//    * <p>Behavior:
//    *
//    * <ul>
//    *   <li>Returns false if a wildcard import exists for the package (no update needed)
//    *   <li>Searches for the exact old class import and updates only the class name
//    *   <li>Returns true if the import was successfully updated
//    *   <li>Returns false if the old import was not found
//    *   <li>Validates that all parameters are non-null and non-empty
//    * </ul>
//    *
//    * <p>Example usage:
//    *
//    * <pre>{@code
//    * TSFile javaFile = new TSFile("Test.java",
//    *     "package com.example;\n\nimport org.springframework.UserService;\n\nclass Test {}");
//    * ImportDeclarationService service = new ImportDeclarationService();
//    *
//    * boolean updated = service.updateImportClassName(javaFile,
//    *     "org.springframework", "UserService", "CustomerService");
//    * // updated == true
//    * // Result: "package com.example;\n\nimport org.springframework.CustomerService;\n\nclass
// Test {}"
//    *
//    * // With wildcard import
//    * TSFile wildcardFile = new TSFile("Test.java", "import org.springframework.*;");
//    * boolean notUpdated = service.updateImportClassName(wildcardFile,
//    *     "org.springframework", "UserService", "CustomerService");
//    * // notUpdated == false (wildcard covers both old and new class names)
//    * }</pre>
//    *
//    * @param file The file to update the import in
//    * @param packageName The package name of the import (e.g., "org.springframework")
//    * @param oldClassName The current class name (e.g., "UserService")
//    * @param newClassName The new class name (e.g., "CustomerService")
//    * @return true if the import was updated, false if no update was needed or possible
//    * @throws IllegalArgumentException if any parameter is null or empty
//    */
//   public boolean updateImportClassName(
//       TSFile file, String packageName, String oldClassName, String newClassName) {
//     if (packageName == null || packageName.trim().isEmpty()) {
//       throw new IllegalArgumentException("Package name cannot be null or empty");
//     }
//     if (oldClassName == null || oldClassName.trim().isEmpty()) {
//       throw new IllegalArgumentException("Old class name cannot be null or empty");
//     }
//     if (newClassName == null || newClassName.trim().isEmpty()) {
//       throw new IllegalArgumentException("New class name cannot be null or empty");
//     }
//     if (hasWildcardImport(file, packageName)) {
//       return false;
//     }
//     Optional<Map<String, TSNode>> existingImport =
//         this.getImportDeclarationMap(file, oldClassName, packageName);
//     if (existingImport.isEmpty()) {
//       return false;
//     }
//     Map<String, TSNode> importMap = existingImport.get();
//     if (importMap.containsKey("isWildCard")) {
//       return false;
//     }
//     TSNode classNode = importMap.get("class");
//     if (classNode != null) {
//       file.updateSourceCode(classNode, newClassName);
//       return true;
//     }
//     return false;
//   }
//
//   /**
//    * Updates an existing import declaration to use a new package name for the same class.
//    *
//    * <p>This method is specifically for package refactoring scenarios where a class moves to a
//    * different package but keeps the same name. It updates only the package part of the import
//    * statement, making it ideal for package restructuring operations.
//    *
//    * <p>Behavior:
//    *
//    * <ul>
//    *   <li>Returns false if a wildcard import exists for the old package (no update needed)
//    *   <li>Searches for the exact class import in the old package and updates the package name
//    *   <li>Returns true if the import was successfully updated
//    *   <li>Returns false if the old import was not found
//    *   <li>Validates that all parameters are non-null and non-empty
//    * </ul>
//    *
//    * <p>Example usage:
//    *
//    * <pre>{@code
//    * TSFile javaFile = new TSFile("Test.java",
//    *     "package com.example;\n\nimport com.example.service.UserService;\n\nclass Test {}");
//    * ImportDeclarationService service = new ImportDeclarationService();
//    *
//    * boolean updated = service.updateImportPackageName(javaFile,
//    *     "com.example.service", "com.example.domain", "UserService");
//    * // updated == true
//    * // Result: "package com.example;\n\nimport com.example.domain.UserService;\n\nclass Test {}"
//    *
//    * // With wildcard import
//    * TSFile wildcardFile = new TSFile("Test.java", "import com.example.service.*;");
//    * boolean notUpdated = service.updateImportPackageName(wildcardFile,
//    *     "com.example.service", "com.example.domain", "UserService");
//    * // notUpdated == false (wildcard covers the class in old package)
//    * }</pre>
//    *
//    * @param file The file to update the import in
//    * @param oldPackageName The current package name (e.g., "com.example.service")
//    * @param newPackageName The new package name (e.g., "com.example.domain")
//    * @param className The class name that remains unchanged (e.g., "UserService")
//    * @return true if the import was updated, false if no update was needed or possible
//    * @throws IllegalArgumentException if any parameter is null or empty
//    */
//   public boolean updateImportPackageName(
//       TSFile file, String oldPackageName, String newPackageName, String className) {
//     if (oldPackageName == null || oldPackageName.trim().isEmpty()) {
//       throw new IllegalArgumentException("Old package name cannot be null or empty");
//     }
//     if (newPackageName == null || newPackageName.trim().isEmpty()) {
//       throw new IllegalArgumentException("New package name cannot be null or empty");
//     }
//     if (className == null || className.trim().isEmpty()) {
//       throw new IllegalArgumentException("Class name cannot be null or empty");
//     }
//     if (hasWildcardImport(file, oldPackageName)) {
//       return false;
//     }
//     Optional<Map<String, TSNode>> existingImport =
//         this.getImportDeclarationMap(file, className, oldPackageName);
//     if (existingImport.isEmpty()) {
//       return false;
//     }
//     Map<String, TSNode> importMap = existingImport.get();
//     if (importMap.containsKey("isWildCard")) {
//       return false;
//     }
//     TSNode packageNode = importMap.get("package");
//     if (packageNode != null) {
//       file.updateSourceCode(packageNode, newPackageName);
//       return true;
//     }
//     return false;
//   }
//
//   /**
//    * Updates an existing import declaration to use a new fully qualified class name.
//    *
//    * <p>This method handles cases where both package and class name change. It's equivalent to
//    * removing the old import and adding a new one, but done as an atomic update operation. This
// is
//    * useful for comprehensive refactoring operations like moving and renaming a class
//    * simultaneously.
//    *
//    * <p>Behavior:
//    *
//    * <ul>
//    *   <li>Returns false if a wildcard import exists for the old package (no update needed)
//    *   <li>Searches for the exact old import and replaces it with the new import
//    *   <li>Returns true if the import was successfully updated
//    *   <li>Returns false if the old import was not found
//    * </ul>
//    *
//    * <p>Example usage:
//    *
//    * <pre>{@code
//    * TSFile javaFile = new TSFile("Test.java",
//    *     "package com.example;\n\nimport org.example.OldClass;\n\nclass Test {}");
//    * ImportDeclarationService service = new ImportDeclarationService();
//    *
//    * boolean updated = service.updateImport(javaFile,
//    *     "org.example.OldClass", "org.newcompany.api.NewClass");
//    * // updated == true
//    * // Result: "package com.example;\n\nimport org.newcompany.api.NewClass;\n\nclass Test {}"
//    *
//    * // With wildcard import
//    * TSFile wildcardFile = new TSFile("Test.java", "import org.example.*;");
//    * boolean notUpdated = service.updateImport(wildcardFile,
//    *     "org.example.OldClass", "org.newcompany.api.NewClass");
//    * // notUpdated == false (wildcard covers the old class)
//    * }</pre>
//    *
//    * @param file The file to update the import in
//    * @param oldFullImport The current fully qualified import (e.g., "org.example.OldClass")
//    * @param newFullImport The new fully qualified import (e.g., "org.newcompany.api.NewClass")
//    * @return true if the import was updated, false if no update was needed or possible
//    * @throws IllegalArgumentException if either import name doesn't contain a dot separator
//    */
//   public boolean updateImport(TSFile file, String oldFullImport, String newFullImport) {
//     // Parse old import
//     int oldLastDotIndex = oldFullImport.lastIndexOf('.');
//     if (oldLastDotIndex == -1) {
//       throw new IllegalArgumentException("Invalid old import: " + oldFullImport);
//     }
//     String oldPackage = oldFullImport.substring(0, oldLastDotIndex);
//     String oldClassName = oldFullImport.substring(oldLastDotIndex + 1);
//     // Parse new import
//     int newLastDotIndex = newFullImport.lastIndexOf('.');
//     if (newLastDotIndex == -1) {
//       throw new IllegalArgumentException("Invalid new import: " + newFullImport);
//     }
//     // Check if wildcard import exists for old package
//     if (hasWildcardImport(file, oldPackage)) {
//       // Wildcard import exists, no need to update
//       return false;
//     }
//     // Find the specific import to update using the maps directly
//     List<Map<String, TSNode>> allImportDeclarations = this.getAllImportDeclarations(file);
//     for (Map<String, TSNode> importMap : allImportDeclarations) {
//       // Skip wildcard imports, only process regular class imports
//       if (!importMap.containsKey("isWildCard")) {
//         TSNode packageNode = importMap.get("package");
//         TSNode classNode = importMap.get("class");
//         if (packageNode != null && classNode != null) {
//           String packageText = file.getTextFromNode(packageNode);
//           String classText = file.getTextFromNode(classNode);
//           // Check if this matches the old import we want to update
//           if (packageText.equals(oldPackage) && classText.equals(oldClassName)) {
//             // Found the import to update - get the scoped identifier and update it
//             TSNode importDeclarationNode = importMap.get("importDeclaration");
//             Optional<TSNode> scopedIdentifier =
//                 file.findChildNodeByType(importDeclarationNode, "scoped_identifier");
//             if (scopedIdentifier.isPresent()) {
//               file.updateSourceCode(scopedIdentifier.get(), newFullImport);
//               return true;
//             }
//           }
//         }
//       }
//     }
//     // Import not found
//     return false;
//   }
// }
