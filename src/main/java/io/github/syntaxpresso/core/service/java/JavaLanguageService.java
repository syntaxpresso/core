package io.github.syntaxpresso.core.service.java;

import io.github.syntaxpresso.core.command.extra.JavaSourceDirectoryType;
import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.common.extra.SupportedIDE;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import io.github.syntaxpresso.core.service.extra.JavaIdentifierType;
import io.github.syntaxpresso.core.service.java.language.AnnotationDeclarationService;
import io.github.syntaxpresso.core.service.java.language.AnnotationService;
import io.github.syntaxpresso.core.service.java.language.ClassDeclarationService;
import io.github.syntaxpresso.core.service.java.language.EnumDeclarationService;
import io.github.syntaxpresso.core.service.java.language.ImportDeclarationService;
import io.github.syntaxpresso.core.service.java.language.InterfaceDeclarationService;
import io.github.syntaxpresso.core.service.java.language.LocalVariableDeclarationService;
import io.github.syntaxpresso.core.service.java.language.PackageDeclarationService;
import io.github.syntaxpresso.core.service.java.language.RecordDeclarationService;
import io.github.syntaxpresso.core.service.java.language.VariableNamingService;
import io.github.syntaxpresso.core.util.PathHelper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.treesitter.TSNode;

@Getter
@RequiredArgsConstructor
public class JavaLanguageService {
  private final PathHelper pathHelper;
  private final VariableNamingService variableNamingService;
  private final ClassDeclarationService classDeclarationService;
  private final InterfaceDeclarationService interfaceDeclarationService;
  private final EnumDeclarationService enumDeclarationService;
  private final RecordDeclarationService recordDeclarationService;
  private final AnnotationDeclarationService annotationDeclarationService;
  private final PackageDeclarationService packageDeclarationService;
  private final ImportDeclarationService importDeclarationService;
  private final LocalVariableDeclarationService localVariableDeclarationService;
  private final AnnotationService annotationService;

  /**
   * Gets all Java files from the current working directory based on source directory type.
   *
   * @param cwd The current working directory.
   * @param sourceDirectoryType The type of source directory to search in.
   * @return A list of all Java files.
   */
  public List<TSFile> getAllJavaFilesFromCwd(
      Path cwd, JavaSourceDirectoryType sourceDirectoryType) {
    try {
      if (cwd == null) {
        return List.of();
      }
      switch (sourceDirectoryType) {
        case MAIN:
          Optional<Path> mainDir = this.pathHelper.findDirectoryRecursively(cwd, "src/main");
          Path mainSearchPath = mainDir.orElse(cwd);
          return this.pathHelper.findFilesByExtention(mainSearchPath, SupportedLanguage.JAVA);
        case TEST:
          Optional<Path> testDir = this.pathHelper.findDirectoryRecursively(cwd, "src/test");
          Path testSearchPath = testDir.orElse(cwd);
          return this.pathHelper.findFilesByExtention(testSearchPath, SupportedLanguage.JAVA);
        case ALL:
        default:
          return this.pathHelper.findFilesByExtention(cwd, SupportedLanguage.JAVA);
      }
    } catch (Exception e) {
      return List.of();
    }
  }

  public List<TSFile> getAllJavaFilesFromCwd(Path cwd) {
    return this.getAllJavaFilesFromCwd(cwd, JavaSourceDirectoryType.ALL);
  }

  /**
   * Checks if a directory is a Java project.
   *
   * @param rootDir The root path of the project.
   * @return True if the directory is a Java project, false otherwise.
   */
  public boolean isJavaProject(Path rootDir) {
    if (rootDir == null || !Files.isDirectory(rootDir)) {
      return false;
    }
    return Files.exists(rootDir.resolve("build.gradle"))
        || Files.exists(rootDir.resolve("build.gradle.kts"))
        || Files.exists(rootDir.resolve("pom.xml"))
        || Files.isDirectory(rootDir.resolve("src/main/java"));
  }

  public JavaIdentifierType getIdentifierType(TSNode node, SupportedIDE ide) {
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
      case "interface_declaration":
        return JavaIdentifierType.CLASS_NAME;
      case "enum_declaration":
        return JavaIdentifierType.CLASS_NAME;
      case "record_declaration":
        return JavaIdentifierType.CLASS_NAME;
      case "annotation_type_declaration":
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
}
