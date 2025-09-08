package io.github.syntaxpresso.core.service.java.language.extra;

import lombok.Data;

@Data
public class ImportInsertionPoint {

  /** Defines the possible insertion points for an import statement. */
  public enum ImportInsertionPosition {
    /** At the very beginning of the file. */
    BEGINNING,

    /** Immediately after the package declaration. */
    AFTER_PACKAGE_DECLARATION,

    /** After the last existing import statement. */
    AFTER_LAST_IMPORT
  }

  int insertByte;
  ImportInsertionPosition position;
}
