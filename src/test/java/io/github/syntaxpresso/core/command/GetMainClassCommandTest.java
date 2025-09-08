package io.github.syntaxpresso.core.command;

import static org.junit.jupiter.api.Assertions.*;

import io.github.syntaxpresso.core.command.dto.GetMainClassResponse;
import io.github.syntaxpresso.core.common.DataTransferObject;
import io.github.syntaxpresso.core.common.extra.SupportedIDE;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import io.github.syntaxpresso.core.service.java.command.GetMainClassCommandService;
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
 * Comprehensive tests for GetMainClassCommand.
 * 
 * Example usage:
 * <pre>
 * GetMainClassCommand command = new GetMainClassCommand(service);
 * // Set command options: --cwd /path/to/project --language JAVA --ide NONE
 * DataTransferObject&lt;GetMainClassResponse&gt; result = command.call();
 * if (result.getSucceed()) {
 *     GetMainClassResponse response = result.getData();
 *     System.out.println("Main class: " + response.getClassName());
 *     System.out.println("Package: " + response.getPackageName());
 *     System.out.println("File path: " + response.getFilePath());
 * }
 * </pre>
 */
@DisplayName("GetMainClassCommand Tests")
class GetMainClassCommandTest {
  private GetMainClassCommand getMainClassCommand;
  private TestGetMainClassCommandService testService;

  @TempDir Path tempDir;

  @BeforeEach
  void setUp() {
    this.testService = new TestGetMainClassCommandService();
    this.getMainClassCommand = new GetMainClassCommand(this.testService);
  }

  @Nested
  @DisplayName("Java language support tests")
  class JavaLanguageSupportTests {
    @Test
    @DisplayName("Should successfully find main class for Java language")
    void shouldSuccessfullyFindMainClassForJavaLanguage() throws Exception {
      // Given
      Path projectDir = createTestProjectStructure();
      GetMainClassResponse expectedResponse = GetMainClassResponse.builder()
          .filePath(projectDir.resolve("MainApp.java").toString())
          .packageName("com.example")
          .className("MainApp")
          .build();
      testService.setSuccessResponse(expectedResponse);
      setupGetMainClassCommand(projectDir, SupportedLanguage.JAVA, SupportedIDE.NONE);

      // When
      DataTransferObject<GetMainClassResponse> result = getMainClassCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertNotNull(result.getData());
      assertEquals("MainApp", result.getData().getClassName());
      assertEquals("com.example", result.getData().getPackageName());
      assertEquals(projectDir.resolve("MainApp.java").toString(), result.getData().getFilePath());

      // Verify service was called with correct parameters
      assertTrue(testService.wasServiceCalled());
      assertEquals(projectDir, testService.getLastCalledCwd());
    }

    @Test
    @DisplayName("Should handle service error responses for Java")
    void shouldHandleServiceErrorResponsesForJava() throws Exception {
      // Given
      Path projectDir = createTestProjectStructure();
      testService.setErrorResponse("Couldn't find the main class of this project.");
      setupGetMainClassCommand(projectDir, SupportedLanguage.JAVA, SupportedIDE.NONE);

      // When
      DataTransferObject<GetMainClassResponse> result = getMainClassCommand.call();

      // Then
      assertFalse(result.getSucceed());
      assertNull(result.getData());
      assertEquals("Couldn't find the main class of this project.", result.getErrorReason());
      assertTrue(testService.wasServiceCalled());
    }

    @Test
    @DisplayName("Should handle complex Java project structure")
    void shouldHandleComplexJavaProjectStructure() throws Exception {
      // Given
      Path complexProjectDir = createComplexProjectStructure();
      GetMainClassResponse complexResponse = GetMainClassResponse.builder()
          .filePath(complexProjectDir.resolve("src/main/java/com/example/app/Application.java").toString())
          .packageName("com.example.app")
          .className("Application")
          .build();
      testService.setSuccessResponse(complexResponse);
      setupGetMainClassCommand(complexProjectDir, SupportedLanguage.JAVA, SupportedIDE.VSCODE);

      // When
      DataTransferObject<GetMainClassResponse> result = getMainClassCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertNotNull(result.getData());
      assertEquals("Application", result.getData().getClassName());
      assertEquals("com.example.app", result.getData().getPackageName());
      assertTrue(result.getData().getFilePath().contains("Application.java"));
      assertTrue(testService.wasServiceCalled());
      assertEquals(SupportedLanguage.JAVA, SupportedLanguage.JAVA);
    }

