package io.github.syntaxpresso.core.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.syntaxpresso.core.command.dto.ParseSourceCodeResponse;
import io.github.syntaxpresso.core.common.DataTransferObject;
import io.github.syntaxpresso.core.common.extra.SupportedIDE;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import io.github.syntaxpresso.core.service.java.JavaCommandService;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ParseSourceCodeCommand Tests")
class ParseSourceCodeCommandTest {
  private JavaCommandService mockJavaCommandService;
  private ParseSourceCodeCommand command;

  @BeforeEach
  void setUp() {
    this.mockJavaCommandService = mock(JavaCommandService.class);
  }

  private void setupCommand(
      String sourceCode, Path filePath, SupportedLanguage language, SupportedIDE ide) {
    this.command = new ParseSourceCodeCommand(mockJavaCommandService);
    // Use reflection to set private fields since they're @Option annotated
    try {
      var sourceCodeField = ParseSourceCodeCommand.class.getDeclaredField("sourceCode");
      sourceCodeField.setAccessible(true);
      sourceCodeField.set(command, sourceCode);

      var filePathField = ParseSourceCodeCommand.class.getDeclaredField("filePath");
      filePathField.setAccessible(true);
      filePathField.set(command, filePath);

      var languageField = ParseSourceCodeCommand.class.getDeclaredField("language");
      languageField.setAccessible(true);
      languageField.set(command, language);

      var ideField = ParseSourceCodeCommand.class.getDeclaredField("ide");
      ideField.setAccessible(true);
      ideField.set(command, ide);
    } catch (Exception e) {
      throw new RuntimeException("Failed to setup command fields", e);
    }
  }

  @Nested
  @DisplayName("call() with Java language")
  class CallWithJavaLanguageTests {

    @Test
    @DisplayName("should parse Java source code successfully with IDE NONE")
    void call_withJavaLanguageAndIDENone_shouldParseSuccessfully() {
      // Given
      String sourceCode = "public class Test { private String name; }";
      Path filePath = Path.of("/test/Test.java");
      ParseSourceCodeResponse mockResponse =
          ParseSourceCodeResponse.builder()
              .ide(SupportedIDE.NONE)
              .language(SupportedLanguage.JAVA)
              .sourceCode(sourceCode)
              .filePath(filePath.toString())
              .parseSuccess(true)
              .build();

      when(mockJavaCommandService.parseSourceCommand(
              eq(sourceCode), eq(filePath), eq(SupportedLanguage.JAVA), eq(SupportedIDE.NONE)))
          .thenReturn(DataTransferObject.success(mockResponse));

      setupCommand(sourceCode, filePath, SupportedLanguage.JAVA, SupportedIDE.NONE);

      // Capture System.out
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      PrintStream originalOut = System.out;
      System.setOut(new PrintStream(outputStream));

      try {
        // When
        DataTransferObject<ParseSourceCodeResponse> result = command.call();

        // Then
        assertTrue(result.getSucceed());
        assertNotNull(result.getData());
        assertEquals(sourceCode, result.getData().getSourceCode());
        assertEquals(filePath.toString(), result.getData().getFilePath());
        assertEquals(SupportedLanguage.JAVA, result.getData().getLanguage());
        assertEquals(SupportedIDE.NONE, result.getData().getIde());
        assertTrue(result.getData().isParseSuccess());

        // Verify that source code was printed to System.out
        assertEquals(sourceCode + System.lineSeparator(), outputStream.toString());
        verify(mockJavaCommandService)
            .parseSourceCommand(sourceCode, filePath, SupportedLanguage.JAVA, SupportedIDE.NONE);
      } finally {
        System.setOut(originalOut);
      }
    }

    @Test
    @DisplayName("should parse Java source code successfully with IDE VSCODE")
    void call_withJavaLanguageAndIDEVSCode_shouldParseSuccessfully() {
      // Given
      String sourceCode = "public class Test { private int value; }";
      Path filePath = Path.of("/test/Test.java");
      ParseSourceCodeResponse mockResponse =
          ParseSourceCodeResponse.builder()
              .ide(SupportedIDE.VSCODE)
              .language(SupportedLanguage.JAVA)
              .sourceCode(sourceCode)
              .filePath(filePath.toString())
              .parseSuccess(true)
              .build();

      when(mockJavaCommandService.parseSourceCommand(
              eq(sourceCode), eq(filePath), eq(SupportedLanguage.JAVA), eq(SupportedIDE.VSCODE)))
          .thenReturn(DataTransferObject.success(mockResponse));

      setupCommand(sourceCode, filePath, SupportedLanguage.JAVA, SupportedIDE.VSCODE);

      // Capture System.out to ensure nothing is printed
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      PrintStream originalOut = System.out;
      System.setOut(new PrintStream(outputStream));

      try {
        // When
        DataTransferObject<ParseSourceCodeResponse> result = command.call();

        // Then
        assertTrue(result.getSucceed());
        // Should return success without data when IDE is not NONE
        assertEquals("", outputStream.toString()); // No output should be printed
        verify(mockJavaCommandService)
            .parseSourceCommand(sourceCode, filePath, SupportedLanguage.JAVA, SupportedIDE.VSCODE);
      } finally {
        System.setOut(originalOut);
      }
    }

