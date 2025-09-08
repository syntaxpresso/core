package io.github.syntaxpresso.core.service.java.language;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("VariableNamingService Tests")
class VariableNamingServiceTest {
  private VariableNamingService variableNamingService;

  @BeforeEach
  void setUp() {
    this.variableNamingService = new VariableNamingService();
  }

  @Nested
  @DisplayName("isCollectionType() Tests")
  class IsCollectionTypeTests {

    /**
     * Tests that isCollectionType correctly identifies various collection types.
     *
     * <p>Usage example:
     *
     * <pre>
     * boolean isList = service.isCollectionType("List&lt;String&gt;");
     * boolean isSet = service.isCollectionType("Set&lt;Integer&gt;");
     * // isList = true, isSet = true
     * </pre>
     */
    @Test
    @DisplayName("should return true for List types")
    void isCollectionType_withListTypes_shouldReturnTrue() {
      assertTrue(variableNamingService.isCollectionType("List<String>"));
      assertTrue(variableNamingService.isCollectionType("List<Integer>"));
      assertTrue(variableNamingService.isCollectionType("List<User>"));
      assertTrue(variableNamingService.isCollectionType("List<>"));
    }

    @Test
    @DisplayName("should return true for Set types")
    void isCollectionType_withSetTypes_shouldReturnTrue() {
      assertTrue(variableNamingService.isCollectionType("Set<String>"));
      assertTrue(variableNamingService.isCollectionType("Set<Long>"));
      assertTrue(variableNamingService.isCollectionType("Set<Product>"));
      assertTrue(variableNamingService.isCollectionType("Set<>"));
    }

    @Test
    @DisplayName("should return true for ArrayList types")
    void isCollectionType_withArrayListTypes_shouldReturnTrue() {
      assertTrue(variableNamingService.isCollectionType("ArrayList<String>"));
      assertTrue(variableNamingService.isCollectionType("ArrayList<Double>"));
      assertTrue(variableNamingService.isCollectionType("ArrayList<Customer>"));
      assertTrue(variableNamingService.isCollectionType("ArrayList<>"));
    }

    @Test
    @DisplayName("should return true for LinkedList types")
    void isCollectionType_withLinkedListTypes_shouldReturnTrue() {
      assertTrue(variableNamingService.isCollectionType("LinkedList<String>"));
      assertTrue(variableNamingService.isCollectionType("LinkedList<Integer>"));
      assertTrue(variableNamingService.isCollectionType("LinkedList<Order>"));
      assertTrue(variableNamingService.isCollectionType("LinkedList<>"));
    }

    @Test
    @DisplayName("should return true for HashSet types")
    void isCollectionType_withHashSetTypes_shouldReturnTrue() {
      assertTrue(variableNamingService.isCollectionType("HashSet<String>"));
      assertTrue(variableNamingService.isCollectionType("HashSet<Integer>"));
      assertTrue(variableNamingService.isCollectionType("HashSet<Employee>"));
      assertTrue(variableNamingService.isCollectionType("HashSet<>"));
    }

    @Test
    @DisplayName("should return true for LinkedHashSet types")
    void isCollectionType_withLinkedHashSetTypes_shouldReturnTrue() {
      assertTrue(variableNamingService.isCollectionType("LinkedHashSet<String>"));
      assertTrue(variableNamingService.isCollectionType("LinkedHashSet<Integer>"));
      assertTrue(variableNamingService.isCollectionType("LinkedHashSet<Item>"));
      assertTrue(variableNamingService.isCollectionType("LinkedHashSet<>"));
    }

    @Test
    @DisplayName("should return true for TreeSet types")
    void isCollectionType_withTreeSetTypes_shouldReturnTrue() {
      assertTrue(variableNamingService.isCollectionType("TreeSet<String>"));
      assertTrue(variableNamingService.isCollectionType("TreeSet<Integer>"));
      assertTrue(variableNamingService.isCollectionType("TreeSet<Category>"));
      assertTrue(variableNamingService.isCollectionType("TreeSet<>"));
    }

