package io.github.syntaxpresso.core.service.java.extra;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ImportDeclarationService Tests")
class ImportDeclarationServiceTest {

  private ImportDeclarationService importService;

  @BeforeEach
  void setUp() {
    importService = new ImportDeclarationService();
  }

  @Nested
  @DisplayName("addImport(TSFile, String, String)")
  class AddImportWithPackageAndClassTests {

    @Test
    @DisplayName("should add import to file with package declaration")
    void addImport_withPackageDeclaration_shouldAddImportAfterPackage() {
      String sourceCode = """
          package com.example;

          public class MyClass {
          }
          """;
      TSFile file = new TSFile(SupportedLanguage.JAVA, sourceCode);
      
      importService.addImport(file, "java.util", "List");
      
      String expectedCode = """
          package com.example;

          import java.util.List;

          public class MyClass {
          }
          """;
      
      assertEquals(expectedCode, file.getSourceCode());
    }

    @Test
    @DisplayName("should add import to file without package declaration")
    void addImport_withoutPackageDeclaration_shouldAddImportAtStart() {
      String sourceCode = """
          public class MyClass {
          }
          """;
      TSFile file = new TSFile(SupportedLanguage.JAVA, sourceCode);
      
      importService.addImport(file, "java.util", "List");
      
      String expectedCode = """
          import java.util.List;

          public class MyClass {
          }
          """;
      assertEquals(expectedCode, file.getSourceCode());
    }

    @Test
    @DisplayName("should add import after existing imports")
    void addImport_withExistingImports_shouldAddAfterLastImport() {
      String sourceCode = """
          package com.example;
          import java.util.Map;

          public class MyClass {
          }
          """;
      TSFile file = new TSFile(SupportedLanguage.JAVA, sourceCode);
      
      importService.addImport(file, "java.util", "List");
      
      String expectedCode = """
          package com.example;
          import java.util.Map;
          import java.util.List;

          public class MyClass {
          }
          """;
      assertEquals(expectedCode, file.getSourceCode());
    }

    @Test
    @DisplayName("should not add duplicate import")
    void addImport_withDuplicateImport_shouldNotAddDuplicate() {
      String sourceCode = """
          package com.example;
          import java.util.List;

          public class MyClass {
          }
          """;
      TSFile file = new TSFile(SupportedLanguage.JAVA, sourceCode);
      
      importService.addImport(file, "java.util", "List");
      
      String expectedCode = """
          package com.example;
          import java.util.List;

          public class MyClass {
          }
          """;
      assertEquals(expectedCode, file.getSourceCode());
    }

    @Test
    @DisplayName("should not add specific import when wildcard exists")
    void addImport_whenWildcardExists_shouldNotAddSpecificImport() {
      String sourceCode = """
          package com.example;
          import java.util.*;

          public class MyClass {
          }
          """;
      TSFile file = new TSFile(SupportedLanguage.JAVA, sourceCode);
      
      importService.addImport(file, "java.util", "List");
      
      String expectedCode = """
          package com.example;
          import java.util.*;

          public class MyClass {
          }
          """;
      assertEquals(expectedCode, file.getSourceCode());
    }
  }

  @Nested
  @DisplayName("addImport(TSFile, String)")
  class AddImportWithFullPackageNameTests {

    @Test
    @DisplayName("should add import using full package name")
    void addImport_withFullPackageName_shouldAddImport() {
      String sourceCode = """
          package com.example;

          public class MyClass {
          }
          """;
      TSFile file = new TSFile(SupportedLanguage.JAVA, sourceCode);
      
      importService.addImport(file, "java.util.List");
      
      String expectedCode = """
          package com.example;

          import java.util.List;

          public class MyClass {
          }
          """;
      assertEquals(expectedCode, file.getSourceCode());
    }

    @Test
    @DisplayName("should throw exception for invalid full package name")
    void addImport_withInvalidFullPackageName_shouldThrowException() {
      String sourceCode = """
          package com.example;

          public class MyClass {
          }
          """;
      TSFile file = new TSFile(SupportedLanguage.JAVA, sourceCode);
      
      assertThrows(IllegalArgumentException.class, () -> {
        importService.addImport(file, "InvalidPackageName");
      });
    }

