package io.github.syntaxpresso.core.service.java.language;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.treesitter.TSNode;

@DisplayName("ClassFieldDeclarationService Tests")
class ClassFieldDeclarationServiceTest {

  private ClassFieldDeclarationService service;
  private ClassDeclarationService classService;

  @BeforeEach
  void setUp() {
    this.service = new ClassFieldDeclarationService();
    this.classService = new ClassDeclarationService();
  }

  @Nested
  @DisplayName("getAllClassFieldDeclarationNodes Tests")
  class GetAllClassFieldDeclarationNodesTests {

    @Test
    @DisplayName("Should return all field declarations from a simple class")
    void shouldReturnAllFieldDeclarationsFromSimpleClass() {
      String sourceCode =
          """
          public class TestClass {
              private String name;
              private int age;
              public boolean active;
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
      TSNode classDeclarationNode =
          classService.getAllClassDeclarations(tsFile).get(0).get("classDeclaration");

      List<TSNode> fieldDeclarations =
          service.getAllClassFieldDeclarationNodes(tsFile, classDeclarationNode);

      assertEquals(3, fieldDeclarations.size());
      assertEquals("field_declaration", fieldDeclarations.get(0).getType());
      assertEquals("field_declaration", fieldDeclarations.get(1).getType());
      assertEquals("field_declaration", fieldDeclarations.get(2).getType());
    }

    @Test
    @DisplayName("Should return empty list when class has no fields")
    void shouldReturnEmptyListWhenClassHasNoFields() {
      String sourceCode =
          """
          public class TestClass {
              public void method() {
                  System.out.println("No fields here");
              }
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
      TSNode classDeclarationNode =
          classService.getAllClassDeclarations(tsFile).get(0).get("classDeclaration");

      List<TSNode> fieldDeclarations =
          service.getAllClassFieldDeclarationNodes(tsFile, classDeclarationNode);

      assertTrue(fieldDeclarations.isEmpty());
    }

    @Test
    @DisplayName("Should handle class with mixed members correctly")
    void shouldHandleClassWithMixedMembersCorrectly() {
      String sourceCode =
          """
          public class TestClass {
              private String field1;

              public TestClass() {
              }

              private int field2;

              public void method() {
              }

              protected boolean field3;
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
      TSNode classDeclarationNode =
          classService.getAllClassDeclarations(tsFile).get(0).get("classDeclaration");

      List<TSNode> fieldDeclarations =
          service.getAllClassFieldDeclarationNodes(tsFile, classDeclarationNode);

      assertEquals(3, fieldDeclarations.size());
    }

    @Test
    @DisplayName("Should return empty list when tsFile is null")
    void shouldReturnEmptyListWhenTsFileIsNull() {
      String sourceCode =
          """
          public class TestClass {
              private String name;
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
      TSNode classDeclarationNode =
          classService.getAllClassDeclarations(tsFile).get(0).get("classDeclaration");

      List<TSNode> fieldDeclarations =
          service.getAllClassFieldDeclarationNodes(null, classDeclarationNode);

      assertTrue(fieldDeclarations.isEmpty());
    }

    @Test
    @DisplayName("Should return empty list when classDeclarationNode is not class_declaration")
    void shouldReturnEmptyListWhenClassDeclarationNodeIsNotClassDeclaration() {
      String sourceCode =
          """
          public class TestClass {
              private String name;
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
      TSNode rootNode = tsFile.getTree().getRootNode();

      List<TSNode> fieldDeclarations = service.getAllClassFieldDeclarationNodes(tsFile, rootNode);

      assertTrue(fieldDeclarations.isEmpty());
    }
  }

  @Nested
  @DisplayName(" getClassFieldNodeInfo Tests")
  class GetClassFieldInfoTests {

    @Test
    @DisplayName("Should extract field info with type and name")
    void shouldExtractFieldInfoWithTypeAndName() {
      String sourceCode =
          """
          public class TestClass {
              private String name;
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
      TSNode classDeclarationNode =
          classService.getAllClassDeclarations(tsFile).get(0).get("classDeclaration");
      List<TSNode> fieldDeclarations =
          service.getAllClassFieldDeclarationNodes(tsFile, classDeclarationNode);

      List<Map<String, TSNode>> fieldInfo =
          service.getClassFieldNodeInfo(tsFile, fieldDeclarations.get(0));

      assertEquals(1, fieldInfo.size());
      Map<String, TSNode> firstField = fieldInfo.get(0);
      assertTrue(firstField.containsKey("type"));
      assertTrue(firstField.containsKey("name"));
      assertNotNull(firstField.get("type"));
      assertNotNull(firstField.get("name"));
      assertEquals("String", tsFile.getTextFromNode(firstField.get("type")));
      assertEquals("name", tsFile.getTextFromNode(firstField.get("name")));
    }

    @Test
    @DisplayName("Should extract field info with type, name and value")
    void shouldExtractFieldInfoWithTypeNameAndValue() {
      String sourceCode =
          """
          public class TestClass {
              private String name = "defaultName";
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
      TSNode classDeclarationNode =
          classService.getAllClassDeclarations(tsFile).get(0).get("classDeclaration");
      List<TSNode> fieldDeclarations =
          service.getAllClassFieldDeclarationNodes(tsFile, classDeclarationNode);

      List<Map<String, TSNode>> fieldInfo =
          service.getClassFieldNodeInfo(tsFile, fieldDeclarations.get(0));

      assertEquals(1, fieldInfo.size());
      Map<String, TSNode> firstField = fieldInfo.get(0);
      assertTrue(firstField.containsKey("type"));
      assertTrue(firstField.containsKey("name"));
      assertTrue(firstField.containsKey("value"));
      assertEquals("String", tsFile.getTextFromNode(firstField.get("type")));
      assertEquals("name", tsFile.getTextFromNode(firstField.get("name")));
      assertEquals("\"defaultName\"", tsFile.getTextFromNode(firstField.get("value")));
    }

    @Test
    @DisplayName("Should handle multiple field declarators")
    void shouldHandleMultipleFieldDeclarators() {
      String sourceCode =
          """
          public class TestClass {
              private int x, y;
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
      TSNode classDeclarationNode =
          classService.getAllClassDeclarations(tsFile).get(0).get("classDeclaration");
      List<TSNode> fieldDeclarations =
          service.getAllClassFieldDeclarationNodes(tsFile, classDeclarationNode);

      List<Map<String, TSNode>> fieldInfo =
          service.getClassFieldNodeInfo(tsFile, fieldDeclarations.get(0));

      assertEquals(2, fieldInfo.size());
      assertEquals("x", tsFile.getTextFromNode(fieldInfo.get(0).get("name")));
      assertEquals("y", tsFile.getTextFromNode(fieldInfo.get(1).get("name")));
      assertEquals("int", tsFile.getTextFromNode(fieldInfo.get(0).get("type")));
      assertEquals("int", tsFile.getTextFromNode(fieldInfo.get(1).get("type")));
    }

    @Test
    @DisplayName("Should handle complex field types")
    void shouldHandleComplexFieldTypes() {
      String sourceCode =
          """
          public class TestClass {
              private List<String> names;
              private Map<String, Integer> scoreMap;
              private Optional<User> user;
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
      TSNode classDeclarationNode =
          classService.getAllClassDeclarations(tsFile).get(0).get("classDeclaration");
      List<TSNode> fieldDeclarations =
          service.getAllClassFieldDeclarationNodes(tsFile, classDeclarationNode);

      List<Map<String, TSNode>> firstFieldInfo =
          service.getClassFieldNodeInfo(tsFile, fieldDeclarations.get(0));
      List<Map<String, TSNode>> secondFieldInfo =
          service.getClassFieldNodeInfo(tsFile, fieldDeclarations.get(1));
      List<Map<String, TSNode>> thirdFieldInfo =
          service.getClassFieldNodeInfo(tsFile, fieldDeclarations.get(2));

      assertEquals("names", tsFile.getTextFromNode(firstFieldInfo.get(0).get("name")));
      assertEquals("scoreMap", tsFile.getTextFromNode(secondFieldInfo.get(0).get("name")));
      assertEquals("user", tsFile.getTextFromNode(thirdFieldInfo.get(0).get("name")));
      assertTrue(tsFile.getTextFromNode(firstFieldInfo.get(0).get("type")).contains("List"));
      assertTrue(tsFile.getTextFromNode(secondFieldInfo.get(0).get("type")).contains("Map"));
      assertTrue(tsFile.getTextFromNode(thirdFieldInfo.get(0).get("type")).contains("Optional"));
    }

    @Test
    @DisplayName("Should handle field with object instantiation")
    void shouldHandleFieldWithObjectInstantiation() {
      String sourceCode =
          """
          public class TestClass {
              private List<String> names = new ArrayList<>();
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
      TSNode classDeclarationNode =
          classService.getAllClassDeclarations(tsFile).get(0).get("classDeclaration");
      List<TSNode> fieldDeclarations =
          service.getAllClassFieldDeclarationNodes(tsFile, classDeclarationNode);

      List<Map<String, TSNode>> fieldInfo =
          service.getClassFieldNodeInfo(tsFile, fieldDeclarations.get(0));

      assertEquals(1, fieldInfo.size());
      Map<String, TSNode> firstField = fieldInfo.get(0);
      assertTrue(firstField.containsKey("value"));
      assertTrue(tsFile.getTextFromNode(firstField.get("value")).contains("new ArrayList<>()"));
    }

    @Test
    @DisplayName("Should return empty list when tsFile is null")
    void shouldReturnEmptyListWhenTsFileIsNull() {
      String sourceCode =
          """
          public class TestClass {
              private String name;
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
      TSNode classDeclarationNode =
          classService.getAllClassDeclarations(tsFile).get(0).get("classDeclaration");
      List<TSNode> fieldDeclarations =
          service.getAllClassFieldDeclarationNodes(tsFile, classDeclarationNode);

      List<Map<String, TSNode>> fieldInfo =
          service.getClassFieldNodeInfo(null, fieldDeclarations.get(0));

      assertTrue(fieldInfo.isEmpty());
    }

    @Test
    @DisplayName("Should return empty list when fieldDeclarationNode is not field_declaration")
    void shouldReturnEmptyListWhenFieldDeclarationNodeIsNotFieldDeclaration() {
      String sourceCode =
          """
          public class TestClass {
              private String name;
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
      TSNode rootNode = tsFile.getTree().getRootNode();

      List<Map<String, TSNode>> fieldInfo = service.getClassFieldNodeInfo(tsFile, rootNode);

      assertTrue(fieldInfo.isEmpty());
    }

    @Test
    @DisplayName("Should handle static fields")
    void shouldHandleStaticFields() {
      String sourceCode =
          """
          public class TestClass {
              private static final String CONSTANT = "value";
              public static int counter = 0;
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
      TSNode classDeclarationNode =
          classService.getAllClassDeclarations(tsFile).get(0).get("classDeclaration");
      List<TSNode> fieldDeclarations =
          service.getAllClassFieldDeclarationNodes(tsFile, classDeclarationNode);

      List<Map<String, TSNode>> firstFieldInfo =
          service.getClassFieldNodeInfo(tsFile, fieldDeclarations.get(0));
      List<Map<String, TSNode>> secondFieldInfo =
          service.getClassFieldNodeInfo(tsFile, fieldDeclarations.get(1));

      assertEquals("CONSTANT", tsFile.getTextFromNode(firstFieldInfo.get(0).get("name")));
      assertEquals("counter", tsFile.getTextFromNode(secondFieldInfo.get(0).get("name")));
      assertEquals("String", tsFile.getTextFromNode(firstFieldInfo.get(0).get("type")));
      assertEquals("int", tsFile.getTextFromNode(secondFieldInfo.get(0).get("type")));
    }

    @Test
    @DisplayName("Should handle array fields")
    void shouldHandleArrayFields() {
      String sourceCode =
          """
          public class TestClass {
              private String[] names;
              private int[][] matrix;
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
      TSNode classDeclarationNode =
          classService.getAllClassDeclarations(tsFile).get(0).get("classDeclaration");
      List<TSNode> fieldDeclarations =
          service.getAllClassFieldDeclarationNodes(tsFile, classDeclarationNode);

      List<Map<String, TSNode>> firstFieldInfo =
          service.getClassFieldNodeInfo(tsFile, fieldDeclarations.get(0));
      List<Map<String, TSNode>> secondFieldInfo =
          service.getClassFieldNodeInfo(tsFile, fieldDeclarations.get(1));

      assertEquals("names", tsFile.getTextFromNode(firstFieldInfo.get(0).get("name")));
      assertEquals("matrix", tsFile.getTextFromNode(secondFieldInfo.get(0).get("name")));
      assertTrue(tsFile.getTextFromNode(firstFieldInfo.get(0).get("type")).contains("String[]"));
      assertTrue(tsFile.getTextFromNode(secondFieldInfo.get(0).get("type")).contains("int[][]"));
    }
  }

  @Nested
  @DisplayName("getClassFieldTypeNode Tests")
  class GetClassFieldTypeNodeTests {

    @Test
    @DisplayName("Should return type node for simple field")
    void shouldReturnTypeNodeForSimpleField() {
      String sourceCode =
          """
          public class TestClass {
              private String name;
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
      TSNode classDeclarationNode =
          classService.getAllClassDeclarations(tsFile).get(0).get("classDeclaration");
      List<TSNode> fieldDeclarations =
          service.getAllClassFieldDeclarationNodes(tsFile, classDeclarationNode);

      Optional<TSNode> typeNode = service.getClassFieldTypeNode(tsFile, fieldDeclarations.get(0));

      assertTrue(typeNode.isPresent());
      assertEquals("String", tsFile.getTextFromNode(typeNode.get()));
      assertEquals("type_identifier", typeNode.get().getType());
    }

    @Test
    @DisplayName("Should return type node for complex generic field")
    void shouldReturnTypeNodeForComplexGenericField() {
      String sourceCode =
          """
          public class TestClass {
              private List<Map<String, Integer>> complexField;
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
      TSNode classDeclarationNode =
          classService.getAllClassDeclarations(tsFile).get(0).get("classDeclaration");
      List<TSNode> fieldDeclarations =
          service.getAllClassFieldDeclarationNodes(tsFile, classDeclarationNode);

      Optional<TSNode> typeNode = service.getClassFieldTypeNode(tsFile, fieldDeclarations.get(0));

      assertTrue(typeNode.isPresent());
      assertTrue(tsFile.getTextFromNode(typeNode.get()).contains("List<Map<String, Integer>>"));
    }

    @Test
    @DisplayName("Should return type node for array field")
    void shouldReturnTypeNodeForArrayField() {
      String sourceCode =
          """
          public class TestClass {
              private int[][] matrix;
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
      TSNode classDeclarationNode =
          classService.getAllClassDeclarations(tsFile).get(0).get("classDeclaration");
      List<TSNode> fieldDeclarations =
          service.getAllClassFieldDeclarationNodes(tsFile, classDeclarationNode);

      Optional<TSNode> typeNode = service.getClassFieldTypeNode(tsFile, fieldDeclarations.get(0));

      assertTrue(typeNode.isPresent());
      assertTrue(tsFile.getTextFromNode(typeNode.get()).contains("int[][]"));
    }

    @Test
    @DisplayName("Should return type node for primitive field")
    void shouldReturnTypeNodeForPrimitiveField() {
      String sourceCode =
          """
          public class TestClass {
              private int count;
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
      TSNode classDeclarationNode =
          classService.getAllClassDeclarations(tsFile).get(0).get("classDeclaration");
      List<TSNode> fieldDeclarations =
          service.getAllClassFieldDeclarationNodes(tsFile, classDeclarationNode);

      Optional<TSNode> typeNode = service.getClassFieldTypeNode(tsFile, fieldDeclarations.get(0));

      assertTrue(typeNode.isPresent());
      assertEquals("int", tsFile.getTextFromNode(typeNode.get()));
      assertEquals("integral_type", typeNode.get().getType());
    }

    @Test
    @DisplayName("Should return empty when tsFile is null")
    void shouldReturnEmptyWhenTsFileIsNull() {
      String sourceCode =
          """
          public class TestClass {
              private String name;
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
      TSNode classDeclarationNode =
          classService.getAllClassDeclarations(tsFile).get(0).get("classDeclaration");
      List<TSNode> fieldDeclarations =
          service.getAllClassFieldDeclarationNodes(tsFile, classDeclarationNode);

      Optional<TSNode> typeNode = service.getClassFieldTypeNode(null, fieldDeclarations.get(0));

      assertTrue(typeNode.isEmpty());
    }

    @Test
    @DisplayName("Should return empty when fieldDeclarationNode is not field_declaration")
    void shouldReturnEmptyWhenFieldDeclarationNodeIsNotFieldDeclaration() {
      String sourceCode =
          """
          public class TestClass {
              private String name;
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
      TSNode rootNode = tsFile.getTree().getRootNode();

      Optional<TSNode> typeNode = service.getClassFieldTypeNode(tsFile, rootNode);

      assertTrue(typeNode.isEmpty());
    }
  }

  @Nested
  @DisplayName("getClassFieldNameNode Tests")
  class GetClassFieldNameNodeTests {

    @Test
    @DisplayName("Should return name node for simple field")
    void shouldReturnNameNodeForSimpleField() {
      String sourceCode =
          """
          public class TestClass {
              private String name;
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
      TSNode classDeclarationNode =
          classService.getAllClassDeclarations(tsFile).get(0).get("classDeclaration");
      List<TSNode> fieldDeclarations =
          service.getAllClassFieldDeclarationNodes(tsFile, classDeclarationNode);

      Optional<TSNode> nameNode = service.getClassFieldNameNode(tsFile, fieldDeclarations.get(0));

      assertTrue(nameNode.isPresent());
      assertEquals("name", tsFile.getTextFromNode(nameNode.get()));
      assertEquals("identifier", nameNode.get().getType());
    }

    @Test
    @DisplayName("Should return name node for field with complex type")
    void shouldReturnNameNodeForFieldWithComplexType() {
      String sourceCode =
          """
          public class TestClass {
              private List<Map<String, Integer>> complexData;
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
      TSNode classDeclarationNode =
          classService.getAllClassDeclarations(tsFile).get(0).get("classDeclaration");
      List<TSNode> fieldDeclarations =
          service.getAllClassFieldDeclarationNodes(tsFile, classDeclarationNode);

      Optional<TSNode> nameNode = service.getClassFieldNameNode(tsFile, fieldDeclarations.get(0));

      assertTrue(nameNode.isPresent());
      assertEquals("complexData", tsFile.getTextFromNode(nameNode.get()));
      assertEquals("identifier", nameNode.get().getType());
    }

    @Test
    @DisplayName("Should return name node for static field")
    void shouldReturnNameNodeForStaticField() {
      String sourceCode =
          """
          public class TestClass {
              private static final String CONSTANT = "value";
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
      TSNode classDeclarationNode =
          classService.getAllClassDeclarations(tsFile).get(0).get("classDeclaration");
      List<TSNode> fieldDeclarations =
          service.getAllClassFieldDeclarationNodes(tsFile, classDeclarationNode);

      Optional<TSNode> nameNode = service.getClassFieldNameNode(tsFile, fieldDeclarations.get(0));

      assertTrue(nameNode.isPresent());
      assertEquals("CONSTANT", tsFile.getTextFromNode(nameNode.get()));
      assertEquals("identifier", nameNode.get().getType());
    }

    @Test
    @DisplayName("Should return name node for array field")
    void shouldReturnNameNodeForArrayField() {
      String sourceCode =
          """
          public class TestClass {
              private String[] names;
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
      TSNode classDeclarationNode =
          classService.getAllClassDeclarations(tsFile).get(0).get("classDeclaration");
      List<TSNode> fieldDeclarations =
          service.getAllClassFieldDeclarationNodes(tsFile, classDeclarationNode);

      Optional<TSNode> nameNode = service.getClassFieldNameNode(tsFile, fieldDeclarations.get(0));

      assertTrue(nameNode.isPresent());
      assertEquals("names", tsFile.getTextFromNode(nameNode.get()));
      assertEquals("identifier", nameNode.get().getType());
    }

    @Test
    @DisplayName("Should return empty when tsFile is null")
    void shouldReturnEmptyWhenTsFileIsNull() {
      String sourceCode =
          """
          public class TestClass {
              private String name;
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
      TSNode classDeclarationNode =
          classService.getAllClassDeclarations(tsFile).get(0).get("classDeclaration");
      List<TSNode> fieldDeclarations =
          service.getAllClassFieldDeclarationNodes(tsFile, classDeclarationNode);

      Optional<TSNode> nameNode = service.getClassFieldNameNode(null, fieldDeclarations.get(0));

      assertTrue(nameNode.isEmpty());
    }

    @Test
    @DisplayName("Should return empty when fieldDeclarationNode is not field_declaration")
    void shouldReturnEmptyWhenFieldDeclarationNodeIsNotFieldDeclaration() {
      String sourceCode =
          """
          public class TestClass {
              private String name;
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
      TSNode rootNode = tsFile.getTree().getRootNode();

      Optional<TSNode> nameNode = service.getClassFieldNameNode(tsFile, rootNode);

      assertTrue(nameNode.isEmpty());
    }
  }

  @Nested
  @DisplayName("getClassFieldValueNode Tests")
  class GetClassFieldValueNodeTests {

    @Test
    @DisplayName("Should return value node for initialized field")
    void shouldReturnValueNodeForInitializedField() {
      String sourceCode =
          """
          public class TestClass {
              private String name = "test";
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
      TSNode classDeclarationNode =
          classService.getAllClassDeclarations(tsFile).get(0).get("classDeclaration");
      List<TSNode> fieldDeclarations =
          service.getAllClassFieldDeclarationNodes(tsFile, classDeclarationNode);

      Optional<TSNode> valueNode = service.getClassFieldValueNode(tsFile, fieldDeclarations.get(0));

      assertTrue(valueNode.isPresent());
      assertEquals("\"test\"", tsFile.getTextFromNode(valueNode.get()));
      assertEquals("string_literal", valueNode.get().getType());
    }

    @Test
    @DisplayName("Should return value node for numeric initialization")
    void shouldReturnValueNodeForNumericInitialization() {
      String sourceCode =
          """
          public class TestClass {
              private int count = 42;
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
      TSNode classDeclarationNode =
          classService.getAllClassDeclarations(tsFile).get(0).get("classDeclaration");
      List<TSNode> fieldDeclarations =
          service.getAllClassFieldDeclarationNodes(tsFile, classDeclarationNode);

      Optional<TSNode> valueNode = service.getClassFieldValueNode(tsFile, fieldDeclarations.get(0));

      assertTrue(valueNode.isPresent());
      assertEquals("42", tsFile.getTextFromNode(valueNode.get()));
      assertEquals("decimal_integer_literal", valueNode.get().getType());
    }

    @Test
    @DisplayName("Should return value node for object instantiation")
    void shouldReturnValueNodeForObjectInstantiation() {
      String sourceCode =
          """
          public class TestClass {
              private List<String> names = new ArrayList<>();
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
      TSNode classDeclarationNode =
          classService.getAllClassDeclarations(tsFile).get(0).get("classDeclaration");
      List<TSNode> fieldDeclarations =
          service.getAllClassFieldDeclarationNodes(tsFile, classDeclarationNode);

      Optional<TSNode> valueNode = service.getClassFieldValueNode(tsFile, fieldDeclarations.get(0));

      assertTrue(valueNode.isPresent());
      assertEquals("new ArrayList<>()", tsFile.getTextFromNode(valueNode.get()));
      assertEquals("object_creation_expression", valueNode.get().getType());
    }

    @Test
    @DisplayName("Should return value node for boolean initialization")
    void shouldReturnValueNodeForBooleanInitialization() {
      String sourceCode =
          """
          public class TestClass {
              private boolean active = true;
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
      TSNode classDeclarationNode =
          classService.getAllClassDeclarations(tsFile).get(0).get("classDeclaration");
      List<TSNode> fieldDeclarations =
          service.getAllClassFieldDeclarationNodes(tsFile, classDeclarationNode);

      Optional<TSNode> valueNode = service.getClassFieldValueNode(tsFile, fieldDeclarations.get(0));

      assertTrue(valueNode.isPresent());
      assertEquals("true", tsFile.getTextFromNode(valueNode.get()));
      assertEquals("true", valueNode.get().getType());
    }

    @Test
    @DisplayName("Should return value node for null initialization")
    void shouldReturnValueNodeForNullInitialization() {
      String sourceCode =
          """
          public class TestClass {
              private String name = null;
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
      TSNode classDeclarationNode =
          classService.getAllClassDeclarations(tsFile).get(0).get("classDeclaration");
      List<TSNode> fieldDeclarations =
          service.getAllClassFieldDeclarationNodes(tsFile, classDeclarationNode);

      Optional<TSNode> valueNode = service.getClassFieldValueNode(tsFile, fieldDeclarations.get(0));

      assertTrue(valueNode.isPresent());
      assertEquals("null", tsFile.getTextFromNode(valueNode.get()));
      assertEquals("null_literal", valueNode.get().getType());
    }

    @Test
    @DisplayName("Should return empty for uninitialized field")
    void shouldReturnEmptyForUninitializedField() {
      String sourceCode =
          """
          public class TestClass {
              private String name;
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
      TSNode classDeclarationNode =
          classService.getAllClassDeclarations(tsFile).get(0).get("classDeclaration");
      List<TSNode> fieldDeclarations =
          service.getAllClassFieldDeclarationNodes(tsFile, classDeclarationNode);

      Optional<TSNode> valueNode = service.getClassFieldValueNode(tsFile, fieldDeclarations.get(0));

      assertTrue(valueNode.isEmpty());
    }

    @Test
    @DisplayName("Should return empty when tsFile is null")
    void shouldReturnEmptyWhenTsFileIsNull() {
      String sourceCode =
          """
          public class TestClass {
              private String name = "test";
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
      TSNode classDeclarationNode =
          classService.getAllClassDeclarations(tsFile).get(0).get("classDeclaration");
      List<TSNode> fieldDeclarations =
          service.getAllClassFieldDeclarationNodes(tsFile, classDeclarationNode);

      Optional<TSNode> valueNode = service.getClassFieldValueNode(null, fieldDeclarations.get(0));

      assertTrue(valueNode.isEmpty());
    }

    @Test
    @DisplayName("Should return empty when fieldDeclarationNode is not field_declaration")
    void shouldReturnEmptyWhenFieldDeclarationNodeIsNotFieldDeclaration() {
      String sourceCode =
          """
          public class TestClass {
              private String name = "test";
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
      TSNode rootNode = tsFile.getTree().getRootNode();

      Optional<TSNode> valueNode = service.getClassFieldValueNode(tsFile, rootNode);

      assertTrue(valueNode.isEmpty());
    }

    @Test
    @DisplayName("Should return value node for complex expression")
    void shouldReturnValueNodeForComplexExpression() {
      String sourceCode =
          """
          public class TestClass {
              private int result = 10 + 5 * 2;
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
      TSNode classDeclarationNode =
          classService.getAllClassDeclarations(tsFile).get(0).get("classDeclaration");
      List<TSNode> fieldDeclarations =
          service.getAllClassFieldDeclarationNodes(tsFile, classDeclarationNode);

      Optional<TSNode> valueNode = service.getClassFieldValueNode(tsFile, fieldDeclarations.get(0));

      assertTrue(valueNode.isPresent());
      assertEquals("10 + 5 * 2", tsFile.getTextFromNode(valueNode.get()));
      assertEquals("binary_expression", valueNode.get().getType());
    }
  }

  @Nested
  @DisplayName("getAllClassFieldUsageNodes Tests")
  class GetAllClassFieldUsageNodesTests {

    @Test
    @DisplayName("Should find field access usages with this keyword")
    void shouldFindFieldAccessUsagesWithThis() {
      String sourceCode =
          """
          public class TestClass {
              private String name;

              public void method() {
                  System.out.println(this.name);
                  this.name = "new value";
              }
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
      TSNode classDeclarationNode =
          classService.getAllClassDeclarations(tsFile).get(0).get("classDeclaration");
      List<TSNode> fieldDeclarations =
          service.getAllClassFieldDeclarationNodes(tsFile, classDeclarationNode);

      List<TSNode> usages =
          service.getAllClassFieldUsageNodes(
              tsFile, fieldDeclarations.get(0), classDeclarationNode);

      assertEquals(2, usages.size());
      for (TSNode usage : usages) {
        assertEquals("name", tsFile.getTextFromNode(usage));
        assertEquals("identifier", usage.getType());
      }
    }

    @Test
    @DisplayName("Should find field access usages without this keyword")
    void shouldFindFieldAccessUsagesWithoutThis() {
      String sourceCode =
          """
          public class TestClass {
              private String name;
              private TestClass other;

              public void method() {
                  System.out.println(other.name);
                  other.name = "value";
              }
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
      TSNode classDeclarationNode =
          classService.getAllClassDeclarations(tsFile).get(0).get("classDeclaration");
      List<TSNode> fieldDeclarations =
          service.getAllClassFieldDeclarationNodes(tsFile, classDeclarationNode);

      // Get the 'name' field (first field)
      TSNode nameFieldDeclaration = fieldDeclarations.get(0);

      List<TSNode> usages =
          service.getAllClassFieldUsageNodes(tsFile, nameFieldDeclaration, classDeclarationNode);

      assertEquals(2, usages.size());
      for (TSNode usage : usages) {
        assertEquals("name", tsFile.getTextFromNode(usage));
        assertEquals("identifier", usage.getType());
      }
    }

    @Test
    @DisplayName("Should find multiple usages of same field")
    void shouldFindMultipleUsagesOfSameField() {
      String sourceCode =
          """
          public class TestClass {
              private int count;

              public void method1() {
                  this.count = 10;
              }

              public void method2() {
                  System.out.println(this.count);
                  this.count += 5;
              }

              public int getCount() {
                  return this.count;
              }
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
      TSNode classDeclarationNode =
          classService.getAllClassDeclarations(tsFile).get(0).get("classDeclaration");
      List<TSNode> fieldDeclarations =
          service.getAllClassFieldDeclarationNodes(tsFile, classDeclarationNode);

      List<TSNode> usages =
          service.getAllClassFieldUsageNodes(
              tsFile, fieldDeclarations.get(0), classDeclarationNode);

      assertEquals(4, usages.size());
      for (TSNode usage : usages) {
        assertEquals("count", tsFile.getTextFromNode(usage));
        assertEquals("identifier", usage.getType());
      }
    }

    @Test
    @DisplayName("Should return empty for field with no usages")
    void shouldReturnEmptyForFieldWithNoUsages() {
      String sourceCode =
          """
          public class TestClass {
              private String unusedField;
              private String usedField;

              public void method() {
                  System.out.println(this.usedField);
              }
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
      TSNode classDeclarationNode =
          classService.getAllClassDeclarations(tsFile).get(0).get("classDeclaration");
      List<TSNode> fieldDeclarations =
          service.getAllClassFieldDeclarationNodes(tsFile, classDeclarationNode);

      // Get the unused field (first field)
      TSNode unusedFieldDeclaration = fieldDeclarations.get(0);

      List<TSNode> usages =
          service.getAllClassFieldUsageNodes(tsFile, unusedFieldDeclaration, classDeclarationNode);

      assertTrue(usages.isEmpty());
    }

    @Test
    @DisplayName("Should find field usage in method call arguments")
    void shouldFindFieldUsageInMethodCallArguments() {
      String sourceCode =
          """
          public class TestClass {
              private String message;

              public void method() {
                  System.out.println(this.message);
                  processMessage(this.message);
              }

              private void processMessage(String msg) {
                  // method implementation
              }
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
      TSNode classDeclarationNode =
          classService.getAllClassDeclarations(tsFile).get(0).get("classDeclaration");
      List<TSNode> fieldDeclarations =
          service.getAllClassFieldDeclarationNodes(tsFile, classDeclarationNode);

      List<TSNode> usages =
          service.getAllClassFieldUsageNodes(
              tsFile, fieldDeclarations.get(0), classDeclarationNode);

      assertEquals(2, usages.size());
      for (TSNode usage : usages) {
        assertEquals("message", tsFile.getTextFromNode(usage));
      }
    }

    @Test
    @DisplayName("Should find field usage in return statements")
    void shouldFindFieldUsageInReturnStatements() {
      String sourceCode =
          """
          public class TestClass {
              private boolean active;

              public boolean isActive() {
                  return this.active;
              }

              public void setActive(boolean value) {
                  this.active = value;
              }
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
      TSNode classDeclarationNode =
          classService.getAllClassDeclarations(tsFile).get(0).get("classDeclaration");
      List<TSNode> fieldDeclarations =
          service.getAllClassFieldDeclarationNodes(tsFile, classDeclarationNode);

      List<TSNode> usages =
          service.getAllClassFieldUsageNodes(
              tsFile, fieldDeclarations.get(0), classDeclarationNode);

      assertEquals(2, usages.size());
      for (TSNode usage : usages) {
        assertEquals("active", tsFile.getTextFromNode(usage));
      }
    }

    @Test
    @DisplayName("Should find field usage in expressions")
    void shouldFindFieldUsageInExpressions() {
      String sourceCode =
          """
          public class TestClass {
              private int value;

              public void method() {
                  int result = this.value + 10;
                  boolean check = this.value > 0;
                  this.value *= 2;
              }
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
      TSNode classDeclarationNode =
          classService.getAllClassDeclarations(tsFile).get(0).get("classDeclaration");
      List<TSNode> fieldDeclarations =
          service.getAllClassFieldDeclarationNodes(tsFile, classDeclarationNode);

      List<TSNode> usages =
          service.getAllClassFieldUsageNodes(
              tsFile, fieldDeclarations.get(0), classDeclarationNode);

      assertEquals(3, usages.size());
      for (TSNode usage : usages) {
        assertEquals("value", tsFile.getTextFromNode(usage));
      }
    }

    @Test
    @DisplayName("Should find field usage in conditional statements")
    void shouldFindFieldUsageInConditionalStatements() {
      String sourceCode =
          """
          public class TestClass {
              private String status;

              public void method() {
                  if (this.status != null) {
                      System.out.println(this.status);
                  }

                  String result = this.status == null ? "empty" : this.status;
              }
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
      TSNode classDeclarationNode =
          classService.getAllClassDeclarations(tsFile).get(0).get("classDeclaration");
      List<TSNode> fieldDeclarations =
          service.getAllClassFieldDeclarationNodes(tsFile, classDeclarationNode);

      List<TSNode> usages =
          service.getAllClassFieldUsageNodes(
              tsFile, fieldDeclarations.get(0), classDeclarationNode);

      assertEquals(4, usages.size());
      for (TSNode usage : usages) {
        assertEquals("status", tsFile.getTextFromNode(usage));
      }
    }

    @Test
    @DisplayName("Should handle static fields")
    void shouldHandleStaticFields() {
      String sourceCode =
          """
          public class TestClass {
              private static String CONSTANT;

              public void method() {
                  System.out.println(TestClass.CONSTANT);
                  TestClass.CONSTANT = "new value";
              }
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
      TSNode classDeclarationNode =
          classService.getAllClassDeclarations(tsFile).get(0).get("classDeclaration");
      List<TSNode> fieldDeclarations =
          service.getAllClassFieldDeclarationNodes(tsFile, classDeclarationNode);

      List<TSNode> usages =
          service.getAllClassFieldUsageNodes(
              tsFile, fieldDeclarations.get(0), classDeclarationNode);

      assertEquals(2, usages.size());
      for (TSNode usage : usages) {
        assertEquals("CONSTANT", tsFile.getTextFromNode(usage));
      }
    }

    @Test
    @DisplayName("Should handle array fields")
    void shouldHandleArrayFields() {
      String sourceCode =
          """
          public class TestClass {
              private int[] numbers;

              public void method() {
                  this.numbers[0] = 5;
                  int length = this.numbers.length;
                  System.out.println(this.numbers[1]);
              }
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
      TSNode classDeclarationNode =
          classService.getAllClassDeclarations(tsFile).get(0).get("classDeclaration");
      List<TSNode> fieldDeclarations =
          service.getAllClassFieldDeclarationNodes(tsFile, classDeclarationNode);

      List<TSNode> usages =
          service.getAllClassFieldUsageNodes(
              tsFile, fieldDeclarations.get(0), classDeclarationNode);

      assertEquals(3, usages.size());
      for (TSNode usage : usages) {
        assertEquals("numbers", tsFile.getTextFromNode(usage));
      }
    }

    @Test
    @DisplayName("Should handle generic type fields")
    void shouldHandleGenericTypeFields() {
      String sourceCode =
          """
          public class TestClass {
              private List<String> items;

              public void method() {
                  this.items.add("item");
                  int size = this.items.size();
                  this.items = new ArrayList<>();
              }
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
      TSNode classDeclarationNode =
          classService.getAllClassDeclarations(tsFile).get(0).get("classDeclaration");
      List<TSNode> fieldDeclarations =
          service.getAllClassFieldDeclarationNodes(tsFile, classDeclarationNode);

      List<TSNode> usages =
          service.getAllClassFieldUsageNodes(
              tsFile, fieldDeclarations.get(0), classDeclarationNode);

      assertEquals(3, usages.size());
      for (TSNode usage : usages) {
        assertEquals("items", tsFile.getTextFromNode(usage));
      }
    }

    @Test
    @DisplayName("Should only find requested field usages when multiple fields exist")
    void shouldOnlyFindRequestedFieldUsagesWhenMultipleFieldsExist() {
      String sourceCode =
          """
          public class TestClass {
              private String name;
              private String title;
              private String description;

              public void method() {
                  this.name = "John";
                  this.title = "Mr.";
                  System.out.println(this.name + " " + this.title);
                  this.description = "Person";
              }
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
      TSNode classDeclarationNode =
          classService.getAllClassDeclarations(tsFile).get(0).get("classDeclaration");
      List<TSNode> fieldDeclarations =
          service.getAllClassFieldDeclarationNodes(tsFile, classDeclarationNode);

      // Get usages for 'name' field (first field)
      List<TSNode> nameUsages =
          service.getAllClassFieldUsageNodes(
              tsFile, fieldDeclarations.get(0), classDeclarationNode);

      // Get usages for 'title' field (second field)
      List<TSNode> titleUsages =
          service.getAllClassFieldUsageNodes(
              tsFile, fieldDeclarations.get(1), classDeclarationNode);

      assertEquals(2, nameUsages.size());
      for (TSNode usage : nameUsages) {
        assertEquals("name", tsFile.getTextFromNode(usage));
      }

      assertEquals(2, titleUsages.size());
      for (TSNode usage : titleUsages) {
        assertEquals("title", tsFile.getTextFromNode(usage));
      }
    }

    @Test
    @DisplayName("Should handle fields with similar names precisely")
    void shouldHandleFieldsWithSimilarNamesPrecisely() {
      String sourceCode =
          """
          public class TestClass {
              private String user;
              private String username;
              private String userInfo;

              public void method() {
                  this.user = "John";
                  this.username = "john123";
                  this.userInfo = "Additional info";
                  System.out.println(this.user);
              }
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
      TSNode classDeclarationNode =
          classService.getAllClassDeclarations(tsFile).get(0).get("classDeclaration");
      List<TSNode> fieldDeclarations =
          service.getAllClassFieldDeclarationNodes(tsFile, classDeclarationNode);

      // Get usages for 'user' field (first field)
      List<TSNode> userUsages =
          service.getAllClassFieldUsageNodes(
              tsFile, fieldDeclarations.get(0), classDeclarationNode);

      assertEquals(2, userUsages.size());
      for (TSNode usage : userUsages) {
        assertEquals("user", tsFile.getTextFromNode(usage));
      }
    }

    @Test
    @DisplayName("Should return empty when tsFile is null")
    void shouldReturnEmptyWhenTsFileIsNull() {
      String sourceCode =
          """
          public class TestClass {
              private String name;
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
      TSNode classDeclarationNode =
          classService.getAllClassDeclarations(tsFile).get(0).get("classDeclaration");
      List<TSNode> fieldDeclarations =
          service.getAllClassFieldDeclarationNodes(tsFile, classDeclarationNode);

      List<TSNode> usages =
          service.getAllClassFieldUsageNodes(null, fieldDeclarations.get(0), classDeclarationNode);

      assertTrue(usages.isEmpty());
    }

    @Test
    @DisplayName("Should return empty when classDeclarationNode is not class_declaration")
    void shouldReturnEmptyWhenClassDeclarationNodeIsNotClassDeclaration() {
      String sourceCode =
          """
          public class TestClass {
              private String name;
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
      TSNode classDeclarationNode =
          classService.getAllClassDeclarations(tsFile).get(0).get("classDeclaration");
      List<TSNode> fieldDeclarations =
          service.getAllClassFieldDeclarationNodes(tsFile, classDeclarationNode);
      TSNode rootNode = tsFile.getTree().getRootNode();

      List<TSNode> usages =
          service.getAllClassFieldUsageNodes(tsFile, fieldDeclarations.get(0), rootNode);

      assertTrue(usages.isEmpty());
    }

    @Test
    @DisplayName("Should return empty when fieldDeclarationNode is not field_declaration")
    void shouldReturnEmptyWhenFieldDeclarationNodeIsNotFieldDeclaration() {
      String sourceCode =
          """
          public class TestClass {
              private String name;
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
      TSNode classDeclarationNode =
          classService.getAllClassDeclarations(tsFile).get(0).get("classDeclaration");
      TSNode rootNode = tsFile.getTree().getRootNode();

      List<TSNode> usages =
          service.getAllClassFieldUsageNodes(tsFile, rootNode, classDeclarationNode);

      assertTrue(usages.isEmpty());
    }

    @Test
    @DisplayName("Should find field usages within class scope including nested classes")
    void shouldFindFieldUsagesWithinClassScopeIncludingNestedClasses() {
      String sourceCode =
          """
          public class OuterClass {
              private String message;

              public void outerMethod() {
                  this.message = "outer";
                  System.out.println(this.message);
              }

              public class InnerClass {
                  private String message;

                  public void innerMethod() {
                      this.message = "inner";
                      System.out.println(this.message);
                  }
              }
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
      List<Map<String, TSNode>> allClasses = classService.getAllClassDeclarations(tsFile);

      // Get outer class
      TSNode outerClassNode = allClasses.get(0).get("classDeclaration");
      List<TSNode> outerFieldDeclarations =
          service.getAllClassFieldDeclarationNodes(tsFile, outerClassNode);

      // Get usages within outer class scope (includes nested classes)
      // Note: This is a current limitation - the method finds all usages of fields with the same
      // name
      // within the class scope, including from nested classes with similarly named fields
      List<TSNode> outerUsages =
          service.getAllClassFieldUsageNodes(tsFile, outerFieldDeclarations.get(0), outerClassNode);

      assertEquals(4, outerUsages.size()); // 2 from outer + 2 from inner class
      for (TSNode usage : outerUsages) {
        assertEquals("message", tsFile.getTextFromNode(usage));
      }
    }
  }

  @Nested
  @DisplayName("findClassFieldNodeByName Tests")
  class FindClassFieldNodeByNameTests {

    @Test
    @DisplayName("Should find field by exact name match")
    void shouldFindFieldByExactNameMatch() {
      String sourceCode =
          """
          public class TestClass {
              private String name;
              private int age;
              private boolean active;
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
      TSNode classDeclarationNode =
          classService.getAllClassDeclarations(tsFile).get(0).get("classDeclaration");

      Optional<TSNode> result =
          service.findClassFieldNodeByName(tsFile, "name", classDeclarationNode);

      assertTrue(result.isPresent());
      assertEquals("field_declaration", result.get().getType());
      String fieldText = tsFile.getTextFromNode(result.get());
      assertTrue(fieldText.contains("String name"));
    }

    @Test
    @DisplayName("Should find field in class with multiple fields")
    void shouldFindFieldInClassWithMultipleFields() {
      String sourceCode =
          """
          public class TestClass {
              private String firstName;
              private String lastName;
              private int count;
              private List<String> items;
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
      TSNode classDeclarationNode =
          classService.getAllClassDeclarations(tsFile).get(0).get("classDeclaration");

      Optional<TSNode> result =
          service.findClassFieldNodeByName(tsFile, "count", classDeclarationNode);

      assertTrue(result.isPresent());
      assertEquals("field_declaration", result.get().getType());
      String fieldText = tsFile.getTextFromNode(result.get());
      assertTrue(fieldText.contains("int count"));
    }

    @Test
    @DisplayName("Should find field with generic type")
    void shouldFindFieldWithGenericType() {
      String sourceCode =
          """
          public class TestClass {
              private List<String> names;
              private Map<String, Integer> scoreMap;
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
      TSNode classDeclarationNode =
          classService.getAllClassDeclarations(tsFile).get(0).get("classDeclaration");

      Optional<TSNode> result =
          service.findClassFieldNodeByName(tsFile, "scoreMap", classDeclarationNode);

      assertTrue(result.isPresent());
      String fieldText = tsFile.getTextFromNode(result.get());
      assertTrue(fieldText.contains("Map<String, Integer> scoreMap"));
    }

    @Test
    @DisplayName("Should find field with initialization")
    void shouldFindFieldWithInitialization() {
      String sourceCode =
          """
          public class TestClass {
              private String defaultName = "John";
              private int count = 0;
              private List<String> items = new ArrayList<>();
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
      TSNode classDeclarationNode =
          classService.getAllClassDeclarations(tsFile).get(0).get("classDeclaration");

      Optional<TSNode> result =
          service.findClassFieldNodeByName(tsFile, "defaultName", classDeclarationNode);

      assertTrue(result.isPresent());
      String fieldText = tsFile.getTextFromNode(result.get());
      assertTrue(fieldText.contains("String defaultName = \"John\""));
    }

    @Test
    @DisplayName("Should find static field")
    void shouldFindStaticField() {
      String sourceCode =
          """
          public class TestClass {
              private static final String CONSTANT = "value";
              private static int counter = 0;
              private String instance;
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
      TSNode classDeclarationNode =
          classService.getAllClassDeclarations(tsFile).get(0).get("classDeclaration");

      Optional<TSNode> result =
          service.findClassFieldNodeByName(tsFile, "CONSTANT", classDeclarationNode);

      assertTrue(result.isPresent());
      String fieldText = tsFile.getTextFromNode(result.get());
      assertTrue(fieldText.contains("static final String CONSTANT"));
    }

    @Test
    @DisplayName("Should find array field")
    void shouldFindArrayField() {
      String sourceCode =
          """
          public class TestClass {
              private int[] numbers;
              private String[][] matrix;
              private List<String> list;
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
      TSNode classDeclarationNode =
          classService.getAllClassDeclarations(tsFile).get(0).get("classDeclaration");

      Optional<TSNode> result =
          service.findClassFieldNodeByName(tsFile, "matrix", classDeclarationNode);

      assertTrue(result.isPresent());
      String fieldText = tsFile.getTextFromNode(result.get());
      assertTrue(fieldText.contains("String[][] matrix"));
    }

    @Test
    @DisplayName("Should find field with multiple declarators")
    void shouldFindFieldWithMultipleDeclarators() {
      String sourceCode =
          """
          public class TestClass {
              private int x, y, z;
              private String name;
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
      TSNode classDeclarationNode =
          classService.getAllClassDeclarations(tsFile).get(0).get("classDeclaration");

      Optional<TSNode> resultY =
          service.findClassFieldNodeByName(tsFile, "y", classDeclarationNode);
      Optional<TSNode> resultZ =
          service.findClassFieldNodeByName(tsFile, "z", classDeclarationNode);

      assertTrue(resultY.isPresent());
      assertTrue(resultZ.isPresent());
      String fieldText = tsFile.getTextFromNode(resultY.get());
      assertTrue(fieldText.contains("int x, y, z"));
    }

    @Test
    @DisplayName("Should return empty when field not found")
    void shouldReturnEmptyWhenFieldNotFound() {
      String sourceCode =
          """
          public class TestClass {
              private String name;
              private int age;
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
      TSNode classDeclarationNode =
          classService.getAllClassDeclarations(tsFile).get(0).get("classDeclaration");

      Optional<TSNode> result =
          service.findClassFieldNodeByName(tsFile, "nonExistentField", classDeclarationNode);

      assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should return empty when class has no fields")
    void shouldReturnEmptyWhenClassHasNoFields() {
      String sourceCode =
          """
          public class TestClass {
              public void method() {
                  System.out.println("No fields");
              }
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
      TSNode classDeclarationNode =
          classService.getAllClassDeclarations(tsFile).get(0).get("classDeclaration");

      Optional<TSNode> result =
          service.findClassFieldNodeByName(tsFile, "anyField", classDeclarationNode);

      assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should be case sensitive")
    void shouldBeCaseSensitive() {
      String sourceCode =
          """
          public class TestClass {
              private String name;
              private String Name;
              private String NAME;
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
      TSNode classDeclarationNode =
          classService.getAllClassDeclarations(tsFile).get(0).get("classDeclaration");

      Optional<TSNode> lowercase =
          service.findClassFieldNodeByName(tsFile, "name", classDeclarationNode);
      Optional<TSNode> capitalized =
          service.findClassFieldNodeByName(tsFile, "Name", classDeclarationNode);
      Optional<TSNode> uppercase =
          service.findClassFieldNodeByName(tsFile, "NAME", classDeclarationNode);

      assertTrue(lowercase.isPresent());
      assertTrue(capitalized.isPresent());
      assertTrue(uppercase.isPresent());
    }

    @Test
    @DisplayName("Should return empty when tsFile is null")
    void shouldReturnEmptyWhenTsFileIsNull() {
      String sourceCode =
          """
          public class TestClass {
              private String name;
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
      TSNode classDeclarationNode =
          classService.getAllClassDeclarations(tsFile).get(0).get("classDeclaration");

      Optional<TSNode> result =
          service.findClassFieldNodeByName(null, "name", classDeclarationNode);

      assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should return empty when fieldDeclaratorName is null")
    void shouldReturnEmptyWhenFieldDeclaratorNameIsNull() {
      String sourceCode =
          """
          public class TestClass {
              private String name;
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
      TSNode classDeclarationNode =
          classService.getAllClassDeclarations(tsFile).get(0).get("classDeclaration");

      Optional<TSNode> result =
          service.findClassFieldNodeByName(tsFile, null, classDeclarationNode);

      assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should return empty when fieldDeclaratorName is empty")
    void shouldReturnEmptyWhenFieldDeclaratorNameIsEmpty() {
      String sourceCode =
          """
          public class TestClass {
              private String name;
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
      TSNode classDeclarationNode =
          classService.getAllClassDeclarations(tsFile).get(0).get("classDeclaration");

      Optional<TSNode> result = service.findClassFieldNodeByName(tsFile, "", classDeclarationNode);

      assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should return empty when classDeclarationNode is not class_declaration")
    void shouldReturnEmptyWhenClassDeclarationNodeIsNotClassDeclaration() {
      String sourceCode =
          """
          public class TestClass {
              private String name;
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
      TSNode rootNode = tsFile.getTree().getRootNode();

      Optional<TSNode> result = service.findClassFieldNodeByName(tsFile, "name", rootNode);

      assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should handle fields with modifiers")
    void shouldHandleFieldsWithModifiers() {
      String sourceCode =
          """
          public class TestClass {
              public String publicField;
              protected String protectedField;
              private String privateField;
              String packagePrivateField;
              private final String finalField = "constant";
              private static String staticField;
              private transient String transientField;
              private volatile String volatileField;
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
      TSNode classDeclarationNode =
          classService.getAllClassDeclarations(tsFile).get(0).get("classDeclaration");

      Optional<TSNode> publicResult =
          service.findClassFieldNodeByName(tsFile, "publicField", classDeclarationNode);
      Optional<TSNode> finalResult =
          service.findClassFieldNodeByName(tsFile, "finalField", classDeclarationNode);
      Optional<TSNode> volatileResult =
          service.findClassFieldNodeByName(tsFile, "volatileField", classDeclarationNode);

      assertTrue(publicResult.isPresent());
      assertTrue(finalResult.isPresent());
      assertTrue(volatileResult.isPresent());

      String publicFieldText = tsFile.getTextFromNode(publicResult.get());
      String finalFieldText = tsFile.getTextFromNode(finalResult.get());
      String volatileFieldText = tsFile.getTextFromNode(volatileResult.get());

      assertTrue(publicFieldText.contains("public String publicField"));
      assertTrue(finalFieldText.contains("private final String finalField"));
      assertTrue(volatileFieldText.contains("private volatile String volatileField"));
    }
  }

  @Nested
  @DisplayName("findClassFieldNodesByType Tests")
  class FindClassFieldNodesByTypeTests {

    @Test
    @DisplayName("Should find fields by primitive type")
    void shouldFindFieldsByPrimitiveType() {
      String sourceCode =
          """
          public class TestClass {
              private int age;
              private String name;
              private int count;
              private boolean active;
              private int value;
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
      TSNode classDeclarationNode =
          classService.getAllClassDeclarations(tsFile).get(0).get("classDeclaration");

      List<TSNode> intFields =
          service.findClassFieldNodesByType(tsFile, "int", classDeclarationNode);

      assertEquals(3, intFields.size());
      for (TSNode field : intFields) {
        assertEquals("field_declaration", field.getType());
        String fieldText = tsFile.getTextFromNode(field);
        assertTrue(fieldText.contains("int"));
      }
    }

    @Test
    @DisplayName("Should find fields by reference type")
    void shouldFindFieldsByReferenceType() {
      String sourceCode =
          """
          public class TestClass {
              private String name;
              private int age;
              private String title;
              private boolean active;
              private String description;
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
      TSNode classDeclarationNode =
          classService.getAllClassDeclarations(tsFile).get(0).get("classDeclaration");

      List<TSNode> stringFields =
          service.findClassFieldNodesByType(tsFile, "String", classDeclarationNode);

      assertEquals(3, stringFields.size());
      for (TSNode field : stringFields) {
        assertEquals("field_declaration", field.getType());
        String fieldText = tsFile.getTextFromNode(field);
        assertTrue(fieldText.contains("String"));
      }
    }

    @Test
    @DisplayName("Should find fields by generic type")
    void shouldFindFieldsByGenericType() {
      String sourceCode =
          """
          public class TestClass {
              private List<String> names;
              private Map<String, Integer> scores;
              private List<String> items;
              private Set<Integer> numbers;
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
      TSNode classDeclarationNode =
          classService.getAllClassDeclarations(tsFile).get(0).get("classDeclaration");

      List<TSNode> listFields =
          service.findClassFieldNodesByType(tsFile, "List<String>", classDeclarationNode);

      assertEquals(2, listFields.size());
      for (TSNode field : listFields) {
        assertEquals("field_declaration", field.getType());
        String fieldText = tsFile.getTextFromNode(field);
        assertTrue(fieldText.contains("List<String>"));
      }
    }

    @Test
    @DisplayName("Should find fields by array type")
    void shouldFindFieldsByArrayType() {
      String sourceCode =
          """
          public class TestClass {
              private int[] numbers;
              private String name;
              private int[] values;
              private String[] names;
              private int[][] matrix;
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
      TSNode classDeclarationNode =
          classService.getAllClassDeclarations(tsFile).get(0).get("classDeclaration");

      List<TSNode> intArrayFields =
          service.findClassFieldNodesByType(tsFile, "int[]", classDeclarationNode);

      assertEquals(2, intArrayFields.size());
      for (TSNode field : intArrayFields) {
        assertEquals("field_declaration", field.getType());
        String fieldText = tsFile.getTextFromNode(field);
        assertTrue(fieldText.contains("int[]") && !fieldText.contains("int[][]"));
      }
    }

    @Test
    @DisplayName("Should find fields by complex generic type")
    void shouldFindFieldsByComplexGenericType() {
      String sourceCode =
          """
          public class TestClass {
              private Map<String, List<Integer>> complexMap;
              private List<String> simpleList;
              private Map<String, List<Integer>> anotherComplexMap;
              private Map<String, Integer> simpleMap;
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
      TSNode classDeclarationNode =
          classService.getAllClassDeclarations(tsFile).get(0).get("classDeclaration");

      List<TSNode> complexFields =
          service.findClassFieldNodesByType(
              tsFile, "Map<String, List<Integer>>", classDeclarationNode);

      assertEquals(2, complexFields.size());
      for (TSNode field : complexFields) {
        assertEquals("field_declaration", field.getType());
        String fieldText = tsFile.getTextFromNode(field);
        assertTrue(fieldText.contains("Map<String, List<Integer>>"));
      }
    }

    @Test
    @DisplayName("Should find fields by custom class type")
    void shouldFindFieldsByCustomClassType() {
      String sourceCode =
          """
          public class TestClass {
              private User user;
              private String name;
              private User admin;
              private List<User> users;
              private User guest;
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
      TSNode classDeclarationNode =
          classService.getAllClassDeclarations(tsFile).get(0).get("classDeclaration");

      List<TSNode> userFields =
          service.findClassFieldNodesByType(tsFile, "User", classDeclarationNode);

      assertEquals(3, userFields.size());
      for (TSNode field : userFields) {
        assertEquals("field_declaration", field.getType());
        String fieldText = tsFile.getTextFromNode(field);
        assertTrue(fieldText.contains("User") && !fieldText.contains("List<User>"));
      }
    }

    @Test
    @DisplayName("Should return empty list when no fields match type")
    void shouldReturnEmptyListWhenNoFieldsMatchType() {
      String sourceCode =
          """
          public class TestClass {
              private String name;
              private int age;
              private boolean active;
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
      TSNode classDeclarationNode =
          classService.getAllClassDeclarations(tsFile).get(0).get("classDeclaration");

      List<TSNode> doubleFields =
          service.findClassFieldNodesByType(tsFile, "double", classDeclarationNode);

      assertTrue(doubleFields.isEmpty());
    }

    @Test
    @DisplayName("Should return empty list when class has no fields")
    void shouldReturnEmptyListWhenClassHasNoFields() {
      String sourceCode =
          """
          public class TestClass {
              public void method() {
                  System.out.println("No fields");
              }
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
      TSNode classDeclarationNode =
          classService.getAllClassDeclarations(tsFile).get(0).get("classDeclaration");

      List<TSNode> stringFields =
          service.findClassFieldNodesByType(tsFile, "String", classDeclarationNode);

      assertTrue(stringFields.isEmpty());
    }

    @Test
    @DisplayName("Should be case sensitive for type matching")
    void shouldBeCaseSensitiveForTypeMatching() {
      String sourceCode =
          """
          public class TestClass {
              private string lowercase;
              private String uppercase;
              private STRING allCaps;
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
      TSNode classDeclarationNode =
          classService.getAllClassDeclarations(tsFile).get(0).get("classDeclaration");

      List<TSNode> stringFields =
          service.findClassFieldNodesByType(tsFile, "String", classDeclarationNode);
      List<TSNode> lowercaseFields =
          service.findClassFieldNodesByType(tsFile, "string", classDeclarationNode);

      assertEquals(1, stringFields.size());
      assertEquals(1, lowercaseFields.size());
      String stringFieldText = tsFile.getTextFromNode(stringFields.get(0));
      String lowercaseFieldText = tsFile.getTextFromNode(lowercaseFields.get(0));
      assertTrue(stringFieldText.contains("String uppercase"));
      assertTrue(lowercaseFieldText.contains("string lowercase"));
    }

    @Test
    @DisplayName("Should handle fields with static modifier")
    void shouldHandleFieldsWithStaticModifier() {
      String sourceCode =
          """
          public class TestClass {
              private static String staticField;
              private String instanceField;
              private static final String CONSTANT = "value";
              private static String anotherStaticField;
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
      TSNode classDeclarationNode =
          classService.getAllClassDeclarations(tsFile).get(0).get("classDeclaration");

      List<TSNode> stringFields =
          service.findClassFieldNodesByType(tsFile, "String", classDeclarationNode);

      assertEquals(4, stringFields.size());
    }

    @Test
    @DisplayName("Should handle fields with multiple declarators of same type")
    void shouldHandleFieldsWithMultipleDeclaratorsOfSameType() {
      String sourceCode =
          """
          public class TestClass {
              private int x, y, z;
              private String name, title;
              private boolean active;
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
      TSNode classDeclarationNode =
          classService.getAllClassDeclarations(tsFile).get(0).get("classDeclaration");

      List<TSNode> intFields =
          service.findClassFieldNodesByType(tsFile, "int", classDeclarationNode);
      List<TSNode> stringFields =
          service.findClassFieldNodesByType(tsFile, "String", classDeclarationNode);

      assertEquals(1, intFields.size()); // One field declaration with multiple declarators
      assertEquals(1, stringFields.size()); // One field declaration with multiple declarators
      String intFieldText = tsFile.getTextFromNode(intFields.get(0));
      String stringFieldText = tsFile.getTextFromNode(stringFields.get(0));
      assertTrue(intFieldText.contains("int x, y, z"));
      assertTrue(stringFieldText.contains("String name, title"));
    }

    @Test
    @DisplayName("Should return empty list when tsFile is null")
    void shouldReturnEmptyListWhenTsFileIsNull() {
      String sourceCode =
          """
          public class TestClass {
              private String name;
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
      TSNode classDeclarationNode =
          classService.getAllClassDeclarations(tsFile).get(0).get("classDeclaration");

      List<TSNode> result = service.findClassFieldNodesByType(null, "String", classDeclarationNode);

      assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should return empty list when fieldDeclaratorType is null")
    void shouldReturnEmptyListWhenFieldDeclaratorTypeIsNull() {
      String sourceCode =
          """
          public class TestClass {
              private String name;
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
      TSNode classDeclarationNode =
          classService.getAllClassDeclarations(tsFile).get(0).get("classDeclaration");

      List<TSNode> result = service.findClassFieldNodesByType(tsFile, null, classDeclarationNode);

      assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should return empty list when fieldDeclaratorType is empty")
    void shouldReturnEmptyListWhenFieldDeclaratorTypeIsEmpty() {
      String sourceCode =
          """
          public class TestClass {
              private String name;
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
      TSNode classDeclarationNode =
          classService.getAllClassDeclarations(tsFile).get(0).get("classDeclaration");

      List<TSNode> result = service.findClassFieldNodesByType(tsFile, "", classDeclarationNode);

      assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should return empty list when classDeclarationNode is not class_declaration")
    void shouldReturnEmptyListWhenClassDeclarationNodeIsNotClassDeclaration() {
      String sourceCode =
          """
          public class TestClass {
              private String name;
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
      TSNode rootNode = tsFile.getTree().getRootNode();

      List<TSNode> result = service.findClassFieldNodesByType(tsFile, "String", rootNode);

      assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should find fields with wildcard generic types")
    void shouldFindFieldsWithWildcardGenericTypes() {
      String sourceCode =
          """
          public class TestClass {
              private List<?> wildcardList;
              private Map<?, String> wildcardKeyMap;
              private List<? extends Number> boundedList;
              private List<String> concreteList;
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
      TSNode classDeclarationNode =
          classService.getAllClassDeclarations(tsFile).get(0).get("classDeclaration");

      List<TSNode> wildcardFields =
          service.findClassFieldNodesByType(tsFile, "List<?>", classDeclarationNode);
      List<TSNode> boundedFields =
          service.findClassFieldNodesByType(tsFile, "List<? extends Number>", classDeclarationNode);

      assertEquals(1, wildcardFields.size());
      assertEquals(1, boundedFields.size());
      String wildcardText = tsFile.getTextFromNode(wildcardFields.get(0));
      String boundedText = tsFile.getTextFromNode(boundedFields.get(0));
      assertTrue(wildcardText.contains("List<?> wildcardList"));
      assertTrue(boundedText.contains("List<? extends Number> boundedList"));
    }

    @Test
    @DisplayName("Should handle nested generic types")
    void shouldHandleNestedGenericTypes() {
      String sourceCode =
          """
          public class TestClass {
              private List<Map<String, Integer>> nestedGeneric;
              private Map<String, List<Integer>> reverseNested;
              private List<Map<String, Integer>> anotherNested;
              private List<String> simpleGeneric;
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
      TSNode classDeclarationNode =
          classService.getAllClassDeclarations(tsFile).get(0).get("classDeclaration");

      List<TSNode> nestedFields =
          service.findClassFieldNodesByType(
              tsFile, "List<Map<String, Integer>>", classDeclarationNode);

      assertEquals(2, nestedFields.size());
      for (TSNode field : nestedFields) {
        String fieldText = tsFile.getTextFromNode(field);
        assertTrue(fieldText.contains("List<Map<String, Integer>>"));
      }
    }
  }
}

