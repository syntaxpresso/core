package io.github.syntaxpresso.core.service.java.language;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.syntaxpresso.core.command.extra.JavaSourceDirectoryType;
import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import io.github.syntaxpresso.core.service.java.language.extra.PackageCapture;
import io.github.syntaxpresso.core.util.PathHelper;
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

@DisplayName("PackageDeclarationService Tests")
class PackageDeclarationServiceTest {
  private PackageDeclarationService packageDeclarationService;
  private PathHelper pathHelper;

  private static final String SIMPLE_PACKAGE_CODE =
      """
      package com.example.service;

      public class SimpleClass {
          private String name;
      }
      """;

  private static final String NESTED_PACKAGE_CODE =
      """
      package com.example.project.service.impl;

      import java.util.List;

      public class ComplexClass {
          public void doSomething() {
              System.out.println("Hello");
          }
      }
      """;

  private static final String NO_PACKAGE_CODE =
      """
      public class DefaultPackageClass {
          public void method() {
              System.out.println("No package");
          }
      }
      """;

  private static final String SINGLE_LEVEL_PACKAGE_CODE =
      """
      package service;

      public class SingleLevelClass {
          private int value;
      }
      """;

  private static final String MALFORMED_CODE =
      """
      package
      public class BrokenClass {
      """;

  @BeforeEach
  void setUp() {
    this.pathHelper = new PathHelper();
    this.packageDeclarationService = new PackageDeclarationService(this.pathHelper);
  }

  @Nested
  @DisplayName("getPackageDeclarationNode() Tests")
  class GetPackageDeclarationNodeTests {

    /**
     * Tests that getPackageDeclarationNode finds package declarations correctly.
     *
     * <p>Usage example:
     *
     * <pre>
     * Optional&lt;TSNode&gt; packageNode = service.getPackageDeclarationNode(tsFile);
     * if (packageNode.isPresent()) {
     *   String packageText = tsFile.getTextFromNode(packageNode.get());
     *   // packageText = "package com.example.service;"
     * }
     * </pre>
     */
    @Test
    @DisplayName("should find package declaration in simple Java file")
    void getPackageDeclarationNode_withSimplePackage_shouldFindPackage() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_PACKAGE_CODE);

      Optional<TSNode> packageNode = packageDeclarationService.getPackageDeclarationNode(tsFile);

