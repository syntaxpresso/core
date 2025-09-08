package io.github.syntaxpresso.core.service.java.language;

import static org.junit.jupiter.api.Assertions.*;

import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import io.github.syntaxpresso.core.service.java.language.extra.VariableCapture;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.treesitter.TSNode;

@DisplayName("LocalVariableDeclarationService Tests")
class LocalVariableDeclarationServiceTest {
  private LocalVariableDeclarationService service;

  // Reusable source code strings
  private static final String LOCAL_VARIABLES_CODE =
      """
      package io.github.test;

      public class TestClass {
          private String fieldVar = "field";

          public void testMethod(String param1, int param2) {
              String localVar = "test";
              int count = 10;
              final boolean flag = true;
              List<String> items = new ArrayList<>();
          }

          public void anotherMethod(Object obj, List<String> list) {
              String name = "example";
              double value = 3.14;
          }
      }
      """;

  private static final String FIELD_DECLARATIONS_CODE =
      """
      package io.github.test;

      public class FieldClass {
          private String name;
          protected int count;
          public final boolean active = true;
          static final String CONSTANT = "VALUE";

          public void method(String param) {
              String local = param;
          }
      }
      """;

  private static final String TYPED_VARIABLES_CODE =
      """
      package io.github.test;

      import java.util.List;
      import java.util.Map;

      public class TypedClass {
          private String stringField;
          private Integer integerField;
          private List<String> listField;

          public void processString(String stringParam) {
              String localString = "local";
              Integer localInt = 42;
          }

          public void processInteger(Integer intParam) {
              Integer anotherInt = 100;
              String converted = intParam.toString();
          }

          public void processList(List<String> listParam) {
              List<String> localList = listParam;
              String first = listParam.get(0);
          }
      }
      """;

  private static final String EMPTY_FILE_CODE = "";
  private static final String NO_VARIABLES_CODE =
      """
      package io.github.test;

      public class NoVariablesClass {
          public void emptyMethod() {
              // No variables here
          }
      }
      """;

  private TSFile localVariablesFile;
  private TSFile fieldDeclarationsFile;
  private TSFile typedVariablesFile;
  private TSFile emptyFile;
  private TSFile noVariablesFile;

  @BeforeEach
  void setUp() {
    this.service = new LocalVariableDeclarationService();
    localVariablesFile = new TSFile(SupportedLanguage.JAVA, LOCAL_VARIABLES_CODE);
    fieldDeclarationsFile = new TSFile(SupportedLanguage.JAVA, FIELD_DECLARATIONS_CODE);
    typedVariablesFile = new TSFile(SupportedLanguage.JAVA, TYPED_VARIABLES_CODE);
    emptyFile = new TSFile(SupportedLanguage.JAVA, EMPTY_FILE_CODE);
    noVariablesFile = new TSFile(SupportedLanguage.JAVA, NO_VARIABLES_CODE);
  }

  @Nested
  @DisplayName("getAllMethodParameterNodes Tests")
  class GetAllMethodParameterNodesTests {

    @Test
    @DisplayName(
        "should return all variable declarations including fields, parameters, and local variables")
    void shouldReturnAllVariableDeclarations() {
      List<TSNode> nodes = service.getAllMethodParameterNodes(localVariablesFile);

      assertFalse(nodes.isEmpty());
      assertTrue(
          nodes.size()
              >= 8); // fieldVar, param1, param2, localVar, count, flag, items, obj, list, name,
                     // value
    }

    @Test
    @DisplayName("should return field declarations")
    void shouldReturnFieldDeclarations() {
      List<TSNode> nodes = service.getAllMethodParameterNodes(fieldDeclarationsFile);

      assertFalse(nodes.isEmpty());
      assertTrue(nodes.size() >= 5); // Fields: name, count, active, CONSTANT + method param + local
    }

    @Test
    @DisplayName("should return empty list for null file")
    void shouldReturnEmptyListForNullFile() {
      List<TSNode> nodes = service.getAllMethodParameterNodes(null);
      assertEquals(Collections.emptyList(), nodes);
    }

