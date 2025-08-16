package io.github.syntaxpresso.core.common;

import static org.junit.jupiter.api.Assertions.*;

import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.treesitter.TSNode;

@DisplayName("TSFile Extended Tests")
class TSFileExtendedTest {
  private TSFile testFile;
  private TSFile complexFile;

  @BeforeEach
  void setUp() {
    String javaCode =
        """
        package io.github.test;
        public class TestClass {
          private String field;
          public void method() {
            String localVar = "test";
          }
        }
        """;
    testFile = new TSFile(SupportedLanguage.JAVA, javaCode);
    String complexJavaCode =
        """
        package io.github.test;
        public class ComplexClass {
          public void outerMethod() {
            for (int i = 0; i < 10; i++) {
              if (i % 2 == 0) {
                String evenVar = "even";
                System.out.println(evenVar);
              }
            }
          }
        }
        """;
    complexFile = new TSFile(SupportedLanguage.JAVA, complexJavaCode);
  }

  @Nested
  @DisplayName("findParentNodeByType() enhanced error handling tests")
  class FindParentNodeByTypeTests {
    @Test
    @DisplayName("Should find parent method declaration from local variable")
    void shouldFindParentMethodDeclarationFromLocalVariable() {
      List<TSNode> localVars = testFile.query("(local_variable_declaration) @var");
      assertFalse(localVars.isEmpty(), "Should find local variable declarations");
      TSNode localVar = localVars.get(0);
      Optional<TSNode> parentMethod = testFile.findParentNodeByType(localVar, "method_declaration");
      assertTrue(parentMethod.isPresent(), "Should find parent method");
      TSNode methodNameNode = parentMethod.get().getChildByFieldName("name");
      assertNotNull(methodNameNode);
      assertEquals("method", testFile.getTextFromNode(methodNameNode));
    }

    @Test
    @DisplayName("Should find parent class declaration from method")
    void shouldFindParentClassDeclarationFromMethod() {
      List<TSNode> methods = testFile.query("(method_declaration) @method");
      assertFalse(methods.isEmpty(), "Should find method declarations");
      TSNode method = methods.get(0);
      Optional<TSNode> parentClass = testFile.findParentNodeByType(method, "class_declaration");
      assertTrue(parentClass.isPresent(), "Should find parent class");
      TSNode classNameNode = parentClass.get().getChildByFieldName("name");
      assertNotNull(classNameNode);
      assertEquals("TestClass", testFile.getTextFromNode(classNameNode));
    }

    @Test
    @DisplayName("Should handle deeply nested structures")
    void shouldHandleDeeplyNestedStructures() {
      List<TSNode> stringLiterals = complexFile.query("(string_literal) @str");
      assertFalse(stringLiterals.isEmpty(), "Should find string literals");
      TSNode stringLiteral = stringLiterals.get(0);
      // Find parent if statement
      Optional<TSNode> parentIf = complexFile.findParentNodeByType(stringLiteral, "if_statement");
      assertTrue(parentIf.isPresent(), "Should find parent if statement");
      // Find parent for loop
      Optional<TSNode> parentFor = complexFile.findParentNodeByType(stringLiteral, "for_statement");
      assertTrue(parentFor.isPresent(), "Should find parent for statement");
      // Find parent method
      Optional<TSNode> parentMethod =
          complexFile.findParentNodeByType(stringLiteral, "method_declaration");
      assertTrue(parentMethod.isPresent(), "Should find parent method");
      // Find parent class
      Optional<TSNode> parentClass =
          complexFile.findParentNodeByType(stringLiteral, "class_declaration");
      assertTrue(parentClass.isPresent(), "Should find parent class");
    }

    @Test
    @DisplayName("Should return empty when parent type not found")
    void shouldReturnEmptyWhenParentTypeNotFound() {
      List<TSNode> classes = testFile.query("(class_declaration) @class");
      assertFalse(classes.isEmpty());
      TSNode classNode = classes.get(0);
      Optional<TSNode> parentInterface =
          testFile.findParentNodeByType(classNode, "interface_declaration");
      assertFalse(parentInterface.isPresent(), "Should not find non-existent parent type");
    }

