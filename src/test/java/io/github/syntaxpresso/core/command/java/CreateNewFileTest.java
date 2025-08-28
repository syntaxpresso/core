package io.github.syntaxpresso.core.command.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.syntaxpresso.core.command.dto.CreateNewFileResponse;
import io.github.syntaxpresso.core.command.extra.JavaFileTemplate;
import io.github.syntaxpresso.core.command.extra.SourceDirectoryType;
import io.github.syntaxpresso.core.common.DataTransferObject;
import io.github.syntaxpresso.core.service.java.JavaCommandService;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("CreateNewFileTest Tests")
class CreateNewFileTest {
  private JavaCommandService javaService;
  @TempDir Path tempDir;

  @BeforeEach
  void setUp() {
    javaService = new JavaCommandService();
  }

  @Nested
  @DisplayName("Argument Tests")
  class ArgumentTests {
    @Test
    @DisplayName("should fail when --cwd does not exist")
    void execute_withNonExistentCwd_shouldReturnError() {
      DataTransferObject<CreateNewFileResponse> result =
          javaService.createNewFile(
              Path.of("/invalid/path"),
              "io.github",
              "MyClass.java",
              JavaFileTemplate.CLASS,
              SourceDirectoryType.MAIN);
      assertFalse(result.getSucceed());
      assertEquals("Current working directory does not exist.", result.getErrorReason());
    }

    @Test
    @DisplayName("should fail when --packageName is empty")
    void execute_withEmptyPackageName_shouldReturnError() {
      DataTransferObject<CreateNewFileResponse> result =
          javaService.createNewFile(
              tempDir, "", "MyClass.java", JavaFileTemplate.CLASS, SourceDirectoryType.MAIN);
      assertFalse(result.getSucceed());
      assertEquals("Package name invalid.", result.getErrorReason());
    }

    @Test
    @DisplayName("should fail when --fileName is empty")
    void execute_withEmptyFileName_shouldReturnError() {
      DataTransferObject<CreateNewFileResponse> result =
          javaService.createNewFile(
              tempDir, "io.github", "", JavaFileTemplate.CLASS, SourceDirectoryType.MAIN);
      assertFalse(result.getSucceed());
      assertEquals("File name invalid.", result.getErrorReason());
    }
  }

  @Nested
  @DisplayName("Execution Tests")
  class ExecutionTests {
    @Test
    @DisplayName("should create Java file successfully")
    void call_shouldCreateJavaFileSuccessfully() throws Exception {
      Path targetPath = tempDir.resolve("src/main/java/io/github");
      Files.createDirectories(targetPath);
      DataTransferObject<CreateNewFileResponse> result =
          javaService.createNewFile(
              tempDir,
              "io.github",
              "MyClass.java",
              JavaFileTemplate.CLASS,
              SourceDirectoryType.MAIN);
      assertTrue(result.getSucceed());
      assertNotNull(result.getData());
      assertTrue(result.getData().getFilePath().endsWith("MyClass.java"));
      assertTrue(Files.exists(Path.of(result.getData().getFilePath())));
    }

    @Test
    @DisplayName("should return error when source directory is missing")
    void shouldReturnErrorWhenSourceDirectoryIsMissing() {
      DataTransferObject<CreateNewFileResponse> result =
          javaService.createNewFile(
              tempDir,
              "io.github",
              "MyClass.java",
              JavaFileTemplate.CLASS,
              SourceDirectoryType.MAIN);
      assertFalse(result.getSucceed());
      assertEquals("Cannot find source directory.", result.getErrorReason());
    }
  }
}
