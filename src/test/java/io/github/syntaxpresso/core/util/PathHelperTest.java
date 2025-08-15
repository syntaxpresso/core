package io.github.syntaxpresso.core.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("PathHelper Tests")
class PathHelperTest {
  private PathHelper pathHelper;

  @BeforeEach
  void setUp() {
    this.pathHelper = new PathHelper();
  }

  @Nested
  @DisplayName("findFilesByExtention()")
  class FindFilesByExtentionTests {
    @Test
    @DisplayName("should find all files with a given extension")
    void findFilesByExtention_shouldReturnMatchingFiles(@TempDir Path tempDir) throws IOException {
      Files.createFile(tempDir.resolve("test1.java"));
      Files.createFile(tempDir.resolve("test2.java"));
      Files.createFile(tempDir.resolve("test.txt"));
      Path subDir = tempDir.resolve("subdir");
      Files.createDirectory(subDir);
      Files.createFile(subDir.resolve("test3.java"));
      List<TSFile> javaFiles = pathHelper.findFilesByExtention(tempDir, SupportedLanguage.JAVA);
      assertEquals(3, javaFiles.size());
      assertTrue(
          javaFiles.stream()
              .allMatch(
                  f -> f.getFile().getName().endsWith(SupportedLanguage.JAVA.getFileExtension())));
    }

    @Test
    @DisplayName("should return empty list when no files match")
    void findFilesByExtention_whenNoMatches_shouldReturnEmptyList(@TempDir Path tempDir)
        throws IOException {
      Files.createFile(tempDir.resolve("test.txt"));
      List<TSFile> javaFiles = pathHelper.findFilesByExtention(tempDir, SupportedLanguage.JAVA);
      assertTrue(javaFiles.isEmpty());
    }
  }

  @Nested
  @DisplayName("findDirectoryRecursively()")
  class FindDirectoryRecursivelyTests {
    @Test
    @DisplayName("should find an existing directory")
    void findDirectory_whenDirectoryExists_shouldReturnPath(@TempDir Path tempDir)
        throws IOException {
      Path subDir = tempDir.resolve("my_dir");
      Files.createDirectory(subDir);
      Optional<Path> foundDir = pathHelper.findDirectoryRecursively(tempDir, "my_dir");
      assertTrue(foundDir.isPresent());
      assertEquals(subDir, foundDir.get());
    }

    @Test
    @DisplayName("should return empty for a non-existent directory")
    void findDirectory_whenDirectoryDoesNotExist_shouldReturnEmpty(@TempDir Path tempDir)
        throws IOException {
      Optional<Path> foundDir = pathHelper.findDirectoryRecursively(tempDir, "non_existent_dir");
      assertFalse(foundDir.isPresent());
    }

    @Test
    @DisplayName("should find a deeply nested directory")
    void findDirectoryRecursively_whenDirIsNested_shouldReturnPath(@TempDir Path tempDir)
        throws IOException {
      Path level1 = tempDir.resolve("level1");
      Path level2 = level1.resolve("level2");
      Path deepDir = level2.resolve("deepDir");
      Files.createDirectories(deepDir);
      Optional<Path> foundDir = pathHelper.findDirectoryRecursively(tempDir, "deepDir");
      assertTrue(foundDir.isPresent());
      assertEquals(deepDir, foundDir.get());
    }

    @Test
    @DisplayName("should find a multi-level path like 'src/main/java'")
    void findDirectoryRecursively_withMultiLevelPath_shouldReturnPath(@TempDir Path tempDir)
        throws IOException {
      Path targetPath = tempDir.resolve("app/src/main/java");
      Files.createDirectories(targetPath);
      Optional<Path> foundDir = pathHelper.findDirectoryRecursively(tempDir, "src/main/java");
      assertTrue(foundDir.isPresent());
      assertEquals(targetPath, foundDir.get());
    }
  }

  @Nested
  @DisplayName("renameDirectory()")
  class RenameDirectoryTests {
    @Test
    @DisplayName("should rename a directory")
    void renameDirectory_shouldSucceed(@TempDir Path tempDir) throws IOException {
      Path sourceDir = tempDir.resolve("sourceDir");
      Files.createDirectory(sourceDir);
      Path destDir = tempDir.resolve("destDir");
      boolean result = pathHelper.renameDirectory(sourceDir, destDir);
      assertTrue(result);
      assertFalse(Files.exists(sourceDir));
      assertTrue(Files.exists(destDir));
    }
  }
}
