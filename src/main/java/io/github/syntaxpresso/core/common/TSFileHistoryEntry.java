package io.github.syntaxpresso.core.common;

import java.nio.file.Path;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class TSFileHistoryEntry {
  private final String sourceCode;
  private final Path filePath;
  private final String fileName;
  private final long timestamp;
  private final String operation;
}

