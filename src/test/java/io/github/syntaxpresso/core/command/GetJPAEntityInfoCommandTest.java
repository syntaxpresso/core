package io.github.syntaxpresso.core.command;

import static org.junit.jupiter.api.Assertions.*;

import io.github.syntaxpresso.core.command.dto.GetJPAEntityInfoResponse;
import io.github.syntaxpresso.core.common.DataTransferObject;
import io.github.syntaxpresso.core.common.extra.SupportedIDE;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import io.github.syntaxpresso.core.service.java.command.GetJPAEntityInfoCommandService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Comprehensive tests for GetJPAEntityInfoCommand.
 *
 * <p>Example usage:
 *
 * <pre>
 * GetJPAEntityInfoCommand command = new GetJPAEntityInfoCommand(service);
 * // Set command options: --cwd /path/to/project --file-path /path/to/Entity.java --language JAVA --ide NONE
 * DataTransferObject&lt;GetJPAEntityInfoResponse&gt; result = command.call();
 * if (result.getSucceed()) {
 *     GetJPAEntityInfoResponse response = result.getData();
 *     System.out.println("Entity type: " + response.getEntityType());
 *     System.out.println("ID field type: " + response.getIdFieldType());
 *     System.out.println("Is JPA Entity: " + response.getIsJPAEntity());
 * }
 * </pre>
 */
@DisplayName("GetJPAEntityInfoCommand Tests")
class GetJPAEntityInfoCommandTest {
  private GetJPAEntityInfoCommand getJPAEntityInfoCommand;
  private TestGetJPAEntityInfoCommandService testService;

  @TempDir Path tempDir;

  @BeforeEach
  void setUp() {
    this.testService = new TestGetJPAEntityInfoCommandService();
    this.getJPAEntityInfoCommand = new GetJPAEntityInfoCommand(this.testService);
  }

  @Nested
  @DisplayName("Java language support tests")
  class JavaLanguageSupportTests {
    @Test
    @DisplayName("Should successfully analyze JPA entity for Java language")
    void shouldSuccessfullyAnalyzeJPAEntityForJavaLanguage() throws Exception {
      // Given
      Path entityFilePath = createTestEntityFile();
      GetJPAEntityInfoResponse expectedResponse =
          GetJPAEntityInfoResponse.builder()
              .isJPAEntity(true)
              .entityType("User")
              .entityPackageName("com.example.model")
              .entityPath(entityFilePath.toString())
              .idFieldType("Long")
              .idFieldPackageName("java.lang")
              .build();
      testService.setSuccessResponse(expectedResponse);
      setupGetJPAEntityInfoCommand(
          tempDir, entityFilePath, SupportedLanguage.JAVA, SupportedIDE.NONE, null);

      // When
      DataTransferObject<GetJPAEntityInfoResponse> result = getJPAEntityInfoCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertNotNull(result.getData());
      assertEquals(true, result.getData().getIsJPAEntity());
      assertEquals("User", result.getData().getEntityType());
      assertEquals("com.example.model", result.getData().getEntityPackageName());
      assertEquals("Long", result.getData().getIdFieldType());

      // Verify service was called with correct parameters
      assertTrue(testService.wasServiceCalled());
      assertEquals(tempDir, testService.getLastCalledCwd());
      assertEquals(entityFilePath, testService.getLastCalledFilePath());
      assertEquals(SupportedLanguage.JAVA, testService.getLastCalledLanguage());
      assertEquals(SupportedIDE.NONE, testService.getLastCalledIDE());
    }

    @Test
    @DisplayName("Should handle service error responses for Java")
    void shouldHandleServiceErrorResponsesForJava() throws Exception {
      // Given
      Path entityFilePath = createTestEntityFile();
      testService.setErrorResponse("No public class found in the entity file.");
      setupGetJPAEntityInfoCommand(
          tempDir, entityFilePath, SupportedLanguage.JAVA, SupportedIDE.NONE, null);

      // When
      DataTransferObject<GetJPAEntityInfoResponse> result = getJPAEntityInfoCommand.call();

      // Then
      assertFalse(result.getSucceed());
      assertNull(result.getData());
      assertEquals("No public class found in the entity file.", result.getErrorReason());
      assertTrue(testService.wasServiceCalled());
    }

