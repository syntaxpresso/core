package io.github.syntaxpresso.core.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.syntaxpresso.core.command.dto.GetCursorPositionInfoResponse;
import io.github.syntaxpresso.core.command.extra.JavaSourceDirectoryType;
import io.github.syntaxpresso.core.common.DataTransferObject;
import io.github.syntaxpresso.core.common.extra.SupportedIDE;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import io.github.syntaxpresso.core.service.java.JavaCommandService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("JavaCommandService Tests")
class JavaCommandServiceTest {
  private JavaCommandService javaCommandService;

  @BeforeEach
  void setUp() {
    this.javaCommandService = new JavaCommandService();
  }

  @Nested
  @DisplayName("isJavaProject()")
  class IsJavaProjectTests {
    @Test
    @DisplayName("should return true for a directory with build.gradle")
    void isJavaProject_withGradleBuildFile_shouldReturnTrue(@TempDir Path tempDir)
        throws IOException {
      Files.createFile(tempDir.resolve("build.gradle"));
      assertTrue(javaCommandService.isJavaProject(tempDir.toFile()));
    }

    @Test
    @DisplayName("should return true for a directory with build.gradle.kts")
    void isJavaProject_withGradleKtsBuildFile_shouldReturnTrue(@TempDir Path tempDir)
        throws IOException {
      Files.createFile(tempDir.resolve("build.gradle.kts"));
      assertTrue(javaCommandService.isJavaProject(tempDir.toFile()));
    }

    @Test
    @DisplayName("should return true for a directory with pom.xml")
    void isJavaProject_withMavenBuildFile_shouldReturnTrue(@TempDir Path tempDir)
        throws IOException {
      Files.createFile(tempDir.resolve("pom.xml"));
      assertTrue(javaCommandService.isJavaProject(tempDir.toFile()));
    }

    @Test
    @DisplayName("should return true for a directory with src/main/java structure")
    void isJavaProject_withSrcMainJava_shouldReturnTrue(@TempDir Path tempDir) throws IOException {
      Files.createDirectories(tempDir.resolve("src/main/java"));
      assertTrue(javaCommandService.isJavaProject(tempDir.toFile()));
    }

    @Test
    @DisplayName("should return false for a directory that is not a Java project")
    void isJavaProject_notAJavaProject_shouldReturnFalse(@TempDir Path tempDir) {
      assertFalse(javaCommandService.isJavaProject(tempDir.toFile()));
    }
  }

  @Nested
  @DisplayName("findFilePath()")
  class FindFilePathTests {
    @Test
    @DisplayName("should find and create the correct main file path")
    void findFilePath_forMain_shouldReturnCorrectPath(@TempDir Path tempDir) throws IOException {
      String packageName = "com.example";
      Optional<Path> result =
          javaCommandService.findFilePath(tempDir, packageName, JavaSourceDirectoryType.MAIN);
      assertTrue(result.isPresent());
      assertTrue(result.get().endsWith(Path.of("src", "main", "java", "com", "example")));
      assertTrue(Files.isDirectory(result.get()));
    }

    @Test
    @DisplayName("should find and create the correct test file path")
    void findFilePath_forTest_shouldReturnCorrectPath(@TempDir Path tempDir) throws IOException {
      String packageName = "com.example.test";
      Optional<Path> result =
          javaCommandService.findFilePath(tempDir, packageName, JavaSourceDirectoryType.TEST);
      assertTrue(result.isPresent());
      assertTrue(result.get().endsWith(Path.of("src", "test", "java", "com", "example", "test")));
      assertTrue(Files.isDirectory(result.get()));
    }

    @Test
    @DisplayName("should return empty if root directory is not valid")
    void findFilePath_withInvalidRootDir_shouldReturnEmpty() {
      Path invalidPath = Path.of("non_existent_directory");
      String packageName = "com.example";
      Optional<Path> result =
          javaCommandService.findFilePath(invalidPath, packageName, JavaSourceDirectoryType.MAIN);
      assertFalse(result.isPresent());
    }
  }

