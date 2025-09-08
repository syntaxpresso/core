package io.github.syntaxpresso.core.service.java.language;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import io.github.syntaxpresso.core.service.java.language.extra.ImportCapture;
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

class ImportDeclarationServiceTest {

  private static final String JAVA_WITH_IMPORTS =
      """
package com.example;

import java.util.List;
import java.util.Map;
import java.util.Set;
""";
  private static final String JAVA_WITH_WILDCARD_IMPORTS =
      """
package com.example;

import java.util.*;
""";
  private static final String JAVA_NO_IMPORTS = "package com.example;";
  private static final String JAVA_NO_PACKAGE_NO_IMPORTS = "class Test {}";
  private static final String JAVA_NO_PACKAGE_WITH_IMPORTS =
      """
import java.util.List;
class Test {}""";

  @TempDir Path tempDir;
  private ImportDeclarationService service;
  private TSFile tsFile;

  @BeforeEach
  void setUp() {
    this.service = new ImportDeclarationService();
  }

  private TSFile createTempTSFile(String content) throws IOException {
    Path file = Files.createFile(tempDir.resolve("test.java"));
    Files.writeString(file, content);
    return new TSFile(SupportedLanguage.JAVA, file);
  }

  @Nested
  @DisplayName("getAllImportDeclarationNodes()")
  class GetAllImportDeclarationNodes {
    @Test
    @DisplayName("should return all import nodes when imports exist")
    void shouldReturnAllImportNodesWhenImportsExist() throws IOException {
      tsFile = createTempTSFile(JAVA_WITH_IMPORTS);
      List<TSNode> imports = service.getAllImportDeclarationNodes(tsFile);
      assertEquals(3, imports.size());
    }

    @Test
    @DisplayName("should return empty list when no imports exist")
    void shouldReturnEmptyListWhenNoImportsExist() throws IOException {
      tsFile = createTempTSFile(JAVA_NO_IMPORTS);
      List<TSNode> imports = service.getAllImportDeclarationNodes(tsFile);
      assertTrue(imports.isEmpty());
    }
  }

  @Nested
  @DisplayName("getImportDeclarationNodeInfo()")
  class GetImportDeclarationNodeInfo {
    @Test
    @DisplayName("should return info for a regular import")
    void shouldReturnInfoForRegularImport() throws IOException {
      tsFile = createTempTSFile(JAVA_WITH_IMPORTS);
      TSNode importNode = service.getAllImportDeclarationNodes(tsFile).get(0);
      List<Map<String, TSNode>> info = service.getImportDeclarationNodeInfo(tsFile, importNode);
      assertFalse(info.isEmpty());
      assertEquals(
          "java.util.List",
          tsFile.getTextFromNode(info.get(0).get(ImportCapture.FULL_IMPORT_SCOPE.getCaptureName())));
      assertEquals(
          "java.util",
          tsFile.getTextFromNode(
              info.get(1).get(ImportCapture.RELATIVE_IMPORT_SCOPE.getCaptureName())));
      assertEquals(
          "List", tsFile.getTextFromNode(info.get(1).get(ImportCapture.CLASS_NAME.getCaptureName())));
    }

    @Test
    @DisplayName("should return info for a wildcard import")
    void shouldReturnInfoForWildcardImport() throws IOException {
      tsFile = createTempTSFile(JAVA_WITH_WILDCARD_IMPORTS);
      TSNode importNode = service.getAllImportDeclarationNodes(tsFile).get(0);
      List<Map<String, TSNode>> info = service.getImportDeclarationNodeInfo(tsFile, importNode);
      assertFalse(info.isEmpty());
      assertEquals(
          "java.util",
          tsFile.getTextFromNode(info.get(0).get(ImportCapture.FULL_IMPORT_SCOPE.getCaptureName())));
      assertTrue(info.get(1).containsKey(ImportCapture.ASTERISK.getCaptureName()));
    }
  }

  @Nested
  @DisplayName("findImportDeclarationNode()")
  class FindImportDeclarationNode {
    @Test
    @DisplayName("should find an existing specific import")
    void shouldFindAnExistingSpecificImport() throws IOException {
      tsFile = createTempTSFile(JAVA_WITH_IMPORTS);
      Optional<TSNode> node = service.findImportDeclarationNode(tsFile, "java.util", "List");
      assertTrue(node.isPresent());
    }

    @Test
    @DisplayName("should find an import covered by a wildcard")
    void shouldFindAnImportCoveredByAWildcard() throws IOException {
      tsFile = createTempTSFile(JAVA_WITH_WILDCARD_IMPORTS);
      Optional<TSNode> node = service.findImportDeclarationNode(tsFile, "java.util", "List");
      assertTrue(node.isPresent());
    }

