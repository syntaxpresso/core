// package io.github.syntaxpresso.core.service.java;
//
// import static org.junit.jupiter.api.Assertions.assertFalse;
// import static org.junit.jupiter.api.Assertions.assertTrue;
//
// import io.github.syntaxpresso.core.common.TSFile;
// import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.DisplayName;
// import org.junit.jupiter.api.Nested;
// import org.junit.jupiter.api.Test;
//
// @DisplayName("JavaLanguageService Tests")
// class JavaLanguageServiceTest {
//   private JavaLanguageService javaLanguageService;
//
//   @BeforeEach
//   void setUp() {
//     this.javaLanguageService = new JavaLanguageService();
//   }
//
//   @Nested
//   @DisplayName("hasMainClass()")
//   class HasMainClass {
//     @Test
//     @DisplayName("should return true for a class with a main method")
//     void isMainClass_whenClassIsMain_shouldReturnTrue() {
//       String sourceCode =
//           """
//           public class Main {
//             public static void main(String[] args) {
//               System.out.println("Hello, world!");
//             }
//           }
//           """;
//       TSFile file = new TSFile(SupportedLanguage.JAVA, sourceCode);
//       assertTrue(javaLanguageService.getProgramService().hasMainClass(file));
//     }
//
//     @Test
//     @DisplayName("should return false for a class without a main method")
//     void isMainClass_whenClassIsNotMain_shouldReturnFalse() {
//       String sourceCode = "public class NotMain { }";
//       TSFile file = new TSFile(SupportedLanguage.JAVA, sourceCode);
//       assertFalse(javaLanguageService.getProgramService().hasMainClass(file));
//     }
//
//     @Test
//     @DisplayName("should return true for a main method with varargs")
//     void isMainClass_whenMainMethodHasVarargs_shouldReturnTrue() {
//       String sourceCode =
//           """
//           public class Main {
//             public static void main(String... args) {
//               System.out.println("Hello, world!");
//             }
//           }
//           """;
//       TSFile file = new TSFile(SupportedLanguage.JAVA, sourceCode);
//       assertTrue(javaLanguageService.getProgramService().hasMainClass(file));
//     }
//   }
// }

