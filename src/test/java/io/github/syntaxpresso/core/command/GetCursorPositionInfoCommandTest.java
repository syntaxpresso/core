package io.github.syntaxpresso.core.command;

import static org.junit.jupiter.api.Assertions.*;

import io.github.syntaxpresso.core.command.dto.GetCursorPositionInfoResponse;
import io.github.syntaxpresso.core.common.DataTransferObject;
import io.github.syntaxpresso.core.common.extra.SupportedIDE;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import io.github.syntaxpresso.core.service.extra.JavaIdentifierType;
import io.github.syntaxpresso.core.service.java.command.GetCursorPositionInfoCommandService;
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
 * Comprehensive tests for GetCursorPositionInfoCommand.
 *
 * <p>Example usage:
 *
 * <pre>
 * GetCursorPositionInfoCommand command = new GetCursorPositionInfoCommand(service);
 * // Set command options: --file-path /path/to/file.java --language JAVA --ide NONE --line 5 --column 10
 * DataTransferObject&lt;GetCursorPositionInfoResponse&gt; result = command.call();
 * if (result.getSucceed()) {
 *     GetCursorPositionInfoResponse response = result.getData();
 *     System.out.println("Node type: " + response.getNodeType());
 *     System.out.println("Node text: " + response.getNodeText());
 *     System.out.println("File path: " + response.getFilePath());
 * }
 * </pre>
 */
@DisplayName("GetCursorPositionInfoCommand Tests")
class GetCursorPositionInfoCommandTest {
  private GetCursorPositionInfoCommand getCursorPositionInfoCommand;
  private TestGetCursorPositionInfoCommandService testService;

  @TempDir Path tempDir;

  @BeforeEach
  void setUp() {
    this.testService = new TestGetCursorPositionInfoCommandService();
    this.getCursorPositionInfoCommand = new GetCursorPositionInfoCommand(this.testService);
  }

  @Nested
  @DisplayName("Java language support tests")
  class JavaLanguageSupportTests {
    @Test
    @DisplayName("Should successfully get cursor position info for Java language")
    void shouldSuccessfullyGetCursorPositionInfoForJavaLanguage() throws Exception {
      // Given
      Path javaFile = createTestJavaFile();
      GetCursorPositionInfoResponse expectedResponse =
          GetCursorPositionInfoResponse.builder()
              .filePath(javaFile.toString())
              .language(SupportedLanguage.JAVA)
              .node("identifier")
              .nodeText("testMethod")
              .nodeType(JavaIdentifierType.METHOD_NAME)
              .build();
      testService.setSuccessResponse(expectedResponse);
      setupGetCursorPositionInfoCommand(javaFile, SupportedLanguage.JAVA, SupportedIDE.NONE, 5, 15);

      // When
      DataTransferObject<GetCursorPositionInfoResponse> result =
          getCursorPositionInfoCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertNotNull(result.getData());
      assertEquals("testMethod", result.getData().getNodeText());
      assertEquals(JavaIdentifierType.METHOD_NAME, result.getData().getNodeType());
      assertEquals(javaFile.toString(), result.getData().getFilePath());
      assertTrue(testService.wasServiceCalled());
    }

    @Test
    @DisplayName("Should handle variable identifiers correctly")
    void shouldHandleVariableIdentifiersCorrectly() throws Exception {
      // Given
      Path javaFile = createTestJavaFile();
      GetCursorPositionInfoResponse expectedResponse =
          GetCursorPositionInfoResponse.builder()
              .filePath(javaFile.toString())
              .language(SupportedLanguage.JAVA)
              .node("identifier")
              .nodeText("variable")
              .nodeType(JavaIdentifierType.LOCAL_VARIABLE_NAME)
              .build();
      testService.setSuccessResponse(expectedResponse);
      setupGetCursorPositionInfoCommand(
          javaFile, SupportedLanguage.JAVA, SupportedIDE.VSCODE, 10, 8);

      // When
      DataTransferObject<GetCursorPositionInfoResponse> result =
          getCursorPositionInfoCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertNotNull(result.getData());
      assertEquals("variable", result.getData().getNodeText());
      assertEquals(JavaIdentifierType.LOCAL_VARIABLE_NAME, result.getData().getNodeType());
      assertTrue(testService.wasServiceCalled());
    }

