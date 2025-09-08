package io.github.syntaxpresso.core.command;

import static org.junit.jupiter.api.Assertions.*;

import io.github.syntaxpresso.core.command.dto.CreateNewFileResponse;
import io.github.syntaxpresso.core.command.extra.JavaFileTemplate;
import io.github.syntaxpresso.core.command.extra.JavaSourceDirectoryType;
import io.github.syntaxpresso.core.common.DataTransferObject;
import io.github.syntaxpresso.core.common.extra.SupportedIDE;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import io.github.syntaxpresso.core.service.java.command.CreateNewFileCommandService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Comprehensive tests for CreateNewFileCommand.
 * 
 * Example usage:
 * <pre>
 * CreateNewFileCommand command = new CreateNewFileCommand(service);
 * // Set command options: --cwd /path/to/project --language JAVA --ide NONE --package-name com.example 
 * // --file-name TestClass --file-type CLASS --source-directory MAIN
 * DataTransferObject&lt;CreateNewFileResponse&gt; result = command.call();
 * if (result.getSucceed()) {
 *     CreateNewFileResponse response = result.getData();
 *     System.out.println("Created file: " + response.getFilePath());
 * }
 * </pre>
 */
@DisplayName("CreateNewFileCommand Tests")
class CreateNewFileCommandTest {
  private CreateNewFileCommand createNewFileCommand;
  private TestCreateNewFileCommandService testService;

  @TempDir Path tempDir;

  @BeforeEach
  void setUp() {
    this.testService = new TestCreateNewFileCommandService();
    this.createNewFileCommand = new CreateNewFileCommand(this.testService);
  }

  @Nested
  @DisplayName("Java language support tests")
  class JavaLanguageSupportTests {
    @Test
    @DisplayName("Should successfully create new Java class file")
    void shouldSuccessfullyCreateNewJavaClassFile() throws Exception {
      // Given
      Path projectDir = createTestProjectStructure();
      CreateNewFileResponse expectedResponse = CreateNewFileResponse.builder()
          .filePath(projectDir.resolve("src/main/java/com/example/TestClass.java").toString())
          .build();
      testService.setSuccessResponse(expectedResponse);
      setupCreateNewFileCommand(projectDir, SupportedLanguage.JAVA, SupportedIDE.NONE, 
          "com.example", "TestClass", JavaFileTemplate.CLASS, JavaSourceDirectoryType.MAIN);

      // When
      DataTransferObject<CreateNewFileResponse> result = createNewFileCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertNotNull(result.getData());
      assertTrue(result.getData().getFilePath().contains("TestClass.java"));
      assertTrue(testService.wasServiceCalled());
      assertEquals("com.example", testService.getLastCalledPackageName());
      assertEquals("TestClass", testService.getLastCalledFileName());
      assertEquals(JavaFileTemplate.CLASS, testService.getLastCalledFileType());
      assertEquals(JavaSourceDirectoryType.MAIN, testService.getLastCalledSourceDirectory());
    }

    @Test
    @DisplayName("Should successfully create new Java interface file")
    void shouldSuccessfullyCreateNewJavaInterfaceFile() throws Exception {
      // Given
      Path projectDir = createTestProjectStructure();
      CreateNewFileResponse expectedResponse = CreateNewFileResponse.builder()
          .filePath(projectDir.resolve("src/main/java/com/example/TestInterface.java").toString())
          .build();
      testService.setSuccessResponse(expectedResponse);
      setupCreateNewFileCommand(projectDir, SupportedLanguage.JAVA, SupportedIDE.VSCODE, 
          "com.example", "TestInterface", JavaFileTemplate.INTERFACE, JavaSourceDirectoryType.MAIN);

      // When
      DataTransferObject<CreateNewFileResponse> result = createNewFileCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertNotNull(result.getData());
      assertTrue(result.getData().getFilePath().contains("TestInterface.java"));
      assertTrue(testService.wasServiceCalled());
      assertEquals(JavaFileTemplate.INTERFACE, testService.getLastCalledFileType());
    }