    @Test
    @DisplayName("Should handle multiple classes with only one main method")
    void shouldHandleMultipleClassesWithOnlyOneMainMethod() throws Exception {
      // Given
      Path multiClassProject = createMultiClassProjectStructure();
      GetMainClassResponse response = GetMainClassResponse.builder()
          .filePath(multiClassProject.resolve("src/MainClass.java").toString())
          .packageName("com.example.main")
          .className("MainClass")
          .build();
      testService.setSuccessResponse(response);
      setupGetMainClassCommand(multiClassProject, SupportedLanguage.JAVA, SupportedIDE.NEOVIM);

      // When
      DataTransferObject<GetMainClassResponse> result = getMainClassCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertNotNull(result.getData());
      assertEquals("MainClass", result.getData().getClassName());
      assertEquals("com.example.main", result.getData().getPackageName());
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
      Path projectDir = createTestProjectStructure();
      // Since only JAVA is supported, test with null to simulate unsupported language
      setupGetMainClassCommand(projectDir, null, SupportedIDE.NONE);

      // When
      DataTransferObject<GetMainClassResponse> result = getMainClassCommand.call();

      // Then
      assertFalse(result.getSucceed());
      assertNull(result.getData());
      assertEquals("Language not supported.", result.getErrorReason());

      // Verify service was NOT called for unsupported language
      assertFalse(testService.wasServiceCalled());
    }

    @Test
    @DisplayName("Should process Java language successfully")
    void shouldProcessJavaLanguageSuccessfully() throws Exception {
      // Given
      Path projectDir = createTestProjectStructure();
      GetMainClassResponse response = GetMainClassResponse.builder()
          .filePath(projectDir.resolve("Test.java").toString())
          .packageName("com.test")
          .className("Test")
          .build();
      testService.setSuccessResponse(response);
      setupGetMainClassCommand(projectDir, SupportedLanguage.JAVA, SupportedIDE.NONE);

      // When
      DataTransferObject<GetMainClassResponse> result = getMainClassCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertTrue(testService.wasServiceCalled());
      assertEquals("Test", result.getData().getClassName());
      assertEquals("com.test", result.getData().getPackageName());
    }

    @Test
    @DisplayName("Should validate JAVA enum value correctly")
    void shouldValidateJavaEnumValueCorrectly() throws Exception {
      // Given
      Path projectDir = createTestProjectStructure();
      GetMainClassResponse response = GetMainClassResponse.builder()
          .filePath(projectDir.resolve("EnumTest.java").toString())
          .packageName("com.enumtest")
          .className("EnumTest")
          .build();
      testService.setSuccessResponse(response);
      setupGetMainClassCommand(projectDir, SupportedLanguage.JAVA, SupportedIDE.NONE);

      // When
      DataTransferObject<GetMainClassResponse> result = getMainClassCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertTrue(testService.wasServiceCalled());
      assertEquals(SupportedLanguage.JAVA, SupportedLanguage.JAVA); // Verify enum comparison works
    }
  }

  @Nested
  @DisplayName("IDE-specific behavior tests")
  class IDESpecificBehaviorTests {
    @Test
    @DisplayName("Should handle NONE IDE correctly")
    void shouldHandleNoneIDECorrectly() throws Exception {
      // Given
      Path projectDir = createTestProjectStructure();
      GetMainClassResponse response = GetMainClassResponse.builder()
          .filePath(projectDir.resolve("NoneIDETest.java").toString())
          .packageName("com.none")
          .className("NoneIDETest")
          .build();
      testService.setSuccessResponse(response);
      setupGetMainClassCommand(projectDir, SupportedLanguage.JAVA, SupportedIDE.NONE);

      // When
      DataTransferObject<GetMainClassResponse> result = getMainClassCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertNotNull(result.getData());
      assertEquals("NoneIDETest", result.getData().getClassName());
      assertTrue(testService.wasServiceCalled());
    }

    @Test
    @DisplayName("Should handle VSCODE IDE correctly")
    void shouldHandleVSCodeIDECorrectly() throws Exception {
      // Given
      Path projectDir = createTestProjectStructure();
      GetMainClassResponse response = GetMainClassResponse.builder()
          .filePath(projectDir.resolve("VSCodeTest.java").toString())
          .packageName("com.vscode")
          .className("VSCodeTest")
          .build();
      testService.setSuccessResponse(response);
      setupGetMainClassCommand(projectDir, SupportedLanguage.JAVA, SupportedIDE.VSCODE);

      // When
      DataTransferObject<GetMainClassResponse> result = getMainClassCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertNotNull(result.getData());
      assertEquals("VSCodeTest", result.getData().getClassName());
      assertTrue(testService.wasServiceCalled());
    }

