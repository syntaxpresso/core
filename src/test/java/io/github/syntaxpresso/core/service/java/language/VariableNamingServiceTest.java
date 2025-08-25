package io.github.syntaxpresso.core.service.java.language;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("VariableNamingService Tests")
class VariableNamingServiceTest {
  private VariableNamingService variableNamingService;

  @BeforeEach
  void setUp() {
    variableNamingService = new VariableNamingService();
  }

  @Nested
  @DisplayName("isCollectionType() tests")
  class IsCollectionTypeTests {
    @Test
    @DisplayName("Should return true for List types")
    void shouldReturnTrueForListTypes() {
      assertTrue(variableNamingService.isCollectionType("List<String>"));
      assertTrue(variableNamingService.isCollectionType("ArrayList<Integer>"));
      assertTrue(variableNamingService.isCollectionType("LinkedList<Object>"));
    }

    @Test
    @DisplayName("Should return true for Set types")
    void shouldReturnTrueForSetTypes() {
      assertTrue(variableNamingService.isCollectionType("Set<String>"));
      assertTrue(variableNamingService.isCollectionType("HashSet<Integer>"));
      assertTrue(variableNamingService.isCollectionType("LinkedHashSet<Object>"));
      assertTrue(variableNamingService.isCollectionType("TreeSet<String>"));
    }

    @Test
    @DisplayName("Should return true for Collection types")
    void shouldReturnTrueForCollectionTypes() {
      assertTrue(variableNamingService.isCollectionType("Collection<String>"));
    }

    @Test
    @DisplayName("Should return false for non-collection types")
    void shouldReturnFalseForNonCollectionTypes() {
      assertFalse(variableNamingService.isCollectionType("String"));
      assertFalse(variableNamingService.isCollectionType("Integer"));
      assertFalse(variableNamingService.isCollectionType("Map<String, Integer>"));
      assertFalse(variableNamingService.isCollectionType("Optional<String>"));
    }

    @Test
    @DisplayName("Should return false for null input")
    void shouldReturnFalseForNullInput() {
      assertFalse(variableNamingService.isCollectionType(null));
    }
  }

  @Nested
  @DisplayName("generateVariableName() tests")
  class GenerateVariableNameTests {
    @Test
    @DisplayName("Should generate singular camelCase name for non-collection types")
    void shouldGenerateSingularCamelCaseNameForNonCollectionTypes() {
      assertEquals("user", variableNamingService.generateVariableName("User", false));
      assertEquals("orderItem", variableNamingService.generateVariableName("OrderItem", false));
    }

    @Test
    @DisplayName("Should generate plural camelCase name for collection types")
    void shouldGeneratePluralCamelCaseNameForCollectionTypes() {
      assertEquals("users", variableNamingService.generateVariableName("User", true));
      assertEquals("orderItems", variableNamingService.generateVariableName("OrderItem", true));
    }
  }

  @Nested
  @DisplayName("shouldRenameVariable() tests")
  class ShouldRenameVariableTests {
    @Test
    @DisplayName(
        "Should return true when variable name matches expected pattern for non-collection")
    void shouldReturnTrueWhenVariableNameMatchesExpectedPatternForNonCollection() {
      assertTrue(variableNamingService.shouldRenameVariable("user", "User", false));
      assertTrue(variableNamingService.shouldRenameVariable("orderItem", "OrderItem", false));
    }

    @Test
    @DisplayName(
        "Should return true when variable name matches expected plural pattern for collection")
    void shouldReturnTrueWhenVariableNameMatchesExpectedPluralPatternForCollection() {
      assertTrue(variableNamingService.shouldRenameVariable("users", "User", true));
      assertTrue(variableNamingService.shouldRenameVariable("orderItems", "OrderItem", true));
    }

    @Test
    @DisplayName("Should return false when variable name does not match expected pattern")
    void shouldReturnFalseWhenVariableNameDoesNotMatchExpectedPattern() {
      assertFalse(variableNamingService.shouldRenameVariable("customName", "User", false));
      assertFalse(variableNamingService.shouldRenameVariable("customList", "User", true));
    }
  }

  @Nested
  @DisplayName("generateNewVariableName() tests")
  class GenerateNewVariableNameTests {
    @Test
    @DisplayName("Should generate new variable name when rename is needed for non-collection")
    void shouldGenerateNewVariableNameWhenRenameIsNeededForNonCollection() {
      String result =
          variableNamingService.generateNewVariableName("user", "User", "Customer", false);
      assertEquals("customer", result);
    }

    @Test
    @DisplayName("Should generate new variable name when rename is needed for collection")
    void shouldGenerateNewVariableNameWhenRenameIsNeededForCollection() {
      String result =
          variableNamingService.generateNewVariableName("users", "User", "Customer", true);
      assertEquals("customers", result);
    }

    @Test
    @DisplayName("Should return current name when no rename is needed")
    void shouldReturnCurrentNameWhenNoRenameIsNeeded() {
      String result =
          variableNamingService.generateNewVariableName("customName", "User", "Customer", false);
      assertEquals("customName", result);
    }
  }
}
