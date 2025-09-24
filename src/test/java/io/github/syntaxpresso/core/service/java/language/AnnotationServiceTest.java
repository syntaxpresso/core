package io.github.syntaxpresso.core.service.java.language;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import io.github.syntaxpresso.core.service.java.language.extra.AnnotationArgument;
import io.github.syntaxpresso.core.service.java.language.extra.AnnotationInsertionPoint;
import io.github.syntaxpresso.core.service.java.language.extra.AnnotationInsertionPoint.AnnotationInsertionPosition;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.treesitter.TSNode;

@DisplayName("AnnotationService Tests")
class AnnotationServiceTest {
  private AnnotationService annotationService;

  @BeforeEach
  void setUp() {
    this.annotationService = new AnnotationService();
  }

  @Nested
  @DisplayName("Basic Functionality Tests")
  class BasicFunctionalityTests {

    @Test
    @DisplayName("should find marker annotation")
    void shouldFindMarkerAnnotation() {
      String code = "@Override public void method() {}";
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, code);
      TSNode methodNode = tsFile.query("(method_declaration) @method").execute().firstNode();

      List<TSNode> annotations = annotationService.getAllAnnotations(tsFile, methodNode);

      assertEquals(1, annotations.size());
    }

    @Test
    @DisplayName("should find annotation with arguments")
    void shouldFindAnnotationWithArguments() {
      String code = "@Table(name = \"users\") public class User {}";
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, code);
      TSNode classNode = tsFile.query("(class_declaration) @class").execute().firstNode();

      List<TSNode> annotations = annotationService.getAllAnnotations(tsFile, classNode);