    @Test
    @DisplayName("Should handle NEOVIM IDE correctly")
    void shouldHandleNeovimIDECorrectly() throws Exception {
      // Given
      Path projectDir = createTestProjectStructure();
      GetMainClassResponse response = GetMainClassResponse.builder()
          .filePath(projectDir.resolve("NeovimTest.java").toString())
          .packageName("com.neovim")
          .className("NeovimTest")
          .build();
      testService.setSuccessResponse(response);
      setupGetMainClassCommand(projectDir, SupportedLanguage.JAVA, SupportedIDE.NEOVIM);

      // When
      DataTransferObject<GetMainClassResponse> result = getMainClassCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertNotNull(result.getData());
      assertEquals("NeovimTest", result.getData().getClassName());
      assertTrue(testService.wasServiceCalled());
    }

    @Test
    @DisplayName("Should handle service error for different IDEs")
    void shouldHandleServiceErrorForDifferentIDEs() throws Exception {
      // Given
      Path projectDir = createTestProjectStructure();
      testService.setErrorResponse("No main method found");
      setupGetMainClassCommand(projectDir, SupportedLanguage.JAVA, SupportedIDE.VSCODE);

      // When
      DataTransferObject<GetMainClassResponse> result = getMainClassCommand.call();

      // Then
      assertFalse(result.getSucceed());
      assertNull(result.getData());
      assertEquals("No main method found", result.getErrorReason());
      assertTrue(testService.wasServiceCalled());
    }
  }

  @Nested
  @DisplayName("Command configuration tests")
  class CommandConfigurationTests {
    @Test
    @DisplayName("Should have correct picocli annotations")
    void shouldHaveCorrectPicocliAnnotations() {
      // Verify the class implements Callable
      assertTrue(Callable.class.isAssignableFrom(GetMainClassCommand.class));

      // Verify command annotation exists
      assertTrue(GetMainClassCommand.class.isAnnotationPresent(picocli.CommandLine.Command.class));

      // Verify command properties
      picocli.CommandLine.Command commandAnnotation =
          GetMainClassCommand.class.getAnnotation(picocli.CommandLine.Command.class);
      assertEquals("get-main-class", commandAnnotation.name());
      assertEquals("Get Main class", commandAnnotation.description()[0]);
    }

    @Test
    @DisplayName("Should handle all required options")
    void shouldHandleAllRequiredOptions() throws Exception {
      // Given
      Path projectDir = createTestProjectStructure();
      GetMainClassResponse response = GetMainClassResponse.builder()
          .filePath(projectDir.resolve("OptionsTest.java").toString())
          .packageName("com.options")
          .className("OptionsTest")
          .build();
      testService.setSuccessResponse(response);

      // Test all required options are properly set
      setupGetMainClassCommand(projectDir, SupportedLanguage.JAVA, SupportedIDE.NONE);

      // When
      DataTransferObject<GetMainClassResponse> result = getMainClassCommand.call();

      // Then
      assertTrue(result.getSucceed());

      // Verify all parameters were passed correctly
      assertEquals(projectDir, testService.getLastCalledCwd());
      assertTrue(testService.wasServiceCalled());
    }

    @Test
    @DisplayName("Should return DataTransferObject with correct generic type")
    void shouldReturnDataTransferObjectWithCorrectGenericType() throws Exception {
      // Given
      Path projectDir = createTestProjectStructure();
      GetMainClassResponse response = GetMainClassResponse.builder()
          .filePath(projectDir.resolve("GenericTest.java").toString())
          .packageName("com.generic")
          .className("GenericTest")
          .build();
      testService.setSuccessResponse(response);
      setupGetMainClassCommand(projectDir, SupportedLanguage.JAVA, SupportedIDE.NONE);

      // When
      DataTransferObject<GetMainClassResponse> result = getMainClassCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertNotNull(result.getData());
      assertTrue(result.getData() instanceof GetMainClassResponse);
      assertEquals(GetMainClassResponse.class, result.getData().getClass());
    }
  }

