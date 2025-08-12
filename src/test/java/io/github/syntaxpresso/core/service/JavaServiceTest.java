package io.github.syntaxpresso.core.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.syntaxpresso.core.command.java.extra.SourceDirectoryType;
import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import io.github.syntaxpresso.core.service.java.JavaService;
import io.github.syntaxpresso.core.util.PathHelper;
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
  private PathHelper pathHelper;

  @BeforeEach
  void setUp() {
    pathHelper = new PathHelper();
    this.javaService = new JavaService(pathHelper);
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
      assertTrue(javaService.isMainClass(file));
    }

    @Test
    @DisplayName("should return false for a class without a main method")
    void isMainClass_whenClassIsNotMain_shouldReturnFalse() {
      String sourceCode = "public class NotMain { }";
      TSFile file = new TSFile(SupportedLanguage.JAVA, sourceCode);
      assertFalse(javaService.isMainClass(file));
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
      assertTrue(javaService.isMainClass(file));
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
          javaService.getPackageDeclarationService().getPackageName(file);
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
          javaService.getPackageDeclarationService().getPackageName(file);
      assertFalse(packageName.isPresent());
    }
  }
}
