package io.github.syntaxpresso.core.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.syntaxpresso.core.command.dto.GetMainClassResponse;
import io.github.syntaxpresso.core.common.DataTransferObject;
import io.github.syntaxpresso.core.common.extra.SupportedIDE;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import io.github.syntaxpresso.core.service.java.JavaCommandService;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Comprehensive test suite for GetMainClassCommand.
 *
 * <p>Tests cover command configuration, language support, main class detection scenarios, error
 * handling, IDE support, and integration with JavaCommandService.
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * GetMainClassCommand command = new GetMainClassCommand(javaService);
 * command.setCwd(projectPath);
 * command.setLanguage(SupportedLanguage.JAVA);
 * command.setIde(SupportedIDE.VSCODE);
 * DataTransferObject<GetMainClassResponse> result = command.call();
 * }</pre>
 */
@DisplayName("GetMainClassCommand Tests")
class GetMainClassCommandTest {
  private JavaCommandService mockJavaService;
  @TempDir Path tempDir;

  @BeforeEach
  void setUp() {
    this.mockJavaService = mock(JavaCommandService.class);
  }

  /**
   * Creates a command instance with mock service and sets all required fields.
   *
   * @param cwd Current working directory path
   * @param language Supported programming language
   * @param ide Target IDE environment
   * @return Configured command instance ready for execution
   */
  private GetMainClassCommand createCommand(Path cwd, SupportedLanguage language, SupportedIDE ide)
      throws Exception {
    GetMainClassCommand command = new GetMainClassCommand(this.mockJavaService);

    setField(command, "cwd", cwd);
    setField(command, "language", language);
    setField(command, "ide", ide);

    return command;
  }

  private void setField(Object target, String fieldName, Object value) throws Exception {
    Field field = target.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }

  @Nested
  @DisplayName("Command Configuration Tests")
  class CommandConfigurationTests {
    @Test
    @DisplayName("should have correct command name and description")
    void shouldHaveCorrectCommandConfiguration() {
      picocli.CommandLine.Command annotation =
          GetMainClassCommand.class.getAnnotation(picocli.CommandLine.Command.class);
      assertNotNull(annotation);
      assertEquals("get-main-class", annotation.name());
      assertEquals("Get Main class", annotation.description()[0]);
    }

    @Test
    @DisplayName("should have required constructor with JavaCommandService")
    void shouldHaveRequiredConstructorWithJavaCommandService() throws Exception {
      GetMainClassCommand command = new GetMainClassCommand(mockJavaService);
      assertNotNull(command);

      Field serviceField = GetMainClassCommand.class.getDeclaredField("javaService");
      serviceField.setAccessible(true);
      assertEquals(mockJavaService, serviceField.get(command));
    }

    @Test
    @DisplayName("should have default IDE as NONE")
    void shouldHaveDefaultIDEAsNone() throws Exception {
      GetMainClassCommand command = new GetMainClassCommand(mockJavaService);
      Field ideField = GetMainClassCommand.class.getDeclaredField("ide");
      ideField.setAccessible(true);
      assertEquals(SupportedIDE.NONE, ideField.get(command));
    }

    @Test
    @DisplayName("should implement Callable interface correctly")
    void shouldImplementCallableInterfaceCorrectly() {
      GetMainClassCommand command = new GetMainClassCommand(mockJavaService);
      assertTrue(command instanceof java.util.concurrent.Callable);
    }
  }

  @Nested
  @DisplayName("Language Support Tests")
  class LanguageSupportTests {
    @Test
    @DisplayName("should delegate to JavaCommandService for JAVA language")
    void shouldDelegateToJavaCommandServiceForJavaLanguage() throws Exception {
      GetMainClassResponse mockResponse =
          GetMainClassResponse.builder()
              .filePath("/src/main/java/com/example/MainApp.java")
              .packageName("com.example")
              .className("MainApp")
              .build();
      when(mockJavaService.getMainClass(any()))
          .thenReturn(DataTransferObject.success(mockResponse));

      GetMainClassCommand command =
          createCommand(tempDir, SupportedLanguage.JAVA, SupportedIDE.NONE);

      DataTransferObject<GetMainClassResponse> result = command.call();
      assertTrue(result.getSucceed());
      assertEquals("/src/main/java/com/example/MainApp.java", result.getData().getFilePath());
      assertEquals("com.example", result.getData().getPackageName());
      assertEquals("MainApp", result.getData().getClassName());
    }

