package io.github.syntaxpresso.core.command.java;

import com.google.common.base.Strings;
import io.github.syntaxpresso.core.common.DataTransferObject;
import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import io.github.syntaxpresso.core.service.extra.JavaIdentifierType;
import io.github.syntaxpresso.core.service.java.JavaService;
import io.github.syntaxpresso.core.util.StringHelper;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import lombok.RequiredArgsConstructor;
import org.treesitter.TSNode;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@RequiredArgsConstructor
@Command(name = "rename", description = "Rename a symbol and all its usages.")
public class RenameCommand implements Callable<DataTransferObject<Void>> {
  private final JavaService javaService;

  @Option(names = "--cwd", description = "Current Working Directory", required = true)
  private Path cwd;

  @Option(names = "--file-path", description = "The path to the file", required = true)
  private Path filePath;

  // @Option(names = "--line", description = "The line number of the symbol", required = true)
  // private int line;
  //
  // @Option(names = "--column", description = "The column number of the symbol", required = true)
  // private int column;
  //
  // @Option(names = "--new-name", description = "The new name for the symbol", required = true)
  // private String newName;

  private List<TSFile> getAllJavaFilesFromCwd(Path cwd) {
    List<TSFile> allFiles = new ArrayList<>();
    try {
      allFiles =
          this.javaService.getPathHelper().findFilesByExtention(this.cwd, SupportedLanguage.JAVA);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return allFiles;
  }

  private boolean isClassImported(TSFile file, String className, String packageName) {
    Optional<String> currentFilePackageName = this.javaService.getPackageName(file);
    if (currentFilePackageName.isPresent()) {
      if (packageName.equals(currentFilePackageName.get())) {
        return true;
      }
    }
    List<TSNode> allImportNodes = this.javaService.findAllImportDeclarations(file);
    for (TSNode importNode : allImportNodes) {
      Optional<TSNode> scopedIdentifier =
          this.javaService.getImportDeclarationService().getImportScopedIdentifier(importNode);
      if (scopedIdentifier.isPresent()) {
        String importScope = file.getTextFromNode(scopedIdentifier.get());
        if (importScope.contains(packageName)) {
          return true;
        }
      }
    }
    return false;
  }

  private void renameFieldDeclaration(
      TSFile file, TSNode declarationNode, String currentName, String newName) {
    Optional<TSNode> fieldInstantiationNode =
        this.javaService
            .getFieldDeclarationService()
            .getFieldInstanceNode(declarationNode, file, currentName);
    Optional<TSNode> fieldNameNode =
        this.javaService.getFieldDeclarationService().getFieldNameNode(declarationNode, file);
    Optional<TSNode> fieldTypeNode =
        this.javaService
            .getFieldDeclarationService()
            .getFieldTypeNode(declarationNode, file, currentName);
    if (fieldInstantiationNode.isPresent()) {
      file.updateSourceCode(fieldInstantiationNode.get(), newName);
    }
    if (fieldNameNode.isPresent()) {
      String newVariableName = StringHelper.pascalToCamel(newName);
      file.updateSourceCode(fieldNameNode.get(), newVariableName);
    }
    if (fieldTypeNode.isPresent()) {
      file.updateSourceCode(fieldTypeNode.get(), newName);
    }
  }

  private void renameLocalVariableDeclaration(
      TSFile file, TSNode declarationNode, String currentName, String newName) {
    // Rename in reverse order for the same reason.
    Optional<TSNode> classDeclarationInstantiationNode =
        this.javaService
            .getLocalVariableDeclarationService()
            .getVariableInstanceNode(declarationNode, file, currentName);
    Optional<TSNode> classDeclarationVariableNode =
        this.javaService
            .getLocalVariableDeclarationService()
            .getVariableNameNode(declarationNode, file);
    Optional<TSNode> classDeclarationTypeNode =
        this.javaService
            .getLocalVariableDeclarationService()
            .getVariableTypeNode(declarationNode, file, currentName);
    if (classDeclarationInstantiationNode.isPresent()) {
      file.updateSourceCode(classDeclarationInstantiationNode.get(), newName);
    }
    if (classDeclarationInstantiationNode.isPresent()) {
      String newVariableName = StringHelper.pascalToCamel(newName);
      file.updateSourceCode(classDeclarationVariableNode.get(), newVariableName);
    }
    if (classDeclarationTypeNode.isPresent()) {
      file.updateSourceCode(classDeclarationTypeNode.get(), newName);
    }
  }

  private List<TSFile> processClassRename(
      Path cwd, TSFile file, TSNode node, String packageName, String currentName, String newName) {
    List<TSFile> modifiedFiles = new ArrayList<>();
    Optional<String> packageScopeName = this.javaService.getPackageName(file);
    if (packageScopeName.isEmpty()) {
      return modifiedFiles;
    }
    file.updateSourceCode(node, "NewName");
    file.rename("NewName");
    List<TSFile> allJavaFiles = this.getAllJavaFilesFromCwd(cwd);
    for (TSFile foundFile : allJavaFiles) {
      // Skip original file.
      if (foundFile.getFile().getAbsolutePath().equals(this.filePath.toAbsolutePath().toString())) {
        continue;
      }
      boolean isImported = this.isClassImported(file, currentName, packageName);
      if (!isImported) {
        continue;
      }
      List<TSNode> usages = this.javaService.findClassUsagesInFile(foundFile, currentName);
      // Rename in reverse order to prevent bytes offset changes.
      usages.sort(Comparator.comparingInt(TSNode::getStartByte).reversed());
      for (TSNode usage : usages) {
        System.out.println(
            foundFile.getFile().getAbsolutePath() + " " + usage.getParent().toString());
        String usageName = foundFile.getTextFromRange(usage.getStartByte(), usage.getEndByte());
        // Skipe usage if doesn't have the same name as the previousName.
        if (!usageName.equals(currentName)) {
          continue;
        }
        if (usage.getParent().getType().contains("local_variable_declaration")) {
          this.renameLocalVariableDeclaration(foundFile, usage.getParent(), currentName, newName);
          modifiedFiles.add(foundFile);
        }
        if (usage.getParent().getType().contains("field_declaration")) {
          this.renameFieldDeclaration(foundFile, usage.getParent(), currentName, newName);
          modifiedFiles.add(foundFile);
        }
      }
    }
    return modifiedFiles;
  }

  @Override
  public DataTransferObject<Void> call() {
    TSFile file = new TSFile(SupportedLanguage.JAVA, this.filePath);
    TSNode node = file.getNodeFromPosition(7, 14);
    String currentName = file.getTextFromRange(node.getStartByte(), node.getEndByte());
    if (Strings.isNullOrEmpty(currentName)) {
      return null;
    }
    Optional<String> packageName = this.javaService.getPackageName(file);
    if (packageName.isEmpty()) {
      return null;
    }
    JavaIdentifierType identifierType = this.javaService.getIdentifierType(node);
    if (identifierType.equals(JavaIdentifierType.CLASS_NAME)) {
      this.processClassRename(this.cwd, file, node, packageName.get(), currentName, "NewName");
    }
    return null;
  }
}
