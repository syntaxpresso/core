package io.github.syntaxpresso.core.service.java.language.extra;

/**
 * Enum representing all capture groups from the Tree-sitter query for Java class declarations. Each
 * enum constant corresponds to a @capture in the query.
 */
public enum VariableCapture {
  // Corresponds to the captures in the associated query
  VARIABLE_MODIFIERS("variableModifiers"),
  VARIABLE_TYPE("variableType"),
  VARIABLE_NAME("variableName"),
  VARIABLE_VALUE("variableValue"),
  VARIABLE("variable");

  private final String captureName;

  VariableCapture(String captureName) {
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
   * Find a VariableCapture by its capture name.
   *
   * @param captureName the name to search for (with or without @ prefix)
   * @return the matching VariableCapture, or null if not found
   */
  public static VariableCapture fromCaptureName(String captureName) {
    if (captureName == null) {
      return null;
    }

    // Remove @ prefix if present
    String cleanName = captureName.startsWith("@") ? captureName.substring(1) : captureName;

    for (VariableCapture capture : values()) {
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
