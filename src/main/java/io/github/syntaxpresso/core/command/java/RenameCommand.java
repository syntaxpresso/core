package io.github.syntaxpresso.core.command.java;

import com.google.common.base.Strings;
import com.google.common.io.Files;
import io.github.syntaxpresso.core.common.DataTransferObject;
import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import io.github.syntaxpresso.core.service.extra.JavaIdentifierType;
import io.github.syntaxpresso.core.service.java.JavaService;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
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

  @Option(names = "--line", description = "The line number of the symbol", required = true)
  private int line;

  @Option(names = "--column", description = "The column number of the symbol", required = true)
  private int column;

  @Option(names = "--new-name", description = "The new name for the symbol", required = true)
  private String newName;

  private boolean shouldRenameFileName(TSFile file, String currentName) {
    // Only appliable to classes.
    String fileName = Files.getNameWithoutExtension(file.getFile().getAbsolutePath());
    if (fileName.equals(currentName)) {
      return true;
    }
    return false;
  }

  private List<TSFile> processClassRename(
      Path cwd, TSFile file, TSNode node, String packageName, String currentName, String newName) {
    List<TSFile> modifiedFiles = new ArrayList<>();
    // Update class name and file name, if necessary.
    file.updateSourceCode(node, newName);
    if (this.shouldRenameFileName(file, currentName)) {
      file.rename(newName);
    }
    if (file.isModified()) {
      modifiedFiles.add(file);
    }
    // Parse all java files, but skip the current one, as it can't instantiate itself.
    List<TSFile> allJavaFiles = this.javaService.getAllJavaFilesFromCwd(cwd);
    for (TSFile foundFile : allJavaFiles) {
      Optional<String> foundFilePackageName =
          this.javaService
              .getProgramService()
              .getPackageDeclarationService()
              .getPackageName(foundFile);
      if (foundFilePackageName.isEmpty()) {
        continue;
      }
      // Skip original file.
      if (foundFile.getFile().getAbsolutePath().equals(file.getFile().getAbsolutePath())) {
        continue;
      }
      Optional<TSNode> importNode =
          this.javaService
              .getProgramService()
              .getImportDeclarationService()
              .getImportDeclarationNode(foundFile, currentName, packageName);
      if (importNode.isEmpty() && !foundFilePackageName.get().equals(packageName)) {
        continue;
      }
      this.javaService
          .getProgramService()
          .getClassDeclarationService()
          .getMethodDeclarationService()
          .renameLocalVariables(foundFile, currentName, newName);
      this.javaService
          .getProgramService()
          .getClassDeclarationService()
          .getMethodDeclarationService()
          .renameFormalParameters(foundFile, currentName, newName);
      this.javaService
          .getProgramService()
          .getClassDeclarationService()
          .getFieldDeclarationService()
          .renameClassFields(foundFile, currentName, newName);
      if (!foundFilePackageName.get().equals(packageName)) {
        this.javaService
            .getProgramService()
            .getImportDeclarationService()
            .updateImport(foundFile, packageName + "." + currentName, packageName + "." + newName);
      }
      if (foundFile.isModified()) {
        modifiedFiles.add(foundFile);
      }
    }
    return modifiedFiles;
  }

  @Override
  public DataTransferObject<Void> call() throws IOException {
    TSFile file = new TSFile(SupportedLanguage.JAVA, this.filePath);
    TSNode node = file.getNodeFromPosition(this.line, this.column);
    String currentName = file.getTextFromRange(node.getStartByte(), node.getEndByte());
    if (Strings.isNullOrEmpty(currentName)) {
      return null;
    }
    Optional<String> packageName =
        this.javaService.getProgramService().getPackageDeclarationService().getPackageName(file);
    if (packageName.isEmpty()) {
      return null;
    }
    JavaIdentifierType identifierType = this.javaService.getIdentifierType(node);
    if (identifierType.equals(JavaIdentifierType.CLASS_NAME)) {
      List<TSFile> modifiedFiles =
          this.processClassRename(
              this.cwd, file, node, packageName.get(), currentName, this.newName);
      for (TSFile modifiedFile : modifiedFiles) {
        modifiedFile.save();
      }
    }
    return null;
  }
}
