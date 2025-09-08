package io.github.syntaxpresso.core.common.extra;

import java.util.HashMap;
import java.util.Map;
import org.treesitter.TSLanguage;
import org.treesitter.TSParser;

/**
 * A factory for TSParser instances. This utility class provides thread-safe, reusable Tree-sitter
 * parsers for multiple languages.
 *
 * <p>This class uses a {@link ThreadLocal} to hold a cache (a {@link Map}) of parsers for each
 * thread. This avoids concurrency issues while efficiently reusing parser instances on a
 * per-language, per-thread basis.
 */
public final class ParserFactory {
  // A ThreadLocal holding a Map where each thread can store its own set of parsers.
  // The key is the TSLanguage, and the value is the configured TSParser.
  private static final ThreadLocal<Map<TSLanguage, TSParser>> PARSERS =
      ThreadLocal.withInitial(HashMap::new);

  /** Private constructor to prevent instantiation. */
  private ParserFactory() {
    // This class is not meant to be instantiated.
  }

  /**
   * Retrieves a parser instance for the current thread, configured for the specified language.
   *
   * <p>If a parser for the given language has already been created for the current thread, the
   * existing instance is returned. Otherwise, a new one is created, configured, and cached for
   * future use by the thread.
   *
   * @param supportedLanguage The {@link SupportedLanguage} grammar to configure the parser with.
   * @return A thread-safe {@link TSParser} configured for the specified language.
   */
  public static TSParser get(SupportedLanguage supportedLanguage) {
    // Get the map for the current thread.
    Map<TSLanguage, TSParser> parserMap = PARSERS.get();
    // Use computeIfAbsent to get the existing parser or create a new one.
    // This is an atomic and clean way to handle the "get or create" logic.
    return parserMap.computeIfAbsent(
        supportedLanguage.getLanguage(),
        lang -> {
          TSParser parser = new TSParser();
          parser.setLanguage(lang);
          return parser;
        });
  }
}