    @Test
    @DisplayName("should not find a non-existent import")
    void shouldNotFindANonExistentImport() throws IOException {
      tsFile = createTempTSFile(JAVA_WITH_IMPORTS);
      Optional<TSNode> node = service.findImportDeclarationNode(tsFile, "java.awt", "Button");
      assertFalse(node.isPresent());
    }
  }

  @Nested
  @DisplayName("isClassImported()")
  class IsClassImported {
    @Test
    @DisplayName("should return true if class is specifically imported")
    void shouldReturnTrueIfClassIsSpecificallyImported() throws IOException {
      tsFile = createTempTSFile(JAVA_WITH_IMPORTS);
      assertTrue(service.isClassImported(tsFile, "java.util", "List"));
    }

    @Test
    @DisplayName("should return true if class is covered by wildcard import")
    void shouldReturnTrueIfClassIsCoveredByWildcardImport() throws IOException {
      tsFile = createTempTSFile(JAVA_WITH_WILDCARD_IMPORTS);
      assertTrue(service.isClassImported(tsFile, "java.util", "List"));
    }

    @Test
    @DisplayName("should return false if class is not imported")
    void shouldReturnFalseIfClassIsNotImported() throws IOException {
      tsFile = createTempTSFile(JAVA_WITH_IMPORTS);
      assertFalse(service.isClassImported(tsFile, "java.awt", "Button"));
    }
  }

  @Nested
  @DisplayName("addImport()")
  class AddImport {
    @Test
    @DisplayName("should add import after package when no imports exist")
    void shouldAddImportAfterPackageWhenNoImportsExist() throws IOException {
      tsFile = createTempTSFile(JAVA_NO_IMPORTS);
      TSNode packageNode =
          new PackageDeclarationService().getPackageDeclarationNode(tsFile).orElse(null);
      service.addImport(tsFile, "java.util", "List", packageNode);
      String expected = "package com.example;\n\nimport java.util.List;";
      assertEquals(expected, tsFile.getSourceCode());
    }

    @Test
    @DisplayName("should add import at beginning when no package or imports exist")
    void shouldAddImportAtBeginningWhenNoPackageOrImportsExist() throws IOException {
      tsFile = createTempTSFile(JAVA_NO_PACKAGE_NO_IMPORTS);
      service.addImport(tsFile, "java.util", "List", null);
      String expected = "import java.util.List;\n\nclass Test {}";
      assertEquals(expected, tsFile.getSourceCode());
    }

    @Test
    @DisplayName("should add import after existing imports")
    void shouldAddImportAfterExistingImports() throws IOException {
      tsFile = createTempTSFile(JAVA_NO_PACKAGE_WITH_IMPORTS);
      TSNode packageNode =
          new PackageDeclarationService().getPackageDeclarationNode(tsFile).orElse(null);
      service.addImport(tsFile, "java.util", "Map", packageNode);
      String expected = "import java.util.List;\nimport java.util.Map;";
      assertTrue(tsFile.getSourceCode().contains(expected));
    }

    @Test
    @DisplayName("should not add import if it already exists")
    void shouldNotAddImportIfItAlreadyExists() throws IOException {
      tsFile = createTempTSFile(JAVA_WITH_IMPORTS);
      String originalSource = tsFile.getSourceCode();
      TSNode packageNode =
          new PackageDeclarationService().getPackageDeclarationNode(tsFile).orElse(null);
      service.addImport(tsFile, "java.util", "List", packageNode);
      assertEquals(originalSource, tsFile.getSourceCode());
    }
  }

  @Nested
  @DisplayName("updateImport()")
  class UpdateImport {
    @Test
    @DisplayName("should update an existing import")
    void shouldUpdateAnExistingImport() throws IOException {
      tsFile = createTempTSFile(JAVA_WITH_IMPORTS);
      boolean updated =
          service.updateImport(tsFile, "java.util", "java.awt", "List", "Button");
      assertTrue(updated);
      assertTrue(tsFile.getSourceCode().contains("import java.awt.Button;"));
      assertFalse(tsFile.getSourceCode().contains("import java.util.List;"));
    }

    @Test
    @DisplayName("should not update if new import already exists")
    void shouldNotUpdateIfNewImportAlreadyExists() throws IOException {
      tsFile = createTempTSFile(JAVA_WITH_IMPORTS);
      boolean updated = service.updateImport(tsFile, "java.util", "java.util", "Set", "Map");
      assertFalse(updated);
    }

    @Test
    @DisplayName("should not update if old import does not exist")
    void shouldNotUpdateIfOldImportDoesNotExist() throws IOException {
      tsFile = createTempTSFile(JAVA_WITH_IMPORTS);
      boolean updated =
          service.updateImport(tsFile, "com.nonexistent", "com.new", "Class", "NewClass");
      assertFalse(updated);
    }
  }
}
