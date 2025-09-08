package io.github.syntaxpresso.core.service.java.language.extra;

/**
 * Enum representing all capture groups from the Tree-sitter query for Java class declarations. Each
 * enum constant corresponds to a @capture in the query.
 */
public enum ImportCapture {
  // Corresponds to the captures in the associated query
  IMPORT("import"),
  RELATIVE_IMPORT_SCOPE("relativeImportScope"),
  FULL_IMPORT_SCOPE("fullImportScope"),
  ASTERISK("asterisk"),
  CLASS_NAME("className"),
  METHOD_TYPE("methodType"),
  METHOD_MODIFIERS("methodModifiers"),
  METHOD_PARAMETERS("methodParameters");

  private final String captureName;

  ImportCapture(String captureName) {
    this.captureName = captureName;
  }

  /**
   * Get the capture name as it appears in the Tree-sitter query (without the @ symbol).
   *
   * @return the capture name
   */
  public String getCaptureName() {
    return captureName;
  }

  /**
   * Get the capture name with the @ symbol prefix as used in Tree-sitter queries.
   *
   * @return the capture name with @ prefix
   */
  public String getCaptureWithAt() {
    return "@" + captureName;
  }

  /**
   * Find a ImportCapture by its capture name.
   *
   * @param captureName the name to search for (with or without @ prefix)
   * @return the matching ImportCapture, or null if not found
   */
  public static ImportCapture fromCaptureName(String captureName) {
    if (captureName == null) {
      return null;
    }

    // Remove @ prefix if present
    String cleanName = captureName.startsWith("@") ? captureName.substring(1) : captureName;

    for (ImportCapture capture : values()) {
      if (capture.captureName.equals(cleanName)) {
        return capture;
      }
    }
    return null;
  }

  @Override
  public String toString() {
    return captureName;
  }
}
