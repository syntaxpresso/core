package io.github.syntaxpresso.core.service.java;

import com.google.common.base.Strings;
import io.github.syntaxpresso.core.command.dto.CreateNewFileResponse;
import io.github.syntaxpresso.core.command.dto.GetCursorPositionInfoResponse;
import io.github.syntaxpresso.core.command.dto.GetMainClassResponse;
import io.github.syntaxpresso.core.command.dto.RenameResponse;
import io.github.syntaxpresso.core.command.extra.JavaFileTemplate;
import io.github.syntaxpresso.core.command.extra.SourceDirectoryType;
import io.github.syntaxpresso.core.common.DataTransferObject;
import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.common.extra.SupportedIDE;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import io.github.syntaxpresso.core.service.extra.JavaIdentifierType;
import io.github.syntaxpresso.core.service.extra.ScopeType;
import io.github.syntaxpresso.core.service.java.extra.ClassDeclarationService;
import io.github.syntaxpresso.core.service.java.extra.FieldDeclarationService;
import io.github.syntaxpresso.core.service.java.extra.FormalParameterService;
import io.github.syntaxpresso.core.service.java.extra.ImportDeclarationService;
import io.github.syntaxpresso.core.service.java.extra.InheritanceService;
import io.github.syntaxpresso.core.service.java.extra.JPAService;
import io.github.syntaxpresso.core.service.java.extra.LocalVariableDeclarationService;
import io.github.syntaxpresso.core.service.java.extra.MethodDeclarationService;
import io.github.syntaxpresso.core.service.java.extra.PackageDeclarationService;
import io.github.syntaxpresso.core.service.java.extra.ProgramService;
import io.github.syntaxpresso.core.service.java.extra.TypeResolutionService;
import io.github.syntaxpresso.core.service.java.extra.VariableNamingService;
import io.github.syntaxpresso.core.util.PathHelper;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.treesitter.TSNode;

@Getter
@NoArgsConstructor
public class JavaService {
  private final PathHelper pathHelper = new PathHelper();
  private final VariableNamingService variableNamingService = new VariableNamingService();
  private final FieldDeclarationService fieldDeclarationService = new FieldDeclarationService();
  private final ImportDeclarationService importDeclarationService = new ImportDeclarationService();
  private final PackageDeclarationService packageDeclarationService =
      new PackageDeclarationService();
  private final LocalVariableDeclarationService localVariableDeclarationService =
      new LocalVariableDeclarationService(variableNamingService);
  private final FormalParameterService formalParameterService =
      new FormalParameterService(localVariableDeclarationService, variableNamingService);
  private final ClassDeclarationService classDeclarationService =
      new ClassDeclarationService(fieldDeclarationService);
  private final TypeResolutionService typeResolutionService =
      new TypeResolutionService(
          formalParameterService,
          localVariableDeclarationService,
          fieldDeclarationService,
          classDeclarationService);
  private final MethodDeclarationService methodDeclarationService =
      new MethodDeclarationService(formalParameterService, localVariableDeclarationService, typeResolutionService);
  private final ProgramService programService =
      new ProgramService(
          variableNamingService,
          fieldDeclarationService,
          importDeclarationService,
          packageDeclarationService,
          localVariableDeclarationService,
          formalParameterService,
          methodDeclarationService,
          classDeclarationService,
          typeResolutionService);
  private final InheritanceService inheritanceService =
      new InheritanceService(classDeclarationService, importDeclarationService);
  private final JPAService jpaService = new JPAService(importDeclarationService);

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
    try {
      Optional<Path> sourceDirOptional =
          this.pathHelper.findDirectoryRecursively(rootDir, srcDirName);
      Path sourceDir;
      if (sourceDirOptional.isPresent()) {
        sourceDir = sourceDirOptional.get();
      } else {
        sourceDir = rootDir.resolve(srcDirName);
      }
      Path packageAsPath = Path.of(packageName.replace('.', '/'));
      Path fullPackageDir = sourceDir.resolve(packageAsPath);
      Files.createDirectories(fullPackageDir);
      return Optional.of(fullPackageDir);
    } catch (IOException e) {
      // Log the error but don't print stack trace
      System.err.println("Failed to create directory structure: " + e.getMessage());
      return Optional.empty();
    }
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
  public JavaIdentifierType getIdentifierType(TSFile file, int line, int column, SupportedIDE ide) {
    TSNode node = file.getNodeFromPosition(line, column, ide);
    return this.getIdentifierType(node, ide);
  }