    @Test
    @DisplayName("Should successfully create new Java enum file")
    void shouldSuccessfullyCreateNewJavaEnumFile() throws Exception {
      // Given
      Path projectDir = createTestProjectStructure();
      CreateNewFileResponse expectedResponse = CreateNewFileResponse.builder()
          .filePath(projectDir.resolve("src/main/java/com/example/TestEnum.java").toString())
          .build();
      testService.setSuccessResponse(expectedResponse);
      setupCreateNewFileCommand(projectDir, SupportedLanguage.JAVA, SupportedIDE.NEOVIM, 
          "com.example", "TestEnum", JavaFileTemplate.ENUM, JavaSourceDirectoryType.MAIN);

      // When
      DataTransferObject<CreateNewFileResponse> result = createNewFileCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertNotNull(result.getData());
      assertTrue(result.getData().getFilePath().contains("TestEnum.java"));
      assertTrue(testService.wasServiceCalled());
      assertEquals(JavaFileTemplate.ENUM, testService.getLastCalledFileType());
    }

    @Test
    @DisplayName("Should successfully create new Java record file")
    void shouldSuccessfullyCreateNewJavaRecordFile() throws Exception {
      // Given
      Path projectDir = createTestProjectStructure();
      CreateNewFileResponse expectedResponse = CreateNewFileResponse.builder()
          .filePath(projectDir.resolve("src/main/java/com/example/TestRecord.java").toString())
          .build();
      testService.setSuccessResponse(expectedResponse);
      setupCreateNewFileCommand(projectDir, SupportedLanguage.JAVA, SupportedIDE.NONE, 
          "com.example", "TestRecord", JavaFileTemplate.RECORD, JavaSourceDirectoryType.MAIN);

      // When
      DataTransferObject<CreateNewFileResponse> result = createNewFileCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertNotNull(result.getData());
      assertTrue(result.getData().getFilePath().contains("TestRecord.java"));
      assertTrue(testService.wasServiceCalled());
      assertEquals(JavaFileTemplate.RECORD, testService.getLastCalledFileType());
    }

    @Test
    @DisplayName("Should successfully create new Java annotation file")
    void shouldSuccessfullyCreateNewJavaAnnotationFile() throws Exception {
      // Given
      Path projectDir = createTestProjectStructure();
      CreateNewFileResponse expectedResponse = CreateNewFileResponse.builder()
          .filePath(projectDir.resolve("src/main/java/com/example/TestAnnotation.java").toString())
          .build();
      testService.setSuccessResponse(expectedResponse);
      setupCreateNewFileCommand(projectDir, SupportedLanguage.JAVA, SupportedIDE.NONE, 
          "com.example", "TestAnnotation", JavaFileTemplate.ANNOTATION, JavaSourceDirectoryType.MAIN);

      // When
      DataTransferObject<CreateNewFileResponse> result = createNewFileCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertNotNull(result.getData());
      assertTrue(result.getData().getFilePath().contains("TestAnnotation.java"));
      assertTrue(testService.wasServiceCalled());
      assertEquals(JavaFileTemplate.ANNOTATION, testService.getLastCalledFileType());
    }

    @Test
    @DisplayName("Should handle service error responses for Java")
    void shouldHandleServiceErrorResponsesForJava() throws Exception {
      // Given
      Path projectDir = createTestProjectStructure();
      testService.setErrorResponse("File already exists: " + projectDir.resolve("TestClass.java"));
      setupCreateNewFileCommand(projectDir, SupportedLanguage.JAVA, SupportedIDE.NONE, 
          "com.example", "TestClass", JavaFileTemplate.CLASS, JavaSourceDirectoryType.MAIN);

      // When
      DataTransferObject<CreateNewFileResponse> result = createNewFileCommand.call();

      // Then
      assertFalse(result.getSucceed());
      assertNull(result.getData());
      assertTrue(result.getErrorReason().contains("File already exists"));
      assertTrue(testService.wasServiceCalled());
    }
  }