    @Test
    @DisplayName("should return true for Collection types")
    void isCollectionType_withCollectionTypes_shouldReturnTrue() {
      assertTrue(variableNamingService.isCollectionType("Collection<String>"));
      assertTrue(variableNamingService.isCollectionType("Collection<Integer>"));
      assertTrue(variableNamingService.isCollectionType("Collection<Document>"));
      assertTrue(variableNamingService.isCollectionType("Collection<>"));
    }

    @Test
    @DisplayName("should return false for non-collection types")
    void isCollectionType_withNonCollectionTypes_shouldReturnFalse() {
      assertFalse(variableNamingService.isCollectionType("String"));
      assertFalse(variableNamingService.isCollectionType("Integer"));
      assertFalse(variableNamingService.isCollectionType("User"));
      assertFalse(variableNamingService.isCollectionType("boolean"));
      assertFalse(variableNamingService.isCollectionType("int"));
      assertFalse(variableNamingService.isCollectionType("Map<String, Integer>"));
      assertFalse(variableNamingService.isCollectionType("Optional<String>"));
      assertFalse(variableNamingService.isCollectionType("Stream<Integer>"));
    }

    @Test
    @DisplayName("should return false for null input")
    void isCollectionType_withNullInput_shouldReturnFalse() {
      assertFalse(variableNamingService.isCollectionType(null));
    }

    @Test
    @DisplayName("should return false for empty string")
    void isCollectionType_withEmptyString_shouldReturnFalse() {
      assertFalse(variableNamingService.isCollectionType(""));
      assertFalse(variableNamingService.isCollectionType("   "));
    }

    @Test
    @DisplayName("should return false for partial matches")
    void isCollectionType_withPartialMatches_shouldReturnFalse() {
      assertFalse(variableNamingService.isCollectionType("List"));
      assertFalse(variableNamingService.isCollectionType("Set"));
      assertFalse(variableNamingService.isCollectionType("ArrayList"));
      assertFalse(variableNamingService.isCollectionType("MyList<String>"));
      assertFalse(variableNamingService.isCollectionType("CustomSet<Integer>"));
    }

    @Test
    @DisplayName("should be case sensitive")
    void isCollectionType_withDifferentCases_shouldBeCaseSensitive() {
      assertFalse(variableNamingService.isCollectionType("list<String>"));
      assertFalse(variableNamingService.isCollectionType("LIST<String>"));
      assertFalse(variableNamingService.isCollectionType("set<Integer>"));
      assertFalse(variableNamingService.isCollectionType("SET<Integer>"));
      assertFalse(variableNamingService.isCollectionType("arraylist<String>"));
      assertFalse(variableNamingService.isCollectionType("ARRAYLIST<String>"));
    }
  }

  @Nested
  @DisplayName("generateVariableName() Tests")
  class GenerateVariableNameTests {

    /**
     * Tests that generateVariableName creates appropriate variable names.
     *
     * <p>Usage example:
     *
     * <pre>
     * String singleName = service.generateVariableName("User", false);
     * String collectionName = service.generateVariableName("User", true);
     * // singleName = "user", collectionName = "users"
     * </pre>
     */
    @Test
    @DisplayName("should generate singular variable names for non-collections")
    void generateVariableName_withNonCollection_shouldGenerateSingularName() {
      assertEquals("user", variableNamingService.generateVariableName("User", false));
      assertEquals("product", variableNamingService.generateVariableName("Product", false));
      assertEquals("order", variableNamingService.generateVariableName("Order", false));
      assertEquals("customer", variableNamingService.generateVariableName("Customer", false));
      assertEquals("employee", variableNamingService.generateVariableName("Employee", false));
    }

