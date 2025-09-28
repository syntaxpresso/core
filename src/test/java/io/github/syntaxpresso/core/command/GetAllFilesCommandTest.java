package io.github.syntaxpresso.core.command;

import static org.junit.jupiter.api.Assertions.*;

import io.github.syntaxpresso.core.command.dto.FileResponse;
import io.github.syntaxpresso.core.command.dto.GetAllFilesResponse;
import io.github.syntaxpresso.core.command.extra.JavaFileTemplate;
import io.github.syntaxpresso.core.common.DataTransferObject;
import io.github.syntaxpresso.core.common.extra.SupportedIDE;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import io.github.syntaxpresso.core.service.java.command.GetAllFilesCommandService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Comprehensive tests for GetAllFilesCommand.
 * 
 * Example usage:
 * <pre>
 * GetAllFilesCommand command = new GetAllFilesCommand(service);
 * // Set command options: --cwd /path/to/project --file-type ENUM --language JAVA --ide NONE
 * DataTransferObject&lt;GetAllFilesResponse&gt; result = command.call();
 * if (result.getSucceed()) {
 *     GetAllFilesResponse response = result.getData();
 *     System.out.println("Found " + response.getResponse().size() + " files");
 * }
 * </pre>
 */
@DisplayName("GetAllFilesCommand Tests")
class GetAllFilesCommandTest {
  private GetAllFilesCommand getAllFilesCommand;
  private TestGetAllFilesCommandService testService;

  @TempDir Path tempDir;

  @BeforeEach
  void setUp() {
    this.testService = new TestGetAllFilesCommandService();
    this.getAllFilesCommand = new GetAllFilesCommand(this.testService);
  }

  @Nested
  @DisplayName("Java language support tests")
  class JavaLanguageSupportTests {
    @Test
    @DisplayName("Should successfully retrieve enum files")
    void shouldSuccessfullyRetrieveEnumFiles() throws Exception {
      // Given
      Path projectDir = createTestProjectStructure();
      GetAllFilesResponse expectedResponse = createSuccessResponse("StateEnum", "com.example.enums", "/path/to/StateEnum.java");
      testService.setSuccessResponse(expectedResponse);
      setupGetAllFilesCommand(projectDir, JavaFileTemplate.ENUM, SupportedLanguage.JAVA, SupportedIDE.NONE);

      // When
      DataTransferObject<GetAllFilesResponse> result = getAllFilesCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertNotNull(result.getData());
      assertEquals(1, result.getData().getResponse().size());
      assertEquals("StateEnum", result.getData().getResponse().get(0).getType());
      assertEquals("com.example.enums", result.getData().getResponse().get(0).getPackagePath());
      assertTrue(testService.wasServiceCalled());
      assertEquals(JavaFileTemplate.ENUM, testService.getLastCalledFileType());
    }

    @Test
    @DisplayName("Should successfully retrieve class files")
    void shouldSuccessfullyRetrieveClassFiles() throws Exception {
      // Given
      Path projectDir = createTestProjectStructure();
      GetAllFilesResponse expectedResponse = createSuccessResponse("UserService", "com.example.service", "/path/to/UserService.java");
      testService.setSuccessResponse(expectedResponse);
      setupGetAllFilesCommand(projectDir, JavaFileTemplate.CLASS, SupportedLanguage.JAVA, SupportedIDE.VSCODE);

      // When
      DataTransferObject<GetAllFilesResponse> result = getAllFilesCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertNotNull(result.getData());
      assertEquals(1, result.getData().getResponse().size());
      assertEquals("UserService", result.getData().getResponse().get(0).getType());
      assertTrue(testService.wasServiceCalled());
      assertEquals(JavaFileTemplate.CLASS, testService.getLastCalledFileType());
    }

    @Test
    @DisplayName("Should successfully retrieve interface files")
    void shouldSuccessfullyRetrieveInterfaceFiles() throws Exception {
      // Given
      Path projectDir = createTestProjectStructure();
      GetAllFilesResponse expectedResponse = createSuccessResponse("UserRepository", "com.example.repository", "/path/to/UserRepository.java");
      testService.setSuccessResponse(expectedResponse);
      setupGetAllFilesCommand(projectDir, JavaFileTemplate.INTERFACE, SupportedLanguage.JAVA, SupportedIDE.NEOVIM);

      // When
      DataTransferObject<GetAllFilesResponse> result = getAllFilesCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertNotNull(result.getData());
      assertEquals(1, result.getData().getResponse().size());
      assertEquals("UserRepository", result.getData().getResponse().get(0).getType());
      assertTrue(testService.wasServiceCalled());
      assertEquals(JavaFileTemplate.INTERFACE, testService.getLastCalledFileType());
    }

