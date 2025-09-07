package io.github.syntaxpresso.core.service.java.language;

import com.google.common.base.Strings;
import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.service.java.language.extra.ParameterCapture;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.treesitter.TSNode;

/**
 * Service for analyzing and extracting method parameter information from Java source code using
 * Tree-sitter parsing.
 *
 * <p>This service provides comprehensive functionality for working with method (formal) parameters
 * in Java, including extraction of parameter nodes, detailed parameter info, finding usages
 * within method bodies, and renaming parameters. It leverages tree-sitter queries to accurately 
 * parse and analyze parameter declarations at the AST level.
 *
 * <p>Key capabilities include:
 *
 * <ul>
 *   <li>Extracting all parameter nodes from a method declaration
 *   <li>Extracting type, name, and full node info for each parameter
 *   <li>Finding all usages of a parameter within a method body
 *   <li>Supporting generic and non-generic parameter types
 *   <li>Renaming parameter declarations and their usages
 * </ul>
 *
 * <p>Usage example:
 *
 * <pre>
 * FormalParameterService paramService = new FormalParameterService();
 * TSNode methodNode = ... // obtain method_declaration node
 * List&lt;TSNode&gt; params = paramService.getAllFormalParameterNodes(tsFile, methodNode);
 * for (TSNode param : params) {
 *   List&lt;Map&lt;String, TSNode&gt;&gt; info = paramService.getFormalParameterNodeInfo(tsFile, param);
 *   for (Map&lt;String, TSNode&gt; map : info) {
 *     String type = tsFile.getTextFromNode(map.get("parameter_type"));
 *     String name = tsFile.getTextFromNode(map.get("parameter_name"));
 *     System.out.println("Parameter: " + type + " " + name);
 *   }
 * }
 * </pre>
 *
 * @see TSFile
 * @see ParameterCapture
 */
public class FormalParameterService {

  /**
   * Retrieves all method parameter nodes from a method declaration.
   *
   * <p>This method searches for all formal parameter declarations within the specified method
   * declaration scope. It returns the AST nodes representing individual parameter declarations.
   *
   * <p>Usage example:
   *
   * <pre>
   * TSFile tsFile = new TSFile(SupportedLanguage.JAVA, "public void test(String name, int age) {}");
   * TSNode methodNode = // ... get method declaration node
   * List&lt;TSNode&gt; parameters = service.getAllFormalParameterNodes(tsFile, methodNode);
   * // Returns nodes for both "String name" and "int age" parameters
   * </pre>
   *
   * @param tsFile The {@link TSFile} containing the Java source code
   * @param methodDeclarationNode The method declaration {@link TSNode} to analyze
   * @return List of formal parameter nodes, empty if method has no parameters or invalid input
   */
  public List<TSNode> getAllFormalParameterNodes(TSFile tsFile, TSNode methodDeclarationNode) {
    if (tsFile == null
        || tsFile.getTree() == null
        || methodDeclarationNode == null
        || !"method_declaration".equals(methodDeclarationNode.getType())) {
      return Collections.emptyList();
    }
    String queryString =
        String.format(
            """
            (method_declaration
              parameters: (formal_parameters
                (formal_parameter) %s
              )
            )
            """,
            ParameterCapture.PARAMETER.getCaptureWithAt());
    return tsFile.query(queryString).within(methodDeclarationNode).execute().nodes();
  }