  @Nested
  @DisplayName("getTextFromCursorPosition()")
  class GetTextFromCursorPositionTests {
    @Test
    @DisplayName("should return class name when cursor is on class identifier")
    void getTextFromCursorPosition_onClassIdentifier_shouldReturnClassName(@TempDir Path tempDir)
        throws IOException {
      String sourceCode =
          """
          package com.example;
          public class MyClass {
              private String name;
              public void setName(String name) {
                  this.name = name;
              }
          }
          """;
      Path filePath = tempDir.resolve("MyClass.java");
      Files.writeString(filePath, sourceCode);
      DataTransferObject<GetCursorPositionInfoResponse> result =
          javaCommandService.getTextFromCursorPosition(
              filePath, SupportedLanguage.JAVA, SupportedIDE.NONE, 2, 14);
      assertTrue(result.getSucceed());
      assertNotNull(result.getData());
      assertEquals("MyClass", result.getData().getNodeText());
      assertEquals(filePath.toString(), result.getData().getFilePath());
      assertEquals(SupportedLanguage.JAVA, result.getData().getLanguage());
      assertNotNull(result.getData().getNode());
    }

    @Test
    @DisplayName("should return method name when cursor is on method identifier")
    void getTextFromCursorPosition_onMethodIdentifier_shouldReturnMethodName(@TempDir Path tempDir)
        throws IOException {
      String sourceCode =
          """
          package com.example;
          public class MyClass {
              public void setName(String name) {
                  // method body
              }
          }
          """;
      Path filePath = tempDir.resolve("MyClass.java");
      Files.writeString(filePath, sourceCode);
      DataTransferObject<GetCursorPositionInfoResponse> result =
          javaCommandService.getTextFromCursorPosition(
              filePath, SupportedLanguage.JAVA, SupportedIDE.NONE, 3, 17);
      assertTrue(result.getSucceed());
      assertNotNull(result.getData());
      assertEquals("setName", result.getData().getNodeText());
      assertEquals(filePath.toString(), result.getData().getFilePath());
      assertEquals(SupportedLanguage.JAVA, result.getData().getLanguage());
      assertNotNull(result.getData().getNode());
    }

    @Test
    @DisplayName("should return field name when cursor is on field identifier")
    void getTextFromCursorPosition_onFieldIdentifier_shouldReturnFieldName(@TempDir Path tempDir)
        throws IOException {
      String sourceCode =
          """
          package com.example;
          public class MyClass {
              private String userName;
              private int age;
          }
          """;
      Path filePath = tempDir.resolve("MyClass.java");
      Files.writeString(filePath, sourceCode);
      DataTransferObject<GetCursorPositionInfoResponse> result =
          javaCommandService.getTextFromCursorPosition(
              filePath, SupportedLanguage.JAVA, SupportedIDE.NONE, 3, 24);
      assertTrue(result.getSucceed());
      assertNotNull(result.getData());
      assertEquals("userName", result.getData().getNodeText());
      assertEquals(filePath.toString(), result.getData().getFilePath());
      assertEquals(SupportedLanguage.JAVA, result.getData().getLanguage());
      assertNotNull(result.getData().getNode());
    }

    @Test
    @DisplayName("should return parameter name when cursor is on parameter identifier")
    void getTextFromCursorPosition_onParameterIdentifier_shouldReturnParameterName(
        @TempDir Path tempDir) throws IOException {
      String sourceCode =
          """
          package com.example;
          public class MyClass {
              public void setName(String newName) {
                  // method body
              }
          }
          """;
      Path filePath = tempDir.resolve("MyClass.java");
      Files.writeString(filePath, sourceCode);
      DataTransferObject<GetCursorPositionInfoResponse> result =
          javaCommandService.getTextFromCursorPosition(
              filePath, SupportedLanguage.JAVA, SupportedIDE.NONE, 3, 35);
      assertTrue(result.getSucceed());
      assertNotNull(result.getData());
      assertEquals("newName", result.getData().getNodeText());
      assertEquals(filePath.toString(), result.getData().getFilePath());
      assertEquals(SupportedLanguage.JAVA, result.getData().getLanguage());
      assertNotNull(result.getData().getNode());
    }

