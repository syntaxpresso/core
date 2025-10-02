package io.github.syntaxpresso.core.service.java.language;

import com.google.common.base.Strings;
import io.github.syntaxpresso.core.common.TSFile;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.treesitter.TSNode;

@Getter
@RequiredArgsConstructor
public class AnnotationDeclarationService {

  /**
   * Finds an annotation declaration node by its name.
   *
   * <p>Usage example:
   *
   * <pre>
   * Optional<TSNode> annotationNode = service.findAnnotationByName(tsFile, "MyAnnotation");
   * if (annotationNode.isPresent()) {
   *   String annotationDecl = tsFile.getTextFromNode(annotationNode.get());
   *   // annotationDecl = "public @interface MyAnnotation { String value(); }"
   * }
   * </pre>
   *
   * @param tsFile {@link TSFile} containing the source code
   * @param annotationName Name of the annotation to search for
   * @return {@link Optional} containing the annotation declaration node, or empty if not found,
   *     file/tree is null, or annotationName is empty
   */
  public Optional<TSNode> findAnnotationByName(TSFile tsFile, String annotationName) {
    if (tsFile == null || tsFile.getTree() == null || Strings.isNullOrEmpty(annotationName)) {
      return Optional.empty();
    }
    String queryString =
        String.format(
            """
            (annotation_type_declaration
              name: (identifier) @annotationName
            (#eq? @annotationName "%s")) @annotationDeclaration
            """,
            annotationName);
    return tsFile
        .query(queryString)
        .returning("annotationDeclaration")
        .execute()
        .firstNodeOptional();
  }

  /**
   * Gets the public annotation of a file, defined as the public annotation whose name matches the
   * file name. If the file name is not available, falls back to finding the first public annotation.
   *
   * <p>Usage example:
   *
   * <pre>
   * // For a file named "MyAnnotation.java"
   * Optional<TSNode> publicAnnotation = service.getPublicAnnotation(tsFile);
   * if (publicAnnotation.isPresent()) {
   *   String annotationDecl = tsFile.getTextFromNode(publicAnnotation.get());
   *   // annotationDecl = "public @interface MyAnnotation ..."
   * }
   * </pre>
   *
   * @param tsFile The {@link TSFile} to analyze.
   * @return An {@link Optional} containing the public annotation declaration node, or empty if not
   *     found, file/tree is null, or file name is missing.
   */
  public Optional<TSNode> getPublicAnnotation(TSFile tsFile) {
    if (tsFile == null || tsFile.getTree() == null) {
      return Optional.empty();
    }
    Optional<String> fileName = tsFile.getFileNameWithoutExtension();
    if (fileName.isEmpty()) {
      return this.getFirstPublicAnnotation(tsFile);
    }
    return this.findAnnotationByName(tsFile, fileName.get());
  }

  /**
   * Gets the name identifier node from an annotation declaration.
   *
   * <p>Usage example:
   *
   * <pre>
   * Optional<TSNode> nameNode = service.getAnnotationNameNode(tsFile, annotationNode);
   * if (nameNode.isPresent()) {
   *   String annotationName = tsFile.getTextFromNode(nameNode.get());
   *   // annotationName = "MyAnnotation"
   * }
   * </pre>
   *
   * @param tsFile The {@link TSFile} containing the annotation declaration
   * @param annotationDeclarationNode The annotation declaration node to analyze
   * @return Optional containing the annotation name identifier node if found, empty otherwise
   */
  public Optional<TSNode> getAnnotationNameNode(TSFile tsFile, TSNode annotationDeclarationNode) {
    if (tsFile == null
        || tsFile.getTree() == null
        || !annotationDeclarationNode.getType().equals("annotation_type_declaration")) {
      return Optional.empty();
    }
    String queryString =
        """
        (annotation_type_declaration
          name: (identifier) @annotationName
        ) @annotationDeclaration
        """;
    List<Map<String, TSNode>> results =
        tsFile
            .query(queryString)
            .within(annotationDeclarationNode)
            .returningAllCaptures()
            .execute()
            .captures();
    for (Map<String, TSNode> result : results) {
      TSNode nameNode = result.get("annotationName");
      if (nameNode != null) {
        return Optional.of(nameNode);
      }
    }
    return Optional.empty();
  }

  /**
   * Gets the first public annotation declaration found in the file. This is useful when the file
   * path is not available but you need to find the main annotation.
   *
   * @param tsFile The {@link TSFile} to analyze
   * @return Optional containing the first public annotation declaration node, empty if none found
   */
  private Optional<TSNode> getFirstPublicAnnotation(TSFile tsFile) {
    String queryString =
        """
        (annotation_type_declaration
          (modifiers) @modifiers
          name: (identifier) @annotationName
        ) @annotationDeclaration
        """;
    List<Map<String, TSNode>> results =
        tsFile.query(queryString).returningAllCaptures().execute().captures();
    for (Map<String, TSNode> result : results) {
      TSNode modifiers = result.get("modifiers");
      if (modifiers != null) {
        String modifierText = tsFile.getTextFromNode(modifiers);
        if (modifierText.contains("public")) {
          return Optional.of(result.get("annotationDeclaration"));
        }
      }
    }
    return Optional.empty();
  }
}