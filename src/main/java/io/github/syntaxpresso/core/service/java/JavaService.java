package io.github.syntaxpresso.core.service.java;

import io.github.syntaxpresso.core.command.java.extra.SourceDirectoryType;
import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import io.github.syntaxpresso.core.service.extra.JavaIdentifierType;
import io.github.syntaxpresso.core.service.extra.ScopeType;
import io.github.syntaxpresso.core.service.java.extra.FieldDeclarationService;
import io.github.syntaxpresso.core.service.java.extra.FormalParameterService;
import io.github.syntaxpresso.core.service.java.extra.ImportDeclarationService;
import io.github.syntaxpresso.core.service.java.extra.LocalVariableDeclarationService;
import io.github.syntaxpresso.core.service.java.extra.MethodDeclarationService;
import io.github.syntaxpresso.core.service.java.extra.PackageDeclarationService;
import io.github.syntaxpresso.core.service.java.extra.VariableNamingService;
import io.github.syntaxpresso.core.util.PathHelper;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.treesitter.TSNode;
import org.treesitter.TSQuery;
import org.treesitter.TSQueryCapture;
import org.treesitter.TSQueryCursor;
import org.treesitter.TSQueryMatch;

@Data
@RequiredArgsConstructor
public class JavaService {
  private final PathHelper pathHelper;
  private final VariableNamingService variableNamingService = new VariableNamingService();
  private final FieldDeclarationService fieldDeclarationService = new FieldDeclarationService();
  private final ImportDeclarationService importDeclarationService = new ImportDeclarationService();
  private final LocalVariableDeclarationService localVariableDeclarationService =
      new LocalVariableDeclarationService(variableNamingService);
  private final FormalParameterService formalParameterService =
      new FormalParameterService(localVariableDeclarationService, variableNamingService);
  private final MethodDeclarationService methodDeclarationService =
      new MethodDeclarationService(formalParameterService, localVariableDeclarationService);
  private final PackageDeclarationService packageDeclarationService =
      new PackageDeclarationService();

  /**
   * Checks if a directory is a Java project.
   *
   * @param rootDir The root directory of the project.
   * @return True if the directory is a Java project, false otherwise.
   */
  public boolean isJavaProject(File rootDir) {
    if (rootDir == null || !rootDir.isDirectory()) {
      return false;
    }
    return Files.exists(rootDir.toPath().resolve("build.gradle"))
        || Files.exists(rootDir.toPath().resolve("build.gradle.kts"))
        || Files.exists(rootDir.toPath().resolve("pom.xml"))
        || Files.isDirectory(rootDir.toPath().resolve("src/main/java"));
  }

  /**
   * Finds the file path for a given package name.
   *
   * @param rootDir The root directory of the project.
   * @param packageName The name of the package.
   * @param sourceDirectoryType The type of source directory.
   * @return An optional containing the path to the file, or empty if not found.
   */
  public Optional<Path> findFilePath(
      Path rootDir, String packageName, SourceDirectoryType sourceDirectoryType) {
    if (rootDir == null || !Files.isDirectory(rootDir)) {
      return Optional.empty();
    }
    if (packageName == null || packageName.isBlank()) {
      return Optional.empty();
    }
    if (sourceDirectoryType == null) {
      return Optional.empty();
    }
    final String srcDirName =
        (sourceDirectoryType == SourceDirectoryType.MAIN) ? "src/main/java" : "src/test/java";
    Optional<Path> sourceDirOptional;
    try {
      sourceDirOptional = this.pathHelper.findDirectoryRecursively(rootDir, srcDirName);
    } catch (IOException e) {
      e.printStackTrace();
      return Optional.empty();
    }
    Path sourceDir;
    if (sourceDirOptional.isPresent()) {
      sourceDir = sourceDirOptional.get();
    } else {
      sourceDir = rootDir.resolve(srcDirName);
    }
    Path packageAsPath = Path.of(packageName.replace('.', '/'));
    Path fullPackageDir = sourceDir.resolve(packageAsPath);
    try {
      Files.createDirectories(fullPackageDir);
      return Optional.of(fullPackageDir);
    } catch (IOException e) {
      e.printStackTrace();
      return Optional.empty();
    }
  }

