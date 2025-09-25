package io.github.syntaxpresso.core.command;

import static org.junit.jupiter.api.Assertions.*;

import io.github.syntaxpresso.core.command.dto.CreateNewJPAEntityResponse;
import io.github.syntaxpresso.core.common.DataTransferObject;
import io.github.syntaxpresso.core.service.java.command.CreateNewJPAEntityCommandService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Comprehensive tests for CreateNewJPAEntityCommand.
 *
 * <p>Example usage:
 *
 * <pre>
 * CreateNewJPAEntityCommand command = new CreateNewJPAEntityCommand(service);
 * // Set command options: --cwd /path/to/project --package-name com.example --file-name User.java
 * DataTransferObject&lt;CreateNewJPAEntityResponse&gt; result = command.call();
 * if (result.getSucceed()) {
 *     CreateNewJPAEntityResponse response = result.getData();
 *     System.out.println("Created entity: " + response.getFilePath());
 * }
 * </pre>
 */
@DisplayName("CreateNewJPAEntityCommand Tests")
class CreateNewJPAEntityCommandTest {

  @TempDir Path tempDir;
  private CreateNewJPAEntityCommand createNewJPAEntityCommand;
  private TestCreateNewJPAEntityCommandService testService;

  @BeforeEach
  void setUp() {
    testService = new TestCreateNewJPAEntityCommandService();
    createNewJPAEntityCommand = new CreateNewJPAEntityCommand(testService);
  }

  @Nested
  @DisplayName("Command configuration tests")
  class CommandConfigurationTests {

    @Test
    @DisplayName("Should have correct picocli annotations")
    void shouldHaveCorrectPicocliAnnotations() {
      // Given & When
      var commandAnnotation =
          CreateNewJPAEntityCommand.class.getAnnotation(picocli.CommandLine.Command.class);

      // Then
      assertNotNull(commandAnnotation);
      assertEquals("create-new-jpa-entity", commandAnnotation.name());
      assertEquals("Create New JPA Entity", commandAnnotation.description()[0]);
    }

    @Test
    @DisplayName("Should return DataTransferObject with correct generic type")
    void shouldReturnDataTransferObjectWithCorrectGenericType() {
      // Given & When
      assertTrue(createNewJPAEntityCommand instanceof Callable);

      // Then - verify return type through successful call
      testService.setSuccessResponse(
          CreateNewJPAEntityResponse.builder().filePath("/test/User.java").build());
      setupCreateNewJPAEntityCommand(tempDir, "com.example", "User.java");

      // When
      assertDoesNotThrow(
          () -> {
            DataTransferObject<CreateNewJPAEntityResponse> result =
                createNewJPAEntityCommand.call();
            assertNotNull(result);
            assertTrue(result.getData() instanceof CreateNewJPAEntityResponse);
          });
    }

    @Test
    @DisplayName("Should handle all required options")
    void shouldHandleAllRequiredOptions() throws Exception {
      // Given
      testService.setSuccessResponse(
          CreateNewJPAEntityResponse.builder().filePath("/test/Entity.java").build());

      // When
      setupCreateNewJPAEntityCommand(tempDir, "com.test.package", "TestEntity.java");
      DataTransferObject<CreateNewJPAEntityResponse> result = createNewJPAEntityCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertNotNull(result.getData());
      assertTrue(testService.wasServiceCalled());
      assertEquals(tempDir, testService.getLastCalledCwd());
      assertEquals("com.test.package", testService.getLastCalledPackageName());
      assertEquals("TestEntity.java", testService.getLastCalledFileName());
    }
  }

  @Nested
  @DisplayName("Entity creation tests")
  class EntityCreationTests {

    @Test
    @DisplayName("Should successfully create new JPA entity")
    void shouldSuccessfullyCreateNewJPAEntity() throws Exception {
      // Given
      testService.setSuccessResponse(
          CreateNewJPAEntityResponse.builder()
              .filePath("/test/src/main/java/com/example/User.java")
              .build());

      setupCreateNewJPAEntityCommand(tempDir, "com.example", "User.java");

      // When
      DataTransferObject<CreateNewJPAEntityResponse> result = createNewJPAEntityCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertNotNull(result.getData());
      assertEquals("/test/src/main/java/com/example/User.java", result.getData().getFilePath());
      assertTrue(testService.wasServiceCalled());
    }

    @Test
    @DisplayName("Should handle different entity names with proper table naming")
    void shouldHandleDifferentEntityNamesWithProperTableNaming() throws Exception {
      // Given
      testService.setSuccessResponse(
          CreateNewJPAEntityResponse.builder().filePath("/test/OrderItem.java").build());

      setupCreateNewJPAEntityCommand(tempDir, "com.example.model", "OrderItem.java");

      // When
      DataTransferObject<CreateNewJPAEntityResponse> result = createNewJPAEntityCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertTrue(testService.wasServiceCalled());
      assertEquals("OrderItem.java", testService.getLastCalledFileName());
    }

