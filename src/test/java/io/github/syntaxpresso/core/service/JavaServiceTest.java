package io.github.syntaxpresso.core.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.syntaxpresso.core.command.dto.GetCursorPositionInfoResponse;
import io.github.syntaxpresso.core.command.extra.SourceDirectoryType;
import io.github.syntaxpresso.core.common.DataTransferObject;
import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.common.extra.SupportedIDE;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import io.github.syntaxpresso.core.service.java.JavaService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("JavaService Tests")
class JavaServiceTest {
  private JavaService javaService;

  @BeforeEach
  void setUp() {
    this.javaService = new JavaService();
  }

  @Nested
  @DisplayName("isJavaProject()")
  class IsJavaProjectTests {
    @Test
    @DisplayName("should return true for a directory with build.gradle")
    void isJavaProject_withGradleBuildFile_shouldReturnTrue(@TempDir Path tempDir)
        throws IOException {
      Files.createFile(tempDir.resolve("build.gradle"));
      assertTrue(javaService.isJavaProject(tempDir.toFile()));
    }

    @Test
    @DisplayName("should return true for a directory with build.gradle.kts")
    void isJavaProject_withGradleKtsBuildFile_shouldReturnTrue(@TempDir Path tempDir)
        throws IOException {
      Files.createFile(tempDir.resolve("build.gradle.kts"));
      assertTrue(javaService.isJavaProject(tempDir.toFile()));
    }

    @Test
    @DisplayName("should return true for a directory with pom.xml")
    void isJavaProject_withMavenBuildFile_shouldReturnTrue(@TempDir Path tempDir)
        throws IOException {
      Files.createFile(tempDir.resolve("pom.xml"));
      assertTrue(javaService.isJavaProject(tempDir.toFile()));
    }

    @Test
    @DisplayName("should return true for a directory with src/main/java structure")
    void isJavaProject_withSrcMainJava_shouldReturnTrue(@TempDir Path tempDir) throws IOException {
      Files.createDirectories(tempDir.resolve("src/main/java"));
      assertTrue(javaService.isJavaProject(tempDir.toFile()));
    }

    @Test
    @DisplayName("should return false for a directory that is not a Java project")
    void isJavaProject_notAJavaProject_shouldReturnFalse(@TempDir Path tempDir) {
      assertFalse(javaService.isJavaProject(tempDir.toFile()));
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
          javaService.findFilePath(tempDir, packageName, SourceDirectoryType.MAIN);
      assertTrue(result.isPresent());
      assertTrue(result.get().endsWith(Path.of("src", "main", "java", "com", "example")));
      assertTrue(Files.isDirectory(result.get()));
    }

    @Test
    @DisplayName("should find and create the correct test file path")
    void findFilePath_forTest_shouldReturnCorrectPath(@TempDir Path tempDir) throws IOException {
      String packageName = "com.example.test";
      Optional<Path> result =
          javaService.findFilePath(tempDir, packageName, SourceDirectoryType.TEST);
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
          javaService.findFilePath(invalidPath, packageName, SourceDirectoryType.MAIN);
      assertFalse(result.isPresent());
    }
  }

  @Nested
  @DisplayName("isMainClass()")
  class IsMainClassTests {
    @Test
    @DisplayName("should return true for a class with a main method")
    void isMainClass_whenClassIsMain_shouldReturnTrue() {
      String sourceCode =
          """
          public class Main {
            public static void main(String[] args) {
            }
          }
          """;
      TSFile file = new TSFile(SupportedLanguage.JAVA, sourceCode);
      assertTrue(javaService.getProgramService().hasMainClass(file));
    }

    @Test
    @DisplayName("should return false for a class without a main method")
    void isMainClass_whenClassIsNotMain_shouldReturnFalse() {
      String sourceCode = "public class NotMain { }";
      TSFile file = new TSFile(SupportedLanguage.JAVA, sourceCode);
      assertFalse(javaService.getProgramService().hasMainClass(file));
    }

    @Test
    @DisplayName("should return true for a main method with varargs")
    void isMainClass_whenMainMethodHasVarargs_shouldReturnTrue() {
      String sourceCode =
          """
          public class Main {
            public static void main(String... args) {
            }
          }
          """;
      TSFile file = new TSFile(SupportedLanguage.JAVA, sourceCode);
      assertTrue(javaService.getProgramService().hasMainClass(file));
    }
  }

