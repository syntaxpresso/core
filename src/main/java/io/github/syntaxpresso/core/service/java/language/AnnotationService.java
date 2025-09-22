package io.github.syntaxpresso.core.service.java.language;

import com.google.common.annotations.Beta;
import com.google.common.base.Strings;
import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.service.java.language.extra.AnnotationArgument;
import io.github.syntaxpresso.core.service.java.language.extra.AnnotationCapture;
import io.github.syntaxpresso.core.service.java.language.extra.AnnotationInsertionPoint;
import io.github.syntaxpresso.core.service.java.language.extra.AnnotationInsertionPoint.AnnotationInsertionPosition;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.treesitter.TSNode;

public class AnnotationService {

  /**
   * Retrieves all annotation nodes (both marker and regular annotations) within a given scope.
   *
   * <p>This method searches for all annotation declarations within the specified scope node,
   * including both marker annotations (e.g., {@code @Override}) and annotations with arguments
   * (e.g., {@code @Table(name = "users")}). The search is performed using tree-sitter queries to
   * accurately identify annotation nodes at the AST level.
   *
   * <p>Usage example:
   *
   * <pre>
   * // Find all annotations on a class
   * TSNode classNode = tsFile.query("(class_declaration) @class").execute().firstNode();
   * List&lt;TSNode&gt; classAnnotations = service.getAllAnnotations(tsFile, classNode);
   * for (TSNode annotation : classAnnotations) {
   *   String annotationText = tsFile.getTextFromNode(annotation);
   *   // annotationText = "@Entity", "@Table(name = \"users\")", etc.
   * }
   *
   * // Find all annotations on a method
   * TSNode methodNode = tsFile.query("(method_declaration) @method").execute().firstNode();
   * List&lt;TSNode&gt; methodAnnotations = service.getAllAnnotations(tsFile, methodNode);
   * </pre>
   *
   * Query captures: - annotation: Regular annotation nodes - markerAnnotation: Marker annotation
   * nodes
   *
   * @param tsFile The {@link TSFile} containing the Java source code
   * @param scopeNode The scope node to search within (e.g., class, method, field declaration)
   * @return List of annotation nodes found within the scope. Returns empty list if no annotations
   *     found or invalid input.
   */
  public List<TSNode> getAllAnnotations(TSFile tsFile, TSNode scopeNode) {
    if (tsFile == null || tsFile.getTree() == null || scopeNode == null) {
      return Collections.emptyList();
    }
    String queryString =
        """
        (
          [
            (annotation) @annotation
            (marker_annotation) @markerAnnotation
          ]
        )
        """;
    return tsFile.query(queryString).execute().nodes();
  }

  /**
   * Gets detailed information about an annotation, including all its argument pairs.
   *
   * <p>This method analyzes an annotation node to extract information about its name and all
   * argument key-value pairs using tree-sitter queries. Each argument pair is returned as a
   * separate map entry in the result list.
   *
   * <p>Usage example:
   *
   * <pre>
   * Optional&lt;TSNode&gt; annotation = service.findAnnotationByName(tsFile, scopeNode, "Table");
   * List&lt;Map&lt;String, TSNode&gt;&gt; info = service.getAnnotationNodeInfo(tsFile, annotation.get());
   * for (Map&lt;String, TSNode&gt; argInfo : info) {
   *   TSNode nameNode = argInfo.get("name");         // The annotation name node
   *   TSNode keyNode = argInfo.get("key");           // The argument key node (if present)
   *   TSNode valueNode = argInfo.get("value");       // The argument value node (if present)
   *   TSNode entityNode = argInfo.get("entity");     // The entire annotation node
   * }
   * </pre>
   *
   * @param tsFile The {@link TSFile} containing the annotation
   * @param annotationNode The annotation node to analyze
   * @return List of maps with capture names: name, key, value, entity, argumentPair
   */
  private List<Map<String, TSNode>> getAnnotationNodeInfo(TSFile tsFile, TSNode annotationNode) {
    if (tsFile == null || tsFile.getTree() == null || annotationNode == null) {
      return Collections.emptyList();
    }
    String nodeType = annotationNode.getType();
    if (!nodeType.equals("annotation") && !nodeType.equals("marker_annotation")) {
      return Collections.emptyList();
    }
    String queryString =
        String.format(
            """
            [
              (annotation
                name: (_) %s
                arguments: (annotation_argument_list
                  (element_value_pair
                    key: (_) %s
                    value: (_) %s
                  ) %s
                )?
              ) %s
              (marker_annotation
                name: (_) %s
              ) %s
            ]
            """,
            AnnotationCapture.NAME.getCaptureWithAt(),
            AnnotationCapture.KEY.getCaptureWithAt(),
            AnnotationCapture.VALUE.getCaptureWithAt(),
            AnnotationCapture.ARGUMENT_PAIR.getCaptureWithAt(),
            AnnotationCapture.ENTITY.getCaptureWithAt(),
            AnnotationCapture.NAME.getCaptureWithAt(),
            AnnotationCapture.ENTITY.getCaptureWithAt());
    return tsFile
        .query(queryString)
        .within(annotationNode)
        .returningAllCaptures()
        .execute()
        .captures();
  }

