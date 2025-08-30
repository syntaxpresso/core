package io.github.syntaxpresso.core.service.java.language;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
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

  private static final String BASIC_CLASS =
      """
      public class TestClass {
      }
      """;

  private static final String CLASS_WITH_PACKAGE =
      """
      package com.example;

      public class TestClass {
      }
      """;

  private static final String CLASS_WITH_SINGLE_IMPORT_NO_PACKAGE =
      """
      import java.util.List;

      public class TestClass {
      }
      """;
  private static final String CLASS_WITH_SINGLE_WILDCARD_IMPORT_NO_PACKAGE =
      """
      import java.util.*;

      public class TestClass {
      }
      """;

  private static final String CLASS_WITH_MULTIPLE_IMPORTS_NO_PACKAGE =
      """
      import java.util.List;
      import java.util.Map;
      import java.io.IOException;

      public class TestClass {
      }
      """;

  private static final String CLASS_WITH_MULTIPLE_IMPORTS_NO_PACKAGE_MIXED =
      """
      import java.util.List;
      import java.io.*;
      import java.util.Map;

      public class TestClass {
      }
      """;

  private static final String CLASS_WITH_SINGLE_IMPORT =
      """
      package com.example;

      import java.util.List;

      public class TestClass {
      }
      """;

  private static final String CLASS_WITH_MULTIPLE_IMPORTS =
      """
      package com.example;

      import java.util.List;
      import java.util.Map;
      import java.io.IOException;

      public class TestClass {
      }
      """;

  private static final String CLASS_WITH_WILDCARD_IMPORT =
      """
      package com.example;

      import java.util.*;

      public class TestClass {
      }
      """;

  private static final String CLASS_WITH_MIXED_IMPORTS =
      """
      package com.example;

      import java.util.List;
      import java.io.*;
      import java.util.Map;
      import org.junit.jupiter.api.*;

      public class TestClass {
      }
      """;

  private static final String CLASS_WITH_NESTED_PACKAGE_IMPORTS =
      """
      package com.example.project.service;

      import org.springframework.boot.SpringApplication;
      import org.springframework.boot.autoconfigure.SpringBootApplication;
      import com.example.project.model.User;

      public class TestClass {
      }
      """;

  private ImportDeclarationService importService;

  @BeforeEach
  void setUp() {
    importService = new ImportDeclarationService();
  }

  @Nested
  @DisplayName("getAllImportDeclarations(TSFile)")
  class GetAllImportDeclarationsTests {

    @Test
    @DisplayName("should return empty list when no imports exist")
    void getAllImportDeclarations_withNoImports_shouldReturnEmptyList() {
      TSFile file = new TSFile(SupportedLanguage.JAVA, BASIC_CLASS);
      List<Map<String, TSNode>> imports = importService.getAllImportDeclarations(file);
      assertTrue(imports.isEmpty());
    }

    @Test
    @DisplayName("should return empty list when only package declaration exists")
    void getAllImportDeclarations_withOnlyPackage_shouldReturnEmptyList() {
      TSFile file = new TSFile(SupportedLanguage.JAVA, CLASS_WITH_PACKAGE);
      List<Map<String, TSNode>> imports = importService.getAllImportDeclarations(file);
      assertTrue(imports.isEmpty());
    }

    @Test
    @DisplayName("should return single import declaration")
    void getAllImportDeclarations_withSingleImport_shouldReturnSingleImport() {
      TSFile file = new TSFile(SupportedLanguage.JAVA, CLASS_WITH_SINGLE_IMPORT);
      List<Map<String, TSNode>> imports = importService.getAllImportDeclarations(file);
      assertEquals(1, imports.size());
      Map<String, TSNode> importMap = imports.get(0);
      assertTrue(importMap.containsKey("importDeclaration"));
      assertTrue(importMap.containsKey("package"));
      assertTrue(importMap.containsKey("class"));
      assertEquals("java.util", file.getTextFromNode(importMap.get("package")));
      assertEquals("List", file.getTextFromNode(importMap.get("class")));
    }

    @Test
    @DisplayName("should return multiple import declarations")
    void getAllImportDeclarations_withMultipleImports_shouldReturnAllImports() {
      TSFile file = new TSFile(SupportedLanguage.JAVA, CLASS_WITH_MULTIPLE_IMPORTS);
      List<Map<String, TSNode>> imports = importService.getAllImportDeclarations(file);
      assertEquals(3, imports.size());
      Map<String, TSNode> firstImport = imports.get(0);
      assertEquals("java.util", file.getTextFromNode(firstImport.get("package")));
      assertEquals("List", file.getTextFromNode(firstImport.get("class")));
      Map<String, TSNode> secondImport = imports.get(1);
      assertEquals("java.util", file.getTextFromNode(secondImport.get("package")));
      assertEquals("Map", file.getTextFromNode(secondImport.get("class")));
      Map<String, TSNode> thirdImport = imports.get(2);
      assertEquals("java.io", file.getTextFromNode(thirdImport.get("package")));
      assertEquals("IOException", file.getTextFromNode(thirdImport.get("class")));
    }

    @Test
    @DisplayName("should identify wildcard import")
    void getAllImportDeclarations_withWildcardImport_shouldIdentifyWildcard() {
      TSFile file = new TSFile(SupportedLanguage.JAVA, CLASS_WITH_WILDCARD_IMPORT);
      List<Map<String, TSNode>> imports = importService.getAllImportDeclarations(file);
      assertEquals(1, imports.size());
      Map<String, TSNode> wildcardImport = imports.get(0);
      assertTrue(wildcardImport.containsKey("isWildCard"));
      assertTrue(wildcardImport.containsKey("package"));
      assertTrue(wildcardImport.containsKey("importDeclaration"));
      assertEquals("java.util", file.getTextFromNode(wildcardImport.get("package")));
    }

    @Test
    @DisplayName("should handle mixed regular and wildcard imports")
    void getAllImportDeclarations_withMixedImports_shouldHandleBoth() {
      TSFile file = new TSFile(SupportedLanguage.JAVA, CLASS_WITH_MIXED_IMPORTS);
      List<Map<String, TSNode>> imports = importService.getAllImportDeclarations(file);
      assertEquals(4, imports.size());
      // First import: java.util.List (regular)
      Map<String, TSNode> firstImport = imports.get(0);
      assertTrue(firstImport.containsKey("class"));
      assertEquals("java.util", file.getTextFromNode(firstImport.get("package")));
      assertEquals("List", file.getTextFromNode(firstImport.get("class")));
      // Second import: java.io.* (wildcard)
      Map<String, TSNode> secondImport = imports.get(1);
      assertTrue(secondImport.containsKey("isWildCard"));
      assertEquals("java.io", file.getTextFromNode(secondImport.get("package")));
      // Third import: java.util.Map (regular)
      Map<String, TSNode> thirdImport = imports.get(2);
      assertTrue(thirdImport.containsKey("class"));
      assertEquals("java.util", file.getTextFromNode(thirdImport.get("package")));
      assertEquals("Map", file.getTextFromNode(thirdImport.get("class")));
      // Fourth import: org.junit.jupiter.api.* (wildcard)
      Map<String, TSNode> fourthImport = imports.get(3);
      assertTrue(fourthImport.containsKey("isWildCard"));
      assertEquals("org.junit.jupiter.api", file.getTextFromNode(fourthImport.get("package")));
    }

    @Test
    @DisplayName("should handle complex nested package imports")
    void getAllImportDeclarations_withNestedPackageImports_shouldParseCorrectly() {
      TSFile file = new TSFile(SupportedLanguage.JAVA, CLASS_WITH_NESTED_PACKAGE_IMPORTS);
      List<Map<String, TSNode>> imports = importService.getAllImportDeclarations(file);
      assertEquals(3, imports.size());
      Map<String, TSNode> firstImport = imports.get(0);
      assertEquals("org.springframework.boot", file.getTextFromNode(firstImport.get("package")));
      assertEquals("SpringApplication", file.getTextFromNode(firstImport.get("class")));
      Map<String, TSNode> secondImport = imports.get(1);
      assertEquals(
          "org.springframework.boot.autoconfigure",
          file.getTextFromNode(secondImport.get("package")));
      assertEquals("SpringBootApplication", file.getTextFromNode(secondImport.get("class")));
      Map<String, TSNode> thirdImport = imports.get(2);
      assertEquals("com.example.project.model", file.getTextFromNode(thirdImport.get("package")));
      assertEquals("User", file.getTextFromNode(thirdImport.get("class")));
    }

    @Test
    @DisplayName("should return single import declaration without package")
    void getAllImportDeclarations_withSingleImportNoPackage_shouldReturnSingleImport() {
      TSFile file = new TSFile(SupportedLanguage.JAVA, CLASS_WITH_SINGLE_IMPORT_NO_PACKAGE);
      List<Map<String, TSNode>> imports = importService.getAllImportDeclarations(file);
      assertEquals(1, imports.size());
      Map<String, TSNode> importMap = imports.get(0);
      assertTrue(importMap.containsKey("importDeclaration"));
      assertTrue(importMap.containsKey("package"));
      assertTrue(importMap.containsKey("class"));
      assertEquals("java.util", file.getTextFromNode(importMap.get("package")));
      assertEquals("List", file.getTextFromNode(importMap.get("class")));
    }

    @Test
    @DisplayName("should identify single wildcard import without package")
    void getAllImportDeclarations_withSingleWildcardImportNoPackage_shouldIdentifyWildcard() {
      TSFile file =
          new TSFile(SupportedLanguage.JAVA, CLASS_WITH_SINGLE_WILDCARD_IMPORT_NO_PACKAGE);
      List<Map<String, TSNode>> imports = importService.getAllImportDeclarations(file);
      assertEquals(1, imports.size());
      Map<String, TSNode> wildcardImport = imports.get(0);
      assertTrue(wildcardImport.containsKey("isWildCard"));
      assertTrue(wildcardImport.containsKey("package"));
      assertTrue(wildcardImport.containsKey("importDeclaration"));
      assertEquals("java.util", file.getTextFromNode(wildcardImport.get("package")));
    }

    @Test
    @DisplayName("should return multiple import declarations without package")
    void getAllImportDeclarations_withMultipleImportsNoPackage_shouldReturnAllImports() {
      TSFile file = new TSFile(SupportedLanguage.JAVA, CLASS_WITH_MULTIPLE_IMPORTS_NO_PACKAGE);
      List<Map<String, TSNode>> imports = importService.getAllImportDeclarations(file);
      assertEquals(3, imports.size());
      Map<String, TSNode> firstImport = imports.get(0);
      assertEquals("java.util", file.getTextFromNode(firstImport.get("package")));
      assertEquals("List", file.getTextFromNode(firstImport.get("class")));
      Map<String, TSNode> secondImport = imports.get(1);
      assertEquals("java.util", file.getTextFromNode(secondImport.get("package")));
      assertEquals("Map", file.getTextFromNode(secondImport.get("class")));
      Map<String, TSNode> thirdImport = imports.get(2);
      assertEquals("java.io", file.getTextFromNode(thirdImport.get("package")));
      assertEquals("IOException", file.getTextFromNode(thirdImport.get("class")));
    }

    @Test
    @DisplayName("should handle mixed regular and wildcard imports without package")
    void getAllImportDeclarations_withMixedImportsNoPackage_shouldHandleBoth() {
      TSFile file =
          new TSFile(SupportedLanguage.JAVA, CLASS_WITH_MULTIPLE_IMPORTS_NO_PACKAGE_MIXED);
      List<Map<String, TSNode>> imports = importService.getAllImportDeclarations(file);
      assertEquals(3, imports.size());
      // First import: java.util.List (regular)
      Map<String, TSNode> firstImport = imports.get(0);
      assertTrue(firstImport.containsKey("class"));
      assertEquals("java.util", file.getTextFromNode(firstImport.get("package")));
      assertEquals("List", file.getTextFromNode(firstImport.get("class")));
      // Second import: java.io.* (wildcard)
      Map<String, TSNode> secondImport = imports.get(1);
      assertTrue(secondImport.containsKey("isWildCard"));
      assertEquals("java.io", file.getTextFromNode(secondImport.get("package")));
      // Third import: java.util.Map (regular)
      Map<String, TSNode> thirdImport = imports.get(2);
      assertTrue(thirdImport.containsKey("class"));
      assertEquals("java.util", file.getTextFromNode(thirdImport.get("package")));
      assertEquals("Map", file.getTextFromNode(thirdImport.get("class")));
    }
  }

  @Nested
  @DisplayName("getImportDeclarationMap(TSFile, String, String)")
  class GetImportDeclarationMapTests {

    @Test
    @DisplayName("should return empty when no imports exist")
    void getImportDeclarationMap_withNoImports_shouldReturnEmpty() {
      TSFile file = new TSFile(SupportedLanguage.JAVA, BASIC_CLASS);
      Optional<Map<String, TSNode>> result =
          importService.getImportDeclarationMap(file, "List", "java.util");
      assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("should return empty when only package declaration exists")
    void getImportDeclarationMap_withOnlyPackage_shouldReturnEmpty() {
      TSFile file = new TSFile(SupportedLanguage.JAVA, CLASS_WITH_PACKAGE);
      Optional<Map<String, TSNode>> result =
          importService.getImportDeclarationMap(file, "List", "java.util");
      assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("should find exact class import match")
    void getImportDeclarationMap_withExactMatch_shouldReturnImport() {
      TSFile file = new TSFile(SupportedLanguage.JAVA, CLASS_WITH_SINGLE_IMPORT);
      Optional<Map<String, TSNode>> result =
          importService.getImportDeclarationMap(file, "List", "java.util");
      assertTrue(result.isPresent());
      Map<String, TSNode> importMap = result.get();
      assertTrue(importMap.containsKey("class"));
      assertTrue(importMap.containsKey("package"));
      assertTrue(importMap.containsKey("importDeclaration"));
      assertEquals("java.util", file.getTextFromNode(importMap.get("package")));
      assertEquals("List", file.getTextFromNode(importMap.get("class")));
    }

    @Test
    @DisplayName("should find wildcard import match")
    void getImportDeclarationMap_withWildcardMatch_shouldReturnWildcardImport() {
      TSFile file = new TSFile(SupportedLanguage.JAVA, CLASS_WITH_WILDCARD_IMPORT);
      Optional<Map<String, TSNode>> result =
          importService.getImportDeclarationMap(file, "List", "java.util");
      assertTrue(result.isPresent());
      Map<String, TSNode> importMap = result.get();
      assertTrue(importMap.containsKey("isWildCard"));
      assertTrue(importMap.containsKey("package"));
      assertTrue(importMap.containsKey("importDeclaration"));
      assertEquals("java.util", file.getTextFromNode(importMap.get("package")));
    }

    @Test
    @DisplayName("should find correct import among multiple imports")
    void getImportDeclarationMap_withMultipleImports_shouldFindCorrectOne() {
      TSFile file = new TSFile(SupportedLanguage.JAVA, CLASS_WITH_MULTIPLE_IMPORTS);
      // Test finding List
      Optional<Map<String, TSNode>> listResult =
          importService.getImportDeclarationMap(file, "List", "java.util");
      assertTrue(listResult.isPresent());
      assertEquals("List", file.getTextFromNode(listResult.get().get("class")));
      // Test finding Map
      Optional<Map<String, TSNode>> mapResult =
          importService.getImportDeclarationMap(file, "Map", "java.util");
      assertTrue(mapResult.isPresent());
      assertEquals("Map", file.getTextFromNode(mapResult.get().get("class")));
      // Test finding IOException
      Optional<Map<String, TSNode>> ioResult =
          importService.getImportDeclarationMap(file, "IOException", "java.io");
      assertTrue(ioResult.isPresent());
      assertEquals("IOException", file.getTextFromNode(ioResult.get().get("class")));
    }

    @Test
    @DisplayName("should prioritize wildcard over missing specific import")
    void getImportDeclarationMap_withWildcardAndMissingSpecific_shouldReturnWildcard() {
      TSFile file = new TSFile(SupportedLanguage.JAVA, CLASS_WITH_MIXED_IMPORTS);
      // Ask for ArrayList (not specifically imported but covered by java.util.*)
      // Note: java.util.* is not in our current mixed imports, let's use java.io.*
      Optional<Map<String, TSNode>> result =
          importService.getImportDeclarationMap(file, "File", "java.io");
      assertTrue(result.isPresent());
      Map<String, TSNode> importMap = result.get();
      assertTrue(importMap.containsKey("isWildCard"));
      assertEquals("java.io", file.getTextFromNode(importMap.get("package")));
    }

    @Test
    @DisplayName("should return empty when class not found")
    void getImportDeclarationMap_withNonExistentClass_shouldReturnEmpty() {
      TSFile file = new TSFile(SupportedLanguage.JAVA, CLASS_WITH_SINGLE_IMPORT);
      // Wrong class name
      Optional<Map<String, TSNode>> wrongClass =
          importService.getImportDeclarationMap(file, "ArrayList", "java.util");
      assertFalse(wrongClass.isPresent());
      // Wrong package
      Optional<Map<String, TSNode>> wrongPackage =
          importService.getImportDeclarationMap(file, "List", "java.io");
      assertFalse(wrongPackage.isPresent());
      // Both wrong
      Optional<Map<String, TSNode>> bothWrong =
          importService.getImportDeclarationMap(file, "File", "java.io");
      assertFalse(bothWrong.isPresent());
    }

    @Test
    @DisplayName("should handle case sensitivity correctly")
    void getImportDeclarationMap_withCaseSensitivity_shouldMatchExactly() {
      TSFile file = new TSFile(SupportedLanguage.JAVA, CLASS_WITH_SINGLE_IMPORT);
      // Correct case
      Optional<Map<String, TSNode>> correctCase =
          importService.getImportDeclarationMap(file, "List", "java.util");
      assertTrue(correctCase.isPresent());
      // Wrong case for class name
      Optional<Map<String, TSNode>> wrongClassCase =
          importService.getImportDeclarationMap(file, "list", "java.util");
      assertFalse(wrongClassCase.isPresent());
      // Wrong case for package name
      Optional<Map<String, TSNode>> wrongPackageCase =
          importService.getImportDeclarationMap(file, "List", "java.Util");
      assertFalse(wrongPackageCase.isPresent());
    }

    @Test
    @DisplayName("should work with files without package declaration")
    void getImportDeclarationMap_withNoPackageDeclaration_shouldFindImports() {
      TSFile file = new TSFile(SupportedLanguage.JAVA, CLASS_WITH_SINGLE_IMPORT_NO_PACKAGE);
      // Should find the import
      Optional<Map<String, TSNode>> result =
          importService.getImportDeclarationMap(file, "List", "java.util");
      assertTrue(result.isPresent());
      Map<String, TSNode> importMap = result.get();
      assertTrue(importMap.containsKey("class"));
      assertEquals("List", file.getTextFromNode(importMap.get("class")));
      assertEquals("java.util", file.getTextFromNode(importMap.get("package")));
    }

    @Test
    @DisplayName("should work with wildcard imports without package declaration")
    void getImportDeclarationMap_withWildcardNoPackage_shouldFindWildcard() {
      TSFile file =
          new TSFile(SupportedLanguage.JAVA, CLASS_WITH_SINGLE_WILDCARD_IMPORT_NO_PACKAGE);
      // Should find via wildcard
      Optional<Map<String, TSNode>> result =
          importService.getImportDeclarationMap(file, "ArrayList", "java.util");
      assertTrue(result.isPresent());
      Map<String, TSNode> importMap = result.get();
      assertTrue(importMap.containsKey("isWildCard"));
      assertEquals("java.util", file.getTextFromNode(importMap.get("package")));
    }

    @Test
    @DisplayName("should handle mixed imports correctly")
    void getImportDeclarationMap_withMixedImports_shouldFindBothTypes() {
      TSFile file = new TSFile(SupportedLanguage.JAVA, CLASS_WITH_MIXED_IMPORTS);
      // Find specific import
      Optional<Map<String, TSNode>> specificResult =
          importService.getImportDeclarationMap(file, "List", "java.util");
      assertTrue(specificResult.isPresent());
      assertTrue(specificResult.get().containsKey("class"));
      // Find via wildcard import
      Optional<Map<String, TSNode>> wildcardResult =
          importService.getImportDeclarationMap(file, "File", "java.io");
      assertTrue(wildcardResult.isPresent());
      assertTrue(wildcardResult.get().containsKey("isWildCard"));
    }

    @Test
    @DisplayName("should handle complex nested packages")
    void getImportDeclarationMap_withNestedPackages_shouldFindCorrectImports() {
      TSFile file = new TSFile(SupportedLanguage.JAVA, CLASS_WITH_NESTED_PACKAGE_IMPORTS);
      // Test finding SpringApplication
      Optional<Map<String, TSNode>> springApp =
          importService.getImportDeclarationMap(
              file, "SpringApplication", "org.springframework.boot");
      assertTrue(springApp.isPresent());
      assertEquals("SpringApplication", file.getTextFromNode(springApp.get().get("class")));
      // Test finding SpringBootApplication
      Optional<Map<String, TSNode>> springBootApp =
          importService.getImportDeclarationMap(
              file, "SpringBootApplication", "org.springframework.boot.autoconfigure");
      assertTrue(springBootApp.isPresent());
      assertEquals("SpringBootApplication", file.getTextFromNode(springBootApp.get().get("class")));
      // Test finding User
      Optional<Map<String, TSNode>> user =
          importService.getImportDeclarationMap(file, "User", "com.example.project.model");
      assertTrue(user.isPresent());
      assertEquals("User", file.getTextFromNode(user.get().get("class")));
    }

    @Test
    @DisplayName("should return empty for partial package matches")
    void getImportDeclarationMap_withPartialPackageMatch_shouldReturnEmpty() {
      TSFile file = new TSFile(SupportedLanguage.JAVA, CLASS_WITH_NESTED_PACKAGE_IMPORTS);
      // Partial package match should not work
      Optional<Map<String, TSNode>> partialMatch =
          importService.getImportDeclarationMap(file, "SpringApplication", "org.springframework");
      assertFalse(partialMatch.isPresent());
      // Too specific package should not work
      Optional<Map<String, TSNode>> tooSpecific =
          importService.getImportDeclarationMap(
              file, "SpringApplication", "org.springframework.boot.extra");
      assertFalse(tooSpecific.isPresent());
    }
  }

  @Nested
  @DisplayName("addImport(TSFile, String, String)")
  class AddImportWithPackageAndClassTests {

    @Test
    @DisplayName("should add import after package declaration")
    void addImport_withPackageDeclaration_shouldAddImportAfterPackage() {
      TSFile file = new TSFile(SupportedLanguage.JAVA, CLASS_WITH_PACKAGE);
      importService.addImport(file, "java.util", "List");
      String expectedCode =
          """
          package com.example;

          import java.util.List;

          public class TestClass {
          }
          """;
      assertEquals(expectedCode, file.getSourceCode());
    }

    @Test
    @DisplayName("should add import at beginning when no package declaration")
    void addImport_withNoPackage_shouldAddImportAtBeginning() {
      TSFile file = new TSFile(SupportedLanguage.JAVA, BASIC_CLASS);
      importService.addImport(file, "java.util", "List");
      String expectedCode =
          """
          import java.util.List;
          public class TestClass {
          }
          """;
      assertEquals(expectedCode, file.getSourceCode());
    }

    @Test
    @DisplayName("should add import after existing imports")
    void addImport_withExistingImports_shouldAddAfterLastImport() {
      TSFile file = new TSFile(SupportedLanguage.JAVA, CLASS_WITH_MULTIPLE_IMPORTS);
      importService.addImport(file, "java.util", "ArrayList");
      String result = file.getSourceCode();
      assertTrue(result.contains("import java.util.List;"));
      assertTrue(result.contains("import java.util.Map;"));
      assertTrue(result.contains("import java.io.IOException;"));
      assertTrue(result.contains("import java.util.ArrayList;"));
      // Verify ArrayList comes after IOException (last existing import)
      int ioIndex = result.indexOf("import java.io.IOException;");
      int arrayListIndex = result.indexOf("import java.util.ArrayList;");
      assertTrue(ioIndex < arrayListIndex);
    }

    @Test
    @DisplayName("should not add duplicate import")
    void addImport_withDuplicateImport_shouldNotAddDuplicate() {
      TSFile file = new TSFile(SupportedLanguage.JAVA, CLASS_WITH_SINGLE_IMPORT);
      String originalCode = file.getSourceCode();
      importService.addImport(file, "java.util", "List");
      assertEquals(originalCode, file.getSourceCode());
    }

    @Test
    @DisplayName("should not add import when wildcard covers it")
    void addImport_withWildcardCoverage_shouldNotAddImport() {
      TSFile file = new TSFile(SupportedLanguage.JAVA, CLASS_WITH_WILDCARD_IMPORT);
      String originalCode = file.getSourceCode();
      importService.addImport(file, "java.util", "List");
      importService.addImport(file, "java.util", "ArrayList");
      importService.addImport(file, "java.util", "HashMap");
      assertEquals(originalCode, file.getSourceCode());
    }

    @Test
    @DisplayName("should add import to file without package but with existing imports")
    void addImport_withNoPackageButExistingImports_shouldAddAfterImports() {
      TSFile file = new TSFile(SupportedLanguage.JAVA, CLASS_WITH_MULTIPLE_IMPORTS_NO_PACKAGE);
      importService.addImport(file, "java.util", "ArrayList");
      String result = file.getSourceCode();
      assertTrue(result.contains("import java.util.List;"));
      assertTrue(result.contains("import java.util.Map;"));
      assertTrue(result.contains("import java.io.IOException;"));
      assertTrue(result.contains("import java.util.ArrayList;"));
    }

    @Test
    @DisplayName("should handle mixed import scenarios correctly")
    void addImport_withMixedImports_shouldInsertCorrectly() {
      TSFile file = new TSFile(SupportedLanguage.JAVA, CLASS_WITH_MIXED_IMPORTS);
      importService.addImport(file, "java.io", "File");
      importService.addImport(file, "java.time", "LocalDate");
      String result = file.getSourceCode();
      assertFalse(result.contains("import java.io.File;"));
      assertTrue(result.contains("import java.time.LocalDate;"));
    }

    @Test
    @DisplayName("should preserve file structure and formatting")
    void addImport_shouldPreserveFileStructure() {
      String sourceCode =
          """
          package com.example;

          public class TestClass {
              public void method() {
                  System.out.println("test");
              }
          }
          """;
      TSFile file = new TSFile(SupportedLanguage.JAVA, sourceCode);
      importService.addImport(file, "java.util", "List");
      String result = file.getSourceCode();
      assertTrue(result.contains("package com.example;"));
      assertTrue(result.contains("import java.util.List;"));
      assertTrue(result.contains("public class TestClass {"));
      assertTrue(result.contains("public void method() {"));
      assertTrue(result.contains("System.out.println(\"test\");"));
    }
  }

  @Nested
  @DisplayName("addImport(TSFile, String)")
  class AddImportWithFullPackageNameTests {

    @Test
    @DisplayName("should add import using full package name")
    void addImport_withFullPackageName_shouldAddImport() {
      TSFile file = new TSFile(SupportedLanguage.JAVA, CLASS_WITH_PACKAGE);
      importService.addImport(file, "java.util.List");
      String expectedCode =
          """
          package com.example;

          import java.util.List;

          public class TestClass {
          }
          """;
      assertEquals(expectedCode, file.getSourceCode());
    }

    @Test
    @DisplayName("should correctly parse complex full package names")
    void addImport_withComplexFullPackageName_shouldParseCorrectly() {
      TSFile file = new TSFile(SupportedLanguage.JAVA, CLASS_WITH_PACKAGE);
      importService.addImport(file, "org.springframework.boot.autoconfigure.SpringBootApplication");
      String result = file.getSourceCode();
      assertTrue(
          result.contains("import org.springframework.boot.autoconfigure.SpringBootApplication;"));
    }

    @Test
    @DisplayName("should throw exception for invalid full package name")
    void addImport_withInvalidFullPackageName_shouldThrowException() {
      TSFile file = new TSFile(SupportedLanguage.JAVA, CLASS_WITH_PACKAGE);
      assertThrows(
          IllegalArgumentException.class,
          () -> {
            importService.addImport(file, "InvalidPackageName");
          });
      assertThrows(
          IllegalArgumentException.class,
          () -> {
            importService.addImport(file, "");
          });
    }

    @Test
    @DisplayName("should not add duplicate using full package name")
    void addImport_withDuplicateFullPackageName_shouldNotAddDuplicate() {
      TSFile file = new TSFile(SupportedLanguage.JAVA, CLASS_WITH_SINGLE_IMPORT);
      String originalCode = file.getSourceCode();
      importService.addImport(file, "java.util.List");
      assertEquals(originalCode, file.getSourceCode());
    }

    @Test
    @DisplayName("should handle edge case with single character class name")
    void addImport_withSingleCharacterClassName_shouldWork() {
      TSFile file = new TSFile(SupportedLanguage.JAVA, CLASS_WITH_PACKAGE);
      importService.addImport(file, "com.example.A");
      String result = file.getSourceCode();
      assertTrue(result.contains("import com.example.A;"));
    }

    @Test
    @DisplayName("should delegate to main addImport method correctly")
    void addImport_withFullPackageName_shouldDelegateCorrectly() {
      TSFile file1 = new TSFile(SupportedLanguage.JAVA, CLASS_WITH_PACKAGE);
      TSFile file2 = new TSFile(SupportedLanguage.JAVA, CLASS_WITH_PACKAGE);
      importService.addImport(file1, "java.util", "ArrayList");
      importService.addImport(file2, "java.util.ArrayList");
      assertEquals(file1.getSourceCode(), file2.getSourceCode());
    }

    @Test
    @DisplayName("should work with files without package declaration")
    void addImport_withFullPackageNameNoPackage_shouldWork() {
      TSFile file = new TSFile(SupportedLanguage.JAVA, BASIC_CLASS);
      importService.addImport(file, "java.util.List");
      String expectedCode =
          """
          import java.util.List;
          public class TestClass {
          }
          """;
      assertEquals(expectedCode, file.getSourceCode());
    }

    @Test
    @DisplayName("should respect wildcard import coverage")
    void addImport_withFullPackageNameAndWildcard_shouldRespectWildcard() {
      TSFile file = new TSFile(SupportedLanguage.JAVA, CLASS_WITH_WILDCARD_IMPORT);
      String originalCode = file.getSourceCode();
      importService.addImport(file, "java.util.ArrayList");
      assertEquals(originalCode, file.getSourceCode());
    }
  }

  @Nested
  @DisplayName("updateImportClassName(TSFile, String, String, String)")
  class UpdateImportClassNameTests {

    @Test
    @DisplayName("should update class name in existing import")
    void updateImportClassName_withExistingImport_shouldUpdateClassName() {
      TSFile file = new TSFile(SupportedLanguage.JAVA, CLASS_WITH_SINGLE_IMPORT);
      boolean result = importService.updateImportClassName(file, "java.util", "List", "ArrayList");
      assertTrue(result);
      String updatedCode = file.getSourceCode();
      assertTrue(updatedCode.contains("import java.util.ArrayList;"));
      assertFalse(updatedCode.contains("import java.util.List;"));
    }

    @Test
    @DisplayName("should return false when wildcard import exists")
    void updateImportClassName_withWildcardImport_shouldReturnFalse() {
      TSFile file = new TSFile(SupportedLanguage.JAVA, CLASS_WITH_WILDCARD_IMPORT);
      boolean result = importService.updateImportClassName(file, "java.util", "List", "ArrayList");
      assertFalse(result);
      String originalCode = file.getSourceCode();
      assertTrue(originalCode.contains("import java.util.*;"));
    }

    @Test
    @DisplayName("should return false when import not found")
    void updateImportClassName_withNonExistentImport_shouldReturnFalse() {
      TSFile file = new TSFile(SupportedLanguage.JAVA, CLASS_WITH_SINGLE_IMPORT);
      boolean result = importService.updateImportClassName(file, "java.io", "File", "BufferedReader");
      assertFalse(result);
      String originalCode = file.getSourceCode();
      assertTrue(originalCode.contains("import java.util.List;"));
    }

    @Test
    @DisplayName("should handle multiple imports correctly")
    void updateImportClassName_withMultipleImports_shouldUpdateOnlyTargetImport() {
      TSFile file = new TSFile(SupportedLanguage.JAVA, CLASS_WITH_MULTIPLE_IMPORTS);
      boolean result = importService.updateImportClassName(file, "java.util", "Map", "HashMap");
      assertTrue(result);
      String updatedCode = file.getSourceCode();
      assertTrue(updatedCode.contains("import java.util.List;"));
      assertTrue(updatedCode.contains("import java.util.HashMap;"));
      assertTrue(updatedCode.contains("import java.io.IOException;"));
      assertFalse(updatedCode.contains("import java.util.Map;"));
    }

    @Test
    @DisplayName("should throw exception for null package name")
    void updateImportClassName_withNullPackageName_shouldThrowException() {
      TSFile file = new TSFile(SupportedLanguage.JAVA, CLASS_WITH_SINGLE_IMPORT);
      assertThrows(IllegalArgumentException.class, () -> {
        importService.updateImportClassName(file, null, "List", "ArrayList");
      });
    }

    @Test
    @DisplayName("should throw exception for empty package name")
    void updateImportClassName_withEmptyPackageName_shouldThrowException() {
      TSFile file = new TSFile(SupportedLanguage.JAVA, CLASS_WITH_SINGLE_IMPORT);
      assertThrows(IllegalArgumentException.class, () -> {
        importService.updateImportClassName(file, "", "List", "ArrayList");
      });
    }

    @Test
    @DisplayName("should throw exception for null old class name")
    void updateImportClassName_withNullOldClassName_shouldThrowException() {
      TSFile file = new TSFile(SupportedLanguage.JAVA, CLASS_WITH_SINGLE_IMPORT);
      assertThrows(IllegalArgumentException.class, () -> {
        importService.updateImportClassName(file, "java.util", null, "ArrayList");
      });
    }

    @Test
    @DisplayName("should throw exception for null new class name")
    void updateImportClassName_withNullNewClassName_shouldThrowException() {
      TSFile file = new TSFile(SupportedLanguage.JAVA, CLASS_WITH_SINGLE_IMPORT);
      assertThrows(IllegalArgumentException.class, () -> {
        importService.updateImportClassName(file, "java.util", "List", null);
      });
    }

    @Test
    @DisplayName("should work with files without package declaration")
    void updateImportClassName_withNoPackageDeclaration_shouldWork() {
      TSFile file = new TSFile(SupportedLanguage.JAVA, CLASS_WITH_SINGLE_IMPORT_NO_PACKAGE);
      boolean result = importService.updateImportClassName(file, "java.util", "List", "ArrayList");
      assertTrue(result);
      String updatedCode = file.getSourceCode();
      assertTrue(updatedCode.contains("import java.util.ArrayList;"));
      assertFalse(updatedCode.contains("import java.util.List;"));
    }
  }

  @Nested
  @DisplayName("updateImportPackageName(TSFile, String, String, String)")
  class UpdateImportPackageNameTests {

    @Test
    @DisplayName("should update package name in existing import")
    void updateImportPackageName_withExistingImport_shouldUpdatePackageName() {
      TSFile file = new TSFile(SupportedLanguage.JAVA, CLASS_WITH_SINGLE_IMPORT);
      boolean result = importService.updateImportPackageName(file, "java.util", "java.util.concurrent", "List");
      assertTrue(result);
      String updatedCode = file.getSourceCode();
      assertTrue(updatedCode.contains("import java.util.concurrent.List;"));
      assertFalse(updatedCode.contains("import java.util.List;"));
    }

    @Test
    @DisplayName("should return false when wildcard import exists")
    void updateImportPackageName_withWildcardImport_shouldReturnFalse() {
      TSFile file = new TSFile(SupportedLanguage.JAVA, CLASS_WITH_WILDCARD_IMPORT);
      boolean result = importService.updateImportPackageName(file, "java.util", "java.util.concurrent", "List");
      assertFalse(result);
      String originalCode = file.getSourceCode();
      assertTrue(originalCode.contains("import java.util.*;"));
    }

    @Test
    @DisplayName("should return false when import not found")
    void updateImportPackageName_withNonExistentImport_shouldReturnFalse() {
      TSFile file = new TSFile(SupportedLanguage.JAVA, CLASS_WITH_SINGLE_IMPORT);
      boolean result = importService.updateImportPackageName(file, "java.io", "java.nio", "File");
      assertFalse(result);
      String originalCode = file.getSourceCode();
      assertTrue(originalCode.contains("import java.util.List;"));
    }

    @Test
    @DisplayName("should handle multiple imports correctly")
    void updateImportPackageName_withMultipleImports_shouldUpdateOnlyTargetImport() {
      TSFile file = new TSFile(SupportedLanguage.JAVA, CLASS_WITH_MULTIPLE_IMPORTS);
      boolean result = importService.updateImportPackageName(file, "java.io", "java.nio.file", "IOException");
      assertTrue(result);
      String updatedCode = file.getSourceCode();
      assertTrue(updatedCode.contains("import java.util.List;"));
      assertTrue(updatedCode.contains("import java.util.Map;"));
      assertTrue(updatedCode.contains("import java.nio.file.IOException;"));
      assertFalse(updatedCode.contains("import java.io.IOException;"));
    }

    @Test
    @DisplayName("should throw exception for null old package name")
    void updateImportPackageName_withNullOldPackageName_shouldThrowException() {
      TSFile file = new TSFile(SupportedLanguage.JAVA, CLASS_WITH_SINGLE_IMPORT);
      assertThrows(IllegalArgumentException.class, () -> {
        importService.updateImportPackageName(file, null, "java.util.concurrent", "List");
      });
    }

    @Test
    @DisplayName("should throw exception for empty new package name")
    void updateImportPackageName_withEmptyNewPackageName_shouldThrowException() {
      TSFile file = new TSFile(SupportedLanguage.JAVA, CLASS_WITH_SINGLE_IMPORT);
      assertThrows(IllegalArgumentException.class, () -> {
        importService.updateImportPackageName(file, "java.util", "", "List");
      });
    }

    @Test
    @DisplayName("should throw exception for null class name")
    void updateImportPackageName_withNullClassName_shouldThrowException() {
      TSFile file = new TSFile(SupportedLanguage.JAVA, CLASS_WITH_SINGLE_IMPORT);
      assertThrows(IllegalArgumentException.class, () -> {
        importService.updateImportPackageName(file, "java.util", "java.util.concurrent", null);
      });
    }

    @Test
    @DisplayName("should work with files without package declaration")
    void updateImportPackageName_withNoPackageDeclaration_shouldWork() {
      TSFile file = new TSFile(SupportedLanguage.JAVA, CLASS_WITH_SINGLE_IMPORT_NO_PACKAGE);
      boolean result = importService.updateImportPackageName(file, "java.util", "java.util.concurrent", "List");
      assertTrue(result);
      String updatedCode = file.getSourceCode();
      assertTrue(updatedCode.contains("import java.util.concurrent.List;"));
      assertFalse(updatedCode.contains("import java.util.List;"));
    }

    @Test
    @DisplayName("should handle complex package changes")
    void updateImportPackageName_withComplexPackageChange_shouldWork() {
      TSFile file = new TSFile(SupportedLanguage.JAVA, CLASS_WITH_NESTED_PACKAGE_IMPORTS);
      boolean result = importService.updateImportPackageName(file, "org.springframework.boot", "org.springframework.boot.web", "SpringApplication");
      assertTrue(result);
      String updatedCode = file.getSourceCode();
      assertTrue(updatedCode.contains("import org.springframework.boot.web.SpringApplication;"));
      assertTrue(updatedCode.contains("import org.springframework.boot.autoconfigure.SpringBootApplication;"));
      assertTrue(updatedCode.contains("import com.example.project.model.User;"));
      assertFalse(updatedCode.contains("import org.springframework.boot.SpringApplication;"));
    }
  }

  @Nested
  @DisplayName("updateImport(TSFile, String, String) - Full Import Update")
  class UpdateImportFullTests {

    @Test
    @DisplayName("should update full import statement")
    void updateImport_withExistingImport_shouldUpdateFullImport() {
      TSFile file = new TSFile(SupportedLanguage.JAVA, CLASS_WITH_SINGLE_IMPORT);
      boolean result = importService.updateImport(file, "java.util.List", "java.util.concurrent.ConcurrentLinkedQueue");
      assertTrue(result);
      String updatedCode = file.getSourceCode();
      assertTrue(updatedCode.contains("import java.util.concurrent.ConcurrentLinkedQueue;"));
      assertFalse(updatedCode.contains("import java.util.List;"));
    }

    @Test
    @DisplayName("should return false when wildcard import exists")
    void updateImport_withWildcardImport_shouldReturnFalse() {
      TSFile file = new TSFile(SupportedLanguage.JAVA, CLASS_WITH_WILDCARD_IMPORT);
      boolean result = importService.updateImport(file, "java.util.List", "java.io.File");
      assertFalse(result);
      String originalCode = file.getSourceCode();
      assertTrue(originalCode.contains("import java.util.*;"));
    }

    @Test
    @DisplayName("should return false when import not found")
    void updateImport_withNonExistentImport_shouldReturnFalse() {
      TSFile file = new TSFile(SupportedLanguage.JAVA, CLASS_WITH_SINGLE_IMPORT);
      boolean result = importService.updateImport(file, "java.io.File", "java.nio.file.Path");
      assertFalse(result);
      String originalCode = file.getSourceCode();
      assertTrue(originalCode.contains("import java.util.List;"));
    }

    @Test
    @DisplayName("should handle multiple imports correctly")
    void updateImport_withMultipleImports_shouldUpdateOnlyTargetImport() {
      TSFile file = new TSFile(SupportedLanguage.JAVA, CLASS_WITH_MULTIPLE_IMPORTS);
      boolean result = importService.updateImport(file, "java.io.IOException", "java.nio.file.FileSystemException");
      assertTrue(result);
      String updatedCode = file.getSourceCode();
      assertTrue(updatedCode.contains("import java.util.List;"));
      assertTrue(updatedCode.contains("import java.util.Map;"));
      assertTrue(updatedCode.contains("import java.nio.file.FileSystemException;"));
      assertFalse(updatedCode.contains("import java.io.IOException;"));
    }

    @Test
    @DisplayName("should throw exception for invalid old import")
    void updateImport_withInvalidOldImport_shouldThrowException() {
      TSFile file = new TSFile(SupportedLanguage.JAVA, CLASS_WITH_SINGLE_IMPORT);
      assertThrows(IllegalArgumentException.class, () -> {
        importService.updateImport(file, "InvalidImport", "java.util.ArrayList");
      });
    }

    @Test
    @DisplayName("should throw exception for invalid new import")
    void updateImport_withInvalidNewImport_shouldThrowException() {
      TSFile file = new TSFile(SupportedLanguage.JAVA, CLASS_WITH_SINGLE_IMPORT);
      assertThrows(IllegalArgumentException.class, () -> {
        importService.updateImport(file, "java.util.List", "InvalidImport");
      });
    }

    @Test
    @DisplayName("should work with files without package declaration")
    void updateImport_withNoPackageDeclaration_shouldWork() {
      TSFile file = new TSFile(SupportedLanguage.JAVA, CLASS_WITH_SINGLE_IMPORT_NO_PACKAGE);
      boolean result = importService.updateImport(file, "java.util.List", "java.util.concurrent.CopyOnWriteArrayList");
      assertTrue(result);
      String updatedCode = file.getSourceCode();
      assertTrue(updatedCode.contains("import java.util.concurrent.CopyOnWriteArrayList;"));
      assertFalse(updatedCode.contains("import java.util.List;"));
    }

    @Test
    @DisplayName("should handle same package different class correctly")
    void updateImport_withSamePackageDifferentClass_shouldWork() {
      TSFile file = new TSFile(SupportedLanguage.JAVA, CLASS_WITH_SINGLE_IMPORT);
      boolean result = importService.updateImport(file, "java.util.List", "java.util.Set");
      assertTrue(result);
      String updatedCode = file.getSourceCode();
      assertTrue(updatedCode.contains("import java.util.Set;"));
      assertFalse(updatedCode.contains("import java.util.List;"));
    }

    @Test
    @DisplayName("should handle different package same class correctly")
    void updateImport_withDifferentPackageSameClass_shouldWork() {
      TSFile file = new TSFile(SupportedLanguage.JAVA, CLASS_WITH_SINGLE_IMPORT);
      boolean result = importService.updateImport(file, "java.util.List", "java.awt.List");
      assertTrue(result);
      String updatedCode = file.getSourceCode();
      assertTrue(updatedCode.contains("import java.awt.List;"));
      assertFalse(updatedCode.contains("import java.util.List;"));
    }
  }
}