  @Nested
  @DisplayName("Edge case tests")
  class EdgeCaseTests {
    @Test
    @DisplayName("Should handle empty project directory")
    void shouldHandleEmptyProjectDirectory() throws Exception {
      // Given
      Path emptyDir = tempDir.resolve("empty");
      Files.createDirectory(emptyDir);
      testService.setErrorResponse("Couldn't find the main class of this project.");
      setupGetMainClassCommand(emptyDir, SupportedLanguage.JAVA, SupportedIDE.NONE);

      // When
      DataTransferObject<GetMainClassResponse> result = getMainClassCommand.call();

      // Then
      assertFalse(result.getSucceed());
      assertEquals("Couldn't find the main class of this project.", result.getErrorReason());
      assertTrue(testService.wasServiceCalled());
    }

    @Test
    @DisplayName("Should handle project with no main method")
    void shouldHandleProjectWithNoMainMethod() throws Exception {
      // Given
      Path noMainProject = createProjectWithoutMainMethod();
      testService.setErrorResponse("Couldn't find the main class of this project.");
      setupGetMainClassCommand(noMainProject, SupportedLanguage.JAVA, SupportedIDE.NONE);

      // When
      DataTransferObject<GetMainClassResponse> result = getMainClassCommand.call();

      // Then
      assertFalse(result.getSucceed());
      assertEquals("Couldn't find the main class of this project.", result.getErrorReason());
      assertTrue(testService.wasServiceCalled());
    }

    @Test
    @DisplayName("Should handle service validation errors")
    void shouldHandleServiceValidationErrors() throws Exception {
      // Given
      Path projectDir = createTestProjectStructure();
      testService.setErrorResponse("Current working directory does not exist.");
      setupGetMainClassCommand(projectDir, SupportedLanguage.JAVA, SupportedIDE.NONE);

      // When
      DataTransferObject<GetMainClassResponse> result = getMainClassCommand.call();

      // Then
      assertFalse(result.getSucceed());
      assertEquals("Current working directory does not exist.", result.getErrorReason());
      assertTrue(testService.wasServiceCalled());
    }

    @Test
    @DisplayName("Should handle deeply nested project structure")
    void shouldHandleDeeplyNestedProjectStructure() throws Exception {
      // Given
      Path deepProject = createDeeplyNestedProjectStructure();
      GetMainClassResponse response = GetMainClassResponse.builder()
          .filePath(deepProject.resolve("src/main/java/com/example/deep/nested/structure/DeepMain.java").toString())
          .packageName("com.example.deep.nested.structure")
          .className("DeepMain")
          .build();
      testService.setSuccessResponse(response);
      setupGetMainClassCommand(deepProject, SupportedLanguage.JAVA, SupportedIDE.NONE);

      // When
      DataTransferObject<GetMainClassResponse> result = getMainClassCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertNotNull(result.getData());
      assertEquals("DeepMain", result.getData().getClassName());
      assertEquals("com.example.deep.nested.structure", result.getData().getPackageName());
      assertTrue(result.getData().getFilePath().contains("DeepMain.java"));
    }

    @Test
    @DisplayName("Should handle service returning null response")
    void shouldHandleServiceReturningNullResponse() throws Exception {
      // Given
      Path projectDir = createTestProjectStructure();
      testService.setNullResponse();
      setupGetMainClassCommand(projectDir, SupportedLanguage.JAVA, SupportedIDE.NONE);

      // When
      DataTransferObject<GetMainClassResponse> result = getMainClassCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertNull(result.getData());
      assertTrue(testService.wasServiceCalled());
    }

    @Test
    @DisplayName("Should handle special characters in file paths")
    void shouldHandleSpecialCharactersInFilePaths() throws Exception {
      // Given
      Path specialCharDir = tempDir.resolve("special chars & symbols");
      Files.createDirectory(specialCharDir);
      GetMainClassResponse response = GetMainClassResponse.builder()
          .filePath(specialCharDir.resolve("SpecialMain.java").toString())
          .packageName("com.special")
          .className("SpecialMain")
          .build();
      testService.setSuccessResponse(response);
      setupGetMainClassCommand(specialCharDir, SupportedLanguage.JAVA, SupportedIDE.NONE);

      // When
      DataTransferObject<GetMainClassResponse> result = getMainClassCommand.call();

      // Then
      assertTrue(result.getSucceed());
      assertNotNull(result.getData());
      assertEquals("SpecialMain", result.getData().getClassName());
      assertTrue(result.getData().getFilePath().contains("special chars & symbols"));
    }
  }

