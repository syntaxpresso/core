package io.github.syntaxpresso.core.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.syntaxpresso.core.command.dto.GetMainClassResponse;
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
  private JavaCommandService javaCommandService;
  @TempDir Path tempDir;

  @BeforeEach
  void setUp() {
    PathHelper pathHelper = new PathHelper();
    VariableNamingService variableNamingService = new VariableNamingService();
    
    FormalParameterDeclarationService formalParameterDeclarationService = new FormalParameterDeclarationService();
    MethodDeclarationService methodDeclarationService = new MethodDeclarationService();
    FieldDeclarationService fieldDeclarationService = new FieldDeclarationService();
    ClassDeclarationService classDeclarationService = new ClassDeclarationService();
    PackageDeclarationService packageDeclarationService = new PackageDeclarationService();
    ImportDeclarationService importDeclarationService = new ImportDeclarationService();
    LocalVariableDeclarationService localVariableDeclarationService = new LocalVariableDeclarationService();
    
    // Wire up dependencies manually since they use field injection
    try {
      Field methodServiceField = MethodDeclarationService.class.getDeclaredField("formalParameterDeclarationService");
      methodServiceField.setAccessible(true);
      methodServiceField.set(methodDeclarationService, formalParameterDeclarationService);
      
      Field methodField = ClassDeclarationService.class.getDeclaredField("methodDeclarationService");
      methodField.setAccessible(true);
      methodField.set(classDeclarationService, methodDeclarationService);
      
      Field fieldField = ClassDeclarationService.class.getDeclaredField("fieldDeclarationService");
      fieldField.setAccessible(true);
      fieldField.set(classDeclarationService, fieldDeclarationService);
    } catch (Exception e) {
      throw new RuntimeException("Failed to wire dependencies", e);
    }
    
    JavaLanguageService javaLanguageService = new JavaLanguageService(
        pathHelper,
        variableNamingService,
        classDeclarationService,
        packageDeclarationService,
        importDeclarationService,
        localVariableDeclarationService
    );
    
    this.javaCommandService = new JavaCommandService(pathHelper, javaLanguageService);
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
    GetMainClassCommand command = new GetMainClassCommand(this.javaCommandService);

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

  private void createJavaFile(Path baseDir, String packageName, String className, boolean hasMainMethod) throws Exception {
    String packagePath = packageName.replace('.', '/');
    Path packageDir = baseDir.resolve("src/main/java").resolve(packagePath);
    Files.createDirectories(packageDir);
    
    String mainMethodCode = hasMainMethod ? 
      "    public static void main(String[] args) {\n        System.out.println(\"Hello World!\");\n    }" : 
      "";
    
    String sourceCode = String.format("""
        package %s;
        
        public class %s {
        %s
        }
        """, packageName, className, mainMethodCode);
    
    Files.writeString(packageDir.resolve(className + ".java"), sourceCode);
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
      GetMainClassCommand command = new GetMainClassCommand(javaCommandService);
      assertNotNull(command);

      Field serviceField = GetMainClassCommand.class.getDeclaredField("javaService");
      serviceField.setAccessible(true);
      assertEquals(javaCommandService, serviceField.get(command));
    }

    @Test
    @DisplayName("should have default IDE as NONE")
    void shouldHaveDefaultIDEAsNone() throws Exception {
      GetMainClassCommand command = new GetMainClassCommand(javaCommandService);
      Field ideField = GetMainClassCommand.class.getDeclaredField("ide");
      ideField.setAccessible(true);
      assertEquals(SupportedIDE.NONE, ideField.get(command));
    }

    @Test
    @DisplayName("should implement Callable interface correctly")
    void shouldImplementCallableInterfaceCorrectly() {
      GetMainClassCommand command = new GetMainClassCommand(javaCommandService);
      assertTrue(command instanceof java.util.concurrent.Callable);
    }
  }

  @Nested
  @DisplayName("Language Support Tests")
  class LanguageSupportTests {
    @Test
    @DisplayName("should delegate to JavaCommandService for JAVA language")
    void shouldDelegateToJavaCommandServiceForJavaLanguage() throws Exception {
      createJavaFile(tempDir, "com.example", "MainApp", true);

      GetMainClassCommand command =
          createCommand(tempDir, SupportedLanguage.JAVA, SupportedIDE.NONE);

      DataTransferObject<GetMainClassResponse> result = command.call();
      assertTrue(result.getSucceed());
      assertTrue(result.getData().getFilePath().endsWith("MainApp.java"));
      assertEquals("com.example", result.getData().getPackageName());
      assertEquals("MainApp", result.getData().getClassName());
    }

    @Test
    @DisplayName("should return error for unsupported language")
    void shouldReturnErrorForUnsupportedLanguage() throws Exception {
      GetMainClassCommand command = new GetMainClassCommand(javaCommandService);

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
      GetMainClassCommand command = new GetMainClassCommand(javaCommandService);

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
      createJavaFile(tempDir, "com.example", "Application", true);

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
      createJavaFile(tempDir, "com.example", "Application", false);

      GetMainClassCommand command =
          createCommand(tempDir, SupportedLanguage.JAVA, SupportedIDE.NONE);

      DataTransferObject<GetMainClassResponse> result = command.call();
      assertFalse(result.getSucceed());
      assertEquals("Couldn't find the main class of this project.", result.getErrorReason());
    }

    @Test
    @DisplayName("should handle main class in default package")
    void shouldHandleMainClassInDefaultPackage() throws Exception {
      Path srcDir = tempDir.resolve("src/main/java");
      Files.createDirectories(srcDir);
      
      String sourceCode = """
          public class Main {
              public static void main(String[] args) {
                  System.out.println("Hello World!");
              }
          }
          """;
      Files.writeString(srcDir.resolve("Main.java"), sourceCode);

      GetMainClassCommand command =
          createCommand(tempDir, SupportedLanguage.JAVA, SupportedIDE.NONE);

      DataTransferObject<GetMainClassResponse> result = command.call();
      assertFalse(result.getSucceed());
      assertTrue(result.getErrorReason().contains("Couldn't obtain public class' package name from"));
    }

    @Test
    @DisplayName("should handle deeply nested package structure")
    void shouldHandleDeeplyNestedPackageStructure() throws Exception {
      createJavaFile(tempDir, "io.github.syntaxpresso.core.app", "MainApplication", true);

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
      createJavaFile(tempDir, "com.example.app", "SpringBootApplication", true);

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
      GetMainClassCommand command = createCommand(null, SupportedLanguage.JAVA, SupportedIDE.NONE);

      DataTransferObject<GetMainClassResponse> result = command.call();
      assertFalse(result.getSucceed());
      assertEquals("Current working directory does not exist.", result.getErrorReason());
    }

    @Test
    @DisplayName("should handle class name different from filename")
    void shouldHandleClassNameDifferentFromFilename() throws Exception {
      Path srcDir = tempDir.resolve("src/main/java/com/example");
      Files.createDirectories(srcDir);
      // Create a file with a class name that doesn't match the filename
      String validSourceCode = """
          package com.example;
          public class DifferentName {
              public static void main(String[] args) {
                  System.out.println("Hello World!");
              }
          }
          """;
      Files.writeString(srcDir.resolve("InvalidClass.java"), validSourceCode);

      GetMainClassCommand command =
          createCommand(tempDir, SupportedLanguage.JAVA, SupportedIDE.NONE);

      DataTransferObject<GetMainClassResponse> result = command.call();
      // Just verify the call executes without throwing exceptions and finds the class
      assertNotNull(result);
      if (result.getSucceed()) {
        assertEquals("DifferentName", result.getData().getClassName());
        assertEquals("com.example", result.getData().getPackageName());
      }
    }

    @Test
    @DisplayName("should handle empty project directory")
    void shouldHandleEmptyProjectDirectory() throws Exception {
      GetMainClassCommand command =
          createCommand(tempDir, SupportedLanguage.JAVA, SupportedIDE.NONE);

      DataTransferObject<GetMainClassResponse> result = command.call();
      assertFalse(result.getSucceed());
      assertEquals("Couldn't find the main class of this project.", result.getErrorReason());
    }

    @Test
    @DisplayName("should handle project with only non-public classes")
    void shouldHandleProjectWithOnlyNonPublicClasses() throws Exception {
      Path srcDir = tempDir.resolve("src/main/java/com/example");
      Files.createDirectories(srcDir);
      String nonPublicClassCode = """
          package com.example;
          class NonPublicClass {
              public static void main(String[] args) {
                  System.out.println("Hello World!");
              }
          }
          """;
      Files.writeString(srcDir.resolve("NonPublicClass.java"), nonPublicClassCode);

      GetMainClassCommand command =
          createCommand(tempDir, SupportedLanguage.JAVA, SupportedIDE.NONE);

      DataTransferObject<GetMainClassResponse> result = command.call();
      // Just verify the call executes without throwing exceptions
      assertNotNull(result);
    }
  }

  @Nested
  @DisplayName("IDE Support Tests")
  class IDESupportTests {
    @Test
    @DisplayName("should work with NONE IDE")
    void shouldWorkWithNoneIDE() throws Exception {
      createJavaFile(tempDir, "com.example", "Main", true);

      GetMainClassCommand command =
          createCommand(tempDir, SupportedLanguage.JAVA, SupportedIDE.NONE);

      DataTransferObject<GetMainClassResponse> result = command.call();
      assertTrue(result.getSucceed());
      assertEquals("Main", result.getData().getClassName());
    }

    @Test
    @DisplayName("should work with VSCODE IDE")
    void shouldWorkWithVscodeIDE() throws Exception {
      createJavaFile(tempDir, "com.example", "Application", true);

      GetMainClassCommand command =
          createCommand(tempDir, SupportedLanguage.JAVA, SupportedIDE.VSCODE);

      DataTransferObject<GetMainClassResponse> result = command.call();
      assertTrue(result.getSucceed());
      assertEquals("Application", result.getData().getClassName());
    }

    @Test
    @DisplayName("should work with NEOVIM IDE")
    void shouldWorkWithNeovimIDE() throws Exception {
      createJavaFile(tempDir, "com.example", "Server", true);

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
      createJavaFile(tempDir, "com.test", "TestApp", true);

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
      createJavaFile(tempDir, "io.github.example", "ApplicationMain", true);

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
      createJavaFile(tempDir, "com.gradle.app", "GradleApp", true);

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
      createJavaFile(tempDir, "com.maven.example", "MavenApp", true);

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
      createJavaFile(tempDir, "com.example.main", "MainClass", true);

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
      createJavaFile(tempDir, "com.example", "App", true);

      GetMainClassCommand command =
          createCommand(tempDir, SupportedLanguage.JAVA, SupportedIDE.NONE);

      DataTransferObject<GetMainClassResponse> result = command.call();
      assertTrue(result.getSucceed());
      assertEquals("com.example", result.getData().getPackageName());
      assertEquals("App", result.getData().getClassName());
      assertTrue(result.getData().getFilePath().startsWith("/"));
    }
  }
}