    @Test
    @DisplayName("should generate plural variable names for collections")
    void generateVariableName_withCollection_shouldGeneratePluralName() {
      assertEquals("users", variableNamingService.generateVariableName("User", true));
      assertEquals("products", variableNamingService.generateVariableName("Product", true));
      assertEquals("orders", variableNamingService.generateVariableName("Order", true));
      assertEquals("customers", variableNamingService.generateVariableName("Customer", true));
      assertEquals("employees", variableNamingService.generateVariableName("Employee", true));
    }

    @Test
    @DisplayName("should handle complex type names correctly")
    void generateVariableName_withComplexTypeNames_shouldHandleCorrectly() {
      assertEquals("orderItem", variableNamingService.generateVariableName("OrderItem", false));
      assertEquals("orderItems", variableNamingService.generateVariableName("OrderItem", true));
      assertEquals("userAccount", variableNamingService.generateVariableName("UserAccount", false));
      assertEquals("userAccounts", variableNamingService.generateVariableName("UserAccount", true));
    }

    @Test
    @DisplayName("should handle irregular plurals correctly")
    void generateVariableName_withIrregularPlurals_shouldHandleCorrectly() {
      assertEquals("person", variableNamingService.generateVariableName("Person", false));
      assertEquals("people", variableNamingService.generateVariableName("Person", true));
      assertEquals("child", variableNamingService.generateVariableName("Child", false));
      assertEquals("children", variableNamingService.generateVariableName("Child", true));
    }

    @Test
    @DisplayName("should handle words ending in special characters")
    void generateVariableName_withSpecialEndings_shouldHandleCorrectly() {
      assertEquals("box", variableNamingService.generateVariableName("Box", false));
      assertEquals("boxes", variableNamingService.generateVariableName("Box", true));
      assertEquals("category", variableNamingService.generateVariableName("Category", false));
      assertEquals("categorys", variableNamingService.generateVariableName("Category", true));
    }

    @Test
    @DisplayName("should handle single letter types")
    void generateVariableName_withSingleLetterTypes_shouldHandleCorrectly() {
      assertEquals("t", variableNamingService.generateVariableName("T", false));
      assertEquals("tS", variableNamingService.generateVariableName("T", true));
      assertEquals("e", variableNamingService.generateVariableName("E", false));
      assertEquals("eS", variableNamingService.generateVariableName("E", true));
    }

    @Test
    @DisplayName("should handle null and empty inputs gracefully")
    void generateVariableName_withNullOrEmptyInputs_shouldHandleGracefully() {
      // These tests depend on StringHelper behavior - testing the integration
      assertNull(variableNamingService.generateVariableName(null, false));
      assertNull(variableNamingService.generateVariableName(null, true));
      assertEquals("", variableNamingService.generateVariableName("", false));
      assertEquals("", variableNamingService.generateVariableName("", true));
    }
  }

  @Nested
  @DisplayName("shouldRenameVariable() Tests")
  class ShouldRenameVariableTests {

    /**
     * Tests that shouldRenameVariable correctly determines when renaming is needed.
     *
     * <p>Usage example:
     *
     * <pre>
     * boolean shouldRename = service.shouldRenameVariable("user", "User", false);
     * boolean shouldRenameCollection = service.shouldRenameVariable("users", "User", true);
     * // shouldRename = true, shouldRenameCollection = true
     * </pre>
     */
    @Test
    @DisplayName("should return true when variable matches expected singular name")
    void shouldRenameVariable_withMatchingSingularName_shouldReturnTrue() {
      assertTrue(variableNamingService.shouldRenameVariable("user", "User", false));
      assertTrue(variableNamingService.shouldRenameVariable("product", "Product", false));
      assertTrue(variableNamingService.shouldRenameVariable("order", "Order", false));
      assertTrue(variableNamingService.shouldRenameVariable("customer", "Customer", false));
    }

    @Test
    @DisplayName("should return true when variable matches expected plural name")
    void shouldRenameVariable_withMatchingPluralName_shouldReturnTrue() {
      assertTrue(variableNamingService.shouldRenameVariable("users", "User", true));
      assertTrue(variableNamingService.shouldRenameVariable("products", "Product", true));
      assertTrue(variableNamingService.shouldRenameVariable("orders", "Order", true));
      assertTrue(variableNamingService.shouldRenameVariable("customers", "Customer", true));
    }

