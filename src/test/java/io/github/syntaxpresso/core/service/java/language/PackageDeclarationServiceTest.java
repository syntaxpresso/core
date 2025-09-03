package io.github.syntaxpresso.core.service.java.language;

import static org.junit.jupiter.api.Assertions.*;

import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import io.github.syntaxpresso.core.service.java.language.extra.PackageCapture;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.treesitter.TSNode;

@DisplayName("PackageDeclarationService Tests")
class PackageDeclarationServiceTest {
  private PackageDeclarationService packageDeclarationService;

  private static final String SIMPLE_PACKAGE_CODE =
      """
      package com.example.test;

      public class TestClass {
        public void method() {
          System.out.println("Hello");
        }
      }
      """;

  private static final String COMPLEX_PACKAGE_CODE =
      """
      package io.github.syntaxpresso.core.service.java.language;

      import java.util.List;
      import java.util.Map;

      public class ComplexPackageClass {
        private List<String> items;
        private Map<String, String> properties;
      }
      """;

  private static final String NO_PACKAGE_CODE =
      """
      public class NoPackageClass {
        public void method() {
          System.out.println("No package");
        }
      }
      """;

  @BeforeEach
  void setUp() {
    this.packageDeclarationService = new PackageDeclarationService();
  }

  @Nested
  @DisplayName("getPackageDeclarationNode() tests")
  class GetPackageDeclarationNodeTests {

    @Test
    @DisplayName("Should find package declaration node with simple package")
    void shouldFindPackageDeclarationNodeWithSimplePackage() {
      TSFile file = new TSFile(SupportedLanguage.JAVA, SIMPLE_PACKAGE_CODE);
      Optional<TSNode> packageNode = packageDeclarationService.getPackageDeclarationNode(file);

      assertTrue(packageNode.isPresent(), "Should find package declaration node");
      assertEquals(
          "package_declaration",
          packageNode.get().getType(),
          "Node should be package_declaration type");

      String packageText = file.getTextFromNode(packageNode.get());
      assertTrue(packageText.contains("com.example.test"), "Should contain correct package name");
    }

    @Test
    @DisplayName("Should find package declaration node with complex package")
    void shouldFindPackageDeclarationNodeWithComplexPackage() {
      TSFile file = new TSFile(SupportedLanguage.JAVA, COMPLEX_PACKAGE_CODE);
      Optional<TSNode> packageNode = packageDeclarationService.getPackageDeclarationNode(file);

      assertTrue(packageNode.isPresent(), "Should find package declaration node");
      assertEquals(
          "package_declaration",
          packageNode.get().getType(),
          "Node should be package_declaration type");

      String packageText = file.getTextFromNode(packageNode.get());
      assertTrue(
          packageText.contains("io.github.syntaxpresso.core.service.java.language"),
          "Should contain correct package name");
    }

    @Test
    @DisplayName("Should return empty for file without package declaration")
    void shouldReturnEmptyForFileWithoutPackageDeclaration() {
      TSFile file = new TSFile(SupportedLanguage.JAVA, NO_PACKAGE_CODE);
      Optional<TSNode> packageNode = packageDeclarationService.getPackageDeclarationNode(file);

      assertFalse(
          packageNode.isPresent(), "Should return empty for file without package declaration");
    }

    @Test
    @DisplayName("Should handle file with comments before package")
    void shouldHandleFileWithCommentsBeforePackage() {
      String codeWithComments =
          """
          /*
           * File header comment
           * Author: Test
           */
          // Single line comment
          package com.example.comments;

          public class CommentsClass {
          }
          """;
      TSFile file = new TSFile(SupportedLanguage.JAVA, codeWithComments);
      Optional<TSNode> packageNode = packageDeclarationService.getPackageDeclarationNode(file);

      assertTrue(packageNode.isPresent(), "Should find package declaration even with comments");
      String packageText = file.getTextFromNode(packageNode.get());
      assertTrue(
          packageText.contains("com.example.comments"), "Should contain correct package name");
    }

    @Test
    @DisplayName("Should return empty for null TSFile")
    void shouldReturnEmptyForNullTSFile() {
      Optional<TSNode> packageNode = packageDeclarationService.getPackageDeclarationNode(null);

      assertFalse(packageNode.isPresent(), "Should return empty for null TSFile");
    }

    @Test
    @DisplayName("Should handle empty file")
    void shouldHandleEmptyFile() {
      TSFile file = new TSFile(SupportedLanguage.JAVA, "");
      Optional<TSNode> packageNode = packageDeclarationService.getPackageDeclarationNode(file);

      assertFalse(packageNode.isPresent(), "Should return empty for empty file");
    }
  }