    @Test
    @DisplayName("should return local variable name when cursor is on local variable identifier")
    void getTextFromCursorPosition_onLocalVariableIdentifier_shouldReturnVariableName(
        @TempDir Path tempDir) throws IOException {
      String sourceCode =
          """
          package com.example;
          public class MyClass {
              public void doSomething() {
                  String localVar = "test";
                  System.out.println(localVar);
              }
          }
          """;
      Path filePath = tempDir.resolve("MyClass.java");
      Files.writeString(filePath, sourceCode);
      DataTransferObject<GetCursorPositionInfoResponse> result =
          javaCommandService.getTextFromCursorPosition(
              filePath, SupportedLanguage.JAVA, SupportedIDE.NONE, 4, 20);
      assertTrue(result.getSucceed());
      assertNotNull(result.getData());
      assertEquals("localVar", result.getData().getNodeText());
      assertEquals(filePath.toString(), result.getData().getFilePath());
      assertEquals(SupportedLanguage.JAVA, result.getData().getLanguage());
      assertNotNull(result.getData().getNode());
    }

    @Test
    @DisplayName("should return error when file does not exist")
    void getTextFromCursorPosition_fileDoesNotExist_shouldReturnError(@TempDir Path tempDir) {
      Path nonExistentFile = tempDir.resolve("NonExistent.java");
      DataTransferObject<GetCursorPositionInfoResponse> result =
          javaCommandService.getTextFromCursorPosition(
              nonExistentFile, SupportedLanguage.JAVA, SupportedIDE.NONE, 1, 1);
      assertFalse(result.getSucceed());
      assertEquals("File does not exist: " + nonExistentFile, result.getErrorReason());
    }

    @Test
    @DisplayName("should return error when file is not a Java file")
    void getTextFromCursorPosition_notJavaFile_shouldReturnError(@TempDir Path tempDir)
        throws IOException {
      Path textFile = tempDir.resolve("NotJava.txt");
      Files.writeString(textFile, "Some content");
      DataTransferObject<GetCursorPositionInfoResponse> result =
          javaCommandService.getTextFromCursorPosition(
              textFile, SupportedLanguage.JAVA, SupportedIDE.NONE, 1, 1);
      assertFalse(result.getSucceed());
      assertEquals("File is not a .java file: " + textFile, result.getErrorReason());
    }

    @Test
    @DisplayName("should return error when no symbol found at position")
    void getTextFromCursorPosition_noSymbolAtPosition_shouldReturnError(@TempDir Path tempDir)
        throws IOException {
      String sourceCode =
          """
          package com.example;
          public class MyClass {
          }
          """;
      Path filePath = tempDir.resolve("MyClass.java");
      Files.writeString(filePath, sourceCode);
      DataTransferObject<GetCursorPositionInfoResponse> result =
          javaCommandService.getTextFromCursorPosition(
              filePath, SupportedLanguage.JAVA, SupportedIDE.NONE, 99, 99);
      assertFalse(result.getSucceed());
      assertEquals("No symbol found at the specified position.", result.getErrorReason());
    }

    @Test
    @DisplayName("should handle different IDE positioning correctly")
    void getTextFromCursorPosition_differentIDE_shouldHandlePositioningCorrectly(
        @TempDir Path tempDir) throws IOException {
      String sourceCode =
          """
          package com.example;
          public class MyClass {
          }
          """;
      Path filePath = tempDir.resolve("MyClass.java");
      Files.writeString(filePath, sourceCode);
      // Test with different IDE settings
      DataTransferObject<GetCursorPositionInfoResponse> resultVSCode =
          javaCommandService.getTextFromCursorPosition(
              filePath, SupportedLanguage.JAVA, SupportedIDE.VSCODE, 2, 13);
      DataTransferObject<GetCursorPositionInfoResponse> resultNeovim =
          javaCommandService.getTextFromCursorPosition(
              filePath, SupportedLanguage.JAVA, SupportedIDE.NEOVIM, 2, 13);
      // Both should succeed for a reasonable position
      assertTrue(resultVSCode.getSucceed());
      assertTrue(resultNeovim.getSucceed());
      assertEquals("MyClass", resultVSCode.getData().getNodeText());
      assertEquals("MyClass", resultNeovim.getData().getNodeText());
      assertEquals(SupportedLanguage.JAVA, resultVSCode.getData().getLanguage());
      assertEquals(SupportedLanguage.JAVA, resultNeovim.getData().getLanguage());
    }