    @Test
    @DisplayName("should parse Java source code successfully with IDE NEOVIM")
    void call_withJavaLanguageAndIDENeovim_shouldParseSuccessfully() {
      // Given
      String sourceCode = "public interface TestInterface { void method(); }";
      Path filePath = Path.of("/test/TestInterface.java");
      ParseSourceCodeResponse mockResponse =
          ParseSourceCodeResponse.builder()
              .ide(SupportedIDE.NEOVIM)
              .language(SupportedLanguage.JAVA)
              .sourceCode(sourceCode)
              .filePath(filePath.toString())
              .parseSuccess(true)
              .build();

      when(mockJavaCommandService.parseSourceCommand(
              eq(sourceCode), eq(filePath), eq(SupportedLanguage.JAVA), eq(SupportedIDE.NEOVIM)))
          .thenReturn(DataTransferObject.success(mockResponse));

      setupCommand(sourceCode, filePath, SupportedLanguage.JAVA, SupportedIDE.NEOVIM);

      // When
      DataTransferObject<ParseSourceCodeResponse> result = command.call();

      // Then
      assertTrue(result.getSucceed());
      verify(mockJavaCommandService)
          .parseSourceCommand(sourceCode, filePath, SupportedLanguage.JAVA, SupportedIDE.NEOVIM);
    }

    @Test
    @DisplayName("should handle complex Java source code successfully")
    void call_withComplexJavaSourceCode_shouldParseSuccessfully() {
      // Given
      String complexSourceCode =
          """
          package com.example.test;
          import java.util.List;
          import java.util.ArrayList;
          
          public class ComplexClass {
              private String name;
              private List<Integer> numbers;
              
              public ComplexClass(String name) {
                  this.name = name;
                  this.numbers = new ArrayList<>();
              }
              
              public void addNumber(int number) {
                  numbers.add(number);
              }
              
              public List<Integer> getNumbers() {
                  return new ArrayList<>(numbers);
              }
          }
          """;
      Path filePath = Path.of("/src/main/java/com/example/test/ComplexClass.java");
      ParseSourceCodeResponse mockResponse =
          ParseSourceCodeResponse.builder()
              .ide(SupportedIDE.NONE)
              .language(SupportedLanguage.JAVA)
              .sourceCode(complexSourceCode)
              .filePath(filePath.toString())
              .parseSuccess(true)
              .build();

      when(mockJavaCommandService.parseSourceCommand(
              eq(complexSourceCode),
              eq(filePath),
              eq(SupportedLanguage.JAVA),
              eq(SupportedIDE.NONE)))
          .thenReturn(DataTransferObject.success(mockResponse));

      setupCommand(complexSourceCode, filePath, SupportedLanguage.JAVA, SupportedIDE.NONE);

      // Capture System.out
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      PrintStream originalOut = System.out;
      System.setOut(new PrintStream(outputStream));

      try {
        // When
        DataTransferObject<ParseSourceCodeResponse> result = command.call();

        // Then
        assertTrue(result.getSucceed());
        assertNotNull(result.getData());
        assertEquals(complexSourceCode, result.getData().getSourceCode());
        assertEquals(filePath.toString(), result.getData().getFilePath());
        assertTrue(result.getData().isParseSuccess());

        // Verify that source code was printed
        assertEquals(complexSourceCode + System.lineSeparator(), outputStream.toString());
      } finally {
        System.setOut(originalOut);
      }
    }

    @Test
    @DisplayName("should handle parsing failure from service")
    void call_withParsingFailure_shouldReturnError() {
      // Given
      String sourceCode = "invalid java code {{{";
      Path filePath = Path.of("/test/Invalid.java");

      when(mockJavaCommandService.parseSourceCommand(
              eq(sourceCode), eq(filePath), eq(SupportedLanguage.JAVA), eq(SupportedIDE.NONE)))
          .thenReturn(DataTransferObject.error("Parsing failed due to syntax errors"));

      setupCommand(sourceCode, filePath, SupportedLanguage.JAVA, SupportedIDE.NONE);

      // When
      DataTransferObject<ParseSourceCodeResponse> result = command.call();

      // Then
      assertFalse(result.getSucceed());
      assertEquals("Parsing failed due to syntax errors", result.getErrorReason());
      verify(mockJavaCommandService)
          .parseSourceCommand(sourceCode, filePath, SupportedLanguage.JAVA, SupportedIDE.NONE);
    }
  }

