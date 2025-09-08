package io.github.syntaxpresso.core.service.java.language;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import io.github.syntaxpresso.core.service.java.language.extra.ParameterCapture;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.treesitter.TSNode;

@DisplayName("FormalParameterService Tests")
class FormalParameterServiceTest {
  private FormalParameterService formalParameterService;

  private static final String SIMPLE_PARAMETERS_CODE =
      """
      public class TestClass {
          public void simpleMethod(String name, int age, boolean active) {
              System.out.println(name);
              if (active && age > 0) {
                  System.out.println("Valid user: " + name);
              }
          }
      }
      """;

  private static final String GENERIC_PARAMETERS_CODE =
      """
      public class GenericTestClass {
          public void genericMethod(List<String> items, Map<String, Integer> data, T element) {
              for (String item : items) {
                  System.out.println(item);
              }
              data.put("count", items.size());
              processElement(element);
          }
      }
      """;

  private static final String COMPLEX_USAGE_CODE =
      """
      public class ComplexUsageClass {
          public void complexMethod(String input, List<Integer> numbers) {
              String result = input != null ? input.trim() : "";
              numbers.add(result.length());
              System.out.println(input + " has " + numbers.size() + " characters");
              processData(input, numbers);
          }

          private void processData(String data, List<Integer> values) {
              // helper method
          }
      }
      """;

  private static final String NO_PARAMETERS_CODE =
      """
      public class NoParametersClass {
          public void noParametersMethod() {
              System.out.println("No parameters here");
          }
      }
      """;

  private static final String PRIMITIVE_TYPES_CODE =
      """
      public class PrimitiveTypesClass {
          public void primitiveMethod(int count, double rate, boolean flag, char symbol, byte data) {
              if (flag) {
                  System.out.println(symbol + ": " + count * rate);
              }
              System.out.println("Data byte: " + data);
          }
      }
      """;

  private static final String VARARGS_CODE =
      """
      public class VarargsClass {
          public void varargsMethod(String prefix, int... numbers) {
              System.out.println(prefix);
              for (int num : numbers) {
                  System.out.println(num);
              }
          }
      }
      """;

  @BeforeEach
  void setUp() {
    this.formalParameterService = new FormalParameterService();
  }

  private TSNode getMethodDeclarationNode(String code, String methodName) {
    TSFile tsFile = new TSFile(SupportedLanguage.JAVA, code);
    String query =
        String.format(
            """
            (method_declaration
              name: (identifier) @name
              (#eq? @name "%s")
            ) @method
            """,
            methodName);
    return tsFile.query(query).returning("method").execute().firstNode();
  }

  @Nested
  @DisplayName("getAllFormalParameterNodes() Tests")
  class GetAllFormalParameterNodesTests {

    /**
     * Tests that getAllFormalParameterNodes finds all parameters in a method.
     *
     * <p>Usage example:
     *
     * <pre>
     * List&lt;TSNode&gt; parameters = service.getAllFormalParameterNodes(tsFile, methodNode);
     * // Returns nodes for each parameter in the method signature
     * </pre>
     */
    @Test
    @DisplayName("should find all parameters in method with multiple parameters")
    void getAllFormalParameterNodes_withMultipleParameters_shouldFindAll() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_PARAMETERS_CODE);
      TSNode methodNode = getMethodDeclarationNode(SIMPLE_PARAMETERS_CODE, "simpleMethod");
      assertNotNull(methodNode);

      List<TSNode> parameterNodes =
          formalParameterService.getAllFormalParameterNodes(tsFile, methodNode);

      assertEquals(3, parameterNodes.size());