  /**
   * Retrieves a specific node from an annotation using a capture name.
   *
   * @param tsFile The {@link TSFile} containing the annotation
   * @param annotationNode The annotation node to analyze
   * @param capture The capture name to retrieve
   * @return Optional containing the requested node if found, empty otherwise
   */
  private Optional<TSNode> getAnnotationNodeByCaptureName(
      TSFile tsFile, TSNode annotationNode, AnnotationCapture capture) {
    List<Map<String, TSNode>> annotationInfo = this.getAnnotationNodeInfo(tsFile, annotationNode);
    for (Map<String, TSNode> map : annotationInfo) {
      TSNode node = map.get(capture.getCaptureName());
      if (node != null) {
        return Optional.of(node);
      }
    }
    return Optional.empty();
  }

  /**
   * Gets all nodes from an annotation that match a specific capture name.
   *
   * @param tsFile The {@link TSFile} containing the annotation
   * @param annotationNode The annotation node to analyze
   * @param capture The capture name to retrieve nodes for
   * @return List of nodes matching the capture name
   */
  private List<TSNode> getAnnotationNodesFromCaptureName(
      TSFile tsFile, TSNode annotationNode, AnnotationCapture capture) {
    List<Map<String, TSNode>> annotationInfo = this.getAnnotationNodeInfo(tsFile, annotationNode);
    List<TSNode> foundNodes = new ArrayList<>();
    for (Map<String, TSNode> map : annotationInfo) {
      TSNode node = map.get(capture.getCaptureName());
      if (node != null) {
        foundNodes.add(node);
      }
    }
    return foundNodes;
  }

  /**
   * Gets the name identifier node from an annotation.
   *
   * <p>Usage example:
   *
   * <pre>
   * Optional&lt;TSNode&gt; nameNode = service.getAnnotationNameNode(tsFile, annotationNode);
   * if (nameNode.isPresent()) {
   *   String annotationName = tsFile.getTextFromNode(nameNode.get());
   *   // annotationName = "Table"
   * }
   * </pre>
   *
   * @param tsFile The {@link TSFile} containing the annotation
   * @param annotationNode The annotation node to analyze
   * @return Optional containing the annotation name node if found, empty otherwise
   */
  public Optional<TSNode> getAnnotationNameNode(TSFile tsFile, TSNode annotationNode) {
    return this.getAnnotationNodeByCaptureName(tsFile, annotationNode, AnnotationCapture.NAME);
  }

  /**
   * Gets all argument pair nodes from an annotation.
   *
   * <p>Usage example:
   *
   * <pre>
   * List&lt;TSNode&gt; argumentPairs = service.getAnnotationArgumentPairs(tsFile, annotationNode);
   * for (TSNode pair : argumentPairs) {
   *   String pairText = tsFile.getTextFromNode(pair);
   *   // pairText = "name = \"users\""
   * }
   * </pre>
   *
   * @param tsFile The {@link TSFile} containing the annotation
   * @param annotationNode The annotation node to analyze
   * @return List of argument pair nodes. Returns empty list if no arguments found.
   */
  public List<TSNode> getAnnotationArgumentPairs(TSFile tsFile, TSNode annotationNode) {
    return this.getAnnotationNodesFromCaptureName(
        tsFile, annotationNode, AnnotationCapture.ARGUMENT_PAIR);
  }

