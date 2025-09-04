package io.github.syntaxpresso.core.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.syntaxpresso.core.command.dto.CreateNewFileResponse;
import io.github.syntaxpresso.core.command.extra.JavaFileTemplate;
import io.github.syntaxpresso.core.command.extra.JavaSourceDirectoryType;
import io.github.syntaxpresso.core.common.DataTransferObject;
import io.github.syntaxpresso.core.common.extra.SupportedIDE;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import io.github.syntaxpresso.core.service.java.JavaCommandService;
import java.lang.reflect.Field;
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
  private JavaCommandService mockJavaService;
  @TempDir Path tempDir;

  @BeforeEach
  void setUp() {
    this.mockJavaService = mock(JavaCommandService.class);
  }

  /** Creates a command instance with mock service and sets all required fields. */
  private CreateNewFileCommand createCommand(
      Path cwd,
      SupportedLanguage language,
      SupportedIDE ide,
      String packageName,
      String fileName,
      JavaFileTemplate fileType,
      JavaSourceDirectoryType sourceDirectoryType)
      throws Exception {
    CreateNewFileCommand command = new CreateNewFileCommand(this.mockJavaService);

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
      CreateNewFileCommand command = new CreateNewFileCommand(mockJavaService);
      assertNotNull(command);

      Field serviceField = CreateNewFileCommand.class.getDeclaredField("javaService");
      serviceField.setAccessible(true);
      assertEquals(mockJavaService, serviceField.get(command));
    }

    @Test
    @DisplayName("should have default IDE as NONE")
    void shouldHaveDefaultIDEAsNone() throws Exception {
      CreateNewFileCommand command = new CreateNewFileCommand(mockJavaService);
      Field ideField = CreateNewFileCommand.class.getDeclaredField("ide");
      ideField.setAccessible(true);
      assertEquals(SupportedIDE.NONE, ideField.get(command));
    }

    @Test
    @DisplayName("should have default source directory as MAIN")
    void shouldHaveDefaultSourceDirectoryAsMain() throws Exception {
      CreateNewFileCommand command = new CreateNewFileCommand(mockJavaService);
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
      CreateNewFileResponse mockResponse =
          CreateNewFileResponse.builder().filePath("/path/to/TestClass.java").build();
      when(mockJavaService.createNewFile(any(), anyString(), anyString(), any(), any()))
          .thenReturn(DataTransferObject.success(mockResponse));

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
      assertEquals("/path/to/TestClass.java", result.getData().getFilePath());
    }

    @Test
    @DisplayName("should return error for unsupported language")
    void shouldReturnErrorForUnsupportedLanguage() throws Exception {
      CreateNewFileCommand command = new CreateNewFileCommand(mockJavaService);

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
      CreateNewFileResponse mockResponse =
          CreateNewFileResponse.builder().filePath("/path/to/MyClass.java").build();
      when(mockJavaService.createNewFile(any(), anyString(), anyString(), any(), any()))
          .thenReturn(DataTransferObject.success(mockResponse));

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
    }

    @Test
    @DisplayName("should handle INTERFACE file template")
    void shouldHandleInterfaceFileTemplate() throws Exception {
      CreateNewFileResponse mockResponse =
          CreateNewFileResponse.builder().filePath("/path/to/MyInterface.java").build();
      when(mockJavaService.createNewFile(any(), anyString(), anyString(), any(), any()))
          .thenReturn(DataTransferObject.success(mockResponse));

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
    }

    @Test
    @DisplayName("should handle RECORD file template")
    void shouldHandleRecordFileTemplate() throws Exception {
      CreateNewFileResponse mockResponse =
          CreateNewFileResponse.builder().filePath("/path/to/MyRecord.java").build();
      when(mockJavaService.createNewFile(any(), anyString(), anyString(), any(), any()))
          .thenReturn(DataTransferObject.success(mockResponse));

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
    }

    @Test
    @DisplayName("should handle ENUM file template")
    void shouldHandleEnumFileTemplate() throws Exception {
      CreateNewFileResponse mockResponse =
          CreateNewFileResponse.builder().filePath("/path/to/MyEnum.java").build();
      when(mockJavaService.createNewFile(any(), anyString(), anyString(), any(), any()))
          .thenReturn(DataTransferObject.success(mockResponse));

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
    }

    @Test
    @DisplayName("should handle ANNOTATION file template")
    void shouldHandleAnnotationFileTemplate() throws Exception {
      CreateNewFileResponse mockResponse =
          CreateNewFileResponse.builder().filePath("/path/to/MyAnnotation.java").build();
      when(mockJavaService.createNewFile(any(), anyString(), anyString(), any(), any()))
          .thenReturn(DataTransferObject.success(mockResponse));

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
    }
  }

  @Nested
  @DisplayName("Source Directory Tests")
  class SourceDirectoryTests {
    @Test
    @DisplayName("should handle MAIN source directory type")
    void shouldHandleMainSourceDirectoryType() throws Exception {
      CreateNewFileResponse mockResponse =
          CreateNewFileResponse.builder().filePath("/path/to/MainClass.java").build();
      when(mockJavaService.createNewFile(any(), anyString(), anyString(), any(), any()))
          .thenReturn(DataTransferObject.success(mockResponse));

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
    }

    @Test
    @DisplayName("should handle TEST source directory type")
    void shouldHandleTestSourceDirectoryType() throws Exception {
      CreateNewFileResponse mockResponse =
          CreateNewFileResponse.builder().filePath("/path/to/TestClass.java").build();
      when(mockJavaService.createNewFile(any(), anyString(), anyString(), any(), any()))
          .thenReturn(DataTransferObject.success(mockResponse));

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
    }
  }

  @Nested
  @DisplayName("IDE Support Tests")
  class IDESupportTests {
    @Test
    @DisplayName("should handle NONE IDE")
    void shouldHandleNoneIDE() throws Exception {
      CreateNewFileResponse mockResponse =
          CreateNewFileResponse.builder().filePath("/path/to/TestClass.java").build();
      when(mockJavaService.createNewFile(any(), anyString(), anyString(), any(), any()))
          .thenReturn(DataTransferObject.success(mockResponse));

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
      CreateNewFileResponse mockResponse =
          CreateNewFileResponse.builder().filePath("/path/to/TestClass.java").build();
      when(mockJavaService.createNewFile(any(), anyString(), anyString(), any(), any()))
          .thenReturn(DataTransferObject.success(mockResponse));

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
      CreateNewFileResponse mockResponse =
          CreateNewFileResponse.builder().filePath("/path/to/TestClass.java").build();
      when(mockJavaService.createNewFile(any(), anyString(), anyString(), any(), any()))
          .thenReturn(DataTransferObject.success(mockResponse));

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
    @DisplayName("should propagate JavaCommandService errors")
    void shouldPropagateJavaCommandServiceErrors() throws Exception {
      when(mockJavaService.createNewFile(any(), anyString(), anyString(), any(), any()))
          .thenReturn(DataTransferObject.error("Cannot find source directory."));

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
      CreateNewFileCommand command = new CreateNewFileCommand(mockJavaService);

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
    @DisplayName("should pass all parameters correctly to JavaCommandService")
    void shouldPassAllParametersCorrectlyToJavaCommandService() throws Exception {
      CreateNewFileResponse mockResponse =
          CreateNewFileResponse.builder().filePath("/path/to/IntegrationTest.java").build();
      when(mockJavaService.createNewFile(
              tempDir,
              "com.example.test",
              "IntegrationTest",
              JavaFileTemplate.CLASS,
              JavaSourceDirectoryType.TEST))
          .thenReturn(DataTransferObject.success(mockResponse));

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
      assertEquals("/path/to/IntegrationTest.java", result.getData().getFilePath());
    }

    @Test
    @DisplayName("should handle complex package names")
    void shouldHandleComplexPackageNames() throws Exception {
      CreateNewFileResponse mockResponse =
          CreateNewFileResponse.builder().filePath("/path/to/ComplexClass.java").build();
      when(mockJavaService.createNewFile(any(), anyString(), anyString(), any(), any()))
          .thenReturn(DataTransferObject.success(mockResponse));

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
    }
  }
}

