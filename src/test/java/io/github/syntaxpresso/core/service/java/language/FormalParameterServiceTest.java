package io.github.syntaxpresso.core.service.java.language;

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

@DisplayName("FormalParameterService Tests")
class FormalParameterServiceTest {
  private FormalParameterService formalParameterService;
  private TSFile testFile;

  @BeforeEach
  void setUp() {
    VariableNamingService variableNamingService = new VariableNamingService();
    LocalVariableDeclarationService localVariableDeclarationService =
        new LocalVariableDeclarationService(variableNamingService);
    formalParameterService =
        new FormalParameterService(localVariableDeclarationService, variableNamingService);
    String javaCode =
        """
        package io.github.test;
        public class TestClass {
          private Calculator calculator;
          public void processCalculator(Calculator calc, String name) {
            calc.compute();
            name.length();
            this.calculator = calc;
          }
          public String testMethod(Calculator primary, Calculator secondary, int count) {
            Calculator local = new Calculator();
            primary.process();
            secondary.validate();
            local.run();
            count++;
            return primary.getName();
          }
          public void multipleParams(List<Calculator> calculators, Calculator single) {
            calculators.add(single);
            single.start();
          }
          public void noMatchingParams(String text, int number) {
            text.trim();
            number += 1;
          }
        }
        """;
    testFile = new TSFile(SupportedLanguage.JAVA, javaCode);
  }

  @Nested
  @DisplayName("findAllFormalParameters() tests")
  class FindAllFormalParametersTests {
    @Test
    @DisplayName("Should find formal parameters of specific type")
    void shouldFindFormalParametersOfSpecificType() {
      testFile.query("(method_declaration) @method");
      // Find processCalculator method
      TSNode processCalculatorMethod = findMethodByName("processCalculator");
      assertNotNull(processCalculatorMethod, "Should find processCalculator method");
      List<TSNode> calculatorParams =
          formalParameterService.findAllFormalParameters(
              testFile, processCalculatorMethod, "Calculator");
      assertEquals(
          1, calculatorParams.size(), "Should find 1 Calculator parameter in processCalculator");
      List<TSNode> stringParams =
          formalParameterService.findAllFormalParameters(
              testFile, processCalculatorMethod, "String");
      assertEquals(1, stringParams.size(), "Should find 1 String parameter in processCalculator");
    }

    @Test
    @DisplayName("Should find multiple parameters of same type")
    void shouldFindMultipleParametersOfSameType() {
      TSNode testMethod = findMethodByName("testMethod");
      assertNotNull(testMethod, "Should find testMethod");
      List<TSNode> calculatorParams =
          formalParameterService.findAllFormalParameters(testFile, testMethod, "Calculator");
      assertEquals(2, calculatorParams.size(), "Should find 2 Calculator parameters in testMethod");
    }

    @Test
    @DisplayName("Should return empty list for non-existent type")
    void shouldReturnEmptyListForNonExistentType() {
      TSNode processCalculatorMethod = findMethodByName("processCalculator");
      List<TSNode> nonExistentParams =
          formalParameterService.findAllFormalParameters(
              testFile, processCalculatorMethod, "NonExistent");
      assertTrue(nonExistentParams.isEmpty(), "Should return empty list for non-existent type");
    }

    @Test
    @DisplayName("Should handle invalid method node")
    void shouldHandleInvalidMethodNode() {
      List<TSNode> classNodes = testFile.query("(class_declaration) @class").execute();
      List<TSNode> params =
          formalParameterService.findAllFormalParameters(testFile, classNodes.get(0), "Calculator");
      assertTrue(params.isEmpty(), "Should return empty list for non-method node");
    }

    @Test
    @DisplayName("Should handle null parameters")
    void shouldHandleNullParameters() {
      TSNode method = findMethodByName("processCalculator");
      List<TSNode> nullNodeResult =
          formalParameterService.findAllFormalParameters(testFile, null, "Calculator");
      assertTrue(nullNodeResult.isEmpty(), "Should handle null method node");
      List<TSNode> nullFileResult =
          formalParameterService.findAllFormalParameters(null, method, "Calculator");
      assertTrue(nullFileResult.isEmpty(), "Should handle null file gracefully");
    }