    @Test
    @DisplayName("should handle complex class with multiple members")
    void getTextFromCursorPosition_complexClass_shouldReturnCorrectText(@TempDir Path tempDir)
        throws IOException {
      String sourceCode =
          """
          package com.example.project;
          import java.util.List;
          public class ComplexClass {
              private String field1;
              private int field2;
              public ComplexClass(String field1, int field2) {
                  this.field1 = field1;
                  this.field2 = field2;
              }
              public String getField1() {
                  return field1;
              }
              public void setField1(String field1) {
                  this.field1 = field1;
              }
          }
          """;
      Path filePath = tempDir.resolve("ComplexClass.java");
      Files.writeString(filePath, sourceCode);
      // Test class name
      DataTransferObject<GetCursorPositionInfoResponse> classResult =
          javaCommandService.getTextFromCursorPosition(
              filePath, SupportedLanguage.JAVA, SupportedIDE.NONE, 3, 20);
      assertTrue(classResult.getSucceed());
      assertEquals("ComplexClass", classResult.getData().getNodeText());
      assertEquals(SupportedLanguage.JAVA, classResult.getData().getLanguage());
      // Test field name
      DataTransferObject<GetCursorPositionInfoResponse> fieldResult =
          javaCommandService.getTextFromCursorPosition(
              filePath, SupportedLanguage.JAVA, SupportedIDE.NONE, 4, 24);
      assertTrue(fieldResult.getSucceed());
      assertEquals("field1", fieldResult.getData().getNodeText());
      assertEquals(SupportedLanguage.JAVA, fieldResult.getData().getLanguage());
      // Test method name
      DataTransferObject<GetCursorPositionInfoResponse> methodResult =
          javaCommandService.getTextFromCursorPosition(
              filePath, SupportedLanguage.JAVA, SupportedIDE.NONE, 10, 25);
      assertTrue(methodResult.getSucceed());
      assertEquals("getField1", methodResult.getData().getNodeText());
      assertEquals(SupportedLanguage.JAVA, methodResult.getData().getLanguage());
    }
  }

  @Nested
  @DisplayName("parseSourceCommand()")
  class ParseSourceCommandTests {
    @Test
    @DisplayName("should parse valid Java source code successfully")
    void parseSourceCommand_withValidJavaSourceCode_shouldReturnSuccess() {
      String sourceCode = "public class TestClass { private String name; }";
      Path filePath = Path.of("/test/TestClass.java");
      SupportedLanguage language = SupportedLanguage.JAVA;
      SupportedIDE ide = SupportedIDE.NONE;

      DataTransferObject<io.github.syntaxpresso.core.command.dto.ParseSourceCodeResponse> result =
          javaCommandService.parseSourceCommand(sourceCode, filePath, language, ide);

      assertTrue(result.getSucceed());
      assertNotNull(result.getData());
      assertEquals(sourceCode, result.getData().getSourceCode());
      assertEquals(filePath.toString(), result.getData().getFilePath());
      assertEquals(ide, result.getData().getIde());
      assertTrue(result.getData().isParseSuccess());
    }

    @Test
    @DisplayName("should parse complex Java source code successfully")
    void parseSourceCommand_withComplexJavaSourceCode_shouldReturnSuccess() {
      String complexSourceCode =
          """
          package com.example.test;
          import java.util.List;
          import java.util.ArrayList;
          
          /**
           * A test class for complex parsing
           */
          public class ComplexTestClass {
              private final String name;
              private List<Integer> numbers;
              
              public ComplexTestClass(String name) {
                  this.name = name;
                  this.numbers = new ArrayList<>();
              }
              
              public void addNumber(int number) {
                  if (number > 0) {
                      numbers.add(number);
                  }
              }
              
              public List<Integer> getNumbers() {
                  return new ArrayList<>(numbers);
              }
              
              @Override
              public String toString() {
                  return "ComplexTestClass{name='" + name + "', numbers=" + numbers + "}";
              }
          }
          """;
      Path filePath = Path.of("/src/main/java/com/example/test/ComplexTestClass.java");
      SupportedLanguage language = SupportedLanguage.JAVA;
      SupportedIDE ide = SupportedIDE.VSCODE;

      DataTransferObject<io.github.syntaxpresso.core.command.dto.ParseSourceCodeResponse> result =
          javaCommandService.parseSourceCommand(complexSourceCode, filePath, language, ide);

      assertTrue(result.getSucceed());
      assertNotNull(result.getData());
      assertEquals(complexSourceCode, result.getData().getSourceCode());
      assertEquals(filePath.toString(), result.getData().getFilePath());
      assertEquals(ide, result.getData().getIde());
      assertTrue(result.getData().isParseSuccess());
    }

