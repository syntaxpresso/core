package io.github.syntaxpresso.core.service.java.extra;

import static org.junit.jupiter.api.Assertions.*;

import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.treesitter.TSNode;

@DisplayName("LocalVariableDeclarationService Tests")
class LocalVariableDeclarationServiceTest {
  private LocalVariableDeclarationService localVariableDeclarationService;
  private TSFile testFile;

  @BeforeEach
  void setUp() {
    VariableNamingService variableNamingService = new VariableNamingService();
    localVariableDeclarationService = new LocalVariableDeclarationService(variableNamingService);
    String javaCode =
        """
        package io.github.test;
        public class TestClass {
          private Calculator calculator;
          public void processData(Calculator param) {
            Calculator localCalc = new Calculator();
            String message = "test";
            int count = 5;
            Calculator backup = param;
            localCalc.process();
            message.length();
            count++;
            backup.validate();
          }
          public String anotherMethod() {
            Calculator helper = new Calculator();
            List<Calculator> calculators = new ArrayList<>();
            String result = helper.compute();
            calculators.add(helper);
            return result;
          }
          public void noLocalVars() {
            this.calculator.run();
          }
        }
        """;
    testFile = new TSFile(SupportedLanguage.JAVA, javaCode);
  }

  @Nested
  @DisplayName("findAllLocalVariableDeclarations() tests")
  class FindAllLocalVariableDeclarationsTests {
    @Test
    @DisplayName("Should find all local variable declarations in method")
    void shouldFindAllLocalVariableDeclarationsInMethod() {
      TSNode processDataMethod = findMethodByName("processData");
      assertNotNull(processDataMethod, "Should find processData method");
      List<TSNode> localVars =
          localVariableDeclarationService.findAllLocalVariableDeclarations(
              processDataMethod, testFile);
      assertEquals(
          4, localVars.size(), "Should find 4 local variable declarations in processData method");
    }

    @Test
    @DisplayName("Should find local variables in different method")
    void shouldFindLocalVariablesInDifferentMethod() {
      TSNode anotherMethod = findMethodByName("anotherMethod");
      assertNotNull(anotherMethod, "Should find anotherMethod");
      List<TSNode> localVars =
          localVariableDeclarationService.findAllLocalVariableDeclarations(anotherMethod, testFile);
      assertEquals(
          3, localVars.size(), "Should find 3 local variable declarations in anotherMethod");
    }

    @Test
    @DisplayName("Should return empty list for method without local variables")
    void shouldReturnEmptyListForMethodWithoutLocalVariables() {
      TSNode noLocalVarsMethod = findMethodByName("noLocalVars");
      assertNotNull(noLocalVarsMethod, "Should find noLocalVars method");
      List<TSNode> localVars =
          localVariableDeclarationService.findAllLocalVariableDeclarations(
              noLocalVarsMethod, testFile);
      assertTrue(
          localVars.isEmpty(), "Should return empty list for method without local variables");
    }

    @Test
    @DisplayName("Should handle invalid method node")
    void shouldHandleInvalidMethodNode() {
      List<TSNode> classNodes = testFile.query("(class_declaration) @class");
      List<TSNode> localVars =
          localVariableDeclarationService.findAllLocalVariableDeclarations(
              classNodes.get(0), testFile);
      assertTrue(localVars.isEmpty(), "Should return empty list for non-method node");
    }

    @Test
    @DisplayName("Should handle null parameters")
    void shouldHandleNullParameters() {
      TSNode method = findMethodByName("processData");
      List<TSNode> nullMethodResult =
          localVariableDeclarationService.findAllLocalVariableDeclarations(null, testFile);
      assertTrue(nullMethodResult.isEmpty(), "Should handle null method node");
      List<TSNode> nullFileResult =
          localVariableDeclarationService.findAllLocalVariableDeclarations(method, null);
      assertTrue(nullFileResult.isEmpty(), "Should handle null file gracefully");
    }