    @Test
    @DisplayName("should not add duplicate using full package name")
    void addImport_withDuplicateFullPackageName_shouldNotAddDuplicate() {
      String sourceCode = """
          package com.example;
          import java.util.List;

          public class MyClass {
          }
          """;
      TSFile file = new TSFile(SupportedLanguage.JAVA, sourceCode);
      
      importService.addImport(file, "java.util.List");
      
      String expectedCode = """
          package com.example;
          import java.util.List;

          public class MyClass {
          }
          """;
      assertEquals(expectedCode, file.getSourceCode());
    }
  }

  @Nested
  @DisplayName("addImportWildcard(TSFile, String)")
  class AddImportWildcardTests {

    @Test
    @DisplayName("should add wildcard import")
    void addImportWildcard_shouldAddWildcardImport() {
      String sourceCode = """
          package com.example;

          public class MyClass {
          }
          """;
      TSFile file = new TSFile(SupportedLanguage.JAVA, sourceCode);
      
      importService.addImportWildcard(file, "java.util");
      
      String expectedCode = """
          package com.example;

          import java.util.*;

          public class MyClass {
          }
          """;
      assertEquals(expectedCode, file.getSourceCode());
    }

    @Test
    @DisplayName("should not add duplicate wildcard import")
    void addImportWildcard_withDuplicateWildcard_shouldNotAddDuplicate() {
      String sourceCode = """
          package com.example;
          import java.util.*;

          public class MyClass {
          }
          """;
      TSFile file = new TSFile(SupportedLanguage.JAVA, sourceCode);
      
      importService.addImportWildcard(file, "java.util");
      
      String expectedCode = """
          package com.example;
          import java.util.*;

          public class MyClass {
          }
          """;
      assertEquals(expectedCode, file.getSourceCode());
    }

    @Test
    @DisplayName("should add wildcard import to empty file")
    void addImportWildcard_toEmptyFile_shouldAddAtStart() {
      String sourceCode = """
          public class MyClass {
          }
          """;
      TSFile file = new TSFile(SupportedLanguage.JAVA, sourceCode);
      
      importService.addImportWildcard(file, "java.util");
      
      String expectedCode = """
          import java.util.*;

          public class MyClass {
          }
          """;
      assertEquals(expectedCode, file.getSourceCode());
    }
  }

  @Nested
  @DisplayName("updateImport(TSFile, String, String)")
  class UpdateImportTests {

    @Test
    @DisplayName("should update existing specific import")
    void updateImport_withExistingSpecificImport_shouldUpdate() {
      String sourceCode = """
          package com.example;
          import org.example.Test;

          public class MyClass {
          }
          """;
      TSFile file = new TSFile(SupportedLanguage.JAVA, sourceCode);
      
      boolean result = importService.updateImport(file, "org.example.Test", "org.example.NewName");
      
      String expectedCode = """
          package com.example;
          import org.example.NewName;

          public class MyClass {
          }
          """;
      assertTrue(result);
      assertEquals(expectedCode, file.getSourceCode());
    }

    @Test
    @DisplayName("should not update when wildcard import exists")
    void updateImport_withWildcardImportExists_shouldNotUpdate() {
      String sourceCode = """
          package com.example;
          import org.example.*;

          public class MyClass {
          }
          """;
      TSFile file = new TSFile(SupportedLanguage.JAVA, sourceCode);
      
      boolean result = importService.updateImport(file, "org.example.Test", "org.example.NewName");
      
      String expectedCode = """
          package com.example;
          import org.example.*;

          public class MyClass {
          }
          """;
      assertFalse(result);
      assertEquals(expectedCode, file.getSourceCode());
    }

    @Test
    @DisplayName("should return false when import does not exist")
    void updateImport_withNonExistentImport_shouldReturnFalse() {
      String sourceCode = """
          package com.example;
          import java.util.List;

          public class MyClass {
          }
          """;
      TSFile file = new TSFile(SupportedLanguage.JAVA, sourceCode);
      
      boolean result = importService.updateImport(file, "org.example.Test", "org.example.NewName");
      
      String expectedCode = """
          package com.example;
          import java.util.List;

          public class MyClass {
          }
          """;
      assertFalse(result);
      assertEquals(expectedCode, file.getSourceCode());
    }

