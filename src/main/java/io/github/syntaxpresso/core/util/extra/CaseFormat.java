package io.github.syntaxpresso.core.util.extra;

/**
 * Enum representing different string case formats.
 * 
 * <p>This enum defines the various case formats that can be detected and converted
 * between in string manipulation operations. Each format represents a specific
 * naming convention commonly used in programming and documentation.
 * 
 * @since 1.0
 */
public enum CaseFormat {
  
  /**
   * camelCase format - starts with lowercase, subsequent words capitalized.
   * Example: "helloWorld", "userName", "getId"
   */
  CAMEL_CASE,
  
  /**
   * PascalCase format - starts with uppercase, subsequent words capitalized.
   * Also known as UpperCamelCase.
   * Example: "HelloWorld", "UserName", "GetId"
   */
  PASCAL_CASE,
  
  /**
   * snake_case format - all lowercase with underscores between words.
   * Example: "hello_world", "user_name", "get_id"
   */
  SNAKE_CASE,
  
  /**
   * SCREAMING_SNAKE_CASE format - all uppercase with underscores between words.
   * Also known as CONSTANT_CASE, commonly used for constants.
   * Example: "HELLO_WORLD", "USER_NAME", "GET_ID"
   */
  SCREAMING_SNAKE_CASE,
  
  /**
   * kebab-case format - all lowercase with hyphens between words.
   * Commonly used in URLs and CSS classes.
   * Example: "hello-world", "user-name", "get-id"
   */
  KEBAB_CASE,
  
  /**
   * dot.case format - all lowercase with dots between words.
   * Example: "hello.world", "user.name", "get.id"
   */
  DOT_CASE,
  
  /**
   * Title Case format - each word capitalized with spaces between words.
   * Example: "Hello World", "User Name", "Get Id"
   */
  TITLE_CASE,
  
  /**
   * Sentence case format - first word capitalized, rest lowercase with spaces.
   * Example: "Hello world", "User name", "Get id"
   */
  SENTENCE_CASE,
  
  /**
   * Unknown format - used when the format cannot be determined.
   * This is returned when the input doesn't match any recognized pattern.
   */
  UNKNOWN
}