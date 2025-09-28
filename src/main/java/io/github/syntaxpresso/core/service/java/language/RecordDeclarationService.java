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
public class RecordDeclarationService {

  /**
   * Finds a record declaration node by its name.
   *
   * <p>Usage example:
   *
   * <pre>
   * Optional<TSNode> recordNode = service.findRecordByName(tsFile, "Person");
   * if (recordNode.isPresent()) {
   *   String recordDecl = tsFile.getTextFromNode(recordNode.get());
   *   // recordDecl = "public record Person(String name, int age) {}"
   * }
   * </pre>
   *
   * @param tsFile {@link TSFile} containing the source code
   * @param recordName Name of the record to search for
   * @return {@link Optional} containing the record declaration node, or empty if not found,
   *     file/tree is null, or recordName is empty
   */
  public Optional<TSNode> findRecordByName(TSFile tsFile, String recordName) {
    if (tsFile == null || tsFile.getTree() == null || Strings.isNullOrEmpty(recordName)) {
      return Optional.empty();
    }
    String queryString =
        String.format(
            """
            (record_declaration
              name: (identifier) @recordName
            (#eq? @recordName "%s")) @recordDeclaration
            """,
            recordName);
    return tsFile
        .query(queryString)
        .returning("recordDeclaration")
        .execute()
        .firstNodeOptional();
  }

  /**
   * Gets the public record of a file, defined as the public record whose name matches the
   * file name. If the file name is not available, falls back to finding the first public record.
   *
   * <p>Usage example:
   *
   * <pre>
   * // For a file named "Person.java"
   * Optional<TSNode> publicRecord = service.getPublicRecord(tsFile);
   * if (publicRecord.isPresent()) {
   *   String recordDecl = tsFile.getTextFromNode(publicRecord.get());
   *   // recordDecl = "public record Person ..."
   * }
   * </pre>
   *
   * @param tsFile The {@link TSFile} to analyze.
   * @return An {@link Optional} containing the public record declaration node, or empty if not
   *     found, file/tree is null, or file name is missing.
   */
  public Optional<TSNode> getPublicRecord(TSFile tsFile) {
    if (tsFile == null || tsFile.getTree() == null) {
      return Optional.empty();
    }
    Optional<String> fileName = tsFile.getFileNameWithoutExtension();
    if (fileName.isEmpty()) {
      return this.getFirstPublicRecord(tsFile);
    }
    return this.findRecordByName(tsFile, fileName.get());
  }

  /**
   * Gets the name identifier node from a record declaration.
   *
   * <p>Usage example:
   *
   * <pre>
   * Optional<TSNode> nameNode = service.getRecordNameNode(tsFile, recordNode);
   * if (nameNode.isPresent()) {
   *   String recordName = tsFile.getTextFromNode(nameNode.get());
   *   // recordName = "Person"
   * }
   * </pre>
   *
   * @param tsFile The {@link TSFile} containing the record declaration
   * @param recordDeclarationNode The record declaration node to analyze
   * @return Optional containing the record name identifier node if found, empty otherwise
   */
  public Optional<TSNode> getRecordNameNode(TSFile tsFile, TSNode recordDeclarationNode) {
    if (tsFile == null
        || tsFile.getTree() == null
        || !recordDeclarationNode.getType().equals("record_declaration")) {
      return Optional.empty();
    }
    String queryString =
        """
        (record_declaration
          name: (identifier) @recordName
        ) @recordDeclaration
        """;
    List<Map<String, TSNode>> results =
        tsFile
            .query(queryString)
            .within(recordDeclarationNode)
            .returningAllCaptures()
            .execute()
            .captures();
    for (Map<String, TSNode> result : results) {
      TSNode nameNode = result.get("recordName");
      if (nameNode != null) {
        return Optional.of(nameNode);
      }
    }
    return Optional.empty();
  }

  /**
   * Gets the first public record declaration found in the file. This is useful when the file
   * path is not available but you need to find the main record.
   *
   * @param tsFile The {@link TSFile} to analyze
   * @return Optional containing the first public record declaration node, empty if none found
   */
  private Optional<TSNode> getFirstPublicRecord(TSFile tsFile) {
    String queryString =
        """
        (record_declaration
          (modifiers) @modifiers
          name: (identifier) @recordName
        ) @recordDeclaration
        """;
    List<Map<String, TSNode>> results =
        tsFile.query(queryString).returningAllCaptures().execute().captures();
    for (Map<String, TSNode> result : results) {
      TSNode modifiers = result.get("modifiers");
      if (modifiers != null) {
        String modifierText = tsFile.getTextFromNode(modifiers);
        if (modifierText.contains("public")) {
          return Optional.of(result.get("recordDeclaration"));
        }
      }
    }
    return Optional.empty();
  }
}