  @Nested
  @DisplayName("Language validation tests")
  class LanguageValidationTests {
    @Test
    @DisplayName("Should return error for non-Java language")
    void shouldReturnErrorForNonJavaLanguage() throws Exception {
      // Given
      Path projectDir = createTestProjectStructure();
      setupCreateNewFileCommand(projectDir, null, SupportedIDE.NONE, 
          "com.example", "TestClass", JavaFileTemplate.CLASS, JavaSourceDirectoryType.MAIN);

      // When
      DataTransferObject<CreateNewFileResponse> result = createNewFileCommand.call();

      // Then
      assertFalse(result.getSucceed());
      assertNull(result.getData());
      assertEquals("Language not supported.", result.getErrorReason());
      assertFalse(testService.wasServiceCalled());
    }

    @Test
    @DisplayName("Should process Java language successfully")
    void shouldProcessJavaLanguageSuccessfully() throws Exception {
      // Given
      Path projectDir = createTestProjectStructure();
      CreateNewFileResponse response = CreateNewFileResponse.builder()
          .filePath(projectDir.resolve("src/main/java/com/test/TestFile.java").toString())
          .build();
      testService.setSuccessResponse(response);
      setupCreateNewFileCommand(projectDir, SupportedLanguage.JAVA, SupportedIDE.NONE, 
          "com.test", "TestFile", JavaFileTemplate.CLASS, JavaSourceDirectoryType.MAIN);

      // When
      DataTransferObject<CreateNewFileResponse> result = createNewFileCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertTrue(testService.wasServiceCalled());
      assertTrue(result.getData().getFilePath().contains("TestFile.java"));
    }
  }

  @Nested
  @DisplayName("Source directory type tests")
  class SourceDirectoryTypeTests {
    @Test
    @DisplayName("Should create file in main source directory")
    void shouldCreateFileInMainSourceDirectory() throws Exception {
      // Given
      Path projectDir = createTestProjectStructure();
      CreateNewFileResponse response = CreateNewFileResponse.builder()
          .filePath(projectDir.resolve("src/main/java/com/example/MainClass.java").toString())
          .build();
      testService.setSuccessResponse(response);
      setupCreateNewFileCommand(projectDir, SupportedLanguage.JAVA, SupportedIDE.NONE, 
          "com.example", "MainClass", JavaFileTemplate.CLASS, JavaSourceDirectoryType.MAIN);

      // When
      DataTransferObject<CreateNewFileResponse> result = createNewFileCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertNotNull(result.getData());
      assertTrue(result.getData().getFilePath().contains("src/main/java"));
      assertTrue(testService.wasServiceCalled());
      assertEquals(JavaSourceDirectoryType.MAIN, testService.getLastCalledSourceDirectory());
    }

    @Test
    @DisplayName("Should create file in test source directory")
    void shouldCreateFileInTestSourceDirectory() throws Exception {
      // Given
      Path projectDir = createTestProjectStructure();
      CreateNewFileResponse response = CreateNewFileResponse.builder()
          .filePath(projectDir.resolve("src/test/java/com/example/TestClass.java").toString())
          .build();
      testService.setSuccessResponse(response);
      setupCreateNewFileCommand(projectDir, SupportedLanguage.JAVA, SupportedIDE.NONE, 
          "com.example", "TestClass", JavaFileTemplate.CLASS, JavaSourceDirectoryType.TEST);

      // When
      DataTransferObject<CreateNewFileResponse> result = createNewFileCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertNotNull(result.getData());
      assertTrue(result.getData().getFilePath().contains("src/test/java"));
      assertTrue(testService.wasServiceCalled());
      assertEquals(JavaSourceDirectoryType.TEST, testService.getLastCalledSourceDirectory());
    }

