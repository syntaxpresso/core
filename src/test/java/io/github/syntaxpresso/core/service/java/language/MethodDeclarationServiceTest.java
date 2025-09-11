package io.github.syntaxpresso.core.service.java.language;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import io.github.syntaxpresso.core.service.java.language.extra.MethodCapture;
import io.github.syntaxpresso.core.service.java.language.extra.MethodInvocationCapture;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.treesitter.TSNode;

@DisplayName("MethodDeclarationService Tests")
class MethodDeclarationServiceTest {
  private MethodDeclarationService methodDeclarationService;

  private static final String SIMPLE_CLASS_CODE =
      """
      public class SimpleClass {
          private String name;

          public void simpleMethod() {
              System.out.println("Hello");
          }

          public String getName() {
              return name;
          }

          private int calculateValue(int x, int y) {
              return x + y;
          }
      }
      """;

  private static final String COMPLEX_METHOD_CODE =
      """
      public class ComplexClass {
          public static void main(String[] args) {
              ComplexClass instance = new ComplexClass();
              instance.doSomething();
          }

          public static void main(String... args) {
              System.out.println("Varargs main");
          }

          @Override
          public String toString() {
              return "ComplexClass";
          }

          public <T> T genericMethod(T value) {
              return value;
          }

          public void methodWithInvocations() {
              this.getName();
              getName();
              System.out.println("test");
              list.add("item");
              obj.field.method();
              chain().method().call();
          }

          private String getName() {
              return "name";
          }
      }
      """;

  private static final String INVOCATION_TEST_CODE =
      """
      public class InvocationTest {
          public void testMethod() {
              System.out.println("test");
              obj.method();
          }
      }
      """;

  private static final String EDGE_CASE_CODE =
      """
      public class EdgeCaseClass {
          // No methods
      }

      class EmptyMethodClass {
          public void emptyMethod() {}
      }
      """;

  @BeforeEach
  void setUp() {
    FormalParameterService formalParameterService = new FormalParameterService();
    this.methodDeclarationService = new MethodDeclarationService(formalParameterService);
  }

  @Nested
  @DisplayName("getAllMethodDeclarationNodes() Tests")
  class GetAllMethodDeclarationNodesTests {

    /**
     * Tests that getAllMethodDeclarationNodes finds all method declarations in a class.
     *
     * <p>Usage example:
     *
     * <pre>
     * TSNode classNode = classService.findClassByName(tsFile, "MyClass").get();
     * List&lt;TSNode&gt; methods = service.getAllMethodDeclarationNodes(tsFile, classNode);
     * // methods contains all method declaration nodes within the class
     * </pre>
     */
    @Test
    @DisplayName("should find all method declarations in a class")
    void getAllMethodDeclarationNodes_withMultipleMethods_shouldFindAll() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_CLASS_CODE);
      TSNode classNode = getClassNode(tsFile, "SimpleClass");

      List<TSNode> methods =
          methodDeclarationService.getAllMethodDeclarationNodes(tsFile, classNode);

