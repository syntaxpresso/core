package io.github.syntaxpresso.core.service.java.language.extra;

import io.github.syntaxpresso.core.common.TSFile;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.treesitter.TSNode;

/**
 * Represents an annotation argument key-value pair with both TSNode references and convenience methods.
 * 
 * <p>This class encapsulates both the key and value nodes from an annotation argument,
 * providing direct access to the tree-sitter nodes as well as convenience methods
 * for extracting the text content.
 * 
 * <p>Usage example:
 * <pre>
 * AnnotationArgument nameArg = new AnnotationArgument(keyNode, valueNode);
 * TSNode keyNode = nameArg.getKeyNode();        // Direct node access
 * TSNode valueNode = nameArg.getValueNode();    // Direct node access  
 * String keyText = nameArg.getKey(tsFile);      // Convenience method
 * String valueText = nameArg.getValue(tsFile);  // Convenience method
 * </pre>
 *
 * @see TSFile
 * @see TSNode
 */
@Data
@RequiredArgsConstructor
public class AnnotationArgument {
  /**
   * The tree-sitter node representing the argument key (left side of assignment).
   */
  private final TSNode keyNode;
  
  /**
   * The tree-sitter node representing the argument value (right side of assignment).
   */
  private final TSNode valueNode;
  
  /**
   * Extracts the text content of the key node.
   * 
   * <p>Usage example:
   * <pre>
   * AnnotationArgument arg = new AnnotationArgument(keyNode, valueNode);
   * String keyText = arg.getKey(tsFile);  // "name"
   * </pre>
   * 
   * @param tsFile The TSFile containing the annotation
   * @return The key text (e.g., "name", "value", "schema")
   */
  public String getKey(TSFile tsFile) {
    return tsFile.getTextFromNode(this.keyNode);
  }
  
  /**
   * Extracts the text content of the value node.
   * 
   * <p>Usage example:
   * <pre>
   * AnnotationArgument arg = new AnnotationArgument(keyNode, valueNode);
   * String valueText = arg.getValue(tsFile);  // "\"users\""
   * </pre>
   * 
   * @param tsFile The TSFile containing the annotation
   * @return The value text (e.g., "\"users\"", "true", "42")
   */
  public String getValue(TSFile tsFile) {
    return tsFile.getTextFromNode(this.valueNode);
  }
}