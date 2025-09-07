package io.github.syntaxpresso.core.util;

import com.google.common.base.Strings;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.jvnet.inflector.Noun;

public class StringHelper {

  // Patterns for detecting and converting different case formats
  private static final Pattern CAMEL_PATTERN = Pattern.compile("([a-z])([A-Z])");
  private static final Pattern PASCAL_CASE_PATTERN =
      Pattern.compile("^[A-Z][a-z0-9]*([A-Z][a-z0-9]*)*$");
  private static final Pattern CAMEL_CASE_PATTERN = Pattern.compile("^[a-z]+([A-Z][a-z0-9]*)*$");

  /**
   * Checks if a string is in PascalCase (also known as UpperCamelCase).
   *
   * @param str The string to check.
   * @return {@code true} if the string is in PascalCase, {@code false} otherwise.
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
   * @param str The string to check.
   * @return {@code true} if the string is in camelCase, {@code false} otherwise.
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
   * @param str The string to check.
   * @return {@code true} if the string is in snake_case, {@code false} otherwise.
   */
  public static boolean isSnakeCase(String str) {
    if (Strings.isNullOrEmpty(str)) {
      return false;
    }
    return str.matches("^[a-z]+(_[a-z0-9]+)*$");
  }

  /**
   * Checks if a string is in SCREAMING_SNAKE_CASE.
   *
   * @param str The string to check.
   * @return {@code true} if the string is in SCREAMING_SNAKE_CASE, {@code false} otherwise.
   */
  public static boolean isScreamingSnakeCase(String str) {
    if (Strings.isNullOrEmpty(str)) {
      return false;
    }
    return str.matches("^[A-Z]+(_[A-Z0-9]+)*$");
  }

  /**
   * Checks if a string is in kebab-case.
   *
   * @param str The string to check.
   * @return {@code true} if the string is in kebab-case, {@code false} otherwise.
   */
  public static boolean isKebabCase(String str) {
    if (Strings.isNullOrEmpty(str)) {
      return false;
    }
    return str.matches("^[a-z]+(-[a-z0-9]+)*$");
  }

  /**
   * Converts any string to camelCase. Examples: "hello_world" -> "helloWorld", "HelloWorld" ->
   * "helloWorld"
   *
   * @param input The input string in any format.
   * @return The converted string in camelCase.
   */
  public static String toCamelCase(String input) {
    if (Strings.isNullOrEmpty(input)) {
      return input;
    }

    String[] words = splitIntoWords(input);
    if (words.length == 0) return "";

    StringBuilder result = new StringBuilder(words[0].toLowerCase());
    for (int i = 1; i < words.length; i++) {
      result.append(capitalize(words[i]));
    }
    return result.toString();
  }

  /**
   * Converts any string to PascalCase (UpperCamelCase). Examples: "hello_world" -> "HelloWorld",
   * "helloWorld" -> "HelloWorld"
   *
   * @param input The input string in any format.
   * @return The converted string in PascalCase.
   */
  public static String toPascalCase(String input) {
    if (Strings.isNullOrEmpty(input)) {
      return input;
    }

    String[] words = splitIntoWords(input);
    return Arrays.stream(words).map(StringHelper::capitalize).collect(Collectors.joining());
  }

  /**
   * Converts any string to snake_case. Examples: "helloWorld" -> "hello_world", "HelloWorld" ->
   * "hello_world"
   *
   * @param input The input string in any format.
   * @return The converted string in snake_case.
   */
  public static String toSnakeCase(String input) {
    if (Strings.isNullOrEmpty(input)) {
      return input;
    }

    String[] words = splitIntoWords(input);
    return Arrays.stream(words).map(String::toLowerCase).collect(Collectors.joining("_"));
  }