    @Test
    @DisplayName("Should handle class identifiers correctly")
    void shouldHandleClassIdentifiersCorrectly() throws Exception {
      // Given
      Path javaFile = createTestJavaFile();
      GetCursorPositionInfoResponse expectedResponse =
          GetCursorPositionInfoResponse.builder()
              .filePath(javaFile.toString())
              .language(SupportedLanguage.JAVA)
              .node("identifier")
              .nodeText("TestClass")
              .nodeType(JavaIdentifierType.CLASS_NAME)
              .build();
      testService.setSuccessResponse(expectedResponse);
      setupGetCursorPositionInfoCommand(
          javaFile, SupportedLanguage.JAVA, SupportedIDE.NEOVIM, 3, 13);

      // When
      DataTransferObject<GetCursorPositionInfoResponse> result =
          getCursorPositionInfoCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertNotNull(result.getData());
      assertEquals("TestClass", result.getData().getNodeText());
      assertEquals(JavaIdentifierType.CLASS_NAME, result.getData().getNodeType());
      assertTrue(testService.wasServiceCalled());
    }

    @Test
    @DisplayName("Should handle service error responses for Java")
    void shouldHandleServiceErrorResponsesForJava() throws Exception {
      // Given
      Path javaFile = createTestJavaFile();
      testService.setErrorResponse("No symbol found at the specified position.");
      setupGetCursorPositionInfoCommand(
          javaFile, SupportedLanguage.JAVA, SupportedIDE.NONE, 100, 50);

      // When
      DataTransferObject<GetCursorPositionInfoResponse> result =
          getCursorPositionInfoCommand.call();

      // Then
      assertFalse(result.getSucceed());
      assertNull(result.getData());
      assertEquals("No symbol found at the specified position.", result.getErrorReason());
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
      Path javaFile = createTestJavaFile();
      setupGetCursorPositionInfoCommand(javaFile, null, SupportedIDE.NONE, 5, 10);

      // When & Then
      assertThrows(
          NullPointerException.class,
          () -> {
            getCursorPositionInfoCommand.call();
          });

      assertFalse(testService.wasServiceCalled());
    }

    @Test
    @DisplayName("Should process Java language successfully")
    void shouldProcessJavaLanguageSuccessfully() throws Exception {
      // Given
      Path javaFile = createTestJavaFile();
      GetCursorPositionInfoResponse response =
          GetCursorPositionInfoResponse.builder()
              .filePath(javaFile.toString())
              .language(SupportedLanguage.JAVA)
              .node("identifier")
              .nodeText("identifier")
              .nodeType(JavaIdentifierType.LOCAL_VARIABLE_NAME)
              .build();
      testService.setSuccessResponse(response);
      setupGetCursorPositionInfoCommand(javaFile, SupportedLanguage.JAVA, SupportedIDE.NONE, 5, 10);

      // When
      DataTransferObject<GetCursorPositionInfoResponse> result =
          getCursorPositionInfoCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertTrue(testService.wasServiceCalled());
      assertEquals("identifier", result.getData().getNodeText());
      assertEquals(JavaIdentifierType.LOCAL_VARIABLE_NAME, result.getData().getNodeType());
    }
  }

  @Nested
  @DisplayName("IDE-specific behavior tests")
  class IDESpecificBehaviorTests {
    @Test
    @DisplayName("Should handle NONE IDE correctly")
    void shouldHandleNoneIDECorrectly() throws Exception {
      // Given
      Path javaFile = createTestJavaFile();
      GetCursorPositionInfoResponse response =
          GetCursorPositionInfoResponse.builder()
              .filePath(javaFile.toString())
              .language(SupportedLanguage.JAVA)
              .node("identifier")
              .nodeText("noneIDETest")
              .nodeType(JavaIdentifierType.METHOD_NAME)
              .build();
      testService.setSuccessResponse(response);
      setupGetCursorPositionInfoCommand(javaFile, SupportedLanguage.JAVA, SupportedIDE.NONE, 5, 10);

      // When
      DataTransferObject<GetCursorPositionInfoResponse> result =
          getCursorPositionInfoCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertNotNull(result.getData());
      assertEquals("noneIDETest", result.getData().getNodeText());
      assertTrue(testService.wasServiceCalled());
    }

    @Test
    @DisplayName("Should handle VSCODE IDE correctly")
    void shouldHandleVSCodeIDECorrectly() throws Exception {
      // Given
      Path javaFile = createTestJavaFile();
      GetCursorPositionInfoResponse response =
          GetCursorPositionInfoResponse.builder()
              .filePath(javaFile.toString())
              .language(SupportedLanguage.JAVA)
              .node("identifier")
              .nodeText("vscodeTest")
              .nodeType(JavaIdentifierType.LOCAL_VARIABLE_NAME)
              .build();
      testService.setSuccessResponse(response);
      setupGetCursorPositionInfoCommand(
          javaFile, SupportedLanguage.JAVA, SupportedIDE.VSCODE, 8, 12);

      // When
      DataTransferObject<GetCursorPositionInfoResponse> result =
          getCursorPositionInfoCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertNotNull(result.getData());
      assertEquals("vscodeTest", result.getData().getNodeText());
      assertTrue(testService.wasServiceCalled());
    }

