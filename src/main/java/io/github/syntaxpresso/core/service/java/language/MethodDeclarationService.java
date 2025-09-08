package io.github.syntaxpresso.core.service.java.language;

import com.google.common.base.Strings;
import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.service.java.language.extra.MethodCapture;
import io.github.syntaxpresso.core.service.java.language.extra.MethodInvocationCapture;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.treesitter.TSNode;

/**
 * Service class for handling operations related to method declarations and invocations in Java code
 * using Tree-sitter. This service provides functionalities to find method nodes, extract their
 * components, and identify specific types of methods like the main method.
 */
@Getter
@RequiredArgsConstructor
public class MethodDeclarationService {
  private final FormalParameterService formalParameterService;

  /**
   * Retrieves all method declaration nodes within a given class declaration node.
   *
   * @param tsFile The parsed source file wrapper.
   * @param classDeclarationNode The {@link TSNode} representing the class declaration to search
   *     within.
   * @return A list of {@link TSNode} objects, each representing a method declaration. Returns an
   *     empty list if the inputs are invalid or no methods are found.
   *     <pre>{@code
   * // Assuming 'file' is a parsed TSFile and 'classNode' is a class_declaration node
   * MethodDeclarationService service = new MethodDeclarationService();
   * List<TSNode> methodNodes = service.getAllMethodDeclarationNodes(file, classNode);
   * for (TSNode methodNode : methodNodes) {
   * System.out.println("Found method: " + file.getTextFromNode(methodNode));
   * }
   * }</pre>
   */
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

  public List<TSNode> getAllLocalMethodInvocationNodes(TSFile tsFile, TSNode classDeclarationNode) {
    if (tsFile == null
        || tsFile.getTree() == null
        || !classDeclarationNode.getType().equals("class_declaration")) {
      return Collections.emptyList();
    }
    String queryString =
        """
        [
          (method_invocation
            object: (this))
          (
            (method_invocation) @invocation
            (#not-match? @invocation "\\.")
          )
        ] @local_invocation
        """;
    return tsFile.query(queryString).within(classDeclarationNode).execute().nodes();
  }

  /**
   * Extracts detailed information from a method declaration node, such as modifiers, return type,
   * name, parameters, and body.
   *
   * @param tsFile The parsed source file wrapper.
   * @param methodDeclarationNode The {@link TSNode} for the method declaration.
   * @return A list containing a map of capture names to their corresponding {@link TSNode}. The
   *     list is typically of size one for a valid method declaration.
   *     <p><b>Capture Names:</b>
   *     <ul>
   *       <li>{@code method.modifiers}: The node for the method's modifiers (e.g., public static).
   *       <li>{@code method.type}: The node for the method's return type.
   *       <li>{@code method.name}: The node for the method's identifier (name).
   *       <li>{@code method.parameters}: The node for the formal parameter list.
   *       <li>{@code method.body}: The node for the method's body block.
   *       <li>{@code method}: The entire method_declaration node.
   *     </ul>
   *     <pre>{@code
   * // Assuming 'file' is a parsed TSFile and 'methodNode' is a method_declaration node
   * MethodDeclarationService service = new MethodDeclarationService();
   * List<Map<String, TSNode>> methodInfoList = service.getMethodDeclarationNodeInfo(file, methodNode);
   * if (!methodInfoList.isEmpty()) {
   * Map<String, TSNode> methodInfo = methodInfoList.get(0);
   * TSNode nameNode = methodInfo.get("method.name");
   * System.out.println("Method name: " + file.getTextFromNode(nameNode));
   * }
   * }</pre>
   */
  public List<Map<String, TSNode>> getMethodDeclarationNodeInfo(
      TSFile tsFile, TSNode methodDeclarationNode) {
    if (tsFile == null
        || tsFile.getTree() == null
        || !methodDeclarationNode.getType().equals("method_declaration")) {
      return Collections.emptyList();
    }
    String queryString =
        String.format(
            """
            (method_declaration
              (modifiers) %s
              type: (_) %s
              name: (_) %s
              parameters: (_) %s
              body: (_) %s
            ) %s
            """,
            MethodCapture.METHOD_MODIFIERS.getCaptureWithAt(),
            MethodCapture.METHOD_TYPE.getCaptureWithAt(),
            MethodCapture.METHOD_NAME.getCaptureWithAt(),
            MethodCapture.METHOD_PARAMETERS.getCaptureWithAt(),
            MethodCapture.METHOD_BODY.getCaptureWithAt(),
            MethodCapture.METHOD.getCaptureWithAt());
    return tsFile
        .query(queryString)
        .within(methodDeclarationNode)
        .returningAllCaptures()
        .execute()
        .captures();
  }

