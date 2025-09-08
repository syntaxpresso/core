package io.github.syntaxpresso.core.util;

import com.google.common.base.Strings;
import io.github.syntaxpresso.core.util.extra.CaseFormat;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Utility class for converting strings between different case formats.
 * 
 * <p>This class provides methods to convert strings from any case format to any other case format,
 * including camelCase, PascalCase, snake_case, SCREAMING_SNAKE_CASE, kebab-case, dot.case,
 * Title Case, and Sentence case.
 * 
 * <p>All methods are static and thread-safe. The class handles the parsing of input strings
 * automatically, regardless of their original format.
 * 
 * @since 1.0
 */
public final class CaseConverter {

  private static final Pattern CAMEL_PATTERN = Pattern.compile("([a-z])([A-Z])");

  private CaseConverter() {
    throw new UnsupportedOperationException("Utility class cannot be instantiated");
  }

  /**
   * Converts any string to camelCase.
   *
   * <p>CamelCase format starts with a lowercase letter, with subsequent words capitalized
   * and no separators between words.
   *
   * @param input The input string in any format.
   * @return The converted string in camelCase.
   * 
   * @example
   * <pre>
   * CaseConverter.toCamelCase("hello_world")  // returns "helloWorld"
   * CaseConverter.toCamelCase("HelloWorld")   // returns "helloWorld"
   * CaseConverter.toCamelCase("hello-world")  // returns "helloWorld"
   * CaseConverter.toCamelCase("Hello World")  // returns "helloWorld"
   * </pre>
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
   * Converts any string to PascalCase (UpperCamelCase).
   *
   * <p>PascalCase format starts with an uppercase letter, with subsequent words capitalized
   * and no separators between words.
   *
   * @param input The input string in any format.
   * @return The converted string in PascalCase.
   * 
   * @example
   * <pre>
   * CaseConverter.toPascalCase("hello_world")  // returns "HelloWorld"
   * CaseConverter.toPascalCase("helloWorld")   // returns "HelloWorld"
   * CaseConverter.toPascalCase("hello-world")  // returns "HelloWorld"
   * CaseConverter.toPascalCase("hello world")  // returns "HelloWorld"
   * </pre>
   */
  public static String toPascalCase(String input) {
    if (Strings.isNullOrEmpty(input)) {
      return input;
    }

    String[] words = splitIntoWords(input);
    return Arrays.stream(words).map(CaseConverter::capitalize).collect(Collectors.joining());
  }

  /**
   * Converts any string to snake_case.
   *
   * <p>Snake_case format uses lowercase letters with words separated by underscores.
   *
   * @param input The input string in any format.
   * @return The converted string in snake_case.
   * 
   * @example
   * <pre>
   * CaseConverter.toSnakeCase("helloWorld")   // returns "hello_world"
   * CaseConverter.toSnakeCase("HelloWorld")   // returns "hello_world"
   * CaseConverter.toSnakeCase("hello-world")  // returns "hello_world"
   * CaseConverter.toSnakeCase("Hello World")  // returns "hello_world"
   * </pre>
   */
  public static String toSnakeCase(String input) {
    if (Strings.isNullOrEmpty(input)) {
      return input;
    }

    String[] words = splitIntoWords(input);
    return Arrays.stream(words).map(String::toLowerCase).collect(Collectors.joining("_"));
  }

  /**
   * Converts any string to SCREAMING_SNAKE_CASE (CONSTANT_CASE).
   *
   * <p>SCREAMING_SNAKE_CASE format uses uppercase letters with words separated by underscores.
   * This format is commonly used for constants.
   *
   * @param input The input string in any format.
   * @return The converted string in SCREAMING_SNAKE_CASE.
   * 
   * @example
   * <pre>
   * CaseConverter.toScreamingSnakeCase("helloWorld")   // returns "HELLO_WORLD"
   * CaseConverter.toScreamingSnakeCase("hello-world")  // returns "HELLO_WORLD"
   * CaseConverter.toScreamingSnakeCase("Hello World")  // returns "HELLO_WORLD"
   * </pre>
   */
  public static String toScreamingSnakeCase(String input) {
    if (Strings.isNullOrEmpty(input)) {
      return input;
    }

    String[] words = splitIntoWords(input);
    return Arrays.stream(words).map(String::toUpperCase).collect(Collectors.joining("_"));
  }

  /**
   * Converts any string to kebab-case.
   *
   * <p>Kebab-case format uses lowercase letters with words separated by hyphens.
   * This format is commonly used in URLs and CSS classes.
   *
   * @param input The input string in any format.
   * @return The converted string in kebab-case.
   * 
   * @example
   * <pre>
   * CaseConverter.toKebabCase("helloWorld")   // returns "hello-world"
   * CaseConverter.toKebabCase("HelloWorld")   // returns "hello-world"
   * CaseConverter.toKebabCase("hello_world")  // returns "hello-world"
   * CaseConverter.toKebabCase("Hello World")  // returns "hello-world"
   * </pre>
   */
  public static String toKebabCase(String input) {
    if (Strings.isNullOrEmpty(input)) {
      return input;
    }

    String[] words = splitIntoWords(input);
    return Arrays.stream(words).map(String::toLowerCase).collect(Collectors.joining("-"));
  }

