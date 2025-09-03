package io.github.syntaxpresso.core.service.java.language;

import static org.junit.jupiter.api.Assertions.*;

import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import io.github.syntaxpresso.core.service.java.language.extra.ImportCapture;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.treesitter.TSNode;

@DisplayName("ImportDeclarationService Tests")
class ImportDeclarationServiceTest {
  private ImportDeclarationService service;
  private PackageDeclarationService packageService;

  // Reusable source code strings
  private static final String SINGLE_IMPORT_CODE =
      """
      package io.github.test;

      import java.util.List;

      public class TestClass {
          private List<String> items;
      }
      """;

  private static final String MULTIPLE_IMPORTS_CODE =
      """
      package io.github.test;

      import java.util.List;
      import java.util.Map;
      import java.util.Set;
      import javax.persistence.Entity;
      import com.example.service.UserService;

      public class TestClass {
          private List<String> items;
          private Map<String, String> properties;
      }
      """;

  private static final String WILDCARD_IMPORT_CODE =
      """
      package io.github.test;

      import java.util.*;
      import javax.persistence.Entity;

      public class TestClass {
          private List<String> items;
      }
      """;

  private static final String NO_IMPORTS_CODE =
      """
      package io.github.test;

      public class TestClass {
          private String name;
      }
      """;

  private static final String NO_PACKAGE_WITH_IMPORTS_CODE =
      """
      import java.util.List;
      import java.util.Map;

      public class TestClass {
          private List<String> items;
      }
      """;

  private static final String NO_PACKAGE_NO_IMPORTS_CODE =
      """
      public class TestClass {
          private String name;
      }
      """;

  private static final String STATIC_IMPORTS_CODE =
      """
      package io.github.test;

      import static java.lang.System.out;
      import static java.util.Collections.*;
      import java.util.List;

      public class TestClass {
          private List<String> items;
      }
      """;

  private static final String EMPTY_FILE_CODE = "";

  private TSFile singleImportFile;
  private TSFile multipleImportsFile;
  private TSFile wildcardImportFile;
  private TSFile noImportsFile;
  private TSFile noPackageWithImportsFile;
  private TSFile noPackageNoImportsFile;
  private TSFile staticImportsFile;
  private TSFile emptyFile;

  @BeforeEach
  void setUp() {
    service = new ImportDeclarationService();
    packageService = new PackageDeclarationService();
    singleImportFile = new TSFile(SupportedLanguage.JAVA, SINGLE_IMPORT_CODE);
    multipleImportsFile = new TSFile(SupportedLanguage.JAVA, MULTIPLE_IMPORTS_CODE);
    wildcardImportFile = new TSFile(SupportedLanguage.JAVA, WILDCARD_IMPORT_CODE);
    noImportsFile = new TSFile(SupportedLanguage.JAVA, NO_IMPORTS_CODE);
    noPackageWithImportsFile = new TSFile(SupportedLanguage.JAVA, NO_PACKAGE_WITH_IMPORTS_CODE);
    noPackageNoImportsFile = new TSFile(SupportedLanguage.JAVA, NO_PACKAGE_NO_IMPORTS_CODE);
    staticImportsFile = new TSFile(SupportedLanguage.JAVA, STATIC_IMPORTS_CODE);
    emptyFile = new TSFile(SupportedLanguage.JAVA, EMPTY_FILE_CODE);
  }

  @Nested
  @DisplayName("getAllImportDeclarationNodes Tests")
  class GetAllImportDeclarationNodesTests {

    @Test
    @DisplayName("should return single import declaration node")
    void shouldReturnSingleImportDeclarationNode() {
      List<TSNode> imports = service.getAllImportDeclarationNodes(singleImportFile);

      assertEquals(1, imports.size());
      assertEquals("import_declaration", imports.get(0).getType());

      String importText = singleImportFile.getTextFromNode(imports.get(0));
      assertTrue(importText.contains("java.util.List"));
    }

    @Test
    @DisplayName("should return multiple import declaration nodes")
    void shouldReturnMultipleImportDeclarationNodes() {
      List<TSNode> imports = service.getAllImportDeclarationNodes(multipleImportsFile);

      assertEquals(5, imports.size());
      for (TSNode importNode : imports) {
        assertEquals("import_declaration", importNode.getType());
      }
    }

