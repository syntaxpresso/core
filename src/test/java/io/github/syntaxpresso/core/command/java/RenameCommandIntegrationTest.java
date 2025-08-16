package io.github.syntaxpresso.core.command.java;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.github.syntaxpresso.core.common.DataTransferObject;
import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import io.github.syntaxpresso.core.service.extra.JavaIdentifierType;
import io.github.syntaxpresso.core.service.java.JavaService;
import io.github.syntaxpresso.core.service.java.extra.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.treesitter.TSNode;

@ExtendWith(MockitoExtension.class)
@DisplayName("RenameCommand Integration Tests")
class RenameCommandIntegrationTest {
  @Mock private JavaService mockJavaService;
  @Mock private ProgramService mockProgramService;
  @Mock private ClassDeclarationService mockClassDeclarationService;
  @Mock private MethodDeclarationService mockMethodDeclarationService;
  @Mock private PackageDeclarationService mockPackageDeclarationService;
  @Mock private TypeResolutionService mockTypeResolutionService;
  private RenameCommand renameCommand;
  private TSFile testFile;
  private TSFile clientFile;
  @TempDir Path tempDir;

  @BeforeEach
  void setUp() throws IOException {
    // Create test files
    String calculatorCode =
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
    String clientCode =
        """
        package io.github.test;
        public class Client {
          private Calculator calc;
          public void doWork() {
            Calculator localCalc = new Calculator();
            calc.calculateSum(1, 2);
            localCalc.calculateSum(3, 4);
          }
          public void processData(Calculator parameter) {
            parameter.calculateSum(5, 6);
          }
        }
        """;
    Path calculatorPath = tempDir.resolve("Calculator.java");
    Path clientPath = tempDir.resolve("Client.java");
    Files.write(calculatorPath, calculatorCode.getBytes());
    Files.write(clientPath, clientCode.getBytes());
    testFile = new TSFile(SupportedLanguage.JAVA, calculatorPath);
    clientFile = new TSFile(SupportedLanguage.JAVA, clientPath);
    // Setup mocks
    when(mockJavaService.getProgramService()).thenReturn(mockProgramService);
    when(mockProgramService.getPackageDeclarationService())
        .thenReturn(mockPackageDeclarationService);
    when(mockProgramService.getClassDeclarationService()).thenReturn(mockClassDeclarationService);
    when(mockProgramService.getTypeResolutionService()).thenReturn(mockTypeResolutionService);
    when(mockClassDeclarationService.getMethodDeclarationService())
        .thenReturn(mockMethodDeclarationService);
    when(mockPackageDeclarationService.getPackageName(any()))
        .thenReturn(Optional.of("io.github.test"));
    renameCommand = new RenameCommand(mockJavaService);
    // Use reflection to set the required fields
    setField(renameCommand, "cwd", tempDir);
    setField(renameCommand, "filePath", calculatorPath);
    setField(renameCommand, "newName", "computeSum");
  }

