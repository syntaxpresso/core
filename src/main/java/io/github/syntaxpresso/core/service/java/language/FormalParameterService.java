package io.github.syntaxpresso.core.service.java.language;

import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.service.java.language.extra.ParameterCapture;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.treesitter.TSNode;

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
                [
                  (formal_parameter) %s
                  (spread_parameter) %s
                ]
              )
            )
            """,
            ParameterCapture.PARAMETER.getCaptureWithAt(),
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
                (type_identifier) %s
                (integral_type) %s
                (floating_point_type) %s
                (boolean_type) %s
                (void_type) %s
                (array_type) %s
                (generic_type
                  (type_identifier)
                  (type_arguments
                    [
                      (type_identifier) %s
                      (integral_type) %s
                      (floating_point_type) %s
                      (boolean_type) %s
                      (generic_type) %s
                      (wildcard) %s
                    ]
                  )
                ) %s
              ]
              name: (identifier) %s
            ) %s
            """,
            ParameterCapture.PARAMETER_TYPE.getCaptureWithAt(),
            ParameterCapture.PARAMETER_TYPE.getCaptureWithAt(),
            ParameterCapture.PARAMETER_TYPE.getCaptureWithAt(),
            ParameterCapture.PARAMETER_TYPE.getCaptureWithAt(),
            ParameterCapture.PARAMETER_TYPE.getCaptureWithAt(),
            ParameterCapture.PARAMETER_TYPE.getCaptureWithAt(),
            ParameterCapture.PARAMETER_TYPE_ARGUMENT.getCaptureWithAt(),
            ParameterCapture.PARAMETER_TYPE_ARGUMENT.getCaptureWithAt(),
            ParameterCapture.PARAMETER_TYPE_ARGUMENT.getCaptureWithAt(),
            ParameterCapture.PARAMETER_TYPE_ARGUMENT.getCaptureWithAt(),
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
   * <p>This method searches for a parameter in various contexts including method calls,
   * assignments, binary expressions, ternary expressions, and argument lists. It identifies all
   * locations where the parameter is referenced within the method scope.
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
   * @return List of identifier nodes where the parameter is used, empty if no usages found or
   *     invalid input
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
}
