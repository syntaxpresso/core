package io.github.syntaxpresso.core.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.syntaxpresso.core.command.dto.CreateNewFileResponse;
import io.github.syntaxpresso.core.command.extra.JavaFileTemplate;
import io.github.syntaxpresso.core.command.extra.JavaSourceDirectoryType;
import io.github.syntaxpresso.core.common.DataTransferObject;
import io.github.syntaxpresso.core.common.extra.SupportedIDE;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import io.github.syntaxpresso.core.service.java.JavaCommandService;
import io.github.syntaxpresso.core.service.java.JavaLanguageService;
import io.github.syntaxpresso.core.service.java.language.ClassDeclarationService;
import io.github.syntaxpresso.core.service.java.language.FieldDeclarationService;
import io.github.syntaxpresso.core.service.java.language.FormalParameterDeclarationService;
import io.github.syntaxpresso.core.service.java.language.ImportDeclarationService;
import io.github.syntaxpresso.core.service.java.language.LocalVariableDeclarationService;
import io.github.syntaxpresso.core.service.java.language.MethodDeclarationService;
import io.github.syntaxpresso.core.service.java.language.PackageDeclarationService;
import io.github.syntaxpresso.core.service.java.language.VariableNamingService;
import io.github.syntaxpresso.core.util.PathHelper;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Comprehensive test suite for CreateNewFileCommand.
 *
 * <p>Tests cover command configuration, language support, file templates, source directory
 * handling, IDE support, parameter validation, and integration scenarios.
 */
@DisplayName("CreateNewFileCommand Tests")
class CreateNewFileCommandTest {
  private JavaCommandService javaService;
  @TempDir Path tempDir;

  @BeforeEach
  void setUp() {
    PathHelper pathHelper = new PathHelper();
    VariableNamingService variableNamingService = new VariableNamingService();
    FieldDeclarationService fieldDeclarationService = new FieldDeclarationService();
    FormalParameterDeclarationService formalParameterDeclarationService =
        new FormalParameterDeclarationService();
    MethodDeclarationService methodDeclarationService =
        new MethodDeclarationService(formalParameterDeclarationService);
    ClassDeclarationService classDeclarationService =
        new ClassDeclarationService(fieldDeclarationService, methodDeclarationService);
    PackageDeclarationService packageDeclarationService = new PackageDeclarationService();
    ImportDeclarationService importDeclarationService = new ImportDeclarationService();
    LocalVariableDeclarationService localVariableDeclarationService =
        new LocalVariableDeclarationService();

    JavaLanguageService javaLanguageService =
        new JavaLanguageService(
            pathHelper,
            variableNamingService,
            classDeclarationService,
            packageDeclarationService,
            importDeclarationService,
            localVariableDeclarationService);

    this.javaService = new JavaCommandService(pathHelper, javaLanguageService);
  }

  /** Creates a command instance with real service and sets all required fields. */
  private CreateNewFileCommand createCommand(
      Path cwd,
      SupportedLanguage language,
      SupportedIDE ide,
      String packageName,
      String fileName,
      JavaFileTemplate fileType,
      JavaSourceDirectoryType sourceDirectoryType)
      throws Exception {
    CreateNewFileCommand command = new CreateNewFileCommand(this.javaService);

    setField(command, "cwd", cwd);
    setField(command, "language", language);
    setField(command, "ide", ide);
    setField(command, "packageName", packageName);
    setField(command, "fileName", fileName);
    setField(command, "fileType", fileType);
    if (sourceDirectoryType != null) {
      setField(command, "sourceDirectoryType", sourceDirectoryType);
    }

    return command;
  }

  private void setField(Object target, String fieldName, Object value) throws Exception {
    Field field = target.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }

  private void setupSourceDirectories() throws IOException {
    Files.createDirectories(tempDir.resolve("src/main/java"));
    Files.createDirectories(tempDir.resolve("src/test/java"));
  }

  @Nested
  @DisplayName("Command Configuration Tests")
  class CommandConfigurationTests {
    @Test
    @DisplayName("should have correct command name and description")
    void shouldHaveCorrectCommandConfiguration() {
      picocli.CommandLine.Command annotation =
          CreateNewFileCommand.class.getAnnotation(picocli.CommandLine.Command.class);
      assertNotNull(annotation);
      assertEquals("create-new-file", annotation.name());
      assertEquals("Create a new Java file", annotation.description()[0]);
    }