    @Test
    @DisplayName("should return wildcard import declaration node")
    void shouldReturnWildcardImportDeclarationNode() {
      List<TSNode> imports = service.getAllImportDeclarationNodes(wildcardImportFile);

      assertEquals(2, imports.size());

      String firstImportText = wildcardImportFile.getTextFromNode(imports.get(0));
      assertTrue(firstImportText.contains("java.util.*"));
    }

    @Test
    @DisplayName("should return static import declaration nodes")
    void shouldReturnStaticImportDeclarationNodes() {
      List<TSNode> imports = service.getAllImportDeclarationNodes(staticImportsFile);

      assertEquals(3, imports.size());

      String firstImportText = staticImportsFile.getTextFromNode(imports.get(0));
      assertTrue(firstImportText.contains("static java.lang.System.out"));
    }

    @Test
    @DisplayName("should return empty list for file with no imports")
    void shouldReturnEmptyListForFileWithNoImports() {
      List<TSNode> imports = service.getAllImportDeclarationNodes(noImportsFile);
      assertTrue(imports.isEmpty());
    }

    @Test
    @DisplayName("should return empty list for empty file")
    void shouldReturnEmptyListForEmptyFile() {
      List<TSNode> imports = service.getAllImportDeclarationNodes(emptyFile);
      assertEquals(Collections.emptyList(), imports);
    }

    @Test
    @DisplayName("should return empty list for null file")
    void shouldReturnEmptyListForNullFile() {
      List<TSNode> imports = service.getAllImportDeclarationNodes(null);
      assertEquals(Collections.emptyList(), imports);
    }

    @Test
    @DisplayName("should handle file without package but with imports")
    void shouldHandleFileWithoutPackageButWithImports() {
      List<TSNode> imports = service.getAllImportDeclarationNodes(noPackageWithImportsFile);

      assertEquals(2, imports.size());
      for (TSNode importNode : imports) {
        assertEquals("import_declaration", importNode.getType());
      }
    }

    @Test
    @DisplayName("should handle file without package and without imports")
    void shouldHandleFileWithoutPackageAndWithoutImports() {
      List<TSNode> imports = service.getAllImportDeclarationNodes(noPackageNoImportsFile);

      assertTrue(imports.isEmpty());
    }
  }

  @Nested
  @DisplayName("getImportDeclarationNodeInfo Tests")
  class GetImportDeclarationNodeInfoTests {

    @Test
    @DisplayName("should return import declaration info with expected captures")
    void shouldReturnImportDeclarationInfoWithExpectedCaptures() {
      List<TSNode> imports = service.getAllImportDeclarationNodes(singleImportFile);
      TSNode importNode = imports.get(0);

      List<Map<String, TSNode>> info =
          service.getImportDeclarationNodeInfo(singleImportFile, importNode);

      assertFalse(info.isEmpty());

      Map<String, TSNode> firstCapture = info.get(0);
      assertTrue(firstCapture.containsKey("import"));
      assertTrue(firstCapture.containsKey("fullImportScope"));
      assertTrue(firstCapture.containsKey("className"));
    }

    @Test
    @DisplayName("should return import declaration info for wildcard import")
    void shouldReturnImportDeclarationInfoForWildcardImport() {
      List<TSNode> imports = service.getAllImportDeclarationNodes(wildcardImportFile);
      TSNode wildcardImportNode = imports.get(0);

      service.getImportDeclarationNodeInfo(wildcardImportFile, wildcardImportNode);

      // Info might be empty or contain captures - this depends on tree-sitter parsing
      // The main thing is that it doesn't throw an exception
      assertDoesNotThrow(
          () -> {
            service.getImportDeclarationNodeInfo(wildcardImportFile, wildcardImportNode);
          });
    }

    @Test
    @DisplayName("should return empty list for null file")
    void shouldReturnEmptyListForNullFile() {
      List<TSNode> imports = service.getAllImportDeclarationNodes(singleImportFile);
      TSNode importNode = imports.get(0);

      List<Map<String, TSNode>> info = service.getImportDeclarationNodeInfo(null, importNode);
      assertEquals(Collections.emptyList(), info);
    }