  private void setField(Object target, String fieldName, Object value) {
    try {
      var field = target.getClass().getDeclaredField(fieldName);
      field.setAccessible(true);
      field.set(target, value);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Nested
  @DisplayName("Method rename integration tests")
  @MockitoSettings(strictness = Strictness.LENIENT)
  class MethodRenameIntegrationTests {
    @Test
    @DisplayName("Should perform end-to-end method rename successfully")
    void shouldPerformEndToEndMethodRenameSuccessfully() throws IOException {
      // Find the calculateSum method in the test file
      List<TSNode> methods = testFile.query("(method_declaration) @method");
      TSNode calculateSumMethod = null;
      for (TSNode method : methods) {
        TSNode nameNode = method.getChildByFieldName("name");
        if (nameNode != null && "calculateSum".equals(testFile.getTextFromNode(nameNode))) {
          calculateSumMethod = method;
          break;
        }
      }
      assertNotNull(calculateSumMethod, "Should find calculateSum method");
      // Set up the line and column for the method name
      TSNode methodNameNode = calculateSumMethod.getChildByFieldName("name");
      int line = methodNameNode.getStartPoint().getRow();
      int column = methodNameNode.getStartPoint().getColumn();
      setField(renameCommand, "line", line);
      setField(renameCommand, "column", column);
      // Mock the identifier type detection
      when(mockJavaService.getIdentifierType(any(TSNode.class)))
          .thenReturn(JavaIdentifierType.METHOD_NAME);
      // Mock the class lookup
      TSNode mockClassNode = mock(TSNode.class);
      when(mockClassDeclarationService.getMainClass(any())).thenReturn(Optional.of(mockClassNode));
      when(mockClassDeclarationService.getClassName(any(), eq(mockClassNode)))
          .thenReturn(Optional.of("Calculator"));
      // Mock file discovery
      when(mockJavaService.getAllJavaFilesFromCwd(tempDir))
          .thenReturn(List.of(testFile, clientFile));
      // Mock method invocation finding
      List<TSNode> clientMethodInvocations = clientFile.query("(method_invocation) @invocation");
      when(mockMethodDeclarationService.findAllMethodInvocations(clientFile))
          .thenReturn(clientMethodInvocations);
      // Mock method invocation analysis
      for (TSNode invocation : clientMethodInvocations) {
        TSNode objectNode = invocation.getChildByFieldName("object");
        TSNode nameNode = invocation.getChildByFieldName("name");
        if (objectNode != null && nameNode != null) {
          when(mockMethodDeclarationService.getMethodInvocationObject(invocation))
              .thenReturn(Optional.of(objectNode));
          when(mockMethodDeclarationService.getMethodInvocationName(invocation))
              .thenReturn(Optional.of(nameNode));
          String methodName = clientFile.getTextFromNode(nameNode);
          if ("calculateSum".equals(methodName)) {
            // Mock type resolution to return Calculator for all objects
            when(mockTypeResolutionService.resolveObjectType(
                    eq(clientFile), eq(objectNode), eq(invocation)))
                .thenReturn("Calculator");
          }
        }
      }
      // Mock the rename method and usages
      when(mockMethodDeclarationService.renameMethodAndUsages(
              any(), any(), eq("calculateSum"), eq("computeSum"), any(), any(), any(), any()))
          .thenReturn(List.of(testFile, clientFile));
      // Execute the rename command
      DataTransferObject<Void> result = renameCommand.call();
      // Verify the command executed without errors
      assertNotNull(result, "Command should return a result");
      // Verify that the service methods were called
      verify(mockJavaService).getIdentifierType(any(TSNode.class));
      verify(mockMethodDeclarationService)
          .renameMethodAndUsages(any(), any(), any(), eq("computeSum"), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should handle class rename gracefully")
    void shouldHandleClassRenameGracefully() throws IOException {
      // Find the class name in the test file
      List<TSNode> classes = testFile.query("(class_declaration) @class");
      assertFalse(classes.isEmpty());
      TSNode classNode = classes.get(0);
      TSNode classNameNode = classNode.getChildByFieldName("name");
      assertNotNull(classNameNode);
      int line = classNameNode.getStartPoint().getRow();
      int column = classNameNode.getStartPoint().getColumn();
      setField(renameCommand, "line", line);
      setField(renameCommand, "column", column);
      setField(renameCommand, "newName", "AdvancedCalculator");
      // Mock the identifier type detection for class
      when(mockJavaService.getIdentifierType(any(TSNode.class)))
          .thenReturn(JavaIdentifierType.CLASS_NAME);
      // Mock file discovery for class rename
      when(mockJavaService.getAllJavaFilesFromCwd(tempDir))
          .thenReturn(List.of(testFile, clientFile));
      // Execute the rename command - should not throw an exception
      assertDoesNotThrow(
          () -> {
            renameCommand.call();
            // Result may be null due to mocked dependencies, but should not crash
          });
      verify(mockJavaService).getIdentifierType(any(TSNode.class));
    }

    @Test
    @DisplayName("Should handle missing package gracefully")
    void shouldHandleMissingPackageGracefully() throws IOException {
      when(mockPackageDeclarationService.getPackageName(any())).thenReturn(Optional.empty());
      setField(renameCommand, "line", 0);
      setField(renameCommand, "column", 0);
      DataTransferObject<Void> result = renameCommand.call();
      assertNull(result, "Should return null when package is missing");
    }

    @Test
    @DisplayName("Should handle empty current name gracefully")
    void shouldHandleEmptyCurrentNameGracefully() throws IOException {
      // Create a scenario where the node at the position has no text
      setField(renameCommand, "line", 1000); // Invalid line
      setField(renameCommand, "column", 1000); // Invalid column
      DataTransferObject<Void> result = renameCommand.call();
      assertNull(result, "Should return null when current name is empty");
    }

    @Test
    @DisplayName("Should handle unknown identifier type gracefully")
    void shouldHandleUnknownIdentifierTypeGracefully() throws IOException {
      // Use valid coordinates that will find a node
      List<TSNode> methods = testFile.query("(method_declaration) @method");
      TSNode calculateSumMethod = methods.get(0);
      TSNode methodNameNode = calculateSumMethod.getChildByFieldName("name");
      int line = methodNameNode.getStartPoint().getRow();
      int column = methodNameNode.getStartPoint().getColumn();
      setField(renameCommand, "line", line);
      setField(renameCommand, "column", column);
      when(mockJavaService.getIdentifierType(any(TSNode.class))).thenReturn(null);
      // Should not throw an exception, may return null
      assertDoesNotThrow(
          () -> {
            renameCommand.call();
            // Result may be null for unknown identifier types, which is acceptable
          });
    }
  }

  @Nested
  @DisplayName("Error handling integration tests")
  @MockitoSettings(strictness = Strictness.LENIENT)
  class ErrorHandlingIntegrationTests {
    @Test
    @DisplayName("Should handle file reading errors gracefully")
    void shouldHandleFileReadingErrorsGracefully() {
      // Set an invalid file path
      setField(renameCommand, "filePath", Paths.get("/nonexistent/file.java"));
      assertThrows(
          RuntimeException.class,
          () -> {
            renameCommand.call();
          },
          "Should throw exception for invalid file path");
    }

    @Test
    @DisplayName("Should handle service call failures gracefully")
    void shouldHandleServiceCallFailuresGracefully() throws IOException {
      // Use valid coordinates that will find a node
      List<TSNode> methods = testFile.query("(method_declaration) @method");
      TSNode calculateSumMethod = methods.get(0);
      TSNode methodNameNode = calculateSumMethod.getChildByFieldName("name");
      int line = methodNameNode.getStartPoint().getRow();
      int column = methodNameNode.getStartPoint().getColumn();
      setField(renameCommand, "line", line);
      setField(renameCommand, "column", column);
      // Mock service to throw exception
      when(mockJavaService.getIdentifierType(any(TSNode.class)))
          .thenThrow(new RuntimeException("Service error"));
      assertThrows(
          RuntimeException.class,
          () -> {
            renameCommand.call();
          },
          "Should propagate service exceptions");
    }
  }

  @Nested
  @DisplayName("Integration with real services")
  @MockitoSettings(strictness = Strictness.LENIENT)
  class RealServiceIntegrationTests {
    @Test
    @DisplayName("Should work with real JavaService for simple cases")
    void shouldWorkWithRealJavaServiceForSimpleCases() throws IOException {
      // Create a real JavaService for testing
      JavaService realJavaService = new JavaService();
      RenameCommand realRenameCommand = new RenameCommand(realJavaService);
      setField(realRenameCommand, "cwd", tempDir);
      setField(realRenameCommand, "filePath", tempDir.resolve("Calculator.java"));
      setField(realRenameCommand, "newName", "computeSum");
      // Find the method position
      List<TSNode> methods = testFile.query("(method_declaration) @method");
      TSNode calculateSumMethod = null;
      for (TSNode method : methods) {
        TSNode nameNode = method.getChildByFieldName("name");
        if (nameNode != null && "calculateSum".equals(testFile.getTextFromNode(nameNode))) {
          calculateSumMethod = method;
          break;
        }
      }
      if (calculateSumMethod != null) {
        TSNode methodNameNode = calculateSumMethod.getChildByFieldName("name");
        int line = methodNameNode.getStartPoint().getRow();
        int column = methodNameNode.getStartPoint().getColumn();
        setField(realRenameCommand, "line", line);
        setField(realRenameCommand, "column", column);
        // This should not throw an exception
        assertDoesNotThrow(
            () -> {
              realRenameCommand.call();
              // Result might be null due to service limitations, but should not crash
            });
      }
    }
  }
}
