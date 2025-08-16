package io.github.syntaxpresso.core.command.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.syntaxpresso.core.command.java.dto.CreateNewJavaFileResponse;
import io.github.syntaxpresso.core.command.java.extra.SourceDirectoryType;
import io.github.syntaxpresso.core.common.DataTransferObject;
import io.github.syntaxpresso.core.service.java.JavaService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

@DisplayName("CreateNewFileTest Tests")
class CreateNewFileTest {
  private JavaService javaService;
  private CommandLine cmd;
  @TempDir Path tempDir;

  @BeforeEach
  void setUp() {
    javaService = mock(JavaService.class);
    cmd = new CommandLine(new CreateNewFileCommand(javaService));
  }

  @Nested
  @DisplayName("Argument Tests")
  class ArgumentTests {
    @Test
    @DisplayName("should fail when --cwd is not provided")
    void execute_withoutCwd_shouldThrowException() {
      String[] args = {
        "--package-name", "io.github", "--file-name", "MyClass.java", "--file-type", "CLASS"
      };
      assertThrows(CommandLine.MissingParameterException.class, () -> cmd.parseArgs(args));
    }

    @Test
    @DisplayName("should fail when --cwd does not exist")
    void execute_withNonExistentCwd_shouldThrowException() {
      String[] args = {
        "--cwd",
        "/invalid/path",
        "--package-name",
        "io.github",
        "--file-name",
        "MyClass.java",
        "--file-type",
        "CLASS"
      };
      int exitCode = cmd.execute(args);
      assertEquals(1, exitCode);
    }

    @Test
    @DisplayName("should fail when --packageName is empty")
    void execute_withEmptyPackageName_shouldThrowException() {
      String[] args = {
        "--cwd",
        tempDir.toString(),
        "--package-name",
        "",
        "--file-name",
        "MyClass.java",
        "--file-type",
        "CLASS"
      };
      int exitCode = cmd.execute(args);
      assertEquals(1, exitCode);
    }

    @Test
    @DisplayName("should fail when --fileName is empty")
    void execute_withEmptyFileName_shouldThrowException() {
      String[] args = {
        "--cwd",
        tempDir.toString(),
        "--package-name",
        "io.github",
        "--file-name",
        "",
        "--file-type",
        "CLASS"
      };
      int exitCode = cmd.execute(args);
      assertEquals(1, exitCode);
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
      when(javaService.findFilePath(any(), eq("io.github"), eq(SourceDirectoryType.MAIN)))
          .thenReturn(Optional.of(targetPath));
      String[] args = {
        "--cwd",
        tempDir.toString(),
        "--package-name",
        "io.github",
        "--file-name",
        "MyClass.java",
        "--file-type",
        "CLASS",
        "--source-directory-type",
        "MAIN"
      };
      cmd.execute(args);
      DataTransferObject<CreateNewJavaFileResponse> result = cmd.getExecutionResult();
      assertTrue(result.getSucceed());
      assertNotNull(result.getData());
      assertTrue(result.getData().getFilePath().endsWith("MyClass.java"));
      assertTrue(Files.exists(Path.of(result.getData().getFilePath())));
    }

    @Test
    @DisplayName("should return error if path not found by JavaService")
    void call_shouldReturnErrorIfFilePathNotFound() {
      when(javaService.findFilePath(any(), any(), any())).thenReturn(Optional.empty());
      String[] args = {
        "--cwd",
        tempDir.toString(),
        "--package-name",
        "io.github",
        "--file-name",
        "MyClass.java",
        "--file-type",
        "CLASS",
        "--source-directory-type",
        "MAIN"
      };
      cmd.execute(args);
      DataTransferObject<CreateNewJavaFileResponse> result = cmd.getExecutionResult();
      assertFalse(result.getSucceed());
      assertEquals("Package name couldn't be determined.", result.getErrorReason());
    }
  }
}
