package io.github.syntaxpresso.core.service.java.language.extra;

/**
 * Enum representing all capture groups from the Tree-sitter query for Java annotation declarations.
 * Each enum constant corresponds to a @capture in the query.
 */
public enum AnnotationCapture {
  ANNOTATION("annotation"),
  MARKER_ANNOTATION("markerAnnotation"),
  ANNOTATION_NAME("annotationName"),
  ANNOTATION_ARGUMENTS("annotationArguments"),
  ENTITY("entity"),
  NAME("name"),
  ARGUMENT_PAIR("argumentPair"),
  KEY("key"),
  VALUE("value");

  private final String captureName;

  AnnotationCapture(String captureName) {
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
   * Find an AnnotationCapture by its capture name.
   *
   * @param captureName the name to search for (with or without @ prefix)
   * @return the matching AnnotationCapture, or null if not found
   */
  public static AnnotationCapture fromCaptureName(String captureName) {
    if (captureName == null) {
      return null;
    }

    String cleanName = captureName.startsWith("@") ? captureName.substring(1) : captureName;

    for (AnnotationCapture capture : values()) {
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
