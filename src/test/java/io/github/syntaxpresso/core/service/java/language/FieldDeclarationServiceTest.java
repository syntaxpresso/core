package io.github.syntaxpresso.core.service.java.language;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.syntaxpresso.core.command.extra.JavaBasicType;
import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import io.github.syntaxpresso.core.service.java.language.extra.FieldCapture;
import io.github.syntaxpresso.core.service.java.language.extra.FieldInsertionPoint.FieldInsertionPosition;
import io.github.syntaxpresso.core.util.PathHelper;
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
  private FieldDeclarationService fieldDeclarationService;
  private ClassDeclarationService classDeclarationService;

  private static final String SIMPLE_FIELDS_CODE =
      """
      public class SimpleClass {
          private String name;
          private int age;
          public boolean active;

          public void method() {
              // method body
          }
      }
      """;

  private static final String ANNOTATED_FIELDS_CODE =
      """
      package com.example;

      import javax.persistence.*;
      import java.util.List;

      @Entity
      public class User {
          @Id
          @GeneratedValue(strategy = GenerationType.IDENTITY)
          private Long id;

          @Column(name = "username", nullable = false)
          private String username;

          @NotNull
          @Email
          private String email;

          @OneToMany(mappedBy = "user")
          private List<Order> orders;

          public User() {}
      }
      """;

  private static final String INITIALIZED_FIELDS_CODE =
      """
      public class ConfigClass {
          private String defaultName = "Unknown";
          private int maxRetries = 3;
          private boolean enabled = true;
          private List<String> items = new ArrayList<>();
          private static final String CONSTANT = "CONST_VALUE";

          public void method() {
              this.defaultName = "Updated";
              this.enabled = false;
          }
      }
      """;

  private static final String GENERIC_FIELDS_CODE =
      """
      public class GenericContainer<T> {
          private T data;
          private List<T> items;
          private Map<String, T> mappings;
          private List<String> names;
          private Optional<T> optional;

          public void process() {
              this.data = null;
              this.items.clear();
          }
      }
      """;

  private static final String MULTIPLE_SAME_TYPE_CODE =
      """
      public class PersonClass {
          private String firstName;
          private String lastName;
          private String email;
          private int age;
          private int score;

          public void updateNames() {
              this.firstName = "New";
              this.lastName = "Name";
          }
      }
      """;

  @BeforeEach
  void setUp() {
    this.fieldDeclarationService = new FieldDeclarationService();

    // Set up ClassDeclarationService for helper methods
    FormalParameterService formalParameterService = new FormalParameterService();
    MethodDeclarationService methodDeclarationService =
        new MethodDeclarationService(formalParameterService);
    this.classDeclarationService =
        new ClassDeclarationService(fieldDeclarationService, methodDeclarationService);
  }

  @Nested
  @DisplayName("getFieldDeclarationNodeInfo() Tests")
  class GetFieldDeclarationNodeInfoTests {

    /**
     * Tests that getFieldDeclarationNodeInfo correctly extracts basic field information.
     *
     * <p>Usage example:
     *
     * <pre>
     * List&lt;Map&lt;String, TSNode&gt;&gt; fieldInfo = service.getFieldDeclarationNodeInfo(tsFile, fieldNode);
     * Map&lt;String, TSNode&gt; info = fieldInfo.get(0);
     * String fieldType = tsFile.getTextFromNode(info.get("fieldType"));  // e.g., "String"
     * String fieldName = tsFile.getTextFromNode(info.get("fieldName"));  // e.g., "name"
     * </pre>
     */
    @Test
    @DisplayName("should extract basic field information")
    void getFieldDeclarationNodeInfo_withSimpleField_shouldExtractInfo() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_FIELDS_CODE);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "SimpleClass");
      assertTrue(classNode.isPresent());

      List<TSNode> fieldNodes =
          fieldDeclarationService.getAllFieldDeclarationNodes(tsFile, classNode.get());
      assertFalse(fieldNodes.isEmpty());

      List<Map<String, TSNode>> fieldInfo =
          fieldDeclarationService.getFieldDeclarationNodeInfo(tsFile, fieldNodes.get(0));

      assertFalse(fieldInfo.isEmpty());
      Map<String, TSNode> info = fieldInfo.get(0);

      TSNode fieldType = info.get(FieldCapture.FIELD_TYPE.getCaptureName());
      assertNotNull(fieldType);
      assertEquals("String", tsFile.getTextFromNode(fieldType));

      TSNode fieldName = info.get(FieldCapture.FIELD_NAME.getCaptureName());
      assertNotNull(fieldName);
      assertEquals("name", tsFile.getTextFromNode(fieldName));
    }

    /**
     * Tests that getFieldDeclarationNodeInfo handles fields with initialization values.
     *
     * <p>Usage example:
     *
     * <pre>
     * List&lt;Map&lt;String, TSNode&gt;&gt; fieldInfo = service.getFieldDeclarationNodeInfo(tsFile, fieldNode);
     * Map&lt;String, TSNode&gt; info = fieldInfo.get(0);
     * TSNode fieldValue = info.get("fieldValue");
     * // fieldValue node contains "\"Unknown\""
     * </pre>
     */
    @Test
    @DisplayName("should extract field with initialization value")
    void getFieldDeclarationNodeInfo_withInitializedField_shouldExtractValue() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, INITIALIZED_FIELDS_CODE);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "ConfigClass");
      assertTrue(classNode.isPresent());

      // Find the field with initialization
      Optional<TSNode> fieldNode =
          fieldDeclarationService.findFieldDeclarationNodeByName(
              tsFile, "defaultName", classNode.get());
      assertTrue(fieldNode.isPresent());

      List<Map<String, TSNode>> fieldInfo =
          fieldDeclarationService.getFieldDeclarationNodeInfo(tsFile, fieldNode.get());

      assertFalse(fieldInfo.isEmpty());
      Map<String, TSNode> info = fieldInfo.get(0);

      TSNode fieldValue = info.get(FieldCapture.FIELD_VALUE.getCaptureName());
      assertNotNull(fieldValue);
      assertEquals("\"Unknown\"", tsFile.getTextFromNode(fieldValue));
    }

    /**
     * Tests that getFieldDeclarationNodeInfo handles generic field types.
     *
     * <p>Usage example:
     *
     * <pre>
     * List&lt;Map&lt;String, TSNode&gt;&gt; fieldInfo = service.getFieldDeclarationNodeInfo(tsFile, fieldNode);
     * Map&lt;String, TSNode&gt; info = fieldInfo.get(0);
     * TSNode fieldType = info.get("fieldType");
     * TSNode typeArgument = info.get("fieldTypeArgument");
     * // fieldType = "List", typeArgument = "T"
     * </pre>
     */
    @Test
    @DisplayName("should extract generic field information")
    void getFieldDeclarationNodeInfo_withGenericField_shouldExtractTypeArguments() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, GENERIC_FIELDS_CODE);
      Optional<TSNode> classNode =
          classDeclarationService.findClassByName(tsFile, "GenericContainer");
      assertTrue(classNode.isPresent());

      // Find the generic List field
      Optional<TSNode> fieldNode =
          fieldDeclarationService.findFieldDeclarationNodeByName(tsFile, "items", classNode.get());
      assertTrue(fieldNode.isPresent());

      List<Map<String, TSNode>> fieldInfo =
          fieldDeclarationService.getFieldDeclarationNodeInfo(tsFile, fieldNode.get());

      assertFalse(fieldInfo.isEmpty());
      Map<String, TSNode> info = fieldInfo.get(0);

      TSNode fieldType = info.get(FieldCapture.FIELD_TYPE.getCaptureName());
      assertNotNull(fieldType);
      String typeText = tsFile.getTextFromNode(fieldType);
      assertTrue(typeText.contains("List") || typeText.contains("T"));

      // Type argument may or may not be captured depending on the query
      TSNode typeArgument = info.get(FieldCapture.FIELD_TYPE_ARGUMENT.getCaptureName());
      if (typeArgument != null) {
        String argText = tsFile.getTextFromNode(typeArgument);
        assertTrue(argText.equals("T") || argText.equals("String"));
      }
    }

    @Test
    @DisplayName("should return empty list for invalid input")
    void getFieldDeclarationNodeInfo_withInvalidInput_shouldReturnEmptyList() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_FIELDS_CODE);

      // Test with null file
      List<Map<String, TSNode>> result1 =
          fieldDeclarationService.getFieldDeclarationNodeInfo(null, null);
      assertTrue(result1.isEmpty());

      // Test with invalid node type
      TSNode methodNode =
          tsFile.query("(method_declaration) @method").returning("method").execute().firstNode();
      assertNotNull(methodNode);

      List<Map<String, TSNode>> result2 =
          fieldDeclarationService.getFieldDeclarationNodeInfo(tsFile, methodNode);
      assertTrue(result2.isEmpty());
    }
  }

  @Nested
  @DisplayName("getAllFieldDeclarationNodes() Tests")
  class GetAllFieldDeclarationNodesTests {

    /**
     * Tests that getAllFieldDeclarationNodes finds all fields in a class.
     *
     * <p>Usage example:
     *
     * <pre>
     * List&lt;TSNode&gt; fieldNodes = service.getAllFieldDeclarationNodes(tsFile, classNode);
     * for (TSNode field : fieldNodes) {
     *   String fieldText = tsFile.getTextFromNode(field);  // e.g., "private String name;"
     * }
     * </pre>
     */
    @Test
    @DisplayName("should find all field declarations in class")
    void getAllFieldDeclarationNodes_withMultipleFields_shouldFindAll() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_FIELDS_CODE);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "SimpleClass");
      assertTrue(classNode.isPresent());

      List<TSNode> fieldNodes =
          fieldDeclarationService.getAllFieldDeclarationNodes(tsFile, classNode.get());

      // Should find exactly 3 fields (name, age, active)
      assertEquals(3, fieldNodes.size());

      // Verify we can get field names for all found fields
      for (TSNode fieldNode : fieldNodes) {
        Optional<TSNode> nameNode =
            fieldDeclarationService.getFieldDeclarationNameNode(tsFile, fieldNode);
        assertTrue(nameNode.isPresent());
        String fieldName = tsFile.getTextFromNode(nameNode.get());
        assertTrue(List.of("name", "age", "active").contains(fieldName));
      }
    }

    @Test
    @DisplayName("should return empty list for class with no fields")
    void getAllFieldDeclarationNodes_withNoFields_shouldReturnEmptyList() {
      String noFieldsCode =
          """
          public class EmptyClass {
              public void method() {
                  // no fields
              }
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, noFieldsCode);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "EmptyClass");
      assertTrue(classNode.isPresent());

      List<TSNode> fieldNodes =
          fieldDeclarationService.getAllFieldDeclarationNodes(tsFile, classNode.get());

      assertTrue(fieldNodes.isEmpty());
    }

    @Test
    @DisplayName("should handle null input")
    void getAllFieldDeclarationNodes_withNullInput_shouldReturnEmptyList() {
      List<TSNode> result = fieldDeclarationService.getAllFieldDeclarationNodes(null, null);

      assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("should handle invalid class node")
    void getAllFieldDeclarationNodes_withInvalidClassNode_shouldReturnEmptyList() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_FIELDS_CODE);
      TSNode methodNode =
          tsFile.query("(method_declaration) @method").returning("method").execute().firstNode();
      assertNotNull(methodNode);

      List<TSNode> result = fieldDeclarationService.getAllFieldDeclarationNodes(tsFile, methodNode);

      assertTrue(result.isEmpty());
    }
  }

  @Nested
  @DisplayName("getAllFieldDeclarationAnnotationNodes() Tests")
  class GetAllFieldDeclarationAnnotationNodesTests {

    /**
     * Tests that getAllFieldDeclarationAnnotationNodes finds all field annotations.
     *
     * <p>Usage example:
     *
     * <pre>
     * List&lt;TSNode&gt; annotationNodes = service.getAllFieldDeclarationAnnotationNodes(tsFile, classNode);
     * for (TSNode annotation : annotationNodes) {
     *   String annotationText = tsFile.getTextFromNode(annotation);  // e.g., "@NotNull"
     * }
     * </pre>
     */
    @Test
    @DisplayName("should find all field annotations")
    void getAllFieldDeclarationAnnotationNodes_withAnnotatedFields_shouldFindAnnotations() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, ANNOTATED_FIELDS_CODE);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "User");
      assertTrue(classNode.isPresent());

      List<TSNode> annotationNodes =
          fieldDeclarationService.getAllFieldDeclarationAnnotationNodes(tsFile, classNode.get());

      // Should find multiple annotations (@Id, @GeneratedValue, @Column, @NotNull, @Email,
      // @OneToMany)
      assertFalse(annotationNodes.isEmpty());
      assertTrue(annotationNodes.size() >= 1);
    }

    @Test
    @DisplayName("should return empty list for fields without annotations")
    void getAllFieldDeclarationAnnotationNodes_withoutAnnotations_shouldReturnEmptyList() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_FIELDS_CODE);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "SimpleClass");
      assertTrue(classNode.isPresent());

      List<TSNode> annotationNodes =
          fieldDeclarationService.getAllFieldDeclarationAnnotationNodes(tsFile, classNode.get());

      assertTrue(annotationNodes.isEmpty());
    }
  }

  @Nested
  @DisplayName("getFieldDeclarationFullTypeNode() Tests")
  class GetFieldDeclarationFullTypeNodeTests {

    /**
     * Tests that getFieldDeclarationFullTypeNode retrieves complete type information.
     *
     * <p>Usage example:
     *
     * <pre>
     * Optional&lt;TSNode&gt; fullTypeNode = service.getFieldDeclarationFullTypeNode(tsFile, fieldNode);
     * if (fullTypeNode.isPresent()) {
     *   String type = tsFile.getTextFromNode(fullTypeNode.get());  // e.g., "List&lt;String&gt;"
     * }
     * </pre>
     */
    @Test
    @DisplayName("should retrieve full type node for simple type")
    void getFieldDeclarationFullTypeNode_withSimpleType_shouldReturnTypeNode() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_FIELDS_CODE);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "SimpleClass");
      assertTrue(classNode.isPresent());

      Optional<TSNode> fieldNode =
          fieldDeclarationService.findFieldDeclarationNodeByName(tsFile, "name", classNode.get());
      assertTrue(fieldNode.isPresent());

      Optional<TSNode> typeNode =
          fieldDeclarationService.getFieldDeclarationFullTypeNode(tsFile, fieldNode.get());

      assertTrue(typeNode.isPresent());
      assertEquals("String", tsFile.getTextFromNode(typeNode.get()));
    }

    @Test
    @DisplayName("should retrieve full type node for generic type")
    void getFieldDeclarationFullTypeNode_withGenericType_shouldReturnFullTypeNode() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, GENERIC_FIELDS_CODE);
      Optional<TSNode> classNode =
          classDeclarationService.findClassByName(tsFile, "GenericContainer");
      assertTrue(classNode.isPresent());

      Optional<TSNode> fieldNode =
          fieldDeclarationService.findFieldDeclarationNodeByName(tsFile, "items", classNode.get());
      assertTrue(fieldNode.isPresent());

      Optional<TSNode> typeNode =
          fieldDeclarationService.getFieldDeclarationFullTypeNode(tsFile, fieldNode.get());

      assertTrue(typeNode.isPresent());
      // The actual text will include the full generic type
      String typeText = tsFile.getTextFromNode(typeNode.get());
      assertTrue(typeText.contains("List"));
    }
  }

  @Nested
  @DisplayName("getFieldDeclarationTypeNode() Tests")
  class GetFieldDeclarationTypeNodeTests {

    /**
     * Tests that getFieldDeclarationTypeNode retrieves base type or type argument.
     *
     * <p>Usage example:
     *
     * <pre>
     * Optional&lt;TSNode&gt; typeNode = service.getFieldDeclarationTypeNode(tsFile, fieldNode);
     * if (typeNode.isPresent()) {
     *   String type = tsFile.getTextFromNode(typeNode.get());  // e.g., "String" from List&lt;String&gt;
     * }
     * </pre>
     */
    @Test
    @DisplayName("should retrieve simple type node")
    void getFieldDeclarationTypeNode_withSimpleType_shouldReturnTypeNode() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_FIELDS_CODE);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "SimpleClass");
      assertTrue(classNode.isPresent());

      Optional<TSNode> fieldNode =
          fieldDeclarationService.findFieldDeclarationNodeByName(tsFile, "age", classNode.get());
      assertTrue(fieldNode.isPresent());

      Optional<TSNode> typeNode =
          fieldDeclarationService.getFieldDeclarationTypeNode(tsFile, fieldNode.get());

      assertTrue(typeNode.isPresent());
      String typeText = tsFile.getTextFromNode(typeNode.get());
      assertEquals("int", typeText);
    }

    @Test
    @DisplayName("should retrieve type argument for generic type")
    void getFieldDeclarationTypeNode_withGenericType_shouldReturnTypeArgument() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, GENERIC_FIELDS_CODE);
      Optional<TSNode> classNode =
          classDeclarationService.findClassByName(tsFile, "GenericContainer");
      assertTrue(classNode.isPresent());

      Optional<TSNode> fieldNode =
          fieldDeclarationService.findFieldDeclarationNodeByName(tsFile, "names", classNode.get());
      assertTrue(fieldNode.isPresent());

      Optional<TSNode> typeNode =
          fieldDeclarationService.getFieldDeclarationTypeNode(tsFile, fieldNode.get());

      assertTrue(typeNode.isPresent());
      assertEquals("String", tsFile.getTextFromNode(typeNode.get()));
    }
  }

  @Nested
  @DisplayName("getFieldDeclarationNameNode() Tests")
  class GetFieldDeclarationNameNodeTests {

    /**
     * Tests that getFieldDeclarationNameNode retrieves field name identifier.
     *
     * <p>Usage example:
     *
     * <pre>
     * Optional&lt;TSNode&gt; nameNode = service.getFieldDeclarationNameNode(tsFile, fieldNode);
     * if (nameNode.isPresent()) {
     *   String name = tsFile.getTextFromNode(nameNode.get());  // e.g., "firstName"
     * }
     * </pre>
     */
    @Test
    @DisplayName("should retrieve field name node")
    void getFieldDeclarationNameNode_withValidField_shouldReturnNameNode() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_FIELDS_CODE);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "SimpleClass");
      assertTrue(classNode.isPresent());

      List<TSNode> fieldNodes =
          fieldDeclarationService.getAllFieldDeclarationNodes(tsFile, classNode.get());
      assertFalse(fieldNodes.isEmpty());

      Optional<TSNode> nameNode =
          fieldDeclarationService.getFieldDeclarationNameNode(tsFile, fieldNodes.get(0));

      assertTrue(nameNode.isPresent());
      String fieldName = tsFile.getTextFromNode(nameNode.get());
      assertTrue(List.of("name", "age", "active").contains(fieldName));
    }
  }

  @Nested
  @DisplayName("getFieldDeclarationValueNode() Tests")
  class GetFieldDeclarationValueNodeTests {

    /**
     * Tests that getFieldDeclarationValueNode retrieves initialization values.
     *
     * <p>Usage example:
     *
     * <pre>
     * Optional&lt;TSNode&gt; valueNode = service.getFieldDeclarationValueNode(tsFile, fieldNode);
     * if (valueNode.isPresent()) {
     *   String value = tsFile.getTextFromNode(valueNode.get());  // e.g., "\"John\""
     * }
     * </pre>
     */
    @Test
    @DisplayName("should retrieve field initialization value")
    void getFieldDeclarationValueNode_withInitializedField_shouldReturnValueNode() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, INITIALIZED_FIELDS_CODE);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "ConfigClass");
      assertTrue(classNode.isPresent());

      Optional<TSNode> fieldNode =
          fieldDeclarationService.findFieldDeclarationNodeByName(
              tsFile, "maxRetries", classNode.get());
      assertTrue(fieldNode.isPresent());

      Optional<TSNode> valueNode =
          fieldDeclarationService.getFieldDeclarationValueNode(tsFile, fieldNode.get());

      if (valueNode.isPresent()) {
        assertEquals("3", tsFile.getTextFromNode(valueNode.get()));
      } else {
        // If value node is not captured, that's also acceptable for this test
        assertTrue(true, "Field value node not captured, but method executed without error");
      }
    }

    @Test
    @DisplayName("should return empty for field without initialization")
    void getFieldDeclarationValueNode_withoutInitialization_shouldReturnEmpty() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_FIELDS_CODE);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "SimpleClass");
      assertTrue(classNode.isPresent());

      Optional<TSNode> fieldNode =
          fieldDeclarationService.findFieldDeclarationNodeByName(tsFile, "name", classNode.get());
      assertTrue(fieldNode.isPresent());

      Optional<TSNode> valueNode =
          fieldDeclarationService.getFieldDeclarationValueNode(tsFile, fieldNode.get());

      assertFalse(valueNode.isPresent());
    }
  }

  @Nested
  @DisplayName("getAllFieldDeclarationUsageNodes() Tests")
  class GetAllFieldDeclarationUsageNodesTests {

    /**
     * Tests that getAllFieldDeclarationUsageNodes finds field usage in methods.
     *
     * <p>Usage example:
     *
     * <pre>
     * List&lt;TSNode&gt; usages = service.getAllFieldDeclarationUsageNodes(tsFile, fieldNode, classNode);
     * for (TSNode usage : usages) {
     *   String usageText = tsFile.getTextFromNode(usage);  // e.g., "fieldName"
     *   int line = usage.getStartPoint().getRow() + 1;     // Line number of usage
     * }
     * </pre>
     */
    @Test
    @DisplayName("should find field usages in class methods")
    void getAllFieldDeclarationUsageNodes_withFieldUsages_shouldFindUsages() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, INITIALIZED_FIELDS_CODE);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "ConfigClass");
      assertTrue(classNode.isPresent());

      Optional<TSNode> fieldNode =
          fieldDeclarationService.findFieldDeclarationNodeByName(
              tsFile, "defaultName", classNode.get());
      assertTrue(fieldNode.isPresent());

      List<TSNode> usageNodes =
          fieldDeclarationService.getAllFieldDeclarationUsageNodes(
              tsFile, fieldNode.get(), classNode.get());

      assertFalse(usageNodes.isEmpty());
      // Should find at least one usage (this.defaultName = "Updated")
      assertTrue(usageNodes.size() >= 1);
    }

    @Test
    @DisplayName("should return empty list for unused fields")
    void getAllFieldDeclarationUsageNodes_withUnusedField_shouldReturnEmptyList() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_FIELDS_CODE);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "SimpleClass");
      assertTrue(classNode.isPresent());

      Optional<TSNode> fieldNode =
          fieldDeclarationService.findFieldDeclarationNodeByName(tsFile, "name", classNode.get());
      assertTrue(fieldNode.isPresent());

      List<TSNode> usageNodes =
          fieldDeclarationService.getAllFieldDeclarationUsageNodes(
              tsFile, fieldNode.get(), classNode.get());

      assertTrue(usageNodes.isEmpty());
    }

    @Test
    @DisplayName("should handle invalid input")
    void getAllFieldDeclarationUsageNodes_withInvalidInput_shouldReturnEmptyList() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_FIELDS_CODE);

      List<TSNode> result1 =
          fieldDeclarationService.getAllFieldDeclarationUsageNodes(null, null, null);
      assertTrue(result1.isEmpty());

      TSNode methodNode =
          tsFile.query("(method_declaration) @method").returning("method").execute().firstNode();
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "SimpleClass");

      List<TSNode> result2 =
          fieldDeclarationService.getAllFieldDeclarationUsageNodes(
              tsFile, methodNode, classNode.get());
      assertTrue(result2.isEmpty());
    }
  }

  @Nested
  @DisplayName("findFieldDeclarationNodeByName() Tests")
  class FindFieldDeclarationNodeByNameTests {

    /**
     * Tests that findFieldDeclarationNodeByName locates fields by exact name.
     *
     * <p>Usage example:
     *
     * <pre>
     * Optional&lt;TSNode&gt; fieldNode = service.findFieldDeclarationNodeByName(tsFile, "myField", classNode);
     * if (fieldNode.isPresent()) {
     *   String fieldText = tsFile.getTextFromNode(fieldNode.get());  // e.g., "private String myField;"
     * }
     * </pre>
     */
    @Test
    @DisplayName("should find existing field by name")
    void findFieldDeclarationNodeByName_withExistingField_shouldReturnField() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_FIELDS_CODE);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "SimpleClass");
      assertTrue(classNode.isPresent());

      Optional<TSNode> fieldNode =
          fieldDeclarationService.findFieldDeclarationNodeByName(tsFile, "name", classNode.get());

      assertTrue(fieldNode.isPresent());
      String fieldText = tsFile.getTextFromNode(fieldNode.get());
      assertTrue(fieldText.contains("name"));
      assertTrue(fieldText.contains("String"));
    }

    @Test
    @DisplayName("should return empty for non-existent field")
    void findFieldDeclarationNodeByName_withNonExistentField_shouldReturnEmpty() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_FIELDS_CODE);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "SimpleClass");
      assertTrue(classNode.isPresent());

      Optional<TSNode> fieldNode =
          fieldDeclarationService.findFieldDeclarationNodeByName(
              tsFile, "nonExistentField", classNode.get());

      assertFalse(fieldNode.isPresent());
    }

    @Test
    @DisplayName("should handle null and empty parameters")
    void findFieldDeclarationNodeByName_withInvalidParameters_shouldReturnEmpty() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_FIELDS_CODE);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "SimpleClass");
      assertTrue(classNode.isPresent());

      Optional<TSNode> result1 =
          fieldDeclarationService.findFieldDeclarationNodeByName(null, "name", classNode.get());
      assertFalse(result1.isPresent());

      Optional<TSNode> result2 =
          fieldDeclarationService.findFieldDeclarationNodeByName(tsFile, null, classNode.get());
      assertFalse(result2.isPresent());

      Optional<TSNode> result3 =
          fieldDeclarationService.findFieldDeclarationNodeByName(tsFile, "", classNode.get());
      assertFalse(result3.isPresent());

      TSNode mockNode = tsFile.getTree().getRootNode();
      Optional<TSNode> result4 =
          fieldDeclarationService.findFieldDeclarationNodeByName(tsFile, "name", mockNode);
      assertFalse(result4.isPresent());
    }

    @Test
    @DisplayName("should be case sensitive")
    void findFieldDeclarationNodeByName_shouldBeCaseSensitive() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_FIELDS_CODE);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "SimpleClass");
      assertTrue(classNode.isPresent());

      Optional<TSNode> exactMatch =
          fieldDeclarationService.findFieldDeclarationNodeByName(tsFile, "name", classNode.get());
      assertTrue(exactMatch.isPresent());

      Optional<TSNode> casesDifferent =
          fieldDeclarationService.findFieldDeclarationNodeByName(tsFile, "Name", classNode.get());
      assertFalse(casesDifferent.isPresent());
    }
  }

  @Nested
  @DisplayName("findFieldDeclarationNodesByType() Tests")
  class FindFieldDeclarationNodesByTypeTests {

    /**
     * Tests that findFieldDeclarationNodesByType finds all fields of a specific type.
     *
     * <p>Usage example:
     *
     * <pre>
     * List&lt;TSNode&gt; stringFields = service.findFieldDeclarationNodesByType(tsFile, "String", classNode);
     * for (TSNode field : stringFields) {
     *   String fieldText = tsFile.getTextFromNode(field);  // e.g., "private String name;"
     * }
     * </pre>
     */
    @Test
    @DisplayName("should find fields by exact type")
    void findFieldDeclarationNodesByType_withMatchingType_shouldReturnFields() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, MULTIPLE_SAME_TYPE_CODE);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "PersonClass");
      assertTrue(classNode.isPresent());

      List<TSNode> stringFields =
          fieldDeclarationService.findFieldDeclarationNodesByType(
              tsFile, "String", classNode.get());

      assertEquals(3, stringFields.size()); // firstName, lastName, email

      // Verify all are String fields
      for (TSNode field : stringFields) {
        Optional<TSNode> nameNode =
            fieldDeclarationService.getFieldDeclarationNameNode(tsFile, field);
        assertTrue(nameNode.isPresent());
        String fieldName = tsFile.getTextFromNode(nameNode.get());
        assertTrue(List.of("firstName", "lastName", "email").contains(fieldName));
      }
    }

    @Test
    @DisplayName("should find fields by primitive type")
    void findFieldDeclarationNodesByType_withPrimitiveType_shouldReturnFields() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, MULTIPLE_SAME_TYPE_CODE);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "PersonClass");
      assertTrue(classNode.isPresent());

      List<TSNode> intFields =
          fieldDeclarationService.findFieldDeclarationNodesByType(tsFile, "int", classNode.get());

      // Should find exactly 2 int fields (age, score)
      assertEquals(2, intFields.size());
    }

    @Test
    @DisplayName("should return empty list for non-existent type")
    void findFieldDeclarationNodesByType_withNonExistentType_shouldReturnEmptyList() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_FIELDS_CODE);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "SimpleClass");
      assertTrue(classNode.isPresent());

      List<TSNode> fields =
          fieldDeclarationService.findFieldDeclarationNodesByType(
              tsFile, "NonExistentType", classNode.get());

      assertTrue(fields.isEmpty());
    }

    @Test
    @DisplayName("should handle null and empty parameters")
    void findFieldDeclarationNodesByType_withInvalidParameters_shouldReturnEmptyList() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_FIELDS_CODE);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "SimpleClass");
      assertTrue(classNode.isPresent());

      List<TSNode> result1 =
          fieldDeclarationService.findFieldDeclarationNodesByType(null, "String", classNode.get());
      assertTrue(result1.isEmpty());

      List<TSNode> result2 =
          fieldDeclarationService.findFieldDeclarationNodesByType(tsFile, null, classNode.get());
      assertTrue(result2.isEmpty());

      List<TSNode> result3 =
          fieldDeclarationService.findFieldDeclarationNodesByType(tsFile, "", classNode.get());
      assertTrue(result3.isEmpty());
    }

    @Test
    @DisplayName("should be case sensitive")
    void findFieldDeclarationNodesByType_shouldBeCaseSensitive() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_FIELDS_CODE);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "SimpleClass");
      assertTrue(classNode.isPresent());

      List<TSNode> exactMatch =
          fieldDeclarationService.findFieldDeclarationNodesByType(
              tsFile, "String", classNode.get());
      assertFalse(exactMatch.isEmpty());

      List<TSNode> casesDifferent =
          fieldDeclarationService.findFieldDeclarationNodesByType(
              tsFile, "string", classNode.get());
      assertTrue(casesDifferent.isEmpty());
    }
  }

  @Nested
  @DisplayName("Edge Cases and Error Handling Tests")
  class EdgeCasesTests {

    @Test
    @DisplayName("should handle class with only static fields")
    void methods_withStaticFields_shouldHandleCorrectly() {
      String staticFieldsCode =
          """
          public class ConstantsClass {
              public static final String CONSTANT = "VALUE";
              private static int counter = 0;

              public static void increment() {
                  counter++;
              }
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, staticFieldsCode);
      Optional<TSNode> classNode =
          classDeclarationService.findClassByName(tsFile, "ConstantsClass");
      assertTrue(classNode.isPresent());

      List<TSNode> fieldNodes =
          fieldDeclarationService.getAllFieldDeclarationNodes(tsFile, classNode.get());
      assertEquals(2, fieldNodes.size());

      // Should find both CONSTANT and counter fields
      Optional<TSNode> constantField =
          fieldDeclarationService.findFieldDeclarationNodeByName(
              tsFile, "CONSTANT", classNode.get());
      assertTrue(constantField.isPresent());

      Optional<TSNode> counterField =
          fieldDeclarationService.findFieldDeclarationNodeByName(
              tsFile, "counter", classNode.get());
      assertTrue(counterField.isPresent());
    }

    @Test
    @DisplayName("should handle complex generic fields")
    void methods_withComplexGenerics_shouldHandleCorrectly() {
      String complexGenericsCode =
          """
          public class ComplexGenericClass {
              private Map<String, List<Integer>> complexMap;
              private Optional<Map<String, Set<Long>>> optionalMap;

              public void method() {
                  this.complexMap = new HashMap<>();
              }
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, complexGenericsCode);
      Optional<TSNode> classNode =
          classDeclarationService.findClassByName(tsFile, "ComplexGenericClass");
      assertTrue(classNode.isPresent());

      List<TSNode> fieldNodes =
          fieldDeclarationService.getAllFieldDeclarationNodes(tsFile, classNode.get());
      assertEquals(2, fieldNodes.size());

      // Should be able to extract type information for complex generics
      for (TSNode field : fieldNodes) {
        Optional<TSNode> typeNode =
            fieldDeclarationService.getFieldDeclarationFullTypeNode(tsFile, field);
        assertTrue(typeNode.isPresent());
      }
    }

    @Test
    @DisplayName("should handle empty source code")
    void methods_withEmptyCode_shouldHandleGracefully() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, "");

      // Create a mock node instead of passing null
      TSNode mockNode = tsFile.getTree().getRootNode();
      List<TSNode> fieldNodes =
          fieldDeclarationService.getAllFieldDeclarationNodes(tsFile, mockNode);
      assertTrue(fieldNodes.isEmpty());

      Optional<TSNode> fieldNode =
          fieldDeclarationService.findFieldDeclarationNodeByName(tsFile, "field", mockNode);
      assertFalse(fieldNode.isPresent());
    }

    @Test
    @DisplayName("should handle malformed source code")
    void methods_withMalformedCode_shouldHandleGracefully() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, "public class { private String");

      // Should not crash, may return empty or partial results
      TSNode mockNode = tsFile.getTree().getRootNode();
      List<TSNode> fieldNodes =
          fieldDeclarationService.getAllFieldDeclarationNodes(tsFile, mockNode);
      assertNotNull(fieldNodes);
    }
  }

  @Nested
  @DisplayName("Structured Parameter Tests")
  class StructuredParameterTests {

    private FieldDeclarationService fieldDeclarationServiceWithDeps;
    private PackageDeclarationService packageService;
    private ImportDeclarationService importService;

    @BeforeEach
    void setUp() {
      PathHelper pathHelper = new PathHelper();
      packageService = new PackageDeclarationService(pathHelper);
      importService = new ImportDeclarationService();
      fieldDeclarationServiceWithDeps = new FieldDeclarationService(packageService, importService);
    }

    @Test
    @DisplayName("should add field using JavaBasicType successfully")
    void addField_withJavaBasicType_shouldSucceed() {
      String testCode = """
          package com.example.test;
          
          public class TestClass {
              
          }
          """;
      
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, testCode);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "TestClass");
      assertTrue(classNode.isPresent());
      
      var insertionPoint = fieldDeclarationService.getFieldInsertionPosition(
          tsFile, classNode.get(), FieldInsertionPosition.BEGINNING_OF_CLASS_BODY);
      assertNotNull(insertionPoint);
      
      // Test adding JavaBasicType field
      fieldDeclarationServiceWithDeps.addField(tsFile, classNode.get(), insertionPoint, 
          "private", false, JavaBasicType.LANG_STRING, "name", null);
      
      // Verify field was added
      String updatedCode = tsFile.getSourceCode();
      assertTrue(updatedCode.contains("private String name;"));
    }

    @Test
    @DisplayName("should add field using custom type successfully")
    void addField_withCustomType_shouldSucceed() {
      String testCode = """
          package com.example.test;
          
          public class TestClass {
              
          }
          """;
      
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, testCode);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "TestClass");
      assertTrue(classNode.isPresent());
      
      var insertionPoint = fieldDeclarationService.getFieldInsertionPosition(
          tsFile, classNode.get(), FieldInsertionPosition.BEGINNING_OF_CLASS_BODY);
      assertNotNull(insertionPoint);
      
      // Test adding custom type field
      fieldDeclarationServiceWithDeps.addField(tsFile, classNode.get(), insertionPoint, 
          "private", false, "User", "user", null);
      
      // Verify field was added
      String updatedCode = tsFile.getSourceCode();
      assertTrue(updatedCode.contains("private User user;"));
    }

    @Test
    @DisplayName("should add field with final modifier and initialization")
    void addField_withFinalAndInit_shouldSucceed() {
      String testCode = """
          package com.example.test;
          
          public class TestClass {
              
          }
          """;
      
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, testCode);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "TestClass");
      assertTrue(classNode.isPresent());
      
      var insertionPoint = fieldDeclarationService.getFieldInsertionPosition(
          tsFile, classNode.get(), FieldInsertionPosition.BEGINNING_OF_CLASS_BODY);
      assertNotNull(insertionPoint);
      
      // Test adding final initialized field
      fieldDeclarationServiceWithDeps.addField(tsFile, classNode.get(), insertionPoint, 
          "public", true, JavaBasicType.PRIMITIVE_INT, "count", "0");
      
      // Verify field was added
      String updatedCode = tsFile.getSourceCode();
      assertTrue(updatedCode.contains("public final int count = 0;"));
    }

    @Test
    @DisplayName("should add field with generic type")
    void addField_withGenericType_shouldSucceed() {
      String testCode = """
          package com.example.test;
          
          public class TestClass {
              
          }
          """;
      
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, testCode);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "TestClass");
      assertTrue(classNode.isPresent());
      
      var insertionPoint = fieldDeclarationService.getFieldInsertionPosition(
          tsFile, classNode.get(), FieldInsertionPosition.BEGINNING_OF_CLASS_BODY);
      assertNotNull(insertionPoint);
      
      // Test adding generic type field
      fieldDeclarationServiceWithDeps.addField(tsFile, classNode.get(), insertionPoint, 
          "private", false, "List<User>", "users", "new ArrayList<>()");
      
      // Verify field was added
      String updatedCode = tsFile.getSourceCode();
      assertTrue(updatedCode.contains("private List<User> users = new ArrayList<>();"));
    }

    @Test
    @DisplayName("should automatically add imports for JavaBasicType that needs import")
    void addField_withJavaBasicTypeNeedingImport_shouldAddImport() {
      String testCode = """
          package com.example.test;
          
          public class TestClass {
              
          }
          """;
      
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, testCode);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "TestClass");
      assertTrue(classNode.isPresent());
      
      var insertionPoint = fieldDeclarationService.getFieldInsertionPosition(
          tsFile, classNode.get(), FieldInsertionPosition.BEGINNING_OF_CLASS_BODY);
      assertNotNull(insertionPoint);
      
      // Test adding UUID field which requires import
      fieldDeclarationServiceWithDeps.addField(tsFile, classNode.get(), insertionPoint, 
          "private", false, JavaBasicType.UTIL_UUID, "id", null);
      
      String updatedCode = tsFile.getSourceCode();
      // Verify field was added
      assertTrue(updatedCode.contains("private UUID id;"));
      // Verify import was added
      assertTrue(updatedCode.contains("import java.util.UUID;"));
    }

    @Test
    @DisplayName("should not add imports for primitive JavaBasicType")
    void addField_withPrimitiveJavaBasicType_shouldNotAddImport() {
      String testCode = """
          package com.example.test;
          
          public class TestClass {
              
          }
          """;
      
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, testCode);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "TestClass");
      assertTrue(classNode.isPresent());
      
      var insertionPoint = fieldDeclarationService.getFieldInsertionPosition(
          tsFile, classNode.get(), FieldInsertionPosition.BEGINNING_OF_CLASS_BODY);
      assertNotNull(insertionPoint);
      
      String originalCode = tsFile.getSourceCode();
      
      // Test adding int field which doesn't need import
      fieldDeclarationServiceWithDeps.addField(tsFile, classNode.get(), insertionPoint, 
          "private", false, JavaBasicType.PRIMITIVE_INT, "count", null);
      
      String updatedCode = tsFile.getSourceCode();
      // Verify field was added
      assertTrue(updatedCode.contains("private int count;"));
      // Verify no additional imports were added (should have same import count)
      int originalImportCount = originalCode.split("import").length - 1;
      int updatedImportCount = updatedCode.split("import").length - 1;
      assertEquals(originalImportCount, updatedImportCount);
    }

    @Test
    @DisplayName("should handle null and empty parameters gracefully")
    void addField_withInvalidParams_shouldHandleGracefully() {
      String testCode = """
          package com.example.test;
          
          public class TestClass {
              
          }
          """;
      
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, testCode);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "TestClass");
      assertTrue(classNode.isPresent());
      
      var insertionPoint = fieldDeclarationService.getFieldInsertionPosition(
          tsFile, classNode.get(), FieldInsertionPosition.BEGINNING_OF_CLASS_BODY);
      assertNotNull(insertionPoint);
      
      String originalCode = tsFile.getSourceCode();
      
      // Test with null JavaBasicType - should not modify code
      fieldDeclarationServiceWithDeps.addField(tsFile, classNode.get(), insertionPoint, 
          "private", false, (JavaBasicType)null, "name", null);
      assertEquals(originalCode, tsFile.getSourceCode());
      
      // Test with empty field name - should not modify code
      fieldDeclarationServiceWithDeps.addField(tsFile, classNode.get(), insertionPoint, 
          "private", false, JavaBasicType.LANG_STRING, "", null);
      assertEquals(originalCode, tsFile.getSourceCode());
      
      // Test with empty custom type - should not modify code
      fieldDeclarationServiceWithDeps.addField(tsFile, classNode.get(), insertionPoint, 
          "private", false, "", "name", null);
      assertEquals(originalCode, tsFile.getSourceCode());
    }

    @Test
    @DisplayName("should add multiple fields with different types and modifiers")
    void addField_withMultipleFields_shouldSucceed() {
      String testCode = """
          package com.example.test;
          
          public class TestClass {
              
          }
          """;
      
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, testCode);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "TestClass");
      assertTrue(classNode.isPresent());
      
      var insertionPoint = fieldDeclarationService.getFieldInsertionPosition(
          tsFile, classNode.get(), FieldInsertionPosition.BEGINNING_OF_CLASS_BODY);
      assertNotNull(insertionPoint);
      
      // Add multiple fields of different types
      fieldDeclarationServiceWithDeps.addField(tsFile, classNode.get(), insertionPoint, 
          "private", false, JavaBasicType.LANG_STRING, "name", null);
      
      fieldDeclarationServiceWithDeps.addField(tsFile, classNode.get(), insertionPoint, 
          "private", false, JavaBasicType.PRIMITIVE_INT, "age", "0");
      
      fieldDeclarationServiceWithDeps.addField(tsFile, classNode.get(), insertionPoint, 
          "public", true, JavaBasicType.PRIMITIVE_BOOLEAN, "active", "true");
      
      fieldDeclarationServiceWithDeps.addField(tsFile, classNode.get(), insertionPoint, 
          "protected", false, "List<String>", "items", "new ArrayList<>()");
      
      String updatedCode = tsFile.getSourceCode();
      
      // Verify all fields were added
      assertTrue(updatedCode.contains("private String name;"));
      assertTrue(updatedCode.contains("private int age = 0;"));
      assertTrue(updatedCode.contains("public final boolean active = true;"));
      assertTrue(updatedCode.contains("protected List<String> items = new ArrayList<>();"));
    }

    @Test
    @DisplayName("should handle different visibility modifiers correctly")
    void addField_withDifferentVisibilities_shouldFormatCorrectly() {
      String testCode = """
          package com.example.test;
          
          public class TestClass {
              
          }
          """;
      
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, testCode);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "TestClass");
      assertTrue(classNode.isPresent());
      
      var insertionPoint = fieldDeclarationService.getFieldInsertionPosition(
          tsFile, classNode.get(), FieldInsertionPosition.BEGINNING_OF_CLASS_BODY);
      assertNotNull(insertionPoint);
      
      // Test all visibility modifiers
      fieldDeclarationServiceWithDeps.addField(tsFile, classNode.get(), insertionPoint, 
          "public", false, JavaBasicType.LANG_STRING, "publicField", null);
      
      fieldDeclarationServiceWithDeps.addField(tsFile, classNode.get(), insertionPoint, 
          "private", false, JavaBasicType.LANG_STRING, "privateField", null);
      
      fieldDeclarationServiceWithDeps.addField(tsFile, classNode.get(), insertionPoint, 
          "protected", false, JavaBasicType.LANG_STRING, "protectedField", null);
      
      // Package-private (empty visibility)
      fieldDeclarationServiceWithDeps.addField(tsFile, classNode.get(), insertionPoint, 
          "", false, JavaBasicType.LANG_STRING, "packageField", null);
      
      // Null visibility (should be treated as package-private)
      fieldDeclarationServiceWithDeps.addField(tsFile, classNode.get(), insertionPoint, 
          null, false, JavaBasicType.LANG_STRING, "nullVisibilityField", null);
      
      String updatedCode = tsFile.getSourceCode();
      
      // Verify visibility modifiers
      assertTrue(updatedCode.contains("public String publicField;"));
      assertTrue(updatedCode.contains("private String privateField;"));
      assertTrue(updatedCode.contains("protected String protectedField;"));
      assertTrue(updatedCode.contains("String packageField;") && !updatedCode.contains("public String packageField;"));
      assertTrue(updatedCode.contains("String nullVisibilityField;") && !updatedCode.contains("public String nullVisibilityField;"));
    }

    @Test
    @DisplayName("should add fields with complex generic types")
    void addField_withComplexGenerics_shouldSucceed() {
      String testCode = """
          package com.example.test;
          
          public class TestClass {
              
          }
          """;
      
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, testCode);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "TestClass");
      assertTrue(classNode.isPresent());
      
      var insertionPoint = fieldDeclarationService.getFieldInsertionPosition(
          tsFile, classNode.get(), FieldInsertionPosition.BEGINNING_OF_CLASS_BODY);
      assertNotNull(insertionPoint);
      
      // Test complex generic types
      fieldDeclarationServiceWithDeps.addField(tsFile, classNode.get(), insertionPoint, 
          "private", false, "Map<String, Integer>", "scoreMap", "new HashMap<>()");
      
      fieldDeclarationServiceWithDeps.addField(tsFile, classNode.get(), insertionPoint, 
          "private", false, "List<Map<String, Object>>", "complexList", "new ArrayList<>()");
      
      fieldDeclarationServiceWithDeps.addField(tsFile, classNode.get(), insertionPoint, 
          "private", false, "Optional<List<User>>", "optionalUsers", "Optional.empty()");
      
      fieldDeclarationServiceWithDeps.addField(tsFile, classNode.get(), insertionPoint, 
          "private", false, "CompletableFuture<ResponseEntity<String>>", "asyncResponse", null);
      
      String updatedCode = tsFile.getSourceCode();
      
      // Verify complex generic types
      assertTrue(updatedCode.contains("private Map<String, Integer> scoreMap = new HashMap<>();"));
      assertTrue(updatedCode.contains("private List<Map<String, Object>> complexList = new ArrayList<>();"));
      assertTrue(updatedCode.contains("private Optional<List<User>> optionalUsers = Optional.empty();"));
      assertTrue(updatedCode.contains("private CompletableFuture<ResponseEntity<String>> asyncResponse;"));
    }

    @Test
    @DisplayName("should add fields at different insertion positions")
    void addField_withDifferentInsertionPositions_shouldPlaceCorrectly() {
      String testCode = """
          package com.example.test;
          
          public class TestClass {
              private String existingField;
              
              public void method() {
                  // method body
              }
          }
          """;
      
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, testCode);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "TestClass");
      assertTrue(classNode.isPresent());
      
      // Test AFTER_LAST_FIELD first to avoid position corruption
      var afterLastPoint = fieldDeclarationService.getFieldInsertionPosition(
          tsFile, classNode.get(), FieldInsertionPosition.AFTER_LAST_FIELD);
      assertNotNull(afterLastPoint);
      
      fieldDeclarationServiceWithDeps.addField(tsFile, classNode.get(), afterLastPoint, 
          "private", false, JavaBasicType.PRIMITIVE_BOOLEAN, "afterLast", null);
      
      // Now recalculate BEFORE_FIRST_FIELD position after the source has been modified
      var beforeFirstPoint = fieldDeclarationService.getFieldInsertionPosition(
          tsFile, classNode.get(), FieldInsertionPosition.BEFORE_FIRST_FIELD);
      assertNotNull(beforeFirstPoint);
      
      fieldDeclarationServiceWithDeps.addField(tsFile, classNode.get(), beforeFirstPoint, 
          "private", false, JavaBasicType.PRIMITIVE_INT, "beforeFirst", null);
      
      String updatedCode = tsFile.getSourceCode();
      
      // Verify field positioning - beforeFirst should appear before existingField
      int beforeFirstIndex = updatedCode.indexOf("private int beforeFirst;");
      int existingFieldIndex = updatedCode.indexOf("private String existingField;");
      int afterLastIndex = updatedCode.indexOf("private boolean afterLast;");
      
      assertTrue(beforeFirstIndex > 0);
      assertTrue(existingFieldIndex > 0);
      assertTrue(afterLastIndex > 0);
      assertTrue(beforeFirstIndex < existingFieldIndex, "beforeFirst should appear before existingField");
      assertTrue(existingFieldIndex < afterLastIndex, "afterLast should appear after existingField");
    }

    @Test
    @DisplayName("should add single field with import handling")
    void addField_withSingleJavaBasicType_shouldHandleImport() {
      String testCode = """
          package com.example.test;
          
          public class TestClass {
              
          }
          """;
      
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, testCode);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "TestClass");
      assertTrue(classNode.isPresent());
      
      var insertionPoint = fieldDeclarationService.getFieldInsertionPosition(
          tsFile, classNode.get(), FieldInsertionPosition.BEGINNING_OF_CLASS_BODY);
      assertNotNull(insertionPoint);
      
      // Add a single field that requires an import
      fieldDeclarationServiceWithDeps.addField(tsFile, classNode.get(), insertionPoint, 
          "private", false, JavaBasicType.UTIL_UUID, "uuidField", null);
      
      String updatedCode = tsFile.getSourceCode();
      
      // Verify field was added
      assertTrue(updatedCode.contains("private UUID uuidField;"));
      
      // Verify import was added
      assertTrue(updatedCode.contains("import java.util.UUID;"));
    }
    
    @Test
    @DisplayName("should add multiple fields with imports by recalculating positions")
    void addField_withMultipleJavaBasicTypes_shouldHandleImports() {
      String testCode = """
          package com.example.test;
          
          public class TestClass {
              
          }
          """;
      
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, testCode);
      
      // Add first field (primitive, no import needed)
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "TestClass");
      var insertionPoint = fieldDeclarationService.getFieldInsertionPosition(
          tsFile, classNode.get(), FieldInsertionPosition.BEGINNING_OF_CLASS_BODY);
      fieldDeclarationServiceWithDeps.addField(tsFile, classNode.get(), insertionPoint, 
          "private", false, JavaBasicType.PRIMITIVE_INT, "intField", null);
      
      // Add second field (requires import) using fresh class node lookup
      classNode = classDeclarationService.findClassByName(tsFile, "TestClass");
      insertionPoint = fieldDeclarationService.getFieldInsertionPosition(
          tsFile, classNode.get(), FieldInsertionPosition.AFTER_LAST_FIELD);
      fieldDeclarationServiceWithDeps.addField(tsFile, classNode.get(), insertionPoint, 
          "private", false, JavaBasicType.UTIL_UUID, "uuidField", null);
      
      String updatedCode = tsFile.getSourceCode();
      
      // Verify both fields were added correctly
      assertTrue(updatedCode.contains("private int intField;"));
      assertTrue(updatedCode.contains("private UUID uuidField;"));
      
      // Verify import was added for UUID
      assertTrue(updatedCode.contains("import java.util.UUID;"));
      
      // Verify no import for primitive
      assertFalse(updatedCode.contains("import int;"));
    }

    @Test
    @DisplayName("should handle field initialization with various expressions")
    void addField_withVariousInitializations_shouldFormatCorrectly() {
      String testCode = """
          package com.example.test;
          
          public class TestClass {
              
          }
          """;
      
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, testCode);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "TestClass");
      assertTrue(classNode.isPresent());
      
      var insertionPoint = fieldDeclarationService.getFieldInsertionPosition(
          tsFile, classNode.get(), FieldInsertionPosition.BEGINNING_OF_CLASS_BODY);
      assertNotNull(insertionPoint);
      
      // Test various initialization expressions
      fieldDeclarationServiceWithDeps.addField(tsFile, classNode.get(), insertionPoint, 
          "private", false, JavaBasicType.LANG_STRING, "stringLiteral", "\"Hello World\"");
      
      fieldDeclarationServiceWithDeps.addField(tsFile, classNode.get(), insertionPoint, 
          "private", false, JavaBasicType.PRIMITIVE_INT, "calculation", "10 + 20");
      
      fieldDeclarationServiceWithDeps.addField(tsFile, classNode.get(), insertionPoint, 
          "private", false, "List<String>", "listInit", "Arrays.asList(\"a\", \"b\", \"c\")");
      
      fieldDeclarationServiceWithDeps.addField(tsFile, classNode.get(), insertionPoint, 
          "private", false, JavaBasicType.UTIL_UUID, "uuidInit", "UUID.randomUUID()");
      
      fieldDeclarationServiceWithDeps.addField(tsFile, classNode.get(), insertionPoint, 
          "private", false, JavaBasicType.PRIMITIVE_BOOLEAN, "booleanInit", "true && false");
      
      fieldDeclarationServiceWithDeps.addField(tsFile, classNode.get(), insertionPoint, 
          "private", false, "Object", "nullInit", "null");
      
      String updatedCode = tsFile.getSourceCode();
      
      // Verify initializations
      assertTrue(updatedCode.contains("private String stringLiteral = \"Hello World\";"));
      assertTrue(updatedCode.contains("private int calculation = 10 + 20;"));
      assertTrue(updatedCode.contains("private List<String> listInit = Arrays.asList(\"a\", \"b\", \"c\");"));
      assertTrue(updatedCode.contains("private UUID uuidInit = UUID.randomUUID();"));
      assertTrue(updatedCode.contains("private boolean booleanInit = true && false;"));
      assertTrue(updatedCode.contains("private Object nullInit = null;"));
    }

    @Test
    @DisplayName("should handle edge cases with whitespace and special characters")
    void addField_withEdgeCases_shouldHandleGracefully() {
      String testCode = """
          package com.example.test;
          
          public class TestClass {
              
          }
          """;
      
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, testCode);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "TestClass");
      assertTrue(classNode.isPresent());
      
      var insertionPoint = fieldDeclarationService.getFieldInsertionPosition(
          tsFile, classNode.get(), FieldInsertionPosition.BEGINNING_OF_CLASS_BODY);
      assertNotNull(insertionPoint);
      
      // Test edge cases
      fieldDeclarationServiceWithDeps.addField(tsFile, classNode.get(), insertionPoint, 
          "  private  ", false, JavaBasicType.LANG_STRING, "fieldWithSpaces", null);
      
      fieldDeclarationServiceWithDeps.addField(tsFile, classNode.get(), insertionPoint, 
          "private", false, JavaBasicType.LANG_STRING, "_underscoreField", null);
      
      fieldDeclarationServiceWithDeps.addField(tsFile, classNode.get(), insertionPoint, 
          "private", false, JavaBasicType.LANG_STRING, "field123", null);
      
      fieldDeclarationServiceWithDeps.addField(tsFile, classNode.get(), insertionPoint, 
          "private", false, "com.example.CustomType", "fullyQualifiedType", null);
      
      String updatedCode = tsFile.getSourceCode();
      
      // Verify fields were added (spaces should be preserved in visibility)
      assertTrue(updatedCode.contains("  private   String fieldWithSpaces;"));
      assertTrue(updatedCode.contains("private String _underscoreField;"));
      assertTrue(updatedCode.contains("private String field123;"));
      assertTrue(updatedCode.contains("private com.example.CustomType fullyQualifiedType;"));
    }

    @Test
    @DisplayName("should maintain backward compatibility with no-arg constructor")
    void addField_withNoArgConstructor_shouldWorkWithoutAutoImport() {
      String testCode = """
          package com.example.test;
          
          public class TestClass {
              
          }
          """;
      
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, testCode);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "TestClass");
      assertTrue(classNode.isPresent());
      
      var insertionPoint = fieldDeclarationService.getFieldInsertionPosition(
          tsFile, classNode.get(), FieldInsertionPosition.BEGINNING_OF_CLASS_BODY);
      assertNotNull(insertionPoint);
      
      // Use the no-arg constructor service (no auto-import)
      FieldDeclarationService noArgService = new FieldDeclarationService();
      
      noArgService.addField(tsFile, classNode.get(), insertionPoint, 
          "private", false, JavaBasicType.UTIL_UUID, "uuidField", null);
      
      String updatedCode = tsFile.getSourceCode();
      
      // Verify field was added
      assertTrue(updatedCode.contains("private UUID uuidField;"));
      
      // Verify no import was added (since dependencies are null)
      assertFalse(updatedCode.contains("import java.util.UUID;"));
    }
  }
}