    @Test
    @DisplayName("Should successfully retrieve record files")
    void shouldSuccessfullyRetrieveRecordFiles() throws Exception {
      // Given
      Path projectDir = createTestProjectStructure();
      GetAllFilesResponse expectedResponse = createSuccessResponse("UserDto", "com.example.dto", "/path/to/UserDto.java");
      testService.setSuccessResponse(expectedResponse);
      setupGetAllFilesCommand(projectDir, JavaFileTemplate.RECORD, SupportedLanguage.JAVA, SupportedIDE.NONE);

      // When
      DataTransferObject<GetAllFilesResponse> result = getAllFilesCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertNotNull(result.getData());
      assertEquals(1, result.getData().getResponse().size());
      assertEquals("UserDto", result.getData().getResponse().get(0).getType());
      assertTrue(testService.wasServiceCalled());
      assertEquals(JavaFileTemplate.RECORD, testService.getLastCalledFileType());
    }

    @Test
    @DisplayName("Should successfully retrieve annotation files")
    void shouldSuccessfullyRetrieveAnnotationFiles() throws Exception {
      // Given
      Path projectDir = createTestProjectStructure();
      GetAllFilesResponse expectedResponse = createSuccessResponse("CustomAnnotation", "com.example.annotations", "/path/to/CustomAnnotation.java");
      testService.setSuccessResponse(expectedResponse);
      setupGetAllFilesCommand(projectDir, JavaFileTemplate.ANNOTATION, SupportedLanguage.JAVA, SupportedIDE.NONE);

      // When
      DataTransferObject<GetAllFilesResponse> result = getAllFilesCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertNotNull(result.getData());
      assertEquals(1, result.getData().getResponse().size());
      assertEquals("CustomAnnotation", result.getData().getResponse().get(0).getType());
      assertTrue(testService.wasServiceCalled());
      assertEquals(JavaFileTemplate.ANNOTATION, testService.getLastCalledFileType());
    }

    @Test
    @DisplayName("Should return empty list when no files found")
    void shouldReturnEmptyListWhenNoFilesFound() throws Exception {
      // Given
      Path projectDir = createTestProjectStructure();
      GetAllFilesResponse emptyResponse = new GetAllFilesResponse();
      testService.setSuccessResponse(emptyResponse);
      setupGetAllFilesCommand(projectDir, JavaFileTemplate.ENUM, SupportedLanguage.JAVA, SupportedIDE.NONE);

      // When
      DataTransferObject<GetAllFilesResponse> result = getAllFilesCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertNotNull(result.getData());
      assertEquals(0, result.getData().getResponse().size());
      assertTrue(testService.wasServiceCalled());
    }

    @Test
    @DisplayName("Should handle multiple files of same type")
    void shouldHandleMultipleFilesOfSameType() throws Exception {
      // Given
      Path projectDir = createTestProjectStructure();
      GetAllFilesResponse multipleFilesResponse = createMultipleFilesResponse();
      testService.setSuccessResponse(multipleFilesResponse);
      setupGetAllFilesCommand(projectDir, JavaFileTemplate.ENUM, SupportedLanguage.JAVA, SupportedIDE.NONE);

      // When
      DataTransferObject<GetAllFilesResponse> result = getAllFilesCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertNotNull(result.getData());
      assertEquals(3, result.getData().getResponse().size());
      assertTrue(testService.wasServiceCalled());
    }

    @Test
    @DisplayName("Should handle service error responses for Java")
    void shouldHandleServiceErrorResponsesForJava() throws Exception {
      // Given
      Path projectDir = createTestProjectStructure();
      testService.setErrorResponse("Directory not found: " + projectDir);
      setupGetAllFilesCommand(projectDir, JavaFileTemplate.ENUM, SupportedLanguage.JAVA, SupportedIDE.NONE);

      // When
      DataTransferObject<GetAllFilesResponse> result = getAllFilesCommand.call();

      // Then
      assertFalse(result.getSucceed());
      assertNull(result.getData());
      assertTrue(result.getErrorReason().contains("Directory not found"));
      assertTrue(testService.wasServiceCalled());
    }
  }