    @Test
    @DisplayName("should have required constructor with JavaCommandService")
    void shouldHaveRequiredConstructorWithJavaCommandService() throws Exception {
      CreateNewFileCommand command = new CreateNewFileCommand(javaService);
      assertNotNull(command);

      Field serviceField = CreateNewFileCommand.class.getDeclaredField("javaCommandService");
      serviceField.setAccessible(true);
      assertEquals(javaService, serviceField.get(command));
    }

    @Test
    @DisplayName("should have default IDE as NONE")
    void shouldHaveDefaultIDEAsNone() throws Exception {
      CreateNewFileCommand command = new CreateNewFileCommand(javaService);
      Field ideField = CreateNewFileCommand.class.getDeclaredField("ide");
      ideField.setAccessible(true);
      assertEquals(SupportedIDE.NONE, ideField.get(command));
    }

    @Test
    @DisplayName("should have default source directory as MAIN")
    void shouldHaveDefaultSourceDirectoryAsMain() throws Exception {
      CreateNewFileCommand command = new CreateNewFileCommand(javaService);
      Field sourceDirectoryField =
          CreateNewFileCommand.class.getDeclaredField("sourceDirectoryType");
      sourceDirectoryField.setAccessible(true);
      assertEquals(JavaSourceDirectoryType.MAIN, sourceDirectoryField.get(command));
    }
  }

  @Nested
  @DisplayName("Language Support Tests")
  class LanguageSupportTests {
    @Test
    @DisplayName("should delegate to JavaCommandService for JAVA language")
    void shouldDelegateToJavaCommandServiceForJavaLanguage() throws Exception {
      setupSourceDirectories();

      CreateNewFileCommand command =
          createCommand(
              tempDir,
              SupportedLanguage.JAVA,
              SupportedIDE.NONE,
              "com.example",
              "TestClass",
              JavaFileTemplate.CLASS,
              JavaSourceDirectoryType.MAIN);

      DataTransferObject<CreateNewFileResponse> result = command.call();
      assertTrue(result.getSucceed());
      assertTrue(result.getData().getFilePath().endsWith("TestClass.java"));
    }

    @Test
    @DisplayName("should return error for unsupported language")
    void shouldReturnErrorForUnsupportedLanguage() throws Exception {
      CreateNewFileCommand command = new CreateNewFileCommand(javaService);

      setField(command, "cwd", tempDir);
      setField(command, "language", null);
      setField(command, "ide", SupportedIDE.NONE);
      setField(command, "packageName", "com.example");
      setField(command, "fileName", "TestClass");
      setField(command, "fileType", JavaFileTemplate.CLASS);
      setField(command, "sourceDirectoryType", JavaSourceDirectoryType.MAIN);

      DataTransferObject<CreateNewFileResponse> result = command.call();
      assertFalse(result.getSucceed());
      assertEquals("Language not supported.", result.getErrorReason());
    }
  }

  @Nested
  @DisplayName("File Template Tests")
  class FileTemplateTests {
    @Test
    @DisplayName("should handle CLASS file template")
    void shouldHandleClassFileTemplate() throws Exception {
      setupSourceDirectories();

      CreateNewFileCommand command =
          createCommand(
              tempDir,
              SupportedLanguage.JAVA,
              SupportedIDE.NONE,
              "com.example",
              "MyClass",
              JavaFileTemplate.CLASS,
              JavaSourceDirectoryType.MAIN);

      DataTransferObject<CreateNewFileResponse> result = command.call();
      assertTrue(result.getSucceed());
      assertTrue(result.getData().getFilePath().endsWith("MyClass.java"));
    }

    @Test
    @DisplayName("should handle INTERFACE file template")
    void shouldHandleInterfaceFileTemplate() throws Exception {
      setupSourceDirectories();

      CreateNewFileCommand command =
          createCommand(
              tempDir,
              SupportedLanguage.JAVA,
              SupportedIDE.NONE,
              "com.example",
              "MyInterface",
              JavaFileTemplate.INTERFACE,
              JavaSourceDirectoryType.MAIN);

      DataTransferObject<CreateNewFileResponse> result = command.call();
      assertTrue(result.getSucceed());
      assertTrue(result.getData().getFilePath().endsWith("MyInterface.java"));
    }

