package io.github.syntaxpresso.core.service.java;

import com.google.common.base.Strings;
import io.github.syntaxpresso.core.command.dto.CreateNewFileResponse;
import io.github.syntaxpresso.core.command.dto.GetCursorPositionInfoResponse;
import io.github.syntaxpresso.core.command.dto.GetJPAEntityInfoResponse;
import io.github.syntaxpresso.core.command.dto.GetMainClassResponse;
import io.github.syntaxpresso.core.command.dto.RenameResponse;
import io.github.syntaxpresso.core.command.extra.JavaBasicType;
import io.github.syntaxpresso.core.command.extra.JavaFileTemplate;
import io.github.syntaxpresso.core.command.extra.SourceDirectoryType;
import io.github.syntaxpresso.core.common.DataTransferObject;
import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.common.extra.SupportedIDE;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import io.github.syntaxpresso.core.service.extra.JavaIdentifierType;
import io.github.syntaxpresso.core.service.java.jpa.InheritanceService;
import io.github.syntaxpresso.core.util.PathHelper;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.treesitter.TSNode;

@Getter
@NoArgsConstructor
public class JavaCommandService {
  private final PathHelper pathHelper = new PathHelper();
  private final JavaLanguageService javaLanguageService = new JavaLanguageService();
  private final JPAService jpaService = new JPAService(javaLanguageService);

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
      System.err.println("Failed to create directory structure: " + e.getMessage());
      return Optional.empty();
    }
  }

  /** Renames the main class within a .java file and renames the file itself. */
  private Path renameFileAndContent(final Path filePath, final String newName) throws IOException {
    TSFile tsFile = new TSFile(SupportedLanguage.JAVA, filePath);
    Optional<TSNode> mainClassNode =
        this.javaLanguageService.getClassDeclarationService().getMainClass(tsFile);
    if (mainClassNode.isPresent()) {
      this.javaLanguageService
          .getClassDeclarationService()
          .renameClass(tsFile, mainClassNode.get(), newName);
      String updatedContent = tsFile.getSourceCode();
      Path newPath = filePath.resolveSibling(newName + ".java");
      Files.writeString(newPath, updatedContent);
      Files.delete(filePath);
      return newPath;
    } else {
      List<TSNode> classes =
          this.javaLanguageService.getClassDeclarationService().findAllClassDeclarations(tsFile);
      if (!classes.isEmpty()) {
        this.javaLanguageService
            .getClassDeclarationService()
            .renameClass(tsFile, classes.get(0), newName);
        String updatedContent = tsFile.getSourceCode();
        Path newPath = filePath.resolveSibling(newName + ".java");
        Files.writeString(newPath, updatedContent);
        Files.delete(filePath);
        return newPath;
      }
    }
    throw new IOException("No class found in file " + filePath);
  }

  /** Creates a new Java file. */
  public DataTransferObject<CreateNewFileResponse> createNewFile(
      Path cwd,
      String packageName,
      String fileName,
      JavaFileTemplate fileType,
      SourceDirectoryType sourceDirectoryType) {
    if (cwd == null || !Files.exists(cwd)) {
      return DataTransferObject.error("Current working directory does not exist.");
    }
    if (Strings.isNullOrEmpty(packageName)) {
      return DataTransferObject.error("Package name invalid.");
    }
    if (Strings.isNullOrEmpty(fileName)) {
      return DataTransferObject.error("File name invalid.");
    }
    if (fileType == null) {
      return DataTransferObject.error("File type is required.");
    }
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
      if (Files.exists(targetPath)) {
        return DataTransferObject.error("File already exists: " + targetPath.toString());
      }
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

  /** Finds the main class in the current working directory. */
  public DataTransferObject<GetMainClassResponse> getMainClass(Path cwd) {
    if (cwd == null || !Files.exists(cwd)) {
      return DataTransferObject.error("Current working directory does not exist.");
    }
    try {
      List<TSFile> allFiles = this.javaLanguageService.getAllJavaFilesFromCwd(cwd);
      for (TSFile file : allFiles) {
        boolean isMainClass = this.javaLanguageService.getProgramService().hasMainClass(file);
        if (isMainClass) {
          Optional<String> packageName =
              this.javaLanguageService.getPackageDeclarationService().getPackageName(file);
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

  /** Renames a Java class/interface/enum and its file. */
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
    TSNode node = file.getNodeFromPosition(line, column, ide);
    if (node == null) {
      return DataTransferObject.error("No symbol found at the specified position.");
    }
    JavaIdentifierType identifierType = this.javaLanguageService.getIdentifierType(node, ide);
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

  /** Renames a symbol (class, method, field, etc.) and all its usages based on cursor position. */
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
      Optional<String> packageName =
          this.javaLanguageService.getPackageDeclarationService().getPackageName(file);
      if (packageName.isEmpty()) {
        return DataTransferObject.error("Unable to determine package name.");
      }
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
      JavaIdentifierType identifierType = this.javaLanguageService.getIdentifierType(node, ide);
      if (identifierType == null) {
        return DataTransferObject.error(
            "Unable to determine symbol type at cursor position. Node type: "
                + node.getType()
                + ", Node text: '"
                + file.getTextFromNode(node)
                + "'");
      }
      List<TSFile> modifiedFiles = new java.util.ArrayList<>();
      int renamedNodes = 0;
      if (identifierType.equals(JavaIdentifierType.CLASS_NAME)) {
        modifiedFiles.addAll(
            this.javaLanguageService.processClassRename(
                cwd, file, node, packageName.get(), currentName, newName));
        renamedNodes = 1;
        String fileName =
            com.google.common.io.Files.getNameWithoutExtension(file.getFile().getAbsolutePath());
        if (fileName.equals(currentName)) {
          renamedNodes += 1;
        }
        for (TSFile modifiedFile : modifiedFiles) {
          if (!modifiedFile.getFile().equals(file.getFile()) && modifiedFile.isModified()) {
            renamedNodes += 1;
          }
        }
      } else if (identifierType.equals(JavaIdentifierType.METHOD_NAME)) {
        TSNode methodDeclarationNode = node.getParent();
        if (methodDeclarationNode != null) {
          Optional<TSNode> classDeclarationNode =
              this.javaLanguageService.getClassDeclarationService().getMainClass(file);
          if (classDeclarationNode.isEmpty()) {
            return DataTransferObject.error("Unable to find main class declaration in file.");
          }
          Optional<String> className =
              this.javaLanguageService
                  .getClassDeclarationService()
                  .getClassName(file, classDeclarationNode.get());
          if (className.isEmpty()) {
            return DataTransferObject.error("Unable to determine class name from declaration.");
          }
          List<TSFile> renamedMethodFiles =
              this.javaLanguageService.processMethodRename(
                  cwd, file, methodDeclarationNode, currentName, newName, className.get());
          if (renamedMethodFiles != null) {
            modifiedFiles.addAll(renamedMethodFiles);
          }
          renamedNodes = modifiedFiles.size();
        }
      } else {
        return DataTransferObject.error("Renaming of " + identifierType + " is not yet supported.");
      }
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

  /** Creates a JPA Repository interface for an entity class. */
  public DataTransferObject<CreateNewFileResponse> createJPARepository(
      Path cwd, Path entityFilePath) {
    return this.jpaService.createJPARepository(cwd, entityFilePath);
  }

  public DataTransferObject<GetJPAEntityInfoResponse> getJPAEntityInfo(
      final Path cwd, final Path filePath, final SupportedIDE ide) {
    if (!filePath.toString().endsWith(".java")) {
      return DataTransferObject.error("File is not a .java file: " + filePath);
    }
    TSFile file = new TSFile(SupportedLanguage.JAVA, filePath);
    GetJPAEntityInfoResponse response = new GetJPAEntityInfoResponse();
    Optional<TSNode> mainClassNode =
        this.getJavaLanguageService().getClassDeclarationService().getMainClass(file);
    if (mainClassNode.isEmpty()) {
      return DataTransferObject.error("Unable to find main class declaration in file.");
    }
    boolean isJPAEntity =
        this.getJpaService().getJpaOperations().isJPAEntity(file, mainClassNode.get());
    if (!isJPAEntity) {
      return DataTransferObject.error("Class is not a JPA entity (@Entity annotation not found)");
    }
    Optional<String> packageName =
        this.getJavaLanguageService().getPackageDeclarationService().getPackageName(file);
    if (packageName.isEmpty()) {
      return DataTransferObject.error("Unable to get package name");
    }
    Optional<InheritanceService.FieldWithFile> idFieldWithFile =
        this.jpaService
            .getInheritanceService()
            .findIdFieldInHierarchy(cwd, file, mainClassNode.get());
    if (idFieldWithFile.isPresent()) {
      Optional<String> idTypeName =
          this.jpaService
              .getJpaOperations()
              .extractFieldType(idFieldWithFile.get().file, idFieldWithFile.get().field);
      if (idTypeName.isPresent()) {
        Optional<JavaBasicType> idType = JavaBasicType.getByTypeName(idTypeName.get());
        if (idType.isPresent()) {
          response.setIdFieldType(idType.get().getTypeName());
          response.setIdFieldPackageName(idType.get().getPackageName().get());
        }
      }
    }
    if (response.getIdFieldType() == null || response.getIdFieldPackageName() == null) {
      List<Map<String, String>> recommendedTypes = JavaBasicType.getRecommendedIdTypesForJson();
      response.setRecommendedTypes(recommendedTypes);
    }
    Optional<String> className =
        this.getJavaLanguageService()
            .getClassDeclarationService()
            .getClassName(file, mainClassNode.get());
    if (className.isPresent()) {
      response.setRecommendedRepositoryName(className.get() + "Repository");
    } else {
      response.setRecommendedRepositoryName(file.getFileNameWithoutExtension() + "Repository");
    }
    response.setIsJPAEntity(isJPAEntity);
    response.setEntityPackageName(packageName.get());
    response.setEntityPath(file.getFile().getAbsolutePath());
    return DataTransferObject.success(response);
  }
}