  /**
   * Renames the main class within a .java file and renames the file itself.
   *
   * <p>This method performs the following steps:
   *
   * <ol>
   *   <li>Parses the Java file to find the main class declaration.
   *   <li>If no main class is found, it attempts to find the first class declaration.
   *   <li>Updates the source code in memory to rename the class.
   *   <li>Writes the updated content to a new file with the new class name.
   *   <li>Deletes the original file.
   * </ol>
   *
   * @param filePath The absolute path to the .java file to be renamed.
   * @param newName The new name for the class and the file (without the .java extension).
   * @return The path to the newly created and renamed file.
   * @throws IOException if an I/O error occurs during file reading, writing, or deletion, or if no
   *     class declaration is found in the file.
   */
  private Path renameFileAndContent(final Path filePath, final String newName) throws IOException {
    TSFile tsFile = new TSFile(SupportedLanguage.JAVA, filePath);
    Optional<TSNode> mainClassNode = this.classDeclarationService.getMainClass(tsFile);
    if (mainClassNode.isPresent()) {
      this.classDeclarationService.renameClass(tsFile, mainClassNode.get(), newName);
      String updatedContent = tsFile.getSourceCode();
      Path newPath = filePath.resolveSibling(newName + ".java");
      Files.writeString(newPath, updatedContent);
      Files.delete(filePath);
      return newPath;
    } else {
      List<TSNode> classes = this.classDeclarationService.findAllClassDeclarations(tsFile);
      if (!classes.isEmpty()) {
        this.classDeclarationService.renameClass(tsFile, classes.get(0), newName);
        String updatedContent = tsFile.getSourceCode();
        Path newPath = filePath.resolveSibling(newName + ".java");
        Files.writeString(newPath, updatedContent);
        Files.delete(filePath);
        return newPath;
      }
    }
    throw new IOException("No class found in file " + filePath);
  }

  /**
   * Checks if the file should be renamed based on the class name.
   *
   * @param file The TSFile to check.
   * @param currentName The current class name.
   * @return True if the file should be renamed, false otherwise.
   */
  private boolean shouldRenameFileName(TSFile file, String currentName) {
    String fileName =
        com.google.common.io.Files.getNameWithoutExtension(file.getFile().getAbsolutePath());
    return fileName.equals(currentName);
  }