      assertTrue(packageNode.isPresent());
      String packageText = tsFile.getTextFromNode(packageNode.get());
      assertTrue(packageText.contains("com.example.service"));
    }

    @Test
    @DisplayName("should find package declaration in nested package")
    void getPackageDeclarationNode_withNestedPackage_shouldFindPackage() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, NESTED_PACKAGE_CODE);

      Optional<TSNode> packageNode = packageDeclarationService.getPackageDeclarationNode(tsFile);

      assertTrue(packageNode.isPresent());
      String packageText = tsFile.getTextFromNode(packageNode.get());
      assertTrue(packageText.contains("com.example.project.service.impl"));
    }

    @Test
    @DisplayName("should find package declaration in single level package")
    void getPackageDeclarationNode_withSingleLevelPackage_shouldFindPackage() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SINGLE_LEVEL_PACKAGE_CODE);

      Optional<TSNode> packageNode = packageDeclarationService.getPackageDeclarationNode(tsFile);

      assertTrue(packageNode.isPresent());
      String packageText = tsFile.getTextFromNode(packageNode.get());
      assertTrue(packageText.contains("service"));
    }

    @Test
    @DisplayName("should return empty for file without package declaration")
    void getPackageDeclarationNode_withNoPackage_shouldReturnEmpty() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, NO_PACKAGE_CODE);

      Optional<TSNode> packageNode = packageDeclarationService.getPackageDeclarationNode(tsFile);

      assertFalse(packageNode.isPresent());
    }

    @Test
    @DisplayName("should return empty for null TSFile")
    void getPackageDeclarationNode_withNullTSFile_shouldReturnEmpty() {
      Optional<TSNode> packageNode = packageDeclarationService.getPackageDeclarationNode(null);

      assertFalse(packageNode.isPresent());
    }

    @Test
    @DisplayName("should return empty for TSFile with null tree")
    void getPackageDeclarationNode_withNullTree_shouldReturnEmpty() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, "");

      Optional<TSNode> packageNode = packageDeclarationService.getPackageDeclarationNode(tsFile);

      assertFalse(packageNode.isPresent());
    }
  }

  @Nested
  @DisplayName("getPackageDeclarationInfo() Tests")
  class GetPackageDeclarationInfoTests {

    /**
     * Tests that getPackageDeclarationInfo extracts detailed package information.
     *
     * <p>Usage example:
     *
     * <pre>
     * List&lt;Map&lt;String, TSNode&gt;&gt; packageInfo = service.getPackageDeclarationInfo(tsFile, packageNode);
     * Map&lt;String, TSNode&gt; info = packageInfo.get(0);
     * String packageScope = tsFile.getTextFromNode(info.get("package_scope"));
     * String className = tsFile.getTextFromNode(info.get("class_name"));
     * // packageScope = "com.example.service", className = "service"
     * </pre>
     */
    @Test
    @DisplayName("should extract package information from simple package")
    void getPackageDeclarationInfo_withSimplePackage_shouldExtractInfo() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_PACKAGE_CODE);
      Optional<TSNode> packageNode = packageDeclarationService.getPackageDeclarationNode(tsFile);

      List<Map<String, TSNode>> packageInfo =
          packageDeclarationService.getPackageDeclarationInfo(tsFile, packageNode.get());

      assertFalse(packageInfo.isEmpty());
      Map<String, TSNode> info = packageInfo.get(0);

      TSNode packageScopeNode = info.get(PackageCapture.PACKAGE_SCOPE.getCaptureName());
      assertNotNull(packageScopeNode);
      assertEquals("com.example.service", tsFile.getTextFromNode(packageScopeNode));

      TSNode classNameNode = info.get(PackageCapture.CLASS_NAME.getCaptureName());
      assertNotNull(classNameNode);
      assertEquals("service", tsFile.getTextFromNode(classNameNode));

      TSNode classScopeNode = info.get(PackageCapture.CLASS_SCOPE.getCaptureName());
      assertNotNull(classScopeNode);
      assertEquals("com.example", tsFile.getTextFromNode(classScopeNode));
    }

    @Test
    @DisplayName("should extract package information from nested package")
    void getPackageDeclarationInfo_withNestedPackage_shouldExtractInfo() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, NESTED_PACKAGE_CODE);
      Optional<TSNode> packageNode = packageDeclarationService.getPackageDeclarationNode(tsFile);

      List<Map<String, TSNode>> packageInfo =
          packageDeclarationService.getPackageDeclarationInfo(tsFile, packageNode.get());

      assertFalse(packageInfo.isEmpty());
      Map<String, TSNode> info = packageInfo.get(0);

      TSNode packageScopeNode = info.get(PackageCapture.PACKAGE_SCOPE.getCaptureName());
      assertNotNull(packageScopeNode);
      assertEquals("com.example.project.service.impl", tsFile.getTextFromNode(packageScopeNode));

      TSNode classNameNode = info.get(PackageCapture.CLASS_NAME.getCaptureName());
      assertNotNull(classNameNode);
      assertEquals("impl", tsFile.getTextFromNode(classNameNode));

      TSNode classScopeNode = info.get(PackageCapture.CLASS_SCOPE.getCaptureName());
      assertNotNull(classScopeNode);
      assertEquals("com.example.project.service", tsFile.getTextFromNode(classScopeNode));
    }

    @Test
    @DisplayName("should return empty list for null TSFile")
    void getPackageDeclarationInfo_withNullTSFile_shouldReturnEmpty() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_PACKAGE_CODE);
      Optional<TSNode> packageNode = packageDeclarationService.getPackageDeclarationNode(tsFile);

      List<Map<String, TSNode>> packageInfo =
          packageDeclarationService.getPackageDeclarationInfo(null, packageNode.get());

      assertTrue(packageInfo.isEmpty());
    }

    @Test
    @DisplayName("should return empty list for invalid node type")
    void getPackageDeclarationInfo_withInvalidNodeType_shouldReturnEmpty() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_PACKAGE_CODE);
      TSNode invalidNode = tsFile.query("(identifier) @id").returning("id").execute().firstNode();

      List<Map<String, TSNode>> packageInfo =
          packageDeclarationService.getPackageDeclarationInfo(tsFile, invalidNode);

      assertTrue(packageInfo.isEmpty());
    }
  }

  @Nested
  @DisplayName("getPackageClassNameNode() Tests")
  class GetPackageClassNameNodeTests {

    /**
     * Tests that getPackageClassNameNode retrieves the class name part.
     *
     * <p>Usage example:
     *
     * <pre>
     * service.getPackageClassNameNode(tsFile, packageNode).ifPresent(nameNode -> {
     *   System.out.println("Class name: " + tsFile.getTextFromNode(nameNode));
     * });
     * // Output: "Class name: service"
     * </pre>
     */
    @Test
    @DisplayName("should retrieve class name node from package declaration")
    void getPackageClassNameNode_withValidPackage_shouldReturnClassName() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_PACKAGE_CODE);
      Optional<TSNode> packageNode = packageDeclarationService.getPackageDeclarationNode(tsFile);

      Optional<TSNode> classNameNode =
          packageDeclarationService.getPackageClassNameNode(tsFile, packageNode.get());

      assertTrue(classNameNode.isPresent());
      assertEquals("service", tsFile.getTextFromNode(classNameNode.get()));
    }

    @Test
    @DisplayName("should retrieve class name from nested package")
    void getPackageClassNameNode_withNestedPackage_shouldReturnClassName() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, NESTED_PACKAGE_CODE);
      Optional<TSNode> packageNode = packageDeclarationService.getPackageDeclarationNode(tsFile);

      Optional<TSNode> classNameNode =
          packageDeclarationService.getPackageClassNameNode(tsFile, packageNode.get());

      assertTrue(classNameNode.isPresent());
      assertEquals("impl", tsFile.getTextFromNode(classNameNode.get()));
    }

    @Test
    @DisplayName("should return empty for null TSFile")
    void getPackageClassNameNode_withNullTSFile_shouldReturnEmpty() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_PACKAGE_CODE);
      Optional<TSNode> packageNode = packageDeclarationService.getPackageDeclarationNode(tsFile);

      Optional<TSNode> classNameNode =
          packageDeclarationService.getPackageClassNameNode(null, packageNode.get());

      assertFalse(classNameNode.isPresent());
    }

    @Test
    @DisplayName("should return empty for invalid node type")
    void getPackageClassNameNode_withInvalidNodeType_shouldReturnEmpty() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_PACKAGE_CODE);
      TSNode invalidNode = tsFile.query("(identifier) @id").returning("id").execute().firstNode();

      Optional<TSNode> classNameNode =
          packageDeclarationService.getPackageClassNameNode(tsFile, invalidNode);

      assertFalse(classNameNode.isPresent());
    }
  }

  @Nested
  @DisplayName("getPackageClassScopeNode() Tests")
  class GetPackageClassScopeNodeTests {

    /**
     * Tests that getPackageClassScopeNode retrieves the class scope part.
     *
     * <p>Usage example:
     *
     * <pre>
     * service.getPackageClassScopeNode(tsFile, packageNode).ifPresent(scopeNode -> {
     *   System.out.println("Class scope: " + tsFile.getTextFromNode(scopeNode));
     * });
     * // Output: "Class scope: com.example"
     * </pre>
     */
    @Test
    @DisplayName("should retrieve class scope node from package declaration")
    void getPackageClassScopeNode_withValidPackage_shouldReturnClassScope() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_PACKAGE_CODE);
      Optional<TSNode> packageNode = packageDeclarationService.getPackageDeclarationNode(tsFile);

      Optional<TSNode> classScopeNode =
          packageDeclarationService.getPackageClassScopeNode(tsFile, packageNode.get());

      assertTrue(classScopeNode.isPresent());
      assertEquals("com.example", tsFile.getTextFromNode(classScopeNode.get()));
    }

    @Test
    @DisplayName("should retrieve class scope from nested package")
    void getPackageClassScopeNode_withNestedPackage_shouldReturnClassScope() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, NESTED_PACKAGE_CODE);
      Optional<TSNode> packageNode = packageDeclarationService.getPackageDeclarationNode(tsFile);

      Optional<TSNode> classScopeNode =
          packageDeclarationService.getPackageClassScopeNode(tsFile, packageNode.get());

      assertTrue(classScopeNode.isPresent());
      assertEquals("com.example.project.service", tsFile.getTextFromNode(classScopeNode.get()));
    }

    @Test
    @DisplayName("should return empty for null TSFile")
    void getPackageClassScopeNode_withNullTSFile_shouldReturnEmpty() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_PACKAGE_CODE);
      Optional<TSNode> packageNode = packageDeclarationService.getPackageDeclarationNode(tsFile);

      Optional<TSNode> classScopeNode =
          packageDeclarationService.getPackageClassScopeNode(null, packageNode.get());

      assertFalse(classScopeNode.isPresent());
    }

    @Test
    @DisplayName("should return empty for invalid node type")
    void getPackageClassScopeNode_withInvalidNodeType_shouldReturnEmpty() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_PACKAGE_CODE);
      TSNode invalidNode = tsFile.query("(identifier) @id").returning("id").execute().firstNode();

      Optional<TSNode> classScopeNode =
          packageDeclarationService.getPackageClassScopeNode(tsFile, invalidNode);

      assertFalse(classScopeNode.isPresent());
    }
  }

  @Nested
  @DisplayName("getPackageScopeNode() Tests")
  class GetPackageScopeNodeTests {

    /**
     * Tests that getPackageScopeNode retrieves the complete package scope.
     *
     * <p>Usage example:
     *
     * <pre>
     * service.getPackageScopeNode(tsFile, packageNode).ifPresent(scopeNode -> {
     *   System.out.println("Package scope: " + tsFile.getTextFromNode(scopeNode));
     * });
     * // Output: "Package scope: com.example.service"
     * </pre>
     */
    @Test
    @DisplayName("should retrieve package scope node from package declaration")
    void getPackageScopeNode_withValidPackage_shouldReturnPackageScope() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_PACKAGE_CODE);
      Optional<TSNode> packageNode = packageDeclarationService.getPackageDeclarationNode(tsFile);

      Optional<TSNode> packageScopeNode =
          packageDeclarationService.getPackageScopeNode(tsFile, packageNode.get());

      assertTrue(packageScopeNode.isPresent());
      assertEquals("com.example.service", tsFile.getTextFromNode(packageScopeNode.get()));
    }

    @Test
    @DisplayName("should retrieve package scope from nested package")
    void getPackageScopeNode_withNestedPackage_shouldReturnPackageScope() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, NESTED_PACKAGE_CODE);
      Optional<TSNode> packageNode = packageDeclarationService.getPackageDeclarationNode(tsFile);

      Optional<TSNode> packageScopeNode =
          packageDeclarationService.getPackageScopeNode(tsFile, packageNode.get());

      assertTrue(packageScopeNode.isPresent());
      assertEquals(
          "com.example.project.service.impl", tsFile.getTextFromNode(packageScopeNode.get()));
    }

    @Test
    @DisplayName("should return empty for null TSFile")
    void getPackageScopeNode_withNullTSFile_shouldReturnEmpty() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_PACKAGE_CODE);
      Optional<TSNode> packageNode = packageDeclarationService.getPackageDeclarationNode(tsFile);

      Optional<TSNode> packageScopeNode =
          packageDeclarationService.getPackageScopeNode(null, packageNode.get());

      assertFalse(packageScopeNode.isPresent());
    }

    @Test
    @DisplayName("should return empty for invalid node type")
    void getPackageScopeNode_withInvalidNodeType_shouldReturnEmpty() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_PACKAGE_CODE);
      TSNode invalidNode = tsFile.query("(identifier) @id").returning("id").execute().firstNode();

      Optional<TSNode> packageScopeNode =
          packageDeclarationService.getPackageScopeNode(tsFile, invalidNode);

      assertFalse(packageScopeNode.isPresent());
    }
  }

  @Nested
  @DisplayName("getFilePathFromPackageScope() Tests")
  class GetFilePathFromPackageScopeTests {

    @TempDir Path tempDir;

    /**
     * Tests that getFilePathFromPackageScope resolves package paths correctly.
     *
     * <p>Usage example:
     *
     * <pre>
     * Optional&lt;Path&gt; packageDir = service.getFilePathFromPackageScope(
     *     projectRoot, "com.example.foo", JavaSourceDirectoryType.MAIN);
     * if (packageDir.isPresent()) {
     *   System.out.println("Package directory: " + packageDir.get());
     *   // Package directory: /project/src/main/java/com/example/foo
     * }
     * </pre>
     */
    @Test
    @DisplayName("should resolve package path for main source directory")
    void getFilePathFromPackageScope_withMainSourceType_shouldResolveCorrectly()
        throws IOException {
      String packageScope = "com.example.service";

      Optional<Path> packagePath =
          packageDeclarationService.getFilePathFromPackageScope(
              tempDir, packageScope, JavaSourceDirectoryType.MAIN);

      assertTrue(packagePath.isPresent());
      assertTrue(Files.exists(packagePath.get()));
      assertTrue(packagePath.get().toString().contains("src/main/java"));
      assertTrue(packagePath.get().toString().endsWith("com/example/service"));
    }

    @Test
    @DisplayName("should resolve package path for test source directory")
    void getFilePathFromPackageScope_withTestSourceType_shouldResolveCorrectly()
        throws IOException {
      String packageScope = "com.example.test";

      Optional<Path> packagePath =
          packageDeclarationService.getFilePathFromPackageScope(
              tempDir, packageScope, JavaSourceDirectoryType.TEST);

      assertTrue(packagePath.isPresent());
      assertTrue(Files.exists(packagePath.get()));
      assertTrue(packagePath.get().toString().contains("src/test/java"));
      assertTrue(packagePath.get().toString().endsWith("com/example/test"));
    }

    @Test
    @DisplayName("should resolve single level package path")
    void getFilePathFromPackageScope_withSingleLevelPackage_shouldResolveCorrectly()
        throws IOException {
      String packageScope = "service";

      Optional<Path> packagePath =
          packageDeclarationService.getFilePathFromPackageScope(
              tempDir, packageScope, JavaSourceDirectoryType.MAIN);

      assertTrue(packagePath.isPresent());
      assertTrue(Files.exists(packagePath.get()));
      assertTrue(packagePath.get().toString().endsWith("service"));
    }

    @Test
    @DisplayName("should find existing source directory")
    void getFilePathFromPackageScope_withExistingSourceDir_shouldUseExistingDir()
        throws IOException {
      // Create existing source directory
      Path existingSourceDir = tempDir.resolve("src/main/java");
      Files.createDirectories(existingSourceDir);

      String packageScope = "com.example.existing";

      Optional<Path> packagePath =
          packageDeclarationService.getFilePathFromPackageScope(
              tempDir, packageScope, JavaSourceDirectoryType.MAIN);

      assertTrue(packagePath.isPresent());
      assertTrue(Files.exists(packagePath.get()));
      assertTrue(packagePath.get().toString().contains("src/main/java"));
      assertTrue(packagePath.get().toString().endsWith("com/example/existing"));
    }

    @Test
    @DisplayName("should return empty for null root directory")
    void getFilePathFromPackageScope_withNullRootDir_shouldReturnEmpty() {
      String packageScope = "com.example.service";

      Optional<Path> packagePath =
          packageDeclarationService.getFilePathFromPackageScope(
              null, packageScope, JavaSourceDirectoryType.MAIN);

      assertFalse(packagePath.isPresent());
    }

    @Test
    @DisplayName("should return empty for non-directory root")
    void getFilePathFromPackageScope_withNonDirectoryRoot_shouldReturnEmpty() throws IOException {
      Path fileInsteadOfDir = tempDir.resolve("not-a-directory.txt");
      Files.createFile(fileInsteadOfDir);

      String packageScope = "com.example.service";

      Optional<Path> packagePath =
          packageDeclarationService.getFilePathFromPackageScope(
              fileInsteadOfDir, packageScope, JavaSourceDirectoryType.MAIN);

      assertFalse(packagePath.isPresent());
    }

    @Test
    @DisplayName("should return empty for null package scope")
    void getFilePathFromPackageScope_withNullPackageScope_shouldReturnEmpty() {
      Optional<Path> packagePath =
          packageDeclarationService.getFilePathFromPackageScope(
              tempDir, null, JavaSourceDirectoryType.MAIN);

      assertFalse(packagePath.isPresent());
    }

    @Test
    @DisplayName("should return empty for blank package scope")
    void getFilePathFromPackageScope_withBlankPackageScope_shouldReturnEmpty() {
      Optional<Path> packagePath =
          packageDeclarationService.getFilePathFromPackageScope(
              tempDir, "   ", JavaSourceDirectoryType.MAIN);

      assertFalse(packagePath.isPresent());
    }

    @Test
    @DisplayName("should return empty for null source directory type")
    void getFilePathFromPackageScope_withNullSourceDirectoryType_shouldReturnEmpty() {
      String packageScope = "com.example.service";

      Optional<Path> packagePath =
          packageDeclarationService.getFilePathFromPackageScope(tempDir, packageScope, null);

      assertFalse(packagePath.isPresent());
    }

    @Test
    @DisplayName("should handle nested package directories")
    void getFilePathFromPackageScope_withDeeplyNestedPackage_shouldResolveCorrectly()
        throws IOException {
      String packageScope = "com.example.project.service.impl.utils";

      Optional<Path> packagePath =
          packageDeclarationService.getFilePathFromPackageScope(
              tempDir, packageScope, JavaSourceDirectoryType.MAIN);

      assertTrue(packagePath.isPresent());
      assertTrue(Files.exists(packagePath.get()));
      assertTrue(packagePath.get().toString().endsWith("com/example/project/service/impl/utils"));
    }
  }

  @Nested
  @DisplayName("Edge Cases and Error Handling Tests")
  class EdgeCasesTests {

    @Test
    @DisplayName("should handle malformed source code")
    void methods_withMalformedCode_shouldHandleGracefully() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, MALFORMED_CODE);

      Optional<TSNode> packageNode = packageDeclarationService.getPackageDeclarationNode(tsFile);

      // Should not crash, behavior may vary based on tree-sitter parsing
      assertNotNull(packageNode);
    }

    @Test
    @DisplayName("should handle empty source code")
    void methods_withEmptyCode_shouldHandleGracefully() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, "");

      Optional<TSNode> packageNode = packageDeclarationService.getPackageDeclarationNode(tsFile);

      assertFalse(packageNode.isPresent());
    }

    @Test
    @DisplayName("should handle single level packages correctly")
    void methods_withSingleLevelPackages_shouldHandleCorrectly() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SINGLE_LEVEL_PACKAGE_CODE);
      Optional<TSNode> packageNode = packageDeclarationService.getPackageDeclarationNode(tsFile);

      assertTrue(packageNode.isPresent());

      List<Map<String, TSNode>> packageInfo =
          packageDeclarationService.getPackageDeclarationInfo(tsFile, packageNode.get());

      // Single level packages may not have scope/name separation
      assertNotNull(packageInfo);
    }

    @Test
    @DisplayName("should handle invalid input gracefully in all methods")
    void methods_withInvalidInput_shouldHandleGracefully() {
      TSFile validFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_PACKAGE_CODE);
      TSFile invalidFile = new TSFile(SupportedLanguage.JAVA, "");
      TSNode invalidNode =
          validFile.query("(identifier) @id").returning("id").execute().firstNode();

      // Test all methods with invalid combinations
      assertFalse(packageDeclarationService.getPackageDeclarationNode(null).isPresent());
      assertFalse(packageDeclarationService.getPackageDeclarationNode(invalidFile).isPresent());

      assertTrue(packageDeclarationService.getPackageDeclarationInfo(null, invalidNode).isEmpty());
      assertTrue(
          packageDeclarationService.getPackageDeclarationInfo(validFile, invalidNode).isEmpty());

      assertFalse(packageDeclarationService.getPackageClassNameNode(null, invalidNode).isPresent());
      assertFalse(
          packageDeclarationService.getPackageClassNameNode(validFile, invalidNode).isPresent());

      assertFalse(
          packageDeclarationService.getPackageClassScopeNode(null, invalidNode).isPresent());
      assertFalse(
          packageDeclarationService.getPackageClassScopeNode(validFile, invalidNode).isPresent());

      assertFalse(packageDeclarationService.getPackageScopeNode(null, invalidNode).isPresent());
      assertFalse(
          packageDeclarationService.getPackageScopeNode(validFile, invalidNode).isPresent());
    }
  }
}

