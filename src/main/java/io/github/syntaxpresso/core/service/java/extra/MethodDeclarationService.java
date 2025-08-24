package io.github.syntaxpresso.core.service.java.extra;

import com.google.common.base.Strings;
import io.github.syntaxpresso.core.common.TSFile;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.treesitter.TSNode;

@RequiredArgsConstructor
@Getter
public class MethodDeclarationService {
  private static final String METHOD_DECLARATION_QUERY = "(method_declaration) @method";
  private static final String METHOD_INVOCATION_QUERY = "(method_invocation) @invocation";
  private final FormalParameterService formalParameterService;
  private final LocalVariableDeclarationService localVariableDeclarationService;

  /**
   * Finds all method declaration nodes in the given TSFile.
   *
   * @param file The TSFile containing the source code.
   * @return A list of all method declaration nodes.
   */
  public List<TSNode> findAllMethodDeclarations(TSFile file) {
    if (file == null || file.getTree() == null) {
      return Collections.emptyList();
    }
    return file.query(METHOD_DECLARATION_QUERY);
  }

  /**
   * Finds all method invocation nodes in the given TSFile.
   *
   * @param file The TSFile containing the source code.
   * @return A list of all method invocation nodes.
   */
  public List<TSNode> findAllMethodInvocations(TSFile file) {
    if (file == null || file.getTree() == null) {
      return Collections.emptyList();
    }
    return file.query(METHOD_INVOCATION_QUERY).reversed();
  }

  /**
   * Gets the name identifier from a method invocation node.
   *
   * @param methodInvocationNode The method invocation node.
   * @return An optional containing the name identifier node, or empty if not found.
   */
  public Optional<TSNode> getMethodInvocationName(TSNode methodInvocationNode) {
    if (methodInvocationNode == null
        || !"method_invocation".equals(methodInvocationNode.getType())) {
      return Optional.empty();
    }
    return Optional.ofNullable(methodInvocationNode.getChildByFieldName("name"));
  }

  /**
   * Gets the object identifier from a method invocation node.
   *
   * @param methodInvocationNode The method invocation node.
   * @return An optional containing the object identifier node, or empty if not found.
   */
  public Optional<TSNode> getMethodInvocationObject(TSNode methodInvocationNode) {
    if (methodInvocationNode == null
        || !"method_invocation".equals(methodInvocationNode.getType())) {
      return Optional.empty();
    }
    TSNode objectNode = methodInvocationNode.getChildByFieldName("object");
    if (objectNode == null) {
      return Optional.empty();
    }
    String nodeType = objectNode.getType();
    if ("identifier".equals(nodeType) || "this".equals(nodeType)) {
      return Optional.of(objectNode);
    }
    if ("field_access".equals(nodeType)) {
      return Optional.ofNullable(objectNode.getChildByFieldName("field"));
    }
    return Optional.empty();
  }

  /**
   * Renames a specific method in a file.
   *
   * @param file The file containing the source code.
   * @param methodDeclarationNode The method declaration node to be changed.
   * @param newName The new name for the method.
   */
  public void renameMethod(TSFile file, TSNode methodDeclarationNode, String newName) {
    if (file == null
        || methodDeclarationNode == null
        || !"method_declaration".equals(methodDeclarationNode.getType())
        || Strings.isNullOrEmpty(newName)) {
      return;
    }
    TSNode nameNode = methodDeclarationNode.getChildByFieldName("name");
    if (nameNode == null) {
      return;
    }
    file.updateSourceCode(nameNode, newName);
  }