    private TSNode findMethodByName(String methodName) {
      List<TSNode> methods = testFile.query("(method_declaration) @method");
      for (TSNode method : methods) {
        TSNode nameNode = method.getChildByFieldName("name");
        if (nameNode != null && methodName.equals(testFile.getTextFromNode(nameNode))) {
          return method;
        }
      }
      return null;
    }
  }

  @Nested
  @DisplayName("getVariableTypeNode() tests")
  class GetVariableTypeNodeTests {
    @Test
    @DisplayName("Should find type node in local variable declaration")
    void shouldFindTypeNodeInLocalVariableDeclaration() {
      TSNode processDataMethod = findMethodByName("processData");
      List<TSNode> localVars =
          localVariableDeclarationService.findAllLocalVariableDeclarations(
              processDataMethod, testFile);
      // Find Calculator local variable
      TSNode calculatorVar = null;
      for (TSNode localVar : localVars) {
        String varText = testFile.getTextFromNode(localVar);
        if (varText.contains("Calculator localCalc")) {
          calculatorVar = localVar;
          break;
        }
      }
      assertNotNull(calculatorVar, "Should find Calculator local variable");
      Optional<TSNode> typeNode =
          localVariableDeclarationService.getVariableTypeNode(
              calculatorVar, testFile, "Calculator");
      assertTrue(typeNode.isPresent(), "Should find Calculator type node");
      assertEquals("Calculator", testFile.getTextFromNode(typeNode.get()));
    }

    @Test
    @DisplayName("Should return empty for non-matching type")
    void shouldReturnEmptyForNonMatchingType() {
      TSNode processDataMethod = findMethodByName("processData");
      List<TSNode> localVars =
          localVariableDeclarationService.findAllLocalVariableDeclarations(
              processDataMethod, testFile);
      // Find String local variable
      TSNode stringVar = null;
      for (TSNode localVar : localVars) {
        String varText = testFile.getTextFromNode(localVar);
        if (varText.contains("String message")) {
          stringVar = localVar;
          break;
        }
      }
      assertNotNull(stringVar, "Should find String local variable");
      Optional<TSNode> typeNode =
          localVariableDeclarationService.getVariableTypeNode(stringVar, testFile, "Calculator");
      assertFalse(typeNode.isPresent(), "Should not find Calculator type in String variable");
    }

    @Test
    @DisplayName("Should handle invalid local variable node")
    void shouldHandleInvalidLocalVariableNode() {
      List<TSNode> classNodes = testFile.query("(class_declaration) @class");
      Optional<TSNode> typeNode =
          localVariableDeclarationService.getVariableTypeNode(
              classNodes.get(0), testFile, "Calculator");
      assertFalse(typeNode.isPresent(), "Should return empty for non-local-variable node");
    }

    @Test
    @DisplayName("Should handle null parameters")
    void shouldHandleNullParameters() {
      TSNode processDataMethod = findMethodByName("processData");
      List<TSNode> localVars =
          localVariableDeclarationService.findAllLocalVariableDeclarations(
              processDataMethod, testFile);
      TSNode localVar = localVars.get(0);
      assertFalse(
          localVariableDeclarationService
              .getVariableTypeNode(null, testFile, "Calculator")
              .isPresent());
      assertFalse(
          localVariableDeclarationService
              .getVariableTypeNode(localVar, null, "Calculator")
              .isPresent());
      assertFalse(
          localVariableDeclarationService
              .getVariableTypeNode(localVar, testFile, null)
              .isPresent());
    }

    private TSNode findMethodByName(String methodName) {
      List<TSNode> methods = testFile.query("(method_declaration) @method");
      for (TSNode method : methods) {
        TSNode nameNode = method.getChildByFieldName("name");
        if (nameNode != null && methodName.equals(testFile.getTextFromNode(nameNode))) {
          return method;
        }
      }
      return null;
    }
  }