    @Test
    @DisplayName("should parse Java interface successfully")
    void parseSourceCommand_withJavaInterface_shouldReturnSuccess() {
      String interfaceSourceCode =
          """
          package com.example.interfaces;
          
          /**
           * Test interface
           */
          public interface TestInterface {
              void doSomething();
              String getName();
              default void defaultMethod() {
                  System.out.println("Default implementation");
              }
          }
          """;
      Path filePath = Path.of("/src/main/java/com/example/interfaces/TestInterface.java");
      SupportedLanguage language = SupportedLanguage.JAVA;
      SupportedIDE ide = SupportedIDE.NEOVIM;

      DataTransferObject<io.github.syntaxpresso.core.command.dto.ParseSourceCodeResponse> result =
          javaCommandService.parseSourceCommand(interfaceSourceCode, filePath, language, ide);

      assertTrue(result.getSucceed());
      assertNotNull(result.getData());
      assertEquals(interfaceSourceCode, result.getData().getSourceCode());
      assertEquals(filePath.toString(), result.getData().getFilePath());
      assertEquals(ide, result.getData().getIde());
      assertTrue(result.getData().isParseSuccess());
    }

    @Test
    @DisplayName("should parse Java enum successfully")
    void parseSourceCommand_withJavaEnum_shouldReturnSuccess() {
      String enumSourceCode =
          """
          package com.example.enums;
          
          public enum Color {
              RED("Red Color"),
              GREEN("Green Color"),
              BLUE("Blue Color");
              
              private final String description;
              
              Color(String description) {
                  this.description = description;
              }
              
              public String getDescription() {
                  return description;
              }
          }
          """;
      Path filePath = Path.of("/src/main/java/com/example/enums/Color.java");
      SupportedLanguage language = SupportedLanguage.JAVA;
      SupportedIDE ide = SupportedIDE.NONE;

      DataTransferObject<io.github.syntaxpresso.core.command.dto.ParseSourceCodeResponse> result =
          javaCommandService.parseSourceCommand(enumSourceCode, filePath, language, ide);

      assertTrue(result.getSucceed());
      assertNotNull(result.getData());
      assertEquals(enumSourceCode, result.getData().getSourceCode());
      assertEquals(filePath.toString(), result.getData().getFilePath());
      assertEquals(ide, result.getData().getIde());
      assertTrue(result.getData().isParseSuccess());
    }

    @Test
    @DisplayName("should parse Java annotation successfully")
    void parseSourceCommand_withJavaAnnotation_shouldReturnSuccess() {
      String annotationSourceCode =
          """
          package com.example.annotations;
          
          import java.lang.annotation.ElementType;
          import java.lang.annotation.Retention;
          import java.lang.annotation.RetentionPolicy;
          import java.lang.annotation.Target;
          
          @Target(ElementType.TYPE)
          @Retention(RetentionPolicy.RUNTIME)
          public @interface TestAnnotation {
              String value() default "";
              int priority() default 0;
          }
          """;
      Path filePath = Path.of("/src/main/java/com/example/annotations/TestAnnotation.java");
      SupportedLanguage language = SupportedLanguage.JAVA;
      SupportedIDE ide = SupportedIDE.VSCODE;

      DataTransferObject<io.github.syntaxpresso.core.command.dto.ParseSourceCodeResponse> result =
          javaCommandService.parseSourceCommand(annotationSourceCode, filePath, language, ide);

      assertTrue(result.getSucceed());
      assertNotNull(result.getData());
      assertEquals(annotationSourceCode, result.getData().getSourceCode());
      assertEquals(filePath.toString(), result.getData().getFilePath());
      assertEquals(ide, result.getData().getIde());
      assertTrue(result.getData().isParseSuccess());
    }

