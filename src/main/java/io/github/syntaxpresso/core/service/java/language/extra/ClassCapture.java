package io.github.syntaxpresso.core.service.java.language.extra;

/**
 * Enum representing all capture groups from the Tree-sitter query for Java class declarations. Each
 * enum constant corresponds to a @capture in the query.
 */
public enum ClassCapture {
  // Corresponds to the captures in the associated query
  CLASS("class"),
  MODIFIERS("modifiers"),
  CLASS_ANNOTATION("classAnnotation"),
  CLASS_NAME("className"),
  CLASS_INTERFACES("classInterfaces"),
  SUPERCLASS("superclass"),
  SUPERCLASS_NAME("superclassName"),
  CLASS_BODY("classBody"),
  CONSTRUCTOR_DECLARATION("constructorDeclaration");

  private final String captureName;

  ClassCapture(String captureName) {
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
   * Find a ClassCapture by its capture name.
   *
   * @param captureName the name to search for (with or without @ prefix)
   * @return the matching ClassCapture, or null if not found
   */
  public static ClassCapture fromCaptureName(String captureName) {
    if (captureName == null) {
      return null;
    }

    // Remove @ prefix if present
    String cleanName = captureName.startsWith("@") ? captureName.substring(1) : captureName;

    for (ClassCapture capture : values()) {
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
