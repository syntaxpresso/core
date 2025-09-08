package io.github.syntaxpresso.core.util;

import io.github.syntaxpresso.core.util.extra.CaseFormat;

/**
 * Facade utility class providing convenient access to string manipulation operations.
 * 
 * <p>This class serves as a central entry point for string case conversion, detection,
 * and pluralization operations. It delegates to specialized classes to maintain
 * separation of concerns while providing a unified API for backward compatibility.
 * 
 * <p>All methods are static and thread-safe.
 * 
 * @since 1.0
 * @see CaseDetector for case format detection operations
 * @see CaseConverter for case format conversion operations  
 * @see StringPluralizer for pluralization operations
 */
public final class StringHelper {

  private StringHelper() {
    throw new UnsupportedOperationException("Utility class cannot be instantiated");
  }

  // Case Detection Methods - Delegated to CaseDetector

  /**
   * Checks if a string is in PascalCase (also known as UpperCamelCase).
   *
   * @param str The string to check.
   * @return {@code true} if the string is in PascalCase, {@code false} otherwise.
   * @see CaseDetector#isPascalCase(String)
   */
  public static boolean isPascalCase(String str) {
    return CaseDetector.isPascalCase(str);
  }

  /**
   * Checks if a string is in camelCase.
   *
   * @param str The string to check.
   * @return {@code true} if the string is in camelCase, {@code false} otherwise.
   * @see CaseDetector#isCamelCase(String)
   */
  public static boolean isCamelCase(String str) {
    return CaseDetector.isCamelCase(str);
  }

  /**
   * Checks if a string is in snake_case.
   *
   * @param str The string to check.
   * @return {@code true} if the string is in snake_case, {@code false} otherwise.
   * @see CaseDetector#isSnakeCase(String)
   */
  public static boolean isSnakeCase(String str) {
    return CaseDetector.isSnakeCase(str);
  }

  /**
   * Checks if a string is in SCREAMING_SNAKE_CASE.
   *
   * @param str The string to check.
   * @return {@code true} if the string is in SCREAMING_SNAKE_CASE, {@code false} otherwise.
   * @see CaseDetector#isScreamingSnakeCase(String)
   */
  public static boolean isScreamingSnakeCase(String str) {
    return CaseDetector.isScreamingSnakeCase(str);
  }

  /**
   * Checks if a string is in kebab-case.
   *
   * @param str The string to check.
   * @return {@code true} if the string is in kebab-case, {@code false} otherwise.
   * @see CaseDetector#isKebabCase(String)
   */
  public static boolean isKebabCase(String str) {
    return CaseDetector.isKebabCase(str);
  }

  /**
   * Detects the case format of the input string.
   *
   * @param input The string to analyze.
   * @return The detected CaseFormat enum value.
   * @see CaseDetector#detectFormat(String)
   */
  public static CaseFormat detectFormat(String input) {
    return CaseDetector.detectFormat(input);
  }

  // Case Conversion Methods - Delegated to CaseConverter

  /**
   * Converts any string to camelCase.
   *
   * @param input The input string in any format.
   * @return The converted string in camelCase.
   * @see CaseConverter#toCamelCase(String)
   */
  public static String toCamelCase(String input) {
    return CaseConverter.toCamelCase(input);
  }

  /**
   * Converts any string to PascalCase (UpperCamelCase).
   *
   * @param input The input string in any format.
   * @return The converted string in PascalCase.
   * @see CaseConverter#toPascalCase(String)
   */
  public static String toPascalCase(String input) {
    return CaseConverter.toPascalCase(input);
  }

  /**
   * Converts any string to snake_case.
   *
   * @param input The input string in any format.
   * @return The converted string in snake_case.
   * @see CaseConverter#toSnakeCase(String)
   */
  public static String toSnakeCase(String input) {
    return CaseConverter.toSnakeCase(input);
  }

  /**
   * Converts any string to SCREAMING_SNAKE_CASE (CONSTANT_CASE).
   *
   * @param input The input string in any format.
   * @return The converted string in SCREAMING_SNAKE_CASE.
   * @see CaseConverter#toScreamingSnakeCase(String)
   */
  public static String toScreamingSnakeCase(String input) {
    return CaseConverter.toScreamingSnakeCase(input);
  }

