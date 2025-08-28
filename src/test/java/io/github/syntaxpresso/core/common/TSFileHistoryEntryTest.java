package io.github.syntaxpresso.core.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TSFileHistoryEntry Tests")
class TSFileHistoryEntryTest {
  @Test
  @DisplayName("should create history entry with all fields")
  void createHistoryEntry_withAllFields_shouldSetAllProperties() {
    String sourceCode = "public class Test {}";
    Path filePath = Path.of("/test/Test.java");
    String fileName = "Test.java";
    long timestamp = System.currentTimeMillis();
    String operation = "UPDATE_SOURCE";
    TSFileHistoryEntry entry =
        new TSFileHistoryEntry(sourceCode, filePath, fileName, timestamp, operation);
    assertEquals(sourceCode, entry.getSourceCode());
    assertEquals(filePath, entry.getFilePath());
    assertEquals(fileName, entry.getFileName());
    assertEquals(timestamp, entry.getTimestamp());
    assertEquals(operation, entry.getOperation());
  }

  @Test
  @DisplayName("should handle null values")
  void createHistoryEntry_withNullValues_shouldAllowNulls() {
    TSFileHistoryEntry entry = new TSFileHistoryEntry(null, null, null, 0L, null);
    assertNotNull(entry);
    assertEquals(null, entry.getSourceCode());
    assertEquals(null, entry.getFilePath());
    assertEquals(null, entry.getFileName());
    assertEquals(0L, entry.getTimestamp());
    assertEquals(null, entry.getOperation());
  }

  @Test
  @DisplayName("should support different operation types")
  void createHistoryEntry_withDifferentOperations_shouldStoreCorrectly() {
    String[] operations = {"UPDATE_SOURCE", "RENAME_FILE", "MOVE_FILE"};
    long timestamp = System.currentTimeMillis();
    for (String operation : operations) {
      TSFileHistoryEntry entry =
          new TSFileHistoryEntry("test", Path.of("/test"), "test", timestamp, operation);
      assertEquals(operation, entry.getOperation());
    }
  }
}
