package io.github.syntaxpresso.core.service.java.language;

import io.github.syntaxpresso.core.common.TSFile;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.treesitter.TSNode;

public class MethodDeclarationService {

  public List<TSNode> getAllMethodDeclarationNodes(TSFile tsFile, TSNode classDeclarationNode) {
    if (tsFile == null
        || tsFile.getTree() == null
        || !classDeclarationNode.getType().equals("class_declaration")) {
      return Collections.emptyList();
    }
    String queryString =
        """
        (class_declaration
          body: (class_body
            (method_declaration) @method
          )
        )
        """;
    return tsFile.query(queryString).within(classDeclarationNode).execute().nodes();
  }

  public List<Map<String, TSNode>> getMethodDeclarationNodeInfo(
      TSFile tsFile, TSNode methodDeclarationNode) {
    if (tsFile == null
        || tsFile.getTree() == null
        || !methodDeclarationNode.getType().equals("method_declaration")) {
      return Collections.emptyList();
    }
    String queryString =
        """
        (method_declaration
          (modifiers) @modifiers
          type: (_) @type
          name: (_) @name
          parameters: (_) @parameters
          body: (_) @body
        ) @method
        """;
    return tsFile
        .query(queryString)
        .within(methodDeclarationNode)
        .returningAllCaptures()
        .execute()
        .captures();
  }

  public Optional<TSNode> getMethodDeclarationTypeNode(
      TSFile tsFile, TSNode methodDeclarationNode) {
    if (tsFile == null
        || tsFile.getTree() == null
        || !methodDeclarationNode.getType().equals("method_declaration")) {
      return Optional.empty();
    }
    List<Map<String, TSNode>> methodDeclarationInfo =
        this.getMethodDeclarationNodeInfo(tsFile, methodDeclarationNode);
    for (Map<String, TSNode> map : methodDeclarationInfo) {
      TSNode typeNode = map.get("type");
      if (typeNode != null) {
        return Optional.of(typeNode);
      }
    }
    return Optional.empty();
  }

  public Optional<TSNode> getMethodDeclarationNameNode(
      TSFile tsFile, TSNode methodDeclarationNode) {
    if (tsFile == null
        || tsFile.getTree() == null
        || !methodDeclarationNode.getType().equals("method_declaration")) {
      return Optional.empty();
    }
    List<Map<String, TSNode>> methodDeclarationInfo =
        this.getMethodDeclarationNodeInfo(tsFile, methodDeclarationNode);
    for (Map<String, TSNode> map : methodDeclarationInfo) {
      TSNode nameNode = map.get("name");
      if (nameNode != null) {
        return Optional.of(nameNode);
      }
    }
    return Optional.empty();
  }

  /**
   * Checks if a method declaration node represents a main method.
   *
   * @param file The TSFile containing the source code.
   * @param methodDeclarationNode The method declaration node to check.
   * @return True if the method is a main method, false otherwise.
   */
  public boolean isMainMethod(TSFile file, TSNode methodDeclarationNode) {
    if (file == null
        || methodDeclarationNode == null
        || !"method_declaration".equals(methodDeclarationNode.getType())) {
      return false;
    }
    String methodText = file.getTextFromNode(methodDeclarationNode);
    // Check if it's a main method by looking for the common patterns:
    // 1. public static void main(String[] args)
    // 2. public static void main(String... args)
    // Allow for different parameter names and spacing
    return methodText.matches(
            "(?s).*public\\s+static\\s+void\\s+main\\s*\\(\\s*String\\s*\\[\\s*\\]\\s+\\w+\\s*\\).*")
        || methodText.matches(
            "(?s).*public\\s+static\\s+void\\s+main\\s*\\(\\s*String\\s*\\.\\s*\\.\\s*\\.\\s+\\w+\\s*\\).*");
  }

