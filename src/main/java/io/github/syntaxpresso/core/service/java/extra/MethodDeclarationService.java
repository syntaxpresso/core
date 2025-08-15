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
  // TODO: Create ClassDeclarationService.
  // TODO: Create methods to find methoddeclaration by name and to rename.

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

  /**
   * Renames all formal parameters of the specified type across all methods in a file.
   *
   * @param file The file containing the source code.
   * @param currentName The current name of the parameter type (PascalCase).
   * @param newName The new name for the parameter type (PascalCase).
   */
  public void renameFormalParameters(TSFile file, String currentName, String newName) {
    this.formalParameterService.renameFormalParametersInFile(file, currentName, newName);
  }

  /**
   * Renames all local variables of the specified type across all methods in a file.
   *
   * @param file The file containing the source code.
   * @param currentName The current name of the variable type (PascalCase).
   * @param newName The new name for the variable type (PascalCase).
   */
  public void renameLocalVariables(TSFile file, String currentName, String newName) {
    this.localVariableDeclarationService.renameLocalVariablesInFile(file, currentName, newName);
  }
}
