package io.github.syntaxpresso.core.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("StringHelper Tests")
class StringHelperTest {

  @Nested
  @DisplayName("isPascalCase()")
  class IsPascalCaseTests {
    @Test
    @DisplayName("should return true for valid PascalCase strings")
    void isPascalCase_withValidInput_shouldReturnTrue() {
      assertTrue(StringHelper.isPascalCase("PascalCase"));
      assertTrue(StringHelper.isPascalCase("AnotherPascalCase"));
      assertTrue(StringHelper.isPascalCase("A"));
      assertTrue(StringHelper.isPascalCase("PascalCase1"));
    }

    @Test
    @DisplayName("should return false for invalid PascalCase strings")
    void isPascalCase_withInvalidInput_shouldReturnFalse() {
      assertFalse(StringHelper.isPascalCase("camelCase"));
      assertFalse(StringHelper.isPascalCase("snake_case"));
      assertFalse(StringHelper.isPascalCase("kebab-case"));
      assertFalse(StringHelper.isPascalCase(" space case"));
      assertFalse(StringHelper.isPascalCase("1PascalCase"));
    }

    @Test
    @DisplayName("should return false for null or empty strings")
    void isPascalCase_withNullOrEmpty_shouldReturnFalse() {
      assertFalse(StringHelper.isPascalCase(null));
      assertFalse(StringHelper.isPascalCase(""));
    }
  }

  @Nested
  @DisplayName("isCamelCase()")
  class IsCamelCaseTests {
    @Test
    @DisplayName("should return true for valid camelCase strings")
    void isCamelCase_withValidInput_shouldReturnTrue() {
      assertTrue(StringHelper.isCamelCase("camelCase"));
      assertTrue(StringHelper.isCamelCase("anotherCamelCase"));
      assertTrue(StringHelper.isCamelCase("a"));
      assertTrue(StringHelper.isCamelCase("camelCase1"));
    }

    @Test
    @DisplayName("should return false for invalid camelCase strings")
    void isCamelCase_withInvalidInput_shouldReturnFalse() {
      assertFalse(StringHelper.isCamelCase("PascalCase"));
      assertFalse(StringHelper.isCamelCase("Snake_case"));
      assertFalse(StringHelper.isCamelCase("kebab-case"));
      assertFalse(StringHelper.isCamelCase(" Space case"));
      assertFalse(StringHelper.isCamelCase("1camelCase"));
    }

    @Test
    @DisplayName("should return false for null or empty strings")
    void isCamelCase_withNullOrEmpty_shouldReturnFalse() {
      assertFalse(StringHelper.isCamelCase(null));
      assertFalse(StringHelper.isCamelCase(""));
    }
  }

  @Nested
  @DisplayName("camelToPascal()")
  class CamelToPascalTests {
    @Test
    @DisplayName("should correctly convert camelCase to PascalCase")
    void camelToPascal_shouldSucceed() {
      assertEquals("CamelCase", StringHelper.camelToPascal("camelCase"));
      assertEquals("A", StringHelper.camelToPascal("a"));
      assertEquals("MyVariableName", StringHelper.camelToPascal("myVariableName"));
    }

    @Test
    @DisplayName("should return null or empty for null or empty input")
    void camelToPascal_withNullOrEmpty_shouldReturnAsIs() {
      assertEquals("", StringHelper.camelToPascal(""));
      assertNull(StringHelper.camelToPascal(null));
    }
  }

  @Nested
  @DisplayName("pascalToCamel()")
  class PascalToCamelTests {
    @Test
    @DisplayName("should correctly convert PascalCase to camelCase")
    void pascalToCamel_shouldSucceed() {
      assertEquals("pascalCase", StringHelper.pascalToCamel("PascalCase"));
      assertEquals("a", StringHelper.pascalToCamel("A"));
      assertEquals("myVariableName", StringHelper.pascalToCamel("MyVariableName"));
    }

    @Test
    @DisplayName("should return null or empty for null or empty input")
    void pascalToCamel_withNullOrEmpty_shouldReturnAsIs() {
      assertEquals("", StringHelper.pascalToCamel(""));
      assertNull(StringHelper.pascalToCamel(null));
    }
  }

  @Nested
  @DisplayName("pluralizeCamelCase()")
  class PluralizeCamelCaseTests {
    @Test
    @DisplayName("should correctly pluralize camelCase strings")
    void pluralizeCamelCase_shouldSucceed() {
      assertEquals("singleWords", StringHelper.pluralizeCamelCase("singleWord"));
      assertEquals("camelCases", StringHelper.pluralizeCamelCase("camelCase"));
      assertEquals("userProfiles", StringHelper.pluralizeCamelCase("userProfile"));
      assertEquals("companyAddresses", StringHelper.pluralizeCamelCase("companyAddress"));
    }

    @Test
    @DisplayName("should handle irregular plural nouns")
    void pluralizeCamelCase_withIrregularNouns_shouldSucceed() {
      assertEquals("people", StringHelper.pluralizeCamelCase("person"));
      assertEquals("men", StringHelper.pluralizeCamelCase("man"));
      assertEquals("geese", StringHelper.pluralizeCamelCase("goose"));
    }

    @Test
    @DisplayName("should handle words ending in 's', 'sh', 'ch', 'x', 'z'")
    void pluralizeCamelCase_withSpecialEndings_shouldSucceed() {
      assertEquals("buses", StringHelper.pluralizeCamelCase("bus"));
      assertEquals("wishes", StringHelper.pluralizeCamelCase("wish"));
      assertEquals("pitches", StringHelper.pluralizeCamelCase("pitch"));
      assertEquals("boxes", StringHelper.pluralizeCamelCase("box"));
    }

    @Test
    @DisplayName("should return null or empty for null or empty input")
    void pluralizeCamelCase_withNullOrEmpty_shouldReturnAsIs() {
      assertEquals("", StringHelper.pluralizeCamelCase(""));
      assertNull(StringHelper.pluralizeCamelCase(null));
    }
  }
}
