package io.github.syntaxpresso.core.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.syntaxpresso.core.command.dto.ParseSourceCodeResponse;
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
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ParseSourceCodeCommand Tests")
class ParseSourceCodeCommandTest {
  private JavaCommandService javaCommandService;
  private ParseSourceCodeCommand command;

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

    this.javaCommandService = new JavaCommandService(pathHelper, javaLanguageService);
  }

  private void setupCommand(
      String sourceCode, Path filePath, SupportedLanguage language, SupportedIDE ide) {
    this.command = new ParseSourceCodeCommand(javaCommandService);
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
        assertEquals(SupportedIDE.NONE, result.getData().getIde());
        assertTrue(result.getData().isParseSuccess());

        // Verify that source code was printed to System.out
        assertEquals(sourceCode + System.lineSeparator(), outputStream.toString());
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
        assertNull(result.getData()); // Should return success without data when IDE is not NONE
        assertEquals("", outputStream.toString()); // No output should be printed
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

      setupCommand(sourceCode, filePath, SupportedLanguage.JAVA, SupportedIDE.NEOVIM);

      // When
      DataTransferObject<ParseSourceCodeResponse> result = command.call();

      // Then
      assertTrue(result.getSucceed());
      assertNull(result.getData()); // Should return success without data when IDE is not NONE
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
    @DisplayName("should handle invalid Java source code gracefully")
    void call_withInvalidJavaSourceCode_shouldStillSucceed() {
      // Given - The current implementation doesn't validate Java syntax, it just wraps the input
      String sourceCode = "invalid java code {{{";
      Path filePath = Path.of("/test/Invalid.java");

      setupCommand(sourceCode, filePath, SupportedLanguage.JAVA, SupportedIDE.NONE);

      // Capture System.out
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      PrintStream originalOut = System.out;
      System.setOut(new PrintStream(outputStream));

      try {
        // When
        DataTransferObject<ParseSourceCodeResponse> result = command.call();

        // Then - The service doesn't actually parse/validate, so it succeeds
        assertTrue(result.getSucceed());
        assertNotNull(result.getData());
        assertEquals(sourceCode, result.getData().getSourceCode());
        assertEquals(sourceCode + System.lineSeparator(), outputStream.toString());
      } finally {
        System.setOut(originalOut);
      }
    }
  }

  @Nested
  @DisplayName("call() with unsupported language")
  class CallWithUnsupportedLanguageTests {

    @Test
    @DisplayName("should return error for null language")
    void call_withNullLanguage_shouldReturnError() {
      // Given
      String sourceCode = "some source code";
      Path filePath = Path.of("/test/file.txt");

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

      setupCommand(sourceCode, filePath, SupportedLanguage.JAVA, SupportedIDE.NONE);

      // When
      DataTransferObject<ParseSourceCodeResponse> result = command.call();

      // Then
      assertTrue(result.getSucceed());
      assertNotNull(result.getData());
      assertEquals(sourceCode, result.getData().getSourceCode());
      assertNull(result.getData().getFilePath());
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
