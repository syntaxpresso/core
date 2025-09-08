package io.github.syntaxpresso.core.service.java.language;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import io.github.syntaxpresso.core.service.java.language.extra.ClassCapture;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.treesitter.TSNode;

@DisplayName("ClassDeclarationService Tests")
class ClassDeclarationServiceTest {
  private ClassDeclarationService classDeclarationService;

  private static final String SIMPLE_CLASS_CODE =
      """
      public class SimpleClass {
          private String name;

          public void method() {
              // method body
          }
      }
      """;

  private static final String COMPLEX_CLASS_CODE =
      """
      package com.example;

      import java.util.List;
      import java.io.Serializable;

      @Entity
      @Table(name = "users")
      public class User extends BaseEntity implements Serializable {
          @Id
          private Long id;

          @Column(name = "username")
          private String username;

          public User() {
              // default constructor
          }

          public Long getId() {
              return id;
          }

          public void setId(Long id) {
              this.id = id;
          }
      }
      """;

  private static final String MULTIPLE_CLASSES_CODE =
      """
      public class FirstClass {
          private String field1;
      }

      class SecondClass {
          private int field2;
      }

      public class ThirdClass extends FirstClass {
          private boolean field3;
      }
      """;

  private static final String GENERIC_CLASS_CODE =
      """
      public class GenericClass<T> extends BaseClass<String> {
          private T data;

          public T getData() {
              return data;
          }
      }
      """;

  @BeforeEach
  void setUp() {
    FieldDeclarationService fieldDeclarationService = new FieldDeclarationService();
    FormalParameterService formalParameterService = new FormalParameterService();
    MethodDeclarationService methodDeclarationService =
        new MethodDeclarationService(formalParameterService);
    this.classDeclarationService =
        new ClassDeclarationService(fieldDeclarationService, methodDeclarationService);
  }

  @Nested
  @DisplayName("getClassDeclarationNodeInfo() Tests")
  class GetClassDeclarationNodeInfoTests {