  /**
   * Extracts detailed information about a specific method parameter node.
   *
   * <p>This method analyzes a formal parameter node to extract information about its type, name,
   * and optional type arguments using tree-sitter queries. It supports both primitive types and
   * generic types with type arguments.
   *
   * <p>Usage example:
   *
   * <pre>
   * TSFile tsFile = new TSFile(SupportedLanguage.JAVA, "public void test(String name) {}");
   * TSNode parameterNode = // ... get formal_parameter node for "String name"
   * List&lt;Map&lt;String, TSNode&gt;&gt; info = service.getFormalParameterNodeInfo(tsFile, parameterNode);
   * // Returns map with:
   * // "parameter_type" -&gt; TSNode for "String"
   * // "parameter_name" -&gt; TSNode for "name"
   * // "parameter" -&gt; TSNode for entire "String name"
   * </pre>
   *
   * Query captures: - parameter_type: The type node of the parameter - parameter_name: The name
   * identifier of the parameter - parameter_type_argument: Type arguments for generic types -
   * parameter: The entire parameter declaration
   *
   * @param tsFile The {@link TSFile} containing the Java source code
   * @param methodParameterNode The formal parameter {@link TSNode} to analyze
   * @return A list of maps containing the captured nodes, or an empty list if invalid input
   */
  public List<Map<String, TSNode>> getFormalParameterNodeInfo(
      TSFile tsFile, TSNode methodParameterNode) {
    if (tsFile == null
        || tsFile.getTree() == null
        || !methodParameterNode.getType().equals("formal_parameter")) {
      return Collections.emptyList();
    }
    String queryString =
        String.format(
            """
            (formal_parameter
              type: [
                (type_identifier)
                (generic_type
                  (type_identifier)
                  (type_arguments
                    [
                      (type_identifier) %s
                      (generic_type) %s
                    ]
                  )
                )
              ] %s
              name: (identifier) %s
            ) %s
            """,
            ParameterCapture.PARAMETER_TYPE_ARGUMENT.getCaptureWithAt(),
            ParameterCapture.PARAMETER_TYPE_ARGUMENT.getCaptureWithAt(),
            ParameterCapture.PARAMETER_TYPE.getCaptureWithAt(),
            ParameterCapture.PARAMETER_NAME.getCaptureWithAt(),
            ParameterCapture.PARAMETER.getCaptureWithAt());
    return tsFile
        .query(queryString)
        .within(methodParameterNode)
        .returningAllCaptures()
        .execute()
        .captures();
  }

  /**
   * Retrieves a specific child node from a method parameter by capture name.
   *
   * <p>This method extracts a specific component of a formal parameter declaration based on the
   * provided capture type. It can retrieve the parameter type, name, or the entire parameter node.
   *
   * <p>Usage example:
   *
   * <pre>
   * TSFile tsFile = new TSFile(SupportedLanguage.JAVA, "public void test(List&lt;String&gt; items) {}");
   * TSNode parameterNode = // ... get formal_parameter node
   * Optional&lt;TSNode&gt; typeNode = service.getFormalParameterChildByCaptureName(
   *     tsFile, parameterNode, ParameterCapture.PARAMETER_TYPE);
   * // Returns TSNode for "List&lt;String&gt;"
   * </pre>
   *
   * @param tsFile The {@link TSFile} containing the Java source code
   * @param methodParameterNode The formal parameter {@link TSNode} to search within
   * @param capture The specific {@link ParameterCapture} type to retrieve
   * @return Optional containing the requested node, empty if not found or invalid input
   */
  public Optional<TSNode> getFormalParameterChildByCaptureName(
      TSFile tsFile, TSNode methodParameterNode, ParameterCapture capture) {
    if (tsFile == null
        || tsFile.getTree() == null
        || !methodParameterNode.getType().equals("formal_parameter")) {
      return Optional.empty();
    }
    List<Map<String, TSNode>> nodeInfo =
        this.getFormalParameterNodeInfo(tsFile, methodParameterNode);
    for (Map<String, TSNode> map : nodeInfo) {
      TSNode node = map.get(capture.getCaptureName());
      if (node != null) {
        return Optional.of(node);
      }
    }
    return Optional.empty();
  }

  /**
   * Retrieves the base type node of a method parameter, excluding generic type arguments.
   *
   * <p>This method extracts the base type node from a formal parameter declaration. For generic
   * types, it returns only the type argument. For example, in {@code List<String> items}, it would
   * return just the {@code String} type node.
   *
   * <p>Usage example:
   *
   * <pre>
   * List<TSNode> params = service.getAllFormalParameterNodes(tsFile, methodNode);
   * Optional<TSNode> typeNode = service.getFormalParameterTypeNode(tsFile, params.get(0));
   * if (typeNode.isPresent()) {
   *   String type = tsFile.getTextFromNode(typeNode.get()); // e.g., "String" from List<String>
   * }
   * </pre>
   *
   * @param tsFile The {@link TSFile} containing the source code
   * @param methodParameterNode The formal parameter {@link TSNode} to analyze
   * @return An {@link Optional} containing the base type node if found
   */
  public Optional<TSNode> getFormalParameterTypeNode(TSFile tsFile, TSNode methodParameterNode) {
    Optional<TSNode> parameterTypeArgumentNode =
        this.getFormalParameterChildByCaptureName(
            tsFile, methodParameterNode, ParameterCapture.PARAMETER_TYPE_ARGUMENT);
    if (parameterTypeArgumentNode.isPresent()) {
      return parameterTypeArgumentNode;
    }
    return this.getFormalParameterChildByCaptureName(
        tsFile, methodParameterNode, ParameterCapture.PARAMETER_TYPE);
  }