  /**
   * Checks if a file contains a main class.
   *
   * @param file The file to check.
   * @return True if the file contains a main class, false otherwise.
   */
  public Boolean isMainClass(TSFile file) {
    String mainMethodQuery =
        "(class_declaration  body: (class_body    (method_declaration       (modifiers) @mods      "
            + " type: (void_type)       name: (identifier) @name       parameters:"
            + " (formal_parameters         [          (formal_parameter type: (array_type element:"
            + " (type_identifier) @param_type))          (spread_parameter (type_identifier)"
            + " @param_type)        ]      )     )  ))";
    TSQuery query = new TSQuery(file.getParser().getLanguage(), mainMethodQuery);
    TSQueryCursor queryCursor = new TSQueryCursor();
    queryCursor.exec(query, file.getTree().getRootNode());
    TSQueryMatch match = new TSQueryMatch();
    while (queryCursor.nextMatch(match)) {
      Map<String, TSNode> captures = new HashMap<>();
      for (TSQueryCapture capture : match.getCaptures()) {
        String captureName = query.getCaptureNameForId(capture.getIndex());
        captures.put(captureName, capture.getNode());
      }
      TSNode nameNode = captures.get("name");
      TSNode modsNode = captures.get("mods");
      TSNode paramTypeNode = captures.get("param_type");
      if (nameNode != null && modsNode != null && paramTypeNode != null) {
        String methodName =
            file.getSourceCode().substring(nameNode.getStartByte(), nameNode.getEndByte());
        String paramType =
            file.getSourceCode()
                .substring(paramTypeNode.getStartByte(), paramTypeNode.getEndByte());
        String methodModifiers =
            file.getSourceCode().substring(modsNode.getStartByte(), modsNode.getEndByte());
        Set<String> modifiersSet =
            new HashSet<>(Arrays.asList(methodModifiers.trim().split("\\s+")));
        boolean hasCorrectModifiers =
            modifiersSet.size() == 2
                && modifiersSet.contains("public")
                && modifiersSet.contains("static");
        if (methodName.equals("main") && paramType.equals("String") && hasCorrectModifiers) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Gets all Java files from the current working directory.
   *
   * @param cwd The current working directory.
   * @return A list of all Java files.
   */
  public List<TSFile> getAllJavaFilesFromCwd(Path cwd) {
    List<TSFile> allFiles = new ArrayList<>();
    try {
      allFiles = this.pathHelper.findFilesByExtention(cwd, SupportedLanguage.JAVA);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return allFiles;
  }

  /**
   * Gets the scope of a node.
   *
   * @param node The node to get the scope of.
   * @return An optional containing the scope type, or empty if not found.
   */
  public Optional<ScopeType> getNodeScope(TSNode node) {
    if (node == null) {
      return Optional.empty();
    }
    String nodeType = node.getType();
    if ("local_variable_declaration".equals(nodeType) || "formal_parameter".equals(nodeType)) {
      return Optional.of(ScopeType.LOCAL);
    }
    boolean isPublic = false;
    for (int i = 0; i < node.getChildCount(); i++) {
      TSNode child = node.getChild(i);
      if ("modifiers".equals(child.getType())) {
        for (int j = 0; j < child.getChildCount(); j++) {
          if ("public".equals(child.getChild(j).getType())) {
            isPublic = true;
            break;
          }
        }
      }
      if (isPublic) {
        break;
      }
    }
    if ("class_declaration".equals(nodeType)
        || "interface_declaration".equals(nodeType)
        || "enum_declaration".equals(nodeType)
        || "record_declaration".equals(nodeType)
        || "annotation_type_declaration".equals(nodeType)) {
      return isPublic ? Optional.of(ScopeType.PROJECT) : Optional.of(ScopeType.CLASS);
    }
    if ("field_declaration".equals(nodeType) || "method_declaration".equals(nodeType)) {
      return isPublic ? Optional.of(ScopeType.PROJECT) : Optional.of(ScopeType.CLASS);
    }
    return Optional.empty();
  }

  /**
   * Gets the identifier type of a node.
   *
   * @param node The node to get the identifier type of.
   * @return The identifier type, or null if not found.
   */
  public JavaIdentifierType getIdentifierType(TSNode node) {
    if (!"identifier".equals(node.getType())) {
      return null;
    }
    TSNode parent = node.getParent();
    if (parent == null) {
      return null;
    }
    String parentType = parent.getType();
    switch (parentType) {
      case "class_declaration":
        return JavaIdentifierType.CLASS_NAME;
      case "method_declaration":
        return JavaIdentifierType.METHOD_NAME;
      case "formal_parameter":
        return JavaIdentifierType.FORMAL_PARAMETER_NAME;
      case "variable_declarator":
        TSNode grandParent = parent.getParent();
        if (grandParent != null && "field_declaration".equals(grandParent.getType())) {
          return JavaIdentifierType.FIELD_NAME;
        }
        return JavaIdentifierType.LOCAL_VARIABLE_NAME;
      default:
        return null;
    }
  }

  /**
   * Gets the identifier type of a node at a given position.
   *
   * @param file The file to search in.
   * @param line The line number of the node.
   * @param column The column number of the node.
   * @return The identifier type, or null if not found.
   */
  public JavaIdentifierType getIdentifierType(TSFile file, int line, int column) {
    TSNode node = file.getNodeFromPosition(line, column);
    return this.getIdentifierType(node);
  }

  /**
   * Finds all usages of a class in a file.
   *
   * @param file The file to search in.
   * @param className The name of the class to find usages of.
   * @return A list of all nodes representing usages of the class.
   */
  public List<TSNode> findClassUsagesInFile(TSFile file, String className) {
    List<TSNode> confirmedUsages = new ArrayList<>();
    String usageQuery = "[(type_identifier) @usage (class_declaration name: (identifier) @usage)]";
    TSQuery query = new TSQuery(file.getParser().getLanguage(), usageQuery);
    TSQueryCursor cursor = new TSQueryCursor();
    cursor.exec(query, file.getTree().getRootNode());
    TSQueryMatch match = new TSQueryMatch();
    while (cursor.nextMatch(match)) {
      for (TSQueryCapture capture : match.getCaptures()) {
        TSNode potentialUsage = capture.getNode();
        String usageName =
            file.getTextFromRange(potentialUsage.getStartByte(), potentialUsage.getEndByte());
        if (usageName.equals(className)) {
          confirmedUsages.add(potentialUsage);
        }
      }
    }
    return confirmedUsages;
  }

  /**
   * Renames a method parameter usage.
   *
   * @param file The file containing the source code.
   * @param methodDeclarationNode The node of the method declaration.
   * @param currentName The current name of the parameter.
   * @param newName The new name of the parameter.
   */
  public void renameMethodParamUsage(
      TSFile file, TSNode methodDeclarationNode, String currentName, String newName) {
    if (!"method_declaration".equals(methodDeclarationNode.getType())) {
      throw new IllegalArgumentException("Node is not a method_declaration");
    }
    TSNode bodyNode = methodDeclarationNode.getChildByFieldName("body");
    if (bodyNode == null) {
      return;
    }
    List<TSNode> nodesToRename = new ArrayList<>();
    String queryStr = "(identifier) @id";
    TSQuery query = new TSQuery(file.getParser().getLanguage(), queryStr);
    TSQueryCursor cursor = new TSQueryCursor();
    cursor.exec(query, bodyNode);
    TSQueryMatch match = new TSQueryMatch();
    while (cursor.nextMatch(match)) {
      for (TSQueryCapture capture : match.getCaptures()) {
        TSNode idNode = capture.getNode();
        if (file.getTextFromNode(idNode).equals(currentName)) {
          TSNode parent = idNode.getParent();
          if (parent != null && "field_access".equals(parent.getType())) {
            TSNode objectNode = parent.getChildByFieldName("object");
            TSNode fieldNode = parent.getChildByFieldName("field");
            if (objectNode != null
                && "this".equals(objectNode.getType())
                && idNode.equals(fieldNode)) {
              continue;
            }
          }
          nodesToRename.add(idNode);
        }
      }
    }
    nodesToRename.sort((a, b) -> Integer.compare(b.getStartByte(), a.getStartByte()));
    for (TSNode node : nodesToRename) {
      file.updateSourceCode(node, newName);
    }
  }
}