    @Test
    @DisplayName("Should handle service error responses")
    void shouldHandleServiceErrorResponses() throws Exception {
      // Given
      testService.setErrorResponse("File creation failed");
      setupCreateNewJPAEntityCommand(tempDir, "com.example", "Product.java");

      // When
      DataTransferObject<CreateNewJPAEntityResponse> result = createNewJPAEntityCommand.call();

      // Then
      assertFalse(result.getSucceed());
      assertNull(result.getData());
      assertEquals("File creation failed", result.getErrorReason());
      assertTrue(testService.wasServiceCalled());
    }

    @Test
    @DisplayName("Should handle duplicate entity names")
    void shouldHandleDuplicateEntityNames() throws Exception {
      // Given
      testService.setErrorResponse("JPA Entity already exist");
      setupCreateNewJPAEntityCommand(tempDir, "com.example", "User.java");

      // When
      DataTransferObject<CreateNewJPAEntityResponse> result = createNewJPAEntityCommand.call();

      // Then
      assertFalse(result.getSucceed());
      assertEquals("JPA Entity already exist", result.getErrorReason());
      assertTrue(testService.wasServiceCalled());
    }
  }

  @Nested
  @DisplayName("File validation tests")
  class FileValidationTests {

    @Test
    @DisplayName("Should validate Java file extension")
    void shouldValidateJavaFileExtension() throws Exception {
      // Given
      testService.setErrorResponse("File is not a .java file: User.txt");
      setupCreateNewJPAEntityCommand(tempDir, "com.example", "User.txt");

      // When
      DataTransferObject<CreateNewJPAEntityResponse> result = createNewJPAEntityCommand.call();

      // Then
      assertFalse(result.getSucceed());
      assertEquals("File is not a .java file: User.txt", result.getErrorReason());
      assertTrue(testService.wasServiceCalled());
    }

    @Test
    @DisplayName("Should handle files without extension")
    void shouldHandleFilesWithoutExtension() throws Exception {
      // Given
      testService.setErrorResponse("File is not a .java file: User");
      setupCreateNewJPAEntityCommand(tempDir, "com.example", "User");

      // When
      DataTransferObject<CreateNewJPAEntityResponse> result = createNewJPAEntityCommand.call();

      // Then
      assertFalse(result.getSucceed());
      assertTrue(testService.wasServiceCalled());
    }

    @Test
    @DisplayName("Should handle different package name formats")
    void shouldHandleDifferentPackageNameFormats() throws Exception {
      // Given
      testService.setSuccessResponse(
          CreateNewJPAEntityResponse.builder().filePath("/test/Entity.java").build());

      setupCreateNewJPAEntityCommand(tempDir, "com.company.project.model.entity", "Entity.java");

      // When
      DataTransferObject<CreateNewJPAEntityResponse> result = createNewJPAEntityCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertEquals("com.company.project.model.entity", testService.getLastCalledPackageName());
    }
  }

  @Nested
  @DisplayName("Package and naming tests")
  class PackageAndNamingTests {

    @Test
    @DisplayName("Should handle simple package names")
    void shouldHandleSimplePackageNames() throws Exception {
      // Given
      testService.setSuccessResponse(
          CreateNewJPAEntityResponse.builder().filePath("/test/User.java").build());

      setupCreateNewJPAEntityCommand(tempDir, "model", "User.java");

      // When
      DataTransferObject<CreateNewJPAEntityResponse> result = createNewJPAEntityCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertEquals("model", testService.getLastCalledPackageName());
    }

    @Test
    @DisplayName("Should handle complex class names")
    void shouldHandleComplexClassNames() throws Exception {
      // Given
      testService.setSuccessResponse(
          CreateNewJPAEntityResponse.builder().filePath("/test/UserOrderHistory.java").build());

      setupCreateNewJPAEntityCommand(tempDir, "com.example.model", "UserOrderHistory.java");

      // When
      DataTransferObject<CreateNewJPAEntityResponse> result = createNewJPAEntityCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertEquals("UserOrderHistory.java", testService.getLastCalledFileName());
    }

    @Test
    @DisplayName("Should handle single character class names")
    void shouldHandleSingleCharacterClassNames() throws Exception {
      // Given
      testService.setSuccessResponse(
          CreateNewJPAEntityResponse.builder().filePath("/test/X.java").build());

      setupCreateNewJPAEntityCommand(tempDir, "com.example", "X.java");

      // When
      DataTransferObject<CreateNewJPAEntityResponse> result = createNewJPAEntityCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertEquals("X.java", testService.getLastCalledFileName());
    }
  }

  @Nested
  @DisplayName("Edge case tests")
  class EdgeCaseTests {

    @Test
    @DisplayName("Should handle service returning null response")
    void shouldHandleServiceReturningNullResponse() throws Exception {
      // Given
      testService.setNullResponse();
      setupCreateNewJPAEntityCommand(tempDir, "com.example", "User.java");

      // When
      DataTransferObject<CreateNewJPAEntityResponse> result = createNewJPAEntityCommand.call();

      // Then
      assertNull(result); // Service returned null, so command returns null
      assertTrue(testService.wasServiceCalled());
    }