  /**
   * Retrieves the complete type node of a method parameter, including generic type information if
   * present.
   *
   * <p>This method extracts the full type node from a formal parameter declaration, which includes
   * the base type and any generic type arguments. For example, in {@code List<String> items}, it
   * would return the entire {@code List<String>} type node.
   *
   * <p>Usage example:
   *
   * <pre>
   * List<TSNode> params = service.getAllFormalParameterNodes(tsFile, methodNode);
   * Optional<TSNode> fullTypeNode = service.getFormalParameterFullTypeNode(tsFile, params.get(0));
   * if (fullTypeNode.isPresent()) {
   *   String type = tsFile.getTextFromNode(fullTypeNode.get()); // e.g., "List<String>"
   * }
   * </pre>
   *
   * @param tsFile The {@link TSFile} containing the source code
   * @param methodParameterNode The formal parameter {@link TSNode} to analyze
   * @return An {@link Optional} containing the complete type node if found
   */
  public Optional<TSNode> getFormalParameterFullTypeNode(
      TSFile tsFile, TSNode methodParameterNode) {
    return this.getFormalParameterChildByCaptureName(
        tsFile, methodParameterNode, ParameterCapture.PARAMETER_TYPE);
  }

  /**
   * Retrieves the name node of a method parameter.
   *
   * <p>This method extracts the identifier node that represents the parameter's name from a formal
   * parameter declaration.
   *
   * <p>Usage example:
   *
   * <pre>
   * TSFile tsFile = new TSFile(SupportedLanguage.JAVA, "public void test(String userName) {}");
   * TSNode parameterNode = // ... get formal_parameter node
   * Optional&lt;TSNode&gt; nameNode = service.getFormalParameterNameNode(tsFile, parameterNode);
   * if (nameNode.isPresent()) {
   *   String paramName = tsFile.getTextFromNode(nameNode.get()); // "userName"
   * }
   * </pre>
   *
   * @param tsFile The {@link TSFile} containing the Java source code
   * @param methodParameterNode The formal parameter {@link TSNode} to analyze
   * @return Optional containing the parameter name node, empty if not found or invalid input
   */
  public Optional<TSNode> getFormalParameterNameNode(TSFile tsFile, TSNode methodParameterNode) {
    return this.getFormalParameterChildByCaptureName(
        tsFile, methodParameterNode, ParameterCapture.PARAMETER_NAME);
  }

  /**
   * Finds all usages of a method parameter within the method body.
   *
   * <p>This method searches for a parameter in various contexts including method calls, assignments,
   * binary expressions, ternary expressions, and argument lists. It identifies all locations where
   * the parameter is referenced within the method scope.
   *
   * <p>Usage example:
   *
   * <pre>
   * TSFile tsFile = new TSFile(SupportedLanguage.JAVA, """
   *     public void process(String input) {
   *         System.out.println(input);
   *         String result = input.trim();
   *         if (input != null) { ... }
   *     }
   * """);
   * TSNode parameterNode = // ... get formal_parameter node for "input"
   * TSNode methodNode = // ... get method_declaration node
   * List&lt;TSNode&gt; usages = service.findAllFormalParameterNodeUsages(tsFile, parameterNode, methodNode);
   * // Returns identifier nodes for all three usages of "input"
   * </pre>
   *
   * Query captures: - name: The parameter name identifier within various expression contexts
   *
   * @param tsFile The {@link TSFile} containing the Java source code
   * @param methodParameterNode The formal parameter {@link TSNode} whose usages to find
   * @param methodDeclarationNode The method declaration {@link TSNode} to search within
   * @return List of identifier nodes where the parameter is used, empty if no usages found or invalid input
   */
  public List<TSNode> findAllFormalParameterNodeUsages(
      TSFile tsFile, TSNode methodParameterNode, TSNode methodDeclarationNode) {
    if (tsFile == null
        || tsFile.getTree() == null
        || methodParameterNode == null
        || methodDeclarationNode == null
        || !"formal_parameter".equals(methodParameterNode.getType())
        || !"method_declaration".equals(methodDeclarationNode.getType())) {
      return Collections.emptyList();
    }
    Optional<TSNode> methodParameterNameNode =
        this.getFormalParameterNameNode(tsFile, methodParameterNode);
    if (methodParameterNameNode.isEmpty()) {
      return Collections.emptyList();
    }
    String methodParameterName = tsFile.getTextFromNode(methodParameterNameNode.get());
    String queryString =
        String.format(
            """
            [
              (method_invocation
                object: (identifier) @name
              )
              (argument_list
                (identifier) @name
              )
              (variable_declarator
                value: (identifier) @name
              )
              (assignment_expression
                left: (identifier) @name
              )
              (assignment_expression
                right: (identifier) @name
              )
              (binary_expression
                left: (identifier) @name
              )
              (binary_expression
                right: (identifier) @name
              )
              (ternary_expression
                consequence: (identifier) @name
              )
              (ternary_expression
                alternative: (identifier) @name
              )
              (ternary_expression
                condition: (identifier) @name
              )
              (#eq? @name "%s")
            ]
            """,
            methodParameterName);
    return tsFile.query(queryString).within(methodDeclarationNode).execute().nodes();
  }

