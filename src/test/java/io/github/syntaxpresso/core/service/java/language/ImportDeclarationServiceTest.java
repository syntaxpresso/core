package io.github.syntaxpresso.core.service.java.language;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import java.util.List;
import java.util.Map;
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
}