  /**
   * Gets all argument key nodes from an annotation.
   *
   * <p>Usage example:
   *
   * <pre>
   * List&lt;TSNode&gt; keys = service.getAnnotationKeys(tsFile, annotationNode);
   * for (TSNode key : keys) {
   *   String keyName = tsFile.getTextFromNode(key);
   *   // keyName = "name", "schema", etc.
   * }
   * </pre>
   *
   * @param tsFile The {@link TSFile} containing the annotation
   * @param annotationNode The annotation node to analyze
   * @return List of key nodes. Returns empty list if no arguments found.
   */
  public List<TSNode> getAnnotationKeys(TSFile tsFile, TSNode annotationNode) {
    return this.getAnnotationNodesFromCaptureName(tsFile, annotationNode, AnnotationCapture.KEY);
  }

  /**
   * Gets all argument value nodes from an annotation.
   *
   * <p>Usage example:
   *
   * <pre>
   * List&lt;TSNode&gt; values = service.getAnnotationValues(tsFile, annotationNode);
   * for (TSNode value : values) {
   *   String valueText = tsFile.getTextFromNode(value);
   *   // valueText = "\"users\"", "true", etc.
   * }
   * </pre>
   *
   * @param tsFile The {@link TSFile} containing the annotation
   * @param annotationNode The annotation node to analyze
   * @return List of value nodes. Returns empty list if no arguments found.
   */
  public List<TSNode> getAnnotationValues(TSFile tsFile, TSNode annotationNode) {
    return this.getAnnotationNodesFromCaptureName(tsFile, annotationNode, AnnotationCapture.VALUE);
  }

  /**
   * Finds a specific annotation argument value by its key name.
   *
   * <p>Usage example:
   *
   * <pre>
   * Optional&lt;TSNode&gt; nameValue = service.getAnnotationValueByKey(tsFile, annotationNode, "name");
   * if (nameValue.isPresent()) {
   *   String tableName = tsFile.getTextFromNode(nameValue.get());
   *   // tableName = "\"users\""
   * }
   * </pre>
   *
   * @param tsFile The {@link TSFile} containing the annotation
   * @param annotationNode The annotation node to analyze
   * @param keyName The key name to search for
   * @return Optional containing the value node if found, empty otherwise
   */
  public Optional<TSNode> getAnnotationValueByKey(
      TSFile tsFile, TSNode annotationNode, String keyName) {
    if (Strings.isNullOrEmpty(keyName)) {
      return Optional.empty();
    }

    List<Map<String, TSNode>> annotationInfo = this.getAnnotationNodeInfo(tsFile, annotationNode);
    for (Map<String, TSNode> map : annotationInfo) {
      TSNode keyNode = map.get(AnnotationCapture.KEY.getCaptureName());
      if (keyNode != null) {
        String key = tsFile.getTextFromNode(keyNode);
        if (keyName.equals(key)) {
          return Optional.ofNullable(map.get(AnnotationCapture.VALUE.getCaptureName()));
        }
      }
    }
    return Optional.empty();
  }

  /**
   * Gets annotation arguments as a map of key names to AnnotationArgument objects.
   *
   * <p>This method provides a convenient way to access annotation arguments with both the key and
   * value nodes available. Each argument is mapped by its key name for easy lookup.
   *
   * <p>Usage example:
   *
   * <pre>
   * Map&lt;String, AnnotationArgument&gt; args = service.getAnnotationArguments(tsFile, annotationNode);
   *
   * AnnotationArgument nameArg = args.get("name");
   * if (nameArg != null) {
   *   TSNode keyNode = nameArg.getKeyNode();        // Access to key node
   *   TSNode valueNode = nameArg.getValueNode();    // Access to value node
   *   String keyText = nameArg.getKey(tsFile);      // "name"
   *   String valueText = nameArg.getValue(tsFile);  // "\"users\""
   * }
   *
   * // Iterate through all arguments
   * for (Map.Entry&lt;String, AnnotationArgument&gt; entry : args.entrySet()) {
   *   String key = entry.getKey();                  // "name", "schema", etc.
   *   AnnotationArgument arg = entry.getValue();
   *   // Process argument...
   * }
   * </pre>
   *
   * @param tsFile The {@link TSFile} containing the annotation
   * @param annotationNode The annotation node to analyze
   * @return Map of key names to AnnotationArgument objects. Returns empty map if no arguments
   *     found.
   */
  public Map<String, AnnotationArgument> getAnnotationArguments(
      TSFile tsFile, TSNode annotationNode) {
    if (tsFile == null || tsFile.getTree() == null || annotationNode == null) {
      return Collections.emptyMap();
    }
    List<Map<String, TSNode>> annotationInfo = this.getAnnotationNodeInfo(tsFile, annotationNode);
    Map<String, AnnotationArgument> argumentMap = new HashMap<>();
    for (Map<String, TSNode> map : annotationInfo) {
      TSNode keyNode = map.get(AnnotationCapture.KEY.getCaptureName());
      TSNode valueNode = map.get(AnnotationCapture.VALUE.getCaptureName());

      if (keyNode != null && valueNode != null) {
        String keyText = tsFile.getTextFromNode(keyNode);
        argumentMap.put(keyText, new AnnotationArgument(keyNode, valueNode));
      }
    }
    return argumentMap;
  }

