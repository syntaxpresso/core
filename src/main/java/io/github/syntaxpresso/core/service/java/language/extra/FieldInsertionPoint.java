package io.github.syntaxpresso.core.service.java.language.extra;

import lombok.Data;

@Data
public class FieldInsertionPoint {
  private boolean breakLineBefore = false;
  private boolean breakLineAfter = false;

  /** Defines the possible insertion points for a field declaration. */
  public enum FieldInsertionPosition {
    /** At the beginning of the class body (after opening brace). */
    BEGINNING_OF_CLASS_BODY,

    /** Before the first existing field declaration. */
    BEFORE_FIRST_FIELD,

    /** After the last existing field declaration. */
    AFTER_LAST_FIELD
  }

  int insertByte;
  FieldInsertionPosition position;
}