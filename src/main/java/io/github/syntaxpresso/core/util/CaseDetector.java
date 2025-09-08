package io.github.syntaxpresso.core.util;

import com.google.common.base.Strings;
import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * Utility class for detecting string case formats.
 * 
 * <p>This class provides methods to identify whether a string follows specific case conventions
 * such as camelCase, PascalCase, snake_case, SCREAMING_SNAKE_CASE, kebab-case, etc.
 * 
 * <p>All methods are static and thread-safe as they use pre-compiled regex patterns.
 * 
 * @since 1.0
 */
public final class CaseDetector {

  private static final Pattern CAMEL_PATTERN = Pattern.compile("([a-z])([A-Z])");
  private static final Pattern PASCAL_CASE_PATTERN =
      Pattern.compile("^[A-Z][a-z0-9]*([A-Z][a-z0-9]*)*$");
  private static final Pattern CAMEL_CASE_PATTERN = Pattern.compile("^[a-z]+([A-Z][a-z0-9]*)*$");
  private static final Pattern SNAKE_CASE_PATTERN = Pattern.compile("^[a-z]+(_[a-z0-9]+)*$");
  private static final Pattern SCREAMING_SNAKE_CASE_PATTERN = Pattern.compile("^[A-Z]+(_[A-Z0-9]+)*$");
  private static final Pattern KEBAB_CASE_PATTERN = Pattern.compile("^[a-z]+(-[a-z0-9]+)*$");

  private CaseDetector() {
    throw new UnsupportedOperationException("Utility class cannot be instantiated");
  }

  /**
   * Checks if a string is in PascalCase (also known as UpperCamelCase).
   *
   * <p>PascalCase strings start with an uppercase letter followed by lowercase letters,
   * with subsequent words starting with uppercase letters.
   *
   * @param str The string to check.
   * @return {@code true} if the string is in PascalCase, {@code false} otherwise.
   * 
   * @example
   * <pre>
   * CaseDetector.isPascalCase("HelloWorld") // returns true
   * CaseDetector.isPascalCase("helloWorld") // returns false
   * CaseDetector.isPascalCase("HELLO")      // returns true
   * </pre>
   */
  public static boolean isPascalCase(String str) {
    if (Strings.isNullOrEmpty(str)) {
      return false;
    }
    return PASCAL_CASE_PATTERN.matcher(str).matches();
  }

  /**
   * Checks if a string is in camelCase.
   *
   * <p>CamelCase strings start with a lowercase letter followed by lowercase letters,
   * with subsequent words starting with uppercase letters.
   *
   * @param str The string to check.
   * @return {@code true} if the string is in camelCase, {@code false} otherwise.
   * 
   * @example
   * <pre>
   * CaseDetector.isCamelCase("helloWorld") // returns true
   * CaseDetector.isCamelCase("HelloWorld") // returns false
   * CaseDetector.isCamelCase("hello")      // returns true
   * </pre>
   */
  public static boolean isCamelCase(String str) {
    if (Strings.isNullOrEmpty(str)) {
      return false;
    }
    return CAMEL_CASE_PATTERN.matcher(str).matches();
  }

  /**
   * Checks if a string is in snake_case.
   *
   * <p>Snake_case strings use lowercase letters and numbers, with words separated by underscores.
   *
   * @param str The string to check.
   * @return {@code true} if the string is in snake_case, {@code false} otherwise.
   * 
   * @example
   * <pre>
   * CaseDetector.isSnakeCase("hello_world")    // returns true
   * CaseDetector.isSnakeCase("hello_world_123") // returns true
   * CaseDetector.isSnakeCase("HelloWorld")     // returns false
   * </pre>
   */
  public static boolean isSnakeCase(String str) {
    if (Strings.isNullOrEmpty(str)) {
      return false;
    }
    return SNAKE_CASE_PATTERN.matcher(str).matches();
  }