    @Test
    @DisplayName("Should default to main source directory when not specified")
    void shouldDefaultToMainSourceDirectoryWhenNotSpecified() throws Exception {
      // Given
      Path projectDir = createTestProjectStructure();
      CreateNewFileResponse response = CreateNewFileResponse.builder()
          .filePath(projectDir.resolve("src/main/java/com/example/DefaultClass.java").toString())
          .build();
      testService.setSuccessResponse(response);
      setupCreateNewFileCommand(projectDir, SupportedLanguage.JAVA, SupportedIDE.NONE, 
          "com.example", "DefaultClass", JavaFileTemplate.CLASS, null);

      // When
      DataTransferObject<CreateNewFileResponse> result = createNewFileCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertTrue(testService.wasServiceCalled());
      // The command should use the default MAIN directory when null is passed
      assertTrue(result.getData().getFilePath().contains("src/main/java"));
    }
  }

  @Nested
  @DisplayName("IDE-specific behavior tests")
  class IDESpecificBehaviorTests {
    @Test
    @DisplayName("Should handle NONE IDE correctly")
    void shouldHandleNoneIDECorrectly() throws Exception {
      // Given
      Path projectDir = createTestProjectStructure();
      CreateNewFileResponse response = CreateNewFileResponse.builder()
          .filePath(projectDir.resolve("src/main/java/com/example/NoneIDETest.java").toString())
          .build();
      testService.setSuccessResponse(response);
      setupCreateNewFileCommand(projectDir, SupportedLanguage.JAVA, SupportedIDE.NONE, 
          "com.example", "NoneIDETest", JavaFileTemplate.CLASS, JavaSourceDirectoryType.MAIN);

      // When
      DataTransferObject<CreateNewFileResponse> result = createNewFileCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertNotNull(result.getData());
      assertTrue(result.getData().getFilePath().contains("NoneIDETest.java"));
      assertTrue(testService.wasServiceCalled());
    }

    @Test
    @DisplayName("Should handle VSCODE IDE correctly")
    void shouldHandleVSCodeIDECorrectly() throws Exception {
      // Given
      Path projectDir = createTestProjectStructure();
      CreateNewFileResponse response = CreateNewFileResponse.builder()
          .filePath(projectDir.resolve("src/main/java/com/example/VSCodeTest.java").toString())
          .build();
      testService.setSuccessResponse(response);
      setupCreateNewFileCommand(projectDir, SupportedLanguage.JAVA, SupportedIDE.VSCODE, 
          "com.example", "VSCodeTest", JavaFileTemplate.CLASS, JavaSourceDirectoryType.MAIN);

      // When
      DataTransferObject<CreateNewFileResponse> result = createNewFileCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertNotNull(result.getData());
      assertTrue(result.getData().getFilePath().contains("VSCodeTest.java"));
      assertTrue(testService.wasServiceCalled());
    }

    @Test
    @DisplayName("Should handle NEOVIM IDE correctly")
    void shouldHandleNeovimIDECorrectly() throws Exception {
      // Given
      Path projectDir = createTestProjectStructure();
      CreateNewFileResponse response = CreateNewFileResponse.builder()
          .filePath(projectDir.resolve("src/main/java/com/example/NeovimTest.java").toString())
          .build();
      testService.setSuccessResponse(response);
      setupCreateNewFileCommand(projectDir, SupportedLanguage.JAVA, SupportedIDE.NEOVIM, 
          "com.example", "NeovimTest", JavaFileTemplate.INTERFACE, JavaSourceDirectoryType.MAIN);

      // When
      DataTransferObject<CreateNewFileResponse> result = createNewFileCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertNotNull(result.getData());
      assertTrue(result.getData().getFilePath().contains("NeovimTest.java"));
      assertTrue(testService.wasServiceCalled());
    }
  }

