package io.github.syntaxpresso.core.service.java.language;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.treesitter.TSNode;

@DisplayName("ProgramService Tests")
class ProgramServiceTest {
  private ProgramService programService;
  private TSFile testFile;

  @BeforeEach
  void setUp() {
    VariableNamingService variableNamingService = new VariableNamingService();
    FieldDeclarationService fieldDeclarationService = new FieldDeclarationService();
    ImportDeclarationService importDeclarationService = new ImportDeclarationService();
    PackageDeclarationService packageDeclarationService = new PackageDeclarationService();
    LocalVariableDeclarationService localVariableDeclarationService =
        new LocalVariableDeclarationService(variableNamingService);
    FormalParameterService formalParameterService =
        new FormalParameterService(localVariableDeclarationService, variableNamingService);
    ClassDeclarationService classDeclarationService =
        new ClassDeclarationService(fieldDeclarationService);
    TypeResolutionService typeResolutionService =
        new TypeResolutionService(
            formalParameterService,
            localVariableDeclarationService,
            fieldDeclarationService,
            classDeclarationService);
    MethodDeclarationService methodDeclarationService =
        new MethodDeclarationService(
            formalParameterService, localVariableDeclarationService, typeResolutionService);
    programService =
        new ProgramService(
            variableNamingService,
            fieldDeclarationService,
            importDeclarationService,
            packageDeclarationService,
            localVariableDeclarationService,
            formalParameterService,
            methodDeclarationService,
            classDeclarationService,
            typeResolutionService);
    String javaCode =
        """
        package io.github.syntaxpresso.core;
        import java.util.List;
        import java.nio.file.Path;
        @Data
        @Getter
        public class Test {
          private String value;
          private File file;
          @Override
          public void run(File name, List<File> files, int id) {
            File x = new File();
            boolean res = false;
            this.name = "ok";
            client.out.println(name);
            client.out.println(file);
          }
        }
        """;
    testFile = new TSFile(SupportedLanguage.JAVA, javaCode);
  }

  @Test
  @DisplayName("should get package name from program")
  void getPackageName_shouldReturnCorrectPackage() {
    Optional<String> packageName =
        programService.getPackageDeclarationService().getPackageName(testFile);
    assertTrue(packageName.isPresent());
    assertEquals("io.github.syntaxpresso.core", packageName.get());
  }

  @Test
  @DisplayName("should get all imports from program")
  void getAllImports_shouldReturnAllImports() {
    List<Map<String, TSNode>> imports =
        programService.getImportDeclarationService().getAllImportDeclarations(testFile);
    assertEquals(2, imports.size());
  }

  @Test
  @DisplayName("should get all classes from program")
  void getAllClasses_shouldReturnAllClasses() {
    List<TSNode> classes =
        programService.getClassDeclarationService().findAllClassDeclarations(testFile);
    assertEquals(1, classes.size());
  }

  @Test
  @DisplayName("should find class by name")
  void findClassByName_shouldReturnCorrectClass() {
    Optional<TSNode> classNode =
        programService.getClassDeclarationService().findClassByName(testFile, "Test");
    assertTrue(classNode.isPresent());
  }

  @Test
  @DisplayName("should check if program has main class")
  void hasMainClass_shouldReturnFalse() {
    assertFalse(programService.hasMainClass(testFile));
  }

  @Test
  @DisplayName("should get all fields from program")
  void getAllFields_shouldReturnAllFields() {
    List<TSNode> fields = testFile.query("(field_declaration) @field").execute();
    assertEquals(2, fields.size());
  }

  @Test
  @DisplayName("should get all methods from program")
  void getAllMethods_shouldReturnAllMethods() {
    List<TSNode> methods =
        programService.getMethodDeclarationService().findAllMethodDeclarations(testFile);
    assertEquals(1, methods.size());
  }
}