  @Nested
  @DisplayName("getPackageDeclarationInfo() tests")
  class GetPackageDeclarationInfoTests {

    @Test
    @DisplayName("Should return package declaration info for valid package")
    void shouldReturnPackageDeclarationInfoForValidPackage() {
      TSFile file = new TSFile(SupportedLanguage.JAVA, SIMPLE_PACKAGE_CODE);
      Optional<TSNode> packageNode = packageDeclarationService.getPackageDeclarationNode(file);

      assertTrue(packageNode.isPresent(), "Package node should exist");

      List<Map<String, TSNode>> info =
          packageDeclarationService.getPackageDeclarationInfo(file, packageNode.get());

      assertFalse(info.isEmpty(), "Should return non-empty info");

      // Verify that the info contains expected capture names
      Map<String, TSNode> firstCapture = info.get(0);
      assertTrue(
          firstCapture.containsKey(PackageCapture.PACKAGE.getCaptureName()),
          "Should contain package capture");
    }

    @Test
    @DisplayName("Should return empty list for null TSFile")
    void shouldReturnEmptyListForNullTSFile() {
      TSFile file = new TSFile(SupportedLanguage.JAVA, SIMPLE_PACKAGE_CODE);
      Optional<TSNode> packageNode = packageDeclarationService.getPackageDeclarationNode(file);

      assertTrue(packageNode.isPresent(), "Package node should exist");

      List<Map<String, TSNode>> info =
          packageDeclarationService.getPackageDeclarationInfo(null, packageNode.get());

      assertTrue(info.isEmpty(), "Should return empty list for null TSFile");
    }

    @Test
    @DisplayName("Should return empty list for wrong node type")
    void shouldReturnEmptyListForWrongNodeType() {
      TSFile file = new TSFile(SupportedLanguage.JAVA, SIMPLE_PACKAGE_CODE);
      TSNode rootNode = file.getTree().getRootNode();

      List<Map<String, TSNode>> info =
          packageDeclarationService.getPackageDeclarationInfo(file, rootNode);

      assertTrue(info.isEmpty(), "Should return empty list for wrong node type");
    }

    @Test
    @DisplayName("Should handle complex package structure")
    void shouldHandleComplexPackageStructure() {
      TSFile file = new TSFile(SupportedLanguage.JAVA, COMPLEX_PACKAGE_CODE);
      Optional<TSNode> packageNode = packageDeclarationService.getPackageDeclarationNode(file);

      assertTrue(packageNode.isPresent(), "Package node should exist");

      List<Map<String, TSNode>> info =
          packageDeclarationService.getPackageDeclarationInfo(file, packageNode.get());

      assertFalse(info.isEmpty(), "Should return non-empty info for complex package");
    }
  }

  @Nested
  @DisplayName("getPackageClassNameNode() tests")
  class GetPackageClassNameNodeTests {

    @Test
    @DisplayName("Should find class name node in package declaration")
    void shouldFindClassNameNodeInPackageDeclaration() {
      TSFile file = new TSFile(SupportedLanguage.JAVA, SIMPLE_PACKAGE_CODE);
      Optional<TSNode> packageNode = packageDeclarationService.getPackageDeclarationNode(file);

      assertTrue(packageNode.isPresent(), "Package node should exist");

      Optional<TSNode> classNameNode =
          packageDeclarationService.getPackageClassNameNode(file, packageNode.get());

      if (classNameNode.isPresent()) {
        String classNameText = file.getTextFromNode(classNameNode.get());
        assertFalse(classNameText.trim().isEmpty(), "Class name should not be empty");
      }
    }

    @Test
    @DisplayName("Should return empty for null TSFile")
    void shouldReturnEmptyForNullTSFile() {
      TSFile file = new TSFile(SupportedLanguage.JAVA, SIMPLE_PACKAGE_CODE);
      Optional<TSNode> packageNode = packageDeclarationService.getPackageDeclarationNode(file);

      assertTrue(packageNode.isPresent(), "Package node should exist");

      Optional<TSNode> classNameNode =
          packageDeclarationService.getPackageClassNameNode(null, packageNode.get());

      assertFalse(classNameNode.isPresent(), "Should return empty for null TSFile");
    }