    @Test
    @DisplayName("should return false when variable doesn't match expected singular name")
    void shouldRenameVariable_withNonMatchingSingularName_shouldReturnFalse() {
      assertFalse(variableNamingService.shouldRenameVariable("myUser", "User", false));
      assertFalse(variableNamingService.shouldRenameVariable("userEntity", "User", false));
      assertFalse(variableNamingService.shouldRenameVariable("currentUser", "User", false));
      assertFalse(variableNamingService.shouldRenameVariable("u", "User", false));
    }

    @Test
    @DisplayName("should return false when variable doesn't match expected plural name")
    void shouldRenameVariable_withNonMatchingPluralName_shouldReturnFalse() {
      assertFalse(variableNamingService.shouldRenameVariable("userList", "User", true));
      assertFalse(variableNamingService.shouldRenameVariable("allUsers", "User", true));
      assertFalse(variableNamingService.shouldRenameVariable("userCollection", "User", true));
      assertFalse(variableNamingService.shouldRenameVariable("user", "User", true)); // singular vs plural
    }

    @Test
    @DisplayName("should handle complex type names correctly")
    void shouldRenameVariable_withComplexTypeNames_shouldHandleCorrectly() {
      assertTrue(variableNamingService.shouldRenameVariable("orderItem", "OrderItem", false));
      assertTrue(variableNamingService.shouldRenameVariable("orderItems", "OrderItem", true));
      assertTrue(variableNamingService.shouldRenameVariable("userAccount", "UserAccount", false));
      assertTrue(variableNamingService.shouldRenameVariable("userAccounts", "UserAccount", true));
    }

    @Test
    @DisplayName("should handle irregular plurals correctly")
    void shouldRenameVariable_withIrregularPlurals_shouldHandleCorrectly() {
      assertTrue(variableNamingService.shouldRenameVariable("person", "Person", false));
      assertTrue(variableNamingService.shouldRenameVariable("people", "Person", true));
      assertTrue(variableNamingService.shouldRenameVariable("child", "Child", false));
      assertTrue(variableNamingService.shouldRenameVariable("children", "Child", true));
    }

    @Test
    @DisplayName("should be case sensitive")
    void shouldRenameVariable_withDifferentCases_shouldBeCaseSensitive() {
      assertFalse(variableNamingService.shouldRenameVariable("User", "User", false));
      assertFalse(variableNamingService.shouldRenameVariable("USER", "User", false));
      assertFalse(variableNamingService.shouldRenameVariable("Users", "User", true));
      assertFalse(variableNamingService.shouldRenameVariable("USERS", "User", true));
    }

    @Test
    @DisplayName("should handle null inputs gracefully")
    void shouldRenameVariable_withNullInputs_shouldHandleGracefully() {
      // StringHelper.pascalToCamel handles null gracefully, but equals() call will throw NPE
      // when currentVariableName is null
      try {
        variableNamingService.shouldRenameVariable(null, "User", false);
        // Should not reach here
        assertTrue(false, "Expected NullPointerException");
      } catch (NullPointerException e) {
        // Expected behavior
        assertTrue(true);
      }
      
      // When typeName is null, StringHelper returns null, and equals handles null comparison
      assertFalse(variableNamingService.shouldRenameVariable("user", null, false));
      
      // Null typeName with collection
      assertFalse(variableNamingService.shouldRenameVariable("users", null, true));
    }
  }

  @Nested
  @DisplayName("generateNewVariableName() Tests")
  class GenerateNewVariableNameTests {