  /**
   * Finds an annotation by name within a given scope.
   *
   * <p>Usage example:
   *
   * <pre>
   * Optional&lt;TSNode&gt; tableAnnotation = service.findAnnotationByName(tsFile, classNode, "Table");
   * if (tableAnnotation.isPresent()) {
   *   String annotationText = tsFile.getTextFromNode(tableAnnotation.get());
   *   // annotationText = "@Table(name = \"users\")"
   * }
   * </pre>
   *
   * @param tsFile The {@link TSFile} containing the source code
   * @param scopeNode The scope node to search within
   * @param annotationName The name of the annotation to find
   * @return Optional containing the annotation node if found, empty otherwise
   */
  public Optional<TSNode> findAnnotationByName(
      TSFile tsFile, TSNode scopeNode, String annotationName) {
    if (tsFile == null || tsFile.getTree() == null || Strings.isNullOrEmpty(annotationName)) {
      return Optional.empty();
    }
    String queryString =
        String.format(
            """
            (
              [
                (annotation name: (_) @name)
                (marker_annotation name: (_) @name)
              ] @node
              (#eq? @name "%s")
            )
            """,
            annotationName);
    return tsFile.query(queryString).within(scopeNode).execute().firstNodeOptional();
  }

  /**
   * Determines the optimal insertion position for adding a new annotation to a declaration.
   *
   * <p>This method calculates the byte position where a new annotation should be inserted based on
   * the specified insertion strategy and existing annotations. It supports different insertion
   * positions to accommodate various annotation placement preferences.
   *
   * <p>Supported insertion positions:
   *
   * <ul>
   *   <li>{@code BEFORE_FIRST_ANNOTATION}: Inserts before the first existing annotation, or at the
   *       declaration start if no annotations exist
   *   <li>{@code ABOVE_SCOPE_DECLARATION}: Inserts directly above the declaration (class, method,
   *       or field)
   * </ul>
   *
   * <p>Usage example:
   *
   * <pre>
   * // Get insertion point before first annotation
   * TSNode classNode = tsFile.query("(class_declaration) @class").execute().firstNode();
   * AnnotationInsertionPoint insertionPoint = service.getAnnotationInsertionPosition(
   *     tsFile, classNode, AnnotationInsertionPosition.BEFORE_FIRST_ANNOTATION);
   *
   * if (insertionPoint != null) {
   *   int bytePosition = insertionPoint.getInsertByte();
   *   AnnotationInsertionPosition position = insertionPoint.getPosition();
   *   // Use insertion point to add annotation
   * }
   *
   * // Get insertion point above declaration
   * AnnotationInsertionPoint abovePoint = service.getAnnotationInsertionPosition(
   *     tsFile, methodNode, AnnotationInsertionPosition.ABOVE_SCOPE_DECLARATION);
   * </pre>
   *
   * @param tsFile The {@link TSFile} containing the Java source code
   * @param declarationNode The declaration node (class, method, or field) to add annotation to
   * @param insertionPoint The desired insertion position strategy
   * @return {@link AnnotationInsertionPoint} containing position details, or null if invalid input
   */
  @Beta
  public AnnotationInsertionPoint getAnnotationInsertionPosition(
      TSFile tsFile, TSNode declarationNode, AnnotationInsertionPosition insertionPoint) {
    if (tsFile == null || tsFile.getTree() == null || declarationNode == null) {
      return null;
    }
    String nodeType = declarationNode.getType();
    if (!nodeType.equals("class_declaration")
        && !nodeType.equals("field_declaration")
        && !nodeType.equals("method_declaration")) {
      return null;
    }
    List<TSNode> allAnnotations = this.getAllAnnotations(tsFile, declarationNode);
    AnnotationInsertionPoint annotationInsertionPoint = new AnnotationInsertionPoint();
    annotationInsertionPoint.setPosition(insertionPoint);
    if (insertionPoint.equals(AnnotationInsertionPosition.BEFORE_FIRST_ANNOTATION)) {
      if (!allAnnotations.isEmpty()) {
        annotationInsertionPoint.setInsertByte(allAnnotations.get(0).getStartByte());
      } else {
        annotationInsertionPoint.setInsertByte(declarationNode.getStartByte());
      }
    } else {
      if (allAnnotations.size() == 0) {
        annotationInsertionPoint.setInsertByte(declarationNode.getStartByte());
      } else {
        annotationInsertionPoint.setBreakLineBefore(true);
        annotationInsertionPoint.setInsertByte(allAnnotations.getLast().getEndByte());
      }
    }
    return annotationInsertionPoint;
  }