  /**
   * Checks if a string is in SCREAMING_SNAKE_CASE.
   *
   * <p>SCREAMING_SNAKE_CASE strings use uppercase letters and numbers, with words separated by underscores.
   * This format is commonly used for constants.
   *
   * @param str The string to check.
   * @return {@code true} if the string is in SCREAMING_SNAKE_CASE, {@code false} otherwise.
   * 
   * @example
   * <pre>
   * CaseDetector.isScreamingSnakeCase("HELLO_WORLD")    // returns true
   * CaseDetector.isScreamingSnakeCase("MAX_VALUE_123")  // returns true
   * CaseDetector.isScreamingSnakeCase("hello_world")    // returns false
   * </pre>
   */
  public static boolean isScreamingSnakeCase(String str) {
    if (Strings.isNullOrEmpty(str)) {
      return false;
    }
    return SCREAMING_SNAKE_CASE_PATTERN.matcher(str).matches();
  }

  /**
   * Checks if a string is in kebab-case.
   *
   * <p>Kebab-case strings use lowercase letters and numbers, with words separated by hyphens.
   * This format is commonly used in URLs and CSS classes.
   *
   * @param str The string to check.
   * @return {@code true} if the string is in kebab-case, {@code false} otherwise.
   * 
   * @example
   * <pre>
   * CaseDetector.isKebabCase("hello-world")    // returns true
   * CaseDetector.isKebabCase("hello-world-123") // returns true
   * CaseDetector.isKebabCase("HelloWorld")     // returns false
   * </pre>
   */
  public static boolean isKebabCase(String str) {
    if (Strings.isNullOrEmpty(str)) {
      return false;
    }
    return KEBAB_CASE_PATTERN.matcher(str).matches();
  }

  /**
   * Detects the case format of the input string.
   *
   * <p>This method analyzes the input string and returns the most likely case format
   * based on the presence of separators and capitalization patterns.
   *
   * @param input The string to analyze.
   * @return The detected CaseFormat enum value.
   * 
   * @example
   * <pre>
   * CaseDetector.detectFormat("helloWorld")    // returns CaseFormat.CAMEL_CASE
   * CaseDetector.detectFormat("HelloWorld")    // returns CaseFormat.PASCAL_CASE
   * CaseDetector.detectFormat("hello_world")   // returns CaseFormat.SNAKE_CASE
   * CaseDetector.detectFormat("HELLO_WORLD")   // returns CaseFormat.SCREAMING_SNAKE_CASE
   * CaseDetector.detectFormat("hello-world")   // returns CaseFormat.KEBAB_CASE
   * CaseDetector.detectFormat("hello.world")   // returns CaseFormat.DOT_CASE
   * CaseDetector.detectFormat("Hello World")   // returns CaseFormat.TITLE_CASE
   * CaseDetector.detectFormat("Hello world")   // returns CaseFormat.SENTENCE_CASE
   * </pre>
   */
  public static StringHelper.CaseFormat detectFormat(String input) {
    if (Strings.isNullOrEmpty(input)) {
      return StringHelper.CaseFormat.UNKNOWN;
    }

    if (input.contains("_")) {
      if (input.equals(input.toUpperCase())) {
        return StringHelper.CaseFormat.SCREAMING_SNAKE_CASE;
      }
      return StringHelper.CaseFormat.SNAKE_CASE;
    }

    if (input.contains("-")) {
      return StringHelper.CaseFormat.KEBAB_CASE;
    }

    if (input.contains(".")) {
      return StringHelper.CaseFormat.DOT_CASE;
    }

    if (input.contains(" ")) {
      String[] words = input.split("\\s+");
      boolean allCapitalized =
          Arrays.stream(words).allMatch(w -> w.isEmpty() || Character.isUpperCase(w.charAt(0)));
      return allCapitalized ? StringHelper.CaseFormat.TITLE_CASE : StringHelper.CaseFormat.SENTENCE_CASE;
    }

    if (Character.isUpperCase(input.charAt(0))) {
      return StringHelper.CaseFormat.PASCAL_CASE;
    } else if (CAMEL_PATTERN.matcher(input).find()) {
      return StringHelper.CaseFormat.CAMEL_CASE;
    }

    return StringHelper.CaseFormat.UNKNOWN;
  }
}