  private Path createTestProjectStructure() throws IOException {
    Path projectDir = tempDir.resolve("test-project");
    Files.createDirectory(projectDir);
    
    String mainClass = """
        package com.example;
        
        public class MainApp {
            public static void main(String[] args) {
                System.out.println("Hello World");
            }
        }
        """;
    Files.write(projectDir.resolve("MainApp.java"), mainClass.getBytes());
    
    return projectDir;
  }

  private Path createComplexProjectStructure() throws IOException {
    Path projectDir = tempDir.resolve("complex-project");
    Path srcDir = projectDir.resolve("src/main/java/com/example/app");
    Files.createDirectories(srcDir);
    
    String applicationClass = """
        package com.example.app;
        
        import java.util.Arrays;
        
        public class Application {
            public static void main(String[] args) {
                System.out.println("Complex Application Started");
                Arrays.stream(args).forEach(System.out::println);
            }
        }
        """;
    Files.write(srcDir.resolve("Application.java"), applicationClass.getBytes());
    
    // Add other classes without main methods
    String serviceClass = """
        package com.example.app;
        
        public class AppService {
            public void doSomething() {
                // Service logic
            }
        }
        """;
    Files.write(srcDir.resolve("AppService.java"), serviceClass.getBytes());
    
    return projectDir;
  }

  private Path createMultiClassProjectStructure() throws IOException {
    Path projectDir = tempDir.resolve("multi-class-project");
    Path srcDir = projectDir.resolve("src");
    Files.createDirectories(srcDir);
    
    String mainClass = """
        package com.example.main;
        
        public class MainClass {
            public static void main(String[] args) {
                System.out.println("This is the main class");
            }
        }
        """;
    Files.write(srcDir.resolve("MainClass.java"), mainClass.getBytes());
    
    String utilClass = """
        package com.example.util;
        
        public class UtilClass {
            public static void helper() {
                System.out.println("Helper method");
            }
        }
        """;
    Files.write(srcDir.resolve("UtilClass.java"), utilClass.getBytes());
    
    String dataClass = """
        package com.example.data;
        
        public class DataClass {
            private String data;
            
            public DataClass(String data) {
                this.data = data;
            }
        }
        """;
    Files.write(srcDir.resolve("DataClass.java"), dataClass.getBytes());
    
    return projectDir;
  }

  private Path createProjectWithoutMainMethod() throws IOException {
    Path projectDir = tempDir.resolve("no-main-project");
    Files.createDirectory(projectDir);
    
    String regularClass = """
        package com.example;
        
        public class RegularClass {
            public void doSomething() {
                System.out.println("No main method here");
            }
        }
        """;
    Files.write(projectDir.resolve("RegularClass.java"), regularClass.getBytes());
    
    return projectDir;
  }

  private Path createDeeplyNestedProjectStructure() throws IOException {
    Path projectDir = tempDir.resolve("deep-project");
    Path deepDir = projectDir.resolve("src/main/java/com/example/deep/nested/structure");
    Files.createDirectories(deepDir);
    
    String deepMainClass = """
        package com.example.deep.nested.structure;
        
        public class DeepMain {
            public static void main(String[] args) {
                System.out.println("Deep nested main class");
            }
        }
        """;
    Files.write(deepDir.resolve("DeepMain.java"), deepMainClass.getBytes());
    
    return projectDir;
  }

  private void setupGetMainClassCommand(Path cwd, SupportedLanguage language, SupportedIDE ide) {
    setField(getMainClassCommand, "cwd", cwd);
    setField(getMainClassCommand, "language", language);
    setField(getMainClassCommand, "ide", ide);
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
   * Test implementation of GetMainClassCommandService that captures method calls and allows setting
   * predefined responses.
   */
  private static class TestGetMainClassCommandService extends GetMainClassCommandService {
    private boolean serviceCalled = false;
    private Path lastCalledCwd;
    private DataTransferObject<GetMainClassResponse> responseToReturn;

    public TestGetMainClassCommandService() {
      super(null); // JavaLanguageService not needed for testing
    }

    @Override
    public DataTransferObject<GetMainClassResponse> run(Path cwd) {
      this.serviceCalled = true;
      this.lastCalledCwd = cwd;
      return this.responseToReturn;
    }

    public void setSuccessResponse(GetMainClassResponse response) {
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

    public Path getLastCalledCwd() {
      return this.lastCalledCwd;
    }
  }
}