    @Test
    @DisplayName("should return error for unsupported language")
    void shouldReturnErrorForUnsupportedLanguage() throws Exception {
      GetMainClassCommand command = new GetMainClassCommand(mockJavaService);

      setField(command, "cwd", tempDir);
      setField(command, "language", null);
      setField(command, "ide", SupportedIDE.NONE);

      DataTransferObject<GetMainClassResponse> result = command.call();
      assertFalse(result.getSucceed());
      assertEquals("Language not supported.", result.getErrorReason());
    }

    @Test
    @DisplayName("should handle null language gracefully")
    void shouldHandleNullLanguageGracefully() throws Exception {
      GetMainClassCommand command = new GetMainClassCommand(mockJavaService);

      setField(command, "cwd", tempDir);
      setField(command, "language", null);
      setField(command, "ide", SupportedIDE.NONE);

      DataTransferObject<GetMainClassResponse> result = command.call();
      assertFalse(result.getSucceed());
      assertEquals("Language not supported.", result.getErrorReason());
    }
  }

  @Nested
  @DisplayName("Main Class Detection Tests")
  class MainClassDetectionTests {
    @Test
    @DisplayName("should find main class with main method")
    void shouldFindMainClassWithMainMethod() throws Exception {
      GetMainClassResponse mockResponse =
          GetMainClassResponse.builder()
              .filePath("/src/main/java/com/example/Application.java")
              .packageName("com.example")
              .className("Application")
              .build();
      when(mockJavaService.getMainClass(any()))
          .thenReturn(DataTransferObject.success(mockResponse));

      GetMainClassCommand command =
          createCommand(tempDir, SupportedLanguage.JAVA, SupportedIDE.NONE);

      DataTransferObject<GetMainClassResponse> result = command.call();
      assertTrue(result.getSucceed());
      assertEquals("Application", result.getData().getClassName());
      assertEquals("com.example", result.getData().getPackageName());
    }

    @Test
    @DisplayName("should handle project without main class")
    void shouldHandleProjectWithoutMainClass() throws Exception {
      when(mockJavaService.getMainClass(any()))
          .thenReturn(DataTransferObject.error("Couldn't find the main class of this project."));

      GetMainClassCommand command =
          createCommand(tempDir, SupportedLanguage.JAVA, SupportedIDE.NONE);

      DataTransferObject<GetMainClassResponse> result = command.call();
      assertFalse(result.getSucceed());
      assertEquals("Couldn't find the main class of this project.", result.getErrorReason());
    }

    @Test
    @DisplayName("should handle main class in default package")
    void shouldHandleMainClassInDefaultPackage() throws Exception {
      GetMainClassResponse mockResponse =
          GetMainClassResponse.builder()
              .filePath("/src/main/java/Main.java")
              .packageName("")
              .className("Main")
              .build();
      when(mockJavaService.getMainClass(any()))
          .thenReturn(DataTransferObject.success(mockResponse));

      GetMainClassCommand command =
          createCommand(tempDir, SupportedLanguage.JAVA, SupportedIDE.NONE);

      DataTransferObject<GetMainClassResponse> result = command.call();
      assertTrue(result.getSucceed());
      assertEquals("Main", result.getData().getClassName());
      assertEquals("", result.getData().getPackageName());
    }

    @Test
    @DisplayName("should handle deeply nested package structure")
    void shouldHandleDeeplyNestedPackageStructure() throws Exception {
      GetMainClassResponse mockResponse =
          GetMainClassResponse.builder()
              .filePath("/src/main/java/io/github/syntaxpresso/core/app/MainApplication.java")
              .packageName("io.github.syntaxpresso.core.app")
              .className("MainApplication")
              .build();
      when(mockJavaService.getMainClass(any()))
          .thenReturn(DataTransferObject.success(mockResponse));

      GetMainClassCommand command =
          createCommand(tempDir, SupportedLanguage.JAVA, SupportedIDE.NONE);

      DataTransferObject<GetMainClassResponse> result = command.call();
      assertTrue(result.getSucceed());
      assertEquals("MainApplication", result.getData().getClassName());
      assertEquals("io.github.syntaxpresso.core.app", result.getData().getPackageName());
    }

