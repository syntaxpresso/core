package io.github.syntaxpresso.core.service.java.language.extra;

/**
 * Enum representing all capture groups from the Tree-sitter query for Java interface declarations.
 * Each enum constant corresponds to a @capture in the query.
 */
public enum InterfaceCapture {
  // Corresponds to the captures in the associated query
  INTERFACE("interface"),
  MODIFIERS("modifiers"),
  INTERFACE_ANNOTATION("interfaceAnnotation"),
  INTERFACE_NAME("interfaceName"),
  INTERFACE_EXTENDS("interfaceExtends"),
  INTERFACE_BODY("interfaceBody");

  private final String captureName;

  InterfaceCapture(String captureName) {
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
   * Find an InterfaceCapture by its capture name.
   *
   * @param captureName the name to search for (with or without @ prefix)
   * @return the matching InterfaceCapture, or null if not found
   */
  public static InterfaceCapture fromCaptureName(String captureName) {
    if (captureName == null) {
      return null;
    }

    // Remove @ prefix if present
    String cleanName = captureName.startsWith("@") ? captureName.substring(1) : captureName;

    for (InterfaceCapture capture : values()) {
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