  /**
   * Renames a formal parameter's type and optionally its variable name.
   *
   * <p>This method updates both the type and optionally the variable name of a formal parameter
   * declaration. It supports renaming both primitive types and reference types. When renaming a
   * parameter, you can choose whether to also rename the variable name or just the type.
   *
   * <p>Usage example:
   *
   * <pre>
   * List&lt;TSNode&gt; paramNodes = service.getAllFormalParameterNodes(tsFile, methodNode);
   * // Rename both type and variable: "String name" to "Username userName"
   * boolean success = service.renameFormalParameterNode(
   *     tsFile, paramNodes.get(0), "Username", "userName", true);
   *
   * // Rename only type: "String name" to "Username name"
   * success = service.renameFormalParameterNode(
   *     tsFile, paramNodes.get(0), "Username", "", false);
   * </pre>
   *
   * @param tsFile The {@link TSFile} containing the source code
   * @param formalParameterNode The formal parameter {@link TSNode} to rename
   * @param newTypeName The new type name for the parameter
   * @param newVariableName The new variable name (used only if shouldRenameVariable is true)
   * @param shouldRenameVariable Whether to rename the variable in addition to the type
   * @return true if the rename operation was successful, false otherwise
   */
  public boolean renameFormalParameterNode(
      TSFile tsFile,
      TSNode formalParameterNode,
      String newTypeName,
      String newVariableName,
      Boolean shouldRenameVariable) {
    if (tsFile == null
        || tsFile.getTree() == null
        || Strings.isNullOrEmpty(newTypeName)
        || !formalParameterNode.getType().equals("formal_parameter")) {
      return false;
    }
    Optional<TSNode> formalParameterTypeNode =
        this.getFormalParameterTypeNode(tsFile, formalParameterNode);
    Optional<TSNode> formalParameterNameNode =
        this.getFormalParameterNameNode(tsFile, formalParameterNode);
    if (formalParameterNameNode.isEmpty() || formalParameterTypeNode.isEmpty()) {
      return false;
    }
    if (shouldRenameVariable) {
      tsFile.updateSourceCode(formalParameterNameNode.get(), newVariableName);
    }
    tsFile.updateSourceCode(formalParameterTypeNode.get(), newTypeName);
    return true;
  }

  /**
   * Renames a formal parameter usage node to match a new variable name.
   *
   * <p>This method updates a parameter usage identifier to match a new variable name. This is
   * typically used in conjunction with {@link #renameFormalParameterNode} to update all references
   * to a renamed parameter within the method body.
   *
   * <p>Usage example:
   *
   * <pre>
   * // After renaming a parameter's variable name
   * List&lt;TSNode&gt; usages = service.findAllFormalParameterNodeUsages(tsFile, paramNode, methodNode);
   * for (TSNode usage : usages) {
   *   boolean success = service.renameFormalParameterUsageNode(tsFile, usage, "newVariableName");
   * }
   * </pre>
   *
   * @param tsFile The {@link TSFile} containing the source code
   * @param formalParameterUsageNode The parameter usage {@link TSNode} to rename
   * @param newVariableName The new variable name to apply
   * @return true if the rename operation was successful, false otherwise
   */
  public boolean renameFormalParameterUsageNode(
      TSFile tsFile, TSNode formalParameterUsageNode, String newVariableName) {
    if (tsFile == null
        || tsFile.getTree() == null
        || Strings.isNullOrEmpty(newVariableName)
        || !formalParameterUsageNode.getType().equals("identifier")) {
      return false;
    }
    tsFile.updateSourceCode(formalParameterUsageNode, newVariableName);
    return true;
  }
}