    @Test
    @DisplayName("Should handle NEOVIM IDE correctly")
    void shouldHandleNeovimIDECorrectly() throws Exception {
      // Given
      Path javaFile = createTestJavaFile();
      GetCursorPositionInfoResponse response =
          GetCursorPositionInfoResponse.builder()
              .filePath(javaFile.toString())
              .language(SupportedLanguage.JAVA)
              .node("identifier")
              .nodeText("neovimTest")
              .nodeType(JavaIdentifierType.CLASS_NAME)
              .build();
      testService.setSuccessResponse(response);
      setupGetCursorPositionInfoCommand(
          javaFile, SupportedLanguage.JAVA, SupportedIDE.NEOVIM, 3, 15);

      // When
      DataTransferObject<GetCursorPositionInfoResponse> result =
          getCursorPositionInfoCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertNotNull(result.getData());
      assertEquals("neovimTest", result.getData().getNodeText());
      assertTrue(testService.wasServiceCalled());
    }
  }

  @Nested
  @DisplayName("Cursor position validation tests")
  class CursorPositionValidationTests {
    @Test
    @DisplayName("Should handle valid line and column positions")
    void shouldHandleValidLineAndColumnPositions() throws Exception {
      // Given
      Path javaFile = createTestJavaFile();
      GetCursorPositionInfoResponse response =
          GetCursorPositionInfoResponse.builder()
              .filePath(javaFile.toString())
              .language(SupportedLanguage.JAVA)
              .node("identifier")
              .nodeText("validPosition")
              .nodeType(JavaIdentifierType.METHOD_NAME)
              .build();
      testService.setSuccessResponse(response);
      setupGetCursorPositionInfoCommand(javaFile, SupportedLanguage.JAVA, SupportedIDE.NONE, 5, 10);

      // When
      DataTransferObject<GetCursorPositionInfoResponse> result =
          getCursorPositionInfoCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertNotNull(result.getData());
      assertEquals("validPosition", result.getData().getNodeText());
      assertTrue(testService.wasServiceCalled());
      assertEquals(Integer.valueOf(5), testService.getLastCalledLine());
      assertEquals(Integer.valueOf(10), testService.getLastCalledColumn());
    }

    @Test
    @DisplayName("Should handle zero line and column positions")
    void shouldHandleZeroLineAndColumnPositions() throws Exception {
      // Given
      Path javaFile = createTestJavaFile();
      GetCursorPositionInfoResponse response =
          GetCursorPositionInfoResponse.builder()
              .filePath(javaFile.toString())
              .language(SupportedLanguage.JAVA)
              .node("identifier")
              .nodeText("zeroPosition")
              .nodeType(JavaIdentifierType.CLASS_NAME)
              .build();
      testService.setSuccessResponse(response);
      setupGetCursorPositionInfoCommand(javaFile, SupportedLanguage.JAVA, SupportedIDE.NONE, 0, 0);

      // When
      DataTransferObject<GetCursorPositionInfoResponse> result =
          getCursorPositionInfoCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertNotNull(result.getData());
      assertEquals("zeroPosition", result.getData().getNodeText());
      assertTrue(testService.wasServiceCalled());
      assertEquals(Integer.valueOf(0), testService.getLastCalledLine());
      assertEquals(Integer.valueOf(0), testService.getLastCalledColumn());
    }

    @Test
    @DisplayName("Should handle large line and column positions")
    void shouldHandleLargeLineAndColumnPositions() throws Exception {
      // Given
      Path javaFile = createTestJavaFile();
      testService.setErrorResponse("No symbol found at the specified position.");
      setupGetCursorPositionInfoCommand(
          javaFile, SupportedLanguage.JAVA, SupportedIDE.NONE, 1000, 500);

      // When
      DataTransferObject<GetCursorPositionInfoResponse> result =
          getCursorPositionInfoCommand.call();

      // Then
      assertFalse(result.getSucceed());
      assertEquals("No symbol found at the specified position.", result.getErrorReason());
      assertTrue(testService.wasServiceCalled());
      assertEquals(Integer.valueOf(1000), testService.getLastCalledLine());
      assertEquals(Integer.valueOf(500), testService.getLastCalledColumn());
    }
  }