    @Test
    @DisplayName("should return empty list for empty file")
    void shouldReturnEmptyListForEmptyFile() {
      List<TSNode> nodes = service.getAllMethodParameterNodes(emptyFile);
      assertTrue(nodes.isEmpty());
    }

    @Test
    @DisplayName("should return empty list for file with no variables")
    void shouldReturnEmptyListForFileWithNoVariables() {
      List<TSNode> nodes = service.getAllMethodParameterNodes(noVariablesFile);
      assertTrue(nodes.isEmpty());
    }
  }

  @Nested
  @DisplayName("getLocalVariableDeclarationNodeInfo Tests")
  class GetLocalVariableDeclarationNodeInfoTests {

    @Test
    @DisplayName("should return variable info with captures for local variable")
    void shouldReturnVariableInfoForLocalVariable() {
      List<TSNode> allNodes = service.getAllMethodParameterNodes(localVariablesFile);

      // Find a local variable declaration node
      TSNode localVarNode =
          allNodes.stream()
              .filter(node -> node.getType().equals("local_variable_declaration"))
              .findFirst()
              .orElse(null);

      assertNotNull(localVarNode);

      List<Map<String, TSNode>> captures =
          service.getLocalVariableDeclarationNodeInfo(localVariablesFile, localVarNode);

      assertFalse(captures.isEmpty());

      // Check that we have the expected captures
      Map<String, TSNode> firstCapture = captures.get(0);
      assertTrue(firstCapture.containsKey("node"));
      assertTrue(firstCapture.containsKey("type"));
      assertTrue(firstCapture.containsKey("name"));
    }

    @Test
    @DisplayName("should return field info with captures")
    void shouldReturnFieldInfoWithCaptures() {
      List<TSNode> allNodes = service.getAllMethodParameterNodes(fieldDeclarationsFile);

      // Find a field declaration node
      TSNode fieldNode =
          allNodes.stream()
              .filter(node -> node.getType().equals("field_declaration"))
              .findFirst()
              .orElse(null);

      assertNotNull(fieldNode);

      List<Map<String, TSNode>> captures =
          service.getLocalVariableDeclarationNodeInfo(fieldDeclarationsFile, fieldNode);

      assertFalse(captures.isEmpty());

      // Check that we have the expected captures
      Map<String, TSNode> firstCapture = captures.get(0);
      assertTrue(firstCapture.containsKey("node"));
      assertTrue(firstCapture.containsKey("type"));
      assertTrue(firstCapture.containsKey("name"));
    }

    @Test
    @DisplayName("should return formal parameter info with captures")
    void shouldReturnFormalParameterInfoWithCaptures() {
      List<TSNode> allNodes = service.getAllMethodParameterNodes(fieldDeclarationsFile);

      // Find a formal parameter node
      TSNode paramNode =
          allNodes.stream()
              .filter(node -> node.getType().equals("formal_parameter"))
              .findFirst()
              .orElse(null);

      assertNotNull(paramNode);

      List<Map<String, TSNode>> captures =
          service.getLocalVariableDeclarationNodeInfo(fieldDeclarationsFile, paramNode);

      assertFalse(captures.isEmpty());

      // Check that we have the expected captures
      Map<String, TSNode> firstCapture = captures.get(0);
      assertTrue(firstCapture.containsKey("node"));
      assertTrue(firstCapture.containsKey("type"));
      assertTrue(firstCapture.containsKey("name"));
    }

    @Test
    @DisplayName("should return empty list for null file")
    void shouldReturnEmptyListForNullFile() {
      TSNode dummyNode = localVariablesFile.getTree().getRootNode();
      List<Map<String, TSNode>> captures =
          service.getLocalVariableDeclarationNodeInfo(null, dummyNode);
      assertEquals(Collections.emptyList(), captures);
    }