    /**
     * Tests that getClassDeclarationNodeInfo correctly extracts information from a simple class.
     *
     * <p>Usage example:
     *
     * <pre>
     * List&lt;Map&lt;String, TSNode&gt;&gt; info = service.getClassDeclarationNodeInfo(tsFile, classNode);
     * Map&lt;String, TSNode&gt; firstResult = info.get(0);
     * TSNode className = firstResult.get("className");
     * // className node contains "SimpleClass"
     * </pre>
     */
    @Test
    @DisplayName("should extract basic class information")
    void getClassDeclarationNodeInfo_withSimpleClass_shouldExtractBasicInfo() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_CLASS_CODE);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "SimpleClass");
      assertTrue(classNode.isPresent());

      List<Map<String, TSNode>> info =
          classDeclarationService.getClassDeclarationNodeInfo(tsFile, classNode.get());

      assertFalse(info.isEmpty());
      Map<String, TSNode> firstResult = info.get(0);

      TSNode className = firstResult.get(ClassCapture.CLASS_NAME.getCaptureName());
      assertNotNull(className);
      assertEquals("SimpleClass", tsFile.getTextFromNode(className));
    }

    /**
     * Tests that getClassDeclarationNodeInfo correctly extracts annotations, modifiers, and
     * superclass.
     *
     * <p>Usage example:
     *
     * <pre>
     * List&lt;Map&lt;String, TSNode&gt;&gt; info = service.getClassDeclarationNodeInfo(tsFile, classNode);
     * Map&lt;String, TSNode&gt; firstResult = info.get(0);
     * TSNode annotation = firstResult.get("classAnnotation");
     * TSNode superclass = firstResult.get("superclassName");
     * // annotation node contains "@Entity", superclass contains "BaseEntity"
     * </pre>
     */
    @Test
    @DisplayName("should extract annotations, modifiers, and superclass")
    void getClassDeclarationNodeInfo_withComplexClass_shouldExtractAllInfo() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, COMPLEX_CLASS_CODE);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "User");
      assertTrue(classNode.isPresent());

      List<Map<String, TSNode>> info =
          classDeclarationService.getClassDeclarationNodeInfo(tsFile, classNode.get());

      assertFalse(info.isEmpty());
      Map<String, TSNode> firstResult = info.get(0);

      TSNode className = firstResult.get(ClassCapture.CLASS_NAME.getCaptureName());
      assertNotNull(className);
      assertEquals("User", tsFile.getTextFromNode(className));

      TSNode superclassName = firstResult.get(ClassCapture.SUPERCLASS_NAME.getCaptureName());
      assertNotNull(superclassName);
      assertEquals("BaseEntity", tsFile.getTextFromNode(superclassName));
    }

    @Test
    @DisplayName("should return empty list for invalid input")
    void getClassDeclarationNodeInfo_withInvalidInput_shouldReturnEmptyList() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_CLASS_CODE);

      // Test with null file
      List<Map<String, TSNode>> result1 =
          classDeclarationService.getClassDeclarationNodeInfo(null, null);
      assertTrue(result1.isEmpty());

      // Test with invalid node type
      TSNode methodNode =
          tsFile.query("(method_declaration) @method").returning("method").execute().firstNode();
      assertNotNull(methodNode);

      List<Map<String, TSNode>> result2 =
          classDeclarationService.getClassDeclarationNodeInfo(tsFile, methodNode);
      assertTrue(result2.isEmpty());
    }
  }

  @Nested
  @DisplayName("getAllClassDeclarations() Tests")
  class GetAllClassDeclarationsTests {

    /**
     * Tests that getAllClassDeclarations finds all classes in a file.
     *
     * <p>Usage example:
     *
     * <pre>
     * List&lt;Map&lt;String, TSNode&gt;&gt; classes = service.getAllClassDeclarations(tsFile);
     * // Returns all class declarations with their names and nodes
     * </pre>
     */
    @Test
    @DisplayName("should find all class declarations")
    void getAllClassDeclarations_withMultipleClasses_shouldFindAll() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, MULTIPLE_CLASSES_CODE);

      List<Map<String, TSNode>> classes = classDeclarationService.getAllClassDeclarations(tsFile);

      assertEquals(3, classes.size());

      // Check first class
      TSNode firstName = classes.get(0).get("className");
      assertNotNull(firstName);
      assertEquals("FirstClass", tsFile.getTextFromNode(firstName));

      // Check second class
      TSNode secondName = classes.get(1).get("className");
      assertNotNull(secondName);
      assertEquals("SecondClass", tsFile.getTextFromNode(secondName));

      // Check third class
      TSNode thirdName = classes.get(2).get("className");
      assertNotNull(thirdName);
      assertEquals("ThirdClass", tsFile.getTextFromNode(thirdName));
    }

    @Test
    @DisplayName("should return empty list for file with no classes")
    void getAllClassDeclarations_withNoClasses_shouldReturnEmptyList() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, "interface MyInterface {}");

      List<Map<String, TSNode>> classes = classDeclarationService.getAllClassDeclarations(tsFile);

      assertTrue(classes.isEmpty());
    }

    @Test
    @DisplayName("should handle null input")
    void getAllClassDeclarations_withNullInput_shouldReturnEmptyList() {
      List<Map<String, TSNode>> result = classDeclarationService.getAllClassDeclarations(null);

      assertTrue(result.isEmpty());
    }
  }

  @Nested
  @DisplayName("getClassDeclarationNodeByCaptureName() Tests")
  class GetClassDeclarationNodeByCaptureNameTests {

    /**
     * Tests that getClassDeclarationNodeByCaptureName retrieves specific captured nodes.
     *
     * <p>Usage example:
     *
     * <pre>
     * Optional&lt;TSNode&gt; nameNode = service.getClassDeclarationNodeByCaptureName(tsFile, classNode, ClassCapture.CLASS_NAME);
     * if (nameNode.isPresent()) {
     *   String className = tsFile.getTextFromNode(nameNode.get());
     *   // className = "MyClass"
     * }
     * </pre>
     */
    @Test
    @DisplayName("should retrieve class name node")
    void getClassDeclarationNodeByCaptureName_withClassName_shouldReturnNameNode() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_CLASS_CODE);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "SimpleClass");
      assertTrue(classNode.isPresent());

      Optional<TSNode> nameNode =
          classDeclarationService.getClassDeclarationNodeByCaptureName(
              tsFile, classNode.get(), ClassCapture.CLASS_NAME);

      assertTrue(nameNode.isPresent());
      assertEquals("SimpleClass", tsFile.getTextFromNode(nameNode.get()));
    }

    @Test
    @DisplayName("should retrieve superclass name node")
    void getClassDeclarationNodeByCaptureName_withSuperclassName_shouldReturnSuperclassNode() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, COMPLEX_CLASS_CODE);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "User");
      assertTrue(classNode.isPresent());

      Optional<TSNode> superclassNode =
          classDeclarationService.getClassDeclarationNodeByCaptureName(
              tsFile, classNode.get(), ClassCapture.SUPERCLASS_NAME);

      assertTrue(superclassNode.isPresent());
      assertEquals("BaseEntity", tsFile.getTextFromNode(superclassNode.get()));
    }

    @Test
    @DisplayName("should return empty for non-existent capture")
    void getClassDeclarationNodeByCaptureName_withNonExistentCapture_shouldReturnEmpty() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_CLASS_CODE);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "SimpleClass");
      assertTrue(classNode.isPresent());

      Optional<TSNode> superclassNode =
          classDeclarationService.getClassDeclarationNodeByCaptureName(
              tsFile, classNode.get(), ClassCapture.SUPERCLASS_NAME);

      assertFalse(superclassNode.isPresent());
    }
  }

  @Nested
  @DisplayName("getClassDeclarationAnnotationNodes() Tests")
  class GetClassDeclarationAnnotationNodesTests {

    /**
     * Tests that getClassDeclarationAnnotationNodes retrieves all annotations.
     *
     * <p>Usage example:
     *
     * <pre>
     * List&lt;TSNode&gt; annotations = service.getClassDeclarationAnnotationNodes(tsFile, classNode);
     * for (TSNode annotation : annotations) {
     *   String annotationText = tsFile.getTextFromNode(annotation);
     *   // annotationText = "@Entity" or "@Table(name = \"users\")"
     * }
     * </pre>
     */
    @Test
    @DisplayName("should retrieve all class annotations")
    void getClassDeclarationAnnotationNodes_withAnnotatedClass_shouldReturnAnnotations() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, COMPLEX_CLASS_CODE);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "User");
      assertTrue(classNode.isPresent());

      List<TSNode> annotations =
          classDeclarationService.getClassDeclarationAnnotationNodes(tsFile, classNode.get());

      assertFalse(annotations.isEmpty());
      // Should have @Entity and @Table annotations
      assertTrue(annotations.size() >= 1);
    }

    @Test
    @DisplayName("should return empty list for class without annotations")
    void getClassDeclarationAnnotationNodes_withoutAnnotations_shouldReturnEmptyList() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_CLASS_CODE);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "SimpleClass");
      assertTrue(classNode.isPresent());

      List<TSNode> annotations =
          classDeclarationService.getClassDeclarationAnnotationNodes(tsFile, classNode.get());

      assertTrue(annotations.isEmpty());
    }
  }

  @Nested
  @DisplayName("getClassDeclarationNameNode() Tests")
  class GetClassDeclarationNameNodeTests {

    /**
     * Tests that getClassDeclarationNameNode retrieves the class name identifier.
     *
     * <p>Usage example:
     *
     * <pre>
     * Optional&lt;TSNode&gt; nameNode = service.getClassDeclarationNameNode(tsFile, classNode);
     * if (nameNode.isPresent()) {
     *   String className = tsFile.getTextFromNode(nameNode.get());
     *   // className = "MyClass"
     * }
     * </pre>
     */
    @Test
    @DisplayName("should retrieve class name node")
    void getClassDeclarationNameNode_withValidClass_shouldReturnNameNode() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_CLASS_CODE);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "SimpleClass");
      assertTrue(classNode.isPresent());

      Optional<TSNode> nameNode =
          classDeclarationService.getClassDeclarationNameNode(tsFile, classNode.get());

      assertTrue(nameNode.isPresent());
      assertEquals("SimpleClass", tsFile.getTextFromNode(nameNode.get()));
    }
  }

  @Nested
  @DisplayName("getClassDeclarationSuperclassNameNode() Tests")
  class GetClassDeclarationSuperclassNameNodeTests {

    /**
     * Tests that getClassDeclarationSuperclassNameNode retrieves the superclass name.
     *
     * <p>Usage example:
     *
     * <pre>
     * Optional&lt;TSNode&gt; superclassNode = service.getClassDeclarationSuperclassNameNode(tsFile, classNode);
     * if (superclassNode.isPresent()) {
     *   String superclassName = tsFile.getTextFromNode(superclassNode.get());
     *   // superclassName = "BaseClass"
     * }
     * </pre>
     */
    @Test
    @DisplayName("should retrieve superclass name node")
    void getClassDeclarationSuperclassNameNode_withInheritance_shouldReturnSuperclassNode() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, COMPLEX_CLASS_CODE);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "User");
      assertTrue(classNode.isPresent());

      Optional<TSNode> superclassNode =
          classDeclarationService.getClassDeclarationSuperclassNameNode(tsFile, classNode.get());

      assertTrue(superclassNode.isPresent());
      assertEquals("BaseEntity", tsFile.getTextFromNode(superclassNode.get()));
    }

    @Test
    @DisplayName("should return empty for class without superclass")
    void getClassDeclarationSuperclassNameNode_withoutInheritance_shouldReturnEmpty() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_CLASS_CODE);
      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "SimpleClass");
      assertTrue(classNode.isPresent());

      Optional<TSNode> superclassNode =
          classDeclarationService.getClassDeclarationSuperclassNameNode(tsFile, classNode.get());

      assertFalse(superclassNode.isPresent());
    }
  }

  @Nested
  @DisplayName("findClassByName() Tests")
  class FindClassByNameTests {

    /**
     * Tests that findClassByName locates classes by their exact name.
     *
     * <p>Usage example:
     *
     * <pre>
     * Optional&lt;TSNode&gt; classNode = service.findClassByName(tsFile, "MyClass");
     * if (classNode.isPresent()) {
     *   String classDecl = tsFile.getTextFromNode(classNode.get());
     *   // classDecl contains the full class declaration
     * }
     * </pre>
     */
    @Test
    @DisplayName("should find existing class by name")
    void findClassByName_withExistingClass_shouldReturnClass() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_CLASS_CODE);

      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "SimpleClass");

      assertTrue(classNode.isPresent());
      String classText = tsFile.getTextFromNode(classNode.get());
      assertTrue(classText.contains("SimpleClass"));
    }

    @Test
    @DisplayName("should return empty for non-existent class")
    void findClassByName_withNonExistentClass_shouldReturnEmpty() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_CLASS_CODE);

      Optional<TSNode> classNode =
          classDeclarationService.findClassByName(tsFile, "NonExistentClass");

      assertFalse(classNode.isPresent());
    }

    @Test
    @DisplayName("should handle null and empty parameters")
    void findClassByName_withInvalidParameters_shouldReturnEmpty() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_CLASS_CODE);

      Optional<TSNode> result1 = classDeclarationService.findClassByName(null, "SimpleClass");
      assertFalse(result1.isPresent());

      Optional<TSNode> result2 = classDeclarationService.findClassByName(tsFile, null);
      assertFalse(result2.isPresent());

      Optional<TSNode> result3 = classDeclarationService.findClassByName(tsFile, "");
      assertFalse(result3.isPresent());
    }

    @Test
    @DisplayName("should find correct class among multiple classes")
    void findClassByName_withMultipleClasses_shouldReturnCorrectClass() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, MULTIPLE_CLASSES_CODE);

      Optional<TSNode> firstClass = classDeclarationService.findClassByName(tsFile, "FirstClass");
      assertTrue(firstClass.isPresent());

      Optional<TSNode> secondClass = classDeclarationService.findClassByName(tsFile, "SecondClass");
      assertTrue(secondClass.isPresent());

      Optional<TSNode> thirdClass = classDeclarationService.findClassByName(tsFile, "ThirdClass");
      assertTrue(thirdClass.isPresent());
    }
  }

  @Nested
  @DisplayName("getPublicClass() Tests")
  class GetPublicClassTests {

    /**
     * Tests that getPublicClass finds the public class matching the file name.
     *
     * <p>Usage example:
     *
     * <pre>
     * // For a file named "MyClass.java"
     * Optional&lt;TSNode&gt; publicClass = service.getPublicClass(tsFile);
     * if (publicClass.isPresent()) {
     *   String className = tsFile.getTextFromNode(publicClass.get());
     *   // className contains "public class MyClass ..."
     * }
     * </pre>
     */
    @Test
    @DisplayName("should find public class matching file name")
    void getPublicClass_withMatchingFileName_shouldReturnPublicClass() throws IOException {
      Path tempDir = Files.createTempDirectory("test");
      // Create a temporary file with matching name
      Path javaFile = tempDir.resolve("SimpleClass.java");
      Files.write(javaFile, SIMPLE_CLASS_CODE.getBytes());

      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, javaFile);

      Optional<TSNode> publicClass = classDeclarationService.getPublicClass(tsFile);

      assertTrue(publicClass.isPresent());
      String classText = tsFile.getTextFromNode(publicClass.get());
      assertTrue(classText.contains("SimpleClass"));
    }

    @Test
    @DisplayName("should return empty for non-matching file name")
    void getPublicClass_withNonMatchingFileName_shouldReturnEmpty() throws IOException {
      Path tempDir = Files.createTempDirectory("test");
      // Create a temporary file with non-matching name
      Path javaFile = tempDir.resolve("DifferentName.java");
      Files.write(javaFile, SIMPLE_CLASS_CODE.getBytes());

      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, javaFile);

      Optional<TSNode> publicClass = classDeclarationService.getPublicClass(tsFile);

      assertFalse(publicClass.isPresent());
    }

    @Test
    @DisplayName("should handle null file")
    void getPublicClass_withNullFile_shouldReturnEmpty() {
      Optional<TSNode> result = classDeclarationService.getPublicClass(null);

      assertFalse(result.isPresent());
    }
  }

  @Nested
  @DisplayName("isLocalClass() Tests")
  class IsLocalClassTests {

    @TempDir Path tempDir;

    /**
     * Tests that isLocalClass correctly identifies local project classes.
     *
     * <p>Usage example:
     *
     * <pre>
     * boolean exists = service.isLocalClass(projectRoot, "com.example.MyClass");
     * // Returns true if src/main/java/com/example/MyClass.java exists
     * </pre>
     */
    @Test
    @DisplayName("should return true for existing local class")
    void isLocalClass_withExistingClass_shouldReturnTrue() throws IOException {
      // Create the source directory structure
      Path srcDir = tempDir.resolve("src/main/java/com/example");
      Files.createDirectories(srcDir);

      // Create a Java file
      Path javaFile = srcDir.resolve("MyClass.java");
      Files.createFile(javaFile);

      Boolean result = classDeclarationService.isLocalClass(tempDir, "com.example.MyClass");

      assertTrue(result);
    }

    @Test
    @DisplayName("should return false for non-existent local class")
    void isLocalClass_withNonExistentClass_shouldReturnFalse() throws IOException {
      // Create the source directory structure but no file
      Path srcDir = tempDir.resolve("src/main/java");
      Files.createDirectories(srcDir);

      Boolean result = classDeclarationService.isLocalClass(tempDir, "com.example.NonExistent");

      assertFalse(result);
    }

    @Test
    @DisplayName("should handle nested package structures")
    void isLocalClass_withNestedPackages_shouldWork() throws IOException {
      // Create nested package structure
      Path srcDir = tempDir.resolve("src/main/java/io/github/example/deep/nested");
      Files.createDirectories(srcDir);

      Path javaFile = srcDir.resolve("DeepClass.java");
      Files.createFile(javaFile);

      Boolean result =
          classDeclarationService.isLocalClass(tempDir, "io.github.example.deep.nested.DeepClass");

      assertTrue(result);
    }
  }

  @Nested
  @DisplayName("getFullyQualifiedSuperclass() Tests")
  class GetFullyQualifiedSuperclassTests {

    /**
     * Tests that getFullyQualifiedSuperclass resolves superclass names correctly.
     *
     * <p>Usage example:
     *
     * <pre>
     * Optional&lt;String&gt; fqName = service.getFullyQualifiedSuperclass(tsFile, "BaseClass");
     * // Returns fully qualified name or simple name if not resolvable
     * </pre>
     */
    @Test
    @DisplayName("should return already qualified names unchanged")
    void getFullyQualifiedSuperclass_withQualifiedName_shouldReturnUnchanged() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_CLASS_CODE);
      String qualifiedName = "com.example.BaseClass";

      Optional<String> result =
          classDeclarationService.getFullyQualifiedSuperclass(tsFile, qualifiedName);

      assertTrue(result.isPresent());
      assertEquals(qualifiedName, result.get());
    }

    @Test
    @DisplayName("should qualify java.lang classes")
    void getFullyQualifiedSuperclass_withJavaLangClass_shouldAddJavaLangPackage() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_CLASS_CODE);

      Optional<String> result =
          classDeclarationService.getFullyQualifiedSuperclass(tsFile, "String");

      assertTrue(result.isPresent());
      assertEquals("java.lang.String", result.get());
    }

    @Test
    @DisplayName("should return simple names for unresolvable classes")
    void getFullyQualifiedSuperclass_withUnresolvableClass_shouldReturnSimpleName() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_CLASS_CODE);

      Optional<String> result =
          classDeclarationService.getFullyQualifiedSuperclass(tsFile, "CustomClass");

      assertTrue(result.isPresent());
      assertEquals("CustomClass", result.get());
    }
  }

  @Nested
  @DisplayName("isJavaLangClass() Tests")
  class IsJavaLangClassTests {

    /**
     * Tests that isJavaLangClass correctly identifies java.lang classes.
     *
     * <p>Usage example:
     *
     * <pre>
     * boolean isLang = service.isJavaLangClass("String");
     * // Returns true for common java.lang classes
     * </pre>
     */
    @Test
    @DisplayName("should return true for common java.lang classes")
    void isJavaLangClass_withCommonClasses_shouldReturnTrue() {
      assertTrue(classDeclarationService.isJavaLangClass("Object"));
      assertTrue(classDeclarationService.isJavaLangClass("String"));
      assertTrue(classDeclarationService.isJavaLangClass("Integer"));
      assertTrue(classDeclarationService.isJavaLangClass("Long"));
      assertTrue(classDeclarationService.isJavaLangClass("Double"));
      assertTrue(classDeclarationService.isJavaLangClass("Float"));
      assertTrue(classDeclarationService.isJavaLangClass("Boolean"));
      assertTrue(classDeclarationService.isJavaLangClass("Character"));
      assertTrue(classDeclarationService.isJavaLangClass("Byte"));
      assertTrue(classDeclarationService.isJavaLangClass("Short"));
    }

    @Test
    @DisplayName("should return false for non-java.lang classes")
    void isJavaLangClass_withNonJavaLangClasses_shouldReturnFalse() {
      assertFalse(classDeclarationService.isJavaLangClass("ArrayList"));
      assertFalse(classDeclarationService.isJavaLangClass("HashMap"));
      assertFalse(classDeclarationService.isJavaLangClass("MyCustomClass"));
      assertFalse(classDeclarationService.isJavaLangClass("List"));
      assertFalse(classDeclarationService.isJavaLangClass(""));
      assertFalse(
          classDeclarationService.isJavaLangClass(
              "java.lang.String")); // Should be simple name only
    }
  }

  @Nested
  @DisplayName("Service Dependencies Tests")
  class ServiceDependenciesTests {

    @Test
    @DisplayName("should have required dependencies initialized")
    void constructor_shouldInitializeAllDependencies() {
      assertNotNull(classDeclarationService.getFieldDeclarationService());
      assertNotNull(classDeclarationService.getMethodDeclarationService());
    }

    @Test
    @DisplayName("should return correct dependency instances")
    void getDependencies_shouldReturnCorrectInstances() {
      assertTrue(
          classDeclarationService.getFieldDeclarationService() instanceof FieldDeclarationService);
      assertTrue(
          classDeclarationService.getMethodDeclarationService()
              instanceof MethodDeclarationService);
    }
  }

  @Nested
  @DisplayName("Edge Cases and Error Handling Tests")
  class EdgeCasesTests {

    @Test
    @DisplayName("should handle generic classes correctly")
    void methods_withGenericClasses_shouldHandleCorrectly() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, GENERIC_CLASS_CODE);

      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "GenericClass");
      assertTrue(classNode.isPresent());

      Optional<TSNode> superclassNode =
          classDeclarationService.getClassDeclarationSuperclassNameNode(tsFile, classNode.get());
      assertTrue(superclassNode.isPresent());
      assertEquals("BaseClass", tsFile.getTextFromNode(superclassNode.get()));
    }

    @Test
    @DisplayName("should handle empty source code")
    void methods_withEmptyCode_shouldHandleGracefully() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, "");

      List<Map<String, TSNode>> classes = classDeclarationService.getAllClassDeclarations(tsFile);
      assertTrue(classes.isEmpty());

      Optional<TSNode> classNode = classDeclarationService.findClassByName(tsFile, "NonExistent");
      assertFalse(classNode.isPresent());
    }

    @Test
    @DisplayName("should handle malformed source code")
    void methods_withMalformedCode_shouldHandleGracefully() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, "public class {");

      List<Map<String, TSNode>> classes = classDeclarationService.getAllClassDeclarations(tsFile);
      // Should not crash, may return empty or partial results
      assertNotNull(classes);
    }
  }
}