  // /**
  //  * Finds all method invocation nodes in the given TSFile.
  //  *
  //  * @param file The TSFile containing the source code.
  //  * @return A list of all method invocation nodes.
  //  */
  // public List<TSNode> findAllMethodInvocations(TSFile file) {
  //   if (file == null || file.getTree() == null) {
  //     return Collections.emptyList();
  //   }
  //   List<TSNode> nodes = file.query(METHOD_INVOCATION_QUERY).execute();
  //   return nodes.reversed();
  // }
  //
  // /**
  //  * Gets the name identifier from a method invocation node.
  //  *
  //  * @param methodInvocationNode The method invocation node.
  //  * @return An optional containing the name identifier node, or empty if not found.
  //  */
  // public Optional<TSNode> getMethodInvocationName(TSNode methodInvocationNode) {
  //   if (methodInvocationNode == null
  //       || !"method_invocation".equals(methodInvocationNode.getType())) {
  //     return Optional.empty();
  //   }
  //   return Optional.ofNullable(methodInvocationNode.getChildByFieldName("name"));
  // }
  //
  // /**
  //  * Gets the object identifier from a method invocation node.
  //  *
  //  * @param methodInvocationNode The method invocation node.
  //  * @return An optional containing the object identifier node, or empty if not found.
  //  */
  // public Optional<TSNode> getMethodInvocationObject(TSNode methodInvocationNode) {
  //   if (methodInvocationNode == null
  //       || !"method_invocation".equals(methodInvocationNode.getType())) {
  //     return Optional.empty();
  //   }
  //   TSNode objectNode = methodInvocationNode.getChildByFieldName("object");
  //   if (objectNode == null) {
  //     return Optional.empty();
  //   }
  //   String nodeType = objectNode.getType();
  //   if ("identifier".equals(nodeType) || "this".equals(nodeType)) {
  //     return Optional.of(objectNode);
  //   }
  //   if ("field_access".equals(nodeType)) {
  //     return Optional.ofNullable(objectNode.getChildByFieldName("field"));
  //   }
  //   return Optional.empty();
  // }

  // /**
  //  * Renames a specific method in a file.
  //  *
  //  * @param file The file containing the source code.
  //  * @param methodDeclarationNode The method declaration node to be changed.
  //  * @param newName The new name for the method.
  //  */
  // public void renameMethod(TSFile file, TSNode methodDeclarationNode, String newName) {
  //   if (file == null
  //       || methodDeclarationNode == null
  //       || !"method_declaration".equals(methodDeclarationNode.getType())
  //       || Strings.isNullOrEmpty(newName)) {
  //     return;
  //   }
  //   TSNode nameNode = methodDeclarationNode.getChildByFieldName("name");
  //   if (nameNode == null) {
  //     return;
  //   }
  //   file.updateSourceCode(nameNode, newName);
  // }

  // /**
  //  * Renames a method declaration node.
  //  *
  //  * @param file The file containing the method declaration.
  //  * @param methodDeclarationNode The method declaration node to rename.
  //  * @param newName The new name for the method.
  //  * @return True if the method was successfully renamed, false otherwise.
  //  */
  // public boolean renameMethodDeclaration(
  //     TSFile file, TSNode methodDeclarationNode, String newName) {
  //   if (file == null
  //       || methodDeclarationNode == null
  //       || !"method_declaration".equals(methodDeclarationNode.getType())
  //       || Strings.isNullOrEmpty(newName)) {
  //     return false;
  //   }
  //   TSNode nameNode = methodDeclarationNode.getChildByFieldName("name");
  //   if (nameNode != null) {
  //     file.updateSourceCode(nameNode, newName);
  //     return true;
  //   }
  //   return false;
  // }

  // /**
  //  * Finds method usage nodes in a file that match the given method name and belong to the
  // specified
  //  * class.
  //  *
  //  * @param file The file to search in.
  //  * @param methodName The name of the method to find usages for.
  //  * @param className The class name that the method belongs to.
  //  * @return A list of method invocation name nodes that should be renamed.
  //  */
  // public List<TSNode> findMethodUsagesInFile(TSFile file, String methodName, String className) {
  //   if (file == null || Strings.isNullOrEmpty(methodName) || Strings.isNullOrEmpty(className)) {
  //     return Collections.emptyList();
  //   }
  //
  //   List<TSNode> usagesToRename = new ArrayList<>();
  //   List<TSNode> allMethodInvocations = this.findAllMethodInvocations(file);
  //
  //   for (TSNode methodInvocation : allMethodInvocations) {
  //     Optional<TSNode> methodInvocationObjectNode =
  //         this.getMethodInvocationObject(methodInvocation);
  //     Optional<TSNode> methodInvocationNameNode = this.getMethodInvocationName(methodInvocation);
  //
  //     if (methodInvocationNameNode.isEmpty() || methodInvocationObjectNode.isEmpty()) {
  //       continue;
  //     }
  //
  //     // Check if the method name matches
  //     String invocationName = file.getTextFromNode(methodInvocationNameNode.get());
  //     if (!methodName.equals(invocationName)) {
  //       continue;
  //     }
  //
  //     // Resolve the type of the object on which the method is called
  //     String objectType =
  //         this.typeResolutionService.resolveObjectType(
  //             file, methodInvocationObjectNode.get(), methodInvocation);
  //
  //     // Only include if the object type matches our class
  //     if (className.equals(objectType)) {
  //       usagesToRename.add(methodInvocationNameNode.get());
  //     }
  //   }
  //
  //   return usagesToRename;
  // }
}
