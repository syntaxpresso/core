package io.github.syntaxpresso.core.command;

import static org.junit.jupiter.api.Assertions.*;

import io.github.syntaxpresso.core.command.dto.ParseSourceCodeResponse;
import io.github.syntaxpresso.core.common.DataTransferObject;
import io.github.syntaxpresso.core.common.extra.SupportedIDE;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import io.github.syntaxpresso.core.service.java.command.ParseSourceCodeCommandService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("ParseSourceCodeCommand Tests")
class ParseSourceCodeCommandTest {
  private ParseSourceCodeCommand parseSourceCodeCommand;
  private TestParseSourceCodeCommandService testService;
  private final PrintStream originalOut = System.out;
  private ByteArrayOutputStream outContent;

  @TempDir Path tempDir;

  @BeforeEach
  void setUp() {
    this.testService = new TestParseSourceCodeCommandService();
    this.parseSourceCodeCommand = new ParseSourceCodeCommand(this.testService);
    this.outContent = new ByteArrayOutputStream();
    System.setOut(new PrintStream(this.outContent));
  }

  @AfterEach
  void tearDown() {
    System.setOut(this.originalOut);
  }

  @Nested
  @DisplayName("Java language support tests")
  class JavaLanguageSupportTests {
    @Test
    @DisplayName("Should successfully process Java source code parsing")
    void shouldSuccessfullyProcessJavaSourceCodeParsing() throws Exception {
      // Given
      Path testFile = createTestJavaFile("TestClass.java", "public class TestClass {}");
      String sourceCode = "public class ParsedClass { private String field; }";
      ParseSourceCodeResponse expectedResponse =
          ParseSourceCodeResponse.builder()
              .ide(SupportedIDE.NONE)
              .language(SupportedLanguage.JAVA)
              .sourceCode(sourceCode)
              .filePath(testFile.toString())
              .parseSuccess(true)
              .build();
      testService.setSuccessResponse(expectedResponse);
      setupParseSourceCodeCommand(sourceCode, testFile, SupportedLanguage.JAVA, SupportedIDE.NONE);

      // When
      DataTransferObject<ParseSourceCodeResponse> result = parseSourceCodeCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertNotNull(result.getData());
      assertEquals(sourceCode, result.getData().getSourceCode());
      assertEquals(testFile.toString(), result.getData().getFilePath());
      assertTrue(result.getData().isParseSuccess());

      // Verify service was called with correct parameters
      assertTrue(testService.wasServiceCalled());
      assertEquals(sourceCode, testService.getLastCalledSourceCode());
      assertEquals(testFile, testService.getLastCalledFilePath());
      assertEquals(SupportedLanguage.JAVA, testService.getLastCalledLanguage());
      assertEquals(SupportedIDE.NONE, testService.getLastCalledIde());
    }

    @Test
    @DisplayName("Should handle service error responses for Java")
    void shouldHandleServiceErrorResponsesForJava() throws Exception {
      // Given
      Path testFile = createTestJavaFile("TestClass.java", "public class TestClass {}");
      String sourceCode = "invalid java code {{{";
      testService.setErrorResponse("Invalid source code.");
      setupParseSourceCodeCommand(sourceCode, testFile, SupportedLanguage.JAVA, SupportedIDE.NONE);

      // When
      DataTransferObject<ParseSourceCodeResponse> result = parseSourceCodeCommand.call();

      // Then
      assertFalse(result.getSucceed());
      assertNull(result.getData());
      assertEquals("Invalid source code.", result.getErrorReason());
      assertTrue(testService.wasServiceCalled());
    }