  @Nested
  @DisplayName("getPackageName()")
  class GetPackageNameTests {
    @Test
    @DisplayName("should return package name when declared")
    void getPackageName_whenPackageIsDeclared_shouldReturnPackageName() {
      String sourceCode =
          """
          package com.example.myproject;
          class MyClass {
          }
          """;
      TSFile file = new TSFile(SupportedLanguage.JAVA, sourceCode);
      Optional<String> packageName =
          javaService.getProgramService().getPackageDeclarationService().getPackageName(file);
      assertTrue(packageName.isPresent());
      assertEquals("com.example.myproject", packageName.get());
    }

    @Test
    @DisplayName("should return empty for default package")
    void getPackageName_whenInDefaultPackage_shouldReturnEmpty() {
      String sourceCode =
          """
          class MyClass {
          }
          """;
      TSFile file = new TSFile(SupportedLanguage.JAVA, sourceCode);
      Optional<String> packageName =
          javaService.getProgramService().getPackageDeclarationService().getPackageName(file);
      assertFalse(packageName.isPresent());
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
          javaService.getTextFromCursorPosition(
              filePath, SupportedLanguage.JAVA, SupportedIDE.NONE, 2, 14);
      assertTrue(result.getSucceed());
      assertNotNull(result.getData());
      assertEquals("MyClass", result.getData().getNodeText());
      assertEquals(filePath.toString(), result.getData().getFilePath());
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
          javaService.getTextFromCursorPosition(
              filePath, SupportedLanguage.JAVA, SupportedIDE.NONE, 3, 17);
      assertTrue(result.getSucceed());
      assertNotNull(result.getData());
      assertEquals("setName", result.getData().getNodeText());
      assertEquals(filePath.toString(), result.getData().getFilePath());
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
          javaService.getTextFromCursorPosition(
              filePath, SupportedLanguage.JAVA, SupportedIDE.NONE, 3, 24);
      assertTrue(result.getSucceed());
      assertNotNull(result.getData());
      assertEquals("userName", result.getData().getNodeText());
      assertEquals(filePath.toString(), result.getData().getFilePath());
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
          javaService.getTextFromCursorPosition(
              filePath, SupportedLanguage.JAVA, SupportedIDE.NONE, 3, 35);
      assertTrue(result.getSucceed());
      assertNotNull(result.getData());
      assertEquals("newName", result.getData().getNodeText());
      assertEquals(filePath.toString(), result.getData().getFilePath());
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
          javaService.getTextFromCursorPosition(
              filePath, SupportedLanguage.JAVA, SupportedIDE.NONE, 4, 20);
      assertTrue(result.getSucceed());
      assertNotNull(result.getData());
      assertEquals("localVar", result.getData().getNodeText());
      assertEquals(filePath.toString(), result.getData().getFilePath());
      assertNotNull(result.getData().getNode());
    }

    @Test
    @DisplayName("should return error when file does not exist")
    void getTextFromCursorPosition_fileDoesNotExist_shouldReturnError(@TempDir Path tempDir) {
      Path nonExistentFile = tempDir.resolve("NonExistent.java");
      DataTransferObject<GetCursorPositionInfoResponse> result =
          javaService.getTextFromCursorPosition(
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
          javaService.getTextFromCursorPosition(
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
          javaService.getTextFromCursorPosition(
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
          javaService.getTextFromCursorPosition(
              filePath, SupportedLanguage.JAVA, SupportedIDE.VSCODE, 2, 13);
      DataTransferObject<GetCursorPositionInfoResponse> resultNeovim =
          javaService.getTextFromCursorPosition(
              filePath, SupportedLanguage.JAVA, SupportedIDE.NEOVIM, 2, 13);
      // Both should succeed for a reasonable position
      assertTrue(resultVSCode.getSucceed());
      assertTrue(resultNeovim.getSucceed());
      assertEquals("MyClass", resultVSCode.getData().getNodeText());
      assertEquals("MyClass", resultNeovim.getData().getNodeText());
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
          javaService.getTextFromCursorPosition(
              filePath, SupportedLanguage.JAVA, SupportedIDE.NONE, 3, 20);
      assertTrue(classResult.getSucceed());
      assertEquals("ComplexClass", classResult.getData().getNodeText());
      // Test field name
      DataTransferObject<GetCursorPositionInfoResponse> fieldResult =
          javaService.getTextFromCursorPosition(
              filePath, SupportedLanguage.JAVA, SupportedIDE.NONE, 4, 24);
      assertTrue(fieldResult.getSucceed());
      assertEquals("field1", fieldResult.getData().getNodeText());
      // Test method name
      DataTransferObject<GetCursorPositionInfoResponse> methodResult =
          javaService.getTextFromCursorPosition(
              filePath, SupportedLanguage.JAVA, SupportedIDE.NONE, 10, 25);
      assertTrue(methodResult.getSucceed());
      assertEquals("getField1", methodResult.getData().getNodeText());
    }
  }
}
