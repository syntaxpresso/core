package io.github.syntaxpresso.core.service.java.extra;

import io.github.syntaxpresso.core.common.TSFile;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.treesitter.TSNode;

@RequiredArgsConstructor
@Getter
public class ProgramService {
  private final ImportDeclarationService importDeclarationService;
  private final PackageDeclarationService packageDeclarationService;
  private final ClassDeclarationService classDeclarationService;

  /**
   * Gets the package name from the program.
   *
   * @param file The TSFile containing the source code.
   * @return The package name, or empty if not found.
   */
  public Optional<String> getPackageName(TSFile file) {
    return this.packageDeclarationService.getPackageName(file);
  }

  /**
   * Gets all import declarations from the program.
   *
   * @param file The TSFile containing the source code.
   * @return A list of import declaration nodes.
   */
  public List<TSNode> getAllImports(TSFile file) {
    return this.importDeclarationService.findAllImportDeclarations(file);
  }

  /**
   * Gets all class declarations from the program.
   *
   * @param file The TSFile containing the source code.
   * @return A list of class declaration nodes.
   */
  public List<TSNode> getAllClasses(TSFile file) {
    return this.classDeclarationService.findAllClassDeclarations(file);
  }

  /**
   * Finds a specific import declaration by name.
   *
   * @param file The TSFile containing the source code.
   * @param importName The name of the import to find.
   * @param packageName The package name of the import.
   * @return The import declaration node, or empty if not found.
   */
  public Optional<TSNode> findImportByName(TSFile file, String importName, String packageName) {
    return this.importDeclarationService.getImportDeclarationNode(file, importName, packageName);
  }

  /**
   * Finds a specific class declaration by name.
   *
   * @param file The TSFile containing the source code.
   * @param className The name of the class to find.
   * @return The class declaration node, or empty if not found.
   */
  public Optional<TSNode> findClassByName(TSFile file, String className) {
    return this.classDeclarationService.findClassByName(file, className);
  }

  /**
   * Checks if the program has a main class (a class with a main method).
   *
   * @param file The TSFile containing the source code.
   * @return True if the program has a main class, false otherwise.
   */
  public boolean hasMainClass(TSFile file) {
    List<TSNode> classes = this.getAllClasses(file);
    for (TSNode classNode : classes) {
      if (this.classDeclarationService.hasMainMethod(file, classNode)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Finds the main class in the program.
   *
   * @param file The TSFile containing the source code.
   * @return The main class declaration node, or empty if not found.
   */
  public Optional<TSNode> findMainClass(TSFile file) {
    List<TSNode> classes = this.getAllClasses(file);
    for (TSNode classNode : classes) {
      if (this.classDeclarationService.hasMainMethod(file, classNode)) {
        return Optional.of(classNode);
      }
    }
    return Optional.empty();
  }

  /**
   * Updates an import declaration with new names.
   *
   * @param file The TSFile containing the source code.
   * @param oldImport The old import path.
   * @param newImport The new import path.
   */
  public void updateImport(TSFile file, String oldImport, String newImport) {
    this.importDeclarationService.updateImport(file, oldImport, newImport);
  }

  /**
   * Renames a class and updates all related imports.
   *
   * @param file The TSFile containing the source code.
   * @param oldClassName The old class name.
   * @param newClassName The new class name.
   */
  public void renameClass(TSFile file, String oldClassName, String newClassName) {
    Optional<TSNode> classNode = this.findClassByName(file, oldClassName);
    if (classNode.isPresent()) {
      this.classDeclarationService.renameClass(file, classNode.get(), newClassName);
    }
    // Update any imports that reference this class
    Optional<String> packageName = this.getPackageName(file);
    if (packageName.isPresent()) {
      String oldImport = packageName.get() + "." + oldClassName;
      String newImport = packageName.get() + "." + newClassName;
      this.updateImport(file, oldImport, newImport);
    }
  }

  /**
   * Gets all fields from all classes in the program.
   *
   * @param file The TSFile containing the source code.
   * @return A list of all field declaration nodes in the program.
   */
  public List<TSNode> getAllFields(TSFile file) {
    return file.query("(field_declaration) @field");
  }

  /**
   * Gets all methods from all classes in the program.
   *
   * @param file The TSFile containing the source code.
   * @return A list of all method declaration nodes in the program.
   */
  public List<TSNode> getAllMethods(TSFile file) {
    return this.classDeclarationService.getMethodDeclarationService().findAllMethodDeclarations(file);
  }
}