    @Test
    @DisplayName("Should pass superclass source when provided")
    void shouldPassSuperclassSourceWhenProvided() throws Exception {
      // Given
      Path entityFilePath = createTestEntityFile();
      String superclassSource = "public class BaseEntity { @Id private Long id; }";
      String encodedSuperclassSource =
          Base64.getEncoder().encodeToString(superclassSource.getBytes());

      GetJPAEntityInfoResponse expectedResponse =
          GetJPAEntityInfoResponse.builder()
              .isJPAEntity(true)
              .entityType("User")
              .entityPackageName("com.example.model")
              .entityPath(entityFilePath.toString())
              .idFieldType("Long")
              .idFieldPackageName("java.lang")
              .build();
      testService.setSuccessResponse(expectedResponse);
      setupGetJPAEntityInfoCommand(
          tempDir,
          entityFilePath,
          SupportedLanguage.JAVA,
          SupportedIDE.NONE,
          encodedSuperclassSource);

      // When
      DataTransferObject<GetJPAEntityInfoResponse> result = getJPAEntityInfoCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertTrue(testService.wasServiceCalled());
      assertEquals(encodedSuperclassSource, testService.getLastCalledSuperclassSource());
    }
  }

  @Nested
  @DisplayName("Unsupported language tests")
  class UnsupportedLanguageTests {
    @Test
    @DisplayName("Should return error for unsupported language")
    void shouldReturnErrorForUnsupportedLanguage() throws Exception {
      // Given
      Path entityFilePath = createTestEntityFile();
      // Use reflection to mock an unsupported language by setting language to null
      setupGetJPAEntityInfoCommandWithNullLanguage(
          tempDir, entityFilePath, SupportedIDE.NONE, null);

      // When
      DataTransferObject<GetJPAEntityInfoResponse> result = getJPAEntityInfoCommand.call();

      // Then
      assertFalse(result.getSucceed());
      assertNull(result.getData());
      assertEquals("Language not supported.", result.getErrorReason());
      assertFalse(testService.wasServiceCalled());
    }
  }

  private Path createTestEntityFile() throws IOException {
    Path entityFile = tempDir.resolve("User.java");
    String entityContent =
        "package com.example.model;\n"
            + "\n"
            + "import jakarta.persistence.*;\n"
            + "\n"
            + "@Entity\n"
            + "@Table(name = \"users\")\n"
            + "public class User {\n"
            + "    @Id\n"
            + "    @GeneratedValue(strategy = GenerationType.IDENTITY)\n"
            + "    private Long id;\n"
            + "    \n"
            + "    private String name;\n"
            + "    private String email;\n"
            + "}\n";
    Files.writeString(entityFile, entityContent);
    return entityFile;
  }

  private void setupGetJPAEntityInfoCommand(
      Path cwd,
      Path filePath,
      SupportedLanguage language,
      SupportedIDE ide,
      String superclassSource) {
    try {
      // Use reflection to set private fields since they're annotated with @Option
      var cwdField = GetJPAEntityInfoCommand.class.getDeclaredField("cwd");
      cwdField.setAccessible(true);
      cwdField.set(getJPAEntityInfoCommand, cwd);

      var filePathField = GetJPAEntityInfoCommand.class.getDeclaredField("filePath");
      filePathField.setAccessible(true);
      filePathField.set(getJPAEntityInfoCommand, filePath);

      var languageField = GetJPAEntityInfoCommand.class.getDeclaredField("language");
      languageField.setAccessible(true);
      languageField.set(getJPAEntityInfoCommand, language);

      var ideField = GetJPAEntityInfoCommand.class.getDeclaredField("ide");
      ideField.setAccessible(true);
      ideField.set(getJPAEntityInfoCommand, ide);

      if (superclassSource != null) {
        var superclassSourceField =
            GetJPAEntityInfoCommand.class.getDeclaredField("superclassSource");
        superclassSourceField.setAccessible(true);
        superclassSourceField.set(getJPAEntityInfoCommand, superclassSource);
      }
    } catch (Exception e) {
      throw new RuntimeException("Failed to setup command", e);
    }
  }

