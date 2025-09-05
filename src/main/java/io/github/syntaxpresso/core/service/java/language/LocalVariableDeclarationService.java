package io.github.syntaxpresso.core.service.java.language;

import com.google.common.base.Strings;
import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.service.java.language.extra.VariableCapture;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.treesitter.TSNode;

public class LocalVariableDeclarationService {
  /**
   * Retrieves all variable declaration nodes from a file, including local variables, formal
   * parameters, and field declarations.
   *
   * @param tsFile The TSFile containing the source code
   * @return List of TSNodes representing variable declarations. Returns empty list if file is null.
   *     <p>Query capture groups: - @node: The entire declaration node
   *     <p>Example:
   *     <pre>
   * public class Example {
   *   private String field;           // field_declaration
   *   public void method(int param) { // formal_parameter
   *     String local = "value";      // local_variable_declaration
   *   }
   * }
   * </pre>
   */
  public List<TSNode> getAllMethodParameterNodes(TSFile tsFile) {
    if (tsFile == null || tsFile.getTree() == null) {
      return Collections.emptyList();
    }
    String queryString =
        """
        [
          (local_variable_declaration
            type: (_)
            declarator: (variable_declarator
              name: (identifier)
            )
          ) @node
          (formal_parameter
            type: (_)
            name: (identifier)
          ) @node
          (field_declaration
            (modifiers)
            type: (_)
            declarator: (variable_declarator
              name: (identifier)
            )
          ) @node
        ]
        """;
    return tsFile.query(queryString).execute().nodes();
  }

  /**
   * Extracts detailed information about a variable declaration node, including modifiers, type,
   * name, and value if present.
   *
   * @param tsFile The TSFile containing the source code
   * @param localVariableDeclarationNode The variable declaration node to analyze
   * @return List of maps containing the captured nodes with their names. Returns empty list if file
   *     is null.
   *     <p>Query capture groups: - @modifiers: Optional modifiers (e.g., private, final) - @type:
   *     Variable type - @name: Variable name identifier - @value: Optional initialization value
   *     - @node: The entire declaration node
   *     <p>Example:
   *     <pre>
   * // For this declaration:
   * private final String name = "value";
   *
   * // The captures will be:
   * modifiers -> "private final"
   * type -> "String"
   * name -> "name"
   * value -> "\"value\""
   * </pre>
   */
  public List<Map<String, TSNode>> getLocalVariableDeclarationNodeInfo(
      TSFile tsFile, TSNode localVariableDeclarationNode) {
    if (tsFile == null || tsFile.getTree() == null) {
      return Collections.emptyList();
    }
    String queryString =
        String.format(
            """
            [
              (local_variable_declaration
                (modifiers)? %s
                type: (_) %s
                declarator: (variable_declarator
                  name: (identifier) %s
                  value: (_)? %s
                )
              ) %s
              (formal_parameter
                (modifiers)? %s
                type: (_) %s
                name: (identifier) %s
              ) %s
              (field_declaration
                (modifiers)? %s
                type: (_) %s
                declarator: (variable_declarator
                  name: (identifier) %s
                  value: (_)? %s
                )
              ) %s
            ]
            """,
            VariableCapture.VARIABLE_MODIFIERS.getCaptureWithAt(),
            VariableCapture.VARIABLE_TYPE.getCaptureWithAt(),
            VariableCapture.VARIABLE_NAME.getCaptureWithAt(),
            VariableCapture.VARIABLE_VALUE.getCaptureWithAt(),
            VariableCapture.VARIABLE.getCaptureWithAt(),
            VariableCapture.VARIABLE_MODIFIERS.getCaptureWithAt(),
            VariableCapture.VARIABLE_TYPE.getCaptureWithAt(),
            VariableCapture.VARIABLE_NAME.getCaptureWithAt(),
            VariableCapture.VARIABLE.getCaptureWithAt(),
            VariableCapture.VARIABLE_MODIFIERS.getCaptureWithAt(),
            VariableCapture.VARIABLE_TYPE.getCaptureWithAt(),
            VariableCapture.VARIABLE_NAME.getCaptureWithAt(),
            VariableCapture.VARIABLE.getCaptureWithAt());
    return tsFile
        .query(queryString)
        .within(localVariableDeclarationNode)
        .returningAllCaptures()
        .execute()
        .captures();
  }

