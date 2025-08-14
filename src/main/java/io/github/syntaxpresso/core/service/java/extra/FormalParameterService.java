package io.github.syntaxpresso.core.service.java.extra;

import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.util.StringHelper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.treesitter.TSNode;
import org.treesitter.TSQuery;
import org.treesitter.TSQueryCursor;
import org.treesitter.TSQueryMatch;

@RequiredArgsConstructor
public class FormalParameterService {

  private final LocalVariableDeclarationService localVariableDeclarationService;

  private static final String FORMAL_PARAMETERS_QUERY = "(formal_parameters) @params";

  @Data
  public static class ParameterRenameContext {
    private final TSNode formalParameterNode;
    private final TSNode methodDeclarationNode;
    private final TSNode paramNameNode;
    private final TSNode paramTypeNode;
    private final String currentParamName;
    private final String currentParamType;
    private final String newTypeName;
    private final String newVariableName;
    private final boolean shouldRenameVariable;
  }

  @Data
  public static class RenameResult {
    private final boolean success;
    private final List<TSNode> renamedNodes;
  }

  /**
   * Finds all formal parameters of a method.
   *
   * @param methodNode The TSNode of the method declaration.
   * @param tsFile The TSFile containing the source code.
   * @return A list of all formal parameter nodes.
   */
  public List<TSNode> findAllFormalParameters(TSNode methodNode, TSFile tsFile) {
    if (methodNode == null || !"method_declaration".equals(methodNode.getType())) {
      return Collections.emptyList();
    }
    TSQuery query = new TSQuery(tsFile.getParser().getLanguage(), FORMAL_PARAMETERS_QUERY);
    TSQueryCursor cursor = new TSQueryCursor();
    cursor.exec(query, methodNode);
    TSQueryMatch match = new TSQueryMatch();
    if (cursor.nextMatch(match)) {
      return IntStream.range(0, match.getCaptures()[0].getNode().getNamedChildCount())
          .mapToObj(i -> match.getCaptures()[0].getNode().getNamedChild(i))
          .filter(node -> "formal_parameter".equals(node.getType()))
          .collect(Collectors.toList());
    }
    return Collections.emptyList();
  }

  /**
   * Gets the type identifier node of a formal parameter.
   *
   * @param formalParameterNode The TSNode of the formal parameter.
   * @return An Optional containing the type identifier node, or empty if not found.
   */
  public Optional<TSNode> getFormalParamTypeIdentifierNode(TSNode formalParameterNode) {
    if (formalParameterNode == null || !"formal_parameter".equals(formalParameterNode.getType())) {
      return Optional.empty();
    }
    return Optional.ofNullable(formalParameterNode.getChildByFieldName("type"));
  }

  /**
   * Gets the name identifier node of a formal parameter.
   *
   * @param formalParameterNode The TSNode of the formal parameter.
   * @return An Optional containing the name identifier node, or empty if not found.
   */
  public Optional<TSNode> getFormalParamNameIdentifierNode(TSNode formalParameterNode) {
    if (formalParameterNode == null || !"formal_parameter".equals(formalParameterNode.getType())) {
      return Optional.empty();
    }
    return Optional.ofNullable(formalParameterNode.getChildByFieldName("name"));
  }