  @Nested
  @DisplayName("Command configuration tests")
  class CommandConfigurationTests {
    @Test
    @DisplayName("Should have correct picocli annotations")
    void shouldHaveCorrectPicocliAnnotations() {
      // Verify the class implements Callable
      assertTrue(Callable.class.isAssignableFrom(CreateNewFileCommand.class));

      // Verify command annotation exists
      assertTrue(CreateNewFileCommand.class.isAnnotationPresent(picocli.CommandLine.Command.class));

      // Verify command properties
      picocli.CommandLine.Command commandAnnotation =
          CreateNewFileCommand.class.getAnnotation(picocli.CommandLine.Command.class);
      assertEquals("create-new-file", commandAnnotation.name());
      assertEquals("Create a new Java file", commandAnnotation.description()[0]);
    }

    @Test
    @DisplayName("Should handle all required options")
    void shouldHandleAllRequiredOptions() throws Exception {
      // Given
      Path projectDir = createTestProjectStructure();
      CreateNewFileResponse response = CreateNewFileResponse.builder()
          .filePath(projectDir.resolve("src/main/java/com/example/OptionsTest.java").toString())
          .build();
      testService.setSuccessResponse(response);
      setupCreateNewFileCommand(projectDir, SupportedLanguage.JAVA, SupportedIDE.NONE, 
          "com.example", "OptionsTest", JavaFileTemplate.CLASS, JavaSourceDirectoryType.MAIN);

      // When
      DataTransferObject<CreateNewFileResponse> result = createNewFileCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertEquals(projectDir, testService.getLastCalledCwd());
      assertEquals("com.example", testService.getLastCalledPackageName());
      assertEquals("OptionsTest", testService.getLastCalledFileName());
      assertEquals(JavaFileTemplate.CLASS, testService.getLastCalledFileType());
      assertEquals(JavaSourceDirectoryType.MAIN, testService.getLastCalledSourceDirectory());
      assertTrue(testService.wasServiceCalled());
    }

    @Test
    @DisplayName("Should return DataTransferObject with correct generic type")
    void shouldReturnDataTransferObjectWithCorrectGenericType() throws Exception {
      // Given
      Path projectDir = createTestProjectStructure();
      CreateNewFileResponse response = CreateNewFileResponse.builder()
          .filePath(projectDir.resolve("src/main/java/com/example/GenericTest.java").toString())
          .build();
      testService.setSuccessResponse(response);
      setupCreateNewFileCommand(projectDir, SupportedLanguage.JAVA, SupportedIDE.NONE, 
          "com.example", "GenericTest", JavaFileTemplate.CLASS, JavaSourceDirectoryType.MAIN);

      // When
      DataTransferObject<CreateNewFileResponse> result = createNewFileCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertNotNull(result.getData());
      assertTrue(result.getData() instanceof CreateNewFileResponse);
      assertEquals(CreateNewFileResponse.class, result.getData().getClass());
    }
  }

  @Nested
  @DisplayName("Edge case tests")
  class EdgeCaseTests {
    @Test
    @DisplayName("Should handle service validation errors")
    void shouldHandleServiceValidationErrors() throws Exception {
      // Given
      Path projectDir = createTestProjectStructure();
      testService.setErrorResponse("Package name invalid.");
      setupCreateNewFileCommand(projectDir, SupportedLanguage.JAVA, SupportedIDE.NONE, 
          "", "TestClass", JavaFileTemplate.CLASS, JavaSourceDirectoryType.MAIN);

      // When
      DataTransferObject<CreateNewFileResponse> result = createNewFileCommand.call();

      // Then
      assertFalse(result.getSucceed());
      assertEquals("Package name invalid.", result.getErrorReason());
      assertTrue(testService.wasServiceCalled());
    }

    @Test
    @DisplayName("Should handle file already exists error")
    void shouldHandleFileAlreadyExistsError() throws Exception {
      // Given
      Path projectDir = createTestProjectStructure();
      testService.setErrorResponse("File already exists: " + projectDir.resolve("TestClass.java"));
      setupCreateNewFileCommand(projectDir, SupportedLanguage.JAVA, SupportedIDE.NONE, 
          "com.example", "TestClass", JavaFileTemplate.CLASS, JavaSourceDirectoryType.MAIN);

      // When
      DataTransferObject<CreateNewFileResponse> result = createNewFileCommand.call();

      // Then
      assertFalse(result.getSucceed());
      assertTrue(result.getErrorReason().contains("File already exists"));
      assertTrue(testService.wasServiceCalled());
    }

