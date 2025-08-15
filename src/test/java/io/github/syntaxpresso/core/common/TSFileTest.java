package io.github.syntaxpresso.core.common;

import static org.junit.jupiter.api.Assertions.*;

import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.treesitter.TSNode;

@DisplayName("TSFile Tests")
class TSFileTest {

  private final String initialContent = "public class MyClass {}";
  private final SupportedLanguage language = SupportedLanguage.JAVA;

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create TSFile from a string")
    void constructor_fromString_shouldSucceed() {
      TSFile tsFile = new TSFile(language, initialContent);
      assertNotNull(tsFile.getParser());
      assertNotNull(tsFile.getTree());
      assertEquals(initialContent, tsFile.getSourceCode());
      assertThrows(IllegalStateException.class, tsFile::getFile);
    }

    @Test
    @DisplayName("should create TSFile from a Path")
    void constructor_fromPath_shouldSucceed(@TempDir Path tempDir) throws IOException {
      Path file = tempDir.resolve("MyClass.java");
      Files.writeString(file, initialContent);
      TSFile tsFile = new TSFile(language, file);
      assertNotNull(tsFile.getParser());
      assertNotNull(tsFile.getTree());
      assertEquals(initialContent, tsFile.getSourceCode());
      assertEquals(file.toFile(), tsFile.getFile());
    }
  }

  @Nested
  @DisplayName("Source Code Manipulation Tests")
  class SourceCodeManipulationTests {
    private TSFile tsFile;

    @BeforeEach
    void setup() {
      tsFile = new TSFile(language, initialContent);
    }

    @Test
    @DisplayName("should update the entire source code")
    void updateSourceCode_full_shouldSucceed() {
      String newContent = "public class NewClass {}";
      tsFile.updateSourceCode(newContent);
      assertEquals(newContent, tsFile.getSourceCode());
      assertNotEquals(initialContent, tsFile.getSourceCode());
    }

    @Test
    @DisplayName("should update a range of the source code")
    void updateSourceCode_range_shouldSucceed() {
      tsFile.updateSourceCode(13, 20, "NewName");
      assertEquals("public class NewName {}", tsFile.getSourceCode());
    }

    @Test
    @DisplayName("should update source code from a TSNode")
    void updateSourceCode_fromNode_shouldSucceed() {
      TSNode node = tsFile.getNodeFromPosition(1, 15);
      assertNotNull(node);
      tsFile.updateSourceCode(node, "UpdatedClass");
      assertEquals("public class UpdatedClass {}", tsFile.getSourceCode());
    }
  }

  @Nested
  @DisplayName("File Operation Tests")
  class FileOperationTests {
    private TSFile tsFile;
    private Path tempFile;

    @BeforeEach
    void setup(@TempDir Path tempDir) throws IOException {
      tempFile = tempDir.resolve("MyClass.java");
      Files.writeString(tempFile, initialContent);
      tsFile = new TSFile(language, tempFile);
    }

    @Test
    @DisplayName("should save changes to the original file")
    void save_shouldWriteToFile() throws IOException {
      tsFile.updateSourceCode("public class SavedClass {}");
      tsFile.save();
      String fileContent = Files.readString(tempFile);
      assertEquals("public class SavedClass {}", fileContent);
    }

    @Test
    @DisplayName("should save to a new file")
    void saveAs_shouldCreateNewFile(@TempDir Path tempDir) throws IOException {
      Path newPath = tempDir.resolve("NewFile.java");
      tsFile.updateSourceCode("public class OtherClass {}");
      tsFile.saveAs(newPath);
      tsFile.save();
      assertTrue(Files.exists(newPath));
      assertFalse(Files.exists(tempFile));
      String fileContent = Files.readString(newPath);
      assertEquals("public class OtherClass {}", fileContent);
      assertEquals(newPath.toFile(), tsFile.getFile());
    }

    @Test
    @DisplayName("should move the file to a new directory")
    void move_shouldRelocateFile(@TempDir Path tempDir) throws IOException {
      Path newDir = tempDir.resolve("new_dir");
      Files.createDirectory(newDir);
      tsFile.move(newDir.toFile());
      tsFile.save();
      Path newPath = newDir.resolve("MyClass.java");
      assertTrue(Files.exists(newPath));
      assertFalse(Files.exists(tempFile));
      assertEquals(newPath.toFile(), tsFile.getFile());
    }

    @Test
    @DisplayName("should rename the file")
    void rename_shouldChangeFileName(@TempDir Path tempDir) throws IOException {
      tsFile.rename("Renamed.java");
      tsFile.save();
      Path newPath = tempFile.getParent().resolve("Renamed.java");
      assertTrue(Files.exists(newPath));
      assertFalse(Files.exists(tempFile));
      assertEquals(newPath.toFile(), tsFile.getFile());
    }
  }

  @Nested
  @DisplayName("Tree and Node Access Tests")
  class TreeAndNodeAccessTests {
    private TSFile tsFile;

    @BeforeEach
    void setup() {
      tsFile = new TSFile(language, "public class MyClass { void method() {} }");
    }

    @Test
    @DisplayName("should get a node from a specific position")
    void getNodeFromPosition_shouldReturnCorrectNode() {
      TSNode node = tsFile.getNodeFromPosition(1, 15); // "MyClass"
      assertNotNull(node);
      assertEquals("identifier", node.getType());
      assertEquals("MyClass", tsFile.getTextFromRange(node.getStartByte(), node.getEndByte()));
    }

    @Test
    @DisplayName("should return null for an invalid position")
    void getNodeFromPosition_invalidPosition_shouldReturnNull() {
      assertNull(tsFile.getNodeFromPosition(99, 99));
    }

    @Test
    @DisplayName("should extract text from a byte range")
    void getTextFromRange_shouldReturnSubstring() {
      String text = tsFile.getTextFromRange(7, 12);
      assertEquals("class", text);
    }

    @Test
    @DisplayName("should throw exception for an invalid range")
    void getTextFromRange_invalidRange_shouldThrowException() {
      assertThrows(IndexOutOfBoundsException.class, () -> tsFile.getTextFromRange(0, 999));
    }
  }

  @Nested
  @DisplayName("History Tracking Tests")
  class HistoryTrackingTests {
    private TSFile tsFile;

    @BeforeEach
    void setup() {
      tsFile = new TSFile(language, initialContent);
    }

    @Test
    @DisplayName("should initially have no modifications")
    void initialState_shouldNotBeModified() {
      assertFalse(tsFile.isModified());
      assertFalse(tsFile.hasUnsavedChanges());
    }

    @Test
    @DisplayName("should track source code updates")
    void updateSourceCode_shouldMarkAsModified() {
      String newContent = "public class Updated {}";
      tsFile.updateSourceCode(newContent);
      assertTrue(tsFile.isModified());
      assertTrue(tsFile.hasUnsavedChanges());
    }

    @Test
    @DisplayName("should track range updates")
    void updateSourceCodeRange_shouldMarkAsModified() {
      tsFile.updateSourceCode(13, 20, "NewName");
      assertTrue(tsFile.isModified());
    }

    @Test
    @DisplayName("should track multiple source code changes")
    void multipleUpdates_shouldStillBeModified() {
      tsFile.updateSourceCode("public class First {}");
      tsFile.updateSourceCode("public class Second {}");
      tsFile.updateSourceCode(13, 19, "Third");
      assertTrue(tsFile.isModified());
    }

    @Test
    @DisplayName("should track file rename")
    void rename_shouldMarkAsModified(@TempDir Path tempDir) throws IOException {
      Path file = tempDir.resolve("Original.java");
      Files.writeString(file, initialContent);
      TSFile fileWithPath = new TSFile(language, file);
      fileWithPath.rename("Renamed.java");
      assertTrue(fileWithPath.isModified());
    }

    @Test
    @DisplayName("should track file move")
    void move_shouldMarkAsModified(@TempDir Path tempDir) throws IOException {
      Path file = tempDir.resolve("Original.java");
      Files.writeString(file, initialContent);
      TSFile fileWithPath = new TSFile(language, file);
      Path newDir = tempDir.resolve("newdir");
      Files.createDirectory(newDir);
      fileWithPath.move(newDir.toFile());
      assertTrue(fileWithPath.isModified());
    }

    @Test
    @DisplayName("should track mixed operations")
    void mixedOperations_shouldMarkAsModified(@TempDir Path tempDir) throws IOException {
      Path file = tempDir.resolve("Test.java");
      Files.writeString(file, initialContent);
      TSFile fileWithPath = new TSFile(language, file);
      fileWithPath.updateSourceCode("public class Updated {}");
      fileWithPath.rename("Renamed.java");
      Path newDir = tempDir.resolve("newdir");
      Files.createDirectory(newDir);
      fileWithPath.move(newDir.toFile());
      assertTrue(fileWithPath.isModified());
    }
  }



  @Nested
  @DisplayName("History Integration Tests")
  class HistoryIntegrationTests {
    private TSFile tsFile;
    private Path tempFile;

    @BeforeEach
    void setup(@TempDir Path tempDir) throws IOException {
      tempFile = tempDir.resolve("Test.java");
      Files.writeString(tempFile, initialContent);
      tsFile = new TSFile(language, tempFile);
    }

    @Test
    @DisplayName("should clear modified flag on save")
    void save_shouldClearModifiedFlag() throws IOException {
      tsFile.updateSourceCode("public class Modified {}");
      tsFile.rename("NewName.java");
      assertTrue(tsFile.isModified());
      tsFile.save();
      assertFalse(tsFile.isModified());
    }

    @Test
    @DisplayName("should preserve functionality after modifications")
    void modifications_shouldNotAffectCoreFunctionality() throws IOException {
      tsFile.updateSourceCode("public class Test1 {}");
      tsFile.updateSourceCode("public class Test2 {}");
      tsFile.save();
      String fileContent = Files.readString(tsFile.getFile().toPath());
      assertEquals("public class Test2 {}", fileContent);
    }

    @Test
    @DisplayName("should handle complex workflow")
    void complexWorkflow_shouldMaintainConsistency(@TempDir Path tempDir) throws IOException {
      tsFile.updateSourceCode("public class Step1 {}");
      assertTrue(tsFile.isModified());
      tsFile.updateSourceCode("public class Step2 {}");
      tsFile.rename("Step2.java");
      assertTrue(tsFile.isModified());
      Path newDir = tempDir.resolve("newdir");
      Files.createDirectory(newDir);
      tsFile.move(newDir.toFile());
      tsFile.save();
      assertFalse(tsFile.isModified());
      assertTrue(Files.exists(newDir.resolve("Test.java")));
    }
  }
}
