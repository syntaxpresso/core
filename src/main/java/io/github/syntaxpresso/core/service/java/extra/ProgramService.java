package io.github.syntaxpresso.core.service.java.extra;

import io.github.syntaxpresso.core.common.TSFile;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.treesitter.TSNode;

@RequiredArgsConstructor
public class ProgramService {
  private final VariableNamingService variableNamingService;
  private final FieldDeclarationService fieldDeclarationService;
  private final ImportDeclarationService importDeclarationService;
  private final PackageDeclarationService packageDeclarationService;
  private final LocalVariableDeclarationService localVariableDeclarationService;
  private final FormalParameterService formalParameterService;
  private final MethodDeclarationService methodDeclarationService;
  private final ClassDeclarationService classDeclarationService;
  private final TypeResolutionService typeResolutionService;

  /**
   * Checks if the program has a main class (a class with a main method).
   *
   * @param file The TSFile containing the source code.
   * @return True if the program has a main class, false otherwise.
   */
  public boolean hasMainClass(TSFile file) {
    List<TSNode> classes = this.classDeclarationService.findAllClassDeclarations(file);
    for (TSNode classNode : classes) {
      List<TSNode> methods =
          this.classDeclarationService.findMethodsByName(file, classNode, "main");
      for (TSNode method : methods) {
        if (this.methodDeclarationService.isMainMethod(file, method)) {
          return true;
        }
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
    List<TSNode> classes = this.classDeclarationService.findAllClassDeclarations(file);
    for (TSNode classNode : classes) {
      List<TSNode> methods =
          this.classDeclarationService.findMethodsByName(file, classNode, "main");
      for (TSNode method : methods) {
        if (this.methodDeclarationService.isMainMethod(file, method)) {
          return Optional.of(classNode);
        }
      }
    }
    return Optional.empty();
  }

  /**
   * Renames a class and updates all related imports.
   *
   * @param file The TSFile containing the source code.
   * @param oldClassName The old class name.
   * @param newClassName The new class name.
   */
  public void renameClass(TSFile file, String oldClassName, String newClassName) {
    Optional<TSNode> classNode = this.classDeclarationService.findClassByName(file, oldClassName);
    if (classNode.isPresent()) {
      this.classDeclarationService.renameClass(file, classNode.get(), newClassName);
    }
    // Update any imports that reference this class
    Optional<String> packageName = this.packageDeclarationService.getPackageName(file);
    if (packageName.isPresent()) {
      String oldImport = packageName.get() + "." + oldClassName;
      String newImport = packageName.get() + "." + newClassName;
      this.importDeclarationService.updateImport(file, oldImport, newImport);
    }
  }

  public VariableNamingService getVariableNamingService() {
    return variableNamingService;
  }

  public FieldDeclarationService getFieldDeclarationService() {
    return fieldDeclarationService;
  }

  public ImportDeclarationService getImportDeclarationService() {
    return importDeclarationService;
  }

  public PackageDeclarationService getPackageDeclarationService() {
    return packageDeclarationService;
  }

  public LocalVariableDeclarationService getLocalVariableDeclarationService() {
    return localVariableDeclarationService;
  }

  public FormalParameterService getFormalParameterService() {
    return formalParameterService;
  }

  public MethodDeclarationService getMethodDeclarationService() {
    return methodDeclarationService;
  }

  public ClassDeclarationService getClassDeclarationService() {
    return classDeclarationService;
  }

  public TypeResolutionService getTypeResolutionService() {
    return typeResolutionService;
  }
}
