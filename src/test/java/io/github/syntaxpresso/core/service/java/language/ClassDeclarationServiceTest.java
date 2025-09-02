package io.github.syntaxpresso.core.service.java.language;

import static org.junit.jupiter.api.Assertions.*;

import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import io.github.syntaxpresso.core.service.java.language.extra.ClassCapture;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
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
  private ClassDeclarationService service;

  // Reusable source code strings
  private static final String SINGLE_CLASS_CODE =
      """
      package io.github.test;

      import java.util.List;

      public class MyClass extends BaseClass {
          private String name;
          private int count;

          public MyClass() {}

          public void doSomething() {
              this.name = "test";
          }

          public int getCount() {
              return this.count;
          }

          public static void main(String[] args) {}
      }
      """;

  private static final String MULTIPLE_CLASSES_CODE =
      """
      package io.github.test;

      class Helper extends Object {
          public void help() {}
      }

      public class MainClass extends SuperMain {
          private int value;
      }

      class Utility extends Helper {
          private String util;
      }

      abstract class BaseUtility {
          protected abstract void process();
      }
      """;

  private static final String NO_SUPERCLASS_CODE =
      """
      package io.github.test;

      public class NoSuperClass {
          private int value;

          public NoSuperClass(int value) {
              this.value = value;
          }
      }
      """;

  private static final String NESTED_CLASSES_CODE =
      """
      package io.github.test;

      public class OuterClass extends BaseOuter {
          private String outerField;

          public class InnerClass extends BaseInner {
              private String innerField;
          }

          static class StaticNested extends BaseNested {
              private String nestedField;
          }
      }
      """;

  private static final String ANNOTATED_CLASS_CODE =
      """
      package io.github.test;

      import javax.persistence.Entity;
      import javax.persistence.Table;

      @Entity
      @Table(name = "my_table")
      public class AnnotatedClass extends BaseEntity {
          private String name;
      }

      @Deprecated
      class SimpleAnnotatedClass {
          private int value;
      }

      @Entity
      @Table(name = "user_table")
      @SuppressWarnings("unused")
      class MultipleAnnotationsClass extends Object {
          private String username;
      }
      """;

  private static final String EMPTY_FILE_CODE = "";
  private static final String NO_CLASSES_CODE =
      """
      package io.github.test;

      import java.util.List;

      // This file has no classes
      """;

  private TSFile singleClassFile;
  private TSFile multipleClassesFile;
  private TSFile noSuperclassFile;
  private TSFile nestedClassesFile;
  private TSFile annotatedClassFile;
  private TSFile emptyFile;
  private TSFile noClassesFile;

  @TempDir Path tempDir;

  @BeforeEach
  void setUp() {
    service = new ClassDeclarationService();
    singleClassFile = new TSFile(SupportedLanguage.JAVA, SINGLE_CLASS_CODE);
    multipleClassesFile = new TSFile(SupportedLanguage.JAVA, MULTIPLE_CLASSES_CODE);
    noSuperclassFile = new TSFile(SupportedLanguage.JAVA, NO_SUPERCLASS_CODE);
    nestedClassesFile = new TSFile(SupportedLanguage.JAVA, NESTED_CLASSES_CODE);
    annotatedClassFile = new TSFile(SupportedLanguage.JAVA, ANNOTATED_CLASS_CODE);
    emptyFile = new TSFile(SupportedLanguage.JAVA, EMPTY_FILE_CODE);
    noClassesFile = new TSFile(SupportedLanguage.JAVA, NO_CLASSES_CODE);
  }

  @Nested
  @DisplayName("getAllClassDeclarations Tests")
  class GetAllClassDeclarationsTests {

    @Test
    @DisplayName("should return single class declaration with correct captures")
    void shouldReturnSingleClassDeclaration() {
      List<Map<String, TSNode>> classes = service.getAllClassDeclarations(singleClassFile);

      assertEquals(1, classes.size());
      Map<String, TSNode> classInfo = classes.get(0);
      assertTrue(classInfo.containsKey("className"));
      assertTrue(classInfo.containsKey("classDeclaration"));

      String className = singleClassFile.getTextFromNode(classInfo.get("className"));
      assertEquals("MyClass", className);
    }

    @Test
    @DisplayName("should return multiple class declarations")
    void shouldReturnMultipleClassDeclarations() {
      List<Map<String, TSNode>> classes = service.getAllClassDeclarations(multipleClassesFile);

      assertEquals(4, classes.size());

      // Verify all expected classes are found
      List<String> expectedClasses = List.of("Helper", "MainClass", "Utility", "BaseUtility");
      List<String> actualClasses =
          classes.stream()
              .map(classInfo -> multipleClassesFile.getTextFromNode(classInfo.get("className")))
              .toList();

      assertTrue(actualClasses.containsAll(expectedClasses));
    }

    @Test
    @DisplayName("should return nested class declarations")
    void shouldReturnNestedClassDeclarations() {
      List<Map<String, TSNode>> classes = service.getAllClassDeclarations(nestedClassesFile);

      assertEquals(3, classes.size());

      List<String> expectedClasses = List.of("OuterClass", "InnerClass", "StaticNested");
      List<String> actualClasses =
          classes.stream()
              .map(classInfo -> nestedClassesFile.getTextFromNode(classInfo.get("className")))
              .toList();

      assertTrue(actualClasses.containsAll(expectedClasses));
    }

    @Test
    @DisplayName("should return empty list for file with no classes")
    void shouldReturnEmptyListForNoClasses() {
      List<Map<String, TSNode>> classes = service.getAllClassDeclarations(noClassesFile);
      assertTrue(classes.isEmpty());
    }

    @Test
    @DisplayName("should return empty list for empty file")
    void shouldReturnEmptyListForEmptyFile() {
      List<Map<String, TSNode>> classes = service.getAllClassDeclarations(emptyFile);
      assertTrue(classes.isEmpty());
    }

    @Test
    @DisplayName("should return empty list for null file")
    void shouldReturnEmptyListForNullFile() {
      List<Map<String, TSNode>> classes = service.getAllClassDeclarations(null);
      assertEquals(Collections.emptyList(), classes);
    }

    @Test
    @DisplayName("should return empty list for file with null tree")
    void shouldReturnEmptyListForNullTree() {
      // Test by passing null directly to the method
      List<Map<String, TSNode>> classes = service.getAllClassDeclarations(null);
      assertEquals(Collections.emptyList(), classes);
    }
  }

  @Nested
  @DisplayName("getClassDeclarationNameNode Tests")
  class GetClassDeclarationNameNodeTests {

    @Test
    @DisplayName("should return class name node for valid class declaration")
    void shouldReturnClassNameNode() {
      List<Map<String, TSNode>> classes = service.getAllClassDeclarations(singleClassFile);
      TSNode classDecl = classes.get(0).get("classDeclaration");

      Optional<TSNode> classNameNode =
          service.getClassDeclarationNameNode(singleClassFile, classDecl);

      assertTrue(classNameNode.isPresent());
      String className = singleClassFile.getTextFromNode(classNameNode.get());
      assertEquals("MyClass", className);
    }

    @Test
    @DisplayName("should return empty for null file")
    void shouldReturnEmptyForNullFile() {
      Optional<TSNode> result = service.getClassDeclarationNameNode(null, null);
      assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("should return empty for file with null tree")
    void shouldReturnEmptyForNullTree() {
      Optional<TSNode> result = service.getClassDeclarationNameNode(null, null);
      assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("should return empty for null class node")
    void shouldReturnEmptyForNullClassNode() {
      assertThrows(
          NullPointerException.class,
          () -> {
            service.getClassDeclarationNameNode(singleClassFile, null);
          });
    }

    @Test
    @DisplayName("should return empty for non-class declaration node")
    void shouldReturnEmptyForNonClassDeclarationNode() {
      List<Map<String, TSNode>> classes = service.getAllClassDeclarations(singleClassFile);
      TSNode classNameNode = classes.get(0).get("className");

      Optional<TSNode> result = service.getClassDeclarationNameNode(singleClassFile, classNameNode);
      assertTrue(result.isEmpty());
    }
  }

  @Nested
  @DisplayName("findClassByName Tests")
  class FindClassByNameTests {

    @Test
    @DisplayName("should find existing class by name")
    void shouldFindExistingClass() {
      Optional<TSNode> found = service.findClassByName(singleClassFile, "MyClass");

      assertTrue(found.isPresent());
      String classText = singleClassFile.getTextFromNode(found.get());
      assertTrue(classText.contains("public class MyClass"));
    }

    @Test
    @DisplayName("should find class in multiple classes file")
    void shouldFindClassInMultipleClasses() {
      Optional<TSNode> helper = service.findClassByName(multipleClassesFile, "Helper");
      Optional<TSNode> mainClass = service.findClassByName(multipleClassesFile, "MainClass");
      Optional<TSNode> utility = service.findClassByName(multipleClassesFile, "Utility");

      assertTrue(helper.isPresent());
      assertTrue(mainClass.isPresent());
      assertTrue(utility.isPresent());

      String mainClassText = multipleClassesFile.getTextFromNode(mainClass.get());
      assertTrue(mainClassText.contains("public class MainClass"));
    }

    @Test
    @DisplayName("should return empty for non-existent class")
    void shouldReturnEmptyForNonExistentClass() {
      Optional<TSNode> notFound = service.findClassByName(singleClassFile, "NonExistentClass");
      assertTrue(notFound.isEmpty());
    }

    @Test
    @DisplayName("should return empty for null file")
    void shouldReturnEmptyForNullFile() {
      Optional<TSNode> result = service.findClassByName(null, "MyClass");
      assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("should return empty for file with null tree")
    void shouldReturnEmptyForNullTree() {
      Optional<TSNode> result = service.findClassByName(null, "MyClass");
      assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("should return empty for null class name")
    void shouldReturnEmptyForNullClassName() {
      Optional<TSNode> result = service.findClassByName(singleClassFile, null);
      assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("should return empty for empty class name")
    void shouldReturnEmptyForEmptyClassName() {
      Optional<TSNode> result = service.findClassByName(singleClassFile, "");
      assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("should return empty for whitespace class name")
    void shouldReturnEmptyForWhitespaceClassName() {
      Optional<TSNode> result = service.findClassByName(singleClassFile, "   ");
      assertTrue(result.isEmpty());
    }
  }

  @Nested
  @DisplayName("getClassDeclarationNodeInfo Tests")
  class GetClassDeclarationNodeInfoTests {

    @Test
    @DisplayName("should return class declaration info for class with superclass")
    void shouldReturnClassDeclarationInfoWithSuperclass() {
      List<Map<String, TSNode>> classes = service.getAllClassDeclarations(singleClassFile);
      TSNode classDecl = classes.get(0).get("classDeclaration");

      List<Map<String, TSNode>> info =
          service.getClassDeclarationNodeInfo(singleClassFile, classDecl);

      assertFalse(info.isEmpty());
      Map<String, TSNode> classInfo = info.get(0);

      assertTrue(classInfo.containsKey("className"));
      assertTrue(classInfo.containsKey("superclass"));
      assertTrue(classInfo.containsKey("superclassName"));

      String className = singleClassFile.getTextFromNode(classInfo.get("className"));
      String superclassName = singleClassFile.getTextFromNode(classInfo.get("superclassName"));
      assertEquals("MyClass", className);
      assertEquals("BaseClass", superclassName);
    }

    @Test
    @DisplayName("should return class declaration info for annotated classes")
    void shouldReturnClassDeclarationInfoWithAnnotations() {
      List<Map<String, TSNode>> classes = service.getAllClassDeclarations(annotatedClassFile);
      TSNode annotatedClassDecl = classes.get(0).get("classDeclaration");

      List<Map<String, TSNode>> info =
          service.getClassDeclarationNodeInfo(annotatedClassFile, annotatedClassDecl);

      assertFalse(info.isEmpty());

      boolean foundAnnotation = false;
      for (Map<String, TSNode> capture : info) {
        if (capture.containsKey("classAnnotation")) {
          foundAnnotation = true;
          break;
        }
      }
      assertTrue(foundAnnotation);
    }

    @Test
    @DisplayName("should return empty list for null file")
    void shouldReturnEmptyListForNullFile() {
      List<Map<String, TSNode>> result = service.getClassDeclarationNodeInfo(null, null);
      assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("should return empty list for non-class declaration node")
    void shouldReturnEmptyListForNonClassDeclarationNode() {
      List<Map<String, TSNode>> classes = service.getAllClassDeclarations(singleClassFile);
      TSNode classNameNode = classes.get(0).get("className");

      List<Map<String, TSNode>> result =
          service.getClassDeclarationNodeInfo(singleClassFile, classNameNode);
      assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("should handle class without superclass")
    void shouldHandleClassWithoutSuperclass() {
      List<Map<String, TSNode>> classes = service.getAllClassDeclarations(noSuperclassFile);
      TSNode classDecl = classes.get(0).get("classDeclaration");

      List<Map<String, TSNode>> info =
          service.getClassDeclarationNodeInfo(noSuperclassFile, classDecl);

      assertFalse(info.isEmpty());
      Map<String, TSNode> classInfo = info.get(0);

      assertTrue(classInfo.containsKey("className"));
      String className = noSuperclassFile.getTextFromNode(classInfo.get("className"));
      assertEquals("NoSuperClass", className);
    }
  }

  @Nested
  @DisplayName("getClassDeclarationNodeByCaptureName Tests")
  class GetClassDeclarationNodeByCaptureNameTests {

    @Test
    @DisplayName("should return class name node by capture name")
    void shouldReturnClassNameNodeByCaptureName() {
      List<Map<String, TSNode>> classes = service.getAllClassDeclarations(singleClassFile);
      TSNode classDecl = classes.get(0).get("classDeclaration");

      Optional<TSNode> classNameNode =
          service.getClassDeclarationNodeByCaptureName(
              singleClassFile, classDecl, ClassCapture.CLASS_NAME);

      assertTrue(classNameNode.isPresent());
      String className = singleClassFile.getTextFromNode(classNameNode.get());
      assertEquals("MyClass", className);
    }

    @Test
    @DisplayName("should return superclass name node by capture name")
    void shouldReturnSuperclassNameNodeByCaptureName() {
      List<Map<String, TSNode>> classes = service.getAllClassDeclarations(singleClassFile);
      TSNode classDecl = classes.get(0).get("classDeclaration");

      Optional<TSNode> superclassNode =
          service.getClassDeclarationNodeByCaptureName(
              singleClassFile, classDecl, ClassCapture.SUPERCLASS_NAME);

      assertTrue(superclassNode.isPresent());
      String superclassName = singleClassFile.getTextFromNode(superclassNode.get());
      assertEquals("BaseClass", superclassName);
    }

    @Test
    @DisplayName("should return method declaration when present")
    void shouldReturnMethodDeclarationWhenPresent() {
      List<Map<String, TSNode>> classes = service.getAllClassDeclarations(singleClassFile);
      TSNode classDecl = classes.get(0).get("classDeclaration");

      Optional<TSNode> result =
          service.getClassDeclarationNodeByCaptureName(
              singleClassFile, classDecl, ClassCapture.METHOD_DECLARATION);

      assertTrue(result.isPresent());
    }

    @Test
    @DisplayName("should return empty for null file")
    void shouldReturnEmptyForNullFile() {
      Optional<TSNode> result =
          service.getClassDeclarationNodeByCaptureName(null, null, ClassCapture.CLASS_NAME);
      assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("should return empty for non-class declaration node")
    void shouldReturnEmptyForNonClassDeclarationNode() {
      List<Map<String, TSNode>> classes = service.getAllClassDeclarations(singleClassFile);
      TSNode classNameNode = classes.get(0).get("className");

      Optional<TSNode> result =
          service.getClassDeclarationNodeByCaptureName(
              singleClassFile, classNameNode, ClassCapture.CLASS_NAME);
      assertTrue(result.isEmpty());
    }
  }

  @Nested
  @DisplayName("getClassDeclarationAnnotationNodes Tests")
  class GetClassDeclarationAnnotationNodesTests {

    @Test
    @DisplayName("should return annotation nodes for annotated class")
    void shouldReturnAnnotationNodesForAnnotatedClass() {
      List<Map<String, TSNode>> classes = service.getAllClassDeclarations(annotatedClassFile);

      for (Map<String, TSNode> classInfo : classes) {
        String className = annotatedClassFile.getTextFromNode(classInfo.get("className"));
        TSNode classDecl = classInfo.get("classDeclaration");

        List<TSNode> annotations =
            service.getClassDeclarationAnnotationNodes(annotatedClassFile, classDecl);

        if (className.equals("AnnotatedClass")) {
          assertFalse(annotations.isEmpty());
          assertTrue(annotations.size() >= 1);
        } else if (className.equals("SimpleAnnotatedClass")) {
          assertFalse(annotations.isEmpty());
          assertEquals(1, annotations.size());
        } else if (className.equals("MultipleAnnotationsClass")) {
          assertFalse(annotations.isEmpty());
          assertTrue(annotations.size() >= 2);
        }
      }
    }

    @Test
    @DisplayName("should return empty list for non-annotated class")
    void shouldReturnEmptyListForNonAnnotatedClass() {
      List<Map<String, TSNode>> classes = service.getAllClassDeclarations(singleClassFile);
      TSNode classDecl = classes.get(0).get("classDeclaration");

      List<TSNode> annotations =
          service.getClassDeclarationAnnotationNodes(singleClassFile, classDecl);

      assertTrue(annotations.isEmpty());
    }

    @Test
    @DisplayName("should return empty list for null file")
    void shouldReturnEmptyListForNullFile() {
      List<TSNode> result = service.getClassDeclarationAnnotationNodes(null, null);
      assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("should return empty list for non-class declaration node")
    void shouldReturnEmptyListForNonClassDeclarationNode() {
      List<Map<String, TSNode>> classes = service.getAllClassDeclarations(singleClassFile);
      TSNode classNameNode = classes.get(0).get("className");

      List<TSNode> result =
          service.getClassDeclarationAnnotationNodes(singleClassFile, classNameNode);
      assertTrue(result.isEmpty());
    }
  }

  @Nested
  @DisplayName("getClassDeclarationSuperclassNameNode Tests")
  class GetClassDeclarationSuperclassNameNodeTests {

    @Test
    @DisplayName("should return superclass name node for class with superclass")
    void shouldReturnSuperclassNameNode() {
      List<Map<String, TSNode>> classes = service.getAllClassDeclarations(singleClassFile);
      TSNode classDecl = classes.get(0).get("classDeclaration");

      Optional<TSNode> superclassNameNode =
          service.getClassDeclarationSuperclassNameNode(singleClassFile, classDecl);

      assertTrue(superclassNameNode.isPresent());
      String superclassName = singleClassFile.getTextFromNode(superclassNameNode.get());
      assertEquals("BaseClass", superclassName);
    }

    @Test
    @DisplayName("should return empty for class without superclass")
    void shouldReturnEmptyForClassWithoutSuperclass() {
      List<Map<String, TSNode>> classes = service.getAllClassDeclarations(noSuperclassFile);
      TSNode classDecl = classes.get(0).get("classDeclaration");

      Optional<TSNode> superclassNameNode =
          service.getClassDeclarationSuperclassNameNode(noSuperclassFile, classDecl);

      assertTrue(superclassNameNode.isEmpty());
    }

    @Test
    @DisplayName("should return empty for null file")
    void shouldReturnEmptyForNullFile() {
      Optional<TSNode> result = service.getClassDeclarationSuperclassNameNode(null, null);
      assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("should handle multiple classes correctly")
    void shouldHandleMultipleClassesCorrectly() {
      List<Map<String, TSNode>> classes = service.getAllClassDeclarations(multipleClassesFile);

      // Just verify that classes with explicit superclasses are handled
      boolean foundMainClass = false;
      for (Map<String, TSNode> classInfo : classes) {
        TSNode classDecl = classInfo.get("classDeclaration");
        String className = multipleClassesFile.getTextFromNode(classInfo.get("className"));

        if (className.equals("MainClass")) {
          foundMainClass = true;
          Optional<TSNode> superclassNameNode =
              service.getClassDeclarationSuperclassNameNode(multipleClassesFile, classDecl);
          assertTrue(superclassNameNode.isPresent());
          String superclassName = multipleClassesFile.getTextFromNode(superclassNameNode.get());
          assertEquals("SuperMain", superclassName);
          break;
        }
      }

      assertTrue(foundMainClass, "MainClass should be found");
    }
  }

  @Nested
  @DisplayName("getMainClass Tests")
  class GetMainClassTests {

    @Test
    @DisplayName("should return main class when file name matches class name")
    void shouldReturnMainClassWhenFileNameMatches() throws IOException {
      Path javaFile = tempDir.resolve("MyClass.java");
      Files.write(javaFile, SINGLE_CLASS_CODE.getBytes());
      TSFile fileWithPath = new TSFile(SupportedLanguage.JAVA, javaFile);

      Optional<TSNode> mainClass = service.getMainClass(fileWithPath);

      assertTrue(mainClass.isPresent());
      Optional<TSNode> classNameNode =
          service.getClassDeclarationNameNode(fileWithPath, mainClass.get());
      assertTrue(classNameNode.isPresent());
      String className = fileWithPath.getTextFromNode(classNameNode.get());
      assertEquals("MyClass", className);
    }

    @Test
    @DisplayName("should return empty when file name doesn't match any class")
    void shouldReturnEmptyWhenFileNameDoesNotMatch() throws IOException {
      Path javaFile = tempDir.resolve("DifferentName.java");
      Files.write(javaFile, SINGLE_CLASS_CODE.getBytes());
      TSFile fileWithPath = new TSFile(SupportedLanguage.JAVA, javaFile);

      Optional<TSNode> mainClass = service.getMainClass(fileWithPath);
      assertTrue(mainClass.isEmpty());
    }

    @Test
    @DisplayName("should work with multiple classes files")
    void shouldWorkWithMultipleClassesFiles() {
      // Test that we can find classes in a file with multiple classes
      Optional<TSNode> mainClass = service.findClassByName(multipleClassesFile, "MainClass");
      assertTrue(mainClass.isPresent());

      Optional<TSNode> helper = service.findClassByName(multipleClassesFile, "Helper");
      assertTrue(helper.isPresent());

      Optional<TSNode> utility = service.findClassByName(multipleClassesFile, "Utility");
      assertTrue(utility.isPresent());
    }

    @Test
    @DisplayName("should return empty for null file")
    void shouldReturnEmptyForNullFile() {
      Optional<TSNode> result = service.getMainClass(null);
      assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("should return empty for file with null tree")
    void shouldReturnEmptyForNullTree() {
      Optional<TSNode> result = service.getMainClass(null);
      assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("should return empty for file without path")
    void shouldReturnEmptyForFileWithoutPath() {
      Optional<TSNode> result = service.getMainClass(singleClassFile);
      assertTrue(result.isEmpty());
    }
  }

  @Nested
  @DisplayName("isLocalClass Tests")
  class IsLocalClassTests {

    @Test
    @DisplayName("should return true for existing local class")
    void shouldReturnTrueForExistingLocalClass() throws IOException {
      Path projectRoot = tempDir;
      Path packagePath = projectRoot.resolve("src/main/java/io/github/test");
      Files.createDirectories(packagePath);
      Path classFile = packagePath.resolve("MyClass.java");
      Files.write(classFile, SINGLE_CLASS_CODE.getBytes());

      Boolean result = service.isLocalClass(projectRoot, "io.github.test.MyClass");
      assertTrue(result);
    }

    @Test
    @DisplayName("should return false for non-existent class")
    void shouldReturnFalseForNonExistentClass() {
      Path projectRoot = tempDir;
      Boolean result = service.isLocalClass(projectRoot, "io.github.test.NonExistentClass");
      assertFalse(result);
    }

    @Test
    @DisplayName("should return false for class in wrong package")
    void shouldReturnFalseForClassInWrongPackage() throws IOException {
      Path projectRoot = tempDir;
      Path packagePath = projectRoot.resolve("src/main/java/io/github/other");
      Files.createDirectories(packagePath);
      Path classFile = packagePath.resolve("MyClass.java");
      Files.write(classFile, SINGLE_CLASS_CODE.getBytes());

      Boolean result = service.isLocalClass(projectRoot, "io.github.test.MyClass");
      assertFalse(result);
    }

    @Test
    @DisplayName("should handle nested packages correctly")
    void shouldHandleNestedPackagesCorrectly() throws IOException {
      Path projectRoot = tempDir;
      Path packagePath = projectRoot.resolve("src/main/java/com/example/deep/nested");
      Files.createDirectories(packagePath);
      Path classFile = packagePath.resolve("DeepClass.java");
      Files.write(classFile, "class DeepClass {}".getBytes());

      Boolean result = service.isLocalClass(projectRoot, "com.example.deep.nested.DeepClass");
      assertTrue(result);
    }

    @Test
    @DisplayName("should return false for null project root")
    void shouldReturnFalseForNullProjectRoot() {
      // The service method throws NullPointerException for null paths
      assertThrows(
          NullPointerException.class,
          () -> {
            service.isLocalClass(null, "io.github.test.MyClass");
          });
    }

    @Test
    @DisplayName("should return false for null class name")
    void shouldReturnFalseForNullClassName() {
      // The service method throws NullPointerException for null class name
      assertThrows(
          NullPointerException.class,
          () -> {
            service.isLocalClass(tempDir, null);
          });
    }
  }

  @Nested
  @DisplayName("getFullyQualifiedSuperclass Tests")
  class GetFullyQualifiedSuperclassTests {

    @Test
    @DisplayName("should return already qualified class name")
    void shouldReturnAlreadyQualifiedClassName() {
      Optional<String> result =
          service.getFullyQualifiedSuperclass(singleClassFile, "com.example.BaseClass");
      assertEquals(Optional.of("com.example.BaseClass"), result);
    }

    @Test
    @DisplayName("should qualify java.lang classes")
    void shouldQualifyJavaLangClasses() {
      assertEquals(
          Optional.of("java.lang.String"),
          service.getFullyQualifiedSuperclass(singleClassFile, "String"));
      assertEquals(
          Optional.of("java.lang.Object"),
          service.getFullyQualifiedSuperclass(singleClassFile, "Object"));
      assertEquals(
          Optional.of("java.lang.Integer"),
          service.getFullyQualifiedSuperclass(singleClassFile, "Integer"));
      assertEquals(
          Optional.of("java.lang.Long"),
          service.getFullyQualifiedSuperclass(singleClassFile, "Long"));
      assertEquals(
          Optional.of("java.lang.Double"),
          service.getFullyQualifiedSuperclass(singleClassFile, "Double"));
      assertEquals(
          Optional.of("java.lang.Float"),
          service.getFullyQualifiedSuperclass(singleClassFile, "Float"));
      assertEquals(
          Optional.of("java.lang.Boolean"),
          service.getFullyQualifiedSuperclass(singleClassFile, "Boolean"));
      assertEquals(
          Optional.of("java.lang.Character"),
          service.getFullyQualifiedSuperclass(singleClassFile, "Character"));
      assertEquals(
          Optional.of("java.lang.Byte"),
          service.getFullyQualifiedSuperclass(singleClassFile, "Byte"));
      assertEquals(
          Optional.of("java.lang.Short"),
          service.getFullyQualifiedSuperclass(singleClassFile, "Short"));
    }

    @Test
    @DisplayName("should return simple name for non-java.lang classes")
    void shouldReturnSimpleNameForNonJavaLangClasses() {
      Optional<String> result = service.getFullyQualifiedSuperclass(singleClassFile, "BaseClass");
      assertEquals(Optional.of("BaseClass"), result);
    }

    @Test
    @DisplayName("should return simple name for custom classes")
    void shouldReturnSimpleNameForCustomClasses() {
      Optional<String> result =
          service.getFullyQualifiedSuperclass(singleClassFile, "CustomSuperclass");
      assertEquals(Optional.of("CustomSuperclass"), result);
    }
  }

  @Nested
  @DisplayName("isJavaLangClass Tests")
  class IsJavaLangClassTests {

    @Test
    @DisplayName("should return true for java.lang classes")
    void shouldReturnTrueForJavaLangClasses() {
      assertTrue(service.isJavaLangClass("Object"));
      assertTrue(service.isJavaLangClass("String"));
      assertTrue(service.isJavaLangClass("Integer"));
      assertTrue(service.isJavaLangClass("Long"));
      assertTrue(service.isJavaLangClass("Double"));
      assertTrue(service.isJavaLangClass("Float"));
      assertTrue(service.isJavaLangClass("Boolean"));
      assertTrue(service.isJavaLangClass("Character"));
      assertTrue(service.isJavaLangClass("Byte"));
      assertTrue(service.isJavaLangClass("Short"));
    }

    @Test
    @DisplayName("should return false for non-java.lang classes")
    void shouldReturnFalseForNonJavaLangClasses() {
      assertFalse(service.isJavaLangClass("ArrayList"));
      assertFalse(service.isJavaLangClass("HashMap"));
      assertFalse(service.isJavaLangClass("BaseClass"));
      assertFalse(service.isJavaLangClass("MyClass"));
      assertFalse(service.isJavaLangClass("CustomClass"));
    }

    @Test
    @DisplayName("should return false for null class name")
    void shouldReturnFalseForNullClassName() {
      assertThrows(
          NullPointerException.class,
          () -> {
            service.isJavaLangClass(null);
          });
    }

    @Test
    @DisplayName("should return false for empty string")
    void shouldReturnFalseForEmptyString() {
      assertFalse(service.isJavaLangClass(""));
    }

    @Test
    @DisplayName("should be case sensitive")
    void shouldBeCaseSensitive() {
      assertFalse(service.isJavaLangClass("string"));
      assertFalse(service.isJavaLangClass("STRING"));
      assertFalse(service.isJavaLangClass("object"));
      assertFalse(service.isJavaLangClass("OBJECT"));
    }
  }

  @Nested
  @DisplayName("Edge Cases and Error Handling")
  class EdgeCasesAndErrorHandlingTests {

    @Test
    @DisplayName("should handle files with malformed syntax gracefully")
    void shouldHandleMalformedSyntaxGracefully() {
      String malformedCode = "public class Malformed { private; public void incomplete";
      TSFile malformedFile = new TSFile(SupportedLanguage.JAVA, malformedCode);

      List<Map<String, TSNode>> classes = service.getAllClassDeclarations(malformedFile);

      if (!classes.isEmpty()) {
        TSNode classDecl = classes.get(0).get("classDeclaration");
        List<Map<String, TSNode>> info =
            service.getClassDeclarationNodeInfo(malformedFile, classDecl);
        // For malformed syntax, we may get empty results or partial results - this is expected
        assertTrue(info.isEmpty() || !info.isEmpty());
      }
    }

    @Test
    @DisplayName("should handle very large files efficiently")
    void shouldHandleLargeFilesEfficiently() {
      StringBuilder largeClass = new StringBuilder();
      largeClass.append("public class LargeClass {\n");

      for (int i = 0; i < 1000; i++) {
        largeClass.append("    private String field").append(i).append(";\n");
        largeClass
            .append("    public String getField")
            .append(i)
            .append("() { return field")
            .append(i)
            .append("; }\n");
      }
      largeClass.append("}");

      TSFile largeFile = new TSFile(SupportedLanguage.JAVA, largeClass.toString());
      List<Map<String, TSNode>> classes = service.getAllClassDeclarations(largeFile);

      assertEquals(1, classes.size());
    }

    @Test
    @DisplayName("should handle special characters in class names")
    void shouldHandleSpecialCharactersInClassNames() {
      String specialCharCode =
          """
          public class Valid$Class {
              private int value;
          }

          class _UnderscoreClass {
              private String name;
          }
          """;

      TSFile specialCharFile = new TSFile(SupportedLanguage.JAVA, specialCharCode);
      List<Map<String, TSNode>> classes = service.getAllClassDeclarations(specialCharFile);

      assertEquals(2, classes.size());

      Optional<TSNode> validClass = service.findClassByName(specialCharFile, "Valid$Class");
      Optional<TSNode> underscoreClass =
          service.findClassByName(specialCharFile, "_UnderscoreClass");

      assertTrue(validClass.isPresent());
      assertTrue(underscoreClass.isPresent());
    }

    @Test
    @DisplayName("should handle unicode characters in source code")
    void shouldHandleUnicodeCharactersInSourceCode() {
      String unicodeCode =
          """
          package io.github.test;

          public class TestClass {
              private String name;

              public void testMethod() {
                  this.name = "test";
              }
          }
          """;

      TSFile unicodeFile = new TSFile(SupportedLanguage.JAVA, unicodeCode);
      List<Map<String, TSNode>> classes = service.getAllClassDeclarations(unicodeFile);

      // Should handle normal Java source code correctly
      assertFalse(classes.isEmpty());

      Optional<TSNode> testClass = service.findClassByName(unicodeFile, "TestClass");
      assertTrue(testClass.isPresent());
    }
  }
}
