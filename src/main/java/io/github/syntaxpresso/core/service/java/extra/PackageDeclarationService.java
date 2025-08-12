package io.github.syntaxpresso.core.service.java.extra;

import io.github.syntaxpresso.core.common.TSFile;
import java.util.Optional;
import org.treesitter.TSNode;
import org.treesitter.TSQuery;
import org.treesitter.TSQueryCapture;
import org.treesitter.TSQueryCursor;
import org.treesitter.TSQueryMatch;

public class PackageDeclarationService {

  /**
   * Gets the package name from a Java file.
   *
   * @param file The TSFile to analyze.
   * @return An Optional containing the package name, or empty if not found.
   */
  public Optional<String> getPackageName(TSFile file) {
    String packageQuery = "(package_declaration (scoped_identifier) @package_name)";
    TSQuery query = new TSQuery(file.getParser().getLanguage(), packageQuery);
    TSQueryCursor cursor = new TSQueryCursor();
    cursor.exec(query, file.getTree().getRootNode());
    TSQueryMatch match = new TSQueryMatch();
    if (cursor.nextMatch(match)) {
      for (TSQueryCapture capture : match.getCaptures()) {
        TSNode node = capture.getNode();
        return Optional.of(file.getTextFromRange(node.getStartByte(), node.getEndByte()));
      }
    }
    return Optional.empty();
  }
}