    @Test
    @DisplayName("should handle main class with complex class structure")
    void shouldHandleMainClassWithComplexStructure() throws Exception {
      GetMainClassResponse mockResponse =
          GetMainClassResponse.builder()
              .filePath("/src/main/java/com/example/app/SpringBootApplication.java")
              .packageName("com.example.app")
              .className("SpringBootApplication")
              .build();
      when(mockJavaService.getMainClass(any()))
          .thenReturn(DataTransferObject.success(mockResponse));

      GetMainClassCommand command =
          createCommand(tempDir, SupportedLanguage.JAVA, SupportedIDE.NONE);

      DataTransferObject<GetMainClassResponse> result = command.call();
      assertTrue(result.getSucceed());
      assertEquals("SpringBootApplication", result.getData().getClassName());
      assertEquals("com.example.app", result.getData().getPackageName());
    }
  }

  @Nested
  @DisplayName("Error Handling Tests")
  class ErrorHandlingTests {
    @Test
    @DisplayName("should handle invalid working directory")
    void shouldHandleInvalidWorkingDirectory() throws Exception {
      when(mockJavaService.getMainClass(any()))
          .thenReturn(DataTransferObject.error("Current working directory does not exist."));

      Path invalidPath = Path.of("/non/existent/directory");
      GetMainClassCommand command =
          createCommand(invalidPath, SupportedLanguage.JAVA, SupportedIDE.NONE);

      DataTransferObject<GetMainClassResponse> result = command.call();
      assertFalse(result.getSucceed());
      assertEquals("Current working directory does not exist.", result.getErrorReason());
    }

    @Test
    @DisplayName("should handle null working directory")
    void shouldHandleNullWorkingDirectory() throws Exception {
      when(mockJavaService.getMainClass(any()))
          .thenReturn(DataTransferObject.error("Current working directory does not exist."));

      GetMainClassCommand command = createCommand(null, SupportedLanguage.JAVA, SupportedIDE.NONE);

      DataTransferObject<GetMainClassResponse> result = command.call();
      assertFalse(result.getSucceed());
      assertEquals("Current working directory does not exist.", result.getErrorReason());
    }

    @Test
    @DisplayName("should propagate JavaCommandService parsing errors")
    void shouldPropagateJavaCommandServiceParsingErrors() throws Exception {
      when(mockJavaService.getMainClass(any()))
          .thenReturn(DataTransferObject.error("Couldn't obtain public class' name from file."));

      GetMainClassCommand command =
          createCommand(tempDir, SupportedLanguage.JAVA, SupportedIDE.NONE);

      DataTransferObject<GetMainClassResponse> result = command.call();
      assertFalse(result.getSucceed());
      assertEquals("Couldn't obtain public class' name from file.", result.getErrorReason());
    }

    @Test
    @DisplayName("should handle empty project directory")
    void shouldHandleEmptyProjectDirectory() throws Exception {
      when(mockJavaService.getMainClass(any()))
          .thenReturn(DataTransferObject.error("Couldn't find the main class of this project."));

      GetMainClassCommand command =
          createCommand(tempDir, SupportedLanguage.JAVA, SupportedIDE.NONE);

      DataTransferObject<GetMainClassResponse> result = command.call();
      assertFalse(result.getSucceed());
      assertEquals("Couldn't find the main class of this project.", result.getErrorReason());
    }

    @Test
    @DisplayName("should handle project with only non-public classes")
    void shouldHandleProjectWithOnlyNonPublicClasses() throws Exception {
      when(mockJavaService.getMainClass(any()))
          .thenReturn(DataTransferObject.error("Couldn't find the main class of this project."));

      GetMainClassCommand command =
          createCommand(tempDir, SupportedLanguage.JAVA, SupportedIDE.NONE);

      DataTransferObject<GetMainClassResponse> result = command.call();
      assertFalse(result.getSucceed());
      assertEquals("Couldn't find the main class of this project.", result.getErrorReason());
    }
  }