  @Nested
  @DisplayName("Language validation tests")
  class LanguageValidationTests {
    @Test
    @DisplayName("Should handle null language gracefully")
    void shouldHandleNullLanguageGracefully() throws Exception {
      // Given
      Path projectDir = createTestProjectStructure();
      setupGetAllFilesCommand(projectDir, JavaFileTemplate.ENUM, SupportedLanguage.JAVA, SupportedIDE.NONE);
      // Manually override the language field to null to test null handling
      setField(getAllFilesCommand, "language", null);

      // When
      DataTransferObject<GetAllFilesResponse> result = getAllFilesCommand.call();

      // Then - should handle null gracefully and return error
      assertFalse(result.getSucceed());
      assertNull(result.getData());
      assertEquals("Language not supported.", result.getErrorReason());
      assertFalse(testService.wasServiceCalled());
    }

    @Test
    @DisplayName("Should process Java language successfully with default settings")
    void shouldProcessJavaLanguageSuccessfullyWithDefaultSettings() throws Exception {
      // Given
      Path projectDir = createTestProjectStructure();
      GetAllFilesResponse response = createSuccessResponse("TestFile", "com.test", "/path/to/TestFile.java");
      testService.setSuccessResponse(response);
      setupGetAllFilesCommand(projectDir, JavaFileTemplate.CLASS, SupportedLanguage.JAVA, SupportedIDE.NONE);

      // When
      DataTransferObject<GetAllFilesResponse> result = getAllFilesCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertTrue(testService.wasServiceCalled());
      assertEquals("TestFile", result.getData().getResponse().get(0).getType());
    }
  }

  @Nested
  @DisplayName("File type tests")
  class FileTypeTests {
    @Test
    @DisplayName("Should handle all Java file template types")
    void shouldHandleAllJavaFileTemplateTypes() throws Exception {
      // Given
      Path projectDir = createTestProjectStructure();
      
      // Test each file template type
      for (JavaFileTemplate fileTemplate : JavaFileTemplate.values()) {
        GetAllFilesResponse response = createSuccessResponse(
            fileTemplate.name() + "Test", 
            "com.example." + fileTemplate.name().toLowerCase(), 
            "/path/to/" + fileTemplate.name() + "Test.java"
        );
        testService.setSuccessResponse(response);
        setupGetAllFilesCommand(projectDir, fileTemplate, SupportedLanguage.JAVA, SupportedIDE.NONE);

        // When
        DataTransferObject<GetAllFilesResponse> result = getAllFilesCommand.call();

        // Then
        assertTrue(result.getSucceed(), "Failed for file template: " + fileTemplate);
        assertNotNull(result.getData());
        assertEquals(fileTemplate, testService.getLastCalledFileType());
        assertEquals(1, result.getData().getResponse().size());
      }
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
      GetAllFilesResponse response = createSuccessResponse("NoneIDETest", "com.example", "/path/to/NoneIDETest.java");
      testService.setSuccessResponse(response);
      setupGetAllFilesCommand(projectDir, JavaFileTemplate.CLASS, SupportedLanguage.JAVA, SupportedIDE.NONE);

      // When
      DataTransferObject<GetAllFilesResponse> result = getAllFilesCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertNotNull(result.getData());
      assertEquals("NoneIDETest", result.getData().getResponse().get(0).getType());
      assertTrue(testService.wasServiceCalled());
    }

    @Test
    @DisplayName("Should handle VSCODE IDE correctly")
    void shouldHandleVSCodeIDECorrectly() throws Exception {
      // Given
      Path projectDir = createTestProjectStructure();
      GetAllFilesResponse response = createSuccessResponse("VSCodeTest", "com.example", "/path/to/VSCodeTest.java");
      testService.setSuccessResponse(response);
      setupGetAllFilesCommand(projectDir, JavaFileTemplate.INTERFACE, SupportedLanguage.JAVA, SupportedIDE.VSCODE);

      // When
      DataTransferObject<GetAllFilesResponse> result = getAllFilesCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertNotNull(result.getData());
      assertEquals("VSCodeTest", result.getData().getResponse().get(0).getType());
      assertTrue(testService.wasServiceCalled());
    }