    /**
     * Tests that generateNewVariableName creates appropriate new names when renaming is needed.
     *
     * <p>Usage example:
     *
     * <pre>
     * String newName = service.generateNewVariableName("user", "User", "Customer", false);
     * String newCollectionName = service.generateNewVariableName("users", "User", "Customer", true);
     * // newName = "customer", newCollectionName = "customers"
     * </pre>
     */
    @Test
    @DisplayName("should generate new variable name when renaming is needed for non-collections")
    void generateNewVariableName_withRenamingNeededNonCollection_shouldGenerateNewName() {
      assertEquals("customer", variableNamingService.generateNewVariableName(
          "user", "User", "Customer", false));
      assertEquals("product", variableNamingService.generateNewVariableName(
          "order", "Order", "Product", false));
      assertEquals("employee", variableNamingService.generateNewVariableName(
          "person", "Person", "Employee", false));
    }

    @Test
    @DisplayName("should generate new variable name when renaming is needed for collections")
    void generateNewVariableName_withRenamingNeededCollection_shouldGenerateNewName() {
      assertEquals("customers", variableNamingService.generateNewVariableName(
          "users", "User", "Customer", true));
      assertEquals("products", variableNamingService.generateNewVariableName(
          "orders", "Order", "Product", true));
      assertEquals("employees", variableNamingService.generateNewVariableName(
          "people", "Person", "Employee", true));
    }

    @Test
    @DisplayName("should keep current name when renaming is not needed for non-collections")
    void generateNewVariableName_withRenamingNotNeededNonCollection_shouldKeepCurrentName() {
      assertEquals("myUser", variableNamingService.generateNewVariableName(
          "myUser", "User", "Customer", false));
      assertEquals("currentUser", variableNamingService.generateNewVariableName(
          "currentUser", "User", "Customer", false));
      assertEquals("userEntity", variableNamingService.generateNewVariableName(
          "userEntity", "User", "Customer", false));
    }

    @Test
    @DisplayName("should keep current name when renaming is not needed for collections")
    void generateNewVariableName_withRenamingNotNeededCollection_shouldKeepCurrentName() {
      assertEquals("userList", variableNamingService.generateNewVariableName(
          "userList", "User", "Customer", true));
      assertEquals("allUsers", variableNamingService.generateNewVariableName(
          "allUsers", "User", "Customer", true));
      assertEquals("userCollection", variableNamingService.generateNewVariableName(
          "userCollection", "User", "Customer", true));
    }

    @Test
    @DisplayName("should handle complex type transformations")
    void generateNewVariableName_withComplexTypeTransformations_shouldHandleCorrectly() {
      assertEquals("customerOrder", variableNamingService.generateNewVariableName(
          "userOrder", "UserOrder", "CustomerOrder", false));
      assertEquals("customerOrders", variableNamingService.generateNewVariableName(
          "userOrders", "UserOrder", "CustomerOrder", true));
      assertEquals("orderItem", variableNamingService.generateNewVariableName(
          "productItem", "ProductItem", "OrderItem", false));
      assertEquals("orderItems", variableNamingService.generateNewVariableName(
          "productItems", "ProductItem", "OrderItem", true));
    }

    @Test
    @DisplayName("should handle irregular plural transformations")
    void generateNewVariableName_withIrregularPluralTransformations_shouldHandleCorrectly() {
      assertEquals("child", variableNamingService.generateNewVariableName(
          "person", "Person", "Child", false));
      assertEquals("children", variableNamingService.generateNewVariableName(
          "people", "Person", "Child", true));
      assertEquals("person", variableNamingService.generateNewVariableName(
          "child", "Child", "Person", false));
      assertEquals("people", variableNamingService.generateNewVariableName(
          "children", "Child", "Person", true));
    }

    @Test
    @DisplayName("should handle same type transformation")
    void generateNewVariableName_withSameTypeTransformation_shouldGenerateNewName() {
      assertEquals("user", variableNamingService.generateNewVariableName(
          "user", "User", "User", false));
      assertEquals("users", variableNamingService.generateNewVariableName(
          "users", "User", "User", true));
    }