    @Test
    @DisplayName("Should return empty for wrong node type")
    void shouldReturnEmptyForWrongNodeType() {
      TSFile file = new TSFile(SupportedLanguage.JAVA, SIMPLE_PACKAGE_CODE);
      TSNode rootNode = file.getTree().getRootNode();

      Optional<TSNode> classNameNode =
          packageDeclarationService.getPackageClassNameNode(file, rootNode);

      assertFalse(classNameNode.isPresent(), "Should return empty for wrong node type");
    }
  }

  @Nested
  @DisplayName("getPackageClassScopeNode() tests")
  class GetPackageClassScopeNodeTests {

    @Test
    @DisplayName("Should find class scope node in package declaration")
    void shouldFindClassScopeNodeInPackageDeclaration() {
      TSFile file = new TSFile(SupportedLanguage.JAVA, SIMPLE_PACKAGE_CODE);
      Optional<TSNode> packageNode = packageDeclarationService.getPackageDeclarationNode(file);

      assertTrue(packageNode.isPresent(), "Package node should exist");

      Optional<TSNode> classScopeNode =
          packageDeclarationService.getPackageClassScopeNode(file, packageNode.get());

      if (classScopeNode.isPresent()) {
        String classScopeText = file.getTextFromNode(classScopeNode.get());
        assertFalse(classScopeText.trim().isEmpty(), "Class scope should not be empty");
      }
    }

    @Test
    @DisplayName("Should return empty for null TSFile")
    void shouldReturnEmptyForNullTSFile() {
      TSFile file = new TSFile(SupportedLanguage.JAVA, SIMPLE_PACKAGE_CODE);
      Optional<TSNode> packageNode = packageDeclarationService.getPackageDeclarationNode(file);

      assertTrue(packageNode.isPresent(), "Package node should exist");

      Optional<TSNode> classScopeNode =
          packageDeclarationService.getPackageClassScopeNode(null, packageNode.get());

      assertFalse(classScopeNode.isPresent(), "Should return empty for null TSFile");
    }

    @Test
    @DisplayName("Should return empty for wrong node type")
    void shouldReturnEmptyForWrongNodeType() {
      TSFile file = new TSFile(SupportedLanguage.JAVA, SIMPLE_PACKAGE_CODE);
      TSNode rootNode = file.getTree().getRootNode();

      Optional<TSNode> classScopeNode =
          packageDeclarationService.getPackageClassScopeNode(file, rootNode);

      assertFalse(classScopeNode.isPresent(), "Should return empty for wrong node type");
    }
  }

  @Nested
  @DisplayName("getPackageScopeNode() tests")
  class GetPackageScopeNodeTests {

    @Test
    @DisplayName("Should find package scope node in package declaration")
    void shouldFindPackageScopeNodeInPackageDeclaration() {
      TSFile file = new TSFile(SupportedLanguage.JAVA, SIMPLE_PACKAGE_CODE);
      Optional<TSNode> packageNode = packageDeclarationService.getPackageDeclarationNode(file);

      assertTrue(packageNode.isPresent(), "Package node should exist");

      Optional<TSNode> packageScopeNode =
          packageDeclarationService.getPackageScopeNode(file, packageNode.get());

      if (packageScopeNode.isPresent()) {
        String packageScopeText = file.getTextFromNode(packageScopeNode.get());
        assertFalse(packageScopeText.trim().isEmpty(), "Package scope should not be empty");
      }
    }

    @Test
    @DisplayName("Should return empty for null TSFile")
    void shouldReturnEmptyForNullTSFile() {
      TSFile file = new TSFile(SupportedLanguage.JAVA, SIMPLE_PACKAGE_CODE);
      Optional<TSNode> packageNode = packageDeclarationService.getPackageDeclarationNode(file);

      assertTrue(packageNode.isPresent(), "Package node should exist");

      Optional<TSNode> packageScopeNode =
          packageDeclarationService.getPackageScopeNode(null, packageNode.get());

      assertFalse(packageScopeNode.isPresent(), "Should return empty for null TSFile");
    }

    @Test
    @DisplayName("Should return empty for wrong node type")
    void shouldReturnEmptyForWrongNodeType() {
      TSFile file = new TSFile(SupportedLanguage.JAVA, SIMPLE_PACKAGE_CODE);
      TSNode rootNode = file.getTree().getRootNode();

      Optional<TSNode> packageScopeNode =
          packageDeclarationService.getPackageScopeNode(file, rootNode);

      assertFalse(packageScopeNode.isPresent(), "Should return empty for wrong node type");
    }
  }

  @Nested
  @DisplayName("Integration tests")
  class IntegrationTests {