    @Test
    @DisplayName("Should handle service validation errors")
    void shouldHandleServiceValidationErrors() throws Exception {
      // Given
      testService.setErrorResponse("Package declaration is empty");
      setupCreateNewJPAEntityCommand(tempDir, "com.example", "User.java");

      // When
      DataTransferObject<CreateNewJPAEntityResponse> result = createNewJPAEntityCommand.call();

      // Then
      assertFalse(result.getSucceed());
      assertEquals("Package declaration is empty", result.getErrorReason());
    }

    @Test
    @DisplayName("Should handle complex file paths")
    void shouldHandleComplexFilePaths() throws Exception {
      // Given
      Path complexDir = tempDir.resolve("very").resolve("deep").resolve("nested").resolve("path");
      Files.createDirectories(complexDir);

      testService.setSuccessResponse(
          CreateNewJPAEntityResponse.builder()
              .filePath(complexDir.resolve("ComplexEntity.java").toString())
              .build());

      setupCreateNewJPAEntityCommand(complexDir, "com.example.deep.nested", "ComplexEntity.java");

      // When
      DataTransferObject<CreateNewJPAEntityResponse> result = createNewJPAEntityCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertTrue(result.getData().getFilePath().contains("ComplexEntity.java"));
      assertEquals(complexDir, testService.getLastCalledCwd());
    }

    @Test
    @DisplayName("Should handle annotation processing errors")
    void shouldHandleAnnotationProcessingErrors() throws Exception {
      // Given
      testService.setErrorResponse("Entity annotation is null");
      setupCreateNewJPAEntityCommand(tempDir, "com.example", "User.java");

      // When
      DataTransferObject<CreateNewJPAEntityResponse> result = createNewJPAEntityCommand.call();

      // Then
      assertFalse(result.getSucceed());
      assertEquals("Entity annotation is null", result.getErrorReason());
    }

    @Test
    @DisplayName("Should handle file save errors")
    void shouldHandleFileSaveErrors() throws Exception {
      // Given
      testService.setErrorResponse("Could not save file: Permission denied");
      setupCreateNewJPAEntityCommand(tempDir, "com.example", "User.java");

      // When
      DataTransferObject<CreateNewJPAEntityResponse> result = createNewJPAEntityCommand.call();

      // Then
      assertFalse(result.getSucceed());
      assertTrue(result.getErrorReason().contains("Could not save file"));
    }

    @Test
    @DisplayName("Should handle empty package names")
    void shouldHandleEmptyPackageNames() throws Exception {
      // Given
      testService.setSuccessResponse(
          CreateNewJPAEntityResponse.builder().filePath("/test/User.java").build());

      setupCreateNewJPAEntityCommand(tempDir, "", "User.java");

      // When
      DataTransferObject<CreateNewJPAEntityResponse> result = createNewJPAEntityCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertEquals("", testService.getLastCalledPackageName());
    }
  }

  /** Helper method to set up CreateNewJPAEntityCommand with test data. */
  private void setupCreateNewJPAEntityCommand(Path cwd, String packageName, String fileName) {
    setField(createNewJPAEntityCommand, "cwd", cwd);
    setField(createNewJPAEntityCommand, "packageName", packageName);
    setField(createNewJPAEntityCommand, "fileName", fileName);
  }

  /** Helper method to set private fields using reflection for testing purposes. */
  private void setField(Object target, String fieldName, Object value) {
    try {
      var field = target.getClass().getDeclaredField(fieldName);
      field.setAccessible(true);
      field.set(target, value);
    } catch (Exception e) {
      throw new RuntimeException("Failed to set field: " + fieldName, e);
    }
  }

  /** Test double for CreateNewJPAEntityCommandService to control service behavior in tests. */
  private static class TestCreateNewJPAEntityCommandService
      extends CreateNewJPAEntityCommandService {
    private boolean serviceCalled = false;
    private DataTransferObject<CreateNewJPAEntityResponse> responseToReturn;
    private Path lastCalledCwd;
    private String lastCalledPackageName;
    private String lastCalledFileName;

    public TestCreateNewJPAEntityCommandService() {
      super(null, null); // Mock dependencies are not used in tests
    }

    @Override
    public DataTransferObject<CreateNewJPAEntityResponse> createNewJPAEntity(
        Path cwd, String packageName, String fileName) {
      this.serviceCalled = true;
      this.lastCalledCwd = cwd;
      this.lastCalledPackageName = packageName;
      this.lastCalledFileName = fileName;
      return this.responseToReturn;
    }

    public void setSuccessResponse(CreateNewJPAEntityResponse response) {
      this.responseToReturn = DataTransferObject.success(response);
    }

    public void setErrorResponse(String errorMessage) {
      this.responseToReturn = DataTransferObject.error(errorMessage);
    }

    public void setNullResponse() {
      this.responseToReturn = null;
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
  }
}

