package io.github.syntaxpresso.core.common;

import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import org.treesitter.TSNode;
import org.treesitter.TSParser;
import org.treesitter.TSPoint;
import org.treesitter.TSQuery;
import org.treesitter.TSQueryCapture;
import org.treesitter.TSQueryCursor;
import org.treesitter.TSQueryMatch;
import org.treesitter.TSTree;

@Getter
public class TSFile {
  private final TSParser parser;
  private File file;
  private TSTree tree;
  private String sourceCode;
  private Path newPath;

  /**
   * Creates a TSFile instance from a given programming language and source code string.
   *
   * @param supportedLanguage The language of the source code.
   * @param sourceCode The source code content.
   */
  public TSFile(SupportedLanguage supportedLanguage, String sourceCode) {
    this.parser = ParserFactory.get(supportedLanguage);
    this.setData(sourceCode);
  }

  /**
   * Creates a TSFile instance from a given programming language and a file path.
   *
   * @param supportedLanguage The language of the file.
   * @param path The path to the file to parse.
   * @throws IOException If the file cannot be read.
   */
  public TSFile(SupportedLanguage supportedLanguage, Path path) {
    this.parser = ParserFactory.get(supportedLanguage);
    this.file = path.toFile();
    try {
      String content = Files.readString(path, StandardCharsets.UTF_8);
      this.setData(content);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Internal method to parse source code and set the tree and sourceCode fields.
   *
   * @param sourceCode The source code to parse.
   */
  private void setData(String sourceCode) {
    if (this.parser == null) {
      throw new IllegalStateException("Parser is not initialized.");
    }
    this.tree = this.parser.parseString(null, sourceCode);
    this.sourceCode = sourceCode;
  }

  /**
   * Updates the source code and re-parses the content.
   *
   * @param newSourceCode The new source code.
   */
  public void updateSourceCode(String newSourceCode) {
    this.setData(newSourceCode);
  }

  /**
   * Updates a specific range of the source code and re-parses the content.
   *
   * @param startByte The starting index of the text to replace.
   * @param endByte The ending index of the text to replace.
   * @param newText The new text to insert.
   */
  public void updateSourceCode(int startByte, int endByte, String newText) {
    if (this.sourceCode == null) {
      throw new IllegalStateException("Source code has not been initialized.");
    }
    String newContent =
        new StringBuilder(this.sourceCode).replace(startByte, endByte, newText).toString();
    this.setData(newContent);
  }

  /**
   * Updates the source code by replacing the text content of a given CST node.
   *
   * @param node The TSNode to be replaced.
   * @param newText The new string that will replace the node's text.
   * @throws IllegalStateException if the tree has not been generated yet.
   */
  public void updateSourceCode(TSNode node, String newText) {
    if (this.tree == null) {
      throw new IllegalStateException("Tree is not set.");
    }
    int start = node.getStartByte();
    int end = node.getEndByte();
    this.updateSourceCode(start, end, newText);
  }

  /**
   * Saves the current source code to the original file path.
   *
   * @throws IOException If the file cannot be written.
   * @throws IllegalStateException If the original file path is not known.
   */
  public void save() throws IOException {
    if (this.file == null) {
      throw new IllegalStateException("File path is not set. Use saveAs(path) instead.");
    }
    if (this.newPath != null) {
      Files.move(this.file.toPath(), this.newPath, StandardCopyOption.REPLACE_EXISTING);
      this.file = this.newPath.toFile();
      this.newPath = null;
    }
    Files.writeString(this.file.toPath(), this.sourceCode, StandardCharsets.UTF_8);
  }

  /**
   * Saves the current source code to a new file path.
   *
   * @param path The path to save the file to.
   * @throws IOException If the file cannot be written.
   */
  public void saveAs(Path path) throws IOException {
    if (this.file != null && this.newPath == null) {
      this.newPath = path;
    } else {
      this.file = Files.writeString(path, this.sourceCode, StandardCharsets.UTF_8).toFile();
    }
  }

  /**
   * Moves the file to a new destination.
   *
   * @param destination The destination directory or full file path.
   * @throws IOException If an I/O error occurs.
   * @throws IllegalStateException If the file has not been saved to disk yet.
   */
  public void move(File destination) {
    if (this.file == null) {
      throw new IllegalStateException("Cannot move a file that has not been saved yet.");
    }
    Path targetPath = destination.toPath();
    if (Files.isDirectory(targetPath)) {
      targetPath = targetPath.resolve(this.file.getName());
    }
    this.newPath = targetPath;
  }

  /**
   * Renames the file in its current directory.
   *
   * @param newName The new name for the file.
   * @throws IllegalStateException If the file has not been saved to disk yet.
   */
  public void rename(String newName) {
    if (this.file == null) {
      throw new IllegalStateException("Cannot rename a file that has not been saved yet.");
    }
    Path parentDir = this.file.toPath().getParent();
    if (parentDir == null) {
      throw new IllegalStateException("Unable to get parent directory");
    }
    Path targetPath;
    if (newName.contains(".")) {
      targetPath = parentDir.resolve(newName);
    } else {
      targetPath =
          parentDir.resolve(
              newName
                  + SupportedLanguage.fromLanguage(this.parser.getLanguage())
                      .get()
                      .getFileExtension());
    }
    this.newPath = targetPath;
  }

  /**
   * Retrieves the smallest named CST node at a specific line and column.
   *
   * @param line The one-based line number.
   * @param column The one-based column number.
   * @return The {@link TSNode} at the specified position, or null if not found.
   * @throws IllegalStateException if the source code has not been parsed yet.
   */
  public TSNode getNodeFromPosition(int line, int column) {
    if (this.tree == null) {
      throw new IllegalStateException("Tree is not set; cannot get a node by position.");
    }
    if (line <= 0 || column <= 0) {
      return null;
    }
    TSNode rootNode = this.tree.getRootNode();
    TSPoint endPoint = rootNode.getEndPoint();
    int requestedLine = line - 1;
    int requestedColumn = column - 1;
    if (requestedLine > endPoint.getRow()
        || (requestedLine == endPoint.getRow() && requestedColumn > endPoint.getColumn())) {
      return null;
    }
    TSPoint point = new TSPoint(requestedLine, requestedColumn);
    return rootNode.getNamedDescendantForPointRange(point, point);
  }

  /**
   * Returns a substring from the source code based on a given byte range.
   *
   * @param startByte The starting byte offset.
   * @param endByte The ending byte offset.
   * @return The text within the specified range.
   * @throws IllegalStateException If the source code has not been initialized.
   * @throws IndexOutOfBoundsException If the specified range is invalid.
   */
  public String getTextFromRange(int startByte, int endByte) {
    if (this.sourceCode == null) {
      throw new IllegalStateException("Source code has not been initialized.");
    }
    if (startByte < 0 || endByte > this.sourceCode.length() || startByte > endByte) {
      throw new IndexOutOfBoundsException("Invalid range specified for substring.");
    }
    return this.sourceCode.substring(startByte, endByte);
  }

  /**
   * Returns a substring from the source code that corresponds to the given node.
   *
   * @param node The node from which to extract text.
   * @return The text of the node.
   */
  public String getTextFromNode(TSNode node) {
    return this.getTextFromRange(node.getStartByte(), node.getEndByte());
  }

  /**
   * Returns the file associated with this object.
   *
   * @return The file.
   * @throws IllegalStateException if the file has not been set.
   */
  public File getFile() {
    if (this.file == null) {
      throw new IllegalStateException("File is not set.");
    }
    return this.file;
  }

  /**
   * Returns the syntax tree of the source code.
   *
   * @return The TSTree object.
   * @throws IllegalStateException if the tree has not been generated.
   */
  public TSTree getTree() {
    if (this.tree == null) {
      throw new IllegalStateException("Tree is not set.");
    }
    return this.tree;
  }

  /**
   * Returns the source code as a string.
   *
   * @return The source code.
   * @throws IllegalStateException if the source code has not been set.
   */
  public String getSourceCode() {
    if (this.sourceCode == null) {
      throw new IllegalStateException("Source code is not set.");
    }
    return this.sourceCode;
  }

  /**
   * Returns the TSParser instance associated with this file.
   *
   * @return The TSParser instance.
   */
  public TSParser getParser() {
    return this.parser;
  }

  /**
   * Finds the first parent node of a given node that has a specific type.
   *
   * @param startNode The node from which to start searching upwards.
   * @param parentType The type of the parent node to find (e.g., "method_declaration").
   * @return An Optional containing the found parent TSNode, or empty if not found.
   */
  public Optional<TSNode> findParentNodeByType(TSNode startNode, String parentType) {
    if (startNode == null || parentType == null || parentType.isBlank()) {
      return Optional.empty();
    }
    TSNode currentNode = startNode;
    while (currentNode != null) {
      if (parentType.equals(currentNode.getType())) {
        return Optional.of(currentNode);
      }
      currentNode = currentNode.getParent();
    }
    return Optional.empty();
  }

  /**
   * Executes a Tree-sitter query on a specific node and returns the captured nodes.
   *
   * @param node The node to run the query on.
   * @param query The Tree-sitter query string.
   * @return A list of {@link TSNode} objects captured by the query.
   */
  public List<TSNode> query(TSNode node, String query) {
    List<TSNode> foundNodes = new ArrayList<>();
    TSQuery queryObj = new TSQuery(this.getParser().getLanguage(), query);
    TSQueryCursor cursor = new TSQueryCursor();
    cursor.exec(queryObj, this.getTree().getRootNode());
    TSQueryMatch match = new TSQueryMatch();
    while (cursor.nextMatch(match)) {
      for (TSQueryCapture capture : match.getCaptures()) {
        foundNodes.add(capture.getNode());
      }
    }
    return foundNodes;
  }

  /**
   * Executes a Tree-sitter query on the entire syntax tree and returns the captured nodes.
   *
   * @param query The Tree-sitter query string.
   * @return A list of {@link TSNode} objects captured by the query.
   */
  public List<TSNode> query(String query) {
    return this.query(this.getTree().getRootNode(), query);
  }
}