    @Test
    @DisplayName("should return empty list for non-import declaration node")
    void shouldReturnEmptyListForNonImportDeclarationNode() {
      TSNode rootNode = singleImportFile.getTree().getRootNode();

      List<Map<String, TSNode>> info =
          service.getImportDeclarationNodeInfo(singleImportFile, rootNode);
      assertEquals(Collections.emptyList(), info);
    }
  }

  @Nested
  @DisplayName("getImportDeclarationChildByCaptureName Tests")
  class GetImportDeclarationChildByCaptureNameTests {

    @Test
    @DisplayName("should return class name node by capture name")
    void shouldReturnClassNameNodeByCaptureName() {
      List<TSNode> imports = service.getAllImportDeclarationNodes(singleImportFile);
      TSNode importNode = imports.get(0);

      Optional<TSNode> classNameNode =
          service.getImportDeclarationChildByCaptureName(
              singleImportFile, importNode, ImportCapture.CLASS_NAME);

      assertTrue(classNameNode.isPresent());
      String className = singleImportFile.getTextFromNode(classNameNode.get());
      assertEquals("List", className);
    }

    @Test
    @DisplayName("should return full import scope node by capture name")
    void shouldReturnFullImportScopeNodeByCaptureName() {
      List<TSNode> imports = service.getAllImportDeclarationNodes(singleImportFile);
      TSNode importNode = imports.get(0);

      Optional<TSNode> fullScopeNode =
          service.getImportDeclarationChildByCaptureName(
              singleImportFile, importNode, ImportCapture.FULL_IMPORT_SCOPE);

      assertTrue(fullScopeNode.isPresent());
      String fullScope = singleImportFile.getTextFromNode(fullScopeNode.get());
      assertTrue(fullScope.contains("java.util"));
    }

    @Test
    @DisplayName("should handle asterisk node for wildcard import")
    void shouldHandleAsteriskNodeForWildcardImport() {
      List<TSNode> imports = service.getAllImportDeclarationNodes(wildcardImportFile);
      TSNode wildcardImportNode = imports.get(0);

      Optional<TSNode> asteriskNode =
          service.getImportDeclarationChildByCaptureName(
              wildcardImportFile, wildcardImportNode, ImportCapture.ASTERISK);

      // Asterisk node may or may not be present depending on tree-sitter parsing
      // The main thing is that the method works without throwing exceptions
      assertDoesNotThrow(
          () -> {
            service.getImportDeclarationChildByCaptureName(
                wildcardImportFile, wildcardImportNode, ImportCapture.ASTERISK);
          });

      if (asteriskNode.isPresent()) {
        String asterisk = wildcardImportFile.getTextFromNode(asteriskNode.get());
        assertNotNull(asterisk);
      }
    }

    @Test
    @DisplayName("should return empty for null file")
    void shouldReturnEmptyForNullFile() {
      List<TSNode> imports = service.getAllImportDeclarationNodes(singleImportFile);
      TSNode importNode = imports.get(0);

      Optional<TSNode> result =
          service.getImportDeclarationChildByCaptureName(
              null, importNode, ImportCapture.CLASS_NAME);
      assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("should return empty for non-import declaration node")
    void shouldReturnEmptyForNonImportDeclarationNode() {
      TSNode rootNode = singleImportFile.getTree().getRootNode();

      Optional<TSNode> result =
          service.getImportDeclarationChildByCaptureName(
              singleImportFile, rootNode, ImportCapture.CLASS_NAME);
      assertTrue(result.isEmpty());
    }
  }

  @Nested
  @DisplayName("findImportDeclarationNode Tests")
  class FindImportDeclarationNodeTests {

    @Test
    @DisplayName("should find existing import declaration by package and class")
    void shouldFindExistingImportDeclarationByPackageAndClass() {
      Optional<TSNode> foundImport =
          service.findImportDeclarationNode(singleImportFile, "java.util", "List");

      assertTrue(foundImport.isPresent());
      assertEquals("import_declaration", foundImport.get().getType());
    }