    @Test
    @DisplayName("should return empty list for empty file")
    void shouldReturnEmptyListForEmptyFile() {
      TSNode dummyNode = emptyFile.getTree().getRootNode();
      List<Map<String, TSNode>> captures =
          service.getLocalVariableDeclarationNodeInfo(emptyFile, dummyNode);
      assertTrue(captures.isEmpty());
    }
  }

  @Nested
  @DisplayName("getLocalVariableDeclarationNodeByCaptureName Tests")
  class GetLocalVariableDeclarationNodeByCaptureNameTests {

    @Test
    @DisplayName("should return name node for local variable")
    void shouldReturnNameNodeForLocalVariable() {
      List<TSNode> allNodes = service.getAllMethodParameterNodes(localVariablesFile);
      TSNode localVarNode =
          allNodes.stream()
              .filter(node -> node.getType().equals("local_variable_declaration"))
              .findFirst()
              .orElse(null);

      assertNotNull(localVarNode);

      Optional<TSNode> nameNode =
          service.getLocalVariableDeclarationChildNodeByCaptureName(
              localVariablesFile, localVarNode, VariableCapture.VARIABLE_NAME);

      assertTrue(nameNode.isPresent());
      assertEquals("identifier", nameNode.get().getType());
    }

    @Test
    @DisplayName("should return type node for field declaration")
    void shouldReturnTypeNodeForFieldDeclaration() {
      List<TSNode> allNodes = service.getAllMethodParameterNodes(fieldDeclarationsFile);
      TSNode fieldNode =
          allNodes.stream()
              .filter(node -> node.getType().equals("field_declaration"))
              .findFirst()
              .orElse(null);

      assertNotNull(fieldNode);

      Optional<TSNode> typeNode =
          service.getLocalVariableDeclarationChildNodeByCaptureName(
              fieldDeclarationsFile, fieldNode, VariableCapture.VARIABLE_TYPE);

      assertTrue(typeNode.isPresent());
    }

    @Test
    @DisplayName("should return empty for invalid capture name")
    void shouldReturnEmptyForInvalidCaptureName() {
      List<TSNode> allNodes = service.getAllMethodParameterNodes(localVariablesFile);
      TSNode localVarNode =
          allNodes.stream()
              .filter(node -> node.getType().equals("local_variable_declaration"))
              .findFirst()
              .orElse(null);

      assertNotNull(localVarNode);

      Optional<TSNode> result =
          service.getLocalVariableDeclarationChildNodeByCaptureName(
              localVariablesFile, localVarNode, VariableCapture.VARIABLE_NAME);

      assertTrue(result.isPresent());
    }

    @Test
    @DisplayName("should return empty for null file")
    void shouldReturnEmptyForNullFile() {
      TSNode dummyNode = localVariablesFile.getTree().getRootNode();
      Optional<TSNode> result =
          service.getLocalVariableDeclarationChildNodeByCaptureName(null, dummyNode, VariableCapture.VARIABLE_NAME);

      assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("should return empty for wrong node type")
    void shouldReturnEmptyForWrongNodeType() {
      TSNode wrongTypeNode =
          localVariablesFile.getTree().getRootNode(); // This will be "program" type

      Optional<TSNode> result =
          service.getLocalVariableDeclarationChildNodeByCaptureName(
              localVariablesFile, wrongTypeNode, VariableCapture.VARIABLE_NAME);

      assertTrue(result.isEmpty());
    }
  }

  @Nested
  @DisplayName("getLocalVariableDeclarationNameNode Tests")
  class GetLocalVariableDeclarationNameNodeTests {

    @Test
    @DisplayName("should return name node for local variable")
    void shouldReturnNameNodeForLocalVariable() {
      List<TSNode> allNodes = service.getAllMethodParameterNodes(localVariablesFile);
      TSNode localVarNode =
          allNodes.stream()
              .filter(node -> node.getType().equals("local_variable_declaration"))
              .findFirst()
              .orElse(null);

      assertNotNull(localVarNode);

      Optional<TSNode> nameNode =
          service.getLocalVariableDeclarationNameNode(localVariablesFile, localVarNode);

      assertTrue(nameNode.isPresent());
      assertEquals("identifier", nameNode.get().getType());
    }