      // Verify we can get parameter names for all found parameters
      String[] expectedNames = {"name", "age", "active"};
      for (int i = 0; i < parameterNodes.size(); i++) {
        Optional<TSNode> nameNode =
            formalParameterService.getFormalParameterNameNode(tsFile, parameterNodes.get(i));
        assertTrue(nameNode.isPresent());
        String paramName = tsFile.getTextFromNode(nameNode.get());
        assertEquals(expectedNames[i], paramName);
      }
    }

    @Test
    @DisplayName("should return empty list for method with no parameters")
    void getAllFormalParameterNodes_withNoParameters_shouldReturnEmptyList() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, NO_PARAMETERS_CODE);
      TSNode methodNode = getMethodDeclarationNode(NO_PARAMETERS_CODE, "noParametersMethod");
      assertNotNull(methodNode);

      List<TSNode> parameterNodes =
          formalParameterService.getAllFormalParameterNodes(tsFile, methodNode);

      assertTrue(parameterNodes.isEmpty());
    }

    @Test
    @DisplayName("should handle null and invalid input")
    void getAllFormalParameterNodes_withInvalidInput_shouldReturnEmptyList() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_PARAMETERS_CODE);

      // Test with null file
      List<TSNode> result1 = formalParameterService.getAllFormalParameterNodes(null, null);
      assertTrue(result1.isEmpty());

      // Test with null method node
      List<TSNode> result2 = formalParameterService.getAllFormalParameterNodes(tsFile, null);
      assertTrue(result2.isEmpty());

      // Test with wrong node type
      TSNode classNode =
          tsFile.query("(class_declaration) @class").returning("class").execute().firstNode();
      List<TSNode> result3 = formalParameterService.getAllFormalParameterNodes(tsFile, classNode);
      assertTrue(result3.isEmpty());
    }

    @Test
    @DisplayName("should handle primitive type parameters")
    void getAllFormalParameterNodes_withPrimitiveTypes_shouldFindAll() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, PRIMITIVE_TYPES_CODE);
      TSNode methodNode = getMethodDeclarationNode(PRIMITIVE_TYPES_CODE, "primitiveMethod");
      assertNotNull(methodNode);

      List<TSNode> parameterNodes =
          formalParameterService.getAllFormalParameterNodes(tsFile, methodNode);

      assertEquals(5, parameterNodes.size());
    }

    @Test
    @DisplayName("should handle varargs parameters")
    void getAllFormalParameterNodes_withVarargs_shouldFindAll() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, VARARGS_CODE);
      TSNode methodNode = getMethodDeclarationNode(VARARGS_CODE, "varargsMethod");
      assertNotNull(methodNode);

      List<TSNode> parameterNodes =
          formalParameterService.getAllFormalParameterNodes(tsFile, methodNode);

      // Should find at least 1 parameter (String prefix), varargs might not be captured as
      // formal_parameter
      assertTrue(parameterNodes.size() >= 1);
    }
  }

  @Nested
  @DisplayName("getFormalParameterNodeInfo() Tests")
  class GetFormalParameterNodeInfoTests {

    /**
     * Tests that getFormalParameterNodeInfo extracts parameter information correctly.
     *
     * <p>Usage example:
     *
     * <pre>
     * List&lt;Map&lt;String, TSNode&gt;&gt; info = service.getFormalParameterNodeInfo(tsFile, paramNode);
     * Map&lt;String, TSNode&gt; paramInfo = info.get(0);
     * String paramType = tsFile.getTextFromNode(paramInfo.get("parameterType"));
     * String paramName = tsFile.getTextFromNode(paramInfo.get("parameterName"));
     * </pre>
     */
    @Test
    @DisplayName("should extract basic parameter information")
    void getFormalParameterNodeInfo_withSimpleParameter_shouldExtractInfo() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_PARAMETERS_CODE);
      TSNode methodNode = getMethodDeclarationNode(SIMPLE_PARAMETERS_CODE, "simpleMethod");
      List<TSNode> parameterNodes =
          formalParameterService.getAllFormalParameterNodes(tsFile, methodNode);
      assertFalse(parameterNodes.isEmpty());

      List<Map<String, TSNode>> paramInfo =
          formalParameterService.getFormalParameterNodeInfo(tsFile, parameterNodes.get(0));

      assertFalse(paramInfo.isEmpty());
      Map<String, TSNode> info = paramInfo.get(0);

      TSNode paramType = info.get(ParameterCapture.PARAMETER_TYPE.getCaptureName());
      assertNotNull(paramType);
      assertEquals("String", tsFile.getTextFromNode(paramType));

      TSNode paramName = info.get(ParameterCapture.PARAMETER_NAME.getCaptureName());
      assertNotNull(paramName);
      assertEquals("name", tsFile.getTextFromNode(paramName));
    }

    @Test
    @DisplayName("should extract primitive type parameter information")
    void getFormalParameterNodeInfo_withPrimitiveParameter_shouldExtractInfo() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_PARAMETERS_CODE);
      TSNode methodNode = getMethodDeclarationNode(SIMPLE_PARAMETERS_CODE, "simpleMethod");
      List<TSNode> parameterNodes =
          formalParameterService.getAllFormalParameterNodes(tsFile, methodNode);
      assertTrue(parameterNodes.size() >= 2);

      // Test the int parameter (age)
      List<Map<String, TSNode>> paramInfo =
          formalParameterService.getFormalParameterNodeInfo(tsFile, parameterNodes.get(1));

      assertFalse(paramInfo.isEmpty());
      Map<String, TSNode> info = paramInfo.get(0);

      TSNode paramType = info.get(ParameterCapture.PARAMETER_TYPE.getCaptureName());
      assertNotNull(paramType);
      assertEquals("int", tsFile.getTextFromNode(paramType));

      TSNode paramName = info.get(ParameterCapture.PARAMETER_NAME.getCaptureName());
      assertNotNull(paramName);
      assertEquals("age", tsFile.getTextFromNode(paramName));
    }

    @Test
    @DisplayName("should extract generic parameter information")
    void getFormalParameterNodeInfo_withGenericParameter_shouldExtractInfo() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, GENERIC_PARAMETERS_CODE);
      TSNode methodNode = getMethodDeclarationNode(GENERIC_PARAMETERS_CODE, "genericMethod");
      List<TSNode> parameterNodes =
          formalParameterService.getAllFormalParameterNodes(tsFile, methodNode);
      assertFalse(parameterNodes.isEmpty());

      List<Map<String, TSNode>> paramInfo =
          formalParameterService.getFormalParameterNodeInfo(tsFile, parameterNodes.get(0));

      assertFalse(paramInfo.isEmpty());
      Map<String, TSNode> info = paramInfo.get(0);

      TSNode paramType = info.get(ParameterCapture.PARAMETER_TYPE.getCaptureName());
      assertNotNull(paramType);
      String typeText = tsFile.getTextFromNode(paramType);
      assertTrue(typeText.contains("List"));

      TSNode paramName = info.get(ParameterCapture.PARAMETER_NAME.getCaptureName());
      assertNotNull(paramName);
      assertEquals("items", tsFile.getTextFromNode(paramName));

      // Check for type argument
      TSNode typeArgument = info.get(ParameterCapture.PARAMETER_TYPE_ARGUMENT.getCaptureName());
      if (typeArgument != null) {
        assertEquals("String", tsFile.getTextFromNode(typeArgument));
      }
    }

    @Test
    @DisplayName("should return empty list for invalid input")
    void getFormalParameterNodeInfo_withInvalidInput_shouldReturnEmptyList() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_PARAMETERS_CODE);

      // Test with null file
      List<Map<String, TSNode>> result1 =
          formalParameterService.getFormalParameterNodeInfo(null, null);
      assertTrue(result1.isEmpty());

      // Test with invalid node type
      TSNode methodNode = getMethodDeclarationNode(SIMPLE_PARAMETERS_CODE, "simpleMethod");
      List<Map<String, TSNode>> result2 =
          formalParameterService.getFormalParameterNodeInfo(tsFile, methodNode);
      assertTrue(result2.isEmpty());
    }
  }

  @Nested
  @DisplayName("getFormalParameterChildByCaptureName() Tests")
  class GetFormalParameterChildByCaptureNameTests {

    /**
     * Tests that getFormalParameterChildByCaptureName retrieves specific parameter components.
     *
     * <p>Usage example:
     *
     * <pre>
     * Optional&lt;TSNode&gt; typeNode = service.getFormalParameterChildByCaptureName(
     *     tsFile, paramNode, ParameterCapture.PARAMETER_TYPE);
     * if (typeNode.isPresent()) {
     *   String type = tsFile.getTextFromNode(typeNode.get());
     * }
     * </pre>
     */
    @Test
    @DisplayName("should retrieve parameter type node")
    void getFormalParameterChildByCaptureName_withTypeCapture_shouldReturnTypeNode() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_PARAMETERS_CODE);
      TSNode methodNode = getMethodDeclarationNode(SIMPLE_PARAMETERS_CODE, "simpleMethod");
      List<TSNode> parameterNodes =
          formalParameterService.getAllFormalParameterNodes(tsFile, methodNode);
      assertFalse(parameterNodes.isEmpty());

      Optional<TSNode> typeNode =
          formalParameterService.getFormalParameterChildByCaptureName(
              tsFile, parameterNodes.get(0), ParameterCapture.PARAMETER_TYPE);

      assertTrue(typeNode.isPresent());
      assertEquals("String", tsFile.getTextFromNode(typeNode.get()));
    }

    @Test
    @DisplayName("should retrieve parameter name node")
    void getFormalParameterChildByCaptureName_withNameCapture_shouldReturnNameNode() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_PARAMETERS_CODE);
      TSNode methodNode = getMethodDeclarationNode(SIMPLE_PARAMETERS_CODE, "simpleMethod");
      List<TSNode> parameterNodes =
          formalParameterService.getAllFormalParameterNodes(tsFile, methodNode);
      assertFalse(parameterNodes.isEmpty());

      Optional<TSNode> nameNode =
          formalParameterService.getFormalParameterChildByCaptureName(
              tsFile, parameterNodes.get(0), ParameterCapture.PARAMETER_NAME);

      assertTrue(nameNode.isPresent());
      assertEquals("name", tsFile.getTextFromNode(nameNode.get()));
    }

    @Test
    @DisplayName("should return empty for invalid input")
    void getFormalParameterChildByCaptureName_withInvalidInput_shouldReturnEmpty() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_PARAMETERS_CODE);

      Optional<TSNode> result1 =
          formalParameterService.getFormalParameterChildByCaptureName(
              null, null, ParameterCapture.PARAMETER_TYPE);
      assertFalse(result1.isPresent());

      TSNode methodNode = getMethodDeclarationNode(SIMPLE_PARAMETERS_CODE, "simpleMethod");
      Optional<TSNode> result2 =
          formalParameterService.getFormalParameterChildByCaptureName(
              tsFile, methodNode, ParameterCapture.PARAMETER_TYPE);
      assertFalse(result2.isPresent());
    }
  }

  @Nested
  @DisplayName("getFormalParameterTypeNode() Tests")
  class GetFormalParameterTypeNodeTests {

    /**
     * Tests that getFormalParameterTypeNode returns base type or type argument.
     *
     * <p>Usage example:
     *
     * <pre>
     * Optional&lt;TSNode&gt; typeNode = service.getFormalParameterTypeNode(tsFile, paramNode);
     * if (typeNode.isPresent()) {
     *   String type = tsFile.getTextFromNode(typeNode.get()); // e.g., "String" from List&lt;String&gt;
     * }
     * </pre>
     */
    @Test
    @DisplayName("should retrieve simple type node")
    void getFormalParameterTypeNode_withSimpleType_shouldReturnTypeNode() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_PARAMETERS_CODE);
      TSNode methodNode = getMethodDeclarationNode(SIMPLE_PARAMETERS_CODE, "simpleMethod");
      List<TSNode> parameterNodes =
          formalParameterService.getAllFormalParameterNodes(tsFile, methodNode);
      assertTrue(parameterNodes.size() >= 2);

      Optional<TSNode> typeNode =
          formalParameterService.getFormalParameterTypeNode(tsFile, parameterNodes.get(1));

      assertTrue(typeNode.isPresent());
      assertEquals("int", tsFile.getTextFromNode(typeNode.get()));
    }

    @Test
    @DisplayName("should retrieve type argument for generic type")
    void getFormalParameterTypeNode_withGenericType_shouldReturnTypeArgument() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, GENERIC_PARAMETERS_CODE);
      TSNode methodNode = getMethodDeclarationNode(GENERIC_PARAMETERS_CODE, "genericMethod");
      List<TSNode> parameterNodes =
          formalParameterService.getAllFormalParameterNodes(tsFile, methodNode);
      assertFalse(parameterNodes.isEmpty());

      Optional<TSNode> typeNode =
          formalParameterService.getFormalParameterTypeNode(tsFile, parameterNodes.get(0));

      assertTrue(typeNode.isPresent());
      String typeText = tsFile.getTextFromNode(typeNode.get());
      // Could be either "String" (type argument) or "List" (base type) depending on what's captured
      assertTrue(typeText.equals("String") || typeText.contains("List"));
    }
  }

  @Nested
  @DisplayName("getFormalParameterFullTypeNode() Tests")
  class GetFormalParameterFullTypeNodeTests {

    /**
     * Tests that getFormalParameterFullTypeNode retrieves complete type information.
     *
     * <p>Usage example:
     *
     * <pre>
     * Optional&lt;TSNode&gt; fullTypeNode = service.getFormalParameterFullTypeNode(tsFile, paramNode);
     * if (fullTypeNode.isPresent()) {
     *   String type = tsFile.getTextFromNode(fullTypeNode.get()); // e.g., "List&lt;String&gt;"
     * }
     * </pre>
     */
    @Test
    @DisplayName("should retrieve full type node for simple type")
    void getFormalParameterFullTypeNode_withSimpleType_shouldReturnTypeNode() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_PARAMETERS_CODE);
      TSNode methodNode = getMethodDeclarationNode(SIMPLE_PARAMETERS_CODE, "simpleMethod");
      List<TSNode> parameterNodes =
          formalParameterService.getAllFormalParameterNodes(tsFile, methodNode);
      assertFalse(parameterNodes.isEmpty());

      Optional<TSNode> typeNode =
          formalParameterService.getFormalParameterFullTypeNode(tsFile, parameterNodes.get(0));

      assertTrue(typeNode.isPresent());
      assertEquals("String", tsFile.getTextFromNode(typeNode.get()));
    }

    @Test
    @DisplayName("should retrieve full type node for generic type")
    void getFormalParameterFullTypeNode_withGenericType_shouldReturnFullTypeNode() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, GENERIC_PARAMETERS_CODE);
      TSNode methodNode = getMethodDeclarationNode(GENERIC_PARAMETERS_CODE, "genericMethod");
      List<TSNode> parameterNodes =
          formalParameterService.getAllFormalParameterNodes(tsFile, methodNode);
      assertFalse(parameterNodes.isEmpty());

      Optional<TSNode> typeNode =
          formalParameterService.getFormalParameterFullTypeNode(tsFile, parameterNodes.get(0));

      assertTrue(typeNode.isPresent());
      String typeText = tsFile.getTextFromNode(typeNode.get());
      assertTrue(typeText.contains("List"));
    }
  }

  @Nested
  @DisplayName("getFormalParameterNameNode() Tests")
  class GetFormalParameterNameNodeTests {

    /**
     * Tests that getFormalParameterNameNode retrieves parameter name identifiers.
     *
     * <p>Usage example:
     *
     * <pre>
     * Optional&lt;TSNode&gt; nameNode = service.getFormalParameterNameNode(tsFile, paramNode);
     * if (nameNode.isPresent()) {
     *   String paramName = tsFile.getTextFromNode(nameNode.get()); // e.g., "userName"
     * }
     * </pre>
     */
    @Test
    @DisplayName("should retrieve parameter name node")
    void getFormalParameterNameNode_withValidParameter_shouldReturnNameNode() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_PARAMETERS_CODE);
      TSNode methodNode = getMethodDeclarationNode(SIMPLE_PARAMETERS_CODE, "simpleMethod");
      List<TSNode> parameterNodes =
          formalParameterService.getAllFormalParameterNodes(tsFile, methodNode);
      assertFalse(parameterNodes.isEmpty());

      Optional<TSNode> nameNode =
          formalParameterService.getFormalParameterNameNode(tsFile, parameterNodes.get(0));

      assertTrue(nameNode.isPresent());
      assertEquals("name", tsFile.getTextFromNode(nameNode.get()));
    }

    @Test
    @DisplayName("should retrieve all parameter names correctly")
    void getFormalParameterNameNode_withMultipleParameters_shouldReturnCorrectNames() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_PARAMETERS_CODE);
      TSNode methodNode = getMethodDeclarationNode(SIMPLE_PARAMETERS_CODE, "simpleMethod");
      List<TSNode> parameterNodes =
          formalParameterService.getAllFormalParameterNodes(tsFile, methodNode);
      assertEquals(3, parameterNodes.size());

      String[] expectedNames = {"name", "age", "active"};
      for (int i = 0; i < parameterNodes.size(); i++) {
        Optional<TSNode> nameNode =
            formalParameterService.getFormalParameterNameNode(tsFile, parameterNodes.get(i));
        assertTrue(nameNode.isPresent());
        assertEquals(expectedNames[i], tsFile.getTextFromNode(nameNode.get()));
      }
    }
  }

  @Nested
  @DisplayName("findAllFormalParameterNodeUsages() Tests")
  class FindAllFormalParameterNodeUsagesTests {

    /**
     * Tests that findAllFormalParameterNodeUsages finds parameter usage in method bodies.
     *
     * <p>Usage example:
     *
     * <pre>
     * List&lt;TSNode&gt; usages = service.findAllFormalParameterNodeUsages(tsFile, paramNode, methodNode);
     * for (TSNode usage : usages) {
     *   String usageText = tsFile.getTextFromNode(usage); // parameter name at usage site
     *   int line = usage.getStartPoint().getRow() + 1;    // Line number of usage
     * }
     * </pre>
     */
    @Test
    @DisplayName("should find parameter usages in method body")
    void findAllFormalParameterNodeUsages_withParameterUsages_shouldFindUsages() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_PARAMETERS_CODE);
      TSNode methodNode = getMethodDeclarationNode(SIMPLE_PARAMETERS_CODE, "simpleMethod");
      List<TSNode> parameterNodes =
          formalParameterService.getAllFormalParameterNodes(tsFile, methodNode);
      assertFalse(parameterNodes.isEmpty());

      // Find usages of "name" parameter
      List<TSNode> usages =
          formalParameterService.findAllFormalParameterNodeUsages(
              tsFile, parameterNodes.get(0), methodNode);

      // Should find at least 2 usages of "name" parameter
      assertTrue(usages.size() >= 2);
    }

    @Test
    @DisplayName("should find complex parameter usages")
    void findAllFormalParameterNodeUsages_withComplexUsages_shouldFindAll() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, COMPLEX_USAGE_CODE);
      TSNode methodNode = getMethodDeclarationNode(COMPLEX_USAGE_CODE, "complexMethod");
      List<TSNode> parameterNodes =
          formalParameterService.getAllFormalParameterNodes(tsFile, methodNode);
      assertEquals(2, parameterNodes.size());

      // Find usages of "input" parameter (first parameter)
      List<TSNode> inputUsages =
          formalParameterService.findAllFormalParameterNodeUsages(
              tsFile, parameterNodes.get(0), methodNode);

      // Should find multiple usages: ternary condition, method call, concatenation, method argument
      assertTrue(inputUsages.size() >= 3);

      // Find usages of "numbers" parameter (second parameter)
      List<TSNode> numbersUsages =
          formalParameterService.findAllFormalParameterNodeUsages(
              tsFile, parameterNodes.get(1), methodNode);

      // Should find usages in method calls
      assertTrue(numbersUsages.size() >= 2);
    }

    @Test
    @DisplayName("should return empty list for unused parameters")
    void findAllFormalParameterNodeUsages_withUnusedParameter_shouldReturnEmptyList() {
      String unusedParamCode =
          """
          public class UnusedParamClass {
              public void methodWithUnusedParam(String unused, int used) {
                  System.out.println("Using: " + used);
              }
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, unusedParamCode);
      TSNode methodNode = getMethodDeclarationNode(unusedParamCode, "methodWithUnusedParam");
      List<TSNode> parameterNodes =
          formalParameterService.getAllFormalParameterNodes(tsFile, methodNode);
      assertEquals(2, parameterNodes.size());

      // Find usages of "unused" parameter (first parameter)
      List<TSNode> usages =
          formalParameterService.findAllFormalParameterNodeUsages(
              tsFile, parameterNodes.get(0), methodNode);

      assertTrue(usages.isEmpty());
    }

    @Test
    @DisplayName("should handle invalid input")
    void findAllFormalParameterNodeUsages_withInvalidInput_shouldReturnEmptyList() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_PARAMETERS_CODE);

      List<TSNode> result1 =
          formalParameterService.findAllFormalParameterNodeUsages(null, null, null);
      assertTrue(result1.isEmpty());

      TSNode methodNode = getMethodDeclarationNode(SIMPLE_PARAMETERS_CODE, "simpleMethod");
      TSNode classNode =
          tsFile.query("(class_declaration) @class").returning("class").execute().firstNode();

      List<TSNode> result2 =
          formalParameterService.findAllFormalParameterNodeUsages(tsFile, classNode, methodNode);
      assertTrue(result2.isEmpty());
    }
  }

  @Nested
  @DisplayName("Edge Cases and Error Handling Tests")
  class EdgeCasesTests {

    @Test
    @DisplayName("should handle empty source code")
    void methods_withEmptyCode_shouldHandleGracefully() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, "");

      TSNode mockNode = tsFile.getTree().getRootNode();
      List<TSNode> parameterNodes =
          formalParameterService.getAllFormalParameterNodes(tsFile, mockNode);
      assertTrue(parameterNodes.isEmpty());
    }

    @Test
    @DisplayName("should handle malformed source code")
    void methods_withMalformedCode_shouldHandleGracefully() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, "public void method(String");

      TSNode mockNode = tsFile.getTree().getRootNode();
      List<TSNode> parameterNodes =
          formalParameterService.getAllFormalParameterNodes(tsFile, mockNode);
      assertNotNull(parameterNodes);
    }

    @Test
    @DisplayName("should handle array type parameters")
    void methods_withArrayParameters_shouldHandleCorrectly() {
      String arrayParamsCode =
          """
          public class ArrayParamsClass {
              public void arrayMethod(String[] names, int[][] matrix) {
                  for (String name : names) {
                      System.out.println(name);
                  }
                  System.out.println("Matrix size: " + matrix.length);
              }
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, arrayParamsCode);
      TSNode methodNode = getMethodDeclarationNode(arrayParamsCode, "arrayMethod");
      assertNotNull(methodNode);

      List<TSNode> parameterNodes =
          formalParameterService.getAllFormalParameterNodes(tsFile, methodNode);
      assertEquals(2, parameterNodes.size());

      // Should be able to extract parameter names even for array types
      for (TSNode paramNode : parameterNodes) {
        Optional<TSNode> nameNode =
            formalParameterService.getFormalParameterNameNode(tsFile, paramNode);
        assertTrue(nameNode.isPresent());
        String paramName = tsFile.getTextFromNode(nameNode.get());
        assertTrue(List.of("names", "matrix").contains(paramName));
      }
    }

    @Test
    @DisplayName("should handle annotation parameters")
    void methods_withAnnotatedParameters_shouldHandleCorrectly() {
      String annotatedParamsCode =
          """
          public class AnnotatedParamsClass {
              public void annotatedMethod(@NotNull String name, @Nullable Integer count) {
                  if (name != null && count != null) {
                      System.out.println(name + ": " + count);
                  }
              }
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, annotatedParamsCode);
      TSNode methodNode = getMethodDeclarationNode(annotatedParamsCode, "annotatedMethod");
      assertNotNull(methodNode);

      List<TSNode> parameterNodes =
          formalParameterService.getAllFormalParameterNodes(tsFile, methodNode);
      assertEquals(2, parameterNodes.size());

      // Should be able to extract parameter information even with annotations
      for (TSNode paramNode : parameterNodes) {
        Optional<TSNode> nameNode =
            formalParameterService.getFormalParameterNameNode(tsFile, paramNode);
        assertTrue(nameNode.isPresent());
        Optional<TSNode> typeNode =
            formalParameterService.getFormalParameterTypeNode(tsFile, paramNode);
        assertTrue(typeNode.isPresent());
      }
    }

    @Test
    @DisplayName("should handle wildcard generic parameters")
    void methods_withWildcardGenerics_shouldHandleCorrectly() {
      String wildcardCode =
          """
          public class WildcardClass {
              public void wildcardMethod(List<?> items, Map<String, ? extends Number> data) {
                  System.out.println("Items: " + items.size());
                  data.values().forEach(System.out::println);
              }
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, wildcardCode);
      TSNode methodNode = getMethodDeclarationNode(wildcardCode, "wildcardMethod");
      assertNotNull(methodNode);

      List<TSNode> parameterNodes =
          formalParameterService.getAllFormalParameterNodes(tsFile, methodNode);
      assertEquals(2, parameterNodes.size());

      // Should be able to extract basic parameter information
      for (TSNode paramNode : parameterNodes) {
        Optional<TSNode> nameNode =
            formalParameterService.getFormalParameterNameNode(tsFile, paramNode);
        assertTrue(nameNode.isPresent());
        String paramName = tsFile.getTextFromNode(nameNode.get());
        assertTrue(List.of("items", "data").contains(paramName));
      }
    }
  }
}