    private TSNode findMethodByName(String methodName) {
      List<TSNode> methods = testFile.query("(method_declaration) @method").execute();
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
  @DisplayName("getParameterTypeNode() tests")
  class GetParameterTypeNodeTests {
    @Test
    @DisplayName("Should find type node in formal parameter")
    void shouldFindTypeNodeInFormalParameter() {
      TSNode processCalculatorMethod = findMethodByName("processCalculator");
      List<TSNode> calculatorParams =
          formalParameterService.findAllFormalParameters(
              testFile, processCalculatorMethod, "Calculator");
      assertFalse(calculatorParams.isEmpty(), "Should have Calculator parameters to test");
      TSNode calculatorParam = calculatorParams.get(0);
      Optional<TSNode> typeNode =
          formalParameterService.getParameterTypeNode(calculatorParam, testFile, "Calculator");
      assertTrue(typeNode.isPresent(), "Should find Calculator type node");
      assertEquals("Calculator", testFile.getTextFromNode(typeNode.get()));
    }

    @Test
    @DisplayName("Should return empty for non-matching type")
    void shouldReturnEmptyForNonMatchingType() {
      TSNode processCalculatorMethod = findMethodByName("processCalculator");
      List<TSNode> stringParams =
          formalParameterService.findAllFormalParameters(
              testFile, processCalculatorMethod, "String");
      assertFalse(stringParams.isEmpty(), "Should have String parameters to test");
      TSNode stringParam = stringParams.get(0);
      Optional<TSNode> typeNode =
          formalParameterService.getParameterTypeNode(stringParam, testFile, "Calculator");
      assertFalse(typeNode.isPresent(), "Should not find Calculator type in String parameter");
    }

    @Test
    @DisplayName("Should handle invalid parameter node")
    void shouldHandleInvalidParameterNode() {
      List<TSNode> classNodes = testFile.query("(class_declaration) @class").execute();
      Optional<TSNode> typeNode =
          formalParameterService.getParameterTypeNode(classNodes.get(0), testFile, "Calculator");
      assertFalse(typeNode.isPresent(), "Should return empty for non-parameter node");
    }

    @Test
    @DisplayName("Should handle null parameters")
    void shouldHandleNullParameters() {
      TSNode processCalculatorMethod = findMethodByName("processCalculator");
      List<TSNode> params =
          formalParameterService.findAllFormalParameters(
              testFile, processCalculatorMethod, "Calculator");
      TSNode param = params.get(0);
      assertFalse(
          formalParameterService.getParameterTypeNode(null, testFile, "Calculator").isPresent());
      assertFalse(
          formalParameterService.getParameterTypeNode(param, null, "Calculator").isPresent());
      assertFalse(formalParameterService.getParameterTypeNode(param, testFile, null).isPresent());
    }

    private TSNode findMethodByName(String methodName) {
      List<TSNode> methods = testFile.query("(method_declaration) @method").execute();
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
  @DisplayName("getParameterNameNode() tests")
  class GetParameterNameNodeTests {
    @Test
    @DisplayName("Should extract parameter name from formal parameter")
    void shouldExtractParameterNameFromFormalParameter() {
      TSNode processCalculatorMethod = findMethodByName("processCalculator");
      List<TSNode> calculatorParams =
          formalParameterService.findAllFormalParameters(
              testFile, processCalculatorMethod, "Calculator");
      assertFalse(calculatorParams.isEmpty(), "Should have Calculator parameters to test");
      TSNode calculatorParam = calculatorParams.get(0);
      Optional<TSNode> nameNode =
          formalParameterService.getParameterNameNode(calculatorParam, testFile);
      assertTrue(nameNode.isPresent(), "Should find parameter name node");
      assertEquals("calc", testFile.getTextFromNode(nameNode.get()));
    }

    @Test
    @DisplayName("Should handle different parameter types")
    void shouldHandleDifferentParameterTypes() {
      TSNode processCalculatorMethod = findMethodByName("processCalculator");
      List<TSNode> stringParams =
          formalParameterService.findAllFormalParameters(
              testFile, processCalculatorMethod, "String");
      assertFalse(stringParams.isEmpty(), "Should have String parameters to test");
      TSNode stringParam = stringParams.get(0);
      Optional<TSNode> nameNode =
          formalParameterService.getParameterNameNode(stringParam, testFile);
      assertTrue(nameNode.isPresent(), "Should find String parameter name node");
      assertEquals("name", testFile.getTextFromNode(nameNode.get()));
    }

    @Test
    @DisplayName("Should handle invalid parameter node")
    void shouldHandleInvalidParameterNode() {
      List<TSNode> classNodes = testFile.query("(class_declaration) @class").execute();
      Optional<TSNode> nameNode =
          formalParameterService.getParameterNameNode(classNodes.get(0), testFile);
      assertFalse(nameNode.isPresent(), "Should return empty for non-parameter node");
    }

    @Test
    @DisplayName("Should handle null parameters")
    void shouldHandleNullParameters() {
      TSNode processCalculatorMethod = findMethodByName("processCalculator");
      List<TSNode> params =
          formalParameterService.findAllFormalParameters(
              testFile, processCalculatorMethod, "Calculator");
      TSNode param = params.get(0);
      assertFalse(formalParameterService.getParameterNameNode(null, testFile).isPresent());
      assertFalse(formalParameterService.getParameterNameNode(param, null).isPresent());
    }

    private TSNode findMethodByName(String methodName) {
      List<TSNode> methods = testFile.query("(method_declaration) @method").execute();
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
  @DisplayName("isFormalParameterDeclaration() tests")
  class IsFormalParameterDeclarationTests {
    @Test
    @DisplayName("Should identify formal parameter declarations")
    void shouldIdentifyFormalParameterDeclarations() {
      TSNode processCalculatorMethod = findMethodByName("processCalculator");
      List<TSNode> calculatorParams =
          formalParameterService.findAllFormalParameters(
              testFile, processCalculatorMethod, "Calculator");
      TSNode calculatorParam = calculatorParams.get(0);
      Optional<TSNode> nameNode =
          formalParameterService.getParameterNameNode(calculatorParam, testFile);
      assertTrue(nameNode.isPresent(), "Should have name node to test");
      boolean isDeclaration = formalParameterService.isFormalParameterDeclaration(nameNode.get());
      assertTrue(isDeclaration, "Should identify formal parameter declaration");
    }

    @Test
    @DisplayName("Should not identify non-parameter identifiers as declarations")
    void shouldNotIdentifyNonParameterIdentifiersAsDeclarations() {
      // Find method invocation identifiers
      List<TSNode> identifiers =
          testFile.query("(method_invocation name: (identifier) @id)").execute();
      assertFalse(identifiers.isEmpty(), "Should have method invocation identifiers to test");
      TSNode invocationId = identifiers.get(0);
      boolean isDeclaration = formalParameterService.isFormalParameterDeclaration(invocationId);
      assertFalse(isDeclaration, "Should not identify method invocation as parameter declaration");
    }

    @Test
    @DisplayName("Should handle null and invalid nodes")
    void shouldHandleNullAndInvalidNodes() {
      assertFalse(
          formalParameterService.isFormalParameterDeclaration(null), "Should handle null node");
      // Create a mock null node scenario
      List<TSNode> classNodes = testFile.query("(class_declaration) @class").execute();
      TSNode classNode = classNodes.get(0);
      assertFalse(
          formalParameterService.isFormalParameterDeclaration(classNode),
          "Should handle non-identifier node");
    }

    private TSNode findMethodByName(String methodName) {
      List<TSNode> methods = testFile.query("(method_declaration) @method").execute();
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
  @DisplayName("findAllFormalParameterUsages() tests")
  class FindAllFormalParameterUsagesTests {
    @Test
    @DisplayName("Should find formal parameter usages in method body")
    void shouldFindFormalParameterUsagesInMethodBody() {
      TSNode processCalculatorMethod = findMethodByName("processCalculator");
      List<TSNode> usages =
          formalParameterService.findAllFormalParameterUsages(
              testFile, processCalculatorMethod, "Calculator");
      assertFalse(usages.isEmpty(), "Should find Calculator parameter usages");
      // Check if we found the expected usages (calc.compute(), this.calculator = calc)
      boolean foundCompute = false;
      boolean foundAssignment = false;
      for (TSNode usage : usages) {
        String usageText = testFile.getTextFromNode(usage);
        if ("calc".equals(usageText)) {
          // Check context to determine which usage this is
          TSNode parent = usage.getParent();
          if (parent != null) {
            String parentType = parent.getType();
            if ("method_invocation".equals(parentType)) {
              foundCompute = true;
            } else if ("assignment_expression".equals(parentType)) {
              foundAssignment = true;
            }
          }
        }
      }
      assertTrue(
          foundCompute || foundAssignment, "Should find at least one of the expected usages");
    }

    @Test
    @DisplayName("Should find multiple parameter usages")
    void shouldFindMultipleParameterUsages() {
      TSNode testMethod = findMethodByName("testMethod");
      List<TSNode> usages =
          formalParameterService.findAllFormalParameterUsages(testFile, testMethod, "Calculator");
      assertFalse(usages.isEmpty(), "Should find Calculator parameter usages in testMethod");
      // testMethod has primary.process(), secondary.validate(), return primary.getName()
      // So we should find multiple usages
      long primaryUsages =
          usages.stream()
              .map(node -> testFile.getTextFromNode(node))
              .filter("primary"::equals)
              .count();
      long secondaryUsages =
          usages.stream()
              .map(node -> testFile.getTextFromNode(node))
              .filter("secondary"::equals)
              .count();
      assertTrue(
          primaryUsages > 0 || secondaryUsages > 0,
          "Should find usages of primary or secondary parameters");
    }

    @Test
    @DisplayName("Should return empty list for non-existent parameter type")
    void shouldReturnEmptyListForNonExistentParameterType() {
      TSNode noMatchingMethod = findMethodByName("noMatchingParams");
      List<TSNode> usages =
          formalParameterService.findAllFormalParameterUsages(
              testFile, noMatchingMethod, "Calculator");
      assertTrue(
          usages.isEmpty(), "Should return empty list when no parameters of specified type exist");
    }

    @Test
    @DisplayName("Should handle invalid method node")
    void shouldHandleInvalidMethodNode() {
      List<TSNode> classNodes = testFile.query("(class_declaration) @class").execute();
      List<TSNode> usages =
          formalParameterService.findAllFormalParameterUsages(
              testFile, classNodes.get(0), "Calculator");
      assertTrue(usages.isEmpty(), "Should return empty list for non-method node");
    }

    @Test
    @DisplayName("Should handle null parameters")
    void shouldHandleNullParameters() {
      TSNode method = findMethodByName("processCalculator");
      List<TSNode> nullMethodResult =
          formalParameterService.findAllFormalParameterUsages(testFile, null, "Calculator");
      assertTrue(nullMethodResult.isEmpty(), "Should handle null method node");
      List<TSNode> nullFileResult =
          formalParameterService.findAllFormalParameterUsages(null, method, "Calculator");
      assertTrue(nullFileResult.isEmpty(), "Should handle null file gracefully");
    }

    private TSNode findMethodByName(String methodName) {
      List<TSNode> methods = testFile.query("(method_declaration) @method").execute();
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
    @DisplayName("Should work together to analyze parameter information")
    void shouldWorkTogetherToAnalyzeParameterInformation() {
      TSNode testMethod = findMethodByName("testMethod");
      // Find all Calculator parameters
      List<TSNode> calculatorParams =
          formalParameterService.findAllFormalParameters(testFile, testMethod, "Calculator");
      assertEquals(2, calculatorParams.size(), "Should find 2 Calculator parameters");
      // Check each parameter
      for (TSNode param : calculatorParams) {
        Optional<TSNode> typeNode =
            formalParameterService.getParameterTypeNode(param, testFile, "Calculator");
        Optional<TSNode> nameNode = formalParameterService.getParameterNameNode(param, testFile);
        assertTrue(typeNode.isPresent(), "Should find type node");
        assertTrue(nameNode.isPresent(), "Should find name node");
        assertEquals("Calculator", testFile.getTextFromNode(typeNode.get()));
        String paramName = testFile.getTextFromNode(nameNode.get());
        assertTrue(
            List.of("primary", "secondary").contains(paramName),
            "Parameter name should be 'primary' or 'secondary'");
      }
      // Find usages
      List<TSNode> usages =
          formalParameterService.findAllFormalParameterUsages(testFile, testMethod, "Calculator");
      assertFalse(usages.isEmpty(), "Should find parameter usages");
    }

    @Test
    @DisplayName("Should handle complex parameter scenarios")
    void shouldHandleComplexParameterScenarios() {
      String complexCode =
          """
          public void complexMethod(Calculator calc, List<Calculator> calcs, Calculator... varArgs) {
            calc.process();
            calcs.forEach(Calculator::start);
            for (Calculator c : varArgs) {
              c.validate();
            }
          }
          """;
      TSFile complexFile = new TSFile(SupportedLanguage.JAVA, complexCode);
      List<TSNode> methods = complexFile.query("(method_declaration) @method").execute();
      assertFalse(methods.isEmpty(), "Should have method to test");
      TSNode complexMethod = methods.get(0);
      List<TSNode> calculatorParams =
          formalParameterService.findAllFormalParameters(complexFile, complexMethod, "Calculator");
      // Should find at least the direct Calculator parameter
      assertFalse(calculatorParams.isEmpty(), "Should find Calculator parameters");
      // Test parameter name extraction
      for (TSNode param : calculatorParams) {
        Optional<TSNode> nameNode = formalParameterService.getParameterNameNode(param, complexFile);
        assertTrue(nameNode.isPresent(), "Should find parameter name");
        String paramName = complexFile.getTextFromNode(nameNode.get());
        assertFalse(paramName.isEmpty(), "Parameter name should not be empty");
      }
    }

    private TSNode findMethodByName(String methodName) {
      List<TSNode> methods = testFile.query("(method_declaration) @method").execute();
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