  @Nested
  @DisplayName("Command configuration tests")
  class CommandConfigurationTests {
    @Test
    @DisplayName("Should have correct picocli annotations")
    void shouldHaveCorrectPicocliAnnotations() {
      // Verify the class implements Callable
      assertTrue(Callable.class.isAssignableFrom(GetCursorPositionInfoCommand.class));

      // Verify command annotation exists
      assertTrue(
          GetCursorPositionInfoCommand.class.isAnnotationPresent(
              picocli.CommandLine.Command.class));

      // Verify command properties
      picocli.CommandLine.Command commandAnnotation =
          GetCursorPositionInfoCommand.class.getAnnotation(picocli.CommandLine.Command.class);
      assertEquals("get-info", commandAnnotation.name());
      assertEquals(
          "Get info of an specific node based on cursor position.",
          commandAnnotation.description()[0]);
    }

    @Test
    @DisplayName("Should handle all required options")
    void shouldHandleAllRequiredOptions() throws Exception {
      // Given
      Path javaFile = createTestJavaFile();
      GetCursorPositionInfoResponse response =
          GetCursorPositionInfoResponse.builder()
              .filePath(javaFile.toString())
              .language(SupportedLanguage.JAVA)
              .node("identifier")
              .nodeText("optionsTest")
              .nodeType(JavaIdentifierType.LOCAL_VARIABLE_NAME)
              .build();
      testService.setSuccessResponse(response);
      setupGetCursorPositionInfoCommand(javaFile, SupportedLanguage.JAVA, SupportedIDE.NONE, 5, 10);

      // When
      DataTransferObject<GetCursorPositionInfoResponse> result =
          getCursorPositionInfoCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertEquals(javaFile, testService.getLastCalledFilePath());
      assertTrue(testService.wasServiceCalled());
    }

    @Test
    @DisplayName("Should return DataTransferObject with correct generic type")
    void shouldReturnDataTransferObjectWithCorrectGenericType() throws Exception {
      // Given
      Path javaFile = createTestJavaFile();
      GetCursorPositionInfoResponse response =
          GetCursorPositionInfoResponse.builder()
              .filePath(javaFile.toString())
              .language(SupportedLanguage.JAVA)
              .node("identifier")
              .nodeText("genericTest")
              .nodeType(JavaIdentifierType.METHOD_NAME)
              .build();
      testService.setSuccessResponse(response);
      setupGetCursorPositionInfoCommand(javaFile, SupportedLanguage.JAVA, SupportedIDE.NONE, 5, 10);

      // When
      DataTransferObject<GetCursorPositionInfoResponse> result =
          getCursorPositionInfoCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertNotNull(result.getData());
      assertTrue(result.getData() instanceof GetCursorPositionInfoResponse);
      assertEquals(GetCursorPositionInfoResponse.class, result.getData().getClass());
    }
  }

  @Nested
  @DisplayName("Edge case tests")
  class EdgeCaseTests {
    @Test
    @DisplayName("Should handle service validation errors")
    void shouldHandleServiceValidationErrors() throws Exception {
      // Given
      Path javaFile = createTestJavaFile();
      testService.setErrorResponse("File does not exist: " + javaFile);
      setupGetCursorPositionInfoCommand(javaFile, SupportedLanguage.JAVA, SupportedIDE.NONE, 5, 10);

      // When
      DataTransferObject<GetCursorPositionInfoResponse> result =
          getCursorPositionInfoCommand.call();

      // Then
      assertFalse(result.getSucceed());
      assertEquals("File does not exist: " + javaFile, result.getErrorReason());
      assertTrue(testService.wasServiceCalled());
    }

    @Test
    @DisplayName("Should handle unknown identifier types")
    void shouldHandleUnknownIdentifierTypes() throws Exception {
      // Given
      Path javaFile = createTestJavaFile();
      testService.setErrorResponse(
          "Unable to determine symbol type at cursor position. Node type: unknown, Node text:"
              + " 'unknownSymbol'");
      setupGetCursorPositionInfoCommand(
          javaFile, SupportedLanguage.JAVA, SupportedIDE.NONE, 15, 20);

      // When
      DataTransferObject<GetCursorPositionInfoResponse> result =
          getCursorPositionInfoCommand.call();

      // Then
      assertFalse(result.getSucceed());
      assertTrue(result.getErrorReason().contains("Unable to determine symbol type"));
      assertTrue(testService.wasServiceCalled());
    }

