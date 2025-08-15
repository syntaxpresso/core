package io.github.syntaxpresso.core.service.java.extra;

import io.github.syntaxpresso.core.common.TSFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.treesitter.TSNode;

@RequiredArgsConstructor
@Getter
public class ClassDeclarationService {
  private static final String CLASS_DECLARATION_QUERY = "(class_declaration) @class";
  private final FieldDeclarationService fieldDeclarationService;
  private final MethodDeclarationService methodDeclarationService;

  /**
   * Finds all class declarations in the given TSFile.
   *
   * @param file The TSFile containing the source code.
   * @return A list of all class declaration nodes.
   */
  public List<TSNode> findAllClassDeclarations(TSFile file) {
    if (file == null || file.getTree() == null) {
      return Collections.emptyList();
    }
    return file.query(CLASS_DECLARATION_QUERY);
  }

  /**
   * Gets the class name from a class declaration node.
   *
   * @param file The TSFile containing the source code.
   * @param classNode The class declaration node.
   * @return The class name, or empty if not found.
   */
  public Optional<String> getClassName(TSFile file, TSNode classNode) {
    if (file == null || classNode == null || !"class_declaration".equals(classNode.getType())) {
      return Optional.empty();
    }
    TSNode nameNode = classNode.getChildByFieldName("name");
    if (nameNode != null) {
      return Optional.of(file.getTextFromNode(nameNode));
    }
    return Optional.empty();
  }

  /**
   * Finds a class declaration by name.
   *
   * @param file The TSFile containing the source code.
   * @param className The name of the class to find.
   * @return The class declaration node, or empty if not found.
   */
  public Optional<TSNode> findClassByName(TSFile file, String className) {
    if (file == null || className == null || className.trim().isEmpty()) {
      return Optional.empty();
    }
    List<TSNode> classNodes = this.findAllClassDeclarations(file);
    for (TSNode classNode : classNodes) {
      Optional<String> name = this.getClassName(file, classNode);
      if (name.isPresent() && name.get().equals(className)) {
        return Optional.of(classNode);
      }
    }
    return Optional.empty();
  }

  /**
   * Gets all field declarations within a specific class.
   *
   * @param file The TSFile containing the source code.
   * @param classNode The class declaration node.
   * @return A list of field declaration nodes within the class.
   */
  public List<TSNode> getClassFields(TSFile file, TSNode classNode) {
    if (file == null || classNode == null || !"class_declaration".equals(classNode.getType())) {
      return Collections.emptyList();
    }
    return file.query(classNode, "(field_declaration) @field");
  }

  /**
   * Gets all method declarations within a specific class.
   *
   * @param file The TSFile containing the source code.
   * @param classNode The class declaration node.
   * @return A list of method declaration nodes within the class.
   */
  public List<TSNode> getClassMethods(TSFile file, TSNode classNode) {
    if (file == null || classNode == null || !"class_declaration".equals(classNode.getType())) {
      return Collections.emptyList();
    }
    return file.query(classNode, "(method_declaration) @method");
  }

  /**
   * Finds all fields of a specific type within a class.
   *
   * @param file The TSFile containing the source code.
   * @param classNode The class declaration node.
   * @param typeName The type name to search for.
   * @return A list of field declaration nodes with the specified type.
   */
  public List<TSNode> findFieldsByType(TSFile file, TSNode classNode, String typeName) {
    if (file == null || classNode == null || typeName == null) {
      return Collections.emptyList();
    }
    List<TSNode> classFields = this.getClassFields(file, classNode);
    return this.fieldDeclarationService.filterFieldsByType(file, classFields, typeName);
  }

  /**
   * Finds all methods with a specific name within a class.
   *
   * @param file The TSFile containing the source code.
   * @param classNode The class declaration node.
   * @param methodName The method name to search for.
   * @return A list of method declaration nodes with the specified name.
   */
  public List<TSNode> findMethodsByName(TSFile file, TSNode classNode, String methodName) {
    if (file == null || classNode == null || methodName == null) {
      return Collections.emptyList();
    }
    List<TSNode> classMethods = this.getClassMethods(file, classNode);
    List<TSNode> matchingMethods = new ArrayList<>();
    for (TSNode methodNode : classMethods) {
      TSNode nameNode = methodNode.getChildByFieldName("name");
      if (nameNode != null && methodName.equals(file.getTextFromNode(nameNode))) {
        matchingMethods.add(methodNode);
      }
    }
    return matchingMethods;
  }

  /**
   * Renames a class by updating its declaration.
   *
   * @param file The TSFile containing the source code.
   * @param classNode The class declaration node to rename.
   * @param newName The new name for the class.
   */
  public void renameClass(TSFile file, TSNode classNode, String newName) {
    if (file == null || classNode == null || newName == null || newName.trim().isEmpty()) {
      return;
    }
    if (!"class_declaration".equals(classNode.getType())) {
      return;
    }
    TSNode nameNode = classNode.getChildByFieldName("name");
    if (nameNode != null) {
      file.updateSourceCode(nameNode, newName);
    }
  }

  /**
   * Checks if a class has a main method.
   *
   * @param file The TSFile containing the source code.
   * @param classNode The class declaration node.
   * @return True if the class has a main method, false otherwise.
   */
  public boolean hasMainMethod(TSFile file, TSNode classNode) {
    if (file == null || classNode == null) {
      return false;
    }
    List<TSNode> methods = this.findMethodsByName(file, classNode, "main");
    for (TSNode method : methods) {
      if (this.methodDeclarationService.isMainMethod(file, method)) {
        return true;
      }
    }
    return false;
  }
}