  /**
   * Validates and creates a context for parameter renaming.
   */
  private Optional<ParameterRenameContext> createRenameContext(
      TSFile file, TSNode formalParameterNode, String currentName, String newName, boolean force) {
    // Validation
    if (formalParameterNode == null || formalParameterNode.isNull() 
        || !"formal_parameter".equals(formalParameterNode.getType())) {
      return Optional.empty();
    }

    Optional<TSNode> methodDeclarationNode = file.findParentNodeByType(formalParameterNode, "method_declaration");
    if (methodDeclarationNode.isEmpty()) {
      return Optional.empty();
    }

    Optional<TSNode> paramNameNode = getFormalParamNameIdentifierNode(formalParameterNode);
    Optional<TSNode> paramTypeNode = getFormalParamTypeIdentifierNode(formalParameterNode);
    if (paramNameNode.isEmpty() || paramTypeNode.isEmpty()) {
      return Optional.empty();
    }

    String currentParamName = file.getTextFromNode(paramNameNode.get());
    String currentParamType = file.getTextFromNode(paramTypeNode.get());
    
    // Check if the parameter type matches what we're trying to rename
    if (!currentParamType.equals(currentName)) {
      return Optional.empty();
    }

    String expectedVariableName = StringHelper.pascalToCamel(currentName);
    boolean shouldRenameVariable = currentParamName.equals(expectedVariableName) || force;

    return Optional.of(new ParameterRenameContext(
        formalParameterNode,
        methodDeclarationNode.get(),
        paramNameNode.get(),
        paramTypeNode.get(),
        currentParamName,
        currentParamType,
        newName,
        StringHelper.pascalToCamel(newName),
        shouldRenameVariable
    ));
  }

  /**
   * Checks if an identifier node represents a parameter usage (not a local variable or field).
   */
  private boolean isParameterUsage(
      TSNode identifierNode, TSNode methodDeclarationNode, String paramName, TSFile file) {
    if (identifierNode == null || identifierNode.isNull()) {
      return false;
    }
    TSNode parent = identifierNode.getParent();
    // Exclude field access with 'this'
    if (parent != null && !parent.isNull() && "field_access".equals(parent.getType())) {
      TSNode objectNode = parent.getChildByFieldName("object");
      if (objectNode != null
          && !objectNode.isNull()
          && "this".equals(file.getTextFromNode(objectNode))) {
        return false; // this.paramName is a field access, not parameter
      }
    }
    // Exclude local variable declarations
    if (localVariableDeclarationService.isLocalVariableDeclaration(identifierNode)) {
      return false;
    }
    // Exclude if it's a local variable usage
    if (localVariableDeclarationService.isLocalVariableUsage(identifierNode, methodDeclarationNode, paramName, file)) {
      return false;
    }
    return true; // It's a parameter usage
  }

  /**
   * Checks if a node is within the bounds of another node.
   *
   * @param node The node to check.
   * @param container The container node.
   * @return True if node is within container's bounds.
   */
  private boolean isNodeWithin(TSNode node, TSNode container) {
    if (node == null || node.isNull() || container == null || container.isNull()) {
      return false;
    }
    return node.getStartByte() >= container.getStartByte()
        && node.getEndByte() <= container.getEndByte();
  }

  /**
   * Collects all nodes that need to be renamed for a parameter.
   */
  private List<TSNode> collectNodesToRename(ParameterRenameContext context, TSFile file) {
    List<TSNode> nodesToRename = new ArrayList<>();
    
    // Always rename the parameter type
    nodesToRename.add(context.getParamTypeNode());
    
    // Only rename parameter name and usages if appropriate
    if (context.isShouldRenameVariable()) {
      nodesToRename.add(context.getParamNameNode());
      
      // Find and add parameter usages in the method body
      List<TSNode> usages = findParameterUsagesInMethodBody(
          context.getMethodDeclarationNode(), 
          context.getCurrentParamName(), 
          file
      );
      nodesToRename.addAll(usages);
    }
    
    // Remove duplicates while preserving order and sort by byte position (reverse)
    return nodesToRename.stream()
        .collect(Collectors.toCollection(LinkedHashSet::new))
        .stream()
        .sorted(Comparator.comparingInt(TSNode::getStartByte).reversed())
        .collect(Collectors.toList());
  }