  /**
   * Checks if a method declaration node represents a main method.
   *
   * @param file The TSFile containing the source code.
   * @param methodNode The method declaration node to check.
   * @return True if the method is a main method, false otherwise.
   */
  public boolean isMainMethod(TSFile file, TSNode methodNode) {
    if (file == null || methodNode == null || !"method_declaration".equals(methodNode.getType())) {
      return false;
    }
    // Get the full method text and check if it matches main method patterns
    String methodText = file.getTextFromNode(methodNode);
    // Check if it's a main method by looking for the common patterns:
    // 1. public static void main(String[] args)
    // 2. public static void main(String... args)
    // Allow for different parameter names and spacing
    return methodText.matches(
            "(?s).*public\\s+static\\s+void\\s+main\\s*\\(\\s*String\\s*\\[\\s*\\]\\s+\\w+\\s*\\).*")
        || methodText.matches(
            "(?s).*public\\s+static\\s+void\\s+main\\s*\\(\\s*String\\s*\\.\\s*\\.\\s*\\.\\s+\\w+\\s*\\).*");
  }

  /**
   * Renames a method and all its usages across multiple files.
   *
   * @param originalFile The file containing the method declaration.
   * @param methodDeclarationNode The method declaration node to rename.
   * @param currentName The current name of the method.
   * @param newName The new name for the method.
   * @param cwd The current working directory to search for Java files.
   * @param typeResolutionService Service for resolving object types.
   * @param classDeclarationService Service for class operations.
   * @param javaService Service for finding Java files.
   * @return A list of modified TSFile objects.
   */
  public List<TSFile> renameMethodAndUsages(
      TSFile originalFile,
      TSNode methodDeclarationNode,
      String currentName,
      String newName,
      Path cwd,
      TypeResolutionService typeResolutionService,
      ClassDeclarationService classDeclarationService,
      Object javaService) {
    // First rename the method declaration
    TSNode nameNode = methodDeclarationNode.getChildByFieldName("name");
    if (nameNode != null) {
      originalFile.updateSourceCode(nameNode, newName);
    }
    if (!originalFile.isModified()) {
      return null;
    }
    List<TSFile> modifiedFiles = new ArrayList<>();
    modifiedFiles.add(originalFile);
    Optional<TSNode> classDeclarationNode = classDeclarationService.getMainClass(originalFile);
    if (classDeclarationNode.isEmpty()) {
      return null;
    }
    Optional<String> className =
        classDeclarationService.getClassName(originalFile, classDeclarationNode.get());
    if (className.isEmpty()) {
      return null;
    }
    // Use reflection to call getAllJavaFilesFromCwd since we can't import JavaService directly
    List<TSFile> allJavaFiles;
    try {
      java.lang.reflect.Method getAllJavaFilesMethod =
          javaService.getClass().getMethod("getAllJavaFilesFromCwd", Path.class);
      @SuppressWarnings("unchecked")
      List<TSFile> files = (List<TSFile>) getAllJavaFilesMethod.invoke(javaService, cwd);
      allJavaFiles = files;
    } catch (Exception e) {
      return modifiedFiles;
    }
    for (TSFile foundFile : allJavaFiles) {
      List<TSNode> allMethodInvocations = this.findAllMethodInvocations(foundFile);
      for (TSNode methodInvocation : allMethodInvocations) {
        Optional<TSNode> methodInvocationObjectNode =
            this.getMethodInvocationObject(methodInvocation);
        Optional<TSNode> methodInvocationNameNode = this.getMethodInvocationName(methodInvocation);
        if (methodInvocationNameNode.isEmpty() || methodInvocationObjectNode.isEmpty()) {
          continue;
        }
        // Check if the method name matches the one we're renaming
        String methodInvocationName = foundFile.getTextFromNode(methodInvocationNameNode.get());
        if (!methodInvocationName.equals(currentName)) {
          continue;
        }
        // Resolve the type of the object on which the method is called
        String objectType =
            typeResolutionService.resolveObjectType(
                foundFile, methodInvocationObjectNode.get(), methodInvocation);
        // Only rename if the object type matches our class
        if (className.get().equals(objectType)) {
          foundFile.updateSourceCode(methodInvocationNameNode.get(), newName);
        }
      }
      if (foundFile.isModified()) {
        modifiedFiles.add(foundFile);
      }
    }
    return modifiedFiles;
  }
}
