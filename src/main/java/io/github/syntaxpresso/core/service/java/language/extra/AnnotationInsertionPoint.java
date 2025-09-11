package io.github.syntaxpresso.core.service.java.language.extra;

import lombok.Data;

@Data
public class AnnotationInsertionPoint {

  /** Defines the possible insertion points for an annotation statement. */
  public enum AnnotationInsertionPosition {
    /** Immediately above the scope declaration. */
    ABOVE_SCOPE_DECLARATION,

    /** Before the first existing annotation statement. */
    BEFORE_FIRST_ANNOTATION
  }

  int insertByte;
  AnnotationInsertionPosition position;
}