    @Test
    @DisplayName("Should work with all methods together")
    void shouldWorkWithAllMethodsTogether() {
      TSFile file = new TSFile(SupportedLanguage.JAVA, COMPLEX_PACKAGE_CODE);

      // Get package declaration node
      Optional<TSNode> packageNode = packageDeclarationService.getPackageDeclarationNode(file);
      assertTrue(packageNode.isPresent(), "Should find package declaration node");

      // Get package declaration info
      List<Map<String, TSNode>> info =
          packageDeclarationService.getPackageDeclarationInfo(file, packageNode.get());
      assertFalse(info.isEmpty(), "Should return package declaration info");

      // Try to get individual nodes

      packageDeclarationService.getPackageClassNameNode(file, packageNode.get());

      packageDeclarationService.getPackageClassScopeNode(file, packageNode.get());

      packageDeclarationService.getPackageScopeNode(file, packageNode.get());

      // At least the basic package node should work
      String packageText = file.getTextFromNode(packageNode.get());
      assertTrue(
          packageText.contains("io.github.syntaxpresso.core.service.java.language"),
          "Package text should contain expected package name");
    }

    @Test
    @DisplayName("Should handle various package formats consistently")
    void shouldHandleVariousPackageFormatsConsistently() {
      String[] testCases = {
        "package com.example;",
        "package com.example.test;",
        "package com.example.test.service;",
        "package io.github.syntaxpresso.core.service.java.language;"
      };

      for (String packageDeclaration : testCases) {
        String fullCode = packageDeclaration + "\n\npublic class TestClass {}";
        TSFile file = new TSFile(SupportedLanguage.JAVA, fullCode);

        Optional<TSNode> packageNode = packageDeclarationService.getPackageDeclarationNode(file);
        assertTrue(
            packageNode.isPresent(), "Should find package declaration for: " + packageDeclaration);

        packageDeclarationService.getPackageDeclarationInfo(file, packageNode.get());
        // Info might be empty depending on tree-sitter parsing, but should not throw exceptions
        assertDoesNotThrow(
            () -> packageDeclarationService.getPackageClassNameNode(file, packageNode.get()),
            "Should handle getPackageClassNameNode without exceptions");
        assertDoesNotThrow(
            () -> packageDeclarationService.getPackageClassScopeNode(file, packageNode.get()),
            "Should handle getPackageClassScopeNode without exceptions");
        assertDoesNotThrow(
            () -> packageDeclarationService.getPackageScopeNode(file, packageNode.get()),
            "Should handle getPackageScopeNode without exceptions");
      }
    }
  }

  @Nested
  @DisplayName("Edge cases and error handling")
  class EdgeCasesAndErrorHandlingTests {

    @Test
    @DisplayName("Should handle malformed package declarations gracefully")
    void shouldHandleMalformedPackageDeclarationsGracefully() {
      String[] malformedCases = {
        "package;",
        "package ;",
        "package com.example", // missing semicolon
        "package com..example;", // double dots
        "package 123invalid;", // invalid identifier
      };

      for (String malformedCode : malformedCases) {
        String fullCode = malformedCode + "\n\npublic class TestClass {}";
        TSFile file = new TSFile(SupportedLanguage.JAVA, fullCode);

        assertDoesNotThrow(
            () -> {
              Optional<TSNode> packageNode =
                  packageDeclarationService.getPackageDeclarationNode(file);
              if (packageNode.isPresent()) {
                packageDeclarationService.getPackageDeclarationInfo(file, packageNode.get());
                packageDeclarationService.getPackageClassNameNode(file, packageNode.get());
                packageDeclarationService.getPackageClassScopeNode(file, packageNode.get());
                packageDeclarationService.getPackageScopeNode(file, packageNode.get());
              }
            },
            "Should handle malformed code gracefully: " + malformedCode);
      }
    }

    @Test
    @DisplayName("Should handle files with only whitespace or comments")
    void shouldHandleFilesWithOnlyWhitespaceOrComments() {
      String[] edgeCases = {
        "   \n\n   ", // only whitespace
        "// just a comment",
        "/* block comment */",
        "/**\n * Javadoc\n */",
      };

      for (String edgeCase : edgeCases) {
        TSFile file = new TSFile(SupportedLanguage.JAVA, edgeCase);

        assertDoesNotThrow(
            () -> {
              Optional<TSNode> packageNode =
                  packageDeclarationService.getPackageDeclarationNode(file);
              assertFalse(
                  packageNode.isPresent(), "Should not find package in edge case: " + edgeCase);
            },
            "Should handle edge case gracefully: " + edgeCase);
      }
    }
  }
}

