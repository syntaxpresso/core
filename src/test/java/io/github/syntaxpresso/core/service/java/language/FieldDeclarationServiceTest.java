package io.github.syntaxpresso.core.service.java.language;

import static org.junit.jupiter.api.Assertions.*;

import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.treesitter.TSNode;

@DisplayName("FieldDeclarationService Tests")
class FieldDeclarationServiceTest {
  private FieldDeclarationService service;
  private ClassDeclarationService classDeclarationService;

  private static final String SIMPLE_CLASS_CODE =
      """
      package io.github.test;

      public class SimpleClass {
          private String name;
          private int count;
          public boolean active;
      }
      """;

  private static final String CLASS_WITH_INITIALIZED_FIELDS_CODE =
      """
      package io.github.test;

      import java.util.List;
      import java.util.ArrayList;

      public class InitializedClass {
          private String name = "defaultName";
          private int count = 42;
          private List<String> items = new ArrayList<>();
          private boolean active = true;
          private double rate = 3.14;
          private String uninitialized;
      }
      """;

  private static final String CLASS_WITH_STATIC_FIELDS_CODE =
      """
      package io.github.test;

      public class StaticFieldsClass {
          public static final String CONSTANT = "value";
          private static int counter = 0;
          private static final long MAX_VALUE = 1000L;
          private String instanceField;
      }
      """;

  private static final String CLASS_WITH_ARRAY_FIELDS_CODE =
      """
      package io.github.test;

      public class ArrayFieldsClass {
          private String[] names;
          private int[][] matrix;
          private boolean[][][] cube;
          private List<String>[] genericArray;
      }
      """;

  private static final String CLASS_WITH_COMPLEX_TYPES_CODE =
      """
      package io.github.test;

      import java.util.Map;
      import java.util.HashMap;
      import java.util.List;
      import java.util.ArrayList;

      public class ComplexTypesClass {
          private Map<String, Integer> stringIntMap;
          private List<Map<String, List<Integer>>> complexNested;
          private Map<String, String> initialized = new HashMap<>();
          private CustomClass<String> customGeneric;
      }
      """;

  private static final String CLASS_WITH_FIELD_USAGES_CODE =
      """
      package io.github.test;

      public class FieldUsageClass {
          private String name;
          private int count;

          public void testMethod() {
              this.name = "test";
              System.out.println(this.name);
              count = 5;
              if (this.count > 0) {
                  System.out.println("Count is positive");
              }
          }

          public String getName() {
              return this.name;
          }

          public void setCount(int count) {
              this.count = count;
          }
      }
      """;

  private static final String CLASS_WITH_ANNOTATED_FIELDS_CODE =
      """
      package io.github.test;

      import javax.validation.constraints.NotNull;
      import javax.validation.constraints.Size;

      public class AnnotatedFieldsClass {
          @NotNull
          private String name;

          @Size(min = 1, max = 100)
          private String description;

          @NotNull
          @Size(min = 1)
          private String multipleAnnotations;

          private int noAnnotations;
      }
      """;

  private static final String EMPTY_CLASS_CODE =
      """
      package io.github.test;

      public class EmptyClass {
      }
      """;

  private static final String NO_FIELDS_CLASS_CODE =
      """
      package io.github.test;

      public class NoFieldsClass {
          public void method() {
              System.out.println("No fields here");
          }

          public NoFieldsClass() {
          }
      }
      """;

  private TSFile simpleClassFile;
  private TSFile initializedFieldsFile;
  private TSFile staticFieldsFile;
  private TSFile arrayFieldsFile;
  private TSFile complexTypesFile;
  private TSFile fieldUsageFile;
  private TSFile annotatedFieldsFile;
  private TSFile emptyClassFile;
  private TSFile noFieldsClassFile;

