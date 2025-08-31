// package io.github.syntaxpresso.core.command;
//
// import static org.junit.jupiter.api.Assertions.assertEquals;
// import static org.junit.jupiter.api.Assertions.assertFalse;
// import static org.junit.jupiter.api.Assertions.assertNotNull;
// import static org.junit.jupiter.api.Assertions.assertTrue;
//
// import io.github.syntaxpresso.core.command.dto.GetMainClassResponse;
// import io.github.syntaxpresso.core.common.DataTransferObject;
// import io.github.syntaxpresso.core.service.java.JavaCommandService;
// import java.io.IOException;
// import java.nio.file.Files;
// import java.nio.file.Path;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.DisplayName;
// import org.junit.jupiter.api.Nested;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.io.TempDir;
//
// @DisplayName("GetMainClassTest Tests")
// public class GetMainClassTest {
//   private JavaCommandService javaService;
//
//   @BeforeEach
//   void setUp() {
//     javaService = new JavaCommandService();
//   }
//
//   @Nested
//   @DisplayName("Argument Tests")
//   class ArgumentTests {
//     @Test
//     @DisplayName("should fail when --cwd does not exist")
//     void execute_withNonExistentCwd_shouldReturnError() {
//       DataTransferObject<GetMainClassResponse> result =
//           javaService.getMainClass(Path.of("non/existent/path"));
//       assertFalse(result.getSucceed());
//       assertEquals("Current working directory does not exist.", result.getErrorReason());
//     }
//   }
//
//   @Nested
//   @DisplayName("Execution Tests")
//   class ExecutionTests {
//     @Test
//     @DisplayName("should return success when a main class is found")
//     void call_whenMainClassNotFound_shouldReturnSuccess(@TempDir Path tempDir) throws IOException
// {
//       Path sourceDir = tempDir.resolve("src/main/java/com/example");
//       Files.createDirectories(sourceDir);
//       Path mainFile = sourceDir.resolve("Main.java");
//       Files.writeString(
//           mainFile,
//           "package com.example; public class Main { public static void main(String[] args) {}
// }");
//       DataTransferObject<GetMainClassResponse> result = javaService.getMainClass(tempDir);
//       assertTrue(result.getSucceed());
//       assertNotNull(result.getData());
//       assertEquals(mainFile.toAbsolutePath().toString(), result.getData().getFilePath());
//       assertEquals("com.example", result.getData().getPackageName());
//     }
//
//     @Test
//     @DisplayName("should return error when no main class is found")
//     void call_whenNoMainClassNotFound_shouldReturnError(@TempDir Path tempDir) {
//       DataTransferObject<GetMainClassResponse> result = javaService.getMainClass(tempDir);
//       assertFalse(result.getSucceed());
//       assertEquals(
//           "Main class couldn't be found in the current working directory.",
//           result.getErrorReason());
//     }
//
//     @Test
//     @DisplayName("should return error when main class has no package")
//     void call_whenMainClassHasNoPackage_shouldReturnError(@TempDir Path tempDir)
//         throws IOException {
//       Path sourceDir = tempDir.resolve("src/main/java");
//       Files.createDirectories(sourceDir);
//       Path mainFile = sourceDir.resolve("Main.java");
//       Files.writeString(
//           mainFile, "public class Main { public static void main(String[] args) {} }");
//       DataTransferObject<GetMainClassResponse> result = javaService.getMainClass(tempDir);
//       assertFalse(result.getSucceed());
//       assertEquals(
//           "Main class found, but package name couldn't be determined.", result.getErrorReason());
//     }
//   }
// }