  /**
   * Retrieves a specific node from a variable declaration based on the capture name.
   *
   * @param tsFile The TSFile containing the source code
   * @param captureName The name of the capture to retrieve (e.g., "type", "name", "modifiers")
   * @param localVariableDeclarationNode The variable declaration node
   * @return Optional containing the requested node if found, empty otherwise
   */
  public Optional<TSNode> getLocalVariableDeclarationChildNodeByCaptureName(
      TSFile tsFile, TSNode localVariableDeclarationNode, VariableCapture capture) {
    if (tsFile == null
        || tsFile.getTree() == null
        || (!localVariableDeclarationNode.getType().equals("field_declaration")
            && !localVariableDeclarationNode.getType().equals("formal_parameter")
            && !localVariableDeclarationNode.getType().equals("local_variable_declaration"))) {
      return Optional.empty();
    }
    List<Map<String, TSNode>> localVariableDeclarationInfo =
        this.getLocalVariableDeclarationNodeInfo(tsFile, localVariableDeclarationNode);
    for (Map<String, TSNode> map : localVariableDeclarationInfo) {
      TSNode node = map.get(capture.getCaptureName());
      if (node != null) {
        return Optional.of(node);
      }
    }
    return Optional.empty();
  }

  /**
   * Gets the name identifier node from a variable declaration.
   *
   * @param tsFile The TSFile containing the source code
   * @param localVariableDeclarationNode The variable declaration node
   * @return Optional containing the name identifier node if found, empty otherwise
   */
  public Optional<TSNode> getLocalVariableDeclarationNameNode(
      TSFile tsFile, TSNode localVariableDeclarationNode) {
    return this.getLocalVariableDeclarationChildNodeByCaptureName(
        tsFile, localVariableDeclarationNode, VariableCapture.VARIABLE_NAME);
  }

  /**
   * Gets the type node from a variable declaration.
   *
   * @param tsFile The TSFile containing the source code
   * @param localVariableDeclarationNode The variable declaration node
   * @return Optional containing the type node if found, empty otherwise
   */
  public Optional<TSNode> getLocalVariableDeclarationValueNode(
      TSFile tsFile, TSNode localVariableDeclarationNode) {
    return this.getLocalVariableDeclarationChildNodeByCaptureName(
        tsFile, localVariableDeclarationNode, VariableCapture.VARIABLE_TYPE);
  }

  /**
   * Gets the modifiers node from a variable declaration.
   *
   * @param tsFile The TSFile containing the source code
   * @param localVariableDeclarationNode The variable declaration node
   * @return Optional containing the modifiers node if found, empty otherwise
   */
  public Optional<TSNode> getLocalVariableDeclarationModifiersNode(
      TSFile tsFile, TSNode localVariableDeclarationNode) {
    return this.getLocalVariableDeclarationChildNodeByCaptureName(
        tsFile, localVariableDeclarationNode, VariableCapture.VARIABLE_MODIFIERS);
  }

  /**
   * Finds all variable declarations of a specific type in the file.
   *
   * @param tsFile The TSFile containing the source code
   * @param type The type to search for (e.g., "String", "int")
   * @return List of variable declaration nodes with matching type. Returns empty list if type is
   *     null or empty.
   *     <p>Query capture groups: - @type: The type node - @node: The entire declaration node
   *     <p>Example:
   *     <pre>
   * // Will find all these declarations when searching for "String":
   * private String field;
   * public void method(String param) { }
   * String localVar = "value";
   * </pre>
   */
  public List<TSNode> findLocalVariableDeclarationByType(TSFile tsFile, String type) {
    if (tsFile == null || tsFile.getTree() == null || Strings.isNullOrEmpty(type)) {
      return Collections.emptyList();
    }
    String queryString =
        String.format(
            """
            [
              (formal_parameter
                type: (_) %s
                (#eq? %s "%s")
              ) %s

              (local_variable_declaration
                type: (_) %s
                (#eq? %s "%s")
              ) %s

              (field_declaration
                type: (_) %s
                (#eq? %s "%s")
              ) %s
            ]
            """,
            VariableCapture.VARIABLE_TYPE,
            VariableCapture.VARIABLE_TYPE,
            type,
            VariableCapture.VARIABLE,
            VariableCapture.VARIABLE_TYPE,
            VariableCapture.VARIABLE_TYPE,
            type,
            VariableCapture.VARIABLE,
            VariableCapture.VARIABLE_TYPE,
            VariableCapture.VARIABLE_TYPE,
            type,
            VariableCapture.VARIABLE);
    return tsFile.query(queryString).execute().nodes();
  }
}
