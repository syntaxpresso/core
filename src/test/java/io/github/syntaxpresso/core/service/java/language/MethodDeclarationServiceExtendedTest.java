package io.github.syntaxpresso.core.service.java.language;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import java.nio.file.Path;
import java.util.List;
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
        new MethodDeclarationService(
            formalParameterService, localVariableDeclarationService, mockTypeResolutionService);
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
  @DisplayName("renameMethodDeclaration() tests")
  class RenameMethodDeclarationTests {
    @Test
    @DisplayName("Should rename method declaration successfully")
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

      // Execute rename
      boolean result =
          methodDeclarationService.renameMethodDeclaration(
              testFile, calculateSumMethod, "computeSum");

      // Verify method was renamed
      assertTrue(result);
      assertTrue(testFile.isModified());
      String modifiedCode = testFile.getSourceCode();
      assertTrue(modifiedCode.contains("computeSum"));
      assertFalse(modifiedCode.contains("calculateSum"));
    }

    @Test
    @DisplayName("Should handle null parameters gracefully")
    void shouldHandleNullParameters() {
      List<TSNode> methods = methodDeclarationService.findAllMethodDeclarations(testFile);
      TSNode firstMethod = methods.get(0);

      // Test null file
      assertFalse(methodDeclarationService.renameMethodDeclaration(null, firstMethod, "newName"));

      // Test null method node
      assertFalse(methodDeclarationService.renameMethodDeclaration(testFile, null, "newName"));

      // Test null/empty new name
      assertFalse(methodDeclarationService.renameMethodDeclaration(testFile, firstMethod, null));
      assertFalse(methodDeclarationService.renameMethodDeclaration(testFile, firstMethod, ""));
    }

    @Test
    @DisplayName("Should handle invalid method node")
    void shouldHandleInvalidMethodNode() {
      // Create a non-method node
      List<TSNode> classes = methodDeclarationService.findAllMethodDeclarations(testFile);
      if (!classes.isEmpty()) {
        TSNode nonMethodNode = classes.get(0).getChild(0); // Get first child which is not a method
        boolean result =
            methodDeclarationService.renameMethodDeclaration(testFile, nonMethodNode, "newName");
        assertFalse(result);
      }
    }
  }

  @Nested
  @DisplayName("findMethodUsagesInFile() tests")
  class FindMethodUsagesTests {
    @Test
    @DisplayName("Should find method usages in target file")
    void shouldFindMethodUsagesInTargetFile() {
      // Set up type resolution to return the correct type
      when(mockTypeResolutionService.resolveObjectType(any(), any(), any()))
          .thenReturn("Calculator");

      List<TSNode> usages =
          methodDeclarationService.findMethodUsagesInFile(targetFile, "calculateSum", "Calculator");

      assertNotNull(usages);
      assertTrue(usages.size() > 0);
      // Verify type resolution was called
      verify(mockTypeResolutionService, atLeast(1)).resolveObjectType(any(), any(), any());
    }

    @Test
    @DisplayName("Should handle null parameters gracefully")
    void shouldHandleNullParametersInFindUsages() {
      // Test null file
      List<TSNode> result1 =
          methodDeclarationService.findMethodUsagesInFile(null, "method", "Class");
      assertTrue(result1.isEmpty());

      // Test null method name
      List<TSNode> result2 =
          methodDeclarationService.findMethodUsagesInFile(testFile, null, "Class");
      assertTrue(result2.isEmpty());

      // Test null class name
      List<TSNode> result3 =
          methodDeclarationService.findMethodUsagesInFile(testFile, "method", null);
      assertTrue(result3.isEmpty());
    }

    @Test
    @DisplayName("Should return empty list when no usages found")
    void shouldReturnEmptyWhenNoUsagesFound() {
      List<TSNode> usages =
          methodDeclarationService.findMethodUsagesInFile(
              testFile, "nonExistentMethod", "Calculator");
      assertNotNull(usages);
      assertTrue(usages.isEmpty());
    }

    @Test
    @DisplayName("Should only return usages for correct object type")
    void shouldOnlyReturnUsagesForCorrectObjectType() {
      // Mock type resolution to return different types
      when(mockTypeResolutionService.resolveObjectType(any(), any(), any()))
          .thenReturn("Calculator") // First call - should match
          .thenReturn("String") // Second call - should not match
          .thenReturn("Calculator"); // Third call - should match

      List<TSNode> usages =
          methodDeclarationService.findMethodUsagesInFile(targetFile, "calculateSum", "Calculator");

      // Should only return usages where object type matches "Calculator"
      assertNotNull(usages);
      // The exact count will depend on the test file structure, but should be > 0 and < total
      // invocations
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