    @Test
    @DisplayName("Should handle source directory not found error")
    void shouldHandleSourceDirectoryNotFoundError() throws Exception {
      // Given
      Path projectDir = createTestProjectStructure();
      testService.setErrorResponse("Cannot find source directory.");
      setupCreateNewFileCommand(projectDir, SupportedLanguage.JAVA, SupportedIDE.NONE, 
          "com.example", "TestClass", JavaFileTemplate.CLASS, JavaSourceDirectoryType.MAIN);

      // When
      DataTransferObject<CreateNewFileResponse> result = createNewFileCommand.call();

      // Then
      assertFalse(result.getSucceed());
      assertEquals("Cannot find source directory.", result.getErrorReason());
      assertTrue(testService.wasServiceCalled());
    }

    @Test
    @DisplayName("Should handle service returning null response")
    void shouldHandleServiceReturningNullResponse() throws Exception {
      // Given
      Path projectDir = createTestProjectStructure();
      testService.setNullResponse();
      setupCreateNewFileCommand(projectDir, SupportedLanguage.JAVA, SupportedIDE.NONE, 
          "com.example", "TestClass", JavaFileTemplate.CLASS, JavaSourceDirectoryType.MAIN);

      // When
      DataTransferObject<CreateNewFileResponse> result = createNewFileCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertNull(result.getData());
      assertTrue(testService.wasServiceCalled());
    }

    @Test
    @DisplayName("Should handle all Java file template types")
    void shouldHandleAllJavaFileTemplateTypes() throws Exception {
      // Given
      Path projectDir = createTestProjectStructure();
      
      // Test each file template type
      for (JavaFileTemplate fileTemplate : JavaFileTemplate.values()) {
        CreateNewFileResponse response = CreateNewFileResponse.builder()
            .filePath(projectDir.resolve("src/main/java/com/example/" + fileTemplate.name() + "Test.java").toString())
            .build();
        testService.setSuccessResponse(response);
        setupCreateNewFileCommand(projectDir, SupportedLanguage.JAVA, SupportedIDE.NONE, 
            "com.example", fileTemplate.name() + "Test", fileTemplate, JavaSourceDirectoryType.MAIN);

        // When
        DataTransferObject<CreateNewFileResponse> result = createNewFileCommand.call();

        // Then
        assertTrue(result.getSucceed(), "Failed for file template: " + fileTemplate);
        assertNotNull(result.getData());
        assertEquals(fileTemplate, testService.getLastCalledFileType());
      }
    }

    @Test
    @DisplayName("Should handle complex package names")
    void shouldHandleComplexPackageNames() throws Exception {
      // Given
      Path projectDir = createTestProjectStructure();
      CreateNewFileResponse response = CreateNewFileResponse.builder()
          .filePath(projectDir.resolve("src/main/java/com/example/deep/nested/package/ComplexClass.java").toString())
          .build();
      testService.setSuccessResponse(response);
      setupCreateNewFileCommand(projectDir, SupportedLanguage.JAVA, SupportedIDE.NONE, 
          "com.example.deep.nested.package", "ComplexClass", JavaFileTemplate.CLASS, JavaSourceDirectoryType.MAIN);

      // When
      DataTransferObject<CreateNewFileResponse> result = createNewFileCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertNotNull(result.getData());
      assertEquals("com.example.deep.nested.package", testService.getLastCalledPackageName());
      assertTrue(testService.wasServiceCalled());
    }