  /**
   * Converts any string to dot.case.
   *
   * <p>Dot.case format uses lowercase letters with words separated by dots.
   *
   * @param input The input string in any format.
   * @return The converted string in dot.case.
   * 
   * @example
   * <pre>
   * CaseConverter.toDotCase("helloWorld")   // returns "hello.world"
   * CaseConverter.toDotCase("HelloWorld")   // returns "hello.world"
   * CaseConverter.toDotCase("hello_world")  // returns "hello.world"
   * CaseConverter.toDotCase("Hello World")  // returns "hello.world"
   * </pre>
   */
  public static String toDotCase(String input) {
    if (Strings.isNullOrEmpty(input)) {
      return input;
    }

    String[] words = splitIntoWords(input);
    return Arrays.stream(words).map(String::toLowerCase).collect(Collectors.joining("."));
  }

  /**
   * Converts any string to Title Case.
   *
   * <p>Title Case format capitalizes the first letter of each word with words separated by spaces.
   *
   * @param input The input string in any format.
   * @return The converted string in Title Case.
   * 
   * @example
   * <pre>
   * CaseConverter.toTitleCase("hello_world")  // returns "Hello World"
   * CaseConverter.toTitleCase("helloWorld")   // returns "Hello World"
   * CaseConverter.toTitleCase("hello-world")  // returns "Hello World"
   * </pre>
   */
  public static String toTitleCase(String input) {
    if (Strings.isNullOrEmpty(input)) {
      return input;
    }

    String[] words = splitIntoWords(input);
    return Arrays.stream(words).map(CaseConverter::capitalize).collect(Collectors.joining(" "));
  }

  /**
   * Converts any string to Sentence case.
   *
   * <p>Sentence case format capitalizes only the first word, with subsequent words in lowercase
   * and words separated by spaces.
   *
   * @param input The input string in any format.
   * @return The converted string in Sentence case.
   * 
   * @example
   * <pre>
   * CaseConverter.toSentenceCase("hello_world")  // returns "Hello world"
   * CaseConverter.toSentenceCase("HelloWorld")   // returns "Hello world"
   * CaseConverter.toSentenceCase("HELLO_WORLD")  // returns "Hello world"
   * </pre>
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
   * Converts a camelCase string to PascalCase.
   *
   * <p>This is a specialized method for when you know the input is already camelCase
   * and want to convert it to PascalCase efficiently.
   *
   * @param camelCaseStr The string in camelCase.
   * @return The converted string in PascalCase.
   * 
   * @example
   * <pre>
   * CaseConverter.camelToPascal("helloWorld")  // returns "HelloWorld"
   * CaseConverter.camelToPascal("userName")    // returns "UserName"
   * </pre>
   */
  public static String camelToPascal(String camelCaseStr) {
    if (Strings.isNullOrEmpty(camelCaseStr)) {
      return camelCaseStr;
    }
    return Character.toUpperCase(camelCaseStr.charAt(0)) + camelCaseStr.substring(1);
  }

  /**
   * Converts a PascalCase string to camelCase.
   *
   * <p>This is a specialized method for when you know the input is already PascalCase
   * and want to convert it to camelCase efficiently.
   *
   * @param pascalCaseStr The string in PascalCase.
   * @return The converted string in camelCase.
   * 
   * @example
   * <pre>
   * CaseConverter.pascalToCamel("HelloWorld")  // returns "helloWorld"
   * CaseConverter.pascalToCamel("UserName")    // returns "userName"
   * </pre>
   */
  public static String pascalToCamel(String pascalCaseStr) {
    if (Strings.isNullOrEmpty(pascalCaseStr)) {
      return pascalCaseStr;
    }
    return Character.toLowerCase(pascalCaseStr.charAt(0)) + pascalCaseStr.substring(1);
  }

  /**
   * Converts from any format to any other format.
   *
   * <p>This is a universal conversion method that can convert from any supported case format
   * to any other supported case format.
   *
   * @param input The input string in any format.
   * @param targetFormat The target case format.
   * @return The converted string in the target format.
   * 
   * @example
   * <pre>
   * CaseConverter.convert("hello_world", CaseFormat.CAMEL_CASE)    // returns "helloWorld"
   * CaseConverter.convert("HelloWorld", CaseFormat.SNAKE_CASE)     // returns "hello_world"
   * CaseConverter.convert("hello-world", CaseFormat.PASCAL_CASE)   // returns "HelloWorld"
   * </pre>
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

  /**
   * Splits any format string into individual words.
   *
   * <p>This method handles camelCase, PascalCase, snake_case, kebab-case, dot.case,
   * and space-separated formats.
   *
   * @param input The input string to split.
   * @return An array of individual words.
   */
  private static String[] splitIntoWords(String input) {
    String withSpaces = CAMEL_PATTERN.matcher(input).replaceAll("$1 $2");
    withSpaces = withSpaces.replaceAll("[_\\-\\.]+", " ");
    return Arrays.stream(withSpaces.split("\\s+")).filter(s -> !s.isEmpty()).toArray(String[]::new);
  }

  /**
   * Capitalizes the first letter of a word while making the rest lowercase.
   *
   * @param word The word to capitalize.
   * @return The capitalized word.
   */
  private static String capitalize(String word) {
    if (word == null || word.isEmpty()) return word;
    return Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase();
  }
}