  /**
   * Converts any string to kebab-case.
   *
   * @param input The input string in any format.
   * @return The converted string in kebab-case.
   * @see CaseConverter#toKebabCase(String)
   */
  public static String toKebabCase(String input) {
    return CaseConverter.toKebabCase(input);
  }

  /**
   * Converts any string to dot.case.
   *
   * @param input The input string in any format.
   * @return The converted string in dot.case.
   * @see CaseConverter#toDotCase(String)
   */
  public static String toDotCase(String input) {
    return CaseConverter.toDotCase(input);
  }

  /**
   * Converts any string to Title Case.
   *
   * @param input The input string in any format.
   * @return The converted string in Title Case.
   * @see CaseConverter#toTitleCase(String)
   */
  public static String toTitleCase(String input) {
    return CaseConverter.toTitleCase(input);
  }

  /**
   * Converts any string to Sentence case.
   *
   * @param input The input string in any format.
   * @return The converted string in Sentence case.
   * @see CaseConverter#toSentenceCase(String)
   */
  public static String toSentenceCase(String input) {
    return CaseConverter.toSentenceCase(input);
  }

  /**
   * Converts a camelCase string to PascalCase.
   *
   * @param camelCaseStr The string in camelCase.
   * @return The converted string in PascalCase.
   * @see CaseConverter#camelToPascal(String)
   */
  public static String camelToPascal(String camelCaseStr) {
    return CaseConverter.camelToPascal(camelCaseStr);
  }

  /**
   * Converts a PascalCase string to camelCase.
   *
   * @param pascalCaseStr The string in PascalCase.
   * @return The converted string in camelCase.
   * @see CaseConverter#pascalToCamel(String)
   */
  public static String pascalToCamel(String pascalCaseStr) {
    return CaseConverter.pascalToCamel(pascalCaseStr);
  }

  /**
   * Converts from any format to any other format.
   *
   * @param input The input string in any format.
   * @param targetFormat The target case format.
   * @return The converted string in the target format.
   * @see CaseConverter#convert(String, CaseFormat)
   */
  public static String convert(String input, CaseFormat targetFormat) {
    return CaseConverter.convert(input, targetFormat);
  }

  // Pluralization Methods - Delegated to StringPluralizer

  /**
   * Pluralizes a camelCase string.
   *
   * @param camelCaseStr The camelCase string to pluralize.
   * @return The pluralized camelCase string.
   * @see StringPluralizer#pluralizeCamelCase(String)
   */
  public static String pluralizeCamelCase(String camelCaseStr) {
    return StringPluralizer.pluralizeCamelCase(camelCaseStr);
  }

  /**
   * Pluralizes a PascalCase string.
   *
   * @param pascalCaseStr The PascalCase string to pluralize.
   * @return The pluralized PascalCase string.
   * @see StringPluralizer#pluralizePascalCase(String)
   */
  public static String pluralizePascalCase(String pascalCaseStr) {
    return StringPluralizer.pluralizePascalCase(pascalCaseStr);
  }

  /**
   * Pluralizes a snake_case string.
   *
   * @param snakeCaseStr The snake_case string to pluralize.
   * @return The pluralized snake_case string.
   * @see StringPluralizer#pluralizeSnakeCase(String)
   */
  public static String pluralizeSnakeCase(String snakeCaseStr) {
    return StringPluralizer.pluralizeSnakeCase(snakeCaseStr);
  }

  /**
   * Checks if a camelCase string is in plural form.
   *
   * @param camelCaseStr The camelCase string to check.
   * @return true if the string appears to be plural, false otherwise.
   * @see StringPluralizer#isPluralCamelCase(String)
   */
  public static boolean isPluralCamelCase(String camelCaseStr) {
    return StringPluralizer.isPluralCamelCase(camelCaseStr);
  }

  /**
   * Checks if any case format string is in plural form.
   *
   * @param str The string to check (in any case format).
   * @return true if the string appears to be plural, false otherwise.
   * @see StringPluralizer#isPlural(String)
   */
  public static boolean isPlural(String str) {
    return StringPluralizer.isPlural(str);
  }

  /**
   * Pluralizes a string in any case format while preserving the case format.
   *
   * @param str The string to pluralize (in any case format).
   * @return The pluralized string in the same case format.
   * @see StringPluralizer#pluralize(String)
   */
  public static String pluralize(String str) {
    return StringPluralizer.pluralize(str);
  }
}