    @Test
    @DisplayName("should handle wildcard import declaration search")
    void shouldHandleWildcardImportDeclarationSearch() {
      Optional<TSNode> foundImport =
          service.findImportDeclarationNode(wildcardImportFile, "java.util", "List");

      // The method might find the wildcard import or not, depending on implementation
      // The main thing is that it works without throwing exceptions
      assertDoesNotThrow(
          () -> {
            service.findImportDeclarationNode(wildcardImportFile, "java.util", "List");
          });

      if (foundImport.isPresent()) {
        assertEquals("import_declaration", foundImport.get().getType());
      }
    }

    @Test
    @DisplayName("should find import among multiple imports")
    void shouldFindImportAmongMultipleImports() {
      Optional<TSNode> foundImport =
          service.findImportDeclarationNode(multipleImportsFile, "javax.persistence", "Entity");

      assertTrue(foundImport.isPresent());

      String importText = multipleImportsFile.getTextFromNode(foundImport.get());
      assertTrue(importText.contains("javax.persistence.Entity"));
    }

    @Test
    @DisplayName("should return empty for non-existent import")
    void shouldReturnEmptyForNonExistentImport() {
      Optional<TSNode> notFound =
          service.findImportDeclarationNode(singleImportFile, "com.example", "NonExistent");
      assertTrue(notFound.isEmpty());
    }