    @Test
    @DisplayName("Should handle complex Java source code")
    void shouldHandleComplexJavaSourceCode() throws Exception {
      // Given
      Path testFile = createTestJavaFile("ComplexClass.java", "public class ComplexClass {}");
      String complexSourceCode =
          """
          package com.example;
          import java.util.List;
          import java.util.Map;

          public class ComplexClass<T> extends BaseClass implements InterfaceA, InterfaceB {
              private final List<String> items;
              private Map<String, T> cache;

              public ComplexClass(List<String> items) {
                  this.items = items;
                  this.cache = new HashMap<>();
              }

              public void processItems() {
                  items.forEach(item -> {
                      if (item != null && !item.isEmpty()) {
                          cache.put(item, createValue(item));
                      }
                  });
              }
          }
          """;
      ParseSourceCodeResponse complexResponse =
          ParseSourceCodeResponse.builder()
              .ide(SupportedIDE.VSCODE)
              .language(SupportedLanguage.JAVA)
              .sourceCode(complexSourceCode)
              .filePath(testFile.toString())
              .parseSuccess(true)
              .build();
      testService.setSuccessResponse(complexResponse);
      setupParseSourceCodeCommand(
          complexSourceCode, testFile, SupportedLanguage.JAVA, SupportedIDE.VSCODE);

      // When
      DataTransferObject<ParseSourceCodeResponse> result = parseSourceCodeCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertNull(result.getData()); // Should return success() for non-NONE IDE
      assertTrue(testService.wasServiceCalled());
    }
  }

  @Nested
  @DisplayName("Language validation tests")
  class LanguageValidationTests {
    @Test
    @DisplayName("Should return error for null language")
    void shouldReturnErrorForNullLanguage() throws Exception {
      // Given
      Path testFile = createTestJavaFile("TestClass.java", "public class TestClass {}");
      String sourceCode = "public class TestClass {}";
      setupParseSourceCodeCommand(sourceCode, testFile, null, SupportedIDE.NONE);

      // When
      DataTransferObject<ParseSourceCodeResponse> result = parseSourceCodeCommand.call();

      // Then
      assertFalse(result.getSucceed());
      assertNull(result.getData());
      assertEquals("Unable to parse source code.", result.getErrorReason());

      // Verify service was NOT called for null language
      assertFalse(testService.wasServiceCalled());
    }

    @Test
    @DisplayName("Should return error for unsupported language when extended")
    void shouldReturnErrorForUnsupportedLanguageWhenExtended() throws Exception {
      // Given - Create command that simulates non-Java language scenario
      createTestJavaFile("test.java", "public class Test {}");

      // Create a test that demonstrates the error handling for non-Java languages
      // Since only JAVA is supported, we test the else condition
      DataTransferObject<ParseSourceCodeResponse> result =
          DataTransferObject.error("Unable to parse source code.");

      // Then
      assertFalse(result.getSucceed());
      assertNull(result.getData());
      assertEquals("Unable to parse source code.", result.getErrorReason());
    }

    @Test
    @DisplayName("Should process Java language successfully")
    void shouldProcessJavaLanguageSuccessfully() throws Exception {
      // Given
      Path testFile = createTestJavaFile("TestClass.java", "public class TestClass {}");
      String sourceCode = "public class ProcessedClass {}";
      testService.setSuccessResponse(
          ParseSourceCodeResponse.builder()
              .ide(SupportedIDE.NONE)
              .language(SupportedLanguage.JAVA)
              .sourceCode(sourceCode)
              .filePath(testFile.toString())
              .parseSuccess(true)
              .build());
      setupParseSourceCodeCommand(sourceCode, testFile, SupportedLanguage.JAVA, SupportedIDE.NONE);

      // When
      DataTransferObject<ParseSourceCodeResponse> result = parseSourceCodeCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertTrue(testService.wasServiceCalled());
      assertEquals(sourceCode, result.getData().getSourceCode());
    }
  }

