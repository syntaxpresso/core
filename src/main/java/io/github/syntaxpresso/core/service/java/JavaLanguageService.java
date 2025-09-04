package io.github.syntaxpresso.core.service.java;

import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import io.github.syntaxpresso.core.service.java.language.ClassDeclarationService;
import io.github.syntaxpresso.core.service.java.language.ImportDeclarationService;
import io.github.syntaxpresso.core.service.java.language.LocalVariableDeclarationService;
import io.github.syntaxpresso.core.service.java.language.PackageDeclarationService;
import io.github.syntaxpresso.core.service.java.language.VariableNamingService;
import io.github.syntaxpresso.core.util.PathHelper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class JavaLanguageService {
  private final PathHelper pathHelper;
  private final VariableNamingService variableNamingService;
  private final ClassDeclarationService classDeclarationService;
  private final PackageDeclarationService packageDeclarationService;
  private final ImportDeclarationService importDeclarationService;
  private final LocalVariableDeclarationService localVariableDeclarationService;

  /**
   * Gets all Java files from the current working directory.
   *
   * @param cwd The current working directory.
   * @return A list of all Java files.
   */
  public List<TSFile> getAllJavaFilesFromCwd(Path cwd) {
    try {
      return this.pathHelper.findFilesByExtention(cwd, SupportedLanguage.JAVA);
    } catch (Exception e) {
      e.printStackTrace();
      return List.of();
    }
  }

  /**
   * Checks if a directory is a Java project.
   *
   * @param rootDir The root path of the project.
   * @return True if the directory is a Java project, false otherwise.
   */
  public boolean isJavaProject(Path rootDir) {
    if (rootDir == null || !Files.isDirectory(rootDir)) {
      return false;
    }
    return Files.exists(rootDir.resolve("build.gradle"))
        || Files.exists(rootDir.resolve("build.gradle.kts"))
        || Files.exists(rootDir.resolve("pom.xml"))
        || Files.isDirectory(rootDir.resolve("src/main/java"));
  }

  // /**
  //  * Processes a class rename operation across the entire project, updating all references and
  //  * imports.
  //  *
  //  * <p>This method performs a comprehensive class rename operation that affects multiple files
  //  * across the project. It updates the class declaration itself, renames the file if necessary,
  // and
  //  * then searches through all Java files in the project to update references and import
  // statements.
  //  *
  //  * <p>The method performs the following operations:
  //  *
  //  * <ul>
  //  *   <li>Updates the class name in the original file's class declaration
  //  *   <li>Renames the file if it matches the old class name (following Java conventions)
  //  *   <li>Searches all Java files in the project for references to the renamed class
  //  *   <li>Updates import statements that reference the old class name
  //  *   <li>Updates local variables, formal parameters, and field declarations that use the class
  //  *       type
  //  * </ul>
  //  *
  //  * <p>The method intelligently handles different scenarios:
  //  *
  //  * <ul>
  //  *   <li>Files in the same package don't need import updates (handled by package visibility)
  //  *   <li>Files with wildcard imports are skipped (wildcard covers the rename automatically)
  //  *   <li>Files with direct imports are updated to use the new class name
  //  *   <li>Only processes files that actually import or are in the same package as the renamed
  // class
  //  * </ul>
  //  *
  //  * <p>Example usage:
  //  *
  //  * <pre>{@code
  //  * // Original file: com/example/OldClass.java
  //  * // package com.example;
  //  * // public class OldClass { }
  //  *
  //  * Path projectRoot = Paths.get("/project");
  //  * TSFile classFile = new TSFile(SupportedLanguage.JAVA,
  //  *     Paths.get("/project/com/example/OldClass.java"));
  //  * TSNode classIdentifierNode = // ... get class identifier node
  //  *
  //  * JavaLanguageService service = new JavaLanguageService();
  //  * List<TSFile> modifiedFiles = service.processClassRename(
  //  *     projectRoot, classFile, classIdentifierNode,
  //  *     "com.example", "OldClass", "NewClass");
  //  *
  //  * // Result:
  //  * // - OldClass.java renamed to NewClass.java
  //  * // - Class declaration updated to "public class NewClass"
  //  * // - All files importing "com.example.OldClass" updated to "com.example.NewClass"
  //  * // - All variable/field declarations of type "OldClass" updated to "NewClass"
  //  * }</pre>
  //  *
  //  * @param cwd The current working directory (project root) to search for Java files
  //  * @param file The file containing the class to rename
  //  * @param node The class identifier node to be renamed
  //  * @param packageName The package name of the class being renamed
  //  * @param currentName The current class name
  //  * @param newName The new class name
  //  * @return A list of all files that were modified during the rename operation, or empty list if
  // no
  //  *     changes were made
  //  */
  // public List<TSFile> processClassRename(
  //     Path cwd, TSFile file, TSNode node, String packageName, String currentName, String newName)
  // {
  //   List<TSFile> modifiedFiles = new java.util.ArrayList<>();
  //   file.updateSourceCode(node, newName);
  //   if (this.shouldRenameFileName(file, currentName)) {
  //     file.rename(newName);
  //   }
  //   if (file.isModified()) {
  //     modifiedFiles.add(file);
  //   }
  //   List<TSFile> allJavaFiles = this.getAllJavaFilesFromCwd(cwd);
  //   for (TSFile foundFile : allJavaFiles) {
  //     Optional<String> foundFilePackageName =
  //         this.packageDeclarationService.getPackageName(foundFile);
  //     if (foundFilePackageName.isEmpty()) {
  //       continue;
  //     }
  //     if (foundFile.getFile().getAbsolutePath().equals(file.getFile().getAbsolutePath())) {
  //       continue;
  //     }
  //     Optional<Map<String, TSNode>> importDeclarationMap =
  //         this.importDeclarationService.getImportDeclarationMap(
  //             foundFile, currentName, packageName);
  //     if (importDeclarationMap.isEmpty() && !foundFilePackageName.get().equals(packageName)) {
  //       continue;
  //     }
  //     this.localVariableDeclarationService.renameLocalVariablesInFile(
  //         foundFile, currentName, newName);
  //     this.formalParameterService.renameFormalParametersInFile(foundFile, currentName, newName);
  //     this.fieldDeclarationService.renameClassFields(foundFile, currentName, newName);
  //     if (!foundFilePackageName.get().equals(packageName)) {
  //       this.importDeclarationService.updateImport(
  //           foundFile, packageName + "." + currentName, packageName + "." + newName);
  //     }
  //     if (foundFile.isModified()) {
  //       modifiedFiles.add(foundFile);
  //     }
  //   }
  //   return modifiedFiles;
  // }
  //
  // /**
  //  * Processes a method rename operation across the entire project.
  //  *
  //  * @param cwd The current working directory.
  //  * @param file The file containing the method to rename.
  //  * @param methodDeclarationNode The method declaration node to rename.
  //  * @param currentName The current method name.
  //  * @param newName The new method name.
  //  * @param className The name of the class containing the method.
  //  * @return A list of modified files.
  //  */
  // public List<TSFile> processMethodRename(
  //     Path cwd,
  //     TSFile file,
  //     TSNode methodDeclarationNode,
  //     String currentName,
  //     String newName,
  //     String className) {
  //   List<TSFile> modifiedFiles = new java.util.ArrayList<>();
  //   boolean renamed =
  //       this.methodDeclarationService.renameMethodDeclaration(file, methodDeclarationNode,
  // newName);
  //   if (!renamed || !file.isModified()) {
  //     return null;
  //   }
  //   modifiedFiles.add(file);
  //   List<TSFile> allJavaFiles = this.getAllJavaFilesFromCwd(cwd);
  //   for (TSFile foundFile : allJavaFiles) {
  //     List<TSNode> usageNodes =
  //         this.methodDeclarationService.findMethodUsagesInFile(foundFile, currentName,
  // className);
  //     for (TSNode usageNode : usageNodes) {
  //       foundFile.updateSourceCode(usageNode, newName);
  //     }
  //     if (foundFile.isModified() && !modifiedFiles.contains(foundFile)) {
  //       modifiedFiles.add(foundFile);
  //     }
  //   }
  //   return modifiedFiles;
  // }
  //
  // /**
  //  * Checks if the file should be renamed based on the class name.
  //  *
  //  * @param file The TSFile to check.
  //  * @param currentName The current class name.
  //  * @return True if the file should be renamed, false otherwise.
  //  */
  // private boolean shouldRenameFileName(TSFile file, String currentName) {
  //   String fileName =
  //       com.google.common.io.Files.getNameWithoutExtension(file.getFile().getAbsolutePath());
  //   return fileName.equals(currentName);
  // }
}