  @Nested
  @DisplayName("IDE Support Tests")
  class IDESupportTests {
    @Test
    @DisplayName("should work with NONE IDE")
    void shouldWorkWithNoneIDE() throws Exception {
      GetMainClassResponse mockResponse =
          GetMainClassResponse.builder()
              .filePath("/src/main/java/Main.java")
              .packageName("com.example")
              .className("Main")
              .build();
      when(mockJavaService.getMainClass(any()))
          .thenReturn(DataTransferObject.success(mockResponse));

      GetMainClassCommand command =
          createCommand(tempDir, SupportedLanguage.JAVA, SupportedIDE.NONE);

      DataTransferObject<GetMainClassResponse> result = command.call();
      assertTrue(result.getSucceed());
      assertEquals("Main", result.getData().getClassName());
    }

    @Test
    @DisplayName("should work with VSCODE IDE")
    void shouldWorkWithVscodeIDE() throws Exception {
      GetMainClassResponse mockResponse =
          GetMainClassResponse.builder()
              .filePath("/src/main/java/Application.java")
              .packageName("com.example")
              .className("Application")
              .build();
      when(mockJavaService.getMainClass(any()))
          .thenReturn(DataTransferObject.success(mockResponse));

      GetMainClassCommand command =
          createCommand(tempDir, SupportedLanguage.JAVA, SupportedIDE.VSCODE);

      DataTransferObject<GetMainClassResponse> result = command.call();
      assertTrue(result.getSucceed());
      assertEquals("Application", result.getData().getClassName());
    }

