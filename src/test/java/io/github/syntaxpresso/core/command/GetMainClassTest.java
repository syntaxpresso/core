package io.github.syntaxpresso.core.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.syntaxpresso.core.command.java.GetMainClassCommand;
import io.github.syntaxpresso.core.command.java.dto.GetMainClassResponse;
import io.github.syntaxpresso.core.common.DataTransferObject;
import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.service.JavaService;
import io.github.syntaxpresso.core.util.PathHelper;
import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

@DisplayName("GetMainClassTest Tests")
public class GetMainClassTest {
    private JavaService javaService;
    private PathHelper pathHelper;
    private GetMainClassCommand command;
    private CommandLine cmd;

    @BeforeEach
    void setUp() {
        javaService = mock(JavaService.class);
        pathHelper = mock(PathHelper.class);
        when(javaService.getPathHelper()).thenReturn(pathHelper);
        command = new GetMainClassCommand(javaService);
        cmd = new CommandLine(command);
    }

    @Nested
    @DisplayName("Argument Tests")
    class ArgumentTests {

        @Test
        @DisplayName("should fail when --cwd is not provided")
        void execute_withoutCwd_shouldThrowException() {
            String[] args = {};
            assertThrows(CommandLine.MissingParameterException.class, () -> cmd.parseArgs(args));
        }

        @Test
        @DisplayName("should fail when --cwd does not exist")
        void execute_withNonExistentCwd_shouldThrowException() {
            String nonExistentPath = "non/existent/path";
            cmd.parseArgs("--cwd", nonExistentPath);
            assertThrows(
                    IllegalArgumentException.class,
                    () -> command.call(),
                    "Current working directory does not exist.");
        }
    }

    @Nested
    @DisplayName("Execution Tests")
    class ExecutionTests {

        @Test
        @DisplayName("should return success when a main class is found")
        void call_whenMainClassNotFound_shouldReturnSuccess(@TempDir Path tempDir) throws Exception {
            // Arrange
            TSFile mainClassFile = mock(TSFile.class);
            File file = tempDir.resolve("Main.java").toFile();
            when(mainClassFile.getFile()).thenReturn(file);
            when(pathHelper.findFilesByExtention(any(), any())).thenReturn(List.of(mainClassFile));
            when(javaService.isMainClass(mainClassFile)).thenReturn(true);
            when(javaService.getPackageName(mainClassFile)).thenReturn(Optional.of("com.example"));

            // Act
            cmd.parseArgs("--cwd", tempDir.toString());
            DataTransferObject<GetMainClassResponse> result = command.call();

            // Assert
            assertTrue(result.getSucceed());
            assertNotNull(result.getData());
            assertEquals(file.getAbsolutePath(), result.getData().getFilePath());
            assertEquals("com.example", result.getData().getPackageName());
        }

        @Test
        @DisplayName("should return error when no main class is found")
        void call_whenNoMainClassNotFound_shouldReturnError(@TempDir Path tempDir) throws Exception {
            // Arrange
            when(pathHelper.findFilesByExtention(any(), any())).thenReturn(Collections.emptyList());

            // Act
            cmd.parseArgs("--cwd", tempDir.toString());
            DataTransferObject<GetMainClassResponse> result = command.call();

            // Assert
            assertFalse(result.getSucceed());
            assertEquals(
                    "Main class couldn't be found in the current working directory.",
                    result.getErrorReason());
        }

        @Test
        @DisplayName("should return error when main class has no package")
        void call_whenMainClassHasNoPackage_shouldReturnError(@TempDir Path tempDir) throws Exception {
            // Arrange
            TSFile mainClassFile = mock(TSFile.class);
            when(mainClassFile.getFile()).thenReturn(tempDir.resolve("Main.java").toFile());
            when(pathHelper.findFilesByExtention(any(), any())).thenReturn(List.of(mainClassFile));
            when(javaService.isMainClass(mainClassFile)).thenReturn(true);
            when(javaService.getPackageName(mainClassFile)).thenReturn(Optional.empty());

            // Act
            cmd.parseArgs("--cwd", tempDir.toString());
            DataTransferObject<GetMainClassResponse> result = command.call();

            // Assert
            assertFalse(result.getSucceed());
            assertEquals(
                    "Main class found, but package name couldn't be determined.", result.getErrorReason());
        }
    }
}
