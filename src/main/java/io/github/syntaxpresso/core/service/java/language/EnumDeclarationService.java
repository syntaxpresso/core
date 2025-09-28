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
public class EnumDeclarationService {

  /**
   * Finds an enum declaration node by its name.
   *
   * <p>Usage example:
   *
   * <pre>
   * Optional<TSNode> enumNode = service.findEnumByName(tsFile, "Color");
   * if (enumNode.isPresent()) {
   *   String enumDecl = tsFile.getTextFromNode(enumNode.get());
   *   // enumDecl = "public enum Color { RED, GREEN, BLUE }"
   * }
   * </pre>
   *
   * @param tsFile {@link TSFile} containing the source code
   * @param enumName Name of the enum to search for
   * @return {@link Optional} containing the enum declaration node, or empty if not found,
   *     file/tree is null, or enumName is empty
   */
  public Optional<TSNode> findEnumByName(TSFile tsFile, String enumName) {
    if (tsFile == null || tsFile.getTree() == null || Strings.isNullOrEmpty(enumName)) {
      return Optional.empty();
    }
    String queryString =
        String.format(
            """
            (enum_declaration
              name: (identifier) @enumName
            (#eq? @enumName "%s")) @enumDeclaration
            """,
            enumName);
    return tsFile
        .query(queryString)
        .returning("enumDeclaration")
        .execute()
        .firstNodeOptional();
  }

  /**
   * Gets the public enum of a file, defined as the public enum whose name matches the
   * file name. If the file name is not available, falls back to finding the first public enum.
   *
   * <p>Usage example:
   *
   * <pre>
   * // For a file named "Color.java"
   * Optional<TSNode> publicEnum = service.getPublicEnum(tsFile);
   * if (publicEnum.isPresent()) {
   *   String enumDecl = tsFile.getTextFromNode(publicEnum.get());
   *   // enumDecl = "public enum Color ..."
   * }
   * </pre>
   *
   * @param tsFile The {@link TSFile} to analyze.
   * @return An {@link Optional} containing the public enum declaration node, or empty if not
   *     found, file/tree is null, or file name is missing.
   */
  public Optional<TSNode> getPublicEnum(TSFile tsFile) {
    if (tsFile == null || tsFile.getTree() == null) {
      return Optional.empty();
    }
    Optional<String> fileName = tsFile.getFileNameWithoutExtension();
    if (fileName.isEmpty()) {
      return this.getFirstPublicEnum(tsFile);
    }
    return this.findEnumByName(tsFile, fileName.get());
  }

  /**
   * Gets the name identifier node from an enum declaration.
   *
   * <p>Usage example:
   *
   * <pre>
   * Optional<TSNode> nameNode = service.getEnumNameNode(tsFile, enumNode);
   * if (nameNode.isPresent()) {
   *   String enumName = tsFile.getTextFromNode(nameNode.get());
   *   // enumName = "Color"
   * }
   * </pre>
   *
   * @param tsFile The {@link TSFile} containing the enum declaration
   * @param enumDeclarationNode The enum declaration node to analyze
   * @return Optional containing the enum name identifier node if found, empty otherwise
   */
  public Optional<TSNode> getEnumNameNode(TSFile tsFile, TSNode enumDeclarationNode) {
    if (tsFile == null
        || tsFile.getTree() == null
        || !enumDeclarationNode.getType().equals("enum_declaration")) {
      return Optional.empty();
    }
    String queryString =
        """
        (enum_declaration
          name: (identifier) @enumName
        ) @enumDeclaration
        """;
    List<Map<String, TSNode>> results =
        tsFile
            .query(queryString)
            .within(enumDeclarationNode)
            .returningAllCaptures()
            .execute()
            .captures();
    for (Map<String, TSNode> result : results) {
      TSNode nameNode = result.get("enumName");
      if (nameNode != null) {
        return Optional.of(nameNode);
      }
    }
    return Optional.empty();
  }

  /**
   * Gets the first public enum declaration found in the file. This is useful when the file
   * path is not available but you need to find the main enum.
   *
   * @param tsFile The {@link TSFile} to analyze
   * @return Optional containing the first public enum declaration node, empty if none found
   */
  private Optional<TSNode> getFirstPublicEnum(TSFile tsFile) {
    String queryString =
        """
        (enum_declaration
          (modifiers) @modifiers
          name: (identifier) @enumName
        ) @enumDeclaration
        """;
    List<Map<String, TSNode>> results =
        tsFile.query(queryString).returningAllCaptures().execute().captures();
    for (Map<String, TSNode> result : results) {
      TSNode modifiers = result.get("modifiers");
      if (modifiers != null) {
        String modifierText = tsFile.getTextFromNode(modifiers);
        if (modifierText.contains("public")) {
          return Optional.of(result.get("enumDeclaration"));
        }
      }
    }
    return Optional.empty();
  }
}