    @Test
    @DisplayName("Should handle NEOVIM IDE correctly")
    void shouldHandleNeovimIDECorrectly() throws Exception {
      // Given
      Path projectDir = createTestProjectStructure();
      GetAllFilesResponse response = createSuccessResponse("NeovimTest", "com.example", "/path/to/NeovimTest.java");
      testService.setSuccessResponse(response);
      setupGetAllFilesCommand(projectDir, JavaFileTemplate.RECORD, SupportedLanguage.JAVA, SupportedIDE.NEOVIM);

      // When
      DataTransferObject<GetAllFilesResponse> result = getAllFilesCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertNotNull(result.getData());
      assertEquals("NeovimTest", result.getData().getResponse().get(0).getType());
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
      assertTrue(Callable.class.isAssignableFrom(GetAllFilesCommand.class));

      // Verify command annotation exists
      assertTrue(GetAllFilesCommand.class.isAnnotationPresent(picocli.CommandLine.Command.class));

      // Verify command properties
      picocli.CommandLine.Command commandAnnotation =
          GetAllFilesCommand.class.getAnnotation(picocli.CommandLine.Command.class);
      assertEquals("get-all-files", commandAnnotation.name());
      assertEquals("Get a list of all files in the current working directory by it's type.", commandAnnotation.description()[0]);
    }

    @Test
    @DisplayName("Should handle all required options")
    void shouldHandleAllRequiredOptions() throws Exception {
      // Given
      Path projectDir = createTestProjectStructure();
      GetAllFilesResponse response = createSuccessResponse("OptionsTest", "com.example", "/path/to/OptionsTest.java");
      testService.setSuccessResponse(response);
      setupGetAllFilesCommand(projectDir, JavaFileTemplate.CLASS, SupportedLanguage.JAVA, SupportedIDE.NONE);

      // When
      DataTransferObject<GetAllFilesResponse> result = getAllFilesCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertEquals(projectDir, testService.getLastCalledCwd());
      assertEquals(JavaFileTemplate.CLASS, testService.getLastCalledFileType());
      assertTrue(testService.wasServiceCalled());
    }

    @Test
    @DisplayName("Should return DataTransferObject with correct generic type")
    void shouldReturnDataTransferObjectWithCorrectGenericType() throws Exception {
      // Given
      Path projectDir = createTestProjectStructure();
      GetAllFilesResponse response = createSuccessResponse("GenericTest", "com.example", "/path/to/GenericTest.java");
      testService.setSuccessResponse(response);
      setupGetAllFilesCommand(projectDir, JavaFileTemplate.CLASS, SupportedLanguage.JAVA, SupportedIDE.NONE);

      // When
      DataTransferObject<GetAllFilesResponse> result = getAllFilesCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertNotNull(result.getData());
      assertTrue(result.getData() instanceof GetAllFilesResponse);
      assertEquals(GetAllFilesResponse.class, result.getData().getClass());
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
      testService.setErrorResponse("Invalid directory path.");
      setupGetAllFilesCommand(projectDir, JavaFileTemplate.CLASS, SupportedLanguage.JAVA, SupportedIDE.NONE);

      // When
      DataTransferObject<GetAllFilesResponse> result = getAllFilesCommand.call();

      // Then
      assertFalse(result.getSucceed());
      assertEquals("Invalid directory path.", result.getErrorReason());
      assertTrue(testService.wasServiceCalled());
    }

    @Test
    @DisplayName("Should handle directory not found error")
    void shouldHandleDirectoryNotFoundError() throws Exception {
      // Given
      Path nonExistentDir = tempDir.resolve("non-existent");
      testService.setErrorResponse("Directory not found: " + nonExistentDir);
      setupGetAllFilesCommand(nonExistentDir, JavaFileTemplate.ENUM, SupportedLanguage.JAVA, SupportedIDE.NONE);

      // When
      DataTransferObject<GetAllFilesResponse> result = getAllFilesCommand.call();

      // Then
      assertFalse(result.getSucceed());
      assertTrue(result.getErrorReason().contains("Directory not found"));
      assertTrue(testService.wasServiceCalled());
    }

    @Test
    @DisplayName("Should handle service returning null response")
    void shouldHandleServiceReturningNullResponse() throws Exception {
      // Given
      Path projectDir = createTestProjectStructure();
      testService.setNullResponse();
      setupGetAllFilesCommand(projectDir, JavaFileTemplate.CLASS, SupportedLanguage.JAVA, SupportedIDE.NONE);

      // When
      DataTransferObject<GetAllFilesResponse> result = getAllFilesCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertNull(result.getData());
      assertTrue(testService.wasServiceCalled());
    }

