package io.github.syntaxpresso.core.service.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.common.extra.SupportedIDE;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import io.github.syntaxpresso.core.service.extra.JavaIdentifierType;
import io.github.syntaxpresso.core.service.java.language.AnnotationService;
import io.github.syntaxpresso.core.service.java.language.ClassDeclarationService;
import io.github.syntaxpresso.core.service.java.language.FieldDeclarationService;
import io.github.syntaxpresso.core.service.java.language.FormalParameterService;
import io.github.syntaxpresso.core.service.java.language.ImportDeclarationService;
import io.github.syntaxpresso.core.service.java.language.LocalVariableDeclarationService;
import io.github.syntaxpresso.core.service.java.language.MethodDeclarationService;
import io.github.syntaxpresso.core.service.java.language.PackageDeclarationService;
import io.github.syntaxpresso.core.service.java.language.VariableNamingService;
import io.github.syntaxpresso.core.util.PathHelper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.treesitter.TSNode;

@DisplayName("JavaLanguageService Tests")
class JavaLanguageServiceTest {
  private JavaLanguageService javaLanguageService;
  private PathHelper pathHelper;

  private static final String CLASS_CODE =
      "public class TestClass {\n"
          + "  private String fieldName;\n"
          + "  \n"
          + "  public void methodName(String paramName) {\n"
          + "    String localVar = \"test\";\n"
          + "  }\n"
          + "}";

  @BeforeEach
  void setUp() {
    this.pathHelper = new PathHelper();
    VariableNamingService variableNamingService = new VariableNamingService();
    FieldDeclarationService fieldDeclarationService = new FieldDeclarationService();
    FormalParameterService formalParameterService = new FormalParameterService();
    MethodDeclarationService methodDeclarationService =
        new MethodDeclarationService(formalParameterService);
    ClassDeclarationService classDeclarationService =
        new ClassDeclarationService(fieldDeclarationService, methodDeclarationService);
    PackageDeclarationService packageDeclarationService =
        new PackageDeclarationService(this.pathHelper);
    ImportDeclarationService importDeclarationService = new ImportDeclarationService();
    LocalVariableDeclarationService localVariableDeclarationService =
        new LocalVariableDeclarationService();
    AnnotationService annotationService = new AnnotationService();

    this.javaLanguageService =
        new JavaLanguageService(
            this.pathHelper,
            variableNamingService,
            classDeclarationService,
            packageDeclarationService,
            importDeclarationService,
            localVariableDeclarationService,
            annotationService);
  }

  @Nested
  @DisplayName("getAllJavaFilesFromCwd() Tests")
  class GetAllJavaFilesFromCwdTests {

    /**
     * Tests that getAllJavaFilesFromCwd correctly finds all Java files in a directory structure.
     *
     * <p>Usage example:
     *
     * <pre>
     * Path cwd = Paths.get("/project/root");
     * List&lt;TSFile&gt; javaFiles = javaLanguageService.getAllJavaFilesFromCwd(cwd);
     * // Returns all .java files found recursively
     * </pre>
     */
    @Test
    @DisplayName("should find all Java files in directory structure")
    void getAllJavaFilesFromCwd_withJavaFiles_shouldReturnAllFiles(@TempDir Path tempDir)
        throws IOException {
      Files.createFile(tempDir.resolve("Main.java"));
      Files.createFile(tempDir.resolve("Helper.java"));
      Files.createFile(tempDir.resolve("README.md"));
      Path subDir = tempDir.resolve("subpackage");
      Files.createDirectory(subDir);
      Files.createFile(subDir.resolve("SubClass.java"));

      List<TSFile> result =
          JavaLanguageServiceTest.this.javaLanguageService.getAllJavaFilesFromCwd(tempDir);

      assertEquals(3, result.size());
      assertTrue(result.stream().allMatch(f -> f.getFile().getName().endsWith(".java")));
    }