  @Nested
  @DisplayName("call() with unsupported language")
  class CallWithUnsupportedLanguageTests {

    @Test
    @DisplayName("should return error for unsupported language")
    void call_withUnsupportedLanguage_shouldReturnError() {
      // Given - In reality we only have JAVA, but let's simulate if we had other languages
      // that are not supported by this command
      String sourceCode = "some source code";
      Path filePath = Path.of("/test/file.txt");

      // We'll simulate this by not setting language to JAVA
      // Since we can't create other SupportedLanguage values, we'll test the error path
      // by using null language (though this wouldn't happen in practice due to picocli validation)
      setupCommand(sourceCode, filePath, null, SupportedIDE.NONE);

      // When
      DataTransferObject<ParseSourceCodeResponse> result = command.call();

      // Then
      assertFalse(result.getSucceed());
      assertEquals("Unable to parse source code.", result.getErrorReason());
    }
  }

  @Nested
  @DisplayName("Edge Cases")
  class EdgeCaseTests {

    @Test
    @DisplayName("should handle empty source code")
    void call_withEmptySourceCode_shouldHandleGracefully() {
      // Given
      String emptySourceCode = "";
      Path filePath = Path.of("/test/Empty.java");
      ParseSourceCodeResponse mockResponse =
          ParseSourceCodeResponse.builder()
              .ide(SupportedIDE.NONE)
              .language(SupportedLanguage.JAVA)
              .sourceCode(emptySourceCode)
              .filePath(filePath.toString())
              .parseSuccess(true)
              .build();

      when(mockJavaCommandService.parseSourceCommand(
              eq(emptySourceCode),
              eq(filePath),
              eq(SupportedLanguage.JAVA),
              eq(SupportedIDE.NONE)))
          .thenReturn(DataTransferObject.success(mockResponse));

      setupCommand(emptySourceCode, filePath, SupportedLanguage.JAVA, SupportedIDE.NONE);

      // When
      DataTransferObject<ParseSourceCodeResponse> result = command.call();

      // Then
      assertTrue(result.getSucceed());
      assertNotNull(result.getData());
      assertEquals(emptySourceCode, result.getData().getSourceCode());
    }

    @Test
    @DisplayName("should handle null file path")
    void call_withNullFilePath_shouldHandleGracefully() {
      // Given
      String sourceCode = "public class Test {}";
      Path filePath = null;
      ParseSourceCodeResponse mockResponse =
          ParseSourceCodeResponse.builder()
              .ide(SupportedIDE.NONE)
              .language(SupportedLanguage.JAVA)
              .sourceCode(sourceCode)
              .filePath(null)
              .parseSuccess(true)
              .build();

      when(mockJavaCommandService.parseSourceCommand(
              eq(sourceCode), eq(filePath), eq(SupportedLanguage.JAVA), eq(SupportedIDE.NONE)))
          .thenReturn(DataTransferObject.success(mockResponse));

      setupCommand(sourceCode, filePath, SupportedLanguage.JAVA, SupportedIDE.NONE);

      // When
      DataTransferObject<ParseSourceCodeResponse> result = command.call();

      // Then
      assertTrue(result.getSucceed());
      assertNotNull(result.getData());
      assertEquals(sourceCode, result.getData().getSourceCode());
      assertEquals(null, result.getData().getFilePath());
    }

    @Test
    @DisplayName("should handle very large source code")
    void call_withLargeSourceCode_shouldHandleGracefully() {
      // Given
      StringBuilder largeSourceCode = new StringBuilder("public class LargeClass {\n");
      for (int i = 0; i < 1000; i++) {
        largeSourceCode.append("    private String field").append(i).append(";\n");
      }
      largeSourceCode.append("}");

      String sourceCode = largeSourceCode.toString();
      Path filePath = Path.of("/test/LargeClass.java");
      ParseSourceCodeResponse mockResponse =
          ParseSourceCodeResponse.builder()
              .ide(SupportedIDE.NONE)
              .language(SupportedLanguage.JAVA)
              .sourceCode(sourceCode)
              .filePath(filePath.toString())
              .parseSuccess(true)
              .build();

      when(mockJavaCommandService.parseSourceCommand(
              eq(sourceCode), eq(filePath), eq(SupportedLanguage.JAVA), eq(SupportedIDE.NONE)))
          .thenReturn(DataTransferObject.success(mockResponse));

      setupCommand(sourceCode, filePath, SupportedLanguage.JAVA, SupportedIDE.NONE);

      // When
      DataTransferObject<ParseSourceCodeResponse> result = command.call();

      // Then
      assertTrue(result.getSucceed());
      assertNotNull(result.getData());
      assertEquals(sourceCode, result.getData().getSourceCode());
      assertTrue(result.getData().isParseSuccess());
    }
  }
}