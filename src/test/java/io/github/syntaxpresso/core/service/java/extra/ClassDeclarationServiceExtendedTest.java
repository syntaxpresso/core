package io.github.syntaxpresso.core.service.java.extra;

import static org.junit.jupiter.api.Assertions.*;

import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.treesitter.TSNode;

@DisplayName("ClassDeclarationService Extended Tests")
class ClassDeclarationServiceExtendedTest {
  private ClassDeclarationService classDeclarationService;
  private TSFile testFile;
  private TSFile multiClassFile;

  @TempDir
  File tempDir;

  @BeforeEach
  void setUp() {
    // Use ProgramService for consistent service setup
    ProgramService programService = new ProgramService();
    classDeclarationService = programService.getClassDeclarationService();

    String javaCode =
        """
        package io.github.test;
        public class Calculator {
          private String name;
          private int count;
          private Calculator helper;
          
          public Calculator() {
            this.name = "default";
          }
          
          public void process() {
            System.out.println("Processing");
          }
        }
        """;
    testFile = new TSFile(SupportedLanguage.JAVA, javaCode);

    String multiClassCode =
        """
        package io.github.test;
        
        class Helper {
          private String value;
        }
        
        public class MainClass {
          private Helper helper;
          private String title;
        }
        
        class Utility {
          private int count;
        }
        """;
    multiClassFile = new TSFile(SupportedLanguage.JAVA, multiClassCode);
  }

  @Nested
  @DisplayName("getMainClass() tests")
  class GetMainClassTests {

    @Test
    @DisplayName("Should find main class when file name matches class name")
    void shouldFindMainClassWhenFileNameMatches() throws IOException {
      // Create a temporary file with matching name
      File calculatorFile = new File(tempDir, "Calculator.java");
      Files.write(calculatorFile.toPath(), testFile.getSourceCode().getBytes());
      
      TSFile fileWithCorrectName = new TSFile(SupportedLanguage.JAVA, calculatorFile.toPath());
      
      Optional<TSNode> mainClass = classDeclarationService.getMainClass(fileWithCorrectName);
      assertTrue(mainClass.isPresent(), "Should find main class");
      
      Optional<String> className = classDeclarationService.getClassName(fileWithCorrectName, mainClass.get());
      assertTrue(className.isPresent());
      assertEquals("Calculator", className.get());
    }

    @Test
    @DisplayName("Should find public main class in multi-class file")
    void shouldFindPublicMainClassInMultiClassFile() throws IOException {
      File mainClassFile = new File(tempDir, "MainClass.java");
      Files.write(mainClassFile.toPath(), multiClassFile.getSourceCode().getBytes());
      
      TSFile fileWithCorrectName = new TSFile(SupportedLanguage.JAVA, mainClassFile.toPath());
      
      Optional<TSNode> mainClass = classDeclarationService.getMainClass(fileWithCorrectName);
      assertTrue(mainClass.isPresent(), "Should find main class");
      
      Optional<String> className = classDeclarationService.getClassName(fileWithCorrectName, mainClass.get());
      assertTrue(className.isPresent());
      assertEquals("MainClass", className.get());
    }

    @Test
    @DisplayName("Should return empty when no matching class found")
    void shouldReturnEmptyWhenNoMatchingClassFound() throws IOException {
      File wrongNameFile = new File(tempDir, "WrongName.java");
      Files.write(wrongNameFile.toPath(), testFile.getSourceCode().getBytes());
      
      TSFile fileWithWrongName = new TSFile(SupportedLanguage.JAVA, wrongNameFile.toPath());
      
      Optional<TSNode> mainClass = classDeclarationService.getMainClass(fileWithWrongName);
      assertFalse(mainClass.isPresent(), "Should not find main class with wrong name");
    }

    @Test
    @DisplayName("Should handle null file gracefully")
    void shouldHandleNullFileGracefully() {
      Optional<TSNode> mainClass = classDeclarationService.getMainClass(null);
      assertFalse(mainClass.isPresent());
    }

    @Test
    @DisplayName("Should handle file without backing File gracefully")
    void shouldHandleFileWithoutBackingFileGracefully() {
      // Create TSFile from string (no backing file)
      TSFile stringFile = new TSFile(SupportedLanguage.JAVA, "public class Test {}");
      
      Optional<TSNode> mainClass = classDeclarationService.getMainClass(stringFile);
      assertFalse(mainClass.isPresent());
    }
  }

  @Nested
  @DisplayName("getFieldNameNode() tests")
  class GetFieldNameNodeTests {

    @Test
    @DisplayName("Should extract field name from field declaration")
    void shouldExtractFieldNameFromFieldDeclaration() {
      List<TSNode> fieldDeclarations = testFile.query("(field_declaration) @field");
      assertFalse(fieldDeclarations.isEmpty(), "Should find field declarations");

      for (TSNode fieldDecl : fieldDeclarations) {
        Optional<TSNode> fieldNameNode = classDeclarationService.getFieldNameNode(testFile, fieldDecl);
        assertTrue(fieldNameNode.isPresent(), "Should find field name node");
        
        String fieldName = testFile.getTextFromNode(fieldNameNode.get());
        assertTrue(List.of("name", "count", "helper").contains(fieldName), 
                  "Field name should be one of the expected fields: " + fieldName);
      }
    }

    @Test
    @DisplayName("Should handle null parameters gracefully")
    void shouldHandleNullParametersGracefully() {
      List<TSNode> fieldDeclarations = testFile.query("(field_declaration) @field");
      TSNode firstField = fieldDeclarations.get(0);

      assertFalse(classDeclarationService.getFieldNameNode(null, firstField).isPresent());
      assertFalse(classDeclarationService.getFieldNameNode(testFile, null).isPresent());
      assertFalse(classDeclarationService.getFieldNameNode(null, null).isPresent());
    }

