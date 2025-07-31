package io.github.syntaxpresso.core.service.java;

import io.github.syntaxpresso.core.command.java.extra.SourceDirectoryType;
import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.service.extra.JavaIdentifierType;
import io.github.syntaxpresso.core.service.extra.ScopeType;
import io.github.syntaxpresso.core.service.java.extra.FieldDeclarationService;
import io.github.syntaxpresso.core.service.java.extra.ImportDeclarationService;
import io.github.syntaxpresso.core.service.java.extra.LocalVariableDeclarationService;
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
  private final LocalVariableDeclarationService localVariableDeclarationService =
      new LocalVariableDeclarationService();
  private final FieldDeclarationService fieldDeclarationService = new FieldDeclarationService();
  private final ImportDeclarationService importDeclarationService = new ImportDeclarationService();

  public boolean isJavaProject(File rootDir) {
    if (rootDir == null || !rootDir.isDirectory()) {
      return false;
    }
    return Files.exists(rootDir.toPath().resolve("build.gradle"))
        || Files.exists(rootDir.toPath().resolve("build.gradle.kts"))
        || Files.exists(rootDir.toPath().resolve("pom.xml"))
        || Files.isDirectory(rootDir.toPath().resolve("src/main/java"));
  }

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

  public Optional<String> getPackageName(TSFile file) {
    String packageQuery = "(package_declaration (scoped_identifier) @package_name)";
    TSQuery query = new TSQuery(file.getParser().getLanguage(), packageQuery);
    TSQueryCursor cursor = new TSQueryCursor();
    cursor.exec(query, file.getTree().getRootNode());
    TSQueryMatch match = new TSQueryMatch();
    if (cursor.nextMatch(match)) {
      for (TSQueryCapture capture : match.getCaptures()) {
        TSNode node = capture.getNode();
        String packageName = file.getTextFromRange(node.getStartByte(), node.getEndByte());
        return Optional.of(packageName);
      }
    }
    return Optional.empty();
  }

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

  public JavaIdentifierType getIdentifierType(TSFile file, int line, int column) {
    TSNode node = file.getNodeFromPosition(line, column);
    return this.getIdentifierType(node);
  }

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

  public List<TSNode> findAllImportDeclarations(TSFile file) {
    List<TSNode> importNodes = new ArrayList<>();
    String importQuery = "(import_declaration) @import";
    TSQuery query = new TSQuery(file.getParser().getLanguage(), importQuery);
    TSQueryCursor cursor = new TSQueryCursor();
    cursor.exec(query, file.getTree().getRootNode());
    TSQueryMatch match = new TSQueryMatch();
    while (cursor.nextMatch(match)) {
      for (TSQueryCapture capture : match.getCaptures()) {
        importNodes.add(capture.getNode());
      }
    }
    return importNodes;
  }
}
