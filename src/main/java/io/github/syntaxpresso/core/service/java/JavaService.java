package io.github.syntaxpresso.core.service.java;

import com.google.common.base.Strings;
import io.github.syntaxpresso.core.command.java.dto.CreateNewJPAEntityResponse;
import io.github.syntaxpresso.core.command.java.dto.CreateNewJavaFileResponse;
import io.github.syntaxpresso.core.command.java.dto.GetMainClassResponse;
import io.github.syntaxpresso.core.command.java.dto.RenameResponse;
import io.github.syntaxpresso.core.command.java.extra.JavaFileTemplate;
import io.github.syntaxpresso.core.command.java.extra.SourceDirectoryType;
import io.github.syntaxpresso.core.common.DataTransferObject;
import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import io.github.syntaxpresso.core.service.extra.JavaIdentifierType;
import io.github.syntaxpresso.core.service.extra.ScopeType;
import io.github.syntaxpresso.core.service.java.extra.ClassDeclarationService;
import io.github.syntaxpresso.core.service.java.extra.FieldDeclarationService;
import io.github.syntaxpresso.core.service.java.extra.FormalParameterService;
import io.github.syntaxpresso.core.service.java.extra.LocalVariableDeclarationService;
import io.github.syntaxpresso.core.service.java.extra.MethodDeclarationService;
import io.github.syntaxpresso.core.service.java.extra.ProgramService;
import io.github.syntaxpresso.core.service.java.extra.VariableNamingService;
import io.github.syntaxpresso.core.util.PathHelper;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import io.github.syntaxpresso.core.util.StringHelper;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.treesitter.TSNode;
import org.treesitter.TSQuery;
import org.treesitter.TSQueryCapture;
import org.treesitter.TSQueryCursor;
import org.treesitter.TSQueryMatch;

@Getter
@NoArgsConstructor
public class JavaService {
  private final PathHelper pathHelper = new PathHelper();
  private final ProgramService programService = new ProgramService();
  private final VariableNamingService variableNamingService = new VariableNamingService();
  private final LocalVariableDeclarationService localVariableDeclarationService =
      new LocalVariableDeclarationService(variableNamingService);
  private final FormalParameterService formalParameterService =
      new FormalParameterService(localVariableDeclarationService, variableNamingService);
  private final MethodDeclarationService methodDeclarationService =
      new MethodDeclarationService(formalParameterService, localVariableDeclarationService);
  private final FieldDeclarationService fieldDeclarationService = new FieldDeclarationService();
  private final ClassDeclarationService classDeclarationService =
      new ClassDeclarationService(fieldDeclarationService, methodDeclarationService);

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
   * Checks if a file contains a main class.
   *
   * @param file The file to check.
   * @return True if the file contains a main class, false otherwise.
   */
  public Boolean isMainClass(TSFile file) {
    return this.programService.hasMainClass(file);
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

  /**
   * Finds all classes in a file using the new hierarchical structure.
   *
   * @param file The TSFile containing the source code.
   * @return A list of class declaration nodes.
   */
  public List<TSNode> findAllClasses(TSFile file) {
    return this.programService.getAllClasses(file);
  }

  /**
   * Finds a class by name using the new hierarchical structure.
   *
   * @param file The TSFile containing the source code.
   * @param className The name of the class to find.
   * @return The class declaration node, or empty if not found.
   */
  public Optional<TSNode> findClassByName(TSFile file, String className) {
    return this.programService.findClassByName(file, className);
  }

  /**
   * Gets all fields from a specific class using the new hierarchical structure.
   *
   * @param file The TSFile containing the source code.
   * @param classNode The class declaration node.
   * @return A list of field declaration nodes.
   */
  public List<TSNode> getFieldsFromClass(TSFile file, TSNode classNode) {
    return this.getProgramService().getClassDeclarationService().getClassFields(file, classNode);
  }

  /**
   * Gets all methods from a specific class using the new hierarchical structure.
   *
   * @param file The TSFile containing the source code.
   * @param classNode The class declaration node.
   * @return A list of method declaration nodes.
   */
  public List<TSNode> getMethodsFromClass(TSFile file, TSNode classNode) {
    return this.getProgramService().getClassDeclarationService().getClassMethods(file, classNode);
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
  public DataTransferObject<CreateNewJavaFileResponse> createNewFile(
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
      CreateNewJavaFileResponse response =
          CreateNewJavaFileResponse.builder().filePath(file.getFile().getAbsolutePath()).build();
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
        boolean isMainClass = this.isMainClass(file);
        if (isMainClass) {
          Optional<String> packageName =
              this.programService.getPackageDeclarationService().getPackageName(file);
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

  public DataTransferObject<CreateNewJPAEntityResponse> createNewJPAEntity(final Path cwd, final String packageName, final String fileName) {
    DataTransferObject<CreateNewJavaFileResponse> response = createNewFile(cwd, packageName, fileName, JavaFileTemplate.CLASS, SourceDirectoryType.MAIN);
    if(!response.getSucceed()){
      return DataTransferObject.error("Unable to create file");
    }

    TSFile tsFile = new TSFile(SupportedLanguage.JAVA, Paths.get(response.getData().getFilePath()));

    String nameWithoutExtension = tsFile.getFileNameWithoutExtension().orElse(fileName);

    String snakeName = StringHelper.pascalToSnake(nameWithoutExtension);

    String tableAnnotation = "@Table(name=\"" + snakeName +"\")\n";
    List<TSFile> file = this.getAllJavaFilesFromCwd(cwd);

    for(TSFile files : file){
      List<TSNode> annotations = this.classDeclarationService.getAllClassAnnotationNodes(files);
      for(TSNode a : annotations){
        String annotationText = tsFile.getTextFromNode(a);
        if (annotationText.contains(snakeName)) {
          return DataTransferObject.error("JPA entity does exists");
        }
      }
    }

   //tsFile.insertTextBeforeNode(, tableAnnotation);
   //tsFile.insertTextBeforeNode(findClassByName.get(), "@Entity\n");

    /**
    List<TSNode> annotation = this.classDeclarationService.getAllClassAnnotationNodes(tsFile);
    System.out.println(tsFile.getTextFromNode(annotation.getLast()));
     **/

    this.programService.getImportDeclarationService().addImport(tsFile, "jakarta.persistense", "Entity");
    this.programService.getImportDeclarationService().addImport(tsFile, "jakarta.persistense", "Table");

    try{
      tsFile.save();
    } catch (IOException e) {
      return DataTransferObject.error("Could not save file: " + e.getMessage());
    }
    return null;
  }
}