  /**
   * Extracts detailed information from a method invocation node, such as the object, method name,
   * and arguments. It handles various invocation patterns including simple calls, field access, and
   * chained calls.
   *
   * @param tsFile The parsed source file wrapper.
   * @param methodInvocationNode The {@link TSNode} for the method invocation.
   * @return A list containing a map of capture names to their corresponding {@link TSNode}.
   *     <p><b>Capture Names:</b>
   *     <ul>
   *       <li>{@code object}: The identifier of the object on which the method is called.
   *       <li>{@code this}: The 'this' keyword, if used as the object.
   *       <li>{@code super}: The 'super' keyword, if used as the object.
   *       <li>{@code parent.object}: The object in a field access (e.g., {@code myObj} in {@code
   *           myObj.field}).
   *       <li>{@code field}: The field being accessed (e.g., {@code field} in {@code myObj.field}).
   *       <li>{@code chained.object}: The preceding method_invocation node in a chain.
   *       <li>{@code method}: The identifier of the method being called.
   *       <li>{@code type.arguments}: The node for generic type arguments.
   *       <li>{@code argument.list}: The node for the argument list.
   *       <li>{@code invocation}: The entire method_invocation node.
   *     </ul>
   *     <pre>{@code
   * // Assuming 'file' is a parsed TSFile and 'invocationNode' is a method_invocation node
   * MethodDeclarationService service = new MethodDeclarationService();
   * List<Map<String, TSNode>> invocationInfoList = service.getMethodInvocationNodeInfo(file, invocationNode);
   * if (!invocationInfoList.isEmpty()) {
   * Map<String, TSNode> invocationInfo = invocationInfoList.get(0);
   * TSNode methodIdentifier = invocationInfo.get("method");
   * System.out.println("Invoked method: " + file.getTextFromNode(methodIdentifier));
   * }
   * }</pre>
   */
  public List<Map<String, TSNode>> getMethodInvocationNodeInfo(
      TSFile tsFile, TSNode methodInvocationNode) {
    if (tsFile == null
        || methodInvocationNode == null
        || methodInvocationNode.isNull()
        || !"method_invocation".equals(methodInvocationNode.getType())) {
      return Collections.emptyList();
    }
    String queryString =
        String.format(
            """
            (method_invocation
              object: [
                (identifier) @%s
                (this) @%s
                (super) @%s
                (field_access
                  object: (_) @%s
                  field: (identifier) @%s)
                (method_invocation) @%s
              ]?
              name: (identifier) @%s
              type_arguments: (type_arguments) @%s?
              arguments: (argument_list) @%s?) @%s
            """,
            MethodInvocationCapture.OBJECT.getCaptureName(),
            MethodInvocationCapture.THIS.getCaptureName(),
            MethodInvocationCapture.SUPER.getCaptureName(),
            MethodInvocationCapture.PARENT_OBJECT.getCaptureName(),
            MethodInvocationCapture.FIELD.getCaptureName(),
            MethodInvocationCapture.CHAINED_OBJECT.getCaptureName(),
            MethodInvocationCapture.METHOD.getCaptureName(),
            MethodInvocationCapture.TYPE_ARGUMENTS.getCaptureName(),
            MethodInvocationCapture.ARGUMENT_LIST.getCaptureName(),
            MethodInvocationCapture.INVOCATION.getCaptureName());
    return tsFile
        .query(queryString)
        .within(methodInvocationNode)
        .returningAllCaptures()
        .execute()
        .captures();
  }

  private Optional<TSNode> getMethodDeclarationChildByCapture(
      TSFile tsFile, TSNode methodDeclarationNode, MethodCapture capture) {
    if (tsFile == null
        || tsFile.getTree() == null
        || methodDeclarationNode == null
        || !methodDeclarationNode.getType().equals("method_declaration")) {
      return Optional.empty();
    }
    List<Map<String, TSNode>> nodeInfo =
        this.getMethodDeclarationNodeInfo(tsFile, methodDeclarationNode);
    for (Map<String, TSNode> map : nodeInfo) {
      TSNode node = map.get(capture.getCaptureName());
      if (node != null) {
        return Optional.of(node);
      }
    }
    return Optional.empty();
  }

  private Optional<TSNode> getMethodInvocationChildByCapture(
      TSFile tsFile, TSNode methodInvocationNode, MethodInvocationCapture capture) {
    if (tsFile == null
        || tsFile.getTree() == null
        || methodInvocationNode == null
        || !methodInvocationNode.getType().equals("method_invocation")) {
      return Optional.empty();
    }
    List<Map<String, TSNode>> nodeInfo =
        this.getMethodInvocationNodeInfo(tsFile, methodInvocationNode);
    for (Map<String, TSNode> map : nodeInfo) {
      TSNode node = map.get(capture.getCaptureName());
      if (node != null) {
        return Optional.of(node);
      }
    }
    return Optional.empty();
  }