    @Test
    @DisplayName("Should handle file names with extensions")
    void shouldHandleFileNamesWithExtensions() throws Exception {
      // Given
      Path projectDir = createTestProjectStructure();
      CreateNewFileResponse response = CreateNewFileResponse.builder()
          .filePath(projectDir.resolve("src/main/java/com/example/TestClass.java").toString())
          .build();
      testService.setSuccessResponse(response);
      setupCreateNewFileCommand(projectDir, SupportedLanguage.JAVA, SupportedIDE.NONE, 
          "com.example", "TestClass.java", JavaFileTemplate.CLASS, JavaSourceDirectoryType.MAIN);

      // When
      DataTransferObject<CreateNewFileResponse> result = createNewFileCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertNotNull(result.getData());
      assertEquals("TestClass.java", testService.getLastCalledFileName());
      assertTrue(testService.wasServiceCalled());
    }
  }

  private Path createTestProjectStructure() throws IOException {
    Path projectDir = tempDir.resolve("test-project");
    Files.createDirectories(projectDir);
    
    // Create standard Maven directory structure
    Files.createDirectories(projectDir.resolve("src/main/java"));
    Files.createDirectories(projectDir.resolve("src/test/java"));
    
    return projectDir;
  }

  private void setupCreateNewFileCommand(Path cwd, SupportedLanguage language, SupportedIDE ide, 
      String packageName, String fileName, JavaFileTemplate fileType, JavaSourceDirectoryType sourceDirectoryType) {
    setField(createNewFileCommand, "cwd", cwd);
    setField(createNewFileCommand, "language", language);
    setField(createNewFileCommand, "ide", ide);
    setField(createNewFileCommand, "packageName", packageName);
    setField(createNewFileCommand, "fileName", fileName);
    setField(createNewFileCommand, "fileType", fileType);
    setField(createNewFileCommand, "sourceDirectoryType", sourceDirectoryType != null ? sourceDirectoryType : JavaSourceDirectoryType.MAIN);
  }

  private void setField(Object target, String fieldName, Object value) {
    try {
      var field = target.getClass().getDeclaredField(fieldName);
      field.setAccessible(true);
      field.set(target, value);
    } catch (Exception e) {
      throw new RuntimeException("Failed to set field " + fieldName, e);
    }
  }

  /**
   * Test implementation of CreateNewFileCommandService that captures method calls and allows setting
   * predefined responses.
   */
  private static class TestCreateNewFileCommandService extends CreateNewFileCommandService {
    private boolean serviceCalled = false;
    private Path lastCalledCwd;
    private String lastCalledPackageName;
    private String lastCalledFileName;
    private JavaFileTemplate lastCalledFileType;
    private JavaSourceDirectoryType lastCalledSourceDirectory;
    private DataTransferObject<CreateNewFileResponse> responseToReturn;

    public TestCreateNewFileCommandService() {
      super(null, null); // Services not needed for testing
    }

    @Override
    public DataTransferObject<CreateNewFileResponse> run(
        Path cwd, String packageName, String fileName, JavaFileTemplate fileType, JavaSourceDirectoryType sourceDirectoryType) {
      this.serviceCalled = true;
      this.lastCalledCwd = cwd;
      this.lastCalledPackageName = packageName;
      this.lastCalledFileName = fileName;
      this.lastCalledFileType = fileType;
      this.lastCalledSourceDirectory = sourceDirectoryType;
      return this.responseToReturn;
    }

    public void setSuccessResponse(CreateNewFileResponse response) {
      this.responseToReturn = DataTransferObject.success(response);
    }

    public void setErrorResponse(String errorMessage) {
      this.responseToReturn = DataTransferObject.error(errorMessage);
    }

    public void setNullResponse() {
      this.responseToReturn = DataTransferObject.success();
    }

    public boolean wasServiceCalled() {
      return this.serviceCalled;
    }

    public Path getLastCalledCwd() {
      return this.lastCalledCwd;
    }

    public String getLastCalledPackageName() {
      return this.lastCalledPackageName;
    }

    public String getLastCalledFileName() {
      return this.lastCalledFileName;
    }

    public JavaFileTemplate getLastCalledFileType() {
      return this.lastCalledFileType;
    }

    public JavaSourceDirectoryType getLastCalledSourceDirectory() {
      return this.lastCalledSourceDirectory;
    }
  }
}