    @Test
    @DisplayName("Should handle null parameters gracefully")
    void shouldHandleNullParametersGracefully() {
      List<TSNode> nodes = testFile.query("(identifier) @id");
      TSNode firstNode = nodes.get(0);
      Optional<TSNode> result1 = testFile.findParentNodeByType(null, "class_declaration");
      assertFalse(result1.isPresent(), "Should handle null start node");
      Optional<TSNode> result2 = testFile.findParentNodeByType(firstNode, null);
      assertFalse(result2.isPresent(), "Should handle null parent type");
      Optional<TSNode> result3 = testFile.findParentNodeByType(firstNode, "");
      assertFalse(result3.isPresent(), "Should handle empty parent type");
    }

    @Test
    @DisplayName("Should handle TSException gracefully")
    void shouldHandleTSExceptionGracefully() {
      // Create a scenario that might cause TSException
      // This is tricky to test directly, but we can test with edge cases
      List<TSNode> nodes = testFile.query("(identifier) @id");
      assertFalse(nodes.isEmpty());
      // Test with a very deep traversal that might hit tree boundaries
      TSNode deepNode = nodes.get(0);
      // Should either find the compilation unit or handle any exception gracefully
      // The method should not throw an exception
      assertDoesNotThrow(
          () -> {
            testFile.findParentNodeByType(deepNode, "some_non_existent_type");
          });
    }

    @Test
    @DisplayName("Should find immediate parent correctly")
    void shouldFindImmediateParentCorrectly() {
      List<TSNode> identifiers = testFile.query("(variable_declarator name: (identifier) @name)");
      assertFalse(identifiers.isEmpty(), "Should find variable declarator names");
      TSNode identifierNode = identifiers.get(0);
      Optional<TSNode> parentDeclarator =
          testFile.findParentNodeByType(identifierNode, "variable_declarator");
      assertTrue(parentDeclarator.isPresent(), "Should find immediate parent variable_declarator");
    }

    @Test
    @DisplayName("Should traverse multiple levels correctly")
    void shouldTraverseMultipleLevelsCorrectly() {
      // Find local variable declarations to work with
      List<TSNode> localVars = testFile.query("(local_variable_declaration) @var");
      if (!localVars.isEmpty()) {
        TSNode localVar = localVars.get(0);
        
        // Should be able to find method_declaration (parent)
        Optional<TSNode> methodDecl = testFile.findParentNodeByType(localVar, "method_declaration");
        assertTrue(methodDecl.isPresent(), "Should find method_declaration ancestor");
        
        // Should be able to find class_declaration (grandparent)
        Optional<TSNode> classDecl = testFile.findParentNodeByType(localVar, "class_declaration");
        assertTrue(classDecl.isPresent(), "Should find class_declaration ancestor");
      } else {
        // If no local variables, test with method nodes
        List<TSNode> methods = testFile.query("(method_declaration) @method");
        assertFalse(methods.isEmpty(), "Should have methods to test with");
        
        TSNode method = methods.get(0);
        Optional<TSNode> classDecl = testFile.findParentNodeByType(method, "class_declaration");
        assertTrue(classDecl.isPresent(), "Should find class_declaration ancestor from method");
      }
    }
  }

  @Nested
  @DisplayName("Error resilience tests")
  class ErrorResilienceTests {
    @Test
    @DisplayName("Should maintain functionality after modifications")
    void shouldMaintainFunctionalityAfterModifications() {
      // Modify the file
      testFile.updateSourceCode("/* modified */ " + testFile.getSourceCode());
      // Should still be able to find parent nodes
      List<TSNode> methods = testFile.query("(method_declaration) @method");
      if (!methods.isEmpty()) {
        Optional<TSNode> parentClass =
            testFile.findParentNodeByType(methods.get(0), "class_declaration");
        assertTrue(parentClass.isPresent(), "Should still find parent after modification");
      }
    }

    @Test
    @DisplayName("Should handle malformed code gracefully")
    void shouldHandleMalformedCodeGracefully() {
      String malformedCode = "public class { private void method( { } }";
      TSFile malformedFile = new TSFile(SupportedLanguage.JAVA, malformedCode);
      List<TSNode> allNodes = malformedFile.query("(_) @node");
      if (!allNodes.isEmpty()) {
        // Should not throw exceptions even with malformed code
        assertDoesNotThrow(
            () -> {
              for (TSNode node : allNodes) {
                malformedFile.findParentNodeByType(node, "class_declaration");
                malformedFile.findParentNodeByType(node, "method_declaration");
              }
            });
      }
    }
  }
}