  @Nested
  @DisplayName("getVariableNameNode() tests")
  class GetVariableNameNodeTests {
    @Test
    @DisplayName("Should extract variable name from local declaration")
    void shouldExtractVariableNameFromLocalDeclaration() {
      TSNode processDataMethod = findMethodByName("processData");
      List<TSNode> localVars =
          localVariableDeclarationService.findAllLocalVariableDeclarations(
              processDataMethod, testFile);
      // Find Calculator local variable
      TSNode calculatorVar = null;
      for (TSNode localVar : localVars) {
        String varText = testFile.getTextFromNode(localVar);
        if (varText.contains("Calculator localCalc")) {
          calculatorVar = localVar;
          break;
        }
      }
      assertNotNull(calculatorVar, "Should find Calculator local variable");
      Optional<TSNode> nameNode =
          localVariableDeclarationService.getVariableNameNode(calculatorVar, testFile);
      assertTrue(nameNode.isPresent(), "Should find variable name node");
      assertEquals("localCalc", testFile.getTextFromNode(nameNode.get()));
    }

    @Test
    @DisplayName("Should handle different variable types")
    void shouldHandleDifferentVariableTypes() {
      TSNode processDataMethod = findMethodByName("processData");
      List<TSNode> localVars =
          localVariableDeclarationService.findAllLocalVariableDeclarations(
              processDataMethod, testFile);
      // Check each local variable
      boolean foundString = false;
      boolean foundInt = false;
      for (TSNode localVar : localVars) {
        Optional<TSNode> nameNode =
            localVariableDeclarationService.getVariableNameNode(localVar, testFile);
        if (nameNode.isPresent()) {
          String varName = testFile.getTextFromNode(nameNode.get());
          if ("message".equals(varName)) {
            foundString = true;
          } else if ("count".equals(varName)) {
            foundInt = true;
          }
        }
      }
      assertTrue(foundString, "Should find 'message' variable name");
      assertTrue(foundInt, "Should find 'count' variable name");
    }

    @Test
    @DisplayName("Should handle invalid declaration node")
    void shouldHandleInvalidDeclarationNode() {
      List<TSNode> classNodes = testFile.query("(class_declaration) @class");
      Optional<TSNode> nameNode =
          localVariableDeclarationService.getVariableNameNode(classNodes.get(0), testFile);
      assertFalse(nameNode.isPresent(), "Should return empty for non-local-variable node");
    }

    @Test
    @DisplayName("Should handle null parameters")
    void shouldHandleNullParameters() {
      TSNode processDataMethod = findMethodByName("processData");
      List<TSNode> localVars =
          localVariableDeclarationService.findAllLocalVariableDeclarations(
              processDataMethod, testFile);
      TSNode localVar = localVars.get(0);
      assertFalse(localVariableDeclarationService.getVariableNameNode(null, testFile).isPresent());
      assertFalse(localVariableDeclarationService.getVariableNameNode(localVar, null).isPresent());
    }

    private TSNode findMethodByName(String methodName) {
      List<TSNode> methods = testFile.query("(method_declaration) @method");
      for (TSNode method : methods) {
        TSNode nameNode = method.getChildByFieldName("name");
        if (nameNode != null && methodName.equals(testFile.getTextFromNode(nameNode))) {
          return method;
        }
      }
      return null;
    }
  }