  /**
   * Converts any string to SCREAMING_SNAKE_CASE (CONSTANT_CASE). Examples: "helloWorld" ->
   * "HELLO_WORLD", "hello-world" -> "HELLO_WORLD"
   *
   * @param input The input string in any format.
   * @return The converted string in SCREAMING_SNAKE_CASE.
   */
  public static String toScreamingSnakeCase(String input) {
    if (Strings.isNullOrEmpty(input)) {
      return input;
    }

    String[] words = splitIntoWords(input);
    return Arrays.stream(words).map(String::toUpperCase).collect(Collectors.joining("_"));
  }

  /**
   * Converts any string to kebab-case. Examples: "helloWorld" -> "hello-world", "HelloWorld" ->
   * "hello-world"
   *
   * @param input The input string in any format.
   * @return The converted string in kebab-case.
   */
  public static String toKebabCase(String input) {
    if (Strings.isNullOrEmpty(input)) {
      return input;
    }

    String[] words = splitIntoWords(input);
    return Arrays.stream(words).map(String::toLowerCase).collect(Collectors.joining("-"));
  }

  /**
   * Converts any string to dot.case. Examples: "helloWorld" -> "hello.world", "HelloWorld" ->
   * "hello.world"
   *
   * @param input The input string in any format.
   * @return The converted string in dot.case.
   */
  public static String toDotCase(String input) {
    if (Strings.isNullOrEmpty(input)) {
      return input;
    }

    String[] words = splitIntoWords(input);
    return Arrays.stream(words).map(String::toLowerCase).collect(Collectors.joining("."));
  }

  /**
   * Converts any string to Title Case. Examples: "hello_world" -> "Hello World", "helloWorld" ->
   * "Hello World"
   *
   * @param input The input string in any format.
   * @return The converted string in Title Case.
   */
  public static String toTitleCase(String input) {
    if (Strings.isNullOrEmpty(input)) {
      return input;
    }

    String[] words = splitIntoWords(input);
    return Arrays.stream(words).map(StringHelper::capitalize).collect(Collectors.joining(" "));
  }

  /**
   * Converts any string to Sentence case. Examples: "hello_world" -> "Hello world", "HelloWorld" ->
   * "Hello world"
   *
   * @param input The input string in any format.
   * @return The converted string in Sentence case.
   */
  public static String toSentenceCase(String input) {
    if (Strings.isNullOrEmpty(input)) {
      return input;
    }

    String[] words = splitIntoWords(input);
    if (words.length == 0) return "";

    StringBuilder result = new StringBuilder(capitalize(words[0]));
    for (int i = 1; i < words.length; i++) {
      result.append(" ").append(words[i].toLowerCase());
    }
    return result.toString();
  }

  /**
   * Converts a camelCase string to PascalCase. Simplified version for when you know the input is
   * already camelCase.
   *
   * @param camelCaseStr The string in camelCase.
   * @return The converted string in PascalCase.
   */
  public static String camelToPascal(String camelCaseStr) {
    if (Strings.isNullOrEmpty(camelCaseStr)) {
      return camelCaseStr;
    }
    return Character.toUpperCase(camelCaseStr.charAt(0)) + camelCaseStr.substring(1);
  }

  /**
   * Converts a PascalCase string to camelCase. Simplified version for when you know the input is
   * already PascalCase.
   *
   * @param pascalCaseStr The string in PascalCase.
   * @return The converted string in camelCase.
   */
  public static String pascalToCamel(String pascalCaseStr) {
    if (Strings.isNullOrEmpty(pascalCaseStr)) {
      return pascalCaseStr;
    }
    return Character.toLowerCase(pascalCaseStr.charAt(0)) + pascalCaseStr.substring(1);
  }

  /**
   * Pluralizes a camelCase string.
   *
   * @param camelCaseStr The camelCase string to pluralize.
   * @return The pluralized camelCase string.
   */
  public static String pluralizeCamelCase(String camelCaseStr) {
    if (Strings.isNullOrEmpty(camelCaseStr)) {
      return camelCaseStr;
    }
    String[] words = camelCaseStr.split("(?=\\p{Upper})");
    if (words.length > 0) {
      int lastWordIndex = words.length - 1;
      words[lastWordIndex] = Noun.pluralOf(words[lastWordIndex]);
      return String.join("", words);
    }
    return camelCaseStr;
  }