    @Test
    @DisplayName("should work with NEOVIM IDE")
    void shouldWorkWithNeovimIDE() throws Exception {
      GetMainClassResponse mockResponse =
          GetMainClassResponse.builder()
              .filePath("/src/main/java/Server.java")
              .packageName("com.example")
              .className("Server")
              .build();
      when(mockJavaService.getMainClass(any()))
          .thenReturn(DataTransferObject.success(mockResponse));

      GetMainClassCommand command =
          createCommand(tempDir, SupportedLanguage.JAVA, SupportedIDE.NEOVIM);

      DataTransferObject<GetMainClassResponse> result = command.call();
      assertTrue(result.getSucceed());
      assertEquals("Server", result.getData().getClassName());
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {
    @Test
    @DisplayName("should pass correct working directory to JavaCommandService")
    void shouldPassCorrectWorkingDirectoryToJavaCommandService() throws Exception {
      GetMainClassResponse mockResponse =
          GetMainClassResponse.builder()
              .filePath(tempDir.resolve("src/main/java/TestApp.java").toString())
              .packageName("com.test")
              .className("TestApp")
              .build();
      when(mockJavaService.getMainClass(tempDir))
          .thenReturn(DataTransferObject.success(mockResponse));

      GetMainClassCommand command =
          createCommand(tempDir, SupportedLanguage.JAVA, SupportedIDE.NONE);

      DataTransferObject<GetMainClassResponse> result = command.call();
      assertTrue(result.getSucceed());
      assertEquals("TestApp", result.getData().getClassName());
      assertEquals("com.test", result.getData().getPackageName());
    }

    @Test
    @DisplayName("should handle real project structure scenario")
    void shouldHandleRealProjectStructureScenario() throws Exception {
      Path srcDir = tempDir.resolve("src/main/java/io/github/example");
      Files.createDirectories(srcDir);
      String mainClassCode =
          """
          package io.github.example;
          public class ApplicationMain {
              public static void main(String[] args) {
                  System.out.println("Hello World!");
              }
          }
          """;
      Files.writeString(srcDir.resolve("ApplicationMain.java"), mainClassCode);

      GetMainClassResponse mockResponse =
          GetMainClassResponse.builder()
              .filePath(srcDir.resolve("ApplicationMain.java").toString())
              .packageName("io.github.example")
              .className("ApplicationMain")
              .build();
      when(mockJavaService.getMainClass(tempDir))
          .thenReturn(DataTransferObject.success(mockResponse));

      GetMainClassCommand command =
          createCommand(tempDir, SupportedLanguage.JAVA, SupportedIDE.VSCODE);

      DataTransferObject<GetMainClassResponse> result = command.call();
      assertTrue(result.getSucceed());
      assertEquals("ApplicationMain", result.getData().getClassName());
      assertEquals("io.github.example", result.getData().getPackageName());
      assertTrue(result.getData().getFilePath().endsWith("ApplicationMain.java"));
    }

    @Test
    @DisplayName("should handle Gradle project structure")
    void shouldHandleGradleProjectStructure() throws Exception {
      Files.createFile(tempDir.resolve("build.gradle.kts"));
      Path srcDir = tempDir.resolve("src/main/java/com/gradle/app");
      Files.createDirectories(srcDir);

      GetMainClassResponse mockResponse =
          GetMainClassResponse.builder()
              .filePath(srcDir.resolve("GradleApp.java").toString())
              .packageName("com.gradle.app")
              .className("GradleApp")
              .build();
      when(mockJavaService.getMainClass(tempDir))
          .thenReturn(DataTransferObject.success(mockResponse));

      GetMainClassCommand command =
          createCommand(tempDir, SupportedLanguage.JAVA, SupportedIDE.NONE);

      DataTransferObject<GetMainClassResponse> result = command.call();
      assertTrue(result.getSucceed());
      assertEquals("GradleApp", result.getData().getClassName());
      assertEquals("com.gradle.app", result.getData().getPackageName());
    }

    @Test
    @DisplayName("should handle Maven project structure")
    void shouldHandleMavenProjectStructure() throws Exception {
      String pomContent =
          """
          <?xml version="1.0" encoding="UTF-8"?>
          <project xmlns="http://maven.apache.org/POM/4.0.0">
              <modelVersion>4.0.0</modelVersion>
              <groupId>com.maven.example</groupId>
              <artifactId>maven-app</artifactId>
              <version>1.0.0</version>
          </project>
          """;
      Files.writeString(tempDir.resolve("pom.xml"), pomContent);
      Path srcDir = tempDir.resolve("src/main/java/com/maven/example");
      Files.createDirectories(srcDir);

      GetMainClassResponse mockResponse =
          GetMainClassResponse.builder()
              .filePath(srcDir.resolve("MavenApp.java").toString())
              .packageName("com.maven.example")
              .className("MavenApp")
              .build();
      when(mockJavaService.getMainClass(tempDir))
          .thenReturn(DataTransferObject.success(mockResponse));

      GetMainClassCommand command =
          createCommand(tempDir, SupportedLanguage.JAVA, SupportedIDE.NONE);

      DataTransferObject<GetMainClassResponse> result = command.call();
      assertTrue(result.getSucceed());
      assertEquals("MavenApp", result.getData().getClassName());
      assertEquals("com.maven.example", result.getData().getPackageName());
    }
  }

  @Nested
  @DisplayName("Response Structure Tests")
  class ResponseStructureTests {
    @Test
    @DisplayName("should return valid GetMainClassResponse structure")
    void shouldReturnValidGetMainClassResponseStructure() throws Exception {
      GetMainClassResponse mockResponse =
          GetMainClassResponse.builder()
              .filePath("/path/to/MainClass.java")
              .packageName("com.example.main")
              .className("MainClass")
              .build();
      when(mockJavaService.getMainClass(any()))
          .thenReturn(DataTransferObject.success(mockResponse));

      GetMainClassCommand command =
          createCommand(tempDir, SupportedLanguage.JAVA, SupportedIDE.NONE);

      DataTransferObject<GetMainClassResponse> result = command.call();
      assertTrue(result.getSucceed());
      assertNotNull(result.getData());
      assertNotNull(result.getData().getFilePath());
      assertNotNull(result.getData().getPackageName());
      assertNotNull(result.getData().getClassName());
    }

    @Test
    @DisplayName("should handle response with absolute file path")
    void shouldHandleResponseWithAbsoluteFilePath() throws Exception {
      String absolutePath = "/home/user/project/src/main/java/com/example/App.java";
      GetMainClassResponse mockResponse =
          GetMainClassResponse.builder()
              .filePath(absolutePath)
              .packageName("com.example")
              .className("App")
              .build();
      when(mockJavaService.getMainClass(any()))
          .thenReturn(DataTransferObject.success(mockResponse));

      GetMainClassCommand command =
          createCommand(tempDir, SupportedLanguage.JAVA, SupportedIDE.NONE);

      DataTransferObject<GetMainClassResponse> result = command.call();
      assertTrue(result.getSucceed());
      assertEquals(absolutePath, result.getData().getFilePath());
      assertTrue(result.getData().getFilePath().startsWith("/"));
    }
  }
}