  @Nested
  @DisplayName("getVariableInstanceNode() tests")
  class GetVariableInstanceNodeTests {
    @Test
    @DisplayName("Should find instance node in variable with object creation")
    void shouldFindInstanceNodeInVariableWithObjectCreation() {
      TSNode processDataMethod = findMethodByName("processData");
      List<TSNode> localVars =
          localVariableDeclarationService.findAllLocalVariableDeclarations(
              processDataMethod, testFile);
      // Find variable with object creation (Calculator localCalc = new Calculator())
      TSNode varWithInstance = null;
      for (TSNode localVar : localVars) {
        String varText = testFile.getTextFromNode(localVar);
        if (varText.contains("new Calculator()")) {
          varWithInstance = localVar;
          break;
        }
      }
      assertNotNull(varWithInstance, "Should find local variable with object creation");
      Optional<TSNode> instanceNode =
          localVariableDeclarationService.getVariableInstanceNode(
              varWithInstance, testFile, "Calculator");
      assertTrue(instanceNode.isPresent(), "Should find Calculator instance node");
      assertEquals("Calculator", testFile.getTextFromNode(instanceNode.get()));
    }

    @Test
    @DisplayName("Should return empty for variable without object creation")
    void shouldReturnEmptyForVariableWithoutObjectCreation() {
      TSNode processDataMethod = findMethodByName("processData");
      List<TSNode> localVars =
          localVariableDeclarationService.findAllLocalVariableDeclarations(
              processDataMethod, testFile);
      // Find String variable (no object creation)
      TSNode stringVar = null;
      for (TSNode localVar : localVars) {
        String varText = testFile.getTextFromNode(localVar);
        if (varText.contains("String message")) {
          stringVar = localVar;
          break;
        }
      }
      assertNotNull(stringVar, "Should find String local variable");
      Optional<TSNode> instanceNode =
          localVariableDeclarationService.getVariableInstanceNode(stringVar, testFile, "String");
      assertFalse(
          instanceNode.isPresent(),
          "Should not find instance node in variable without object creation");
    }

    @Test
    @DisplayName("Should handle invalid declaration node")
    void shouldHandleInvalidDeclarationNode() {
      List<TSNode> classNodes = testFile.query("(class_declaration) @class");
      Optional<TSNode> instanceNode =
          localVariableDeclarationService.getVariableInstanceNode(
              classNodes.get(0), testFile, "Calculator");
      assertFalse(instanceNode.isPresent(), "Should return empty for non-local-variable node");
    }

    private TSNode findMethodByName(String methodName) {
      List<TSNode> methods = testFile.query("(method_declaration) @method");
      for (TSNode method : methods) {
        TSNode nameNode = method.getChildByFieldName("name");
        if (nameNode != null && methodName.equals(testFile.getTextFromNode(nameNode))) {
          return method;
        }
      }
      return null;
    }
  }

  @Nested
  @DisplayName("isLocalVariableDeclaration() tests")
  class IsLocalVariableDeclarationTests {
    @Test
    @DisplayName("Should identify local variable declarations")
    void shouldIdentifyLocalVariableDeclarations() {
      TSNode processDataMethod = findMethodByName("processData");
      List<TSNode> localVars =
          localVariableDeclarationService.findAllLocalVariableDeclarations(
              processDataMethod, testFile);
      TSNode calculatorVar = localVars.get(0);
      Optional<TSNode> nameNode =
          localVariableDeclarationService.getVariableNameNode(calculatorVar, testFile);
      assertTrue(nameNode.isPresent(), "Should have name node to test");
      boolean isDeclaration =
          localVariableDeclarationService.isLocalVariableDeclaration(nameNode.get());
      assertTrue(isDeclaration, "Should identify local variable declaration");
    }

    @Test
    @DisplayName("Should not identify non-variable identifiers as declarations")
    void shouldNotIdentifyNonVariableIdentifiersAsDeclarations() {
      // Find method invocation identifiers
      List<TSNode> identifiers = testFile.query("(method_invocation name: (identifier) @id)");
      assertFalse(identifiers.isEmpty(), "Should have method invocation identifiers to test");
      TSNode invocationId = identifiers.get(0);
      boolean isDeclaration =
          localVariableDeclarationService.isLocalVariableDeclaration(invocationId);
      assertFalse(isDeclaration, "Should not identify method invocation as variable declaration");
    }