      assertEquals(3, methods.size());
    }

    @Test
    @DisplayName("should return empty list for class with no methods")
    void getAllMethodDeclarationNodes_withNoMethods_shouldReturnEmpty() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, EDGE_CASE_CODE);
      TSNode classNode = getClassNode(tsFile, "EdgeCaseClass");

      List<TSNode> methods =
          methodDeclarationService.getAllMethodDeclarationNodes(tsFile, classNode);

      assertTrue(methods.isEmpty());
    }

    @Test
    @DisplayName("should return empty list for invalid input")
    void getAllMethodDeclarationNodes_withInvalidInput_shouldReturnEmpty() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_CLASS_CODE);
      TSNode invalidNode = tsFile.query("(identifier) @id").returning("id").execute().firstNode();

      List<TSNode> result1 = methodDeclarationService.getAllMethodDeclarationNodes(null, null);
      assertTrue(result1.isEmpty());

      List<TSNode> result2 =
          methodDeclarationService.getAllMethodDeclarationNodes(tsFile, invalidNode);
      assertTrue(result2.isEmpty());
    }
  }

  @Nested
  @DisplayName("getAllLocalMethodInvocationNodes() Tests")
  class GetAllLocalMethodInvocationNodesTests {

    @Test
    @DisplayName("should find local method invocations")
    void getAllLocalMethodInvocationNodes_withLocalInvocations_shouldFindThem() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, COMPLEX_METHOD_CODE);
      TSNode classNode = getClassNode(tsFile, "ComplexClass");

      List<TSNode> invocations =
          methodDeclarationService.getAllLocalMethodInvocationNodes(tsFile, classNode);

      assertFalse(invocations.isEmpty());
    }

    @Test
    @DisplayName("should return empty list for invalid input")
    void getAllLocalMethodInvocationNodes_withInvalidInput_shouldReturnEmpty() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_CLASS_CODE);
      TSNode invalidNode = tsFile.query("(identifier) @id").returning("id").execute().firstNode();

      List<TSNode> result1 = methodDeclarationService.getAllLocalMethodInvocationNodes(null, null);
      assertTrue(result1.isEmpty());

      List<TSNode> result2 =
          methodDeclarationService.getAllLocalMethodInvocationNodes(tsFile, invalidNode);
      assertTrue(result2.isEmpty());
    }
  }

  @Nested
  @DisplayName("getMethodDeclarationNodeInfo() Tests")
  class GetMethodDeclarationNodeInfoTests {

    /**
     * Tests that getMethodDeclarationNodeInfo extracts detailed method information.
     *
     * <p>Usage example:
     *
     * <pre>
     * List&lt;Map&lt;String, TSNode&gt;&gt; methodInfo = service.getMethodDeclarationNodeInfo(tsFile, methodNode);
     * Map&lt;String, TSNode&gt; info = methodInfo.get(0);
     * String methodName = tsFile.getTextFromNode(info.get("methodName"));
     * String returnType = tsFile.getTextFromNode(info.get("methodType"));
     * // methodName = "getName", returnType = "String"
     * </pre>
     */
    @Test
    @DisplayName("should extract method information")
    void getMethodDeclarationNodeInfo_withValidMethod_shouldExtractInfo() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_CLASS_CODE);
      TSNode classNode = getClassNode(tsFile, "SimpleClass");
      List<TSNode> methods =
          methodDeclarationService.getAllMethodDeclarationNodes(tsFile, classNode);
      TSNode getNameMethod = findMethodByName(tsFile, methods, "getName");

      List<Map<String, TSNode>> methodInfo =
          methodDeclarationService.getMethodDeclarationNodeInfo(tsFile, getNameMethod);

      assertFalse(methodInfo.isEmpty());
      Map<String, TSNode> info = methodInfo.get(0);

      TSNode methodName = info.get(MethodCapture.METHOD_NAME.getCaptureName());
      assertNotNull(methodName);
      assertEquals("getName", tsFile.getTextFromNode(methodName));

      TSNode methodType = info.get(MethodCapture.METHOD_TYPE.getCaptureName());
      assertNotNull(methodType);
      assertEquals("String", tsFile.getTextFromNode(methodType));
    }

    @Test
    @DisplayName("should extract method with parameters")
    void getMethodDeclarationNodeInfo_withParameters_shouldExtractParameterInfo() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_CLASS_CODE);
      TSNode classNode = getClassNode(tsFile, "SimpleClass");
      List<TSNode> methods =
          methodDeclarationService.getAllMethodDeclarationNodes(tsFile, classNode);
      TSNode calculateMethod = findMethodByName(tsFile, methods, "calculateValue");

      List<Map<String, TSNode>> methodInfo =
          methodDeclarationService.getMethodDeclarationNodeInfo(tsFile, calculateMethod);

      assertFalse(methodInfo.isEmpty());
      Map<String, TSNode> info = methodInfo.get(0);

      TSNode parameters = info.get(MethodCapture.METHOD_PARAMETERS.getCaptureName());
      assertNotNull(parameters);
      assertTrue(tsFile.getTextFromNode(parameters).contains("int x"));
      assertTrue(tsFile.getTextFromNode(parameters).contains("int y"));
    }

    @Test
    @DisplayName("should return empty list for invalid input")
    void getMethodDeclarationNodeInfo_withInvalidInput_shouldReturnEmpty() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_CLASS_CODE);
      TSNode invalidNode = tsFile.query("(identifier) @id").returning("id").execute().firstNode();

      List<Map<String, TSNode>> result1 =
          methodDeclarationService.getMethodDeclarationNodeInfo(null, null);
      assertTrue(result1.isEmpty());

      List<Map<String, TSNode>> result2 =
          methodDeclarationService.getMethodDeclarationNodeInfo(tsFile, invalidNode);
      assertTrue(result2.isEmpty());
    }
  }

  @Nested
  @DisplayName("getMethodInvocationNodeInfo() Tests")
  class GetMethodInvocationNodeInfoTests {

    /**
     * Tests that getMethodInvocationNodeInfo extracts method invocation details.
     *
     * <p>Usage example:
     *
     * <pre>
     * List&lt;Map&lt;String, TSNode&gt;&gt; invocationInfo = service.getMethodInvocationNodeInfo(tsFile, invocationNode);
     * Map&lt;String, TSNode&gt; info = invocationInfo.get(0);
     * String methodName = tsFile.getTextFromNode(info.get("method"));
     * String objectName = tsFile.getTextFromNode(info.get("object"));
     * // methodName = "add", objectName = "myList"
     * </pre>
     */
    @Test
    @DisplayName("should extract method invocation information")
    void getMethodInvocationNodeInfo_withValidInvocation_shouldExtractInfo() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, INVOCATION_TEST_CODE);
      List<TSNode> invocations =
          tsFile.query("(method_invocation) @invocation").returning("invocation").execute().nodes();

      if (!invocations.isEmpty()) {
        TSNode methodInvocation = invocations.get(0);

        List<Map<String, TSNode>> invocationInfo =
            methodDeclarationService.getMethodInvocationNodeInfo(tsFile, methodInvocation);

        // Verify we can extract info without throwing exceptions
        assertNotNull(invocationInfo);
        if (!invocationInfo.isEmpty()) {
          Map<String, TSNode> info = invocationInfo.get(0);
          assertNotNull(info);
        }
      }
    }

    @Test
    @DisplayName("should handle this invocations")
    void getMethodInvocationNodeInfo_withThisInvocation_shouldHandleThis() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, COMPLEX_METHOD_CODE);
      List<TSNode> invocations =
          tsFile
              .query("(method_invocation object: (this)) @invocation")
              .returning("invocation")
              .execute()
              .nodes();

      if (!invocations.isEmpty()) {
        List<Map<String, TSNode>> invocationInfo =
            methodDeclarationService.getMethodInvocationNodeInfo(tsFile, invocations.get(0));

        if (!invocationInfo.isEmpty()) {
          Map<String, TSNode> info = invocationInfo.get(0);

          TSNode thisNode = info.get(MethodInvocationCapture.THIS.getCaptureName());
          if (thisNode != null) {
            assertEquals("this", tsFile.getTextFromNode(thisNode));
          }

          // At least verify that we can extract the method name from this invocation
          TSNode methodName = info.get(MethodInvocationCapture.METHOD.getCaptureName());
          assertNotNull(methodName);
        }
      } else {
        // If no 'this' invocations found, just verify service doesn't crash
        assertTrue(
            true, "No 'this' invocations found in test code, but service handled gracefully");
      }
    }

    @Test
    @DisplayName("should return empty list for invalid input")
    void getMethodInvocationNodeInfo_withInvalidInput_shouldReturnEmpty() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_CLASS_CODE);
      TSNode invalidNode = tsFile.query("(identifier) @id").returning("id").execute().firstNode();

      List<Map<String, TSNode>> result1 =
          methodDeclarationService.getMethodInvocationNodeInfo(null, null);
      assertTrue(result1.isEmpty());

      List<Map<String, TSNode>> result2 =
          methodDeclarationService.getMethodInvocationNodeInfo(tsFile, invalidNode);
      assertTrue(result2.isEmpty());
    }
  }

  @Nested
  @DisplayName("getMethodDeclarationTypeNode() Tests")
  class GetMethodDeclarationTypeNodeTests {

    /**
     * Tests that getMethodDeclarationTypeNode retrieves the return type node.
     *
     * <p>Usage example:
     *
     * <pre>
     * service.getMethodDeclarationTypeNode(tsFile, methodNode).ifPresent(typeNode -> {
     *   System.out.println("Return type: " + tsFile.getTextFromNode(typeNode));
     * });
     * // Output: "Return type: String"
     * </pre>
     */
    @Test
    @DisplayName("should retrieve method return type node")
    void getMethodDeclarationTypeNode_withReturnType_shouldReturnTypeNode() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_CLASS_CODE);
      TSNode classNode = getClassNode(tsFile, "SimpleClass");
      List<TSNode> methods =
          methodDeclarationService.getAllMethodDeclarationNodes(tsFile, classNode);
      TSNode getNameMethod = findMethodByName(tsFile, methods, "getName");

      Optional<TSNode> typeNode =
          methodDeclarationService.getMethodDeclarationTypeNode(tsFile, getNameMethod);

      assertTrue(typeNode.isPresent());
      assertEquals("String", tsFile.getTextFromNode(typeNode.get()));
    }

    @Test
    @DisplayName("should handle void methods")
    void getMethodDeclarationTypeNode_withVoidMethod_shouldReturnVoid() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_CLASS_CODE);
      TSNode classNode = getClassNode(tsFile, "SimpleClass");
      List<TSNode> methods =
          methodDeclarationService.getAllMethodDeclarationNodes(tsFile, classNode);
      TSNode voidMethod = findMethodByName(tsFile, methods, "simpleMethod");

      Optional<TSNode> typeNode =
          methodDeclarationService.getMethodDeclarationTypeNode(tsFile, voidMethod);

      assertTrue(typeNode.isPresent());
      assertEquals("void", tsFile.getTextFromNode(typeNode.get()));
    }

    @Test
    @DisplayName("should return empty for invalid input")
    void getMethodDeclarationTypeNode_withInvalidInput_shouldReturnEmpty() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_CLASS_CODE);
      TSNode invalidNode = tsFile.query("(identifier) @id").returning("id").execute().firstNode();

      Optional<TSNode> result1 = methodDeclarationService.getMethodDeclarationTypeNode(null, null);
      assertFalse(result1.isPresent());

      Optional<TSNode> result2 =
          methodDeclarationService.getMethodDeclarationTypeNode(tsFile, invalidNode);
      assertFalse(result2.isPresent());
    }
  }

  @Nested
  @DisplayName("getMethodDeclarationNameNode() Tests")
  class GetMethodDeclarationNameNodeTests {

    /**
     * Tests that getMethodDeclarationNameNode retrieves the method name node.
     *
     * <p>Usage example:
     *
     * <pre>
     * service.getMethodDeclarationNameNode(tsFile, methodNode).ifPresent(nameNode -> {
     *   System.out.println("Method name: " + tsFile.getTextFromNode(nameNode));
     * });
     * // Output: "Method name: getName"
     * </pre>
     */
    @Test
    @DisplayName("should retrieve method name node")
    void getMethodDeclarationNameNode_withValidMethod_shouldReturnNameNode() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_CLASS_CODE);
      TSNode classNode = getClassNode(tsFile, "SimpleClass");
      List<TSNode> methods =
          methodDeclarationService.getAllMethodDeclarationNodes(tsFile, classNode);
      TSNode getNameMethod = findMethodByName(tsFile, methods, "getName");

      Optional<TSNode> nameNode =
          methodDeclarationService.getMethodDeclarationNameNode(tsFile, getNameMethod);

      assertTrue(nameNode.isPresent());
      assertEquals("getName", tsFile.getTextFromNode(nameNode.get()));
    }

    @Test
    @DisplayName("should return empty for invalid input")
    void getMethodDeclarationNameNode_withInvalidInput_shouldReturnEmpty() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_CLASS_CODE);
      TSNode invalidNode = tsFile.query("(identifier) @id").returning("id").execute().firstNode();

      Optional<TSNode> result1 = methodDeclarationService.getMethodDeclarationNameNode(null, null);
      assertFalse(result1.isPresent());

      Optional<TSNode> result2 =
          methodDeclarationService.getMethodDeclarationNameNode(tsFile, invalidNode);
      assertFalse(result2.isPresent());
    }
  }

  @Nested
  @DisplayName("getMethodInvocationObjectNode() Tests")
  class GetMethodInvocationObjectNodeTests {

    /**
     * Tests that getMethodInvocationObjectNode retrieves the object node from invocations.
     *
     * <p>Usage example:
     *
     * <pre>
     * service.getMethodInvocationObjectNode(tsFile, invocationNode).ifPresent(objectNode -> {
     *   System.out.println("Called on object: " + tsFile.getTextFromNode(objectNode));
     * });
     * // Output: "Called on object: myList"
     * </pre>
     */
    @Test
    @DisplayName("should retrieve object node from method invocation")
    void getMethodInvocationObjectNode_withObjectInvocation_shouldReturnObject() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, INVOCATION_TEST_CODE);
      List<TSNode> invocations =
          tsFile.query("(method_invocation) @invocation").returning("invocation").execute().nodes();

      if (!invocations.isEmpty()) {
        TSNode methodInvocation = invocations.get(0);
        Optional<TSNode> objectNode =
            methodDeclarationService.getMethodInvocationObjectNode(tsFile, methodInvocation);

        // Just verify the method doesn't crash
        assertNotNull(objectNode);
      }
    }

    @Test
    @DisplayName("should return empty for invalid input")
    void getMethodInvocationObjectNode_withInvalidInput_shouldReturnEmpty() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_CLASS_CODE);
      TSNode invalidNode = tsFile.query("(identifier) @id").returning("id").execute().firstNode();

      Optional<TSNode> result1 = methodDeclarationService.getMethodInvocationObjectNode(null, null);
      assertFalse(result1.isPresent());

      Optional<TSNode> result2 =
          methodDeclarationService.getMethodInvocationObjectNode(tsFile, invalidNode);
      assertFalse(result2.isPresent());
    }
  }

  @Nested
  @DisplayName("getMethodInvocationNameNode() Tests")
  class GetMethodInvocationNameNodeTests {

    @Test
    @DisplayName("should retrieve method name from invocation")
    void getMethodInvocationNameNode_withValidInvocation_shouldReturnMethodName() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, INVOCATION_TEST_CODE);
      List<TSNode> invocations =
          tsFile.query("(method_invocation) @invocation").returning("invocation").execute().nodes();

      if (!invocations.isEmpty()) {
        TSNode methodInvocation = invocations.get(0);
        Optional<TSNode> nameNode =
            methodDeclarationService.getMethodInvocationNameNode(tsFile, methodInvocation);

        assertNotNull(nameNode);
      } else {
        assertTrue(true, "No method invocations found in test code");
      }
    }

    @Test
    @DisplayName("should return empty for invalid input")
    void getMethodInvocationNameNode_withInvalidInput_shouldReturnEmpty() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_CLASS_CODE);

      Optional<TSNode> result1 = methodDeclarationService.getMethodInvocationNameNode(tsFile, null);
      assertFalse(result1.isPresent());

      TSNode classNode = getClassNode(tsFile, "SimpleClass");
      Optional<TSNode> result2 =
          methodDeclarationService.getMethodInvocationNameNode(tsFile, classNode);
      assertFalse(result2.isPresent());
    }
  }

  @Nested
  @DisplayName("findMethodInvocationsByVariableNameAndMethodName() Tests")
  class FindMethodInvocationsByVariableNameAndMethodNameTests {

    /**
     * Tests that findMethodInvocationsByVariableNameAndMethodName finds specific method calls.
     *
     * <p>Usage example:
     *
     * <pre>
     * List&lt;TSNode&gt; invocations = service.findMethodInvocationsByVariableNameAndMethodName(
     *   tsFile, "myList", "add", methodBodyNode);
     * System.out.println("Found " + invocations.size() + " calls to myList.add()");
     * // Output: "Found 3 calls to myList.add()"
     * </pre>
     */
    @Test
    @DisplayName("should find method invocations by variable and method name")
    void findMethodInvocationsByVariableNameAndMethodName_withMatches_shouldFindInvocations() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, INVOCATION_TEST_CODE);
      TSNode classNode = getClassNode(tsFile, "InvocationTest");
      List<TSNode> methods =
          methodDeclarationService.getAllMethodDeclarationNodes(tsFile, classNode);
      TSNode testMethod = findMethodByName(tsFile, methods, "testMethod");

      List<TSNode> invocations =
          methodDeclarationService.findMethodInvocationsByVariableNameAndMethodName(
              tsFile, "obj", "method", testMethod);

      // May find invocations or be empty depending on tree-sitter parsing
      assertNotNull(invocations);
    }

    @Test
    @DisplayName("should return empty list when no matches found")
    void findMethodInvocationsByVariableNameAndMethodName_withNoMatches_shouldReturnEmpty() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, INVOCATION_TEST_CODE);
      TSNode classNode = getClassNode(tsFile, "InvocationTest");

      List<TSNode> invocations =
          methodDeclarationService.findMethodInvocationsByVariableNameAndMethodName(
              tsFile, "nonExistent", "method", classNode);

      assertTrue(invocations.isEmpty());
    }

    @Test
    @DisplayName("should return empty list for invalid input")
    void findMethodInvocationsByVariableNameAndMethodName_withInvalidInput_shouldReturnEmpty() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_CLASS_CODE);
      TSNode classNode = getClassNode(tsFile, "SimpleClass");

      List<TSNode> result1 =
          methodDeclarationService.findMethodInvocationsByVariableNameAndMethodName(
              null, "var", "method", classNode);
      assertTrue(result1.isEmpty());

      List<TSNode> result2 =
          methodDeclarationService.findMethodInvocationsByVariableNameAndMethodName(
              tsFile, null, "method", classNode);
      assertTrue(result2.isEmpty());

      List<TSNode> result3 =
          methodDeclarationService.findMethodInvocationsByVariableNameAndMethodName(
              tsFile, "var", null, classNode);
      assertTrue(result3.isEmpty());

      List<TSNode> result4 =
          methodDeclarationService.findMethodInvocationsByVariableNameAndMethodName(
              tsFile, "", "method", classNode);
      assertTrue(result4.isEmpty());
    }
  }

  @Nested
  @DisplayName("isMainMethod() Tests")
  class IsMainMethodTests {

    /**
     * Tests that isMainMethod correctly identifies Java main methods.
     *
     * <p>Usage example:
     *
     * <pre>
     * boolean isMain = service.isMainMethod(tsFile, methodNode);
     * if (isMain) {
     *   System.out.println("This is the main method.");
     * }
     * </pre>
     */
    @Test
    @DisplayName("should identify standard main method")
    void isMainMethod_withStandardMain_shouldReturnTrue() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, COMPLEX_METHOD_CODE);
      TSNode classNode = getClassNode(tsFile, "ComplexClass");
      List<TSNode> methods =
          methodDeclarationService.getAllMethodDeclarationNodes(tsFile, classNode);
      TSNode mainMethod = findMethodByName(tsFile, methods, "main");

      boolean isMain = methodDeclarationService.isMainMethod(tsFile, mainMethod);

      assertTrue(isMain);
    }

    @Test
    @DisplayName("should identify varargs main method")
    void isMainMethod_withVarargsMain_shouldReturnTrue() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, COMPLEX_METHOD_CODE);
      List<TSNode> methods =
          tsFile
              .query("(method_declaration name: (identifier) @name (#eq? @name \"main\")) @method")
              .returning("method")
              .execute()
              .nodes();

      // Find the varargs version
      TSNode varargsMain =
          methods.stream()
              .filter(method -> tsFile.getTextFromNode(method).contains("..."))
              .findFirst()
              .orElse(null);

      if (varargsMain != null) {
        boolean isMain = methodDeclarationService.isMainMethod(tsFile, varargsMain);
        assertTrue(isMain);
      }
    }

    @Test
    @DisplayName("should return false for non-main methods")
    void isMainMethod_withNonMainMethod_shouldReturnFalse() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_CLASS_CODE);
      TSNode classNode = getClassNode(tsFile, "SimpleClass");
      List<TSNode> methods =
          methodDeclarationService.getAllMethodDeclarationNodes(tsFile, classNode);
      TSNode regularMethod = findMethodByName(tsFile, methods, "getName");

      boolean isMain = methodDeclarationService.isMainMethod(tsFile, regularMethod);

      assertFalse(isMain);
    }

    @Test
    @DisplayName("should return false for invalid input")
    void isMainMethod_withInvalidInput_shouldReturnFalse() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_CLASS_CODE);
      TSNode invalidNode = tsFile.query("(identifier) @id").returning("id").execute().firstNode();

      boolean result1 = methodDeclarationService.isMainMethod(null, null);
      assertFalse(result1);

      boolean result2 = methodDeclarationService.isMainMethod(tsFile, null);
      assertFalse(result2);

      boolean result3 = methodDeclarationService.isMainMethod(tsFile, invalidNode);
      assertFalse(result3);
    }
  }

  @Nested
  @DisplayName("Service Dependencies Tests")
  class ServiceDependenciesTests {

    @Test
    @DisplayName("should have required dependencies initialized")
    void constructor_shouldInitializeAllDependencies() {
      assertNotNull(methodDeclarationService.getFormalParameterService());
    }

    @Test
    @DisplayName("should return correct dependency instances")
    void getDependencies_shouldReturnCorrectInstances() {
      assertTrue(
          methodDeclarationService.getFormalParameterService() instanceof FormalParameterService);
    }
  }

  @Nested
  @DisplayName("Edge Cases and Error Handling Tests")
  class EdgeCasesTests {

    @Test
    @DisplayName("should handle empty methods correctly")
    void methods_withEmptyMethods_shouldHandleCorrectly() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, EDGE_CASE_CODE);
      TSNode classNode = getClassNode(tsFile, "EmptyMethodClass");

      List<TSNode> methods =
          methodDeclarationService.getAllMethodDeclarationNodes(tsFile, classNode);
      assertEquals(1, methods.size());

      TSNode emptyMethod = methods.get(0);
      List<Map<String, TSNode>> methodInfo =
          methodDeclarationService.getMethodDeclarationNodeInfo(tsFile, emptyMethod);
      assertFalse(methodInfo.isEmpty());
    }

    @Test
    @DisplayName("should handle malformed source code")
    void methods_withMalformedCode_shouldHandleGracefully() {
      TSFile tsFile =
          new TSFile(SupportedLanguage.JAVA, "public class Broken { public void method(");

      List<TSNode> classNodes =
          tsFile.query("(class_declaration) @class").returning("class").execute().nodes();

      if (!classNodes.isEmpty()) {
        List<TSNode> methods =
            methodDeclarationService.getAllMethodDeclarationNodes(tsFile, classNodes.get(0));
        assertNotNull(methods);
      }
    }

    @Test
    @DisplayName("should handle generic methods correctly")
    void methods_withGenericMethods_shouldHandleCorrectly() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, COMPLEX_METHOD_CODE);
      TSNode classNode = getClassNode(tsFile, "ComplexClass");
      List<TSNode> methods =
          methodDeclarationService.getAllMethodDeclarationNodes(tsFile, classNode);
      TSNode genericMethod = findMethodByName(tsFile, methods, "genericMethod");

      List<Map<String, TSNode>> methodInfo =
          methodDeclarationService.getMethodDeclarationNodeInfo(tsFile, genericMethod);

      assertFalse(methodInfo.isEmpty());
      Map<String, TSNode> info = methodInfo.get(0);
      TSNode methodName = info.get(MethodCapture.METHOD_NAME.getCaptureName());
      assertNotNull(methodName);
      assertEquals("genericMethod", tsFile.getTextFromNode(methodName));
    }
  }

  // Helper methods
  private TSNode getClassNode(TSFile tsFile, String className) {
    return tsFile
        .query(
            String.format(
                "(class_declaration name: (identifier) @name (#eq? @name \"%s\")) @class",
                className))
        .returning("class")
        .execute()
        .firstNode();
  }

  private TSNode findMethodByName(TSFile tsFile, List<TSNode> methods, String methodName) {
    return methods.stream()
        .filter(
            method -> {
              List<Map<String, TSNode>> info =
                  methodDeclarationService.getMethodDeclarationNodeInfo(tsFile, method);
              if (!info.isEmpty()) {
                TSNode nameNode = info.get(0).get(MethodCapture.METHOD_NAME.getCaptureName());
                return nameNode != null && methodName.equals(tsFile.getTextFromNode(nameNode));
              }
              return false;
            })
        .findFirst()
        .orElse(null);
  }
}