    @Test
    @DisplayName("Should handle files with complex package paths")
    void shouldHandleFilesWithComplexPackagePaths() throws Exception {
      // Given
      Path projectDir = createTestProjectStructure();
      GetAllFilesResponse response = createSuccessResponse(
          "ComplexClass", 
          "com.example.deep.nested.package", 
          "/path/to/deep/nested/ComplexClass.java"
      );
      testService.setSuccessResponse(response);
      setupGetAllFilesCommand(projectDir, JavaFileTemplate.CLASS, SupportedLanguage.JAVA, SupportedIDE.NONE);

      // When
      DataTransferObject<GetAllFilesResponse> result = getAllFilesCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertNotNull(result.getData());
      assertEquals("com.example.deep.nested.package", result.getData().getResponse().get(0).getPackagePath());
      assertTrue(testService.wasServiceCalled());
    }

    @Test
    @DisplayName("Should use default language and IDE when not specified")
    void shouldUseDefaultLanguageAndIDEWhenNotSpecified() throws Exception {
      // Given
      Path projectDir = createTestProjectStructure();
      GetAllFilesResponse response = createSuccessResponse("DefaultTest", "com.example", "/path/to/DefaultTest.java");
      testService.setSuccessResponse(response);
      // Don't explicitly set language and IDE - should use defaults
      setupGetAllFilesCommandWithDefaults(projectDir, JavaFileTemplate.CLASS);

      // When
      DataTransferObject<GetAllFilesResponse> result = getAllFilesCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertTrue(testService.wasServiceCalled());
      assertEquals("DefaultTest", result.getData().getResponse().get(0).getType());
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

  private GetAllFilesResponse createSuccessResponse(String type, String packagePath, String filePath) {
    FileResponse fileResponse = new FileResponse();
    fileResponse.setType(type);
    fileResponse.setPackagePath(packagePath);
    fileResponse.setFilePath(filePath);
    
    GetAllFilesResponse response = new GetAllFilesResponse();
    response.getResponse().add(fileResponse);
    
    return response;
  }

  private GetAllFilesResponse createMultipleFilesResponse() {
    GetAllFilesResponse response = new GetAllFilesResponse();
    
    FileResponse enum1 = new FileResponse();
    enum1.setType("StateEnum");
    enum1.setPackagePath("com.example.enums");
    enum1.setFilePath("/path/to/StateEnum.java");
    
    FileResponse enum2 = new FileResponse();
    enum2.setType("StatusEnum");
    enum2.setPackagePath("com.example.enums");
    enum2.setFilePath("/path/to/StatusEnum.java");
    
    FileResponse enum3 = new FileResponse();
    enum3.setType("TypeEnum");
    enum3.setPackagePath("com.example.types");
    enum3.setFilePath("/path/to/TypeEnum.java");
    
    response.getResponse().add(enum1);
    response.getResponse().add(enum2);
    response.getResponse().add(enum3);
    
    return response;
  }

  private void setupGetAllFilesCommand(Path cwd, JavaFileTemplate fileType, SupportedLanguage language, SupportedIDE ide) {
    setField(getAllFilesCommand, "cwd", cwd);
    setField(getAllFilesCommand, "fileType", fileType);
    setField(getAllFilesCommand, "language", language);
    setField(getAllFilesCommand, "ide", ide);
  }

  private void setupGetAllFilesCommandWithDefaults(Path cwd, JavaFileTemplate fileType) {
    setField(getAllFilesCommand, "cwd", cwd);
    setField(getAllFilesCommand, "fileType", fileType);
    // Don't set language and IDE - let them use defaults
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
   * Test implementation of GetAllFilesCommandService that captures method calls and allows setting
   * predefined responses.
   */
  private static class TestGetAllFilesCommandService extends GetAllFilesCommandService {
    private boolean serviceCalled = false;
    private Path lastCalledCwd;
    private JavaFileTemplate lastCalledFileType;
    private DataTransferObject<GetAllFilesResponse> responseToReturn;

    public TestGetAllFilesCommandService() {
      super(null); // Service not needed for testing
    }

    @Override
    public DataTransferObject<GetAllFilesResponse> run(Path cwd, JavaFileTemplate fileType) {
      this.serviceCalled = true;
      this.lastCalledCwd = cwd;
      this.lastCalledFileType = fileType;
      return this.responseToReturn;
    }

    public void setSuccessResponse(GetAllFilesResponse response) {
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

    public JavaFileTemplate getLastCalledFileType() {
      return this.lastCalledFileType;
    }
  }
}