    @Test
    @DisplayName("Should handle null and invalid nodes")
    void shouldHandleNullAndInvalidNodes() {
      assertFalse(
          localVariableDeclarationService.isLocalVariableDeclaration(null),
          "Should handle null node");
      List<TSNode> classNodes = testFile.query("(class_declaration) @class");
      TSNode classNode = classNodes.get(0);
      assertFalse(
          localVariableDeclarationService.isLocalVariableDeclaration(classNode),
          "Should handle non-identifier node");
    }

    private TSNode findMethodByName(String methodName) {
      List<TSNode> methods = testFile.query("(method_declaration) @method");
      for (TSNode method : methods) {
        TSNode nameNode = method.getChildByFieldName("name");
        if (nameNode != null && methodName.equals(testFile.getTextFromNode(nameNode))) {
          return method;
        }
      }
      return null;
    }
  }

  @Nested
  @DisplayName("isLocalVariableUsage() tests")
  class IsLocalVariableUsageTests {
    @Test
    @DisplayName("Should identify local variable usage")
    void shouldIdentifyLocalVariableUsage() {
      TSNode processDataMethod = findMethodByName("processData");
      // Find usage of localCalc in method invocation
      List<TSNode> identifiers = testFile.query(processDataMethod, "(identifier) @id");
      TSNode localCalcUsage = null;
      for (TSNode identifier : identifiers) {
        String idText = testFile.getTextFromNode(identifier);
        if ("localCalc".equals(idText)) {
          // Check if it's in a method invocation context
          TSNode parent = identifier.getParent();
          if (parent != null && "method_invocation".equals(parent.getType())) {
            localCalcUsage = identifier;
            break;
          }
        }
      }
      if (localCalcUsage != null) {
        boolean isUsage =
            localVariableDeclarationService.isLocalVariableUsage(
                localCalcUsage, processDataMethod, "localCalc", testFile);
        assertTrue(isUsage, "Should identify local variable usage");
      }
    }

    @Test
    @DisplayName("Should not identify parameter as local variable usage")
    void shouldNotIdentifyParameterAsLocalVariableUsage() {
      TSNode processDataMethod = findMethodByName("processData");
      // Find usage of param in method
      List<TSNode> identifiers = testFile.query(processDataMethod, "(identifier) @id");
      TSNode paramUsage = null;
      for (TSNode identifier : identifiers) {
        String idText = testFile.getTextFromNode(identifier);
        if ("param".equals(idText)) {
          paramUsage = identifier;
          break;
        }
      }
      if (paramUsage != null) {
        boolean isUsage =
            localVariableDeclarationService.isLocalVariableUsage(
                paramUsage, processDataMethod, "param", testFile);
        assertFalse(isUsage, "Should not identify parameter as local variable usage");
      }
    }

    @Test
    @DisplayName("Should handle identifier before declaration")
    void shouldHandleIdentifierBeforeDeclaration() {
      // This test would require a scenario where an identifier appears before its declaration
      // In normal Java, this shouldn't happen, but we should handle it gracefully
      String testCode =
          """
          public void testMethod() {
            // usage before declaration (invalid Java, but we should handle it)
            Calculator calc = new Calculator();
          }
          """;
      TSFile testMethodFile = new TSFile(SupportedLanguage.JAVA, testCode);
      List<TSNode> methods = testMethodFile.query("(method_declaration) @method");
      if (!methods.isEmpty()) {
        TSNode method = methods.get(0);
        List<TSNode> identifiers = testMethodFile.query(method, "(identifier) @id");
        for (TSNode identifier : identifiers) {
          String idText = testMethodFile.getTextFromNode(identifier);
          if ("calc".equals(idText)) {
            localVariableDeclarationService.isLocalVariableUsage(
                identifier, method, "calc", testMethodFile);
            // This depends on whether it's the declaration or usage
            break;
          }
        }
      }
    }