  /**
   * Finds all parameter usages within a method body.
   */
  private List<TSNode> findParameterUsagesInMethodBody(
      TSNode methodDeclarationNode, String paramName, TSFile file) {
    if (methodDeclarationNode == null || methodDeclarationNode.isNull()) {
      return Collections.emptyList();
    }
    // Get the method body (block node)
    TSNode bodyNode = methodDeclarationNode.getChildByFieldName("body");
    if (bodyNode == null || bodyNode.isNull() || !"block".equals(bodyNode.getType())) {
      return Collections.emptyList();
    }
    List<TSNode> identifierNodes = file.query(bodyNode, "(identifier) @id");
    return identifierNodes.stream()
        .filter(idNode -> idNode != null && !idNode.isNull())
        .filter(idNode -> isNodeWithin(idNode, bodyNode))
        .filter(idNode -> paramName.equals(file.getTextFromNode(idNode)))
        .filter(idNode -> isParameterUsage(idNode, methodDeclarationNode, paramName, file))
        .collect(Collectors.toList());
  }

  /**
   * Performs the actual renaming of nodes.
   */
  private RenameResult performRename(ParameterRenameContext context, List<TSNode> nodesToRename, TSFile file) {
    List<TSNode> renamedNodes = new ArrayList<>();
    
    for (TSNode node : nodesToRename) {
      if (node.equals(context.getParamTypeNode())) {
        // Type node gets PascalCase
        file.updateSourceCode(node, context.getNewTypeName());
      } else {
        // Variable name and usages get camelCase
        file.updateSourceCode(node, context.getNewVariableName());
      }
      renamedNodes.add(node);
    }
    
    return new RenameResult(!renamedNodes.isEmpty(), renamedNodes);
  }

  /**
   * Renames a method parameter and all its usages within the method body. This method ONLY renames:
   * 1. The parameter type in the method signature 
   * 2. The parameter name in the method signature (if it matches naming convention or force is true) 
   * 3. Parameter usages within the method body 
   * It does NOT rename field declarations or field accesses.
   *
   * @param file The file containing the source code.
   * @param formalParameterNode The TSNode of the formal parameter.
   * @param currentName The current name of the parameter type (PascalCase).
   * @param newName The new name for the parameter type (PascalCase).
   * @param force If true, renames variable even if it doesn't follow type naming convention.
   * @return RenameResult containing success status and list of renamed nodes.
   */
  public RenameResult renameMethodParamWithResult(
      TSFile file, TSNode formalParameterNode, String currentName, String newName, boolean force) {
    
    Optional<ParameterRenameContext> contextOpt = createRenameContext(file, formalParameterNode, currentName, newName, force);
    if (contextOpt.isEmpty()) {
      return new RenameResult(false, Collections.emptyList());
    }
    
    ParameterRenameContext context = contextOpt.get();
    List<TSNode> nodesToRename = collectNodesToRename(context, file);
    
    return performRename(context, nodesToRename, file);
  }

  /**
   * Renames a method parameter and all its usages within the method body with default behavior.
   *
   * @param file The file containing the source code.
   * @param formalParameterNode The TSNode of the formal parameter.
   * @param currentName The current name of the parameter type (PascalCase).
   * @param newName The new name for the parameter type (PascalCase).
   * @return RenameResult containing success status and list of renamed nodes.
   */
  public RenameResult renameMethodParamWithResult(
      TSFile file, TSNode formalParameterNode, String currentName, String newName) {
    return renameMethodParamWithResult(file, formalParameterNode, currentName, newName, false);
  }

  /**
   * Legacy method for backward compatibility. 
   * @deprecated Use renameMethodParamWithResult for better error handling and result information.
   */
  @Deprecated
  public boolean renameMethodParam(
      TSFile file, TSNode formalParameterNode, String currentName, String newName, boolean force,
      LocalVariableDeclarationService localVariableService) {
    return renameMethodParamWithResult(file, formalParameterNode, currentName, newName, force).isSuccess();
  }

  /**
   * Legacy method for backward compatibility.
   * @deprecated Use renameMethodParamWithResult for better error handling and result information.
   */
  @Deprecated
  public boolean renameMethodParam(
      TSFile file, TSNode formalParameterNode, String currentName, String newName,
      LocalVariableDeclarationService localVariableService) {
    return renameMethodParamWithResult(file, formalParameterNode, currentName, newName, false).isSuccess();
  }
}
