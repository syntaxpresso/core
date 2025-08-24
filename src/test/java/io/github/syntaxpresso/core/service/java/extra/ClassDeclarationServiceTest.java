package io.github.syntaxpresso.core.service.java.extra;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.treesitter.TSNode;

@DisplayName("ClassDeclarationService Tests")
class ClassDeclarationServiceTest {
  private ClassDeclarationService classDeclarationService;
  private TSFile testFile;

  @BeforeEach
  void setUp() {
    FieldDeclarationService fieldDeclarationService = new FieldDeclarationService();
    classDeclarationService =
        new ClassDeclarationService(fieldDeclarationService);
    String javaCode =
        """
        package io.github.test;
        public class MyClass {
          private String name;
          private int count;
          public void doSomething() {
            System.out.println("Hello");
          }
          public int getCount() {
            return count;
          }
          public static void main(String[] args) {
            System.out.println("Main method");
          }
        }
        """;
    testFile = new TSFile(SupportedLanguage.JAVA, javaCode);
  }

  @Test
  @DisplayName("should find all class declarations")
  void findAllClassDeclarations_shouldReturnAllClasses() {
    List<TSNode> classes = classDeclarationService.findAllClassDeclarations(testFile);
    assertEquals(1, classes.size());
  }

  @Test
  @DisplayName("should get class name")
  void getClassName_shouldReturnCorrectName() {
    List<TSNode> classes = classDeclarationService.findAllClassDeclarations(testFile);
    TSNode classNode = classes.get(0);
    Optional<String> className = classDeclarationService.getClassName(testFile, classNode);
    assertTrue(className.isPresent());
    assertEquals("MyClass", className.get());
  }

  @Test
  @DisplayName("should find class by name")
  void findClassByName_shouldReturnCorrectClass() {
    Optional<TSNode> classNode = classDeclarationService.findClassByName(testFile, "MyClass");
    assertTrue(classNode.isPresent());
  }

  @Test
  @DisplayName("should get class fields")
  void getClassFields_shouldReturnAllFields() {
    Optional<TSNode> classNode = classDeclarationService.findClassByName(testFile, "MyClass");
    assertTrue(classNode.isPresent());
    List<TSNode> fields = classDeclarationService.getClassFields(testFile, classNode.get());
    assertEquals(2, fields.size());
  }

  @Test
  @DisplayName("should get class methods")
  void getClassMethods_shouldReturnAllMethods() {
    Optional<TSNode> classNode = classDeclarationService.findClassByName(testFile, "MyClass");
    assertTrue(classNode.isPresent());
    List<TSNode> methods = classDeclarationService.getClassMethods(testFile, classNode.get());
    assertEquals(3, methods.size()); // doSomething, getCount, main
  }

  @Test
  @DisplayName("should find methods by name")
  void findMethodsByName_shouldReturnCorrectMethods() {
    Optional<TSNode> classNode = classDeclarationService.findClassByName(testFile, "MyClass");
    assertTrue(classNode.isPresent());
    List<TSNode> mainMethods =
        classDeclarationService.findMethodsByName(testFile, classNode.get(), "main");
    assertEquals(1, mainMethods.size());
  }

  @Test
  @DisplayName("should rename class")
  void renameClass_shouldUpdateClassName() {
    Optional<TSNode> classNode = classDeclarationService.findClassByName(testFile, "MyClass");
    assertTrue(classNode.isPresent());
    classDeclarationService.renameClass(testFile, classNode.get(), "RenamedClass");
    // After modifying the source code, we need to find the class again since the tree has changed
    Optional<TSNode> renamedClassNode =
        classDeclarationService.findClassByName(testFile, "RenamedClass");
    assertTrue(renamedClassNode.isPresent());
    Optional<String> newName =
        classDeclarationService.getClassName(testFile, renamedClassNode.get());
    assertTrue(newName.isPresent());
    assertEquals("RenamedClass", newName.get());
  }
}