    @Test
    @DisplayName("Should handle non-field-declaration nodes gracefully")
    void shouldHandleNonFieldDeclarationNodesGracefully() {
      List<TSNode> methodDeclarations = testFile.query("(method_declaration) @method");
      assertFalse(methodDeclarations.isEmpty());

      Optional<TSNode> result = classDeclarationService.getFieldNameNode(testFile, methodDeclarations.get(0));
      assertFalse(result.isPresent(), "Should not find field name in method declaration");
    }

    @Test
    @DisplayName("Should handle field declaration without declarator")
    void shouldHandleFieldDeclarationWithoutDeclarator() {
      // Create a malformed field declaration scenario
      String malformedCode = "public class Test { private; }";
      TSFile malformedFile = new TSFile(SupportedLanguage.JAVA, malformedCode);
      
      List<TSNode> nodes = malformedFile.query("(ERROR) @error");
      if (!nodes.isEmpty()) {
        Optional<TSNode> result = classDeclarationService.getFieldNameNode(malformedFile, nodes.get(0));
        assertFalse(result.isPresent(), "Should handle malformed field gracefully");
      }
    }
  }

  @Nested
  @DisplayName("getFieldTypeNode() tests")
  class GetFieldTypeNodeTests {

    @Test
    @DisplayName("Should extract field type from field declaration")
    void shouldExtractFieldTypeFromFieldDeclaration() {
      List<TSNode> fieldDeclarations = testFile.query("(field_declaration) @field");
      assertFalse(fieldDeclarations.isEmpty(), "Should find field declarations");

      boolean foundStringType = false;
      boolean foundIntType = false;
      boolean foundCalculatorType = false;

      for (TSNode fieldDecl : fieldDeclarations) {
        Optional<TSNode> fieldTypeNode = classDeclarationService.getFieldTypeNode(testFile, fieldDecl);
        assertTrue(fieldTypeNode.isPresent(), "Should find field type node");
        
        String fieldType = testFile.getTextFromNode(fieldTypeNode.get());
        switch (fieldType) {
          case "String":
            foundStringType = true;
            break;
          case "int":
            foundIntType = true;
            break;
          case "Calculator":
            foundCalculatorType = true;
            break;
        }
      }

      assertTrue(foundStringType, "Should find String type field");
      assertTrue(foundIntType, "Should find int type field");
      assertTrue(foundCalculatorType, "Should find Calculator type field");
    }

    @Test
    @DisplayName("Should handle null parameters gracefully")
    void shouldHandleNullParametersGracefully() {
      List<TSNode> fieldDeclarations = testFile.query("(field_declaration) @field");
      TSNode firstField = fieldDeclarations.get(0);

      assertFalse(classDeclarationService.getFieldTypeNode(null, firstField).isPresent());
      assertFalse(classDeclarationService.getFieldTypeNode(testFile, null).isPresent());
      assertFalse(classDeclarationService.getFieldTypeNode(null, null).isPresent());
    }

    @Test
    @DisplayName("Should handle non-field-declaration nodes gracefully")
    void shouldHandleNonFieldDeclarationNodesGracefully() {
      List<TSNode> methodDeclarations = testFile.query("(method_declaration) @method");
      assertFalse(methodDeclarations.isEmpty());

      Optional<TSNode> result = classDeclarationService.getFieldTypeNode(testFile, methodDeclarations.get(0));
      assertFalse(result.isPresent(), "Should not find field type in method declaration");
    }

    @Test
    @DisplayName("Should handle field declaration without type")
    void shouldHandleFieldDeclarationWithoutType() {
      // In normal Java, this shouldn't happen, but we should handle edge cases
      String edgeCaseCode = "public class Test { /* field without type */ }";
      TSFile edgeCaseFile = new TSFile(SupportedLanguage.JAVA, edgeCaseCode);
      
      List<TSNode> classDeclarations = edgeCaseFile.query("(class_declaration) @class");
      if (!classDeclarations.isEmpty()) {
        Optional<TSNode> result = classDeclarationService.getFieldTypeNode(edgeCaseFile, classDeclarations.get(0));
        assertFalse(result.isPresent(), "Should handle missing field type gracefully");
      }
    }
  }

  @Nested
  @DisplayName("Integration tests")
  class IntegrationTests {

    @Test
    @DisplayName("Should work together to analyze complete field information")
    void shouldWorkTogetherToAnalyzeCompleteFieldInformation() {
      List<TSNode> fieldDeclarations = testFile.query("(field_declaration) @field");
      
      for (TSNode fieldDecl : fieldDeclarations) {
        Optional<TSNode> nameNode = classDeclarationService.getFieldNameNode(testFile, fieldDecl);
        Optional<TSNode> typeNode = classDeclarationService.getFieldTypeNode(testFile, fieldDecl);
        
        assertTrue(nameNode.isPresent(), "Should find field name");
        assertTrue(typeNode.isPresent(), "Should find field type");
        
        String fieldName = testFile.getTextFromNode(nameNode.get());
        String fieldType = testFile.getTextFromNode(typeNode.get());
        
        // Verify expected field combinations
        switch (fieldName) {
          case "name":
            assertEquals("String", fieldType);
            break;
          case "count":
            assertEquals("int", fieldType);
            break;
          case "helper":
            assertEquals("Calculator", fieldType);
            break;
          default:
            fail("Unexpected field name: " + fieldName);
        }
      }
    }
  }
}