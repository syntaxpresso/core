package io.github.syntaxpresso.core.service.java.language.extra;

/**
 * Enum representing all capture groups from the Tree-sitter query for Java class declarations. Each
 * enum constant corresponds to a @capture in the query.
 */
public enum MethodCapture {
  // Corresponds to the captures in the associated query
  METHOD("method"),
  METHOD_BODY("methodBody"),
  METHOD_NAME("methodName"),
  METHOD_TYPE("methodType"),
  METHOD_MODIFIERS("methodModifiers"),
  METHOD_PARAMETERS("methodParameters");

  private final String captureName;

  MethodCapture(String captureName) {
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
   * Find a MethodCapture by its capture name.
   *
   * @param captureName the name to search for (with or without @ prefix)
   * @return the matching MethodCapture, or null if not found
   */
  public static MethodCapture fromCaptureName(String captureName) {
    if (captureName == null) {
      return null;
    }

    // Remove @ prefix if present
    String cleanName = captureName.startsWith("@") ? captureName.substring(1) : captureName;

    for (MethodCapture capture : values()) {
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
