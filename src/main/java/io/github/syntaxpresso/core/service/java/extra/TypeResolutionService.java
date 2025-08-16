package io.github.syntaxpresso.core.service.java.extra;

import io.github.syntaxpresso.core.common.TSFile;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.treesitter.TSNode;

@RequiredArgsConstructor
public class TypeResolutionService {
  private final FormalParameterService formalParameterService;
  private final LocalVariableDeclarationService localVariableDeclarationService;
  private final FieldDeclarationService fieldDeclarationService;
  private final ClassDeclarationService classDeclarationService;

  /**
   * Resolves the type of an object node by traversing up the tree to find its declaration.
   *
   * @param file The TSFile containing the source code.
   * @param objectNode The node representing the object whose type we want to resolve.
   * @param contextNode The context node where the object is used (for scope resolution).
   * @return The type name of the object, or empty string if not found.
   */
  public String resolveObjectType(TSFile file, TSNode objectNode, TSNode contextNode) {
    String objectName = file.getTextFromNode(objectNode);
    // Handle 'this' keyword
    if ("this".equals(objectName)) {
      Optional<TSNode> classNode = file.findParentNodeByType(contextNode, "class_declaration");
      if (classNode.isPresent()) {
        return this.classDeclarationService.getClassName(file, classNode.get()).orElse("");
      }
      return "";
    }
    // Find the parent method/constructor/field where this invocation occurs
    Optional<TSNode> parentMethodNode =
        file.findParentNodeByType(contextNode, "method_declaration");
    Optional<TSNode> parentConstructorNode =
        file.findParentNodeByType(contextNode, "constructor_declaration");
    Optional<TSNode> parentFieldNode = file.findParentNodeByType(contextNode, "field_declaration");
    TSNode searchScope = null;
    if (parentMethodNode.isPresent()) {
      searchScope = parentMethodNode.get();
    } else if (parentConstructorNode.isPresent()) {
      searchScope = parentConstructorNode.get();
    } else if (parentFieldNode.isPresent()) {
      searchScope = parentFieldNode.get();
    }
    if (searchScope == null) {
      return "";
    }
    // 1. Check formal parameters
    String typeFromFormalParams =
        this.resolveTypeFromFormalParameters(file, searchScope, objectName);
    if (!typeFromFormalParams.isEmpty()) {
      return typeFromFormalParams;
    }
    // 2. Check local variables (only for methods/constructors)
    if (parentMethodNode.isPresent() || parentConstructorNode.isPresent()) {
      String typeFromLocalVars = this.resolveTypeFromLocalVariables(file, searchScope, objectName);
      if (!typeFromLocalVars.isEmpty()) {
        return typeFromLocalVars;
      }
    }
    // 3. Check class fields
    Optional<TSNode> classNode = file.findParentNodeByType(searchScope, "class_declaration");
    if (classNode.isPresent()) {
      String typeFromFields = this.resolveTypeFromClassFields(file, classNode.get(), objectName);
      if (!typeFromFields.isEmpty()) {
        return typeFromFields;
      }
    }
    return "";
  }

  /**
   * Resolves object type from formal parameters.
   *
   * @param file The TSFile containing the source code.
   * @param methodOrConstructor The method or constructor node to search in.
   * @param objectName The name of the object to find.
   * @return The type name, or empty string if not found.
   */
  private String resolveTypeFromFormalParameters(
      TSFile file, TSNode methodOrConstructor, String objectName) {
    // Get all formal parameters using query
    List<TSNode> formalParameters = file.query(methodOrConstructor, "(formal_parameter) @param");
    for (TSNode param : formalParameters) {
      Optional<TSNode> paramNameNode =
          this.formalParameterService.getParameterNameNode(param, file);
      if (paramNameNode.isPresent()) {
        String paramName = file.getTextFromNode(paramNameNode.get());
        if (objectName.equals(paramName)) {
          // Get the type of this parameter
          List<TSNode> typeNodes = file.query(param, "(type_identifier) @type");
          if (!typeNodes.isEmpty()) {
            return file.getTextFromNode(typeNodes.get(0));
          }
        }
      }
    }
    return "";
  }

  /**
   * Resolves object type from local variable declarations.
   *
   * @param file The TSFile containing the source code.
   * @param methodNode The method node to search in.
   * @param objectName The name of the object to find.
   * @return The type name, or empty string if not found.
   */
  private String resolveTypeFromLocalVariables(TSFile file, TSNode methodNode, String objectName) {
    List<TSNode> localVars =
        this.localVariableDeclarationService.findAllLocalVariableDeclarations(methodNode, file);
    for (TSNode localVar : localVars) {
      Optional<TSNode> varNameNode =
          this.localVariableDeclarationService.getVariableNameNode(localVar, file);
      if (varNameNode.isPresent()) {
        String varName = file.getTextFromNode(varNameNode.get());
        if (objectName.equals(varName)) {
          // Get the type of this local variable
          List<TSNode> typeNodes = file.query(localVar, "(type_identifier) @type");
          if (!typeNodes.isEmpty()) {
            return file.getTextFromNode(typeNodes.get(0));
          }
        }
      }
    }
    return "";
  }

  /**
   * Resolves object type from class field declarations.
   *
   * @param file The TSFile containing the source code.
   * @param classNode The class node to search in.
   * @param objectName The name of the object to find.
   * @return The type name, or empty string if not found.
   */
  private String resolveTypeFromClassFields(TSFile file, TSNode classNode, String objectName) {
    // Get all field declarations using query
    List<TSNode> fields = file.query(classNode, "(field_declaration) @field");
    for (TSNode field : fields) {
      Optional<TSNode> fieldNameNode = this.fieldDeclarationService.getFieldNameNode(field, file);
      if (fieldNameNode.isPresent()) {
        String fieldName = file.getTextFromNode(fieldNameNode.get());
        if (objectName.equals(fieldName)) {
          // Get the type of this field
          List<TSNode> typeNodes = file.query(field, "(type_identifier) @type");
          if (!typeNodes.isEmpty()) {
            return file.getTextFromNode(typeNodes.get(0));
          }
        }
      }
    }
    return "";
  }
}

