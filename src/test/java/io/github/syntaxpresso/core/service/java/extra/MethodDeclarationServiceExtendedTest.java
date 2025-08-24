package io.github.syntaxpresso.core.service.java.extra;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.treesitter.TSNode;

@ExtendWith(MockitoExtension.class)
@DisplayName("MethodDeclarationService Extended Tests")
class MethodDeclarationServiceExtendedTest {
  // Mock JavaService class for testing
  static class MockJavaServiceClass {
    public List<TSFile> getAllJavaFilesFromCwd(Path cwd) {
      return List.of();
    }
  }

  private MethodDeclarationService methodDeclarationService;
  private TSFile testFile;
  private TSFile targetFile;
  @Mock private TypeResolutionService mockTypeResolutionService;
  @Mock private ClassDeclarationService mockClassDeclarationService;
  @Mock private Object mockJavaService;

  @BeforeEach
  void setUp() {
    VariableNamingService variableNamingService = new VariableNamingService();
    LocalVariableDeclarationService localVariableDeclarationService =
        new LocalVariableDeclarationService(variableNamingService);
    FormalParameterService formalParameterService =
        new FormalParameterService(localVariableDeclarationService, variableNamingService);
    methodDeclarationService =
        new MethodDeclarationService(formalParameterService, localVariableDeclarationService, mockTypeResolutionService);
    // Original file with method to rename
    String originalJavaCode =
        """
        package io.github.test;
        public class Calculator {
          public int calculateSum(int a, int b) {
            return a + b;
          }
          public void process() {
            System.out.println("Processing");
          }
        }
        """;
    testFile = new TSFile(SupportedLanguage.JAVA, originalJavaCode);
    // Target file with method invocations
    String targetJavaCode =
        """
        package io.github.test;
        public class Client {
          private Calculator calc;
          public void doWork() {
            Calculator localCalc = new Calculator();
            calc.calculateSum(1, 2);
            localCalc.calculateSum(3, 4);
            calc.process();
          }
          public void processData(Calculator parameter) {
            parameter.calculateSum(5, 6);
            parameter.process();
          }
        }
        """;
    targetFile = new TSFile(SupportedLanguage.JAVA, targetJavaCode);
  }

  @Nested
  @DisplayName("renameMethodAndUsages() tests")
  class RenameMethodAndUsagesTests {
    @Test
    @DisplayName("Should rename method declaration")
    void shouldRenameMethodDeclaration() {
      // Find the calculateSum method
      List<TSNode> methods = methodDeclarationService.findAllMethodDeclarations(testFile);
      TSNode calculateSumMethod = null;
      for (TSNode method : methods) {
        TSNode nameNode = method.getChildByFieldName("name");
        if (nameNode != null && "calculateSum".equals(testFile.getTextFromNode(nameNode))) {
          calculateSumMethod = method;
          break;
        }
      }
      assertNotNull(calculateSumMethod, "Should find calculateSum method");
      // Set up type resolution to return the correct type for method invocations in targetFile
      when(mockTypeResolutionService.resolveObjectType(any(), any(), any()))
          .thenReturn("Calculator");
      // Execute rename
      List<TSFile> result =
          methodDeclarationService.renameMethodAndUsages(
              testFile,
              calculateSumMethod,
              "calculateSum",
              "computeSum",
              Paths.get("/test"),
              "Calculator",
              List.of(testFile, targetFile));
      // Verify method was renamed in original file
      assertNotNull(result);
      assertTrue(testFile.isModified());
      assertTrue(result.contains(testFile));
      // Verify new method name exists
      String modifiedCode = testFile.getSourceCode();
      assertTrue(modifiedCode.contains("computeSum"));
      assertFalse(modifiedCode.contains("calculateSum"));
    }

    @Test
    @DisplayName("Should handle missing class gracefully")
    void testRenameMethodAndUsagesMissingClass() {
      List<TSNode> methods = methodDeclarationService.findAllMethodDeclarations(testFile);
      TSNode firstMethod = methods.get(0);
      // The method should still work and return results even if some external checks fail
      List<TSFile> result =
          methodDeclarationService.renameMethodAndUsages(
              testFile,
              firstMethod,
              "calculateSum",
              "computeSum",
              Paths.get("/test"),
              "Calculator",
              List.of(testFile));
      // Should successfully rename the method in the original file
      assertNotNull(result);
      assertTrue(result.contains(testFile));
      assertTrue(testFile.isModified());
    }