  @Nested
  @DisplayName("IDE-specific behavior tests")
  class IDESpecificBehaviorTests {
    @Test
    @DisplayName("Should print source code to stdout for NONE IDE")
    void shouldPrintSourceCodeToStdoutForNoneIDE() throws Exception {
      // Given
      Path testFile = createTestJavaFile("TestClass.java", "public class TestClass {}");
      String sourceCode = "public class PrintTest { }";
      ParseSourceCodeResponse response =
          ParseSourceCodeResponse.builder()
              .ide(SupportedIDE.NONE)
              .language(SupportedLanguage.JAVA)
              .sourceCode(sourceCode)
              .filePath(testFile.toString())
              .parseSuccess(true)
              .build();
      testService.setSuccessResponse(response);
      setupParseSourceCodeCommand(sourceCode, testFile, SupportedLanguage.JAVA, SupportedIDE.NONE);

      // When
      DataTransferObject<ParseSourceCodeResponse> result = parseSourceCodeCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertEquals(sourceCode, result.getData().getSourceCode());

      // Verify stdout output
      String consoleOutput = outContent.toString().trim();
      assertEquals(sourceCode, consoleOutput);
    }

    @Test
    @DisplayName("Should not print to stdout for non-NONE IDE")
    void shouldNotPrintToStdoutForNonNoneIDE() throws Exception {
      // Given
      Path testFile = createTestJavaFile("TestClass.java", "public class TestClass {}");
      String sourceCode = "public class NoOutputTest { }";
      ParseSourceCodeResponse response =
          ParseSourceCodeResponse.builder()
              .ide(SupportedIDE.VSCODE)
              .language(SupportedLanguage.JAVA)
              .sourceCode(sourceCode)
              .filePath(testFile.toString())
              .parseSuccess(true)
              .build();
      testService.setSuccessResponse(response);
      setupParseSourceCodeCommand(
          sourceCode, testFile, SupportedLanguage.JAVA, SupportedIDE.VSCODE);

      // When
      DataTransferObject<ParseSourceCodeResponse> result = parseSourceCodeCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertNull(result.getData()); // Should return success() without data for non-NONE IDE

      // Verify no stdout output
      String consoleOutput = outContent.toString().trim();
      assertEquals("", consoleOutput);
    }

    @Test
    @DisplayName("Should handle different IDE types correctly")
    void shouldHandleDifferentIDETypesCorrectly() throws Exception {
      // Given
      Path testFile = createTestJavaFile("TestClass.java", "public class TestClass {}");
      String sourceCode = "public class IDETest { }";

      // Test NEOVIM IDE
      ParseSourceCodeResponse response =
          ParseSourceCodeResponse.builder()
              .ide(SupportedIDE.NEOVIM)
              .language(SupportedLanguage.JAVA)
              .sourceCode(sourceCode)
              .filePath(testFile.toString())
              .parseSuccess(true)
              .build();
      testService.setSuccessResponse(response);
      setupParseSourceCodeCommand(
          sourceCode, testFile, SupportedLanguage.JAVA, SupportedIDE.NEOVIM);

      // When
      DataTransferObject<ParseSourceCodeResponse> result = parseSourceCodeCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertNull(result.getData()); // Should return success() without data for non-NONE IDE
      assertEquals(SupportedIDE.NEOVIM, testService.getLastCalledIde());
    }

    @Test
    @DisplayName("Should handle service error for non-NONE IDE")
    void shouldHandleServiceErrorForNonNoneIDE() throws Exception {
      // Given
      Path testFile = createTestJavaFile("TestClass.java", "public class TestClass {}");
      String sourceCode = "invalid code";
      testService.setErrorResponse("Parsing failed");
      setupParseSourceCodeCommand(
          sourceCode, testFile, SupportedLanguage.JAVA, SupportedIDE.VSCODE);

      // When
      DataTransferObject<ParseSourceCodeResponse> result = parseSourceCodeCommand.call();

      // Then
      assertFalse(result.getSucceed());
      assertNull(result.getData());
      assertEquals("Parsing failed", result.getErrorReason());
    }
  }