    @Test
    @DisplayName("should handle empty source code")
    void parseSourceCommand_withEmptySourceCode_shouldReturnSuccess() {
      String emptySourceCode = "";
      Path filePath = Path.of("/test/Empty.java");
      SupportedLanguage language = SupportedLanguage.JAVA;
      SupportedIDE ide = SupportedIDE.NONE;

      DataTransferObject<io.github.syntaxpresso.core.command.dto.ParseSourceCodeResponse> result =
          javaCommandService.parseSourceCommand(emptySourceCode, filePath, language, ide);

      assertTrue(result.getSucceed());
      assertNotNull(result.getData());
      assertEquals(emptySourceCode, result.getData().getSourceCode());
      assertEquals(filePath.toString(), result.getData().getFilePath());
      assertEquals(ide, result.getData().getIde());
      assertTrue(result.getData().isParseSuccess());
    }

    @Test
    @DisplayName("should handle whitespace-only source code")
    void parseSourceCommand_withWhitespaceOnlySourceCode_shouldReturnSuccess() {
      String whitespaceSourceCode = "   \n\n\t  \n  ";
      Path filePath = Path.of("/test/Whitespace.java");
      SupportedLanguage language = SupportedLanguage.JAVA;
      SupportedIDE ide = SupportedIDE.NEOVIM;

      DataTransferObject<io.github.syntaxpresso.core.command.dto.ParseSourceCodeResponse> result =
          javaCommandService.parseSourceCommand(whitespaceSourceCode, filePath, language, ide);

      assertTrue(result.getSucceed());
      assertNotNull(result.getData());
      assertEquals(whitespaceSourceCode, result.getData().getSourceCode());
      assertEquals(filePath.toString(), result.getData().getFilePath());
      assertEquals(ide, result.getData().getIde());
      assertTrue(result.getData().isParseSuccess());
    }

    @Test
    @DisplayName("should handle null file path")
    void parseSourceCommand_withNullFilePath_shouldReturnSuccess() {
      String sourceCode = "public class TestClass {}";
      Path filePath = null;
      SupportedLanguage language = SupportedLanguage.JAVA;
      SupportedIDE ide = SupportedIDE.NONE;

      DataTransferObject<io.github.syntaxpresso.core.command.dto.ParseSourceCodeResponse> result =
          javaCommandService.parseSourceCommand(sourceCode, filePath, language, ide);

      assertTrue(result.getSucceed());
      assertNotNull(result.getData());
      assertEquals(sourceCode, result.getData().getSourceCode());
      assertEquals(null, result.getData().getFilePath());
      assertEquals(ide, result.getData().getIde());
      assertTrue(result.getData().isParseSuccess());
    }

    @Test
    @DisplayName("should handle invalid Java syntax gracefully")
    void parseSourceCommand_withInvalidJavaSyntax_shouldStillReturnSuccess() {
      // Note: The current implementation doesn't validate syntax, it just stores the source code
      String invalidSourceCode = "public class { invalid syntax }{{{";
      Path filePath = Path.of("/test/Invalid.java");
      SupportedLanguage language = SupportedLanguage.JAVA;
      SupportedIDE ide = SupportedIDE.VSCODE;

      DataTransferObject<io.github.syntaxpresso.core.command.dto.ParseSourceCodeResponse> result =
          javaCommandService.parseSourceCommand(invalidSourceCode, filePath, language, ide);

      // Current implementation doesn't perform syntax validation, so it should still succeed
      assertTrue(result.getSucceed());
      assertNotNull(result.getData());
      assertEquals(invalidSourceCode, result.getData().getSourceCode());
      assertEquals(filePath.toString(), result.getData().getFilePath());
      assertEquals(ide, result.getData().getIde());
      assertTrue(result.getData().isParseSuccess());
    }