    @Test
    @DisplayName("should handle RECORD file template")
    void shouldHandleRecordFileTemplate() throws Exception {
      setupSourceDirectories();

      CreateNewFileCommand command =
          createCommand(
              tempDir,
              SupportedLanguage.JAVA,
              SupportedIDE.NONE,
              "com.example",
              "MyRecord",
              JavaFileTemplate.RECORD,
              JavaSourceDirectoryType.MAIN);

      DataTransferObject<CreateNewFileResponse> result = command.call();
      assertTrue(result.getSucceed());
      assertTrue(result.getData().getFilePath().endsWith("MyRecord.java"));
    }

    @Test
    @DisplayName("should handle ENUM file template")
    void shouldHandleEnumFileTemplate() throws Exception {
      setupSourceDirectories();

      CreateNewFileCommand command =
          createCommand(
              tempDir,
              SupportedLanguage.JAVA,
              SupportedIDE.NONE,
              "com.example",
              "MyEnum",
              JavaFileTemplate.ENUM,
              JavaSourceDirectoryType.MAIN);

      DataTransferObject<CreateNewFileResponse> result = command.call();
      assertTrue(result.getSucceed());
      assertTrue(result.getData().getFilePath().endsWith("MyEnum.java"));
    }

    @Test
    @DisplayName("should handle ANNOTATION file template")
    void shouldHandleAnnotationFileTemplate() throws Exception {
      setupSourceDirectories();

      CreateNewFileCommand command =
          createCommand(
              tempDir,
              SupportedLanguage.JAVA,
              SupportedIDE.NONE,
              "com.example",
              "MyAnnotation",
              JavaFileTemplate.ANNOTATION,
              JavaSourceDirectoryType.MAIN);

      DataTransferObject<CreateNewFileResponse> result = command.call();
      assertTrue(result.getSucceed());
      assertTrue(result.getData().getFilePath().endsWith("MyAnnotation.java"));
    }
  }

  @Nested
  @DisplayName("Source Directory Tests")
  class SourceDirectoryTests {
    @Test
    @DisplayName("should handle MAIN source directory type")
    void shouldHandleMainSourceDirectoryType() throws Exception {
      setupSourceDirectories();

      CreateNewFileCommand command =
          createCommand(
              tempDir,
              SupportedLanguage.JAVA,
              SupportedIDE.NONE,
              "com.example",
              "MainClass",
              JavaFileTemplate.CLASS,
              JavaSourceDirectoryType.MAIN);

      DataTransferObject<CreateNewFileResponse> result = command.call();
      assertTrue(result.getSucceed());
      assertTrue(result.getData().getFilePath().contains("src/main/java"));
    }

    @Test
    @DisplayName("should handle TEST source directory type")
    void shouldHandleTestSourceDirectoryType() throws Exception {
      setupSourceDirectories();

      CreateNewFileCommand command =
          createCommand(
              tempDir,
              SupportedLanguage.JAVA,
              SupportedIDE.NONE,
              "com.example",
              "TestClass",
              JavaFileTemplate.CLASS,
              JavaSourceDirectoryType.TEST);

      DataTransferObject<CreateNewFileResponse> result = command.call();
      assertTrue(result.getSucceed());
      assertTrue(result.getData().getFilePath().contains("src/test/java"));
    }
  }

  @Nested
  @DisplayName("IDE Support Tests")
  class IDESupportTests {
    @Test
    @DisplayName("should handle NONE IDE")
    void shouldHandleNoneIDE() throws Exception {
      setupSourceDirectories();

      CreateNewFileCommand command =
          createCommand(
              tempDir,
              SupportedLanguage.JAVA,
              SupportedIDE.NONE,
              "com.example",
              "TestClass",
              JavaFileTemplate.CLASS,
              JavaSourceDirectoryType.MAIN);

      DataTransferObject<CreateNewFileResponse> result = command.call();
      assertTrue(result.getSucceed());
    }

    @Test
    @DisplayName("should handle NEOVIM IDE")
    void shouldHandleNeovimIDE() throws Exception {
      setupSourceDirectories();

      CreateNewFileCommand command =
          createCommand(
              tempDir,
              SupportedLanguage.JAVA,
              SupportedIDE.NEOVIM,
              "com.example",
              "TestClass",
              JavaFileTemplate.CLASS,
              JavaSourceDirectoryType.MAIN);

      DataTransferObject<CreateNewFileResponse> result = command.call();
      assertTrue(result.getSucceed());
    }