    @Test
    @DisplayName("should handle null inputs gracefully")
    void generateNewVariableName_withNullInputs_shouldHandleGracefully() {
      // When currentTypeName is null, shouldRenameVariable returns false
      assertEquals("customVar", variableNamingService.generateNewVariableName(
          "customVar", null, "NewType", false));
      
      // When newTypeName is null, generateVariableName returns null but shouldRenameVariable is false
      assertEquals("customVar", variableNamingService.generateNewVariableName(
          "customVar", "OldType", null, false));
      
      // When currentVariableName is null, shouldRenameVariable throws NPE
      try {
        variableNamingService.generateNewVariableName(null, "OldType", "NewType", false);
        assertTrue(false, "Expected NullPointerException");
      } catch (NullPointerException e) {
        assertTrue(true);
      }
    }

    @Test
    @DisplayName("should handle empty string inputs gracefully")
    void generateNewVariableName_withEmptyInputs_shouldHandleGracefully() {
      assertEquals("", variableNamingService.generateNewVariableName(
          "", "", "", false));
      assertEquals("", variableNamingService.generateNewVariableName(
          "", "", "", true));
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("should work correctly for typical collection workflow")
    void integrationTest_withTypicalCollectionWorkflow_shouldWorkCorrectly() {
      String typeText = "List<User>";
      boolean isCollection = variableNamingService.isCollectionType(typeText);
      assertTrue(isCollection);

      String variableName = variableNamingService.generateVariableName("User", isCollection);
      assertEquals("users", variableName);

      boolean shouldRename = variableNamingService.shouldRenameVariable(
          "users", "User", isCollection);
      assertTrue(shouldRename);

      String newVariableName = variableNamingService.generateNewVariableName(
          "users", "User", "Customer", isCollection);
      assertEquals("customers", newVariableName);
    }

    @Test
    @DisplayName("should work correctly for typical non-collection workflow")
    void integrationTest_withTypicalNonCollectionWorkflow_shouldWorkCorrectly() {
      String typeText = "User";
      boolean isCollection = variableNamingService.isCollectionType(typeText);
      assertFalse(isCollection);

      String variableName = variableNamingService.generateVariableName("User", isCollection);
      assertEquals("user", variableName);

      boolean shouldRename = variableNamingService.shouldRenameVariable(
          "user", "User", isCollection);
      assertTrue(shouldRename);

      String newVariableName = variableNamingService.generateNewVariableName(
          "user", "User", "Customer", isCollection);
      assertEquals("customer", newVariableName);
    }

    @Test
    @DisplayName("should handle custom variable names that shouldn't be renamed")
    void integrationTest_withCustomVariableNames_shouldNotRename() {
      String typeText = "List<User>";
      boolean isCollection = variableNamingService.isCollectionType(typeText);
      assertTrue(isCollection);

      String customVariableName = "userList";
      boolean shouldRename = variableNamingService.shouldRenameVariable(
          customVariableName, "User", isCollection);
      assertFalse(shouldRename);

      String newVariableName = variableNamingService.generateNewVariableName(
          customVariableName, "User", "Customer", isCollection);
      assertEquals("userList", newVariableName); // Should keep the custom name
    }

    @Test
    @DisplayName("should handle type change from non-collection to collection")
    void integrationTest_withTypeChangeToCollection_shouldHandleCorrectly() {
      String currentVar = "user";
      String oldType = "User";
      String newType = "User"; // same type but now it's a collection
      boolean oldIsCollection = false;
      boolean newIsCollection = true;

      boolean shouldRename = variableNamingService.shouldRenameVariable(
          currentVar, oldType, oldIsCollection);
      assertTrue(shouldRename);

      // For this test, we simulate the new collection scenario
      String newVariableName = variableNamingService.generateVariableName(newType, newIsCollection);
      assertEquals("users", newVariableName);
    }

    @Test
    @DisplayName("should handle type change from collection to non-collection")
    void integrationTest_withTypeChangeFromCollection_shouldHandleCorrectly() {
      String currentVar = "users";
      String oldType = "User";
      String newType = "User"; // same type but now it's not a collection
      boolean oldIsCollection = true;
      boolean newIsCollection = false;

      boolean shouldRename = variableNamingService.shouldRenameVariable(
          currentVar, oldType, oldIsCollection);
      assertTrue(shouldRename);

      // For this test, we simulate the new non-collection scenario
      String newVariableName = variableNamingService.generateVariableName(newType, newIsCollection);
      assertEquals("user", newVariableName);
    }
  }