  /**
   * Gets the return type node of a method declaration.
   *
   * @param tsFile The parsed source file wrapper.
   * @param methodDeclarationNode The method declaration node.
   * @return An {@link Optional} containing the type {@link TSNode}, or empty if not found.
   *     <pre>{@code
   * // Assuming 'file' and 'methodNode' are available
   * service.getMethodDeclarationTypeNode(file, methodNode).ifPresent(typeNode -> {
   * System.out.println("Return type: " + file.getTextFromNode(typeNode));
   * });
   * }</pre>
   */
  public Optional<TSNode> getMethodDeclarationTypeNode(
      TSFile tsFile, TSNode methodDeclarationNode) {
    return this.getMethodDeclarationChildByCapture(
        tsFile, methodDeclarationNode, MethodCapture.METHOD_TYPE);
  }

  /**
   * Gets the name (identifier) node of a method declaration.
   *
   * @param tsFile The parsed source file wrapper.
   * @param methodDeclarationNode The method declaration node.
   * @return An {@link Optional} containing the name {@link TSNode}, or empty if not found.
   *     <pre>{@code
   * // Assuming 'file' and 'methodNode' are available
   * service.getMethodDeclarationNameNode(file, methodNode).ifPresent(nameNode -> {
   * System.out.println("Method name: " + file.getTextFromNode(nameNode));
   * });
   * }</pre>
   */
  public Optional<TSNode> getMethodDeclarationNameNode(
      TSFile tsFile, TSNode methodDeclarationNode) {
    return this.getMethodDeclarationChildByCapture(
        tsFile, methodDeclarationNode, MethodCapture.METHOD_NAME);
  }

  /**
   * Gets the object node from which a method is invoked (e.g., {@code myVar} in {@code
   * myVar.doSomething()}).
   *
   * @param tsFile The parsed source file wrapper.
   * @param methodInvocationNode The method invocation node.
   * @return An {@link Optional} containing the object {@link TSNode}, or empty if not found.
   *     <pre>{@code
   * // Assuming 'file' and 'invocationNode' are available
   * service.getMethodInvocationObjectNode(file, invocationNode).ifPresent(objectNode -> {
   * System.out.println("Called on object: " + file.getTextFromNode(objectNode));
   * });
   * }</pre>
   */
  public Optional<TSNode> getMethodInvocationObjectNode(
      TSFile tsFile, TSNode methodInvocationNode) {
    return this.getMethodInvocationChildByCapture(
        tsFile, methodInvocationNode, MethodInvocationCapture.OBJECT);
  }

  public Optional<TSNode> getMethodInvocationNameNode(TSFile tsFile, TSNode methodInvocationNode) {
    String queryString =
        """
         (method_invocation
            name: (_) @name)
        """;
    return tsFile.query(queryString).within(methodInvocationNode).execute().firstNodeOptional();
  }

