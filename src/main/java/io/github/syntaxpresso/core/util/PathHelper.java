package io.github.syntaxpresso.core.util;

import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PathHelper {
  /**
   * Recursively finds all files in a directory that match a given language's file extension and
   * converts them into a list of {@link TSFile} objects.
   *
   * @param rootDir The directory to start the search from.
   * @param supportedLanguage The language whose file extension will be used for filtering.
   * @return A {@link List} of {@link TSFile} objects. The list will be empty if no matching files
   *     are found.
   * @throws IOException if an I/O error occurs when walking the file tree.
   */
  public List<TSFile> findFilesByExtention(Path rootDir, SupportedLanguage supportedLanguage)
      throws IOException {
    List<TSFile> tsFiles = new ArrayList<>();
    try (Stream<Path> stream = Files.walk(rootDir)) {
      List<Path> filePaths =
          stream
              .filter(Files::isRegularFile)
              .filter(path -> path.toString().endsWith(supportedLanguage.getFileExtension()))
              .collect(Collectors.toList());
      for (Path filePath : filePaths) {
        tsFiles.add(new TSFile(supportedLanguage, filePath));
      }
    }
    return tsFiles;
  }

  /**
   * Recursively finds a directory by its name within a given root directory.
   *
   * @param rootDir The directory to start the search from.
   * @param dirName The name of the directory to find.
   * @return An {@link Optional} containing the {@link Path} of the found directory, or an empty
   *     Optional if not found.
   * @throws IOException if an I/O error occurs when walking the file tree.
   * @throws IllegalArgumentException if rootDir is not a valid directory or dirName is null or
   *     blank.
   */
  public Optional<Path> findDirectoryRecursively(Path rootDir, String dirName) throws IOException {
    if (rootDir == null || !Files.isDirectory(rootDir)) {
      throw new IllegalArgumentException(
          "The provided root path is not a valid directory: " + rootDir);
    }
    if (dirName == null || dirName.isBlank()) {
      throw new IllegalArgumentException("Directory name must not be null or blank.");
    }
    String normalizedDirName = dirName.replace('\\', '/');
    try (Stream<Path> stream = Files.walk(rootDir)) {
      return stream
          .filter(Files::isDirectory)
          .filter(path -> path.toString().replace('\\', '/').endsWith(normalizedDirName))
          .findFirst();
    }
  }

  /**
   * Renames or moves a directory. This operation is not atomic.
   *
   * @param source The source path of the directory.
   * @param destination The destination path.
   * @return {@code true} if the operation is successful, {@code false} if an I/O error occurs.
   */
  public boolean renameDirectory(Path source, Path destination) {
    try {
      Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
      return true;
    } catch (IOException e) {
      return false;
    }
  }
}
