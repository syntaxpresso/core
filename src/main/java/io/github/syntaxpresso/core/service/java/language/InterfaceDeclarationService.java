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
public class InterfaceDeclarationService {

  /**
   * Finds an interface declaration node by its name.
   *
   * <p>Usage example:
   *
   * <pre>
   * Optional<TSNode> interfaceNode = service.findInterfaceByName(tsFile, "MyInterface");
   * if (interfaceNode.isPresent()) {
   *   String interfaceDecl = tsFile.getTextFromNode(interfaceNode.get());
   *   // interfaceDecl = "public interface MyInterface extends BaseInterface { void method(); }"
   * }
   * </pre>
   *
   * @param tsFile {@link TSFile} containing the source code
   * @param interfaceName Name of the interface to search for
   * @return {@link Optional} containing the interface declaration node, or empty if not found,
   *     file/tree is null, or interfaceName is empty
   */
  public Optional<TSNode> findInterfaceByName(TSFile tsFile, String interfaceName) {
    if (tsFile == null || tsFile.getTree() == null || Strings.isNullOrEmpty(interfaceName)) {
      return Optional.empty();
    }
    String queryString =
        String.format(
            """
            (interface_declaration
              name: (identifier) @interfaceName
            (#eq? @interfaceName "%s")) @interfaceDeclaration
            """,
            interfaceName);
    return tsFile
        .query(queryString)
        .returning("interfaceDeclaration")
        .execute()
        .firstNodeOptional();
  }

  /**
   * Gets the public interface of a file, defined as the public interface whose name matches the
   * file name. If the file name is not available, falls back to finding the first public interface.
   *
   * <p>Usage example:
   *
   * <pre>
   * // For a file named "MyInterface.java"
   * Optional<TSNode> publicInterface = service.getPublicInterface(tsFile);
   * if (publicInterface.isPresent()) {
   *   String interfaceDecl = tsFile.getTextFromNode(publicInterface.get());
   *   // interfaceDecl = "public interface MyInterface ..."
   * }
   * </pre>
   *
   * @param tsFile The {@link TSFile} to analyze.
   * @return An {@link Optional} containing the public interface declaration node, or empty if not
   *     found, file/tree is null, or file name is missing.
   */
  public Optional<TSNode> getPublicInterface(TSFile tsFile) {
    if (tsFile == null || tsFile.getTree() == null) {
      return Optional.empty();
    }
    Optional<String> fileName = tsFile.getFileNameWithoutExtension();
    if (fileName.isEmpty()) {
      return this.getFirstPublicInterface(tsFile);
    }
    return this.findInterfaceByName(tsFile, fileName.get());
  }

  /**
   * Gets the name identifier node from an interface declaration.
   *
   * <p>Usage example:
   *
   * <pre>
   * Optional<TSNode> nameNode = service.getInterfaceNameNode(tsFile, interfaceNode);
   * if (nameNode.isPresent()) {
   *   String interfaceName = tsFile.getTextFromNode(nameNode.get());
   *   // interfaceName = "MyInterface"
   * }
   * </pre>
   *
   * @param tsFile The {@link TSFile} containing the interface declaration
   * @param interfaceDeclarationNode The interface declaration node to analyze
   * @return Optional containing the interface name identifier node if found, empty otherwise
   */
  public Optional<TSNode> getInterfaceNameNode(TSFile tsFile, TSNode interfaceDeclarationNode) {
    if (tsFile == null
        || tsFile.getTree() == null
        || !interfaceDeclarationNode.getType().equals("interface_declaration")) {
      return Optional.empty();
    }
    String queryString =
        """
        (interface_declaration
          name: (identifier) @interfaceName
        ) @interfaceDeclaration
        """;
    List<Map<String, TSNode>> results =
        tsFile
            .query(queryString)
            .within(interfaceDeclarationNode)
            .returningAllCaptures()
            .execute()
            .captures();
    for (Map<String, TSNode> result : results) {
      TSNode nameNode = result.get("interfaceName");
      if (nameNode != null) {
        return Optional.of(nameNode);
      }
    }
    return Optional.empty();
  }

  /**
   * Gets the first public interface declaration found in the file. This is useful when the file
   * path is not available but you need to find the main interface.
   *
   * @param tsFile The {@link TSFile} to analyze
   * @return Optional containing the first public interface declaration node, empty if none found
   */
  private Optional<TSNode> getFirstPublicInterface(TSFile tsFile) {
    String queryString =
        """
        (interface_declaration
          (modifiers) @modifiers
          name: (identifier) @interfaceName
        ) @interfaceDeclaration
        """;
    List<Map<String, TSNode>> results =
        tsFile.query(queryString).returningAllCaptures().execute().captures();
    for (Map<String, TSNode> result : results) {
      TSNode modifiers = result.get("modifiers");
      if (modifiers != null) {
        String modifierText = tsFile.getTextFromNode(modifiers);
        if (modifierText.contains("public")) {
          return Optional.of(result.get("interfaceDeclaration"));
        }
      }
    }
    return Optional.empty();
  }
}