    @Test
    @DisplayName("should update import even when changing package")
    void updateImport_changingPackage_shouldUpdate() {
      String sourceCode = """
          package com.example;
          import org.example.Test;

          public class MyClass {
          }
          """;
      TSFile file = new TSFile(SupportedLanguage.JAVA, sourceCode);
      
      boolean result = importService.updateImport(file, "org.example.Test", "com.newpackage.Test");
      
      String expectedCode = """
          package com.example;
          import com.newpackage.Test;

          public class MyClass {
          }
          """;
      assertTrue(result);
      assertEquals(expectedCode, file.getSourceCode());
    }

    @Test
    @DisplayName("should throw exception for invalid old import")
    void updateImport_withInvalidOldImport_shouldThrowException() {
      String sourceCode = """
          package com.example;
          import org.example.Test;

          public class MyClass {
          }
          """;
      TSFile file = new TSFile(SupportedLanguage.JAVA, sourceCode);
      
      assertThrows(IllegalArgumentException.class, () -> {
        importService.updateImport(file, "InvalidImport", "org.example.NewName");
      });
    }

    @Test
    @DisplayName("should throw exception for invalid new import")
    void updateImport_withInvalidNewImport_shouldThrowException() {
      String sourceCode = """
          package com.example;
          import org.example.Test;

          public class MyClass {
          }
          """;
      TSFile file = new TSFile(SupportedLanguage.JAVA, sourceCode);
      
      assertThrows(IllegalArgumentException.class, () -> {
        importService.updateImport(file, "org.example.Test", "InvalidImport");
      });
    }

    @Test
    @DisplayName("should update import among multiple imports")
    void updateImport_withMultipleImports_shouldUpdateCorrectOne() {
      String sourceCode = """
          package com.example;
          import java.util.List;
          import org.example.Test;
          import java.io.IOException;

          public class MyClass {
          }
          """;
      TSFile file = new TSFile(SupportedLanguage.JAVA, sourceCode);
      
      boolean result = importService.updateImport(file, "org.example.Test", "org.example.NewName");
      
      String expectedCode = """
          package com.example;
          import java.util.List;
          import org.example.NewName;
          import java.io.IOException;

          public class MyClass {
          }
          """;
      assertTrue(result);
      assertEquals(expectedCode, file.getSourceCode());
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("should handle complex file with multiple imports correctly")
    void addImport_complexFileWithMultipleImports_shouldInsertCorrectly() {
      String sourceCode = """
          package com.example.project;
          
          import java.io.IOException;
          import java.util.List;
          import org.junit.jupiter.api.Test;

          public class MyClass {
              public void method() throws IOException {
                  List<String> list = new ArrayList<>();
              }
          }
          """;
      TSFile file = new TSFile(SupportedLanguage.JAVA, sourceCode);
      
      importService.addImport(file, "java.util", "ArrayList");
      
      assertTrue(file.getSourceCode().contains("import java.util.ArrayList;"));
      assertTrue(file.getSourceCode().contains("import java.util.List;"));
      assertTrue(file.getSourceCode().contains("import java.io.IOException;"));
      assertTrue(file.getSourceCode().contains("import org.junit.jupiter.api.Test;"));
    }

    @Test
    @DisplayName("should maintain proper ordering when adding imports")
    void addImport_shouldMaintainProperOrdering() {
      String sourceCode = """
          package com.example;
          import java.util.Map;

          public class MyClass {
          }
          """;
      TSFile file = new TSFile(SupportedLanguage.JAVA, sourceCode);
      
      importService.addImport(file, "java.util", "List");
      importService.addImport(file, "java.io", "IOException");
      
      String result = file.getSourceCode();
      int mapIndex = result.indexOf("import java.util.Map;");
      int listIndex = result.indexOf("import java.util.List;");
      int ioIndex = result.indexOf("import java.io.IOException;");
      
      assertTrue(mapIndex < listIndex);
      assertTrue(listIndex < ioIndex);
    }
  }
}