  /**
   * Pluralizes a PascalCase string.
   *
   * @param pascalCaseStr The PascalCase string to pluralize.
   * @return The pluralized PascalCase string.
   */
  public static String pluralizePascalCase(String pascalCaseStr) {
    if (Strings.isNullOrEmpty(pascalCaseStr)) {
      return pascalCaseStr;
    }
    String[] words = splitIntoWords(pascalCaseStr);
    if (words.length > 0) {
      words[words.length - 1] = Noun.pluralOf(words[words.length - 1]);
      return Arrays.stream(words).map(StringHelper::capitalize).collect(Collectors.joining());
    }
    return pascalCaseStr;
  }

  /**
   * Pluralizes a snake_case string.
   *
   * @param snakeCaseStr The snake_case string to pluralize.
   * @return The pluralized snake_case string.
   */
  public static String pluralizeSnakeCase(String snakeCaseStr) {
    if (Strings.isNullOrEmpty(snakeCaseStr)) {
      return snakeCaseStr;
    }
    String[] words = snakeCaseStr.split("_");
    if (words.length > 0) {
      words[words.length - 1] = Noun.pluralOf(words[words.length - 1]);
      return String.join("_", words);
    }
    return snakeCaseStr;
  }

  /**
   * Checks if a camelCase string is in plural form.
   *
   * @param camelCaseStr The camelCase string to check.
   * @return true if the string appears to be plural, false otherwise.
   */
  public static boolean isPluralCamelCase(String camelCaseStr) {
    if (Strings.isNullOrEmpty(camelCaseStr)) {
      return false;
    }
    // Split camelCase to get the last word
    String[] words = camelCaseStr.split("(?=\\p{Upper})");
    if (words.length > 0) {
      String lastWord = words[words.length - 1].toLowerCase();
      // Check if the pluralized form of the singular would match this word
      // This is more reliable than trying to detect if a word is already plural
      String withoutS =
          lastWord.endsWith("s") ? lastWord.substring(0, lastWord.length() - 1) : lastWord;
      String pluralOfSingular = Noun.pluralOf(withoutS).toLowerCase();
      return lastWord.equals(pluralOfSingular);
    }
    return false;
  }

  /**
   * Checks if any case format string is in plural form.
   *
   * @param str The string to check (in any case format).
   * @return true if the string appears to be plural, false otherwise.
   */
  public static boolean isPlural(String str) {
    if (Strings.isNullOrEmpty(str)) {
      return false;
    }
    String[] words = splitIntoWords(str);
    if (words.length > 0) {
      String lastWord = words[words.length - 1].toLowerCase();
      String withoutS =
          lastWord.endsWith("s") ? lastWord.substring(0, lastWord.length() - 1) : lastWord;
      String pluralOfSingular = Noun.pluralOf(withoutS).toLowerCase();
      return lastWord.equals(pluralOfSingular);
    }
    return false;
  }

  /**
   * Pluralizes a string in any case format while preserving the case format.
   *
   * @param str The string to pluralize (in any case format).
   * @return The pluralized string in the same case format.
   */
  public static String pluralize(String str) {
    if (Strings.isNullOrEmpty(str)) {
      return str;
    }

    CaseFormat format = detectFormat(str);
    String[] words = splitIntoWords(str);

    if (words.length > 0) {
      words[words.length - 1] = Noun.pluralOf(words[words.length - 1]);
    }

    // Rebuild in the original format
    switch (format) {
      case CAMEL_CASE:
        return toCamelCase(String.join(" ", words));
      case PASCAL_CASE:
        return toPascalCase(String.join(" ", words));
      case SNAKE_CASE:
        return toSnakeCase(String.join(" ", words));
      case SCREAMING_SNAKE_CASE:
        return toScreamingSnakeCase(String.join(" ", words));
      case KEBAB_CASE:
        return toKebabCase(String.join(" ", words));
      case DOT_CASE:
        return toDotCase(String.join(" ", words));
      case TITLE_CASE:
        return toTitleCase(String.join(" ", words));
      case SENTENCE_CASE:
        return toSentenceCase(String.join(" ", words));
      default:
        return String.join("_", words);
    }
  }

