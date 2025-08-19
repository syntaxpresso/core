package io.github.syntaxpresso.core.command.java;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.syntaxpresso.core.command.dto.RenameResponse;
import io.github.syntaxpresso.core.common.DataTransferObject;
import io.github.syntaxpresso.core.service.java.JavaService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("RenameCommand Tests")
class RenameCommandTest {
  @TempDir Path tempDir;
  private JavaService javaService;

  @BeforeEach
  void setUp() {
    this.javaService = new JavaService();
  }

  @Test
  @DisplayName("Should rename a Java class and its file")
  void shouldRenameJavaClassAndFile() throws IOException {
    Path sourceFile = this.tempDir.resolve("Original.java");
    Files.writeString(sourceFile, "public class Original {}");
    DataTransferObject<RenameResponse> result = javaService.rename(sourceFile, "Renamed");
    assertThat(result.getSucceed()).isTrue();
    Path renamedFile = this.tempDir.resolve("Renamed.java");
    assertThat(renamedFile).exists();
    assertThat(Files.readString(renamedFile)).isEqualTo("public class Renamed {}");
    assertThat(sourceFile).doesNotExist();
  }

  @Test
  @DisplayName("Should return error when file does not exist")
  void shouldReturnErrorWhenFileDoesNotExist() {
    Path nonExistentFile = tempDir.resolve("nonexistent/NonExistent.java");
    DataTransferObject<RenameResponse> result = javaService.rename(nonExistentFile, "NewName");
    assertThat(result.getSucceed()).isFalse();
    assertThat(result.getErrorReason()).isEqualTo("File does not exist: " + nonExistentFile);
  }

  @Test
  @DisplayName("Should return error when file is not a .java file")
  void shouldReturnErrorWhenFileIsNotAJavaFile() throws IOException {
    Path sourceFile = this.tempDir.resolve("Original.txt");
    Files.writeString(sourceFile, "some content");
    DataTransferObject<RenameResponse> result = javaService.rename(sourceFile, "Renamed");
    assertThat(result.getSucceed()).isFalse();
    assertThat(result.getErrorReason()).isEqualTo("File is not a .java file: " + sourceFile);
  }
}
