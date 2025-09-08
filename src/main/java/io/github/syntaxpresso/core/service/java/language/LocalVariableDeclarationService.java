package io.github.syntaxpresso.core.service.java.language;

import com.google.common.base.Strings;
import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.service.java.language.extra.VariableCapture;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
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
   * name, and value if present. Now also captures type components for generic and array types, as
   * well as the class type from initialization values.
   *
   * @param tsFile The TSFile containing the source code
   * @param localVariableDeclarationNode The variable declaration node to analyze
   * @return List of maps containing the captured nodes with their names. Returns empty list if file
   *     is null.
   *     <p>Query capture groups: - @variable_modifiers: Optional modifiers (e.g., private, final)
   *     - @variable_type: Full type node - @variable_type_base: Base type identifier (e.g., "List"
   *     in "List<String>") - @variable_type_argument: Type arguments in generics (e.g., "String" in
   *     "List<String>") - @variable_name: Variable name identifier - @variable_value: Optional
   *     initialization value - @variable_value_type: Class type in object creation (e.g., "Test" in
   *     "new Test()") - @variable_value_type_argument: Type argument in generic object creation
   *     (e.g., "Test" in "new ArrayList<Test>()") - @variable: The entire declaration node
   *     <p>Example:
   *     <pre>
   * // For this declaration:
   * List<String> names = new ArrayList<String>();
   *
   * // The captures will be:
   * variable_type -> "List<String>"
   * variable_type_base -> "List"
   * variable_type_argument -> "String"
   * variable_name -> "names"
   * variable_value -> "new ArrayList<String>()"
   * variable_value_type -> "ArrayList"
   * variable_value_type_argument -> "String"
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
                type: (type_identifier) %s %s
                declarator: (variable_declarator
                  name: (identifier) %s
                  value: (object_creation_expression
                    type: (type_identifier) %s
                  )? %s
                )
              ) %s

              (local_variable_declaration
                (modifiers)? %s
                type: (type_identifier) %s %s
                declarator: (variable_declarator
                  name: (identifier) %s
                  value: (object_creation_expression
                    type: (generic_type
                      (type_identifier) %s
                      (type_arguments
                        (_) %s
                      )
                    )
                  )? %s
                )
              ) %s

              (local_variable_declaration
                (modifiers)? %s
                type: (generic_type
                  (type_identifier) %s
                  (type_arguments
                    (_) %s
                  )
                ) %s
                declarator: (variable_declarator
                  name: (identifier) %s
                  value: (object_creation_expression
                    type: (type_identifier) %s
                  )? %s
                )
              ) %s

              (local_variable_declaration
                (modifiers)? %s
                type: (generic_type
                  (type_identifier) %s
                  (type_arguments
                    (_) %s
                  )
                ) %s
                declarator: (variable_declarator
                  name: (identifier) %s
                  value: (object_creation_expression
                    type: (generic_type
                      (type_identifier) %s
                      (type_arguments
                        (_) %s
                      )
                    )
                  )? %s
                )
              ) %s

              (local_variable_declaration
                (modifiers)? %s
                type: (array_type
                  element: (_) %s
                ) %s
                declarator: (variable_declarator
                  name: (identifier) %s
                  value: (_)? %s
                )
              ) %s

              (formal_parameter
                (modifiers)? %s
                type: (type_identifier) %s %s
                name: (identifier) %s
              ) %s

              (formal_parameter
                (modifiers)? %s
                type: (generic_type
                  (type_identifier) %s
                  (type_arguments
                    (_) %s
                  )
                ) %s
                name: (identifier) %s
              ) %s

              (formal_parameter
                (modifiers)? %s
                type: (array_type
                  element: (_) %s
                ) %s
                name: (identifier) %s
              ) %s

              (field_declaration
                (modifiers)? %s
                type: (type_identifier) %s %s
                declarator: (variable_declarator
                  name: (identifier) %s
                  value: (object_creation_expression
                    type: (type_identifier) %s
                  )? %s
                )
              ) %s

              (field_declaration
                (modifiers)? %s
                type: (type_identifier) %s %s
                declarator: (variable_declarator
                  name: (identifier) %s
                  value: (object_creation_expression
                    type: (generic_type
                      (type_identifier) %s
                      (type_arguments
                        (_) %s
                      )
                    )
                  )? %s
                )
              ) %s

              (field_declaration
                (modifiers)? %s
                type: (generic_type
                  (type_identifier) %s
                  (type_arguments
                    (_) %s
                  )
                ) %s
                declarator: (variable_declarator
                  name: (identifier) %s
                  value: (object_creation_expression
                    type: (type_identifier) %s
                  )? %s
                )
              ) %s

              (field_declaration
                (modifiers)? %s
                type: (generic_type
                  (type_identifier) %s
                  (type_arguments
                    (_) %s
                  )
                ) %s
                declarator: (variable_declarator
                  name: (identifier) %s
                  value: (object_creation_expression
                    type: (generic_type
                      (type_identifier) %s
                      (type_arguments
                        (_) %s
                      )
                    )
                  )? %s
                )
              ) %s

              (field_declaration
                (modifiers)? %s
                type: (array_type
                  element: (_) %s
                ) %s
                declarator: (variable_declarator
                  name: (identifier) %s
                  value: (_)? %s
                )
              ) %s
            ]
            """,
            // local_variable_declaration with type_identifier and simple object creation
            VariableCapture.VARIABLE_MODIFIERS.getCaptureWithAt(),
            VariableCapture.VARIABLE_TYPE.getCaptureWithAt(),
            VariableCapture.VARIABLE_TYPE_BASE.getCaptureWithAt(),
            VariableCapture.VARIABLE_NAME.getCaptureWithAt(),
            VariableCapture.VARIABLE_VALUE_TYPE.getCaptureWithAt(),
            VariableCapture.VARIABLE_VALUE.getCaptureWithAt(),
            VariableCapture.VARIABLE.getCaptureWithAt(),

            // local_variable_declaration with type_identifier and generic object creation
            VariableCapture.VARIABLE_MODIFIERS.getCaptureWithAt(),
            VariableCapture.VARIABLE_TYPE.getCaptureWithAt(),
            VariableCapture.VARIABLE_TYPE_BASE.getCaptureWithAt(),
            VariableCapture.VARIABLE_NAME.getCaptureWithAt(),
            VariableCapture.VARIABLE_VALUE_TYPE.getCaptureWithAt(),
            VariableCapture.VARIABLE_VALUE_TYPE_ARGUMENT.getCaptureWithAt(),
            VariableCapture.VARIABLE_VALUE.getCaptureWithAt(),
            VariableCapture.VARIABLE.getCaptureWithAt(),

            // local_variable_declaration with generic_type and simple object creation
            VariableCapture.VARIABLE_MODIFIERS.getCaptureWithAt(),
            VariableCapture.VARIABLE_TYPE_BASE.getCaptureWithAt(),
            VariableCapture.VARIABLE_TYPE_ARGUMENT.getCaptureWithAt(),
            VariableCapture.VARIABLE_TYPE.getCaptureWithAt(),
            VariableCapture.VARIABLE_NAME.getCaptureWithAt(),
            VariableCapture.VARIABLE_VALUE_TYPE.getCaptureWithAt(),
            VariableCapture.VARIABLE_VALUE.getCaptureWithAt(),
            VariableCapture.VARIABLE.getCaptureWithAt(),

            // local_variable_declaration with generic_type and generic object creation
            VariableCapture.VARIABLE_MODIFIERS.getCaptureWithAt(),
            VariableCapture.VARIABLE_TYPE_BASE.getCaptureWithAt(),
            VariableCapture.VARIABLE_TYPE_ARGUMENT.getCaptureWithAt(),
            VariableCapture.VARIABLE_TYPE.getCaptureWithAt(),
            VariableCapture.VARIABLE_NAME.getCaptureWithAt(),
            VariableCapture.VARIABLE_VALUE_TYPE.getCaptureWithAt(),
            VariableCapture.VARIABLE_VALUE_TYPE_ARGUMENT.getCaptureWithAt(),
            VariableCapture.VARIABLE_VALUE.getCaptureWithAt(),
            VariableCapture.VARIABLE.getCaptureWithAt(),

            // local_variable_declaration with array_type
            VariableCapture.VARIABLE_MODIFIERS.getCaptureWithAt(),
            VariableCapture.VARIABLE_TYPE_BASE.getCaptureWithAt(),
            VariableCapture.VARIABLE_TYPE.getCaptureWithAt(),
            VariableCapture.VARIABLE_NAME.getCaptureWithAt(),
            VariableCapture.VARIABLE_VALUE.getCaptureWithAt(),
            VariableCapture.VARIABLE.getCaptureWithAt(),

            // formal_parameter with type_identifier
            VariableCapture.VARIABLE_MODIFIERS.getCaptureWithAt(),
            VariableCapture.VARIABLE_TYPE.getCaptureWithAt(),
            VariableCapture.VARIABLE_TYPE_BASE.getCaptureWithAt(),
            VariableCapture.VARIABLE_NAME.getCaptureWithAt(),
            VariableCapture.VARIABLE.getCaptureWithAt(),

            // formal_parameter with generic_type
            VariableCapture.VARIABLE_MODIFIERS.getCaptureWithAt(),
            VariableCapture.VARIABLE_TYPE_BASE.getCaptureWithAt(),
            VariableCapture.VARIABLE_TYPE_ARGUMENT.getCaptureWithAt(),
            VariableCapture.VARIABLE_TYPE.getCaptureWithAt(),
            VariableCapture.VARIABLE_NAME.getCaptureWithAt(),
            VariableCapture.VARIABLE.getCaptureWithAt(),

            // formal_parameter with array_type
            VariableCapture.VARIABLE_MODIFIERS.getCaptureWithAt(),
            VariableCapture.VARIABLE_TYPE_BASE.getCaptureWithAt(),
            VariableCapture.VARIABLE_TYPE.getCaptureWithAt(),
            VariableCapture.VARIABLE_NAME.getCaptureWithAt(),
            VariableCapture.VARIABLE.getCaptureWithAt(),

            // field_declaration with type_identifier and simple object creation
            VariableCapture.VARIABLE_MODIFIERS.getCaptureWithAt(),
            VariableCapture.VARIABLE_TYPE.getCaptureWithAt(),
            VariableCapture.VARIABLE_TYPE_BASE.getCaptureWithAt(),
            VariableCapture.VARIABLE_NAME.getCaptureWithAt(),
            VariableCapture.VARIABLE_VALUE_TYPE.getCaptureWithAt(),
            VariableCapture.VARIABLE_VALUE.getCaptureWithAt(),
            VariableCapture.VARIABLE.getCaptureWithAt(),

            // field_declaration with type_identifier and generic object creation
            VariableCapture.VARIABLE_MODIFIERS.getCaptureWithAt(),
            VariableCapture.VARIABLE_TYPE.getCaptureWithAt(),
            VariableCapture.VARIABLE_TYPE_BASE.getCaptureWithAt(),
            VariableCapture.VARIABLE_NAME.getCaptureWithAt(),
            VariableCapture.VARIABLE_VALUE_TYPE.getCaptureWithAt(),
            VariableCapture.VARIABLE_VALUE_TYPE_ARGUMENT.getCaptureWithAt(),
            VariableCapture.VARIABLE_VALUE.getCaptureWithAt(),
            VariableCapture.VARIABLE.getCaptureWithAt(),

            // field_declaration with generic_type and simple object creation
            VariableCapture.VARIABLE_MODIFIERS.getCaptureWithAt(),
            VariableCapture.VARIABLE_TYPE_BASE.getCaptureWithAt(),
            VariableCapture.VARIABLE_TYPE_ARGUMENT.getCaptureWithAt(),
            VariableCapture.VARIABLE_TYPE.getCaptureWithAt(),
            VariableCapture.VARIABLE_NAME.getCaptureWithAt(),
            VariableCapture.VARIABLE_VALUE_TYPE.getCaptureWithAt(),
            VariableCapture.VARIABLE_VALUE.getCaptureWithAt(),
            VariableCapture.VARIABLE.getCaptureWithAt(),

            // field_declaration with generic_type and generic object creation
            VariableCapture.VARIABLE_MODIFIERS.getCaptureWithAt(),
            VariableCapture.VARIABLE_TYPE_BASE.getCaptureWithAt(),
            VariableCapture.VARIABLE_TYPE_ARGUMENT.getCaptureWithAt(),
            VariableCapture.VARIABLE_TYPE.getCaptureWithAt(),
            VariableCapture.VARIABLE_NAME.getCaptureWithAt(),
            VariableCapture.VARIABLE_VALUE_TYPE.getCaptureWithAt(),
            VariableCapture.VARIABLE_VALUE_TYPE_ARGUMENT.getCaptureWithAt(),
            VariableCapture.VARIABLE_VALUE.getCaptureWithAt(),
            VariableCapture.VARIABLE.getCaptureWithAt(),

            // field_declaration with array_type
            VariableCapture.VARIABLE_MODIFIERS.getCaptureWithAt(),
            VariableCapture.VARIABLE_TYPE_BASE.getCaptureWithAt(),
            VariableCapture.VARIABLE_TYPE.getCaptureWithAt(),
            VariableCapture.VARIABLE_NAME.getCaptureWithAt(),
            VariableCapture.VARIABLE_VALUE.getCaptureWithAt(),
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
   * Gets the value node from a variable declaration.
   *
   * @param tsFile The TSFile containing the source code
   * @param localVariableDeclarationNode The variable declaration node
   * @return Optional containing the value node if found, empty otherwise
   */
  public Optional<TSNode> getLocalVariableDeclarationValueNode(
      TSFile tsFile, TSNode localVariableDeclarationNode) {
    return this.getLocalVariableDeclarationChildNodeByCaptureName(
        tsFile, localVariableDeclarationNode, VariableCapture.VARIABLE_VALUE);
  }

  /**
   * Retrieves the complete type node of a variable declaration, including generic type information
   * if present.
   *
   * <p>This method extracts the full type node from a variable declaration (local variable, field,
   * or parameter), which includes the base type and any generic type arguments. For example, in
   * {@code List<String> items}, it would return the entire {@code List<String>} type node. For
   * simple types like {@code String name}, it returns the {@code String} type node.
   *
   * <p>Usage example:
   *
   * <pre>
   * List&lt;TSNode&gt; varNodes = service.getAllMethodParameterNodes(tsFile);
   * Optional&lt;TSNode&gt; fullTypeNode = service.getVariableDeclarationFullTypeNode(tsFile, varNodes.get(0));
   * if (fullTypeNode.isPresent()) {
   *   String type = tsFile.getTextFromNode(fullTypeNode.get());  // e.g., "List&lt;String&gt;" or "String"
   * }
   * </pre>
   *
   * @param tsFile The {@link TSFile} containing the source code
   * @param variableDeclarationNode The variable declaration {@link TSNode} to analyze (can be
   *     local_variable_declaration, field_declaration, or formal_parameter)
   * @return An {@link Optional} containing the complete type node if found
   */
  public Optional<TSNode> getVariableDeclarationFullTypeNode(
      TSFile tsFile, TSNode variableDeclarationNode) {
    return this.getLocalVariableDeclarationChildNodeByCaptureName(
        tsFile, variableDeclarationNode, VariableCapture.VARIABLE_TYPE);
  }

  /**
   * Retrieves the actual class type node from a variable declaration, extracting the innermost
   * type.
   *
   * <p>This method extracts the actual class type from a variable declaration. For generic types,
   * it returns the type argument (the class inside the generics). For example, in {@code
   * List<String> items}, it would return the {@code String} type node. For simple types like {@code
   * String name}, it also returns the {@code String} type node. For array types like {@code
   * String[] names}, it returns the element type {@code String}.
   *
   * <p>This is useful when you need to know the actual data type being stored, regardless of
   * whether it's in a collection, array, or standalone.
   *
   * <p>Usage example:
   *
   * <pre>
   * List&lt;TSNode&gt; varNodes = service.getAllMethodParameterNodes(tsFile);
   * Optional&lt;TSNode&gt; classTypeNode = service.getVariableDeclarationClassTypeNode(tsFile, varNodes.get(0));
   * if (classTypeNode.isPresent()) {
   *   String type = tsFile.getTextFromNode(classTypeNode.get());
   *   // Returns "String" for: String name, List&lt;String&gt; items, or String[] names
   * }
   * </pre>
   *
   * @param tsFile The {@link TSFile} containing the source code
   * @param variableDeclarationNode The variable declaration {@link TSNode} to analyze (can be
   *     local_variable_declaration, field_declaration, or formal_parameter)
   * @return An {@link Optional} containing the class type node if found
   */
  public Optional<TSNode> getVariableDeclarationClassTypeNode(
      TSFile tsFile, TSNode variableDeclarationNode) {
    Optional<TSNode> typeArgumentNode =
        this.getLocalVariableDeclarationChildNodeByCaptureName(
            tsFile, variableDeclarationNode, VariableCapture.VARIABLE_TYPE_ARGUMENT);
    if (typeArgumentNode.isPresent()) {
      return typeArgumentNode;
    }
    Optional<TSNode> typeBaseNode =
        this.getLocalVariableDeclarationChildNodeByCaptureName(
            tsFile, variableDeclarationNode, VariableCapture.VARIABLE_TYPE_BASE);
    if (typeBaseNode.isPresent()) {
      return typeBaseNode;
    }
    return this.getLocalVariableDeclarationChildNodeByCaptureName(
        tsFile, variableDeclarationNode, VariableCapture.VARIABLE_TYPE);
  }

  /**
   * Gets the base type node from a variable declaration (e.g., "List" in "List<String>").
   *
   * @param tsFile The TSFile containing the source code
   * @param localVariableDeclarationNode The variable declaration node
   * @return Optional containing the base type node if found, empty otherwise
   */
  public Optional<TSNode> getLocalVariableDeclarationTypeBaseNode(
      TSFile tsFile, TSNode localVariableDeclarationNode) {
    return this.getLocalVariableDeclarationChildNodeByCaptureName(
        tsFile, localVariableDeclarationNode, VariableCapture.VARIABLE_TYPE_BASE);
  }

  /**
   * Gets the type argument node from a generic variable declaration (e.g., "String" in
   * "List<String>").
   *
   * @param tsFile The TSFile containing the source code
   * @param localVariableDeclarationNode The variable declaration node
   * @return Optional containing the type argument node if found, empty otherwise
   */
  public Optional<TSNode> getLocalVariableDeclarationTypeArgumentNode(
      TSFile tsFile, TSNode localVariableDeclarationNode) {
    return this.getLocalVariableDeclarationChildNodeByCaptureName(
        tsFile, localVariableDeclarationNode, VariableCapture.VARIABLE_TYPE_ARGUMENT);
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
   * Gets the class type from the initialization value of a variable declaration. For example, gets
   * "Test" from "new Test()" or "ArrayList" from "new ArrayList<String>()".
   *
   * @param tsFile The TSFile containing the source code
   * @param localVariableDeclarationNode The variable declaration node
   * @return Optional containing the value type node if found, empty otherwise
   */
  public Optional<TSNode> getLocalVariableDeclarationValueTypeNode(
      TSFile tsFile, TSNode localVariableDeclarationNode) {
    return this.getLocalVariableDeclarationChildNodeByCaptureName(
        tsFile, localVariableDeclarationNode, VariableCapture.VARIABLE_VALUE_TYPE);
  }

  /**
   * Gets the type argument from the initialization value of a generic variable declaration. For
   * example, gets "String" from "new ArrayList<String>()" or "Test" from "new HashMap<String,
   * Test>()".
   *
   * @param tsFile The TSFile containing the source code
   * @param localVariableDeclarationNode The variable declaration node
   * @return Optional containing the value type argument node if found, empty otherwise
   */
  public Optional<TSNode> getLocalVariableDeclarationValueTypeArgumentNode(
      TSFile tsFile, TSNode localVariableDeclarationNode) {
    return this.getLocalVariableDeclarationChildNodeByCaptureName(
        tsFile, localVariableDeclarationNode, VariableCapture.VARIABLE_VALUE_TYPE_ARGUMENT);
  }

  /**
   * Gets the actual class type from the variable's initialization value, prioritizing the type
   * argument if present.
   *
   * <p>This method extracts the most specific class type from the initialization value. For generic
   * object creation like {@code new ArrayList<Test>()}, it returns the type argument {@code Test}.
   * For simple object creation like {@code new Test()}, it returns the class type {@code Test}.
   *
   * <p>Usage example:
   *
   * <pre>
   * // For: Test test = new Test();
   * Optional&lt;TSNode&gt; valueClass = service.getVariableInitializationClassTypeNode(tsFile, varNode);
   * // Returns "Test"
   *
   * // For: List&lt;Test&gt; tests = new ArrayList&lt;Test&gt;();
   * Optional&lt;TSNode&gt; valueClass = service.getVariableInitializationClassTypeNode(tsFile, varNode);
   * // Returns "Test" (the type argument)
   * </pre>
   *
   * @param tsFile The {@link TSFile} containing the source code
   * @param variableDeclarationNode The variable declaration {@link TSNode} to analyze
   * @return An {@link Optional} containing the class type from the initialization value
   */
  public Optional<TSNode> getLocalVariableValueClassTypeNode(
      TSFile tsFile, TSNode variableDeclarationNode) {
    Optional<TSNode> valueTypeArgumentNode =
        this.getLocalVariableDeclarationChildNodeByCaptureName(
            tsFile, variableDeclarationNode, VariableCapture.VARIABLE_VALUE_TYPE_ARGUMENT);
    if (valueTypeArgumentNode.isPresent()) {
      return valueTypeArgumentNode;
    }
    return this.getLocalVariableDeclarationChildNodeByCaptureName(
        tsFile, variableDeclarationNode, VariableCapture.VARIABLE_VALUE_TYPE);
  }

  /**
   * Finds all variable declarations of a specific type in the file.
   *
   * @param tsFile The TSFile containing the source code
   * @param type The type to search for (e.g., "String", "int", "Test")
   * @return List of variable declaration nodes with matching type. Returns empty list if type is
   *     null or empty.
   *     <p>Query capture groups: - @variable_type: The type node - @variable: The entire
   *     declaration node
   *     <p>Example:
   *     <pre>
   * // Will find all these declarations when searching for "Test":
   * private Test field;
   * public void method(Test param) { }
   * Test localVar = new Test();
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
                type: (_) @variable_type
                (#eq? @variable_type "%s")
              ) @variable

              (local_variable_declaration
                type: (_) @variable_type
                (#eq? @variable_type "%s")
              ) @variable

              (field_declaration
                type: (_) @variable_type
                (#eq? @variable_type "%s")
              ) @variable
            ]
            """,
            type, type, type);
    return tsFile.query(queryString).execute().nodes();
  }

  public List<TSNode> findAllIdentifierNodes(
      TSFile tsFile, TSNode scopeNode, String identiferText) {
    if (tsFile == null
        || tsFile.getTree() == null
        || scopeNode == null
        || Strings.isNullOrEmpty(identiferText)) {
      return Collections.emptyList();
    }
    String queryString =
        String.format(
            """
            (
              (identifier) @usage
                (#eq? @usage "%s")
            )
            """,
            identiferText);
    return tsFile.query(queryString).within(scopeNode).execute().nodes();
  }

  /**
   * Finds all usages of a variable within its valid scope.
   *
   * <p>This method determines the appropriate scope for a variable based on its declaration type:
   *
   * <ul>
   *   <li>Local variables: searches within the enclosing block or method
   *   <li>Parameters: searches within the method body
   *   <li>Fields: searches within the entire class
   * </ul>
   *
   * @param tsFile The TSFile containing the source code
   * @param variableDeclarationNode The variable declaration node (local_variable_declaration,
   *     formal_parameter, or field_declaration)
   * @return List of identifier nodes representing usages of the variable
   */
  public List<TSNode> findVariableUsagesInScope(TSFile tsFile, TSNode variableDeclarationNode) {
    if (tsFile == null || tsFile.getTree() == null || variableDeclarationNode == null) {
      return Collections.emptyList();
    }
    Optional<TSNode> nameNode =
        getLocalVariableDeclarationNameNode(tsFile, variableDeclarationNode);
    if (nameNode.isEmpty()) {
      return Collections.emptyList();
    }
    String variableName = tsFile.getTextFromNode(nameNode.get());
    TSNode scopeNode = determineScopeForVariable(variableDeclarationNode);
    if (scopeNode == null) {
      return Collections.emptyList();
    }
    return findVariableUsagesInNode(tsFile, scopeNode, variableName, variableDeclarationNode);
  }

  /**
   * Determines the appropriate scope node for a variable based on its declaration type.
   *
   * @param variableDeclarationNode The variable declaration node
   * @return The scope node within which the variable is valid, or null if not found
   */
  public TSNode determineScopeForVariable(TSNode variableDeclarationNode) {
    String nodeType = variableDeclarationNode.getType();
    TSNode current = variableDeclarationNode;
    switch (nodeType) {
      case "local_variable_declaration":
        while (current != null && !current.isNull()) {
          TSNode parent = current.getParent();
          if (parent != null
              && !parent.isNull()
              && (parent.getType().equals("block")
                  || parent.getType().equals("method_body")
                  || parent.getType().equals("constructor_body"))) {
            return parent;
          }
          current = parent;
        }
        break;
      case "formal_parameter":
        while (current != null && !current.isNull()) {
          TSNode parent = current.getParent();
          if (parent != null
              && !parent.isNull()
              && (parent.getType().equals("method_declaration")
                  || parent.getType().equals("constructor_declaration"))) {
            for (int i = 0; i < parent.getChildCount(); i++) {
              TSNode child = parent.getChild(i);
              if (child.getType().equals("method_body")
                  || child.getType().equals("constructor_body")) {
                return child;
              }
            }
          }
          current = parent;
        }
        break;
      case "field_declaration":
        while (current != null && !current.isNull()) {
          TSNode parent = current.getParent();
          if (parent != null && !parent.isNull() && parent.getType().equals("class_body")) {
            return parent;
          }
          current = parent;
        }
        break;
    }
    return null;
  }

  /**
   * Finds all usages of a variable within a given scope node.
   *
   * @param tsFile The TSFile containing the source code
   * @param scopeNode The node defining the scope to search within
   * @param variableName The name of the variable to find
   * @param declarationNode The original declaration node to exclude from results
   * @return List of identifier nodes representing usages of the variable
   */
  private List<TSNode> findVariableUsagesInNode(
      TSFile tsFile, TSNode scopeNode, String variableName, TSNode declarationNode) {
    String queryString =
        String.format(
            """
            (identifier) @usage
              (#eq? @usage "%s")
            """,
            variableName);
    List<TSNode> allUsages = tsFile.query(queryString).within(scopeNode).execute().nodes();
    return allUsages.stream()
        .filter(usage -> usage != null && !usage.isNull())
        .filter(usage -> !isPartOfDeclaration(tsFile, usage, declarationNode))
        .filter(usage -> isInCorrectScope(tsFile, usage, declarationNode, variableName))
        .collect(Collectors.toList());
  }

  /**
   * Checks if an identifier node is part of the variable's declaration.
   *
   * @param tsFile The TSFile containing the source code
   * @param identifierNode The identifier node to check
   * @param declarationNode The declaration node
   * @return true if the identifier is part of the declaration, false otherwise
   */
  private boolean isPartOfDeclaration(
      TSFile tsFile, TSNode identifierNode, TSNode declarationNode) {
    Optional<TSNode> declNameNode = getLocalVariableDeclarationNameNode(tsFile, declarationNode);
    if (declNameNode.isEmpty()) {
      return false;
    }
    return identifierNode.equals(declNameNode.get());
  }

  /**
   * Checks if a usage is in the correct scope for the variable. This handles shadowing - ensures we
   * don't include usages that belong to a different variable with the same name.
   *
   * @param tsFile The TSFile containing the source code
   * @param usageNode The usage identifier node
   * @param declarationNode The original declaration node
   * @param variableName The variable name
   * @return true if the usage belongs to this variable declaration, false if it's shadowed
   */
  private boolean isInCorrectScope(
      TSFile tsFile, TSNode usageNode, TSNode declarationNode, String variableName) {
    if (declarationNode.getType().equals("field_declaration")) {
      TSNode current = usageNode.getParent();
      while (current != null && !current.isNull()) {
        if (current.getType().equals("method_declaration")
            || current.getType().equals("constructor_declaration")) {
          for (int i = 0; i < current.getChildCount(); i++) {
            TSNode child = current.getChild(i);
            if (child.getType().equals("formal_parameters")) {
              if (hasParameterWithName(tsFile, child, variableName)) {
                return false;
              }
            }
          }
        }
        if (current.getType().equals("block")) {
          if (hasLocalVariableBeforeUsage(tsFile, current, variableName, usageNode)) {
            return false;
          }
        }
        current = current.getParent();
      }
    }
    return true;
  }

  /** Checks if a formal_parameters node contains a parameter with the given name. */
  private boolean hasParameterWithName(TSFile tsFile, TSNode parametersNode, String name) {
    for (int i = 0; i < parametersNode.getChildCount(); i++) {
      TSNode child = parametersNode.getChild(i);
      if (child.getType().equals("formal_parameter")) {
        for (int j = 0; j < child.getChildCount(); j++) {
          TSNode paramChild = child.getChild(j);
          if (paramChild.getType().equals("identifier")
              && tsFile.getTextFromNode(paramChild).equals(name)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  /** Checks if there's a local variable declaration with the given name before the usage point. */
  private boolean hasLocalVariableBeforeUsage(
      TSFile tsFile, TSNode blockNode, String name, TSNode usageNode) {
    for (int i = 0; i < blockNode.getChildCount(); i++) {
      TSNode child = blockNode.getChild(i);
      if (child.getType().equals("local_variable_declaration")) {
        for (int j = 0; j < child.getChildCount(); j++) {
          TSNode declChild = child.getChild(j);
          if (declChild.getType().equals("variable_declarator")) {
            for (int k = 0; k < declChild.getChildCount(); k++) {
              TSNode nameNode = declChild.getChild(k);
              if (nameNode.getType().equals("identifier")
                  && tsFile.getTextFromNode(nameNode).equals(name)) {
                if (child.getStartByte() < usageNode.getStartByte()) {
                  return true;
                }
              }
            }
          }
        }
      }
    }
    return false;
  }

  /**
   * Finds all references to a specific type within a given scope. This is useful for finding where
   * a class is used (as variable types, method parameters, etc.)
   *
   * @param tsFile The TSFile containing the source code
   * @param scopeNode The scope to search within (e.g., a class body or method)
   * @param typeName The name of the type to find
   * @return List of type_identifier nodes representing uses of the type
   */
  public List<TSNode> findTypeReferencesInScope(TSFile tsFile, TSNode scopeNode, String typeName) {
    if (tsFile == null
        || tsFile.getTree() == null
        || scopeNode == null
        || Strings.isNullOrEmpty(typeName)) {
      return Collections.emptyList();
    }
    String queryString =
        String.format(
            """
            [
              (type_identifier) @type_ref
                (#eq? @type_ref "%s")
              (generic_type
                (type_identifier) @type_ref
                (#eq? @type_ref "%s")
              )
              (generic_type
                (type_arguments
                  (type_identifier) @type_ref
                  (#eq? @type_ref "%s")
                )
              )
              (object_creation_expression
                type: (type_identifier) @type_ref
                (#eq? @type_ref "%s")
              )
              (object_creation_expression
                type: (generic_type
                  (type_identifier) @type_ref
                  (#eq? @type_ref "%s")
                )
              )
              (object_creation_expression
                type: (generic_type
                  (type_arguments
                    (type_identifier) @type_ref
                    (#eq? @type_ref "%s")
                  )
                )
              )
            ]
            """,
            typeName, typeName, typeName, typeName, typeName, typeName);
    return tsFile.query(queryString).within(scopeNode).execute().nodes();
  }
}