  /**
   * Detects the case format of the input string.
   *
   * @param input The string to analyze.
   * @return The detected CaseFormat enum value.
   */
  public static CaseFormat detectFormat(String input) {
    if (Strings.isNullOrEmpty(input)) {
      return CaseFormat.UNKNOWN;
    }

    // Check for snake_case or SCREAMING_SNAKE_CASE
    if (input.contains("_")) {
      if (input.equals(input.toUpperCase())) {
        return CaseFormat.SCREAMING_SNAKE_CASE;
      }
      return CaseFormat.SNAKE_CASE;
    }

    // Check for kebab-case
    if (input.contains("-")) {
      return CaseFormat.KEBAB_CASE;
    }

    // Check for dot.case
    if (input.contains(".")) {
      return CaseFormat.DOT_CASE;
    }

    // Check for spaces (Title Case or Sentence case)
    if (input.contains(" ")) {
      String[] words = input.split("\\s+");
      boolean allCapitalized =
          Arrays.stream(words).allMatch(w -> w.isEmpty() || Character.isUpperCase(w.charAt(0)));
      return allCapitalized ? CaseFormat.TITLE_CASE : CaseFormat.SENTENCE_CASE;
    }

    // Check for PascalCase or camelCase
    if (Character.isUpperCase(input.charAt(0))) {
      return CaseFormat.PASCAL_CASE;
    } else if (CAMEL_PATTERN.matcher(input).find()) {
      return CaseFormat.CAMEL_CASE;
    }

    return CaseFormat.UNKNOWN;
  }

  /**
   * Converts from any format to any other format.
   *
   * @param input The input string in any format.
   * @param targetFormat The target case format.
   * @return The converted string in the target format.
   */
  public static String convert(String input, CaseFormat targetFormat) {
    switch (targetFormat) {
      case CAMEL_CASE:
        return toCamelCase(input);
      case PASCAL_CASE:
        return toPascalCase(input);
      case SNAKE_CASE:
        return toSnakeCase(input);
      case SCREAMING_SNAKE_CASE:
        return toScreamingSnakeCase(input);
      case KEBAB_CASE:
        return toKebabCase(input);
      case DOT_CASE:
        return toDotCase(input);
      case TITLE_CASE:
        return toTitleCase(input);
      case SENTENCE_CASE:
        return toSentenceCase(input);
      default:
        return input;
    }
  }

  // Helper method to split any format into words
  private static String[] splitIntoWords(String input) {
    // First, handle camelCase and PascalCase by inserting spaces before capitals
    String withSpaces = CAMEL_PATTERN.matcher(input).replaceAll("$1 $2");

    // Replace all delimiters with spaces
    withSpaces = withSpaces.replaceAll("[_\\-\\.]+", " ");

    // Split by spaces and filter out empty strings
    return Arrays.stream(withSpaces.split("\\s+")).filter(s -> !s.isEmpty()).toArray(String[]::new);
  }

  // Helper method to capitalize first letter
  private static String capitalize(String word) {
    if (word == null || word.isEmpty()) return word;
    return Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase();
  }

  /** Enum for different case formats. */
  public enum CaseFormat {
    CAMEL_CASE,
    PASCAL_CASE,
    SNAKE_CASE,
    SCREAMING_SNAKE_CASE,
    KEBAB_CASE,
    DOT_CASE,
    TITLE_CASE,
    SENTENCE_CASE,
    UNKNOWN
  }
}