  @Nested
  @DisplayName("Edge Cases and Error Handling Tests")
  class EdgeCasesTests {

    @Test
    @DisplayName("should handle all methods with null inputs")
    void methods_withNullInputs_shouldHandleGracefully() {
      // isCollectionType
      assertFalse(variableNamingService.isCollectionType(null));

      // generateVariableName
      assertNull(variableNamingService.generateVariableName(null, false));
      assertNull(variableNamingService.generateVariableName(null, true));

      // shouldRenameVariable - throws NPE when currentVariableName is null
      try {
        variableNamingService.shouldRenameVariable(null, "Type", false);
        assertTrue(false, "Expected NullPointerException");
      } catch (NullPointerException e) {
        assertTrue(true);
      }
      assertFalse(variableNamingService.shouldRenameVariable("var", null, false));

      // generateNewVariableName
      try {
        variableNamingService.generateNewVariableName(null, "OldType", "NewType", false);
        assertTrue(false, "Expected NullPointerException");
      } catch (NullPointerException e) {
        assertTrue(true);
      }
      assertEquals("var", variableNamingService.generateNewVariableName(
          "var", null, "NewType", false));
      assertEquals("var", variableNamingService.generateNewVariableName(
          "var", "OldType", null, false));
    }

    @Test
    @DisplayName("should handle empty and whitespace strings")
    void methods_withEmptyStrings_shouldHandleGracefully() {
      // isCollectionType
      assertFalse(variableNamingService.isCollectionType(""));
      assertFalse(variableNamingService.isCollectionType("   "));

      // generateVariableName
      assertEquals("", variableNamingService.generateVariableName("", false));
      assertEquals("", variableNamingService.generateVariableName("", true));

      // shouldRenameVariable handles empty strings fine
      assertTrue(variableNamingService.shouldRenameVariable("", "", false));
      assertTrue(variableNamingService.shouldRenameVariable("   ", "   ", false));

      // generateNewVariableName
      assertEquals("", variableNamingService.generateNewVariableName(
          "", "", "", false));
    }

    @Test
    @DisplayName("should handle unusual type names")
    void methods_withUnusualTypeNames_shouldHandleGracefully() {
      // Single character types
      assertTrue(variableNamingService.shouldRenameVariable("t", "T", false));
      assertEquals("e", variableNamingService.generateVariableName("E", false));
      assertEquals("eS", variableNamingService.generateVariableName("E", true));

      // Types with numbers
      assertEquals("user1", variableNamingService.generateVariableName("User1", false));
      assertEquals("user1s", variableNamingService.generateVariableName("User1", true));

      // Types with special characters - these still match the collection patterns
      assertTrue(variableNamingService.isCollectionType("List<User$>"));
      assertTrue(variableNamingService.isCollectionType("List<User-Type>"));
      
      // Non-collection types with special characters
      assertFalse(variableNamingService.isCollectionType("User$"));
      assertFalse(variableNamingService.isCollectionType("User-Type"));
    }

    @Test
    @DisplayName("should handle very long type names")
    void methods_withVeryLongTypeNames_shouldHandleGracefully() {
      String longTypeName = "VeryLongComplexBusinessEntityWithManyWordsInTheName";
      
      assertEquals("veryLongComplexBusinessEntityWithManyWordsInTheName", 
          variableNamingService.generateVariableName(longTypeName, false));
      assertEquals("veryLongComplexBusinessEntityWithManyWordsInTheNames", 
          variableNamingService.generateVariableName(longTypeName, true));
    }
  }
}