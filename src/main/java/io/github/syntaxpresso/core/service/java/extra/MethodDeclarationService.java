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

  /**
   * Checks if a method declaration is a main method.
   *
   * @param file The TSFile containing the source code.
   * @param methodNode The method declaration node to check.
   * @return True if the method is a main method, false otherwise.
   */
  public boolean isMainMethod(TSFile file, TSNode methodNode) {
    if (file == null || methodNode == null || !"method_declaration".equals(methodNode.getType())) {
      return false;
    }
    // Get the full method text and check if it matches main method patterns
    String methodText = file.getTextFromNode(methodNode);
    
    // Check if it's a main method by looking for the common patterns:
    // 1. public static void main(String[] args)
    // 2. public static void main(String... args)  
    // Allow for different parameter names and spacing
    return methodText.matches("(?s).*public\\s+static\\s+void\\s+main\\s*\\(\\s*String\\s*\\[\\s*\\]\\s+\\w+\\s*\\).*") ||
           methodText.matches("(?s).*public\\s+static\\s+void\\s+main\\s*\\(\\s*String\\s*\\.\\s*\\.\\s*\\.\\s+\\w+\\s*\\).*");
  }
}