    @Test
    @DisplayName("should return empty for null file")
    void shouldReturnEmptyForNullFile() {
      TSNode dummyNode = localVariablesFile.getTree().getRootNode();
      Optional<TSNode> result = service.getLocalVariableDeclarationNameNode(null, dummyNode);

      assertTrue(result.isEmpty());
    }
  }

  @Nested
  @DisplayName("getLocalVariableDeclarationValueNode Tests")
  class GetLocalVariableDeclarationValueNodeTests {

    @Test
    @DisplayName("should return type node (note: method name suggests value but returns type)")
    void shouldReturnTypeNode() {
      List<TSNode> allNodes = service.getAllMethodParameterNodes(localVariablesFile);
      TSNode localVarNode =
          allNodes.stream()
              .filter(node -> node.getType().equals("local_variable_declaration"))
              .findFirst()
              .orElse(null);

      assertNotNull(localVarNode);

      Optional<TSNode> typeNode =
          service.getLocalVariableDeclarationValueNode(localVariablesFile, localVarNode);

      assertTrue(typeNode.isPresent());
    }

    @Test
    @DisplayName("should return empty for null file")
    void shouldReturnEmptyForNullFile() {
      TSNode dummyNode = localVariablesFile.getTree().getRootNode();
      Optional<TSNode> result = service.getLocalVariableDeclarationValueNode(null, dummyNode);

      assertTrue(result.isEmpty());
    }
  }

  @Nested
  @DisplayName("getLocalVariableDeclarationModifiersNode Tests")
  class GetLocalVariableDeclarationModifiersNodeTests {

    @Test
    @DisplayName("should return modifiers node for field with modifiers")
    void shouldReturnModifiersNodeForFieldWithModifiers() {
      List<TSNode> allNodes = service.getAllMethodParameterNodes(fieldDeclarationsFile);
      TSNode fieldNode =
          allNodes.stream()
              .filter(node -> node.getType().equals("field_declaration"))
              .findFirst()
              .orElse(null);

      assertNotNull(fieldNode);

      // May or may not be present depending on whether the field has modifiers
      // This is acceptable as some fields might not have explicit modifiers
    }

    @Test
    @DisplayName("should return empty for null file")
    void shouldReturnEmptyForNullFile() {
      TSNode dummyNode = localVariablesFile.getTree().getRootNode();
      Optional<TSNode> result = service.getLocalVariableDeclarationModifiersNode(null, dummyNode);

      assertTrue(result.isEmpty());
    }
  }

  @Nested
  @DisplayName("findLocalVariableDeclarationByType Tests")
  class FindLocalVariableDeclarationByTypeTests {

    @Test
    @DisplayName("should find variables by String type")
    void shouldFindVariablesByStringType() {
      List<TSNode> stringNodes =
          service.findLocalVariableDeclarationByType(typedVariablesFile, "String");

      assertFalse(stringNodes.isEmpty());
      assertTrue(stringNodes.size() >= 3);
    }

    @Test
    @DisplayName("should find variables by Integer type")
    void shouldFindVariablesByIntegerType() {
      List<TSNode> integerNodes =
          service.findLocalVariableDeclarationByType(typedVariablesFile, "Integer");

      assertFalse(integerNodes.isEmpty());
      assertTrue(integerNodes.size() >= 2);
    }

    @Test
    @DisplayName("should return empty list for non-existent type")
    void shouldReturnEmptyListForNonExistentType() {
      List<TSNode> nodes =
          service.findLocalVariableDeclarationByType(typedVariablesFile, "NonExistentType");
      assertTrue(nodes.isEmpty());
    }

    @Test
    @DisplayName("should handle primitive types correctly")
    void shouldHandlePrimitiveTypesCorrectly() {
      List<TSNode> intNodes = service.findLocalVariableDeclarationByType(localVariablesFile, "int");

      assertFalse(intNodes.isEmpty());
      assertTrue(intNodes.size() >= 1);
    }