  /**
   * Adds a new annotation to a Java declaration at the specified insertion point.
   *
   * <p>This method inserts a new annotation into the source code at the position determined by the
   * provided {@link AnnotationInsertionPoint}. The annotation text is inserted with appropriate
   * line breaks to maintain proper formatting. The method supports adding annotations to classes,
   * methods, and fields.
   *
   * <p>The insertion behavior depends on the insertion point position:
   *
   * <ul>
   *   <li>{@code BEFORE_FIRST_ANNOTATION}: Inserts the annotation before existing annotations
   *   <li>{@code ABOVE_SCOPE_DECLARATION}: Inserts the annotation directly above the declaration
   * </ul>
   *
   * <p>Usage example:
   *
   * <pre>
   * // Add annotation to a class
   * TSNode classNode = tsFile.query("(class_declaration) @class").execute().firstNode();
   * AnnotationInsertionPoint insertionPoint = service.getAnnotationInsertionPosition(
   *     tsFile, classNode, AnnotationInsertionPosition.ABOVE_SCOPE_DECLARATION);
   *
   * service.addAnnotation(tsFile, classNode, insertionPoint, "@Entity");
   * // Result: @Entity is added above the class declaration
   *
   * // Add annotation with arguments
   * service.addAnnotation(tsFile, classNode, insertionPoint, "@Table(name = \"users\")");
   *
   * // Add annotation to a method
   * TSNode methodNode = tsFile.query("(method_declaration) @method").execute().firstNode();
   * AnnotationInsertionPoint methodPoint = service.getAnnotationInsertionPosition(
   *     tsFile, methodNode, AnnotationInsertionPosition.BEFORE_FIRST_ANNOTATION);
   * service.addAnnotation(tsFile, methodNode, methodPoint, "@Override");
   *
   * // Add annotation to a field
   * TSNode fieldNode = tsFile.query("(field_declaration) @field").execute().firstNode();
   * AnnotationInsertionPoint fieldPoint = service.getAnnotationInsertionPosition(
   *     tsFile, fieldNode, AnnotationInsertionPosition.ABOVE_SCOPE_DECLARATION);
   * service.addAnnotation(tsFile, fieldNode, fieldPoint, "@NotNull");
   * </pre>
   *
   * @param tsFile The {@link TSFile} containing the Java source code to modify
   * @param declarationNode The declaration node (class, method, or field) to add annotation to
   * @param insertionPoint The {@link AnnotationInsertionPoint} specifying where to insert
   * @param annotationText The annotation text to insert (e.g., "@Override", "@Entity", "@Table(name
   *     = \"users\")")
   */
  public void addAnnotation(
      TSFile tsFile,
      TSNode declarationNode,
      AnnotationInsertionPoint insertionPoint,
      String annotationText) {
    if (tsFile == null
        || tsFile.getTree() == null
        || declarationNode == null
        || insertionPoint == null
        || Strings.isNullOrEmpty(annotationText)) {
      return;
    }
    String nodeType = declarationNode.getType();
    if (!nodeType.equals("class_declaration")
        && !nodeType.equals("field_declaration")
        && !nodeType.equals("method_declaration")) {
      return;
    }
    String insertText = annotationText + "\n";
    if (insertionPoint.isBreakLineBefore()) {
      insertText = "\n" + insertText;
    }
    tsFile.updateSourceCode(
        insertionPoint.getInsertByte(), insertionPoint.getInsertByte(), insertText);
  }
}