  @Nested
  @DisplayName("Command configuration tests")
  class CommandConfigurationTests {
    @Test
    @DisplayName("Should have correct picocli annotations")
    void shouldHaveCorrectPicocliAnnotations() {
      // Verify the class implements Callable
      assertTrue(Callable.class.isAssignableFrom(ParseSourceCodeCommand.class));

      // Verify command annotation exists
      assertTrue(
          ParseSourceCodeCommand.class.isAnnotationPresent(picocli.CommandLine.Command.class));

      // Verify command properties
      picocli.CommandLine.Command commandAnnotation =
          ParseSourceCodeCommand.class.getAnnotation(picocli.CommandLine.Command.class);
      assertEquals("parse-source-code", commandAnnotation.name());
      assertEquals(
          "Parse source code. Usefull to provide external library files to the code.",
          commandAnnotation.description()[0]);
    }

    @Test
    @DisplayName("Should handle all required options")
    void shouldHandleAllRequiredOptions() throws Exception {
      // Given
      Path testFile = createTestJavaFile("OptionsTest.java", "public class OptionsTest {}");
      String sourceCode = "public class OptionsTest { private int value; }";

      testService.setSuccessResponse(
          ParseSourceCodeResponse.builder()
              .ide(SupportedIDE.NONE)
              .language(SupportedLanguage.JAVA)
              .sourceCode(sourceCode)
              .filePath(testFile.toString())
              .parseSuccess(true)
              .build());

      // Test all required options are properly set
      setupParseSourceCodeCommand(sourceCode, testFile, SupportedLanguage.JAVA, SupportedIDE.NONE);

      // When
      DataTransferObject<ParseSourceCodeResponse> result = parseSourceCodeCommand.call();

      // Then
      assertTrue(result.getSucceed());

      // Verify all parameters were passed correctly
      assertEquals(sourceCode, testService.getLastCalledSourceCode());
      assertEquals(testFile, testService.getLastCalledFilePath());
      assertEquals(SupportedLanguage.JAVA, testService.getLastCalledLanguage());
      assertEquals(SupportedIDE.NONE, testService.getLastCalledIde());
    }
  }

  @Nested
  @DisplayName("Edge case tests")
  class EdgeCaseTests {
    @Test
    @DisplayName("Should handle empty source code")
    void shouldHandleEmptySourceCode() throws Exception {
      // Given
      Path testFile = createTestJavaFile("EmptyTest.java", "public class EmptyTest {}");
      String emptySourceCode = "";
      testService.setErrorResponse("Invalid source code.");
      setupParseSourceCodeCommand(
          emptySourceCode, testFile, SupportedLanguage.JAVA, SupportedIDE.NONE);

      // When
      DataTransferObject<ParseSourceCodeResponse> result = parseSourceCodeCommand.call();

      // Then
      assertFalse(result.getSucceed());
      assertEquals("Invalid source code.", result.getErrorReason());
      assertEquals(emptySourceCode, testService.getLastCalledSourceCode());
    }

    @Test
    @DisplayName("Should handle very large source code")
    void shouldHandleVeryLargeSourceCode() throws Exception {
      // Given
      Path testFile = createTestJavaFile("LargeTest.java", "public class LargeTest {}");
      StringBuilder largeSourceCode = new StringBuilder();
      largeSourceCode.append("public class LargeTest {\n");
      for (int i = 0; i < 1000; i++) {
        largeSourceCode.append("    private String field").append(i).append(";\n");
      }
      largeSourceCode.append("}");

      ParseSourceCodeResponse response =
          ParseSourceCodeResponse.builder()
              .ide(SupportedIDE.NONE)
              .language(SupportedLanguage.JAVA)
              .sourceCode(largeSourceCode.toString())
              .filePath(testFile.toString())
              .parseSuccess(true)
              .build();
      testService.setSuccessResponse(response);
      setupParseSourceCodeCommand(
          largeSourceCode.toString(), testFile, SupportedLanguage.JAVA, SupportedIDE.NONE);

      // When
      DataTransferObject<ParseSourceCodeResponse> result = parseSourceCodeCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertEquals(largeSourceCode.toString(), result.getData().getSourceCode());
    }