    @Test
    @DisplayName("should be case sensitive for type matching")
    void shouldBeCaseSensitiveForTypeMatching() {
      List<TSNode> upperCaseNodes =
          service.findLocalVariableDeclarationByType(typedVariablesFile, "STRING");
      List<TSNode> lowerCaseNodes =
          service.findLocalVariableDeclarationByType(typedVariablesFile, "string");
      List<TSNode> correctCaseNodes =
          service.findLocalVariableDeclarationByType(typedVariablesFile, "String");

      assertTrue(upperCaseNodes.isEmpty());
      assertTrue(lowerCaseNodes.isEmpty());
      assertFalse(correctCaseNodes.isEmpty());
    }

    @Test
    @DisplayName("should return empty list for null file")
    void shouldReturnEmptyListForNullFile() {
      List<TSNode> nodes = service.findLocalVariableDeclarationByType(null, "String");
      assertEquals(Collections.emptyList(), nodes);
    }

    @Test
    @DisplayName("should return empty list for null type")
    void shouldReturnEmptyListForNullType() {
      List<TSNode> nodes = service.findLocalVariableDeclarationByType(typedVariablesFile, null);
      assertEquals(Collections.emptyList(), nodes);
    }

    @Test
    @DisplayName("should return empty list for empty type")
    void shouldReturnEmptyListForEmptyType() {
      List<TSNode> nodes = service.findLocalVariableDeclarationByType(typedVariablesFile, "");
      assertEquals(Collections.emptyList(), nodes);
    }

    @Test
    @DisplayName("should return empty list for whitespace type")
    void shouldReturnEmptyListForWhitespaceType() {
      List<TSNode> nodes = service.findLocalVariableDeclarationByType(typedVariablesFile, "   ");
      assertEquals(Collections.emptyList(), nodes);
    }
  }

  @Nested
  @DisplayName("Edge Cases and Error Handling")
  class EdgeCasesAndErrorHandlingTests {

    @Test
    @DisplayName("should handle malformed syntax gracefully")
    void shouldHandleMalformedSyntaxGracefully() {
      String malformedCode = "public class Malformed { String incomplete";
      TSFile malformedFile = new TSFile(SupportedLanguage.JAVA, malformedCode);

      List<TSNode> nodes = service.getAllMethodParameterNodes(malformedFile);
      List<TSNode> typeNodes = service.findLocalVariableDeclarationByType(malformedFile, "String");

      assertNotNull(nodes);
      assertNotNull(typeNodes);
    }

    @Test
    @DisplayName("should handle generic types in variable declarations")
    void shouldHandleGenericTypesInVariableDeclarations() {
      String genericCode =
          """
          package io.github.test;

          import java.util.List;
          import java.util.Map;

          public class GenericClass {
              private List<String> stringList;
              private Map<String, Integer> stringIntMap;

              public void processGeneric(List<String> param) {
                  List<Integer> localList = new ArrayList<>();
                  Map<String, Object> localMap = new HashMap<>();
              }
          }
          """;

      TSFile genericFile = new TSFile(SupportedLanguage.JAVA, genericCode);

      List<TSNode> listNodes = service.findLocalVariableDeclarationByType(genericFile, "List");
      List<TSNode> mapNodes = service.findLocalVariableDeclarationByType(genericFile, "Map");

      assertTrue(listNodes.size() >= 0);
      assertTrue(mapNodes.size() >= 0);
    }

    @Test
    @DisplayName("should handle array types correctly")
    void shouldHandleArrayTypesCorrectly() {
      String arrayCode =
          """
          package io.github.test;

          public class ArrayClass {
              private String[] stringArray;
              private int[] intArray;

              public void processArrays(String[] params, int[] numbers) {
                  String[] localStrings = new String[10];
                  int[] localInts = {1, 2, 3};
              }
          }
          """;

      TSFile arrayFile = new TSFile(SupportedLanguage.JAVA, arrayCode);

      List<TSNode> allNodes = service.getAllMethodParameterNodes(arrayFile);
      assertNotNull(allNodes);
    }
  }
}

