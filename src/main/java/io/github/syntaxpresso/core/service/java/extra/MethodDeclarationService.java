package io.github.syntaxpresso.core.service.java.extra;

import io.github.syntaxpresso.core.common.TSFile;
import java.util.Collections;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.treesitter.TSNode;

@RequiredArgsConstructor
@Getter
public class MethodDeclarationService {

  private static final String METHOD_DECLARATION_QUERY = "(method_declaration) @method";

  private final FormalParameterService formalParameterService;
  private final LocalVariableDeclarationService localVariableDeclarationService;

  /**
   * Finds all method declaration nodes in the given TSFile.
   *
   * @param file The TSFile containing the source code.
   * @return A list of all method declaration nodes.
   */
  public List<TSNode> findAllMethodDeclarations(TSFile file) {
    if (file == null || file.getTree() == null) {
      return Collections.emptyList();
    }
    return file.query(METHOD_DECLARATION_QUERY);
  }
}