      assertEquals(1, annotations.size());
    }

    @Test
    @DisplayName("should get annotation name node")
    void shouldGetAnnotationNameNode() {
      String code = "@Override public void method() {}";
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, code);
      TSNode annotationNode = tsFile.query("(marker_annotation) @annotation").execute().firstNode();

      Optional<TSNode> nameNode = annotationService.getAnnotationNameNode(tsFile, annotationNode);

      assertTrue(nameNode.isPresent());
      assertEquals("Override", tsFile.getTextFromNode(nameNode.get()));
    }

    @Test
    @DisplayName("should find annotation by name")
    void shouldFindAnnotationByName() {
      String code = "@Entity @Table(name = \"users\") public class User {}";
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, code);
      TSNode classNode = tsFile.query("(class_declaration) @class").execute().firstNode();

      Optional<TSNode> entityAnnotation = annotationService.findAnnotationByName(tsFile, classNode, "Entity");
      Optional<TSNode> tableAnnotation = annotationService.findAnnotationByName(tsFile, classNode, "Table");

      assertTrue(entityAnnotation.isPresent());
      assertTrue(tableAnnotation.isPresent());
    }

    @Test
    @DisplayName("should return empty for non-existent annotation")
    void shouldReturnEmptyForNonExistentAnnotation() {
      String code = "@Entity public class User {}";
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, code);
      TSNode classNode = tsFile.query("(class_declaration) @class").execute().firstNode();

      Optional<TSNode> result = annotationService.findAnnotationByName(tsFile, classNode, "NonExistent");

      assertFalse(result.isPresent());
    }
  }

  @Nested
  @DisplayName("Annotation Arguments Tests")
  class AnnotationArgumentsTests {

    @Test
    @DisplayName("should get annotation argument pairs")
    void shouldGetAnnotationArgumentPairs() {
      String code = "@Table(name = \"users\", schema = \"public\") public class User {}";
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, code);
      TSNode annotationNode = tsFile.query("(annotation) @annotation").execute().firstNode();

      List<TSNode> argumentPairs = annotationService.getAnnotationArgumentPairs(tsFile, annotationNode);

      assertEquals(2, argumentPairs.size());
    }

    @Test
    @DisplayName("should get annotation keys")
    void shouldGetAnnotationKeys() {
      String code = "@Table(name = \"users\", schema = \"public\") public class User {}";
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, code);
      TSNode annotationNode = tsFile.query("(annotation) @annotation").execute().firstNode();

      List<TSNode> keys = annotationService.getAnnotationKeys(tsFile, annotationNode);

      assertEquals(2, keys.size());
      List<String> keyTexts = keys.stream().map(tsFile::getTextFromNode).toList();
      assertTrue(keyTexts.contains("name"));
      assertTrue(keyTexts.contains("schema"));
    }

    @Test
    @DisplayName("should get annotation values")
    void shouldGetAnnotationValues() {
      String code = "@Table(name = \"users\", schema = \"public\") public class User {}";
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, code);
      TSNode annotationNode = tsFile.query("(annotation) @annotation").execute().firstNode();

      List<TSNode> values = annotationService.getAnnotationValues(tsFile, annotationNode);

      assertEquals(2, values.size());
      List<String> valueTexts = values.stream().map(tsFile::getTextFromNode).toList();
      assertTrue(valueTexts.contains("\"users\""));
      assertTrue(valueTexts.contains("\"public\""));
    }

    @Test
    @DisplayName("should get annotation value by key")
    void shouldGetAnnotationValueByKey() {
      String code = "@Table(name = \"users\", schema = \"public\") public class User {}";
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, code);
      TSNode annotationNode = tsFile.query("(annotation) @annotation").execute().firstNode();

      Optional<TSNode> nameValue = annotationService.getAnnotationValueByKey(tsFile, annotationNode, "name");
      Optional<TSNode> schemaValue = annotationService.getAnnotationValueByKey(tsFile, annotationNode, "schema");

      assertTrue(nameValue.isPresent());
      assertEquals("\"users\"", tsFile.getTextFromNode(nameValue.get()));
      assertTrue(schemaValue.isPresent());
      assertEquals("\"public\"", tsFile.getTextFromNode(schemaValue.get()));
    }

    @Test
    @DisplayName("should get annotation arguments as map")
    void shouldGetAnnotationArgumentsAsMap() {
      String code = "@Table(name = \"users\", schema = \"public\") public class User {}";
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, code);
      TSNode annotationNode = tsFile.query("(annotation) @annotation").execute().firstNode();

      Map<String, AnnotationArgument> arguments = annotationService.getAnnotationArguments(tsFile, annotationNode);

      assertEquals(2, arguments.size());
      assertTrue(arguments.containsKey("name"));
      assertTrue(arguments.containsKey("schema"));

      AnnotationArgument nameArg = arguments.get("name");
      assertNotNull(nameArg);
      assertEquals("name", nameArg.getKey(tsFile));
      assertEquals("\"users\"", nameArg.getValue(tsFile));
    }
  }

  @Nested
  @DisplayName("Insertion Position Tests")
  class InsertionPositionTests {

    @Test
    @DisplayName("should get insertion position before first annotation")
    void shouldGetInsertionPositionBeforeFirstAnnotation() {
      String code = "@Entity @Table(name = \"users\") public class User {}";
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, code);
      TSNode classNode = tsFile.query("(class_declaration) @class").execute().firstNode();

      AnnotationInsertionPoint insertionPoint = annotationService.getAnnotationInsertionPosition(
          tsFile, classNode, AnnotationInsertionPosition.BEFORE_FIRST_ANNOTATION);

      assertNotNull(insertionPoint);
      assertEquals(AnnotationInsertionPosition.BEFORE_FIRST_ANNOTATION, insertionPoint.getPosition());
      assertTrue(insertionPoint.getInsertByte() >= 0);
    }

    @Test
    @DisplayName("should get insertion position above scope declaration")
    void shouldGetInsertionPositionAboveScopeDeclaration() {
      String code = "@Entity public class User {}";
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, code);
      TSNode classNode = tsFile.query("(class_declaration) @class").execute().firstNode();

      AnnotationInsertionPoint insertionPoint = annotationService.getAnnotationInsertionPosition(
          tsFile, classNode, AnnotationInsertionPosition.ABOVE_SCOPE_DECLARATION);

      assertNotNull(insertionPoint);
      assertEquals(AnnotationInsertionPosition.ABOVE_SCOPE_DECLARATION, insertionPoint.getPosition());
      assertTrue(insertionPoint.getInsertByte() >= 0);
    }

    @Test
    @DisplayName("should handle class without existing annotations")
    void shouldHandleClassWithoutExistingAnnotations() {
      String code = "public class Simple {}";
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, code);
      TSNode classNode = tsFile.query("(class_declaration) @class").execute().firstNode();

      AnnotationInsertionPoint insertionPoint = annotationService.getAnnotationInsertionPosition(
          tsFile, classNode, AnnotationInsertionPosition.BEFORE_FIRST_ANNOTATION);

      assertNotNull(insertionPoint);
      assertEquals(AnnotationInsertionPosition.BEFORE_FIRST_ANNOTATION, insertionPoint.getPosition());
    }

    @Test
    @DisplayName("should return null for invalid inputs")
    void shouldReturnNullForInvalidInputs() {
      String code = "public class User {}";
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, code);
      TSNode identifierNode = tsFile.query("(identifier) @id").execute().firstNode();

      AnnotationInsertionPoint nullTsFileResult = annotationService.getAnnotationInsertionPosition(
          null, null, AnnotationInsertionPosition.BEFORE_FIRST_ANNOTATION);
      AnnotationInsertionPoint invalidNodeResult = annotationService.getAnnotationInsertionPosition(
          tsFile, identifierNode, AnnotationInsertionPosition.BEFORE_FIRST_ANNOTATION);

      assertNull(nullTsFileResult);
      assertNull(invalidNodeResult);
    }
  }

  @Nested
  @DisplayName("Add Annotation Tests")
  class AddAnnotationTests {

    @Test
    @DisplayName("should add annotation above scope declaration")
    void shouldAddAnnotationAboveScopeDeclaration() {
      String code = "public class Simple {}";
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, code);
      TSNode classNode = tsFile.query("(class_declaration) @class").execute().firstNode();
      
      AnnotationInsertionPoint insertionPoint = annotationService.getAnnotationInsertionPosition(
          tsFile, classNode, AnnotationInsertionPosition.ABOVE_SCOPE_DECLARATION);
      
      String originalCode = tsFile.getSourceCode();
      annotationService.addAnnotation(tsFile, classNode, insertionPoint, "@Entity");
      String updatedCode = tsFile.getSourceCode();

      assertTrue(updatedCode.contains("@Entity"));
      assertTrue(updatedCode.length() > originalCode.length());
    }

    @Test
    @DisplayName("should handle null inputs gracefully")
    void shouldHandleNullInputsGracefully() {
      String code = "public class User {}";
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, code);
      TSNode classNode = tsFile.query("(class_declaration) @class").execute().firstNode();
      AnnotationInsertionPoint insertionPoint = annotationService.getAnnotationInsertionPosition(
          tsFile, classNode, AnnotationInsertionPosition.ABOVE_SCOPE_DECLARATION);

      String originalCode = tsFile.getSourceCode();

      // These should not modify the source code
      annotationService.addAnnotation(null, classNode, insertionPoint, "@Test");
      annotationService.addAnnotation(tsFile, null, insertionPoint, "@Test");
      annotationService.addAnnotation(tsFile, classNode, null, "@Test");
      annotationService.addAnnotation(tsFile, classNode, insertionPoint, null);
      annotationService.addAnnotation(tsFile, classNode, insertionPoint, "");

      String finalCode = tsFile.getSourceCode();
      assertEquals(originalCode, finalCode);
    }
  }

  @Nested
  @DisplayName("Interface Annotation Tests")
  class InterfaceAnnotationTests {

    @Test
    @DisplayName("should find annotations on interfaces")
    void shouldFindAnnotationsOnInterfaces() {
      String code = "@Repository @Component public interface UserRepository {}";
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, code);
      TSNode interfaceNode = tsFile.query("(interface_declaration) @interface").execute().firstNode();

      List<TSNode> annotations = annotationService.getAllAnnotations(tsFile, interfaceNode);

      assertEquals(2, annotations.size());
    }

    @Test
    @DisplayName("should find specific annotation on interface by name")
    void shouldFindSpecificAnnotationOnInterfaceByName() {
      String code = "@Repository @Component public interface UserRepository {}";
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, code);
      TSNode interfaceNode = tsFile.query("(interface_declaration) @interface").execute().firstNode();

      Optional<TSNode> repositoryAnnotation = annotationService.findAnnotationByName(tsFile, interfaceNode, "Repository");
      Optional<TSNode> componentAnnotation = annotationService.findAnnotationByName(tsFile, interfaceNode, "Component");
      Optional<TSNode> nonExistentAnnotation = annotationService.findAnnotationByName(tsFile, interfaceNode, "NonExistent");

      assertTrue(repositoryAnnotation.isPresent());
      assertTrue(componentAnnotation.isPresent());
      assertFalse(nonExistentAnnotation.isPresent());
    }

    @Test
    @DisplayName("should handle interface annotations with arguments")
    void shouldHandleInterfaceAnnotationsWithArguments() {
      String code = "@Component(value = \"userRepo\") public interface UserRepository {}";
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, code);
      TSNode interfaceNode = tsFile.query("(interface_declaration) @interface").execute().firstNode();

      Optional<TSNode> annotation = annotationService.findAnnotationByName(tsFile, interfaceNode, "Component");

      assertTrue(annotation.isPresent());
      
      Map<String, AnnotationArgument> arguments = annotationService.getAnnotationArguments(tsFile, annotation.get());
      assertEquals(1, arguments.size());
      assertTrue(arguments.containsKey("value"));
      assertEquals("\"userRepo\"", arguments.get("value").getValue(tsFile));
    }

    @Test
    @DisplayName("should get insertion position for interface annotations")
    void shouldGetInsertionPositionForInterfaceAnnotations() {
      String code = "@Repository public interface UserRepository {}";
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, code);
      TSNode interfaceNode = tsFile.query("(interface_declaration) @interface").execute().firstNode();

      AnnotationInsertionPoint beforeFirstPoint = annotationService.getAnnotationInsertionPosition(
          tsFile, interfaceNode, AnnotationInsertionPosition.BEFORE_FIRST_ANNOTATION);
      AnnotationInsertionPoint aboveScopePoint = annotationService.getAnnotationInsertionPosition(
          tsFile, interfaceNode, AnnotationInsertionPosition.ABOVE_SCOPE_DECLARATION);

      assertNotNull(beforeFirstPoint);
      assertNotNull(aboveScopePoint);
      assertEquals(AnnotationInsertionPosition.BEFORE_FIRST_ANNOTATION, beforeFirstPoint.getPosition());
      assertEquals(AnnotationInsertionPosition.ABOVE_SCOPE_DECLARATION, aboveScopePoint.getPosition());
    }

    @Test
    @DisplayName("should add annotation to interface above scope declaration")
    void shouldAddAnnotationToInterfaceAboveScopeDeclaration() {
      String code = "public interface SimpleRepository {}";
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, code);
      TSNode interfaceNode = tsFile.query("(interface_declaration) @interface").execute().firstNode();
      
      AnnotationInsertionPoint insertionPoint = annotationService.getAnnotationInsertionPosition(
          tsFile, interfaceNode, AnnotationInsertionPosition.ABOVE_SCOPE_DECLARATION);
      
      String originalCode = tsFile.getSourceCode();
      annotationService.addAnnotation(tsFile, interfaceNode, insertionPoint, "@Repository");
      String updatedCode = tsFile.getSourceCode();

      assertTrue(updatedCode.contains("@Repository"));
      assertTrue(updatedCode.contains("public interface SimpleRepository"));
      assertTrue(updatedCode.length() > originalCode.length());
    }

    @Test
    @DisplayName("should add annotation to interface before first annotation")
    void shouldAddAnnotationToInterfaceBeforeFirstAnnotation() {
      String code = "@Component public interface UserRepository {}";
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, code);
      TSNode interfaceNode = tsFile.query("(interface_declaration) @interface").execute().firstNode();
      
      AnnotationInsertionPoint insertionPoint = annotationService.getAnnotationInsertionPosition(
          tsFile, interfaceNode, AnnotationInsertionPosition.BEFORE_FIRST_ANNOTATION);
      
      String originalCode = tsFile.getSourceCode();
      annotationService.addAnnotation(tsFile, interfaceNode, insertionPoint, "@Repository");
      String updatedCode = tsFile.getSourceCode();

      assertTrue(updatedCode.contains("@Repository"));
      assertTrue(updatedCode.contains("@Component"));
      assertTrue(updatedCode.length() > originalCode.length());
      // @Repository should appear before @Component
      int repositoryIndex = updatedCode.indexOf("@Repository");
      int componentIndex = updatedCode.indexOf("@Component");
      assertTrue(repositoryIndex < componentIndex);
    }

    @Test
    @DisplayName("should handle complex interface with multiple annotations")
    void shouldHandleComplexInterfaceWithMultipleAnnotations() {
      String code = """
          @Repository
          @Component(value = "userRepo")
          @Transactional(readOnly = true)
          public interface UserRepository extends JpaRepository<User, Long> {
              List<User> findByName(String name);
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, code);
      TSNode interfaceNode = tsFile.query("(interface_declaration) @interface").execute().firstNode();

      List<TSNode> annotations = annotationService.getAllAnnotations(tsFile, interfaceNode);
      Optional<TSNode> repositoryAnnotation = annotationService.findAnnotationByName(tsFile, interfaceNode, "Repository");
      Optional<TSNode> componentAnnotation = annotationService.findAnnotationByName(tsFile, interfaceNode, "Component");
      Optional<TSNode> transactionalAnnotation = annotationService.findAnnotationByName(tsFile, interfaceNode, "Transactional");

      assertEquals(3, annotations.size());
      assertTrue(repositoryAnnotation.isPresent());
      assertTrue(componentAnnotation.isPresent());
      assertTrue(transactionalAnnotation.isPresent());

      // Test getting argument from @Component annotation
      Map<String, AnnotationArgument> componentArgs = annotationService.getAnnotationArguments(tsFile, componentAnnotation.get());
      assertEquals(1, componentArgs.size());
      assertEquals("\"userRepo\"", componentArgs.get("value").getValue(tsFile));

      // Test getting argument from @Transactional annotation
      Map<String, AnnotationArgument> transactionalArgs = annotationService.getAnnotationArguments(tsFile, transactionalAnnotation.get());
      assertEquals(1, transactionalArgs.size());
      assertEquals("true", transactionalArgs.get("readOnly").getValue(tsFile));
    }

    @Test
    @DisplayName("should handle interface without existing annotations")
    void shouldHandleInterfaceWithoutExistingAnnotations() {
      String code = "public interface PlainInterface {}";
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, code);
      TSNode interfaceNode = tsFile.query("(interface_declaration) @interface").execute().firstNode();

      List<TSNode> annotations = annotationService.getAllAnnotations(tsFile, interfaceNode);
      AnnotationInsertionPoint insertionPoint = annotationService.getAnnotationInsertionPosition(
          tsFile, interfaceNode, AnnotationInsertionPosition.BEFORE_FIRST_ANNOTATION);

      assertEquals(0, annotations.size());
      assertNotNull(insertionPoint);
      assertEquals(AnnotationInsertionPosition.BEFORE_FIRST_ANNOTATION, insertionPoint.getPosition());
    }

    @Test
    @DisplayName("should handle nested interface annotations")
    void shouldHandleNestedInterfaceAnnotations() {
      String code = """
          public class OuterClass {
              @Component
              public interface InnerInterface {
                  void method();
              }
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, code);
      TSNode interfaceNode = tsFile.query("(interface_declaration) @interface").execute().firstNode();

      List<TSNode> annotations = annotationService.getAllAnnotations(tsFile, interfaceNode);
      Optional<TSNode> componentAnnotation = annotationService.findAnnotationByName(tsFile, interfaceNode, "Component");

      assertEquals(1, annotations.size());
      assertTrue(componentAnnotation.isPresent());
    }
  }

  @Nested
  @DisplayName("Error Handling Tests")
  class ErrorHandlingTests {

    @Test
    @DisplayName("should handle null inputs for getAllAnnotations")
    void shouldHandleNullInputsForGetAllAnnotations() {
      String code = "@Override public void method() {}";
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, code);

      List<TSNode> result1 = annotationService.getAllAnnotations(null, null);
      List<TSNode> result2 = annotationService.getAllAnnotations(tsFile, null);

      assertTrue(result1.isEmpty());
      assertTrue(result2.isEmpty());
    }

    @Test
    @DisplayName("should handle null inputs for findAnnotationByName")
    void shouldHandleNullInputsForFindAnnotationByName() {
      String code = "@Entity public class User {}";
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, code);
      TSNode classNode = tsFile.query("(class_declaration) @class").execute().firstNode();

      Optional<TSNode> nullResult = annotationService.findAnnotationByName(tsFile, classNode, null);
      Optional<TSNode> emptyResult = annotationService.findAnnotationByName(tsFile, classNode, "");

      assertFalse(nullResult.isPresent());
      assertFalse(emptyResult.isPresent());
    }

    @Test
    @DisplayName("should handle null inputs for getAnnotationValueByKey")
    void shouldHandleNullInputsForGetAnnotationValueByKey() {
      String code = "@Table(name = \"users\") public class User {}";
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, code);
      TSNode annotationNode = tsFile.query("(annotation) @annotation").execute().firstNode();

      Optional<TSNode> nullKeyResult = annotationService.getAnnotationValueByKey(tsFile, annotationNode, null);
      Optional<TSNode> emptyKeyResult = annotationService.getAnnotationValueByKey(tsFile, annotationNode, "");

      assertFalse(nullKeyResult.isPresent());
      assertFalse(emptyKeyResult.isPresent());
    }

    @Test
    @DisplayName("should return empty map for null inputs in getAnnotationArguments")
    void shouldReturnEmptyMapForNullInputsInGetAnnotationArguments() {
      Map<String, AnnotationArgument> result = annotationService.getAnnotationArguments(null, null);

      assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("should reject unsupported node types for annotation operations")
    void shouldRejectUnsupportedNodeTypesForAnnotationOperations() {
      String code = "public interface TestInterface { void method(); }";
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, code);
      TSNode identifierNode = tsFile.query("(identifier) @id").execute().firstNode();

      // Test getAnnotationInsertionPosition with unsupported node type
      AnnotationInsertionPoint insertionPoint = annotationService.getAnnotationInsertionPosition(
          tsFile, identifierNode, AnnotationInsertionPosition.ABOVE_SCOPE_DECLARATION);

      assertNull(insertionPoint);

      // Test addAnnotation with unsupported node type
      String originalCode = tsFile.getSourceCode();
      annotationService.addAnnotation(tsFile, identifierNode, null, "@Test");
      String finalCode = tsFile.getSourceCode();

      assertEquals(originalCode, finalCode); // Should remain unchanged
    }
  }
}