    @Test
    @DisplayName("Should handle empty symbol names")
    void shouldHandleEmptySymbolNames() throws Exception {
      // Given
      Path javaFile = createTestJavaFile();
      testService.setErrorResponse("Unable to determine current symbol name.");
      setupGetCursorPositionInfoCommand(javaFile, SupportedLanguage.JAVA, SupportedIDE.NONE, 1, 1);

      // When
      DataTransferObject<GetCursorPositionInfoResponse> result =
          getCursorPositionInfoCommand.call();

      // Then
      assertFalse(result.getSucceed());
      assertEquals("Unable to determine current symbol name.", result.getErrorReason());
      assertTrue(testService.wasServiceCalled());
    }

    @Test
    @DisplayName("Should handle service returning null response")
    void shouldHandleServiceReturningNullResponse() throws Exception {
      // Given
      Path javaFile = createTestJavaFile();
      testService.setNullResponse();
      setupGetCursorPositionInfoCommand(javaFile, SupportedLanguage.JAVA, SupportedIDE.NONE, 5, 10);

      // When
      DataTransferObject<GetCursorPositionInfoResponse> result =
          getCursorPositionInfoCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertNull(result.getData());
      assertTrue(testService.wasServiceCalled());
    }

    @Test
    @DisplayName("Should handle all Java identifier types")
    void shouldHandleAllJavaIdentifierTypes() throws Exception {
      // Given
      Path javaFile = createTestJavaFile();

      // Test each identifier type
      for (JavaIdentifierType identifierType : JavaIdentifierType.values()) {
        GetCursorPositionInfoResponse response =
            GetCursorPositionInfoResponse.builder()
                .filePath(javaFile.toString())
                .language(SupportedLanguage.JAVA)
                .node("identifier")
                .nodeText(identifierType.name().toLowerCase())
                .nodeType(identifierType)
                .build();
        testService.setSuccessResponse(response);
        setupGetCursorPositionInfoCommand(
            javaFile, SupportedLanguage.JAVA, SupportedIDE.NONE, 5, 10);

        // When
        DataTransferObject<GetCursorPositionInfoResponse> result =
            getCursorPositionInfoCommand.call();

        // Then
        assertTrue(result.getSucceed(), "Failed for identifier type: " + identifierType);
        assertNotNull(result.getData());
        assertEquals(identifierType, result.getData().getNodeType());
      }
    }
  }

  private Path createTestJavaFile() throws IOException {
    String javaCode =
        """
        package com.example;

        public class TestClass {
            private String field;

            public void testMethod() {
                int variable = 10;
                String another = "test";
                testMethod();
            }

            public String getField() {
                return this.field;
            }
        }
        """;

    Path javaFile = tempDir.resolve("TestClass.java");
    Files.write(javaFile, javaCode.getBytes());
    return javaFile;
  }

  private void setupGetCursorPositionInfoCommand(
      Path filePath, SupportedLanguage language, SupportedIDE ide, Integer line, Integer column) {
    setField(getCursorPositionInfoCommand, "filePath", filePath);
    setField(getCursorPositionInfoCommand, "language", language);
    setField(getCursorPositionInfoCommand, "ide", ide);
    setField(getCursorPositionInfoCommand, "line", line);
    setField(getCursorPositionInfoCommand, "column", column);
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
   * Test implementation of GetCursorPositionInfoCommandService that captures method calls and
   * allows setting predefined responses.
   */
  private static class TestGetCursorPositionInfoCommandService
      extends GetCursorPositionInfoCommandService {
    private boolean serviceCalled = false;
    private Path lastCalledFilePath;
    private Integer lastCalledLine;
    private Integer lastCalledColumn;
    private DataTransferObject<GetCursorPositionInfoResponse> responseToReturn;

    public TestGetCursorPositionInfoCommandService() {
      super(null, null); // Services not needed for testing
    }

    @Override
    public DataTransferObject<GetCursorPositionInfoResponse> run(
        Path filePath, SupportedLanguage language, SupportedIDE ide, Integer line, Integer column) {
      this.serviceCalled = true;
      this.lastCalledFilePath = filePath;
      this.lastCalledLine = line;
      this.lastCalledColumn = column;
      return this.responseToReturn;
    }

    public void setSuccessResponse(GetCursorPositionInfoResponse response) {
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

    public Path getLastCalledFilePath() {
      return this.lastCalledFilePath;
    }

    public Integer getLastCalledLine() {
      return this.lastCalledLine;
    }

    public Integer getLastCalledColumn() {
      return this.lastCalledColumn;
    }
  }
}