    @Test
    @DisplayName("should handle very large source code")
    void parseSourceCommand_withLargeSourceCode_shouldReturnSuccess() {
      StringBuilder largeSourceCode = new StringBuilder("public class LargeClass {\n");
      for (int i = 0; i < 1000; i++) {
        largeSourceCode
            .append("    private String field")
            .append(i)
            .append(" = \"value")
            .append(i)
            .append("\";\n");
      }
      largeSourceCode.append("    public void method() {\n");
      for (int i = 0; i < 1000; i++) {
        largeSourceCode
            .append("        System.out.println(field")
            .append(i)
            .append(");\n");
      }
      largeSourceCode.append("    }\n}");

      String sourceCode = largeSourceCode.toString();
      Path filePath = Path.of("/test/LargeClass.java");
      SupportedLanguage language = SupportedLanguage.JAVA;
      SupportedIDE ide = SupportedIDE.NONE;

      DataTransferObject<io.github.syntaxpresso.core.command.dto.ParseSourceCodeResponse> result =
          javaCommandService.parseSourceCommand(sourceCode, filePath, language, ide);

      assertTrue(result.getSucceed());
      assertNotNull(result.getData());
      assertEquals(sourceCode, result.getData().getSourceCode());
      assertEquals(filePath.toString(), result.getData().getFilePath());
      assertEquals(ide, result.getData().getIde());
      assertTrue(result.getData().isParseSuccess());
    }

    @Test
    @DisplayName("should handle source code with special characters and Unicode")
    void parseSourceCommand_withSpecialCharactersAndUnicode_shouldReturnSuccess() {
      String sourceCodeWithSpecialChars =
          """
          package com.example.unicode;
          
          /**
           * Test class with Unicode: 测试类 🚀
           */
          public class UnicodeTestClass {
              private String message = "Hello, 世界! 🌍";
              private String emoji = "😀😃😄😁";
              
              public void printMessage() {
                  System.out.println("Message: " + message);
                  System.out.println("Emojis: " + emoji);
              }
          }
          """;
      Path filePath = Path.of("/src/main/java/com/example/unicode/UnicodeTestClass.java");
      SupportedLanguage language = SupportedLanguage.JAVA;
      SupportedIDE ide = SupportedIDE.VSCODE;

      DataTransferObject<io.github.syntaxpresso.core.command.dto.ParseSourceCodeResponse> result =
          javaCommandService.parseSourceCommand(sourceCodeWithSpecialChars, filePath, language, ide);

      assertTrue(result.getSucceed());
      assertNotNull(result.getData());
      assertEquals(sourceCodeWithSpecialChars, result.getData().getSourceCode());
      assertEquals(filePath.toString(), result.getData().getFilePath());
      assertEquals(ide, result.getData().getIde());
      assertTrue(result.getData().isParseSuccess());
    }

    @Test
    @DisplayName("should work with all supported IDEs")
    void parseSourceCommand_withAllSupportedIDEs_shouldReturnSuccess() {
      String sourceCode = "public class IDETestClass { private String ide; }";
      Path filePath = Path.of("/test/IDETestClass.java");
      SupportedLanguage language = SupportedLanguage.JAVA;

      // Test with NONE
      DataTransferObject<io.github.syntaxpresso.core.command.dto.ParseSourceCodeResponse> noneResult =
          javaCommandService.parseSourceCommand(sourceCode, filePath, language, SupportedIDE.NONE);
      assertTrue(noneResult.getSucceed());
      assertEquals(SupportedIDE.NONE, noneResult.getData().getIde());

      // Test with VSCODE
      DataTransferObject<io.github.syntaxpresso.core.command.dto.ParseSourceCodeResponse> vscodeResult =
          javaCommandService.parseSourceCommand(sourceCode, filePath, language, SupportedIDE.VSCODE);
      assertTrue(vscodeResult.getSucceed());
      assertEquals(SupportedIDE.VSCODE, vscodeResult.getData().getIde());

      // Test with NEOVIM
      DataTransferObject<io.github.syntaxpresso.core.command.dto.ParseSourceCodeResponse> neovimResult =
          javaCommandService.parseSourceCommand(sourceCode, filePath, language, SupportedIDE.NEOVIM);
      assertTrue(neovimResult.getSucceed());
      assertEquals(SupportedIDE.NEOVIM, neovimResult.getData().getIde());
    }
  }
}