    @Test
    @DisplayName("should handle VSCODE IDE")
    void shouldHandleVscodeIDE() throws Exception {
      setupSourceDirectories();

      CreateNewFileCommand command =
          createCommand(
              tempDir,
              SupportedLanguage.JAVA,
              SupportedIDE.VSCODE,
              "com.example",
              "TestClass",
              JavaFileTemplate.CLASS,
              JavaSourceDirectoryType.MAIN);

      DataTransferObject<CreateNewFileResponse> result = command.call();
      assertTrue(result.getSucceed());
    }
  }

  @Nested
  @DisplayName("Error Handling Tests")
  class ErrorHandlingTests {
    @Test
    @DisplayName("should return error when source directory does not exist")
    void shouldReturnErrorWhenSourceDirectoryDoesNotExist() throws Exception {
      CreateNewFileCommand command =
          createCommand(
              tempDir,
              SupportedLanguage.JAVA,
              SupportedIDE.NONE,
              "com.example",
              "TestClass",
              JavaFileTemplate.CLASS,
              JavaSourceDirectoryType.MAIN);

      DataTransferObject<CreateNewFileResponse> result = command.call();
      assertFalse(result.getSucceed());
      assertEquals("Cannot find source directory.", result.getErrorReason());
    }

    @Test
    @DisplayName("should handle null language")
    void shouldHandleNullLanguage() throws Exception {
      CreateNewFileCommand command = new CreateNewFileCommand(javaService);

      setField(command, "cwd", tempDir);
      setField(command, "language", null);
      setField(command, "ide", SupportedIDE.NONE);
      setField(command, "packageName", "com.example");
      setField(command, "fileName", "TestClass");
      setField(command, "fileType", JavaFileTemplate.CLASS);
      setField(command, "sourceDirectoryType", JavaSourceDirectoryType.MAIN);

      DataTransferObject<CreateNewFileResponse> result = command.call();
      assertFalse(result.getSucceed());
      assertEquals("Language not supported.", result.getErrorReason());
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {
    @Test
    @DisplayName("should create file in test directory with all parameters")
    void shouldCreateFileInTestDirectoryWithAllParameters() throws Exception {
      setupSourceDirectories();

      CreateNewFileCommand command =
          createCommand(
              tempDir,
              SupportedLanguage.JAVA,
              SupportedIDE.VSCODE,
              "com.example.test",
              "IntegrationTest",
              JavaFileTemplate.CLASS,
              JavaSourceDirectoryType.TEST);

      DataTransferObject<CreateNewFileResponse> result = command.call();
      assertTrue(result.getSucceed());
      assertTrue(result.getData().getFilePath().endsWith("IntegrationTest.java"));
      assertTrue(result.getData().getFilePath().contains("src/test/java"));
    }

    @Test
    @DisplayName("should handle complex package names")
    void shouldHandleComplexPackageNames() throws Exception {
      setupSourceDirectories();

      CreateNewFileCommand command =
          createCommand(
              tempDir,
              SupportedLanguage.JAVA,
              SupportedIDE.NONE,
              "io.github.syntaxpresso.core.command.test.deep.nested",
              "ComplexClass",
              JavaFileTemplate.CLASS,
              JavaSourceDirectoryType.MAIN);

      DataTransferObject<CreateNewFileResponse> result = command.call();
      assertTrue(result.getSucceed());
      assertTrue(result.getData().getFilePath().endsWith("ComplexClass.java"));
    }

    @Test
    @DisplayName("should return error when file already exists")
    void shouldReturnErrorWhenFileAlreadyExists() throws Exception {
      setupSourceDirectories();

      CreateNewFileCommand command1 =
          createCommand(
              tempDir,
              SupportedLanguage.JAVA,
              SupportedIDE.NONE,
              "com.example",
              "DuplicateClass",
              JavaFileTemplate.CLASS,
              JavaSourceDirectoryType.MAIN);

      DataTransferObject<CreateNewFileResponse> result1 = command1.call();
      assertTrue(result1.getSucceed());

      CreateNewFileCommand command2 =
          createCommand(
              tempDir,
              SupportedLanguage.JAVA,
              SupportedIDE.NONE,
              "com.example",
              "DuplicateClass",
              JavaFileTemplate.CLASS,
              JavaSourceDirectoryType.MAIN);

      DataTransferObject<CreateNewFileResponse> result2 = command2.call();
      assertFalse(result2.getSucceed());
      assertTrue(result2.getErrorReason().contains("File already exists"));
    }
  }
}