  /**
   * Processes a class rename operation across the entire project.
   *
   * @param cwd The current working directory.
   * @param file The file containing the class to rename.
   * @param node The class identifier node.
   * @param packageName The package name of the class.
   * @param currentName The current class name.
   * @param newName The new class name.
   * @return A list of modified files.
   */
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
    List<TSFile> allJavaFiles = this.getAllJavaFilesFromCwd(cwd);
    for (TSFile foundFile : allJavaFiles) {
      Optional<String> foundFilePackageName =
          this.packageDeclarationService.getPackageName(foundFile);
      if (foundFilePackageName.isEmpty()) {
        continue;
      }
      // Skip original file.
      if (foundFile.getFile().getAbsolutePath().equals(file.getFile().getAbsolutePath())) {
        continue;
      }
      Optional<TSNode> importNode =
          this.importDeclarationService.getImportDeclarationNode(
              foundFile, currentName, packageName);
      if (importNode.isEmpty() && !foundFilePackageName.get().equals(packageName)) {
        continue;
      }
      // Rename usages in different contexts
      this.localVariableDeclarationService.renameLocalVariablesInFile(
          foundFile, currentName, newName);
      this.formalParameterService.renameFormalParametersInFile(foundFile, currentName, newName);
      this.fieldDeclarationService.renameClassFields(foundFile, currentName, newName);
      // Update imports if necessary
      if (!foundFilePackageName.get().equals(packageName)) {
        this.importDeclarationService.updateImport(
            foundFile, packageName + "." + currentName, packageName + "." + newName);
      }
      if (foundFile.isModified()) {
        modifiedFiles.add(foundFile);
      }
    }
    return modifiedFiles;
  }

  /**
   * Creates a new Java file.
   *
   * @param cwd The current working directory to search in.
   * @param packageName The package name for the new file.
   * @param fileName The name for the new file.
   * @param fileType The type for the new file (CLASS | INTERFACE | ENUM | RECORD | ANNOTATION).
   * @param sourceDirectoryType Whether if the file should be created in the main or test module
   *     (MAIN | TEST).
   * @return A DataTransferObject containing the new file information or error.
   */
  public DataTransferObject<CreateNewFileResponse> createNewFile(
      Path cwd,
      String packageName,
      String fileName,
      JavaFileTemplate fileType,
      SourceDirectoryType sourceDirectoryType) {
    // Validate current working directory
    if (cwd == null || !Files.exists(cwd)) {
      return DataTransferObject.error("Current working directory does not exist.");
    }
    // Validate package name
    if (Strings.isNullOrEmpty(packageName)) {
      return DataTransferObject.error("Package name invalid.");
    }
    // Validate file name
    if (Strings.isNullOrEmpty(fileName)) {
      return DataTransferObject.error("File name invalid.");
    }
    // Validate file type
    if (fileType == null) {
      return DataTransferObject.error("File type is required.");
    }
    // Validate source directory type
    if (sourceDirectoryType == null) {
      return DataTransferObject.error("Source directory type is required.");
    }
    final String srcDirName =
        (sourceDirectoryType == SourceDirectoryType.MAIN) ? "src/main/java" : "src/test/java";
    try {
      Optional<Path> sourceDirOptional = this.pathHelper.findDirectoryRecursively(cwd, srcDirName);
      if (sourceDirOptional.isEmpty()) {
        if (!Files.exists(cwd.resolve(srcDirName))) {
          return DataTransferObject.error("Cannot find source directory.");
        }
      }
    } catch (IOException e) {
      return DataTransferObject.error("Failed to search for source directory: " + e.getMessage());
    }
    try {
      String className = fileName.trim();
      className = com.google.common.io.Files.getNameWithoutExtension(className);
      String template = fileType.getSourceContent(packageName, className);
      TSFile file = new TSFile(SupportedLanguage.JAVA, template);
      Optional<Path> filePath = this.findFilePath(cwd, packageName, sourceDirectoryType);
      if (filePath.isEmpty()) {
        return DataTransferObject.error("Package name couldn't be determined.");
      }
      Path targetPath =
          filePath.get().resolve(className.concat(SupportedLanguage.JAVA.getFileExtension()));
      // Check if file already exists
      if (Files.exists(targetPath)) {
        return DataTransferObject.error("File already exists: " + targetPath.toString());
      }
      // Attempt to save the file
      file.saveAs(targetPath);
      CreateNewFileResponse response =
          CreateNewFileResponse.builder().filePath(file.getFile().getAbsolutePath()).build();
      return DataTransferObject.success(response);
    } catch (IOException e) {
      return DataTransferObject.error("Failed to create file: " + e.getMessage());
    } catch (Exception e) {
      return DataTransferObject.error("Unexpected error occurred: " + e.getMessage());
    }
  }

  /**
   * Finds the main class in the current working directory.
   *
   * @param cwd The current working directory to search in.
   * @return A DataTransferObject containing the main class information or error.
   */
  public DataTransferObject<GetMainClassResponse> getMainClass(Path cwd) {
    // Validate current working directory
    if (cwd == null || !Files.exists(cwd)) {
      return DataTransferObject.error("Current working directory does not exist.");
    }
    try {
      List<TSFile> allFiles = this.getAllJavaFilesFromCwd(cwd);
      for (TSFile file : allFiles) {
        boolean isMainClass = this.programService.hasMainClass(file);
        if (isMainClass) {
          Optional<String> packageName = this.packageDeclarationService.getPackageName(file);
          if (packageName.isEmpty()) {
            return DataTransferObject.error(
                "Main class found, but package name couldn't be determined.");
          }
          GetMainClassResponse response =
              GetMainClassResponse.builder()
                  .filePath(file.getFile().getAbsolutePath())
                  .packageName(packageName.get())
                  .build();
          return DataTransferObject.success(response);
        }
      }
      return DataTransferObject.error(
          "Main class couldn't be found in the current working directory.");
    } catch (Exception e) {
      return DataTransferObject.error("Unexpected error occurred: " + e.getMessage());
    }
  }

  /**
   * Renames a Java class/interface/enum and its file.
   *
   * @param filePath The absolute path to the .java file.
   * @param newName The new name for the class/interface/enum.
   * @return A DataTransferObject containing the new file path or an error.
   */
  public DataTransferObject<RenameResponse> rename(final Path filePath, final String newName) {
    if (!Files.exists(filePath)) {
      return DataTransferObject.error("File does not exist: " + filePath);
    }
    if (!filePath.toString().endsWith(".java")) {
      return DataTransferObject.error("File is not a .java file: " + filePath);
    }
    try {
      Path newFilePath = this.renameFileAndContent(filePath, newName);
      return DataTransferObject.success(
          RenameResponse.builder().filePath(newFilePath.toString()).build());
    } catch (IOException e) {
      return DataTransferObject.error("Failed to rename file: " + e.getMessage());
    }
  }

  public DataTransferObject<GetCursorPositionInfoResponse> getTextFromCursorPosition(
      Path filePath, SupportedLanguage language, SupportedIDE ide, Integer line, Integer column) {
    if (!Files.exists(filePath)) {
      return DataTransferObject.error("File does not exist: " + filePath);
    }
    if (!filePath.toString().endsWith(".java")) {
      return DataTransferObject.error("File is not a .java file: " + filePath);
    }
    TSFile file = new TSFile(language, filePath);
    // TSFile.getNodeFromPosition expects 1-based coordinates (matching editor conventions)
    TSNode node = file.getNodeFromPosition(line, column, ide);
    if (node == null) {
      return DataTransferObject.error("No symbol found at the specified position.");
    }
    JavaIdentifierType identifierType = this.getIdentifierType(node, ide);
    if (identifierType == null) {
      return DataTransferObject.error(
          "Unable to determine symbol type at cursor position. Node type: "
              + node.getType()
              + ", Node text: '"
              + file.getTextFromNode(node)
              + "'");
    }
    String text;
    try {
      text = file.getTextFromRange(node.getStartByte(), node.getEndByte());
    } catch (Exception e) {
      return DataTransferObject.error("Error getting text from node: " + e.getMessage());
    }
    if (Strings.isNullOrEmpty(text)) {
      return DataTransferObject.error("Unable to determine current symbol name.");
    }
    GetCursorPositionInfoResponse response =
        GetCursorPositionInfoResponse.builder()
            .filePath(filePath.toString())
            .language(SupportedLanguage.JAVA)
            .node(node.toString())
            .nodeText(text)
            .nodeType(identifierType)
            .build();
    return DataTransferObject.success(response);
  }

  /**
   * Renames a symbol (class, method, field, etc.) and all its usages based on cursor position.
   *
   * @param cwd The current working directory.
   * @param filePath The absolute path to the .java file.
   * @param line The cursor line position.
   * @param column The cursor column position.
   * @param newName The new name for the symbol.
   * @return A DataTransferObject containing the result or an error.
   */
  public DataTransferObject<RenameResponse> rename(
      final Path cwd,
      final Path filePath,
      final SupportedIDE ide,
      final int line,
      final int column,
      final String newName) {
    try {
      TSFile file = new TSFile(SupportedLanguage.JAVA, filePath);
      TSNode node = file.getNodeFromPosition(line, column, ide);
      if (node == null) {
        return DataTransferObject.error("No symbol found at the specified position.");
      }
      String currentName;
      try {
        currentName = file.getTextFromRange(node.getStartByte(), node.getEndByte());
      } catch (Exception e) {
        return DataTransferObject.error("Error getting text from node: " + e.getMessage());
      }
      if (Strings.isNullOrEmpty(currentName)) {
        return DataTransferObject.error("Unable to determine current symbol name.");
      }
      Optional<String> packageName = this.packageDeclarationService.getPackageName(file);
      if (packageName.isEmpty()) {
        return DataTransferObject.error("Unable to determine package name.");
      }
      // If we hit a method_declaration, class_declaration, etc., try to find the name node
      if ("method_declaration".equals(node.getType())
          || "class_declaration".equals(node.getType())) {
        TSNode nameNode = node.getChildByFieldName("name");
        if (nameNode != null) {
          node = nameNode;
          currentName = file.getTextFromNode(nameNode);
        } else {
          return DataTransferObject.error("Unable to find name node in " + node.getType());
        }
      }
      JavaIdentifierType identifierType = this.getIdentifierType(node, ide);
      if (identifierType == null) {
        return DataTransferObject.error(
            "Unable to determine symbol type at cursor position. Node type: "
                + node.getType()
                + ", Node text: '"
                + file.getTextFromNode(node)
                + "'");
      }
      List<TSFile> modifiedFiles = new ArrayList<>();
      int renamedNodes = 0;
      if (identifierType.equals(JavaIdentifierType.CLASS_NAME)) {
        modifiedFiles.addAll(
            this.processClassRename(cwd, file, node, packageName.get(), currentName, newName));
        // Count the class name change as 1 node
        renamedNodes = 1;
        // Add count for file rename if applicable
        if (this.shouldRenameFileName(file, currentName)) {
          renamedNodes += 1;
        }
        // Count additional nodes from other files that were modified
        for (TSFile modifiedFile : modifiedFiles) {
          if (!modifiedFile.getFile().equals(file.getFile()) && modifiedFile.isModified()) {
            // Estimate nodes modified in each additional file (conservative estimate)
            renamedNodes += 1; // At least one reference per file
          }
        }
      } else if (identifierType.equals(JavaIdentifierType.METHOD_NAME)) {
        TSNode methodDeclarationNode = node.getParent();
        if (methodDeclarationNode != null) {
          List<TSFile> allJavaFiles = this.getAllJavaFilesFromCwd(cwd);
          Optional<TSNode> classDeclarationNode = this.classDeclarationService.getMainClass(file);
          if (classDeclarationNode.isEmpty()) {
            return null;
          }
          Optional<String> className =
              classDeclarationService.getClassName(file, classDeclarationNode.get());
          if (className.isEmpty()) {
            return null;
          }
          List<TSFile> renamedMethodFiles =
              this.methodDeclarationService.renameMethodAndUsages(
                  file,
                  methodDeclarationNode,
                  currentName,
                  newName,
                  cwd,
                  className.get(),
                  allJavaFiles);
          modifiedFiles.addAll(renamedMethodFiles);
          renamedNodes = modifiedFiles.size();
        }
      } else {
        return DataTransferObject.error("Renaming of " + identifierType + " is not yet supported.");
      }
      // Save all modified files
      for (TSFile modifiedFile : modifiedFiles) {
        modifiedFile.save();
      }
      return DataTransferObject.success(
          RenameResponse.builder()
              .filePath(file.getFile().getAbsolutePath())
              .renamedNodes(renamedNodes)
              .newName(newName)
              .build());
    } catch (Exception e) {
      return DataTransferObject.error("Unexpected error: " + e.getMessage());
    }
  }

  /**
   * Creates a JPA Repository interface for an entity class.
   *
   * @param cwd The current working directory.
   * @param entityFilePath The path to the entity file.
   * @return A DataTransferObject containing the new repository file information or error.
   */
  public DataTransferObject<CreateNewFileResponse> createJPARepository(
      Path cwd, Path entityFilePath) {
    if (!entityFilePath.toString().endsWith(".java")) {
      return DataTransferObject.error("File is not a .java file: " + entityFilePath);
    }
    try {
      TSFile entityFile = new TSFile(SupportedLanguage.JAVA, entityFilePath);
      Optional<String> className = entityFile.getFileNameWithoutExtension();
      if (className.isEmpty()) {
        return DataTransferObject.error("Unable to get entity class name");
      }
      Optional<String> packageName = this.packageDeclarationService.getPackageName(entityFile);
      if (packageName.isEmpty()) {
        return DataTransferObject.error("Unable to get package name");
      }
      Optional<TSNode> classDeclarationNode =
          this.classDeclarationService.findClassByName(entityFile, className.get());
      if (classDeclarationNode.isEmpty()) {
        return DataTransferObject.error("No class declaration found in file");
      }
      if (!this.jpaService.isJPAEntity(entityFile, classDeclarationNode.get())) {
        return DataTransferObject.error("Class is not a JPA entity (@Entity annotation not found)");
      }
      Optional<InheritanceService.FieldWithFile> idFieldWithFile =
          this.inheritanceService.findIdFieldInHierarchy(
              cwd, entityFile, classDeclarationNode.get());
      if (idFieldWithFile.isEmpty()) {
        return DataTransferObject.error(
            "No @Id field found in entity hierarchy. Note: Only local project classes are currently"
                + " supported.");
      }
      Optional<String> idType =
          this.jpaService.extractFieldType(idFieldWithFile.get().file, idFieldWithFile.get().field);
      if (idType.isEmpty()) {
        return DataTransferObject.error("Unable to determine @Id field type");
      }
      String repositoryName = className.get() + "Repository";
      DataTransferObject<CreateNewFileResponse> createResult =
          this.createNewFile(
              cwd,
              packageName.get(),
              repositoryName,
              JavaFileTemplate.INTERFACE,
              SourceDirectoryType.MAIN);
      if (!createResult.getSucceed()) {
        return createResult;
      }
      TSFile repositoryFile =
          new TSFile(SupportedLanguage.JAVA, Path.of(createResult.getData().getFilePath()));
      this.jpaService.configureRepositoryFile(
          repositoryFile, className.get(), idType.get(), packageName.get());
      repositoryFile.save();
      return createResult;
    } catch (Exception e) {
      return DataTransferObject.error("Failed to create repository: " + e.getMessage());
    }
  }
}
