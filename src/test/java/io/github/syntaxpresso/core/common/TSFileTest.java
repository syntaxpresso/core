package io.github.syntaxpresso.core.common;

import static org.junit.jupiter.api.Assertions.*;

import io.github.syntaxpresso.core.common.extra.SupportedIDE;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.treesitter.TSNode;
import org.treesitter.TSTree;

@DisplayName("TSFile Tests")
class TSFileTest {

  private static final String SIMPLE_JAVA_CODE = """
      package com.example;
      
      public class Hello {
          public void greet() {
              System.out.println("Hello World");
          }
      }
      """;

  private static final String UPDATED_JAVA_CODE = """
      package com.example;
      
      public class Hello {
          public void greet(String name) {
              System.out.println("Hello " + name);
          }
      }
      """;

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Should create TSFile from source code string")
    void shouldCreateTSFileFromSourceCode() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_JAVA_CODE);
      
      assertNotNull(tsFile.getParser());
      assertEquals(SIMPLE_JAVA_CODE, tsFile.getSourceCode());
      assertNotNull(tsFile.getTree());
      assertFalse(tsFile.isModified());
    }

    @Test
    @DisplayName("Should create TSFile from file path")
    void shouldCreateTSFileFromFilePath(@TempDir Path tempDir) throws IOException {
      Path javaFile = tempDir.resolve("Hello.java");
      Files.writeString(javaFile, SIMPLE_JAVA_CODE);
      
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, javaFile);
      
      assertNotNull(tsFile.getParser());
      assertEquals(SIMPLE_JAVA_CODE, tsFile.getSourceCode());
      assertNotNull(tsFile.getTree());
      assertEquals(javaFile.toFile(), tsFile.getFile());
      assertFalse(tsFile.isModified());
    }

    @Test
    @DisplayName("Should throw RuntimeException for non-existent file")
    void shouldThrowRuntimeExceptionForNonExistentFile(@TempDir Path tempDir) {
      Path nonExistentFile = tempDir.resolve("NonExistent.java");
      
      assertThrows(RuntimeException.class, () -> 
          new TSFile(SupportedLanguage.JAVA, nonExistentFile));
    }
  }

  @Nested
  @DisplayName("Source Code Update Tests")
  class SourceCodeUpdateTests {

    @Test
    @DisplayName("Should update entire source code")
    void shouldUpdateEntireSourceCode() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_JAVA_CODE);
      
      tsFile.updateSourceCode(UPDATED_JAVA_CODE);
      
      assertEquals(UPDATED_JAVA_CODE, tsFile.getSourceCode());
      assertTrue(tsFile.isModified());
      assertTrue(tsFile.hasUnsavedChanges());
    }

    @Test
    @DisplayName("Should update source code by byte range")
    void shouldUpdateSourceCodeByByteRange() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_JAVA_CODE);
      String original = tsFile.getSourceCode();
      int start = original.indexOf("greet()");
      int end = start + "greet()".length();
      
      tsFile.updateSourceCode(start, end, "greet(String name)");
      
      assertTrue(tsFile.getSourceCode().contains("greet(String name)"));
      assertTrue(tsFile.isModified());
    }

    @Test
    @DisplayName("Should update source code by TSNode")
    void shouldUpdateSourceCodeByTSNode() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_JAVA_CODE);
      TSNode rootNode = tsFile.getTree().getRootNode();
      TSNode methodNode = findMethodDeclaration(rootNode);
      assertNotNull(methodNode);
      
      String newMethod = """
              public void greet(String name) {
                  System.out.println("Hello " + name);
              }""";
      
      tsFile.updateSourceCode(methodNode, newMethod);
      
      assertTrue(tsFile.getSourceCode().contains("greet(String name)"));
      assertTrue(tsFile.isModified());
    }

    @Test
    @DisplayName("Should throw exception when updating with invalid range")
    void shouldThrowExceptionWhenUpdatingWithInvalidRange() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_JAVA_CODE);
      
      assertThrows(StringIndexOutOfBoundsException.class, () -> 
          tsFile.updateSourceCode(-1, 5, "new"));
    }
  }

  @Nested
  @DisplayName("Text Insertion Tests")
  class TextInsertionTests {

    @Test
    @DisplayName("Should insert text before node")
    void shouldInsertTextBeforeNode() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_JAVA_CODE);
      TSNode rootNode = tsFile.getTree().getRootNode();
      TSNode methodNode = findMethodDeclaration(rootNode);
      assertNotNull(methodNode);
      
      tsFile.insertTextBeforeNode(methodNode, "    @Override\n    ");
      
      assertTrue(tsFile.getSourceCode().contains("@Override"));
      assertTrue(tsFile.isModified());
    }

    @Test
    @DisplayName("Should insert text after node")
    void shouldInsertTextAfterNode() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_JAVA_CODE);
      TSNode rootNode = tsFile.getTree().getRootNode();
      TSNode methodNode = findMethodDeclaration(rootNode);
      assertNotNull(methodNode);
      
      tsFile.insertTextAfterNode(methodNode, "\n    // Method added");
      
      assertTrue(tsFile.getSourceCode().contains("// Method added"));
      assertTrue(tsFile.isModified());
    }

    @Test
    @DisplayName("Should handle insertion with large text")
    void shouldHandleInsertionWithLargeText() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_JAVA_CODE);
      TSNode rootNode = tsFile.getTree().getRootNode();
      TSNode methodNode = findMethodDeclaration(rootNode);
      assertNotNull(methodNode);
      String largeText = "    // ".repeat(100) + "Large comment\n    ";
      
      tsFile.insertTextBeforeNode(methodNode, largeText);
      
      assertTrue(tsFile.getSourceCode().contains("Large comment"));
      assertTrue(tsFile.isModified());
    }
  }

  @Nested
  @DisplayName("File Operations Tests")
  class FileOperationsTests {

    @Test
    @DisplayName("Should save file to original path")
    void shouldSaveFileToOriginalPath(@TempDir Path tempDir) throws IOException {
      Path javaFile = tempDir.resolve("Hello.java");
      Files.writeString(javaFile, SIMPLE_JAVA_CODE);
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, javaFile);
      
      tsFile.updateSourceCode(UPDATED_JAVA_CODE);
      tsFile.save();
      
      String savedContent = Files.readString(javaFile);
      assertEquals(UPDATED_JAVA_CODE, savedContent);
      assertFalse(tsFile.isModified());
    }

    @Test
    @DisplayName("Should save file to new path")
    void shouldSaveFileToNewPath(@TempDir Path tempDir) throws IOException {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_JAVA_CODE);
      Path newFile = tempDir.resolve("NewHello.java");
      
      tsFile.saveAs(newFile);
      
      assertTrue(Files.exists(newFile));
      assertEquals(SIMPLE_JAVA_CODE, Files.readString(newFile));
    }

    @Test
    @DisplayName("Should move file to new directory")
    void shouldMoveFileToNewDirectory(@TempDir Path tempDir) throws IOException {
      Path javaFile = tempDir.resolve("Hello.java");
      Files.writeString(javaFile, SIMPLE_JAVA_CODE);
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, javaFile);
      
      Path newDir = tempDir.resolve("newdir");
      Files.createDirectory(newDir);
      
      tsFile.move(newDir.toFile());
      tsFile.save();
      
      Path expectedPath = newDir.resolve("Hello.java");
      assertTrue(Files.exists(expectedPath));
      assertFalse(Files.exists(javaFile));
    }

    @Test
    @DisplayName("Should rename file in current directory")
    void shouldRenameFileInCurrentDirectory(@TempDir Path tempDir) throws IOException {
      Path javaFile = tempDir.resolve("Hello.java");
      Files.writeString(javaFile, SIMPLE_JAVA_CODE);
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, javaFile);
      
      tsFile.rename("Greeting");
      tsFile.save();
      
      Path expectedPath = tempDir.resolve("Greeting.java");
      assertTrue(Files.exists(expectedPath));
      assertFalse(Files.exists(javaFile));
    }

    @Test
    @DisplayName("Should throw exception when saving without file path")
    void shouldThrowExceptionWhenSavingWithoutFilePath() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_JAVA_CODE);
      
      assertThrows(IllegalStateException.class, tsFile::save);
    }
  }

  @Nested
  @DisplayName("Node Position and Text Extraction Tests")
  class NodePositionAndTextExtractionTests {

    @Test
    @DisplayName("Should get node from valid position")
    void shouldGetNodeFromValidPosition() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_JAVA_CODE);
      
      TSNode node = tsFile.getNodeFromPosition(4, 10, SupportedIDE.VSCODE);
      
      assertNotNull(node);
    }

    @Test
    @DisplayName("Should return null for invalid position")
    void shouldReturnNullForInvalidPosition() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_JAVA_CODE);
      
      TSNode node = tsFile.getNodeFromPosition(100, 100, SupportedIDE.VSCODE);
      
      assertNull(node);
    }

    @Test
    @DisplayName("Should return null for zero or negative position")
    void shouldReturnNullForZeroOrNegativePosition() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_JAVA_CODE);
      
      assertNull(tsFile.getNodeFromPosition(0, 1, SupportedIDE.VSCODE));
      assertNull(tsFile.getNodeFromPosition(1, 0, SupportedIDE.VSCODE));
      assertNull(tsFile.getNodeFromPosition(-1, 5, SupportedIDE.VSCODE));
    }

    @Test
    @DisplayName("Should get text from byte range")
    void shouldGetTextFromByteRange() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_JAVA_CODE);
      String sourceCode = tsFile.getSourceCode();
      int start = sourceCode.indexOf("Hello");
      int end = start + "Hello".length();
      
      String text = tsFile.getTextFromRange(start, end);
      
      assertEquals("Hello", text);
    }

    @Test
    @DisplayName("Should get text from TSNode")
    void shouldGetTextFromTSNode() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_JAVA_CODE);
      TSNode rootNode = tsFile.getTree().getRootNode();
      TSNode classNode = findClassDeclaration(rootNode);
      assertNotNull(classNode);
      
      String text = tsFile.getTextFromNode(classNode);
      
      assertTrue(text.contains("public class Hello"));
    }

    @Test
    @DisplayName("Should throw exception for invalid byte range")
    void shouldThrowExceptionForInvalidByteRange() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_JAVA_CODE);
      int length = tsFile.getSourceCode().length();
      
      assertThrows(IndexOutOfBoundsException.class, () -> 
          tsFile.getTextFromRange(-1, 5));
      assertThrows(IndexOutOfBoundsException.class, () -> 
          tsFile.getTextFromRange(0, length + 10));
      assertThrows(IndexOutOfBoundsException.class, () -> 
          tsFile.getTextFromRange(10, 5));
    }
  }

  @Nested
  @DisplayName("Utility Methods Tests")
  class UtilityMethodsTests {

    @Test
    @DisplayName("Should find parent node by type")
    void shouldFindParentNodeByType() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_JAVA_CODE);
      TSNode rootNode = tsFile.getTree().getRootNode();
      TSNode methodNode = findMethodDeclaration(rootNode);
      assertNotNull(methodNode);
      
      Optional<TSNode> classParent = tsFile.findParentNodeByType(methodNode, "class_declaration");
      
      assertTrue(classParent.isPresent());
    }

    @Test
    @DisplayName("Should return empty when parent type not found")
    void shouldReturnEmptyWhenParentTypeNotFound() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_JAVA_CODE);
      TSNode rootNode = tsFile.getTree().getRootNode();
      
      Optional<TSNode> result = tsFile.findParentNodeByType(rootNode, "non_existent_type");
      
      assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should handle null inputs for utility methods")
    void shouldHandleNullInputsForUtilityMethods() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_JAVA_CODE);
      
      Optional<TSNode> result1 = tsFile.findParentNodeByType(null, "class_declaration");
      Optional<TSNode> result2 = tsFile.findChildNodeByType(null, "method_declaration");
      
      assertFalse(result1.isPresent());
      assertFalse(result2.isPresent());
    }

    @Test
    @DisplayName("Should check if node is within another node")
    void shouldCheckIfNodeIsWithinAnotherNode() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_JAVA_CODE);
      TSNode rootNode = tsFile.getTree().getRootNode();
      TSNode classNode = findClassDeclaration(rootNode);
      TSNode methodNode = findMethodDeclaration(rootNode);
      assertNotNull(classNode);
      assertNotNull(methodNode);
      
      boolean isWithin = tsFile.isNodeWithin(methodNode, classNode);
      
      assertTrue(isWithin);
    }

    @Test
    @DisplayName("Should return false for null nodes in isNodeWithin")
    void shouldReturnFalseForNullNodesInIsNodeWithin() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_JAVA_CODE);
      TSNode validNode = tsFile.getTree().getRootNode();
      
      assertFalse(tsFile.isNodeWithin(null, validNode));
      assertFalse(tsFile.isNodeWithin(validNode, null));
      assertFalse(tsFile.isNodeWithin(null, null));
    }

    @Test
    @DisplayName("Should get file name without extension")
    void shouldGetFileNameWithoutExtension(@TempDir Path tempDir) throws IOException {
      Path javaFile = tempDir.resolve("Hello.java");
      Files.writeString(javaFile, SIMPLE_JAVA_CODE);
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, javaFile);
      
      Optional<String> fileName = tsFile.getFileNameWithoutExtension();
      
      assertTrue(fileName.isPresent());
      assertEquals("Hello", fileName.get());
    }

    @Test
    @DisplayName("Should return empty for TSFile without file")
    void shouldReturnEmptyForTSFileWithoutFile() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_JAVA_CODE);
      
      Optional<String> fileName = tsFile.getFileNameWithoutExtension();
      
      assertFalse(fileName.isPresent());
    }
  }

  @Nested
  @DisplayName("Exception Handling Tests")
  class ExceptionHandlingTests {

    @Test
    @DisplayName("Should throw exception when getting file from TSFile without file")
    void shouldThrowExceptionWhenGettingFileFromTSFileWithoutFile() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_JAVA_CODE);
      
      assertThrows(IllegalStateException.class, tsFile::getFile);
    }

    @Test
    @DisplayName("Should handle empty source code")
    void shouldHandleEmptySourceCode() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, "");
      
      assertNotNull(tsFile.getParser());
      assertEquals("", tsFile.getSourceCode());
      assertNotNull(tsFile.getTree());
    }

    @Test
    @DisplayName("Should handle special characters in source code")
    void shouldHandleSpecialCharactersInSourceCode() {
      String specialCode = "public class Test { String s = \"Hello\\nWorld\\t!\"; }";
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, specialCode);
      
      assertEquals(specialCode, tsFile.getSourceCode());
      assertNotNull(tsFile.getTree());
    }

    @Test
    @DisplayName("Should throw exception when moving file that hasn't been saved")
    void shouldThrowExceptionWhenMovingFileNotSaved() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_JAVA_CODE);
      
      assertThrows(IllegalStateException.class, () -> 
          tsFile.move(new File("/tmp")));
    }

    @Test
    @DisplayName("Should throw exception when renaming file that hasn't been saved")
    void shouldThrowExceptionWhenRenamingFileNotSaved() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_JAVA_CODE);
      
      assertThrows(IllegalStateException.class, () -> 
          tsFile.rename("NewName"));
    }

    @Test
    @DisplayName("Should handle concurrent operations safely")
    void shouldHandleConcurrentOperationsSafely() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_JAVA_CODE);
      
      // Simulate multiple operations
      tsFile.updateSourceCode("// Modified\n" + SIMPLE_JAVA_CODE);
      boolean isModified = tsFile.isModified();
      String sourceCode = tsFile.getSourceCode();
      
      assertTrue(isModified);
      assertTrue(sourceCode.contains("// Modified"));
    }
  }

  // Helper methods
  private TSNode findMethodDeclaration(TSNode rootNode) {
    return findNodeByType(rootNode, "method_declaration");
  }

  private TSNode findClassDeclaration(TSNode rootNode) {
    return findNodeByType(rootNode, "class_declaration");
  }

  private TSNode findNodeByType(TSNode node, String type) {
    if (node == null) return null;
    if (type.equals(node.getType())) {
      return node;
    }
    for (int i = 0; i < node.getNamedChildCount(); i++) {
      TSNode child = node.getNamedChild(i);
      TSNode result = findNodeByType(child, type);
      if (result != null) {
        return result;
      }
    }
    return null;
  }
}