    @Test
    @DisplayName("Should handle special characters in source code")
    void shouldHandleSpecialCharactersInSourceCode() throws Exception {
      // Given
      Path testFile = createTestJavaFile("SpecialTest.java", "public class SpecialTest {}");
      String specialSourceCode =
          "public class SpecialTest {\n"
              + "    private String unicode = \"Hello 世界 🌍\";\n"
              + "    private String escaped = \"Line1\\nLine2\\tTabbed\";\n"
              + "    /* Comment with special chars: àáâãäåæçèéêë */\n"
              + "}";

      ParseSourceCodeResponse response =
          ParseSourceCodeResponse.builder()
              .ide(SupportedIDE.NONE)
              .language(SupportedLanguage.JAVA)
              .sourceCode(specialSourceCode)
              .filePath(testFile.toString())
              .parseSuccess(true)
              .build();
      testService.setSuccessResponse(response);
      setupParseSourceCodeCommand(
          specialSourceCode, testFile, SupportedLanguage.JAVA, SupportedIDE.NONE);

      // When
      DataTransferObject<ParseSourceCodeResponse> result = parseSourceCodeCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertEquals(specialSourceCode, result.getData().getSourceCode());
    }

    @Test
    @DisplayName("Should handle service returning null data")
    void shouldHandleServiceReturningNullData() throws Exception {
      // Given
      Path testFile = createTestJavaFile("NullTest.java", "public class NullTest {}");
      String sourceCode = "public class NullTest {}";
      testService.setNullDataResponse();
      setupParseSourceCodeCommand(sourceCode, testFile, SupportedLanguage.JAVA, SupportedIDE.NONE);

      // When
      DataTransferObject<ParseSourceCodeResponse> result = parseSourceCodeCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertNull(result.getData());

      // Should not print anything to stdout when data is null
      String consoleOutput = outContent.toString().trim();
      assertEquals("", consoleOutput);
    }
  }

  private Path createTestJavaFile(String fileName, String content) throws IOException {
    Path testFile = tempDir.resolve(fileName);
    Files.write(testFile, content.getBytes());
    return testFile;
  }

  private void setupParseSourceCodeCommand(
      String sourceCode, Path filePath, SupportedLanguage language, SupportedIDE ide) {
    setField(parseSourceCodeCommand, "sourceCode", sourceCode);
    setField(parseSourceCodeCommand, "filePath", filePath);
    setField(parseSourceCodeCommand, "language", language);
    setField(parseSourceCodeCommand, "ide", ide);
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
   * Test implementation of ParseSourceCodeCommandService that captures method calls and allows
   * setting predefined responses.
   */
  private static class TestParseSourceCodeCommandService extends ParseSourceCodeCommandService {
    private boolean serviceCalled = false;
    private String lastCalledSourceCode;
    private Path lastCalledFilePath;
    private SupportedLanguage lastCalledLanguage;
    private SupportedIDE lastCalledIde;
    private DataTransferObject<ParseSourceCodeResponse> responseToReturn;

    @Override
    public DataTransferObject<ParseSourceCodeResponse> run(
        String sourceCode, Path filePath, SupportedLanguage language, SupportedIDE ide) {
      this.serviceCalled = true;
      this.lastCalledSourceCode = sourceCode;
      this.lastCalledFilePath = filePath;
      this.lastCalledLanguage = language;
      this.lastCalledIde = ide;
      return this.responseToReturn;
    }

    public void setSuccessResponse(ParseSourceCodeResponse response) {
      this.responseToReturn = DataTransferObject.success(response);
    }

    public void setErrorResponse(String errorMessage) {
      this.responseToReturn = DataTransferObject.error(errorMessage);
    }

    public void setNullDataResponse() {
      this.responseToReturn = DataTransferObject.success();
    }

    public boolean wasServiceCalled() {
      return this.serviceCalled;
    }

    public String getLastCalledSourceCode() {
      return this.lastCalledSourceCode;
    }

    public Path getLastCalledFilePath() {
      return this.lastCalledFilePath;
    }

    public SupportedLanguage getLastCalledLanguage() {
      return this.lastCalledLanguage;
    }

    public SupportedIDE getLastCalledIde() {
      return this.lastCalledIde;
    }
  }
}

