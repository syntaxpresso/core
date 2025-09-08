package io.github.syntaxpresso.core.service.java.language.extra;

public enum AnnotationCapture {
  ANNOTATION("annotation"),
  MARKER_ANNOTATION("markerAnnotation"),
  ANNOTATION_NAME("annotationName"),
  ANNOTATION_ARGUMENTS("annotationArguments");

  private final String captureName;

  AnnotationCapture(String captureName) {
    this.captureName = captureName;
  }

  public String getCaptureName() {
    return captureName;
  }
}
