package io.github.syntaxpresso.core.service.java.language.extra;

/**
 * Enum representing all capture groups from the Tree-sitter query for Java method invocations. Each
 * enum constant corresponds to a @capture in the method invocation query patterns.
 */
public enum MethodInvocationCapture {
  // Core invocation captures
  INVOCATION("invocation"),
  OBJECT("object"),
  METHOD("method"),
  ARGUMENTS("arguments"),

  // Object-related captures
  PARENT_OBJECT("parent_object"),
  FIRST_METHOD("first_method"),

  // Argument-related captures
  ARGUMENT("argument"),
  ARGUMENT_LIST("argumentList"),

  // Type-related captures (for generic method calls)
  TYPE_ARGUMENTS("typeArguments"),

  // Additional context captures
  THIS("this"),
  SUPER("super"),
  FIELD("field"),

  // Chained invocation captures
  CHAINED_OBJECT("chainedObject"),
  CHAINED_METHOD("chainedMethod");

  private final String captureName;

  MethodInvocationCapture(String captureName) {
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
   * Find a MethodInvocationCapture by its capture name.
   *
   * @param captureName the name to search for (with or without @ prefix)
   * @return the matching MethodInvocationCapture, or null if not found
   */
  public static MethodInvocationCapture fromCaptureName(String captureName) {
    if (captureName == null) {
      return null;
    }
    // Remove @ prefix if present
    String cleanName = captureName.startsWith("@") ? captureName.substring(1) : captureName;

    for (MethodInvocationCapture capture : values()) {
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