  /**
   * Finds all method invocation nodes within a given scope that match a specific variable and
   * method name.
   *
   * @param tsFile The parsed source file wrapper.
   * @param variableName The name of the variable on which the method is called.
   * @param methodName The name of the method being called.
   * @param scopeNode The {@link TSNode} representing the scope to search within (e.g., a method
   *     body or class body).
   * @return A list of {@link TSNode} objects, each representing a matching method invocation.
   *     <pre>{@code
   * // Find all calls to "myList.add(...)" inside a method body
   * // Assuming 'file' and 'methodBodyNode' are available
   * List<TSNode> invocations = service.findMethodInvocationsByVariableNameAndMethodName(
   * file, "myList", "add", methodBodyNode);
   * System.out.println("Found " + invocations.size() + " calls to myList.add()");
   * }</pre>
   */
  public List<TSNode> findMethodInvocationsByVariableNameAndMethodName(
      TSFile tsFile, String variableName, String methodName, TSNode scopeNode) {
    if (tsFile == null
        || tsFile.getTree() == null
        || scopeNode.isNull()
        || Strings.isNullOrEmpty(variableName)
        || Strings.isNullOrEmpty(methodName)) {
      return Collections.emptyList();
    }
    String queryString =
        String.format(
            """
            [
              ;; Basic method invocation pattern
              (method_invocation
                object: (identifier) %s
                name: (identifier) %s
                (#eq? %s "%s")
                (#eq? %s "%s")) %s
              ;; Method invocation with 'this' prefix
              (method_invocation
                object: (field_access
                  object: (this)
                  field: (identifier) %s)
                name: (identifier) %s
                (#eq? %s "%s")
                (#eq? %s "%s")) %s
              ;; Method invocation on field access (nested objects)
              (method_invocation
                object: (field_access
                  object: (identifier) %s
                  field: (identifier) %s)
                name: (identifier) %s
                (#eq? %s "%s")
                (#eq? %s "%s")) %s
              ;; Method invocation with chained calls
              (method_invocation
                object: (method_invocation
                  object: (identifier) %s
                  name: (identifier) %s)
                name: (identifier) %s
                (#eq? %s "%s")
                (#eq? %s "%s")) %s
              ;; Method invocation with 'this' and chained calls
              (method_invocation
                object: (method_invocation
                  object: (field_access
                    object: (this)
                    field: (identifier) %s)
                  name: (identifier) %s)
                name: (identifier) %s
                (#eq? %s "%s")
                (#eq? %s "%s")) %s
            ]
            """,
            MethodInvocationCapture.OBJECT.getCaptureWithAt(),
            MethodInvocationCapture.METHOD.getCaptureWithAt(),
            MethodInvocationCapture.OBJECT.getCaptureWithAt(),
            variableName,
            MethodInvocationCapture.METHOD.getCaptureWithAt(),
            methodName,
            MethodInvocationCapture.INVOCATION.getCaptureWithAt(),
            MethodInvocationCapture.OBJECT.getCaptureWithAt(),
            MethodInvocationCapture.METHOD.getCaptureWithAt(),
            MethodInvocationCapture.OBJECT.getCaptureWithAt(),
            variableName,
            MethodInvocationCapture.METHOD.getCaptureWithAt(),
            methodName,
            MethodInvocationCapture.INVOCATION.getCaptureWithAt(),
            MethodInvocationCapture.PARENT_OBJECT.getCaptureWithAt(),
            MethodInvocationCapture.OBJECT.getCaptureWithAt(),
            MethodInvocationCapture.METHOD.getCaptureWithAt(),
            MethodInvocationCapture.OBJECT.getCaptureWithAt(),
            variableName,
            MethodInvocationCapture.METHOD.getCaptureWithAt(),
            methodName,
            MethodInvocationCapture.INVOCATION.getCaptureWithAt(),
            MethodInvocationCapture.OBJECT.getCaptureWithAt(),
            MethodInvocationCapture.FIRST_METHOD.getCaptureWithAt(),
            MethodInvocationCapture.METHOD.getCaptureWithAt(),
            MethodInvocationCapture.OBJECT.getCaptureWithAt(),
            variableName,
            MethodInvocationCapture.METHOD.getCaptureWithAt(),
            methodName,
            MethodInvocationCapture.INVOCATION.getCaptureWithAt(),
            MethodInvocationCapture.OBJECT.getCaptureWithAt(),
            MethodInvocationCapture.FIRST_METHOD.getCaptureWithAt(),
            MethodInvocationCapture.METHOD.getCaptureWithAt(),
            MethodInvocationCapture.OBJECT.getCaptureWithAt(),
            variableName,
            MethodInvocationCapture.METHOD.getCaptureWithAt(),
            methodName,
            MethodInvocationCapture.INVOCATION.getCaptureWithAt());
    return tsFile.query(queryString).within(scopeNode).returning("invocation").execute().nodes();
  }

  /**
   * Checks if a method declaration node represents a standard Java main method.
   *
   * @param tsFile The {@link TSFile} containing the source code.
   * @param methodDeclarationNode The method declaration node to check.
   * @return {@code true} if the method is a main method (i.e., {@code public static void
   *     main(String[] args)} or with varargs), {@code false} otherwise.
   *     <pre>{@code
   * // Assuming 'file' and 'methodNode' are available
   * boolean isMain = service.isMainMethod(file, methodNode);
   * if (isMain) {
   * System.out.println("This is the main method.");
   * }
   * }</pre>
   */
  public boolean isMainMethod(TSFile tsFile, TSNode methodDeclarationNode) {
    if (tsFile == null
        || methodDeclarationNode == null
        || !"method_declaration".equals(methodDeclarationNode.getType())) {
      return false;
    }
    String methodText = tsFile.getTextFromNode(methodDeclarationNode);
    // Check for "public static void main(String[] args)" or "public static void main(String...
    // args)"
    return methodText.matches(
            "(?s).*public\\s+static\\s+void\\s+main\\s*\\(\\s*String\\s*\\[\\s*\\]\\s+\\w+\\s*\\).*")
        || methodText.matches(
            "(?s).*public\\s+static\\s+void\\s+main\\s*\\(\\s*String\\s*\\.\\s*\\.\\s*\\.\\s+\\w+\\s*\\).*");
  }
}