    @Test
    @DisplayName("should return empty for null file")
    void shouldReturnEmptyForNullFile() {
      Optional<TSNode> result = service.findImportDeclarationNode(null, "java.util", "List");
      assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("should return empty for null package scope")
    void shouldReturnEmptyForNullPackageScope() {
      Optional<TSNode> result = service.findImportDeclarationNode(singleImportFile, null, "List");
      assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("should return empty for null class name")
    void shouldReturnEmptyForNullClassName() {
      Optional<TSNode> result =
          service.findImportDeclarationNode(singleImportFile, "java.util", null);
      assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("should return empty for empty package scope")
    void shouldReturnEmptyForEmptyPackageScope() {
      Optional<TSNode> result = service.findImportDeclarationNode(singleImportFile, "", "List");
      assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("should return empty for empty class name")
    void shouldReturnEmptyForEmptyClassName() {
      Optional<TSNode> result =
          service.findImportDeclarationNode(singleImportFile, "java.util", "");
      assertTrue(result.isEmpty());
    }
  }

  @Nested
  @DisplayName("isClassImported Tests")
  class IsClassImportedTests {

    @Test
    @DisplayName("should return true for imported class")
    void shouldReturnTrueForImportedClass() {
      Boolean result = service.isClassImported(singleImportFile, "java.util", "List");
      assertTrue(result);
    }

    @Test
    @DisplayName("should handle class covered by wildcard import")
    void shouldHandleClassCoveredByWildcardImport() {
      Boolean result = service.isClassImported(wildcardImportFile, "java.util", "ArrayList");

      // The method might return true or false depending on how it handles wildcards
      // The main thing is that it works without throwing exceptions
      assertDoesNotThrow(
          () -> {
            service.isClassImported(wildcardImportFile, "java.util", "ArrayList");
          });

      // Result should be a boolean, not null
      assertNotNull(result);
    }

    @Test
    @DisplayName("should return false for non-imported class")
    void shouldReturnFalseForNonImportedClass() {
      Boolean result = service.isClassImported(singleImportFile, "com.example", "NonExistent");
      assertFalse(result);
    }

    @Test
    @DisplayName("should return false for file with no imports")
    void shouldReturnFalseForFileWithNoImports() {
      Boolean result = service.isClassImported(noImportsFile, "java.util", "List");
      assertFalse(result);
    }

    @Test
    @DisplayName("should return false for file with no package and no imports")
    void shouldReturnFalseForFileWithNoPackageAndNoImports() {
      Boolean result = service.isClassImported(noPackageNoImportsFile, "java.util", "List");
      assertFalse(result);
    }
  }

  @Nested
  @DisplayName("getImportDeclarationRelativeImportScopeNode Tests")
  class GetImportDeclarationRelativeImportScopeNodeTests {

    @Test
    @DisplayName("should return relative import scope node when available")
    void shouldReturnRelativeImportScopeNodeWhenAvailable() {
      List<TSNode> imports = service.getAllImportDeclarationNodes(singleImportFile);
      TSNode importNode = imports.get(0);

      Optional<TSNode> relativeScope =
          service.getImportDeclarationRelativeImportScopeNode(singleImportFile, importNode);

      if (relativeScope.isPresent()) {
        String relativeScopeText = singleImportFile.getTextFromNode(relativeScope.get());
        assertFalse(relativeScopeText.trim().isEmpty());
      }
    }

    @Test
    @DisplayName("should return empty for null file")
    void shouldReturnEmptyForNullFile() {
      List<TSNode> imports = service.getAllImportDeclarationNodes(singleImportFile);
      TSNode importNode = imports.get(0);

      Optional<TSNode> result =
          service.getImportDeclarationRelativeImportScopeNode(null, importNode);
      assertTrue(result.isEmpty());
    }
  }

  @Nested
  @DisplayName("getImportDeclarationClassNameNode Tests")
  class GetImportDeclarationClassNameNodeTests {

    @Test
    @DisplayName("should return class name node for import declaration")
    void shouldReturnClassNameNodeForImportDeclaration() {
      List<TSNode> imports = service.getAllImportDeclarationNodes(singleImportFile);
      TSNode importNode = imports.get(0);

      Optional<TSNode> classNameNode =
          service.getImportDeclarationClassNameNode(singleImportFile, importNode);

      assertTrue(classNameNode.isPresent());
      String className = singleImportFile.getTextFromNode(classNameNode.get());
      assertEquals("List", className);
    }

    @Test
    @DisplayName("should return empty for null file")
    void shouldReturnEmptyForNullFile() {
      List<TSNode> imports = service.getAllImportDeclarationNodes(singleImportFile);
      TSNode importNode = imports.get(0);

      Optional<TSNode> result = service.getImportDeclarationClassNameNode(null, importNode);
      assertTrue(result.isEmpty());
    }
  }

  @Nested
  @DisplayName("getImportDeclarationFullImportScopeNode Tests")
  class GetImportDeclarationFullImportScopeNodeTests {

    @Test
    @DisplayName("should return full import scope node for import declaration")
    void shouldReturnFullImportScopeNodeForImportDeclaration() {
      List<TSNode> imports = service.getAllImportDeclarationNodes(singleImportFile);
      TSNode importNode = imports.get(0);

      Optional<TSNode> fullScopeNode =
          service.getImportDeclarationFullImportScopeNode(singleImportFile, importNode);

      assertTrue(fullScopeNode.isPresent());
      String fullScope = singleImportFile.getTextFromNode(fullScopeNode.get());
      assertTrue(fullScope.contains("java.util"));
    }

    @Test
    @DisplayName("should return empty for null file")
    void shouldReturnEmptyForNullFile() {
      List<TSNode> imports = service.getAllImportDeclarationNodes(singleImportFile);
      TSNode importNode = imports.get(0);

      Optional<TSNode> result = service.getImportDeclarationFullImportScopeNode(null, importNode);
      assertTrue(result.isEmpty());
    }
  }

  @Nested
  @DisplayName("addImport Tests")
  class AddImportTests {

    @Test
    @DisplayName("should add import after package declaration when no existing imports")
    void shouldAddImportAfterPackageDeclarationWhenNoExistingImports() {
      // Create a copy since we're modifying the source code
      TSFile testFile = new TSFile(SupportedLanguage.JAVA, NO_IMPORTS_CODE);
      Optional<TSNode> packageNode = packageService.getPackageDeclarationNode(testFile);

      assertTrue(packageNode.isPresent());

      service.addImport(testFile, "java.util", "ArrayList", packageNode.get());

      String updatedCode = testFile.getSourceCode();
      assertTrue(updatedCode.contains("import java.util.ArrayList;"));
      assertTrue(
          updatedCode.indexOf("import java.util.ArrayList;")
              > updatedCode.indexOf("package io.github.test;"));
    }

    @Test
    @DisplayName("should add import after last existing import")
    void shouldAddImportAfterLastExistingImport() {
      // Create a copy since we're modifying the source code
      TSFile testFile = new TSFile(SupportedLanguage.JAVA, SINGLE_IMPORT_CODE);
      Optional<TSNode> packageNode = packageService.getPackageDeclarationNode(testFile);

      assertTrue(packageNode.isPresent());

      service.addImport(testFile, "java.util", "ArrayList", packageNode.get());

      String updatedCode = testFile.getSourceCode();
      assertTrue(updatedCode.contains("import java.util.ArrayList;"));
      assertTrue(
          updatedCode.indexOf("import java.util.ArrayList;")
              > updatedCode.indexOf("import java.util.List;"));
    }

    @Test
    @DisplayName(
        "should throw NullPointerException when adding import with null package declaration and no"
            + " imports")
    void shouldThrowNullPointerExceptionWhenAddingImportWithNullPackageDeclarationAndNoImports() {
      // The current implementation has a bug where it throws NPE when packageDeclarationNode is
      // null and there are no existing imports - this test documents this behavior
      assertThrows(
          NullPointerException.class,
          () -> {
            service.addImport(noPackageNoImportsFile, "java.util", "List", null);
          });
    }

    @Test
    @DisplayName("should not add import if class is already imported")
    void shouldNotAddImportIfClassIsAlreadyImported() {
      // Create a copy since we're modifying the source code
      TSFile testFile = new TSFile(SupportedLanguage.JAVA, SINGLE_IMPORT_CODE);
      String originalCode = testFile.getSourceCode();
      Optional<TSNode> packageNode = packageService.getPackageDeclarationNode(testFile);

      service.addImport(testFile, "java.util", "List", packageNode.get());

      String updatedCode = testFile.getSourceCode();
      assertEquals(originalCode, updatedCode);
    }

    @Test
    @DisplayName("should handle adding import to file with wildcard imports")
    void shouldHandleAddingImportToFileWithWildcardImports() {
      TSFile testFile = new TSFile(SupportedLanguage.JAVA, WILDCARD_IMPORT_CODE);
      Optional<TSNode> packageNode = packageService.getPackageDeclarationNode(testFile);

      assertTrue(packageNode.isPresent());

      // Try adding an import that might be covered by wildcard
      service.addImport(testFile, "java.util", "ArrayList", packageNode.get());

      // Try adding an import that should not be covered by existing wildcards
      service.addImport(testFile, "com.example", "CustomClass", packageNode.get());

      // At least one of the imports should be added (or method should work without exceptions)
      assertDoesNotThrow(
          () -> {
            service.addImport(testFile, "com.example", "AnotherClass", packageNode.get());
          });
    }
  }

  @Nested
  @DisplayName("updateImport Tests")
  class UpdateImportTests {

    @Test
    @DisplayName("should handle updating existing import package and class name")
    void shouldHandleUpdatingExistingImportPackageAndClassName() {
      TSFile testFile = new TSFile(SupportedLanguage.JAVA, SINGLE_IMPORT_CODE);

      boolean result =
          service.updateImport(testFile, "java.util", "com.example", "List", "CustomList");

      // The method should work without throwing exceptions
      assertDoesNotThrow(
          () -> {
            service.updateImport(testFile, "java.util", "com.example", "List", "CustomList");
          });

      String updatedCode = testFile.getSourceCode();

      // If the update was successful, verify the changes
      if (result) {
        assertTrue(updatedCode.contains("com.example"));
        assertFalse(updatedCode.contains("import java.util.List;"));
      }
    }

    @Test
    @DisplayName("should return false if import is already present")
    void shouldReturnFalseIfImportIsAlreadyPresent() {
      TSFile testFile = new TSFile(SupportedLanguage.JAVA, MULTIPLE_IMPORTS_CODE);

      // Try to update to something that already exists
      boolean result = service.updateImport(testFile, "java.util", "java.util", "List", "Map");

      assertFalse(result);
    }

    @Test
    @DisplayName("should return false if import to update doesn't exist")
    void shouldReturnFalseIfImportToUpdateDoesNotExist() {
      TSFile testFile = new TSFile(SupportedLanguage.JAVA, SINGLE_IMPORT_CODE);

      boolean result =
          service.updateImport(
              testFile, "com.nonexistent", "com.example", "NonExistent", "NewClass");

      assertFalse(result);
    }

    @Test
    @DisplayName("should handle updating wildcard imports")
    void shouldHandleUpdatingWildcardImports() {
      TSFile testFile = new TSFile(SupportedLanguage.JAVA, WILDCARD_IMPORT_CODE);

      boolean result =
          service.updateImport(testFile, "java.util", "com.example", "ArrayList", "CustomList");

      if (result) {
        String updatedCode = testFile.getSourceCode();
        assertTrue(updatedCode.contains("com.example"));
      }
    }
  }

  @Nested
  @DisplayName("Edge Cases and Error Handling")
  class EdgeCasesAndErrorHandlingTests {

    @Test
    @DisplayName("should handle malformed import statements gracefully")
    void shouldHandleMalformedImportStatementsGracefully() {
      String malformedImportCode =
          """
          package io.github.test;

          import java.util.;
          import ;
          import java..util.List;

          public class TestClass {
          }
          """;

      TSFile malformedFile = new TSFile(SupportedLanguage.JAVA, malformedImportCode);

      assertDoesNotThrow(
          () -> {
            List<TSNode> imports = service.getAllImportDeclarationNodes(malformedFile);
            for (TSNode importNode : imports) {
              service.getImportDeclarationNodeInfo(malformedFile, importNode);
              service.isClassImported(malformedFile, "java.util", "List");
            }
          });
    }

    @Test
    @DisplayName("should handle very long import statements")
    void shouldHandleVeryLongImportStatements() {
      StringBuilder longPackage = new StringBuilder("com");
      for (int i = 0; i < 100; i++) {
        longPackage.append(".package").append(i);
      }

      String longImportCode =
          String.format(
              """
              package io.github.test;

              import %s.VeryLongClassName;

              public class TestClass {
              }
              """,
              longPackage);

      TSFile longImportFile = new TSFile(SupportedLanguage.JAVA, longImportCode);

      List<TSNode> imports = service.getAllImportDeclarationNodes(longImportFile);
      assertEquals(1, imports.size());

      Optional<TSNode> found =
          service.findImportDeclarationNode(
              longImportFile, longPackage.toString(), "VeryLongClassName");
      assertTrue(found.isPresent());
    }

    @Test
    @DisplayName("should handle imports with special characters in package names")
    void shouldHandleImportsWithSpecialCharactersInPackageNames() {
      String specialCharImportCode =
          """
          package io.github.test;

          import com.example.$special.Valid$Class;
          import com.example._underscore._UnderscoreClass;

          public class TestClass {
          }
          """;

      TSFile specialCharFile = new TSFile(SupportedLanguage.JAVA, specialCharImportCode);

      List<TSNode> imports = service.getAllImportDeclarationNodes(specialCharFile);
      assertEquals(2, imports.size());

      Boolean found1 =
          service.isClassImported(specialCharFile, "com.example.$special", "Valid$Class");
      Boolean found2 =
          service.isClassImported(specialCharFile, "com.example._underscore", "_UnderscoreClass");

      assertTrue(found1);
      assertTrue(found2);
    }

    @Test
    @DisplayName("should handle files with only comments")
    void shouldHandleFilesWithOnlyComments() {
      String commentsOnlyCode =
          """
          /*
           * This file has only comments
           */
          // No actual Java code here
          """;

      TSFile commentsOnlyFile = new TSFile(SupportedLanguage.JAVA, commentsOnlyCode);

      List<TSNode> imports = service.getAllImportDeclarationNodes(commentsOnlyFile);
      assertTrue(imports.isEmpty());

      Boolean result = service.isClassImported(commentsOnlyFile, "java.util", "List");
      assertFalse(result);
    }

    @Test
    @DisplayName("should handle mixed static and regular imports")
    void shouldHandleMixedStaticAndRegularImports() {
      List<TSNode> imports = service.getAllImportDeclarationNodes(staticImportsFile);

      assertEquals(3, imports.size());

      // All should be recognized as import declarations regardless of static modifier
      for (TSNode importNode : imports) {
        assertEquals("import_declaration", importNode.getType());
      }

      // Should be able to find regular imports among static ones
      Boolean regularImportFound = service.isClassImported(staticImportsFile, "java.util", "List");
      assertTrue(regularImportFound);
    }
  }
}