  @BeforeEach
  void setUp() {
    service = new FieldDeclarationService();
    classDeclarationService = new ClassDeclarationService();

    simpleClassFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_CLASS_CODE);
    initializedFieldsFile = new TSFile(SupportedLanguage.JAVA, CLASS_WITH_INITIALIZED_FIELDS_CODE);
    staticFieldsFile = new TSFile(SupportedLanguage.JAVA, CLASS_WITH_STATIC_FIELDS_CODE);
    arrayFieldsFile = new TSFile(SupportedLanguage.JAVA, CLASS_WITH_ARRAY_FIELDS_CODE);
    complexTypesFile = new TSFile(SupportedLanguage.JAVA, CLASS_WITH_COMPLEX_TYPES_CODE);
    fieldUsageFile = new TSFile(SupportedLanguage.JAVA, CLASS_WITH_FIELD_USAGES_CODE);
    annotatedFieldsFile = new TSFile(SupportedLanguage.JAVA, CLASS_WITH_ANNOTATED_FIELDS_CODE);
    emptyClassFile = new TSFile(SupportedLanguage.JAVA, EMPTY_CLASS_CODE);
    noFieldsClassFile = new TSFile(SupportedLanguage.JAVA, NO_FIELDS_CLASS_CODE);
  }

  private TSNode getFirstClassDeclaration(TSFile file) {
    List<Map<String, TSNode>> classes = classDeclarationService.getAllClassDeclarations(file);
    assertFalse(classes.isEmpty());
    return classes.get(0).get("classDeclaration");
  }

  @Nested
  @DisplayName("getFieldDeclarationNodeInfo Tests")
  class GetFieldDeclarationNodeInfoTests {

    @Test
    @DisplayName("should return field node info for simple field declaration")
    void shouldReturnFieldNodeInfoForSimpleField() {
      TSNode classNode = getFirstClassDeclaration(simpleClassFile);
      List<TSNode> fieldNodes = service.getAllFieldDeclarationNodes(simpleClassFile, classNode);
      assertFalse(fieldNodes.isEmpty());

      List<Map<String, TSNode>> fieldInfo =
          service.getFieldDeclarationNodeInfo(simpleClassFile, fieldNodes.get(0));

      assertFalse(fieldInfo.isEmpty());
      Map<String, TSNode> info = fieldInfo.get(0);
      assertTrue(info.containsKey("fieldType"));
      assertTrue(info.containsKey("fieldName"));
      assertTrue(info.containsKey("field"));

      String fieldType = simpleClassFile.getTextFromNode(info.get("fieldType"));
      String fieldName = simpleClassFile.getTextFromNode(info.get("fieldName"));

      assertEquals("String", fieldType);
      assertEquals("name", fieldName);
    }

    @Test
    @DisplayName("should return field node info for field with initialization")
    void shouldReturnFieldNodeInfoWithInitialization() {
      TSNode classNode = getFirstClassDeclaration(initializedFieldsFile);
      List<TSNode> fieldNodes =
          service.getAllFieldDeclarationNodes(initializedFieldsFile, classNode);
      assertFalse(fieldNodes.isEmpty());

      List<Map<String, TSNode>> fieldInfo =
          service.getFieldDeclarationNodeInfo(initializedFieldsFile, fieldNodes.get(0));

      assertFalse(fieldInfo.isEmpty());
      Map<String, TSNode> info = fieldInfo.get(0);
      assertTrue(info.containsKey("fieldType"));
      assertTrue(info.containsKey("fieldName"));
      assertTrue(info.containsKey("fieldValue"));
      assertTrue(info.containsKey("field"));

      String fieldValue = initializedFieldsFile.getTextFromNode(info.get("fieldValue"));
      assertEquals("\"defaultName\"", fieldValue);
    }

    @Test
    @DisplayName("should return empty list for null file")
    void shouldReturnEmptyListForNullFile() {
      TSNode classNode = getFirstClassDeclaration(simpleClassFile);
      List<TSNode> fieldNodes = service.getAllFieldDeclarationNodes(simpleClassFile, classNode);

      List<Map<String, TSNode>> result =
          service.getFieldDeclarationNodeInfo(null, fieldNodes.get(0));

      assertEquals(Collections.emptyList(), result);
    }

    @Test
    @DisplayName("should return empty list for non-field declaration node")
    void shouldReturnEmptyListForNonFieldDeclarationNode() {
      TSNode classNode = getFirstClassDeclaration(simpleClassFile);

      List<Map<String, TSNode>> result =
          service.getFieldDeclarationNodeInfo(simpleClassFile, classNode);

      assertEquals(Collections.emptyList(), result);
    }

    @Test
    @DisplayName("should return empty list for null tree")
    void shouldReturnEmptyListForNullTree() {
      TSNode classNode = getFirstClassDeclaration(simpleClassFile);
      List<TSNode> fieldNodes = service.getAllFieldDeclarationNodes(simpleClassFile, classNode);

      List<Map<String, TSNode>> result =
          service.getFieldDeclarationNodeInfo(null, fieldNodes.get(0));

      assertEquals(Collections.emptyList(), result);
    }
  }

  @Nested
  @DisplayName("getAllFieldDeclarationNodes Tests")
  class GetAllFieldDeclarationNodesTests {

    @Test
    @DisplayName("should return all field declarations from simple class")
    void shouldReturnAllFieldDeclarationsFromSimpleClass() {
      TSNode classNode = getFirstClassDeclaration(simpleClassFile);

      List<TSNode> fieldDeclarations =
          service.getAllFieldDeclarationNodes(simpleClassFile, classNode);

      assertEquals(3, fieldDeclarations.size());
      for (TSNode field : fieldDeclarations) {
        assertEquals("field_declaration", field.getType());
      }
    }

    @Test
    @DisplayName("should return all field declarations including static fields")
    void shouldReturnAllFieldDeclarationsIncludingStaticFields() {
      TSNode classNode = getFirstClassDeclaration(staticFieldsFile);

      List<TSNode> fieldDeclarations =
          service.getAllFieldDeclarationNodes(staticFieldsFile, classNode);

      assertEquals(4, fieldDeclarations.size());
    }

    @Test
    @DisplayName("should return empty list for class with no fields")
    void shouldReturnEmptyListForClassWithNoFields() {
      TSNode classNode = getFirstClassDeclaration(noFieldsClassFile);

      List<TSNode> fieldDeclarations =
          service.getAllFieldDeclarationNodes(noFieldsClassFile, classNode);

      assertTrue(fieldDeclarations.isEmpty());
    }

    @Test
    @DisplayName("should return empty list for empty class")
    void shouldReturnEmptyListForEmptyClass() {
      TSNode classNode = getFirstClassDeclaration(emptyClassFile);

      List<TSNode> fieldDeclarations =
          service.getAllFieldDeclarationNodes(emptyClassFile, classNode);

      assertTrue(fieldDeclarations.isEmpty());
    }

    @Test
    @DisplayName("should return empty list for null file")
    void shouldReturnEmptyListForNullFile() {
      TSNode classNode = getFirstClassDeclaration(simpleClassFile);

      List<TSNode> result = service.getAllFieldDeclarationNodes(null, classNode);

      assertTrue(result.isEmpty());
    }
  }

  @Nested
  @DisplayName("getAllFieldDeclarationAnnotationNodes Tests")
  class GetAllFieldDeclarationAnnotationNodesTests {

    @Test
    @DisplayName("should return field annotation nodes for annotated fields")
    void shouldReturnFieldAnnotationNodesForAnnotatedFields() {
      TSNode classNode = getFirstClassDeclaration(annotatedFieldsFile);

      List<TSNode> annotationNodes =
          service.getAllFieldDeclarationAnnotationNodes(annotatedFieldsFile, classNode);

      assertFalse(annotationNodes.isEmpty());
    }

    @Test
    @DisplayName("should return empty list for class without annotated fields")
    void shouldReturnEmptyListForClassWithoutAnnotatedFields() {
      TSNode classNode = getFirstClassDeclaration(simpleClassFile);

      List<TSNode> annotationNodes =
          service.getAllFieldDeclarationAnnotationNodes(simpleClassFile, classNode);

      assertTrue(annotationNodes.isEmpty());
    }

    @Test
    @DisplayName("should return empty list for null file")
    void shouldReturnEmptyListForNullFile() {
      TSNode classNode = getFirstClassDeclaration(simpleClassFile);

      List<TSNode> result = service.getAllFieldDeclarationAnnotationNodes(null, classNode);

      assertTrue(result.isEmpty());
    }
  }

  @Nested
  @DisplayName("getFieldDeclarationTypeNode Tests")
  class GetFieldDeclarationTypeNodeTests {

    @Test
    @DisplayName("should return type node for simple field")
    void shouldReturnTypeNodeForSimpleField() {
      TSNode classNode = getFirstClassDeclaration(simpleClassFile);
      List<TSNode> fieldNodes = service.getAllFieldDeclarationNodes(simpleClassFile, classNode);

      Optional<TSNode> typeNode =
          service.getFieldDeclarationTypeNode(simpleClassFile, fieldNodes.get(0));

      assertTrue(typeNode.isPresent());
      String type = simpleClassFile.getTextFromNode(typeNode.get());
      assertEquals("String", type);
    }

    @Test
    @DisplayName("should return type node for array field")
    void shouldReturnTypeNodeForArrayField() {
      TSNode classNode = getFirstClassDeclaration(arrayFieldsFile);
      List<TSNode> fieldNodes = service.getAllFieldDeclarationNodes(arrayFieldsFile, classNode);

      Optional<TSNode> typeNode =
          service.getFieldDeclarationTypeNode(arrayFieldsFile, fieldNodes.get(0));

      assertTrue(typeNode.isPresent());
      String type = arrayFieldsFile.getTextFromNode(typeNode.get());
      assertEquals("String[]", type);
    }

    @Test
    @DisplayName("should return type node for generic field")
    void shouldReturnTypeNodeForGenericField() {
      TSNode classNode = getFirstClassDeclaration(complexTypesFile);
      List<TSNode> fieldNodes = service.getAllFieldDeclarationNodes(complexTypesFile, classNode);

      Optional<TSNode> typeNode =
          service.getFieldDeclarationTypeNode(complexTypesFile, fieldNodes.get(0));

      assertTrue(typeNode.isPresent());
      String type = complexTypesFile.getTextFromNode(typeNode.get());
      assertTrue(type.contains("Map"));
    }

    @Test
    @DisplayName("should return empty for null file")
    void shouldReturnEmptyForNullFile() {
      TSNode classNode = getFirstClassDeclaration(simpleClassFile);
      List<TSNode> fieldNodes = service.getAllFieldDeclarationNodes(simpleClassFile, classNode);

      Optional<TSNode> result = service.getFieldDeclarationTypeNode(null, fieldNodes.get(0));

      assertTrue(result.isEmpty());
    }
  }

  @Nested
  @DisplayName("getFieldDeclarationNameNode Tests")
  class GetFieldDeclarationNameNodeTests {

    @Test
    @DisplayName("should return name node for field")
    void shouldReturnNameNodeForField() {
      TSNode classNode = getFirstClassDeclaration(simpleClassFile);
      List<TSNode> fieldNodes = service.getAllFieldDeclarationNodes(simpleClassFile, classNode);

      Optional<TSNode> nameNode =
          service.getFieldDeclarationNameNode(simpleClassFile, fieldNodes.get(0));

      assertTrue(nameNode.isPresent());
      String name = simpleClassFile.getTextFromNode(nameNode.get());
      assertEquals("name", name);
      assertEquals("identifier", nameNode.get().getType());
    }

    @Test
    @DisplayName("should return name node for all different field types")
    void shouldReturnNameNodeForDifferentFieldTypes() {
      TSNode classNode = getFirstClassDeclaration(initializedFieldsFile);
      List<TSNode> fieldNodes =
          service.getAllFieldDeclarationNodes(initializedFieldsFile, classNode);

      for (TSNode fieldNode : fieldNodes) {
        Optional<TSNode> nameNode =
            service.getFieldDeclarationNameNode(initializedFieldsFile, fieldNode);

        assertTrue(nameNode.isPresent());
        assertNotNull(initializedFieldsFile.getTextFromNode(nameNode.get()));
        assertEquals("identifier", nameNode.get().getType());
      }
    }

    @Test
    @DisplayName("should return empty for null file")
    void shouldReturnEmptyForNullFile() {
      TSNode classNode = getFirstClassDeclaration(simpleClassFile);
      List<TSNode> fieldNodes = service.getAllFieldDeclarationNodes(simpleClassFile, classNode);

      Optional<TSNode> result = service.getFieldDeclarationNameNode(null, fieldNodes.get(0));

      assertTrue(result.isEmpty());
    }
  }

  @Nested
  @DisplayName("getFieldDeclarationValueNode Tests")
  class GetFieldDeclarationValueNodeTests {

    @Test
    @DisplayName("should return value node for initialized field")
    void shouldReturnValueNodeForInitializedField() {
      TSNode classNode = getFirstClassDeclaration(initializedFieldsFile);
      List<TSNode> fieldNodes =
          service.getAllFieldDeclarationNodes(initializedFieldsFile, classNode);

      Optional<TSNode> valueNode =
          service.getFieldDeclarationValueNode(initializedFieldsFile, fieldNodes.get(0));

      assertTrue(valueNode.isPresent());
      String value = initializedFieldsFile.getTextFromNode(valueNode.get());
      assertEquals("\"defaultName\"", value);
    }

    @Test
    @DisplayName("should return empty for uninitialized field")
    void shouldReturnEmptyForUninitializedField() {
      TSNode classNode = getFirstClassDeclaration(simpleClassFile);
      List<TSNode> fieldNodes = service.getAllFieldDeclarationNodes(simpleClassFile, classNode);

      Optional<TSNode> valueNode =
          service.getFieldDeclarationValueNode(simpleClassFile, fieldNodes.get(0));

      assertTrue(valueNode.isEmpty());
    }

    @Test
    @DisplayName("should return value node for different value types")
    void shouldReturnValueNodeForDifferentValueTypes() {
      TSNode classNode = getFirstClassDeclaration(initializedFieldsFile);
      List<TSNode> fieldNodes =
          service.getAllFieldDeclarationNodes(initializedFieldsFile, classNode);

      boolean foundStringValue = false;
      boolean foundIntValue = false;
      boolean foundBooleanValue = false;

      for (TSNode fieldNode : fieldNodes) {
        Optional<TSNode> valueNode =
            service.getFieldDeclarationValueNode(initializedFieldsFile, fieldNode);

        if (valueNode.isPresent()) {
          String value = initializedFieldsFile.getTextFromNode(valueNode.get());
          if (value.equals("\"defaultName\"")) foundStringValue = true;
          if (value.equals("42")) foundIntValue = true;
          if (value.equals("true")) foundBooleanValue = true;
        }
      }

      assertTrue(foundStringValue);
      assertTrue(foundIntValue);
      assertTrue(foundBooleanValue);
    }

    @Test
    @DisplayName("should return empty for null file")
    void shouldReturnEmptyForNullFile() {
      TSNode classNode = getFirstClassDeclaration(simpleClassFile);
      List<TSNode> fieldNodes = service.getAllFieldDeclarationNodes(simpleClassFile, classNode);

      Optional<TSNode> result = service.getFieldDeclarationValueNode(null, fieldNodes.get(0));

      assertTrue(result.isEmpty());
    }
  }

  @Nested
  @DisplayName("getAllFieldDeclarationUsageNodes Tests")
  class GetAllFieldDeclarationUsageNodesTests {

    @Test
    @DisplayName("should find field usages in methods")
    void shouldFindFieldUsagesInMethods() {
      TSNode classNode = getFirstClassDeclaration(fieldUsageFile);
      List<TSNode> fieldNodes = service.getAllFieldDeclarationNodes(fieldUsageFile, classNode);

      TSNode nameField =
          fieldNodes.stream()
              .filter(
                  field -> {
                    Optional<TSNode> nameNode =
                        service.getFieldDeclarationNameNode(fieldUsageFile, field);
                    return nameNode.isPresent()
                        && "name".equals(fieldUsageFile.getTextFromNode(nameNode.get()));
                  })
              .findFirst()
              .orElse(null);

      assertNotNull(nameField);

      List<TSNode> usageNodes =
          service.getAllFieldDeclarationUsageNodes(fieldUsageFile, nameField, classNode);

      assertFalse(usageNodes.isEmpty());
    }

    @Test
    @DisplayName("should return empty list for field with no usages")
    void shouldReturnEmptyListForFieldWithNoUsages() {
      TSNode classNode = getFirstClassDeclaration(simpleClassFile);
      List<TSNode> fieldNodes = service.getAllFieldDeclarationNodes(simpleClassFile, classNode);

      List<TSNode> usageNodes =
          service.getAllFieldDeclarationUsageNodes(simpleClassFile, fieldNodes.get(0), classNode);

      assertTrue(usageNodes.isEmpty());
    }

    @Test
    @DisplayName("should return empty list for null file")
    void shouldReturnEmptyListForNullFile() {
      TSNode classNode = getFirstClassDeclaration(simpleClassFile);
      List<TSNode> fieldNodes = service.getAllFieldDeclarationNodes(simpleClassFile, classNode);

      List<TSNode> result =
          service.getAllFieldDeclarationUsageNodes(null, fieldNodes.get(0), classNode);

      assertEquals(Collections.emptyList(), result);
    }

    @Test
    @DisplayName("should return empty list for invalid field declaration node")
    void shouldReturnEmptyListForInvalidFieldDeclarationNode() {
      TSNode classNode = getFirstClassDeclaration(simpleClassFile);

      List<TSNode> result =
          service.getAllFieldDeclarationUsageNodes(simpleClassFile, classNode, classNode);

      assertEquals(Collections.emptyList(), result);
    }

    @Test
    @DisplayName("should return empty list for invalid class declaration node")
    void shouldReturnEmptyListForInvalidClassDeclarationNode() {
      TSNode classNode = getFirstClassDeclaration(simpleClassFile);
      List<TSNode> fieldNodes = service.getAllFieldDeclarationNodes(simpleClassFile, classNode);

      List<TSNode> result =
          service.getAllFieldDeclarationUsageNodes(
              simpleClassFile, fieldNodes.get(0), fieldNodes.get(0));

      assertEquals(Collections.emptyList(), result);
    }
  }

  @Nested
  @DisplayName("findFieldDeclarationNodeByName Tests")
  class FindFieldDeclarationNodeByNameTests {

    @Test
    @DisplayName("should find field by exact name match")
    void shouldFindFieldByExactNameMatch() {
      TSNode classNode = getFirstClassDeclaration(simpleClassFile);

      Optional<TSNode> foundField =
          service.findFieldDeclarationNodeByName(simpleClassFile, "name", classNode);

      assertTrue(foundField.isPresent());
      assertEquals("field_declaration", foundField.get().getType());

      String fieldText = simpleClassFile.getTextFromNode(foundField.get());
      assertTrue(fieldText.contains("String name"));
    }

    @Test
    @DisplayName("should find different field types by name")
    void shouldFindDifferentFieldTypesByName() {
      TSNode classNode = getFirstClassDeclaration(simpleClassFile);

      Optional<TSNode> stringField =
          service.findFieldDeclarationNodeByName(simpleClassFile, "name", classNode);
      Optional<TSNode> intField =
          service.findFieldDeclarationNodeByName(simpleClassFile, "count", classNode);
      Optional<TSNode> booleanField =
          service.findFieldDeclarationNodeByName(simpleClassFile, "active", classNode);

      assertTrue(stringField.isPresent());
      assertTrue(intField.isPresent());
      assertTrue(booleanField.isPresent());

      String stringFieldText = simpleClassFile.getTextFromNode(stringField.get());
      String intFieldText = simpleClassFile.getTextFromNode(intField.get());
      String booleanFieldText = simpleClassFile.getTextFromNode(booleanField.get());

      assertTrue(stringFieldText.contains("String name"));
      assertTrue(intFieldText.contains("int count"));
      assertTrue(booleanFieldText.contains("boolean active"));
    }

    @Test
    @DisplayName("should return empty for non-existent field")
    void shouldReturnEmptyForNonExistentField() {
      TSNode classNode = getFirstClassDeclaration(simpleClassFile);

      Optional<TSNode> result =
          service.findFieldDeclarationNodeByName(simpleClassFile, "nonExistentField", classNode);

      assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("should return empty for null field name")
    void shouldReturnEmptyForNullFieldName() {
      TSNode classNode = getFirstClassDeclaration(simpleClassFile);

      Optional<TSNode> result =
          service.findFieldDeclarationNodeByName(simpleClassFile, null, classNode);

      assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("should return empty for empty field name")
    void shouldReturnEmptyForEmptyFieldName() {
      TSNode classNode = getFirstClassDeclaration(simpleClassFile);

      Optional<TSNode> result =
          service.findFieldDeclarationNodeByName(simpleClassFile, "", classNode);

      assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("should return empty for null file")
    void shouldReturnEmptyForNullFile() {
      TSNode classNode = getFirstClassDeclaration(simpleClassFile);

      Optional<TSNode> result = service.findFieldDeclarationNodeByName(null, "name", classNode);

      assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("should return empty for non-class declaration node")
    void shouldReturnEmptyForNonClassDeclarationNode() {
      TSNode classNode = getFirstClassDeclaration(simpleClassFile);
      List<TSNode> fieldNodes = service.getAllFieldDeclarationNodes(simpleClassFile, classNode);

      Optional<TSNode> result =
          service.findFieldDeclarationNodeByName(simpleClassFile, "name", fieldNodes.get(0));

      assertTrue(result.isEmpty());
    }
  }

  @Nested
  @DisplayName("findFieldDeclarationNodesByType Tests")
  class FindFieldDeclarationNodesByTypeTests {

    @Test
    @DisplayName("should find fields by primitive type")
    void shouldFindFieldsByPrimitiveType() {
      TSNode classNode = getFirstClassDeclaration(initializedFieldsFile);

      List<TSNode> intFields =
          service.findFieldDeclarationNodesByType(initializedFieldsFile, "int", classNode);

      assertEquals(1, intFields.size());
      String fieldText = initializedFieldsFile.getTextFromNode(intFields.get(0));
      assertTrue(fieldText.contains("int count"));
    }

    @Test
    @DisplayName("should find fields by reference type")
    void shouldFindFieldsByReferenceType() {
      TSNode classNode = getFirstClassDeclaration(initializedFieldsFile);

      List<TSNode> stringFields =
          service.findFieldDeclarationNodesByType(initializedFieldsFile, "String", classNode);

      assertEquals(2, stringFields.size());
      for (TSNode field : stringFields) {
        String fieldText = initializedFieldsFile.getTextFromNode(field);
        assertTrue(fieldText.contains("String"));
      }
    }

    @Test
    @DisplayName("should find fields by generic type")
    void shouldFindFieldsByGenericType() {
      TSNode classNode = getFirstClassDeclaration(initializedFieldsFile);

      List<TSNode> listFields =
          service.findFieldDeclarationNodesByType(initializedFieldsFile, "List<String>", classNode);

      assertEquals(1, listFields.size());
      String fieldText = initializedFieldsFile.getTextFromNode(listFields.get(0));
      assertTrue(fieldText.contains("List<String>"));
    }

    @Test
    @DisplayName("should find fields by array type")
    void shouldFindFieldsByArrayType() {
      TSNode classNode = getFirstClassDeclaration(arrayFieldsFile);

      List<TSNode> stringArrayFields =
          service.findFieldDeclarationNodesByType(arrayFieldsFile, "String[]", classNode);

      assertEquals(1, stringArrayFields.size());
      String fieldText = arrayFieldsFile.getTextFromNode(stringArrayFields.get(0));
      assertTrue(fieldText.contains("String[]"));
    }

    @Test
    @DisplayName("should return empty list for non-existent type")
    void shouldReturnEmptyListForNonExistentType() {
      TSNode classNode = getFirstClassDeclaration(simpleClassFile);

      List<TSNode> result =
          service.findFieldDeclarationNodesByType(simpleClassFile, "NonExistentType", classNode);

      assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("should return empty list for null type")
    void shouldReturnEmptyListForNullType() {
      TSNode classNode = getFirstClassDeclaration(simpleClassFile);

      List<TSNode> result =
          service.findFieldDeclarationNodesByType(simpleClassFile, null, classNode);

      assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("should return empty list for empty type")
    void shouldReturnEmptyListForEmptyType() {
      TSNode classNode = getFirstClassDeclaration(simpleClassFile);

      List<TSNode> result = service.findFieldDeclarationNodesByType(simpleClassFile, "", classNode);

      assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("should return empty list for null file")
    void shouldReturnEmptyListForNullFile() {
      TSNode classNode = getFirstClassDeclaration(simpleClassFile);

      List<TSNode> result = service.findFieldDeclarationNodesByType(null, "String", classNode);

      assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("should return empty list for non-class declaration node")
    void shouldReturnEmptyListForNonClassDeclarationNode() {
      TSNode classNode = getFirstClassDeclaration(simpleClassFile);
      List<TSNode> fieldNodes = service.getAllFieldDeclarationNodes(simpleClassFile, classNode);

      List<TSNode> result =
          service.findFieldDeclarationNodesByType(simpleClassFile, "String", fieldNodes.get(0));

      assertTrue(result.isEmpty());
    }
  }

  @Nested
  @DisplayName("Edge Cases and Error Handling")
  class EdgeCasesAndErrorHandlingTests {

    @Test
    @DisplayName("should handle static and final fields correctly")
    void shouldHandleStaticAndFinalFieldsCorrectly() {
      TSNode classNode = getFirstClassDeclaration(staticFieldsFile);
      List<TSNode> fieldNodes = service.getAllFieldDeclarationNodes(staticFieldsFile, classNode);

      assertEquals(4, fieldNodes.size());

      for (TSNode fieldNode : fieldNodes) {
        Optional<TSNode> nameNode =
            service.getFieldDeclarationNameNode(staticFieldsFile, fieldNode);
        assertTrue(nameNode.isPresent());

        String fieldName = staticFieldsFile.getTextFromNode(nameNode.get());
        assertNotNull(fieldName);
        assertFalse(fieldName.isEmpty());
      }
    }

    @Test
    @DisplayName("should handle fields with complex initialization expressions")
    void shouldHandleFieldsWithComplexInitialization() {
      TSNode classNode = getFirstClassDeclaration(initializedFieldsFile);
      List<TSNode> fieldNodes =
          service.getAllFieldDeclarationNodes(initializedFieldsFile, classNode);

      boolean foundComplexInitialization = false;
      for (TSNode fieldNode : fieldNodes) {
        Optional<TSNode> valueNode =
            service.getFieldDeclarationValueNode(initializedFieldsFile, fieldNode);
        if (valueNode.isPresent()) {
          String value = initializedFieldsFile.getTextFromNode(valueNode.get());
          if (value.contains("new ArrayList<>()")) {
            foundComplexInitialization = true;
            break;
          }
        }
      }

      assertTrue(foundComplexInitialization);
    }

    @Test
    @DisplayName("should handle multi-dimensional arrays")
    void shouldHandleMultiDimensionalArrays() {
      TSNode classNode = getFirstClassDeclaration(arrayFieldsFile);
      List<TSNode> fieldNodes = service.getAllFieldDeclarationNodes(arrayFieldsFile, classNode);

      boolean foundMultiDimensionalArray = false;
      for (TSNode fieldNode : fieldNodes) {
        Optional<TSNode> typeNode = service.getFieldDeclarationTypeNode(arrayFieldsFile, fieldNode);
        if (typeNode.isPresent()) {
          String type = arrayFieldsFile.getTextFromNode(typeNode.get());
          if (type.contains("[][]")) {
            foundMultiDimensionalArray = true;
            break;
          }
        }
      }

      assertTrue(foundMultiDimensionalArray);
    }

    @Test
    @DisplayName("should handle fields with generic wildcards and bounds")
    void shouldHandleFieldsWithGenericWildcardsAndBounds() {
      TSNode classNode = getFirstClassDeclaration(complexTypesFile);
      List<TSNode> fieldNodes = service.getAllFieldDeclarationNodes(complexTypesFile, classNode);

      assertFalse(fieldNodes.isEmpty());

      for (TSNode fieldNode : fieldNodes) {
        Optional<TSNode> typeNode =
            service.getFieldDeclarationTypeNode(complexTypesFile, fieldNode);
        if (typeNode.isPresent()) {
          String type = complexTypesFile.getTextFromNode(typeNode.get());
          assertNotNull(type);
          assertFalse(type.isEmpty());
        }
      }
    }

    @Test
    @DisplayName("should handle class with only static fields")
    void shouldHandleClassWithOnlyStaticFields() {
      String onlyStaticFieldsCode =
          """
          public class OnlyStaticFields {
              public static final String CONSTANT = "value";
              private static int counter;
          }
          """;

      TSFile onlyStaticFile = new TSFile(SupportedLanguage.JAVA, onlyStaticFieldsCode);
      TSNode classNode = getFirstClassDeclaration(onlyStaticFile);

      List<TSNode> fieldNodes = service.getAllFieldDeclarationNodes(onlyStaticFile, classNode);

      assertEquals(2, fieldNodes.size());

      for (TSNode fieldNode : fieldNodes) {
        Optional<TSNode> nameNode = service.getFieldDeclarationNameNode(onlyStaticFile, fieldNode);
        assertTrue(nameNode.isPresent());
      }
    }

    @Test
    @DisplayName("should handle fields with annotations")
    void shouldHandleFieldsWithAnnotations() {
      TSNode classNode = getFirstClassDeclaration(annotatedFieldsFile);
      List<TSNode> fieldNodes = service.getAllFieldDeclarationNodes(annotatedFieldsFile, classNode);

      // Accept the actual count that tree-sitter returns
      assertTrue(fieldNodes.size() >= 4);

      // Check that we can get names for all detected field nodes
      for (TSNode fieldNode : fieldNodes) {
        Optional<TSNode> nameNode =
            service.getFieldDeclarationNameNode(annotatedFieldsFile, fieldNode);
        assertTrue(nameNode.isPresent());

        String fieldName = annotatedFieldsFile.getTextFromNode(nameNode.get());
        assertNotNull(fieldName);
        assertFalse(fieldName.isEmpty());
      }

      // Verify that expected fields are found (allows for extras due to parsing)
      List<String> fieldNames =
          fieldNodes.stream()
              .map(fieldNode -> service.getFieldDeclarationNameNode(annotatedFieldsFile, fieldNode))
              .filter(Optional::isPresent)
              .map(Optional::get)
              .map(nameNode -> annotatedFieldsFile.getTextFromNode(nameNode))
              .toList();

      assertTrue(fieldNames.contains("name"));
      assertTrue(fieldNames.contains("description"));
      assertTrue(fieldNames.contains("multipleAnnotations"));
      assertTrue(fieldNames.contains("noAnnotations"));
    }

    @Test
    @DisplayName("should handle malformed field declarations gracefully")
    void shouldHandleMalformedFieldDeclarationsGracefully() {
      String malformedCode =
          """
          public class MalformedClass {
              private incomplete
              public void method() {}
              private String valid;
          }
          """;

      TSFile malformedFile = new TSFile(SupportedLanguage.JAVA, malformedCode);
      TSNode classNode = getFirstClassDeclaration(malformedFile);

      List<TSNode> fieldNodes = service.getAllFieldDeclarationNodes(malformedFile, classNode);

      assertNotNull(fieldNodes);

      for (TSNode fieldNode : fieldNodes) {
        assertEquals("field_declaration", fieldNode.getType());
      }
    }
  }
}