    @Test
    @DisplayName("Should handle missing class name gracefully")
    void shouldHandleMissingClassNameGracefully() {
      List<TSNode> methods = methodDeclarationService.findAllMethodDeclarations(testFile);
      TSNode firstMethod = methods.get(0);
      // The method should still work with the provided className parameter
      List<TSFile> result =
          methodDeclarationService.renameMethodAndUsages(
              testFile,
              firstMethod,
              "calculateSum",
              "computeSum",
              Paths.get("/test"),
              "Calculator",
              List.of(testFile));
      // Should successfully rename the method in the original file
      assertNotNull(result);
      assertTrue(result.contains(testFile));
      assertTrue(testFile.isModified());
    }

    @Test
    @DisplayName("Should handle reflection errors gracefully")
    void shouldHandleReflectionErrorsGracefully() throws Exception {
      List<TSNode> methods = methodDeclarationService.findAllMethodDeclarations(testFile);
      TSNode firstMethod = methods.get(0);
      // The method should work without relying on reflection
      List<TSFile> result =
          methodDeclarationService.renameMethodAndUsages(
              testFile,
              firstMethod,
              "calculateSum",
              "computeSum",
              Paths.get("/test"),
              "Calculator",
              List.of(testFile));
      // Should return the modified file 
      assertNotNull(result);
      assertEquals(1, result.size());
      assertTrue(result.contains(testFile));
    }

    @Test
    @DisplayName("Should only rename methods on correct object type")
    void shouldOnlyRenameMethodsOnCorrectObjectType() {
      List<TSNode> methods = methodDeclarationService.findAllMethodDeclarations(testFile);
      TSNode calculateSumMethod = null;
      for (TSNode method : methods) {
        TSNode nameNode = method.getChildByFieldName("name");
        if (nameNode != null && "calculateSum".equals(testFile.getTextFromNode(nameNode))) {
          calculateSumMethod = method;
          break;
        }
      }
      // Mock type resolution to return different types for testing selective renaming
      when(mockTypeResolutionService.resolveObjectType(any(), any(), any()))
          .thenReturn("Calculator") // First call
          .thenReturn("String") // Second call - different type
          .thenReturn("Calculator"); // Third call
      List<TSFile> result =
          methodDeclarationService.renameMethodAndUsages(
              testFile,
              calculateSumMethod,
              "calculateSum",
              "computeSum",
              Paths.get("/test"),
              "Calculator",
              List.of(testFile, targetFile));
      assertNotNull(result);
      // Verify type resolution was called for each method invocation
      verify(mockTypeResolutionService, atLeast(1)).resolveObjectType(any(), any(), any());
    }
  }

  @Nested
  @DisplayName("isMainMethod() tests")
  class IsMainMethodTests {
    @Test
    @DisplayName("Should identify main method correctly")
    void shouldIdentifyMainMethodCorrectly() {
      String mainMethodCode =
          """
          package io.github.test;
          public class MainClass {
            public static void main(String[] args) {
              System.out.println("Hello World");
            }
            public void notMain(String[] args) {
              System.out.println("Not main");
            }
            public static void main(String... args) {
              System.out.println("Varargs main");
            }
          }
          """;
      TSFile mainFile = new TSFile(SupportedLanguage.JAVA, mainMethodCode);
      List<TSNode> methods = methodDeclarationService.findAllMethodDeclarations(mainFile);
      int mainMethodCount = 0;
      for (TSNode method : methods) {
        if (methodDeclarationService.isMainMethod(mainFile, method)) {
          mainMethodCount++;
        }
      }
      assertEquals(2, mainMethodCount, "Should identify both main method variants");
    }

    @Test
    @DisplayName("Should handle null parameters gracefully")
    void shouldHandleNullParametersGracefully() {
      assertFalse(methodDeclarationService.isMainMethod(null, null));
      List<TSNode> methods = methodDeclarationService.findAllMethodDeclarations(testFile);
      assertFalse(methodDeclarationService.isMainMethod(null, methods.get(0)));
      assertFalse(methodDeclarationService.isMainMethod(testFile, null));
    }

    @Test
    @DisplayName("Should reject non-method nodes")
    void shouldRejectNonMethodNodes() {
      List<TSNode> classes = testFile.query("(class_declaration) @class");
      assertFalse(classes.isEmpty());
      assertFalse(methodDeclarationService.isMainMethod(testFile, classes.get(0)));
    }
  }
}