    /**
     * Tests that getAllJavaFilesFromCwd returns empty list when no Java files exist.
     *
     * <p>Usage example:
     *
     * <pre>
     * Path cwd = Paths.get("/project/without/java");
     * List&lt;TSFile&gt; javaFiles = javaLanguageService.getAllJavaFilesFromCwd(cwd);
     * // Returns empty list
     * </pre>
     */
    @Test
    @DisplayName("should return empty list when no Java files exist")
    void getAllJavaFilesFromCwd_withNoJavaFiles_shouldReturnEmptyList(@TempDir Path tempDir)
        throws IOException {
      Files.createFile(tempDir.resolve("README.md"));
      Files.createFile(tempDir.resolve("config.xml"));

      List<TSFile> result =
          JavaLanguageServiceTest.this.javaLanguageService.getAllJavaFilesFromCwd(tempDir);

      assertTrue(result.isEmpty());
    }

    /**
     * Tests that getAllJavaFilesFromCwd handles exceptions gracefully and returns empty list.
     *
     * <p>Usage example:
     *
     * <pre>
     * Path invalidPath = Paths.get("/nonexistent/path");
     * List&lt;TSFile&gt; javaFiles = javaLanguageService.getAllJavaFilesFromCwd(invalidPath);
     * // Returns empty list instead of throwing exception
     * </pre>
     */
    @Test
    @DisplayName("should return empty list on exception")
    void getAllJavaFilesFromCwd_withInvalidPath_shouldReturnEmptyList() {
      Path invalidPath = Path.of("/nonexistent/path");

      List<TSFile> result =
          JavaLanguageServiceTest.this.javaLanguageService.getAllJavaFilesFromCwd(invalidPath);

      assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("should handle empty directory")
    void getAllJavaFilesFromCwd_withEmptyDirectory_shouldReturnEmptyList(@TempDir Path tempDir) {
      List<TSFile> result =
          JavaLanguageServiceTest.this.javaLanguageService.getAllJavaFilesFromCwd(tempDir);

      assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("should find Java files in nested directories")
    void getAllJavaFilesFromCwd_withNestedJavaFiles_shouldReturnAllFiles(@TempDir Path tempDir)
        throws IOException {
      Path level1 = tempDir.resolve("level1");
      Path level2 = level1.resolve("level2");
      Files.createDirectories(level2);
      Files.createFile(tempDir.resolve("Root.java"));
      Files.createFile(level1.resolve("Level1.java"));
      Files.createFile(level2.resolve("Level2.java"));

      List<TSFile> result =
          JavaLanguageServiceTest.this.javaLanguageService.getAllJavaFilesFromCwd(tempDir);

      assertEquals(3, result.size());
    }
  }

  @Nested
  @DisplayName("isJavaProject() Tests")
  class IsJavaProjectTests {

    /**
     * Tests that isJavaProject correctly identifies Maven projects.
     *
     * <p>Usage example:
     *
     * <pre>
     * Path mavenProject = Paths.get("/path/to/maven/project");
     * boolean isMaven = javaLanguageService.isJavaProject(mavenProject);
     * // Returns true if pom.xml exists
     * </pre>
     */
    @Test
    @DisplayName("should identify Maven project by pom.xml")
    void isJavaProject_withPomXml_shouldReturnTrue(@TempDir Path tempDir) throws IOException {
      Files.createFile(tempDir.resolve("pom.xml"));

      boolean result = JavaLanguageServiceTest.this.javaLanguageService.isJavaProject(tempDir);

      assertTrue(result);
    }

    /**
     * Tests that isJavaProject correctly identifies Gradle projects with build.gradle.
     *
     * <p>Usage example:
     *
     * <pre>
     * Path gradleProject = Paths.get("/path/to/gradle/project");
     * boolean isGradle = javaLanguageService.isJavaProject(gradleProject);
     * // Returns true if build.gradle exists
     * </pre>
     */
    @Test
    @DisplayName("should identify Gradle project by build.gradle")
    void isJavaProject_withBuildGradle_shouldReturnTrue(@TempDir Path tempDir) throws IOException {
      Files.createFile(tempDir.resolve("build.gradle"));

      boolean result = JavaLanguageServiceTest.this.javaLanguageService.isJavaProject(tempDir);

      assertTrue(result);
    }

    /**
     * Tests that isJavaProject correctly identifies Gradle projects with build.gradle.kts.
     *
     * <p>Usage example:
     *
     * <pre>
     * Path gradleKtsProject = Paths.get("/path/to/gradle/kts/project");
     * boolean isGradleKts = javaLanguageService.isJavaProject(gradleKtsProject);
     * // Returns true if build.gradle.kts exists
     * </pre>
     */
    @Test
    @DisplayName("should identify Gradle Kotlin project by build.gradle.kts")
    void isJavaProject_withBuildGradleKts_shouldReturnTrue(@TempDir Path tempDir)
        throws IOException {
      Files.createFile(tempDir.resolve("build.gradle.kts"));

      boolean result = JavaLanguageServiceTest.this.javaLanguageService.isJavaProject(tempDir);

      assertTrue(result);
    }

    /**
     * Tests that isJavaProject correctly identifies projects by standard Maven/Gradle directory
     * structure.
     *
     * <p>Usage example:
     *
     * <pre>
     * Path standardProject = Paths.get("/path/to/standard/project");
     * boolean isStandard = javaLanguageService.isJavaProject(standardProject);
     * // Returns true if src/main/java directory exists
     * </pre>
     */
    @Test
    @DisplayName("should identify Java project by src/main/java directory")
    void isJavaProject_withSrcMainJavaDirectory_shouldReturnTrue(@TempDir Path tempDir)
        throws IOException {
      Path srcMainJava = tempDir.resolve("src/main/java");
      Files.createDirectories(srcMainJava);

      boolean result = JavaLanguageServiceTest.this.javaLanguageService.isJavaProject(tempDir);

      assertTrue(result);
    }

    @Test
    @DisplayName("should return false for non-Java project")
    void isJavaProject_withoutJavaIndicators_shouldReturnFalse(@TempDir Path tempDir)
        throws IOException {
      Files.createFile(tempDir.resolve("README.md"));
      Files.createFile(tempDir.resolve("package.json"));

      boolean result = JavaLanguageServiceTest.this.javaLanguageService.isJavaProject(tempDir);

      assertFalse(result);
    }

    @Test
    @DisplayName("should return false for null path")
    void isJavaProject_withNullPath_shouldReturnFalse() {
      boolean result = JavaLanguageServiceTest.this.javaLanguageService.isJavaProject(null);

      assertFalse(result);
    }

    @Test
    @DisplayName("should return false for non-directory path")
    void isJavaProject_withFilePath_shouldReturnFalse(@TempDir Path tempDir) throws IOException {
      Path file = tempDir.resolve("not-a-directory.txt");
      Files.createFile(file);

      boolean result = JavaLanguageServiceTest.this.javaLanguageService.isJavaProject(file);

      assertFalse(result);
    }

    @Test
    @DisplayName("should return false for non-existent directory")
    void isJavaProject_withNonExistentDirectory_shouldReturnFalse() {
      Path nonExistent = Path.of("/nonexistent/directory");

      boolean result = JavaLanguageServiceTest.this.javaLanguageService.isJavaProject(nonExistent);

      assertFalse(result);
    }

    @Test
    @DisplayName("should return true when multiple project indicators exist")
    void isJavaProject_withMultipleIndicators_shouldReturnTrue(@TempDir Path tempDir)
        throws IOException {
      Files.createFile(tempDir.resolve("pom.xml"));
      Files.createFile(tempDir.resolve("build.gradle"));
      Path srcMainJava = tempDir.resolve("src/main/java");
      Files.createDirectories(srcMainJava);

      boolean result = JavaLanguageServiceTest.this.javaLanguageService.isJavaProject(tempDir);

      assertTrue(result);
    }
  }

  @Nested
  @DisplayName("getIdentifierType() Tests")
  class GetIdentifierTypeTests {
    private TSFile tsFile;

    @BeforeEach
    void setUp() {
      this.tsFile = new TSFile(SupportedLanguage.JAVA, CLASS_CODE);
    }

    /**
     * Tests that getIdentifierType correctly identifies class names.
     *
     * <p>Usage example:
     *
     * <pre>
     * TSNode classIdentifier = // get identifier node from class_declaration
     * JavaIdentifierType type = javaLanguageService.getIdentifierType(classIdentifier, SupportedIDE.NONE);
     * // Returns JavaIdentifierType.CLASS_NAME
     * </pre>
     */
    @Test
    @DisplayName("should identify class name identifier")
    void getIdentifierType_withClassIdentifier_shouldReturnClassName() {
      TSNode classNode =
          this.tsFile
              .query("(class_declaration name: (identifier) @class_name)")
              .returning("class_name")
              .execute()
              .firstNode();

      JavaIdentifierType result =
          JavaLanguageServiceTest.this.javaLanguageService.getIdentifierType(
              classNode, SupportedIDE.NONE);

      assertEquals(JavaIdentifierType.CLASS_NAME, result);
    }

    /**
     * Tests that getIdentifierType correctly identifies method names.
     *
     * <p>Usage example:
     *
     * <pre>
     * TSNode methodIdentifier = // get identifier node from method_declaration
     * JavaIdentifierType type = javaLanguageService.getIdentifierType(methodIdentifier, SupportedIDE.NONE);
     * // Returns JavaIdentifierType.METHOD_NAME
     * </pre>
     */
    @Test
    @DisplayName("should identify method name identifier")
    void getIdentifierType_withMethodIdentifier_shouldReturnMethodName() {
      TSNode methodNode =
          this.tsFile
              .query("(method_declaration name: (identifier) @method_name)")
              .returning("method_name")
              .execute()
              .firstNode();

      JavaIdentifierType result =
          JavaLanguageServiceTest.this.javaLanguageService.getIdentifierType(
              methodNode, SupportedIDE.NONE);

      assertEquals(JavaIdentifierType.METHOD_NAME, result);
    }

    /**
     * Tests that getIdentifierType correctly identifies formal parameter names.
     *
     * <p>Usage example:
     *
     * <pre>
     * TSNode paramIdentifier = // get identifier node from formal_parameter
     * JavaIdentifierType type = javaLanguageService.getIdentifierType(paramIdentifier, SupportedIDE.NONE);
     * // Returns JavaIdentifierType.FORMAL_PARAMETER_NAME
     * </pre>
     */
    @Test
    @DisplayName("should identify formal parameter identifier")
    void getIdentifierType_withFormalParameterIdentifier_shouldReturnFormalParameterName() {
      TSNode paramNode =
          this.tsFile
              .query("(formal_parameter name: (identifier) @param_name)")
              .returning("param_name")
              .execute()
              .firstNode();

      JavaIdentifierType result =
          JavaLanguageServiceTest.this.javaLanguageService.getIdentifierType(
              paramNode, SupportedIDE.NONE);

      assertEquals(JavaIdentifierType.FORMAL_PARAMETER_NAME, result);
    }

    /**
     * Tests that getIdentifierType correctly identifies field names.
     *
     * <p>Usage example:
     *
     * <pre>
     * TSNode fieldIdentifier = // get identifier node from field_declaration variable_declarator
     * JavaIdentifierType type = javaLanguageService.getIdentifierType(fieldIdentifier, SupportedIDE.NONE);
     * // Returns JavaIdentifierType.FIELD_NAME
     * </pre>
     */
    @Test
    @DisplayName("should identify field name identifier")
    void getIdentifierType_withFieldIdentifier_shouldReturnFieldName() {
      TSNode fieldNode =
          this.tsFile
              .query(
                  "(field_declaration declarator: (variable_declarator name: (identifier)"
                      + " @field_name))")
              .returning("field_name")
              .execute()
              .firstNode();

      JavaIdentifierType result =
          JavaLanguageServiceTest.this.javaLanguageService.getIdentifierType(
              fieldNode, SupportedIDE.NONE);

      assertEquals(JavaIdentifierType.FIELD_NAME, result);
    }

    /**
     * Tests that getIdentifierType correctly identifies local variable names.
     *
     * <p>Usage example:
     *
     * <pre>
     * TSNode localVarIdentifier = // get identifier node from local variable_declarator
     * JavaIdentifierType type = javaLanguageService.getIdentifierType(localVarIdentifier, SupportedIDE.NONE);
     * // Returns JavaIdentifierType.LOCAL_VARIABLE_NAME
     * </pre>
     */
    @Test
    @DisplayName("should identify local variable identifier")
    void getIdentifierType_withLocalVariableIdentifier_shouldReturnLocalVariableName() {
      TSNode localVarNode =
          this.tsFile
              .query(
                  "(local_variable_declaration declarator: (variable_declarator name: (identifier)"
                      + " @local_var))")
              .returning("local_var")
              .execute()
              .firstNode();

      JavaIdentifierType result =
          JavaLanguageServiceTest.this.javaLanguageService.getIdentifierType(
              localVarNode, SupportedIDE.NONE);

      assertEquals(JavaIdentifierType.LOCAL_VARIABLE_NAME, result);
    }

    @Test
    @DisplayName("should return null for non-identifier node")
    void getIdentifierType_withNonIdentifierNode_shouldReturnNull() {
      TSNode classDeclarationNode =
          this.tsFile.query("(class_declaration) @class").returning("class").execute().firstNode();

      JavaIdentifierType result =
          JavaLanguageServiceTest.this.javaLanguageService.getIdentifierType(
              classDeclarationNode, SupportedIDE.NONE);

      assertNull(result);
    }

    @Test
    @DisplayName("should return null for identifier with no parent")
    void getIdentifierType_withNoParent_shouldReturnNull() {
      TSNode mockIdentifier =
          new TSNode() {
            @Override
            public String getType() {
              return "identifier";
            }

            @Override
            public TSNode getParent() {
              return null;
            }

            @Override
            public int getChildCount() {
              return 0;
            }

            @Override
            public TSNode getChild(int i) {
              return null;
            }

            @Override
            public TSNode getNamedChild(int i) {
              return null;
            }

            @Override
            public int getNamedChildCount() {
              return 0;
            }

            @Override
            public TSNode getNextSibling() {
              return null;
            }

            @Override
            public TSNode getNextNamedSibling() {
              return null;
            }

            @Override
            public TSNode getPrevSibling() {
              return null;
            }

            @Override
            public TSNode getPrevNamedSibling() {
              return null;
            }

            @Override
            public int getStartByte() {
              return 0;
            }

            @Override
            public int getEndByte() {
              return 0;
            }

            @Override
            public boolean isNull() {
              return false;
            }

            @Override
            public boolean isMissing() {
              return false;
            }

            @Override
            public boolean isExtra() {
              return false;
            }

            @Override
            public boolean hasChanges() {
              return false;
            }

            @Override
            public boolean hasError() {
              return false;
            }

            @Override
            public boolean isNamed() {
              return true;
            }

            @Override
            public String toString() {
              return "mock";
            }
          };

      JavaIdentifierType result =
          JavaLanguageServiceTest.this.javaLanguageService.getIdentifierType(
              mockIdentifier, SupportedIDE.NONE);

      assertNull(result);
    }

    @Test
    @DisplayName("should return null for unknown parent type")
    void getIdentifierType_withUnknownParentType_shouldReturnNull() {
      TSNode mockIdentifier =
          new TSNode() {
            @Override
            public String getType() {
              return "identifier";
            }

            @Override
            public TSNode getParent() {
              return new TSNode() {
                @Override
                public String getType() {
                  return "unknown_node_type";
                }

                @Override
                public TSNode getParent() {
                  return null;
                }

                @Override
                public int getChildCount() {
                  return 0;
                }

                @Override
                public TSNode getChild(int i) {
                  return null;
                }

                @Override
                public TSNode getNamedChild(int i) {
                  return null;
                }

                @Override
                public int getNamedChildCount() {
                  return 0;
                }

                @Override
                public TSNode getNextSibling() {
                  return null;
                }

                @Override
                public TSNode getNextNamedSibling() {
                  return null;
                }

                @Override
                public TSNode getPrevSibling() {
                  return null;
                }

                @Override
                public TSNode getPrevNamedSibling() {
                  return null;
                }

                @Override
                public int getStartByte() {
                  return 0;
                }

                @Override
                public int getEndByte() {
                  return 0;
                }

                @Override
                public boolean isNull() {
                  return false;
                }

                @Override
                public boolean isMissing() {
                  return false;
                }

                @Override
                public boolean isExtra() {
                  return false;
                }

                @Override
                public boolean hasChanges() {
                  return false;
                }

                @Override
                public boolean hasError() {
                  return false;
                }

                @Override
                public boolean isNamed() {
                  return true;
                }

                @Override
                public String toString() {
                  return "unknown";
                }
              };
            }

            @Override
            public int getChildCount() {
              return 0;
            }

            @Override
            public TSNode getChild(int i) {
              return null;
            }

            @Override
            public TSNode getNamedChild(int i) {
              return null;
            }

            @Override
            public int getNamedChildCount() {
              return 0;
            }

            @Override
            public TSNode getNextSibling() {
              return null;
            }

            @Override
            public TSNode getNextNamedSibling() {
              return null;
            }

            @Override
            public TSNode getPrevSibling() {
              return null;
            }

            @Override
            public TSNode getPrevNamedSibling() {
              return null;
            }

            @Override
            public int getStartByte() {
              return 0;
            }

            @Override
            public int getEndByte() {
              return 0;
            }

            @Override
            public boolean isNull() {
              return false;
            }

            @Override
            public boolean isMissing() {
              return false;
            }

            @Override
            public boolean isExtra() {
              return false;
            }

            @Override
            public boolean hasChanges() {
              return false;
            }

            @Override
            public boolean hasError() {
              return false;
            }

            @Override
            public boolean isNamed() {
              return true;
            }

            @Override
            public String toString() {
              return "identifier";
            }
          };

      JavaIdentifierType result =
          JavaLanguageServiceTest.this.javaLanguageService.getIdentifierType(
              mockIdentifier, SupportedIDE.NONE);

      assertNull(result);
    }

    @Test
    @DisplayName("should handle variable_declarator without field_declaration parent")
    void getIdentifierType_withVariableDeclaratorNonFieldParent_shouldReturnLocalVariableName() {
      TSNode localVarNode =
          this.tsFile
              .query(
                  "(local_variable_declaration declarator: (variable_declarator name: (identifier)"
                      + " @local_var))")
              .returning("local_var")
              .execute()
              .firstNode();

      JavaIdentifierType result =
          JavaLanguageServiceTest.this.javaLanguageService.getIdentifierType(
              localVarNode, SupportedIDE.NONE);

      assertEquals(JavaIdentifierType.LOCAL_VARIABLE_NAME, result);
    }
  }

  @Nested
  @DisplayName("Service Dependencies Tests")
  class ServiceDependenciesTests {

    @Test
    @DisplayName("should have all required dependencies initialized")
    void constructor_shouldInitializeAllDependencies() {
      assertNotNull(JavaLanguageServiceTest.this.javaLanguageService.getPathHelper());
      assertNotNull(JavaLanguageServiceTest.this.javaLanguageService.getVariableNamingService());
      assertNotNull(JavaLanguageServiceTest.this.javaLanguageService.getClassDeclarationService());
      assertNotNull(
          JavaLanguageServiceTest.this.javaLanguageService.getPackageDeclarationService());
      assertNotNull(JavaLanguageServiceTest.this.javaLanguageService.getImportDeclarationService());
      assertNotNull(
          JavaLanguageServiceTest.this.javaLanguageService.getLocalVariableDeclarationService());
    }

    @Test
    @DisplayName("should return correct dependency instances")
    void getDependencies_shouldReturnCorrectInstances() {
      assertEquals(
          JavaLanguageServiceTest.this.pathHelper,
          JavaLanguageServiceTest.this.javaLanguageService.getPathHelper());
      assertTrue(
          JavaLanguageServiceTest.this.javaLanguageService.getVariableNamingService()
              instanceof VariableNamingService);
      assertTrue(
          JavaLanguageServiceTest.this.javaLanguageService.getClassDeclarationService()
              instanceof ClassDeclarationService);
      assertTrue(
          JavaLanguageServiceTest.this.javaLanguageService.getPackageDeclarationService()
              instanceof PackageDeclarationService);
      assertTrue(
          JavaLanguageServiceTest.this.javaLanguageService.getImportDeclarationService()
              instanceof ImportDeclarationService);
      assertTrue(
          JavaLanguageServiceTest.this.javaLanguageService.getLocalVariableDeclarationService()
              instanceof LocalVariableDeclarationService);
    }
  }
}