  private void setupGetJPAEntityInfoCommandWithNullLanguage(
      Path cwd, Path filePath, SupportedIDE ide, String superclassSource) {
    try {
      // Use reflection to set private fields since they're annotated with @Option
      var cwdField = GetJPAEntityInfoCommand.class.getDeclaredField("cwd");
      cwdField.setAccessible(true);
      cwdField.set(getJPAEntityInfoCommand, cwd);

      var filePathField = GetJPAEntityInfoCommand.class.getDeclaredField("filePath");
      filePathField.setAccessible(true);
      filePathField.set(getJPAEntityInfoCommand, filePath);

      var languageField = GetJPAEntityInfoCommand.class.getDeclaredField("language");
      languageField.setAccessible(true);
      languageField.set(
          getJPAEntityInfoCommand, null); // Set to null to simulate unsupported language

      var ideField = GetJPAEntityInfoCommand.class.getDeclaredField("ide");
      ideField.setAccessible(true);
      ideField.set(getJPAEntityInfoCommand, ide);

      if (superclassSource != null) {
        var superclassSourceField =
            GetJPAEntityInfoCommand.class.getDeclaredField("superclassSource");
        superclassSourceField.setAccessible(true);
        superclassSourceField.set(getJPAEntityInfoCommand, superclassSource);
      }
    } catch (Exception e) {
      throw new RuntimeException("Failed to setup command", e);
    }
  }

  /** Test implementation of GetJPAEntityInfoCommandService for testing purposes. */
  private static class TestGetJPAEntityInfoCommandService extends GetJPAEntityInfoCommandService {
    private DataTransferObject<GetJPAEntityInfoResponse> responseToReturn;
    private boolean serviceCalled = false;
    private Path lastCalledCwd;
    private Path lastCalledFilePath;
    private SupportedLanguage lastCalledLanguage;
    private SupportedIDE lastCalledIDE;
    private String lastCalledSuperclassSource;

    public TestGetJPAEntityInfoCommandService() {
      super(null); // Mock dependencies are not used in tests
    }

    @Override
    public DataTransferObject<GetJPAEntityInfoResponse> run(
        Path cwd,
        Path filePath,
        SupportedLanguage language,
        SupportedIDE ide,
        String superclassSource) {
      this.serviceCalled = true;
      this.lastCalledCwd = cwd;
      this.lastCalledFilePath = filePath;
      this.lastCalledLanguage = language;
      this.lastCalledIDE = ide;
      this.lastCalledSuperclassSource = superclassSource;
      return this.responseToReturn;
    }

    public void setSuccessResponse(GetJPAEntityInfoResponse response) {
      this.responseToReturn = DataTransferObject.success(response);
    }

    public void setErrorResponse(String errorMessage) {
      this.responseToReturn = DataTransferObject.error(errorMessage);
    }

    public boolean wasServiceCalled() {
      return this.serviceCalled;
    }

    public Path getLastCalledCwd() {
      return this.lastCalledCwd;
    }

    public Path getLastCalledFilePath() {
      return this.lastCalledFilePath;
    }

    public SupportedLanguage getLastCalledLanguage() {
      return this.lastCalledLanguage;
    }

    public SupportedIDE getLastCalledIDE() {
      return this.lastCalledIDE;
    }

    public String getLastCalledSuperclassSource() {
      return this.lastCalledSuperclassSource;
    }
  }
}