    @Test
    @DisplayName("Should handle null parameters")
    void shouldHandleNullParameters() {
      TSNode method = findMethodByName("processData");
      assertFalse(
          localVariableDeclarationService.isLocalVariableUsage(
              null, method, "localCalc", testFile));
      assertFalse(
          localVariableDeclarationService.isLocalVariableUsage(
              testFile.query("(identifier) @id").get(0), null, "localCalc", testFile));
      assertFalse(
          localVariableDeclarationService.isLocalVariableUsage(
              testFile.query("(identifier) @id").get(0), method, null, testFile));
      assertFalse(
          localVariableDeclarationService.isLocalVariableUsage(
              testFile.query("(identifier) @id").get(0), method, "localCalc", null));
    }

    private TSNode findMethodByName(String methodName) {
      List<TSNode> methods = testFile.query("(method_declaration) @method");
      for (TSNode method : methods) {
        TSNode nameNode = method.getChildByFieldName("name");
        if (nameNode != null && methodName.equals(testFile.getTextFromNode(nameNode))) {
          return method;
        }
      }
      return null;
    }
  }

  @Nested
  @DisplayName("Integration tests")
  class IntegrationTests {
    @Test
    @DisplayName("Should work together to analyze local variable information")
    void shouldWorkTogetherToAnalyzeLocalVariableInformation() {
      TSNode processDataMethod = findMethodByName("processData");
      // Find all local variables
      List<TSNode> localVars =
          localVariableDeclarationService.findAllLocalVariableDeclarations(
              processDataMethod, testFile);
      assertEquals(4, localVars.size(), "Should find 4 local variables");
      // Check each local variable
      for (TSNode localVar : localVars) {
        Optional<TSNode> nameNode =
            localVariableDeclarationService.getVariableNameNode(localVar, testFile);
        assertTrue(nameNode.isPresent(), "Should find name node for each local variable");
        String varName = testFile.getTextFromNode(nameNode.get());
        assertFalse(varName.isEmpty(), "Variable name should not be empty");
        // Check if it's a declaration
        boolean isDeclaration =
            localVariableDeclarationService.isLocalVariableDeclaration(nameNode.get());
        assertTrue(isDeclaration, "Should identify as local variable declaration");
      }
    }

    @Test
    @DisplayName("Should handle complex local variable scenarios")
    void shouldHandleComplexLocalVariableScenarios() {
      String complexCode =
          """
          public void complexMethod() {
            Calculator calc1 = new Calculator();
            Calculator calc2 = calc1;
            List<Calculator> calculators = new ArrayList<>();
            calculators.add(calc1);
            calculators.add(calc2);
            for (Calculator c : calculators) {
              c.process();
            }
          }
          """;
      TSFile complexFile = new TSFile(SupportedLanguage.JAVA, complexCode);
      List<TSNode> methods = complexFile.query("(method_declaration) @method");
      assertFalse(methods.isEmpty(), "Should have method to test");
      TSNode complexMethod = methods.get(0);
      List<TSNode> localVars =
          localVariableDeclarationService.findAllLocalVariableDeclarations(
              complexMethod, complexFile);
      assertFalse(localVars.isEmpty(), "Should find local variables");
      // Test each local variable
      for (TSNode localVar : localVars) {
        Optional<TSNode> nameNode =
            localVariableDeclarationService.getVariableNameNode(localVar, complexFile);
        assertTrue(nameNode.isPresent(), "Should find variable name");
        String varName = complexFile.getTextFromNode(nameNode.get());
        assertFalse(varName.isEmpty(), "Variable name should not be empty");
      }
    }

    private TSNode findMethodByName(String methodName) {
      List<TSNode> methods = testFile.query("(method_declaration) @method");
      for (TSNode method : methods) {
        TSNode nameNode = method.getChildByFieldName("name");
        if (nameNode != null && methodName.equals(testFile.getTextFromNode(nameNode))) {
          return method;
        }
      }
      return null;
    }
  }
}
