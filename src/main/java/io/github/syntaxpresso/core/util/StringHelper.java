package io.github.syntaxpresso.core.util;

import com.google.common.base.Strings;
import org.jvnet.inflector.Noun;

public class StringHelper {

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
    String pascalCasePattern = "^[A-Z][a-z0-9]*([A-Z][a-z0-9]*)*$";
    return str.matches(pascalCasePattern);
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
    String camelCasePattern = "^[a-z]+([A-Z][a-z0-9]*)*$";
    return str.matches(camelCasePattern);
  }

  /**
   * Converts a camelCase string to PascalCase.
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
   * Converts a PascalCase string to camelCase.
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
}
