package io.github.syntaxpresso.core.service.java.language;

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
 */
public class FormalParameterDeclarationService {

  /**
   * Retrieves all method parameter nodes from a method declaration.
   *
   * @param tsFile The parsed TypeScript file containing the Java source code
   * @param methodDeclarationNode The method declaration node to analyze
   * @return List of method parameter nodes, empty if method has no parameters or invalid input
   * @example
   *     <pre>{@code
   * TSFile tsFile = new TSFile("public void test(String name, int age) {}");
   * TSNode methodNode = // ... get method declaration node
   * List<TSNode> parameters = service.getAllFormalParameterDeclarationNodes(tsFile, methodNode);
   * // Returns nodes for both "String name" and "int age" parameters
   * }</pre>
   */
  public List<TSNode> getAllFormalParameterDeclarationNodes(
      TSFile tsFile, TSNode methodDeclarationNode) {
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
   * @param tsFile The parsed TypeScript file containing the Java source code
   * @param methodParameterNode The formal parameter node to analyze
   * @return List of maps containing captured nodes with keys: "parameter_type", "parameter_name",
   *     "parameter"
   * @example
   *     <pre>{@code
   * TSFile tsFile = new TSFile("public void test(String name) {}");
   * TSNode parameterNode = // ... get formal_parameter node for "String name"
   * List<Map<String, TSNode>> info = service.getFormalParameterDeclarationNodeInfo(tsFile, parameterNode);
   * // Returns map with:
   * // "parameter_type" -> TSNode for "String"
   * // "parameter_name" -> TSNode for "name"
   * // "parameter" -> TSNode for entire "String name"
   * }</pre>
   */
  public List<Map<String, TSNode>> getFormalParameterDeclarationNodeInfo(
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
              type: (_) %s
              name: (identifier) %s
            ) %s
            """,
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
   * @param tsFile The parsed TypeScript file containing the Java source code
   * @param methodParameterNode The formal parameter node to search within
   * @param capture The specific capture type to retrieve (PARAMETER_TYPE, PARAMETER_NAME, or
   *     PARAMETER)
   * @return Optional containing the requested node, empty if not found or invalid input
   * @example
   *     <pre>{@code
   * TSFile tsFile = new TSFile("public void test(List<String> items) {}");
   * TSNode parameterNode = // ... get formal_parameter node
   * Optional<TSNode> typeNode = service.getFormalParameterDeclarationChildByCaptureName(
   *     tsFile, parameterNode, FormalParameterDeclarationCapture.PARAMETER_TYPE);
   * // Returns TSNode for "List<String>"
   * }</pre>
   */
  public Optional<TSNode> getFormalParameterDeclarationChildByCaptureName(
      TSFile tsFile, TSNode methodParameterNode, ParameterCapture capture) {
    if (tsFile == null
        || tsFile.getTree() == null
        || !methodParameterNode.getType().equals("formal_parameter")) {
      return Optional.empty();
    }
    List<Map<String, TSNode>> nodeInfo =
        this.getFormalParameterDeclarationNodeInfo(tsFile, methodParameterNode);
    for (Map<String, TSNode> map : nodeInfo) {
      TSNode node = map.get(capture.getCaptureName());
      if (node != null) {
        return Optional.of(node);
      }
    }
    return Optional.empty();
  }

  /**
   * Retrieves the type node of a method parameter.
   *
   * @param tsFile The parsed TypeScript file containing the Java source code
   * @param methodParameterNode The formal parameter node to analyze
   * @return Optional containing the parameter type node, empty if not found or invalid input
   * @example
   *     <pre>{@code
   * TSFile tsFile = new TSFile("public void test(Map<String, Integer> data) {}");
   * TSNode parameterNode = // ... get formal_parameter node
   * Optional<TSNode> typeNode = service.getFormalParameterDeclarationTypeNode(tsFile, parameterNode);
   * // Returns TSNode for "Map<String, Integer>"
   * }</pre>
   */
  public Optional<TSNode> getFormalParameterDeclarationTypeNode(
      TSFile tsFile, TSNode methodParameterNode) {
    return this.getFormalParameterDeclarationChildByCaptureName(
        tsFile, methodParameterNode, ParameterCapture.PARAMETER_TYPE);
  }

  /**
   * Retrieves the name node of a method parameter.
   *
   * @param tsFile The parsed TypeScript file containing the Java source code
   * @param methodParameterNode The formal parameter node to analyze
   * @return Optional containing the parameter name node, empty if not found or invalid input
   * @example
   *     <pre>{@code
   * TSFile tsFile = new TSFile("public void test(String userName) {}");
   * TSNode parameterNode = // ... get formal_parameter node
   * Optional<TSNode> nameNode = service.getFormalParameterDeclarationNameNode(tsFile, parameterNode);
   * // Returns TSNode for "userName"
   * }</pre>
   */
  public Optional<TSNode> getFormalParameterDeclarationNameNode(
      TSFile tsFile, TSNode methodParameterNode) {
    return this.getFormalParameterDeclarationChildByCaptureName(
        tsFile, methodParameterNode, ParameterCapture.PARAMETER_NAME);
  }

  /**
   * Finds all usages of a method parameter within the method body. Searches for the parameter in
   * various contexts including method calls, assignments, binary expressions, ternary expressions,
   * and argument lists.
   *
   * @param tsFile The parsed TypeScript file containing the Java source code
   * @param methodParameterNode The formal parameter node whose usages to find
   * @param methodDeclarationNode The method declaration node to search within
   * @return List of nodes where the parameter is used, empty if no usages found or invalid input
   * @example
   *     <pre>{@code
   * TSFile tsFile = new TSFile("""
   *     public void process(String input) {
   *         System.out.println(input);
   *         String result = input.trim();
   *         if (input != null) { ... }
   *     }
   * """);
   * TSNode parameterNode = // ... get formal_parameter node for "input"
   * TSNode methodNode = // ... get method_declaration node
   * List<TSNode> usages = service.findAllFormalParameterDeclarationNodeUsages(tsFile, parameterNode, methodNode);
   * // Returns nodes with capture name "name" for all three usages of "input"
   * }</pre>
   */
  public List<TSNode> findAllFormalParameterDeclarationNodeUsages(
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
        this.getFormalParameterDeclarationNameNode(tsFile, methodParameterNode);
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
