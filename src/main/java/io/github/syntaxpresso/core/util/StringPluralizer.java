package io.github.syntaxpresso.core.util;

import com.google.common.base.Strings;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.jvnet.inflector.Noun;

/**
 * Utility class for pluralizing strings while preserving their case format.
 * 
 * <p>This class provides methods to pluralize strings in various case formats such as
 * camelCase, PascalCase, snake_case, and others. It also includes methods to detect
 * whether a string is already in plural form.
 * 
 * <p>The pluralization logic uses the JVNet Inflector library for accurate English
 * plural transformations, handling irregular nouns correctly.
 * 
 * <p>All methods are static and thread-safe.
 * 
 * @since 1.0
 */
public final class StringPluralizer {

  private static final Pattern CAMEL_CASE_SPLIT_PATTERN = Pattern.compile("(?=\\p{Upper})");

  private StringPluralizer() {
    throw new UnsupportedOperationException("Utility class cannot be instantiated");
  }

  /**
   * Pluralizes a camelCase string.
   *
   * <p>This method splits the camelCase string into words, pluralizes the last word,
   * and reassembles the string maintaining the camelCase format.
   *
   * @param camelCaseStr The camelCase string to pluralize.
   * @return The pluralized camelCase string.
   * 
   * @example
   * <pre>
   * StringPluralizer.pluralizeCamelCase("userName")     // returns "userNames"
   * StringPluralizer.pluralizeCamelCase("childRecord")  // returns "childRecords"
   * StringPluralizer.pluralizeCamelCase("person")       // returns "people"
   * StringPluralizer.pluralizeCamelCase("mouse")        // returns "mice"
   * </pre>
   */
  public static String pluralizeCamelCase(String camelCaseStr) {
    if (Strings.isNullOrEmpty(camelCaseStr)) {
      return camelCaseStr;
    }
    String[] words = CAMEL_CASE_SPLIT_PATTERN.split(camelCaseStr);
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
   * <p>This method splits the PascalCase string into words, pluralizes the last word,
   * and reassembles the string maintaining the PascalCase format.
   *
   * @param pascalCaseStr The PascalCase string to pluralize.
   * @return The pluralized PascalCase string.
   * 
   * @example
   * <pre>
   * StringPluralizer.pluralizePascalCase("UserName")     // returns "UserNames"
   * StringPluralizer.pluralizePascalCase("ChildRecord")  // returns "ChildRecords"
   * StringPluralizer.pluralizePascalCase("Person")       // returns "People"
   * StringPluralizer.pluralizePascalCase("Mouse")        // returns "Mice"
   * </pre>
   */
  public static String pluralizePascalCase(String pascalCaseStr) {
    if (Strings.isNullOrEmpty(pascalCaseStr)) {
      return pascalCaseStr;
    }
    String[] words = splitIntoWords(pascalCaseStr);
    if (words.length > 0) {
      words[words.length - 1] = Noun.pluralOf(words[words.length - 1]);
      return Arrays.stream(words).map(StringPluralizer::capitalize).collect(Collectors.joining());
    }
    return pascalCaseStr;
  }

  /**
   * Pluralizes a snake_case string.
   *
   * <p>This method splits the snake_case string by underscores, pluralizes the last word,
   * and reassembles the string maintaining the snake_case format.
   *
   * @param snakeCaseStr The snake_case string to pluralize.
   * @return The pluralized snake_case string.
   * 
   * @example
   * <pre>
   * StringPluralizer.pluralizeSnakeCase("user_name")     // returns "user_names"
   * StringPluralizer.pluralizeSnakeCase("child_record")  // returns "child_records"
   * StringPluralizer.pluralizeSnakeCase("person")        // returns "people"
   * StringPluralizer.pluralizeSnakeCase("mouse")         // returns "mice"
   * </pre>
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
   * <p>This method analyzes the last word in a camelCase string to determine if it
   * appears to be in plural form by comparing it against the pluralized form of
   * its potential singular version.
   *
   * @param camelCaseStr The camelCase string to check.
   * @return true if the string appears to be plural, false otherwise.
   * 
   * @example
   * <pre>
   * StringPluralizer.isPluralCamelCase("userNames")     // returns true
   * StringPluralizer.isPluralCamelCase("userName")      // returns false
   * StringPluralizer.isPluralCamelCase("people")        // returns true
   * StringPluralizer.isPluralCamelCase("person")        // returns false
   * </pre>
   */
  public static boolean isPluralCamelCase(String camelCaseStr) {
    if (Strings.isNullOrEmpty(camelCaseStr)) {
      return false;
    }
    String[] words = CAMEL_CASE_SPLIT_PATTERN.split(camelCaseStr);
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
   * Checks if any case format string is in plural form.
   *
   * <p>This method works with strings in any case format, automatically parsing
   * them to extract the last word and determine if it appears to be plural.
   *
   * @param str The string to check (in any case format).
   * @return true if the string appears to be plural, false otherwise.
   * 
   * @example
   * <pre>
   * StringPluralizer.isPlural("userNames")      // returns true
   * StringPluralizer.isPlural("user_names")     // returns true
   * StringPluralizer.isPlural("UserNames")      // returns true
   * StringPluralizer.isPlural("user-names")     // returns true
   * StringPluralizer.isPlural("people")         // returns true
   * StringPluralizer.isPlural("person")         // returns false
   * </pre>
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
   * <p>This method automatically detects the case format of the input string,
   * pluralizes the last word, and returns the result in the same case format.
   *
   * @param str The string to pluralize (in any case format).
   * @return The pluralized string in the same case format.
   * 
   * @example
   * <pre>
   * StringPluralizer.pluralize("userName")      // returns "userNames"
   * StringPluralizer.pluralize("UserName")      // returns "UserNames"
   * StringPluralizer.pluralize("user_name")     // returns "user_names"
   * StringPluralizer.pluralize("USER_NAME")     // returns "USER_NAMES"
   * StringPluralizer.pluralize("user-name")     // returns "user-names"
   * StringPluralizer.pluralize("user.name")     // returns "user.names"
   * StringPluralizer.pluralize("User Name")     // returns "User Names"
   * StringPluralizer.pluralize("User name")     // returns "User names"
   * </pre>
   */
  public static String pluralize(String str) {
    if (Strings.isNullOrEmpty(str)) {
      return str;
    }

    StringHelper.CaseFormat format = CaseDetector.detectFormat(str);
    String[] words = splitIntoWords(str);

    if (words.length > 0) {
      words[words.length - 1] = Noun.pluralOf(words[words.length - 1]);
    }

    switch (format) {
      case CAMEL_CASE:
        return CaseConverter.toCamelCase(String.join(" ", words));
      case PASCAL_CASE:
        return CaseConverter.toPascalCase(String.join(" ", words));
      case SNAKE_CASE:
        return CaseConverter.toSnakeCase(String.join(" ", words));
      case SCREAMING_SNAKE_CASE:
        return CaseConverter.toScreamingSnakeCase(String.join(" ", words));
      case KEBAB_CASE:
        return CaseConverter.toKebabCase(String.join(" ", words));
      case DOT_CASE:
        return CaseConverter.toDotCase(String.join(" ", words));
      case TITLE_CASE:
        return CaseConverter.toTitleCase(String.join(" ", words));
      case SENTENCE_CASE:
        return CaseConverter.toSentenceCase(String.join(" ", words));
      default:
        return String.join("_", words);
    }
  }

  /**
   * Helper method to capitalize the first letter of a word while making the rest lowercase.
   *
   * @param word The word to capitalize.
   * @return The capitalized word.
   */
  private static String capitalize(String word) {
    if (word == null || word.isEmpty()) return word;
    return Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase();
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
    Pattern camelPattern = Pattern.compile("([a-z])([A-Z])");
    String withSpaces = camelPattern.matcher(input).replaceAll("$1 $2");
    withSpaces = withSpaces.replaceAll("[_\\-\\.]+", " ");
    return Arrays.stream(withSpaces.split("\\s+")).filter(s -> !s.isEmpty()).toArray(String[]::new);
  }
}