package io.github.syntaxpresso.core.service.java.language;

import io.github.syntaxpresso.core.util.StringHelper;

/**
 * Service for generating appropriate variable names based on Java type information using naming conventions.
 *
 * <p>This service provides functionality for automatically generating variable names from type names
 * following standard Java naming conventions. It handles various scenarios including collection types,
 * generic types, and standard class types to produce meaningful and conventional variable names.
 *
 * <p>Key capabilities include:
 *
 * <ul>
 *   <li>Converting type names to appropriate camelCase variable names
 *   <li>Detecting collection types and pluralizing variable names accordingly
 *   <li>Handling generic type arguments for more specific naming
 *   <li>Following Java naming conventions and best practices
 * </ul>
 *
 * <p>Naming rules applied:
 * <ul>
 *   <li><strong>Simple types:</strong> Convert PascalCase to camelCase (User → user)
 *   <li><strong>Collection types:</strong> Pluralize based on generic type argument (List&lt;User&gt; → users)
 *   <li><strong>Fallback:</strong> Use collection type name if no type argument available (List → list)
 * </ul>
 *
 * <p>Usage example:
 *
 * <pre>
 * VariableNamingService namingService = new VariableNamingService();
 *
 * // Simple type naming
 * String varName = namingService.generateVariableName("User");
 * // Returns: "user"
 *
 * // Collection type naming
 * String collectionVarName = namingService.generateVariableName("List", "User");
 * // Returns: "users" (pluralized from the type argument)
 *
 * // Check if type is a collection
 * boolean isCollection = namingService.isCollectionType("List&lt;String&gt;");
 * // Returns: true
 * </pre>
 *
 * @see StringHelper
 */

public class VariableNamingService {

  /**
   * Checks if a type represents a collection (List, Set, ArrayList, etc.).
   *
   * <p>This method determines whether a given type string represents a collection type by checking
   * for common collection interface and implementation class patterns. It supports both raw types
   * and parameterized generic types.
   *
   * <p>Supported collection types:
   * <ul>
   *   <li>List&lt;T&gt; and implementations (ArrayList, LinkedList)
   *   <li>Set&lt;T&gt; and implementations (HashSet, LinkedHashSet, TreeSet)
   *   <li>Collection&lt;T&gt; interface
   * </ul>
   *
   * <p>Usage example:
   *
   * <pre>
   * boolean isList = service.isCollectionType("List&lt;String&gt;");        // true
   * boolean isSet = service.isCollectionType("HashSet&lt;User&gt;");       // true
   * boolean isNotCollection = service.isCollectionType("String");      // false
   * </pre>
   *
   * @param typeText The type text to check (e.g., "List&lt;String&gt;", "ArrayList&lt;User&gt;")
   * @return true if the type is a recognized collection type, false otherwise
   */
  public boolean isCollectionType(String typeText) {
    if (typeText == null) {
      return false;
    }
    return typeText.startsWith("List<")
        || typeText.startsWith("Set<")
        || typeText.startsWith("ArrayList<")
        || typeText.startsWith("LinkedList<")
        || typeText.startsWith("HashSet<")
        || typeText.startsWith("LinkedHashSet<")
        || typeText.startsWith("TreeSet<")
        || typeText.startsWith("Collection<");
  }

  /**
   * Generates an appropriate variable name based on the type name and whether it's a collection.
   *
   * <p>This method creates a conventional variable name from a type name following Java naming
   * conventions. For simple types, it converts PascalCase to camelCase. For collection types, it
   * attempts to use the generic type argument and pluralize it for better readability.
   *
   * <p>Naming strategy:
   * <ul>
   *   <li><strong>Collection with type argument:</strong> Pluralize the type argument (List&lt;User&gt; → users)
   *   <li><strong>Collection without type argument:</strong> Use collection type name (List → list)
   *   <li><strong>Simple type:</strong> Convert to camelCase (User → user)
   * </ul>
   *
   * <p>Usage example:
   *
   * <pre>
   * // For collection types with type arguments
   * String varName = service.generateVariableName("List", "User");
   * // Returns: "users"
   *
   * // For simple types
   * String simpleVarName = service.generateVariableName("User", null);
   * // Returns: "user"
   *
   * // For collections without type arguments
   * String listVarName = service.generateVariableName("ArrayList", null);
   * // Returns: "arrayList"
   * </pre>
   *
   * @param typeName The main type name in PascalCase (e.g., "List", "User", "ArrayList")
   * @param typeArgument The generic type argument if present (e.g., "User" from "List&lt;User&gt;"), can be null
   * @return A conventional camelCase variable name based on the type information
   */
   * @param isCollection Whether the type is a collection type.
   * @return The generated variable name in camelCase.
   */
  public String generateVariableName(String typeName, boolean isCollection) {
    if (isCollection) {
      return StringHelper.pascalToCamel(StringHelper.pluralizeCamelCase(typeName));
    }
    return StringHelper.pascalToCamel(typeName);
  }

  /**
   * Determines if a variable should be renamed based on naming conventions.
   *
   * @param currentVariableName The current variable name.
   * @param typeName The type name in PascalCase.
   * @param isCollection Whether the type is a collection type.
   * @return true if the variable should be renamed, false otherwise.
   */
  public boolean shouldRenameVariable(
      String currentVariableName, String typeName, boolean isCollection) {
    if (isCollection) {
      String expectedPluralName =
          StringHelper.pascalToCamel(StringHelper.pluralizeCamelCase(typeName));
      return currentVariableName.equals(expectedPluralName);
    }
    return currentVariableName.equals(StringHelper.pascalToCamel(typeName));
  }

  /**
   * Generates the new variable name for renaming operations.
   *
   * @param currentVariableName The current variable name.
   * @param currentTypeName The current type name in PascalCase.
   * @param newTypeName The new type name in PascalCase.
   * @param isCollection Whether the type is a collection type.
   * @return The new variable name, or the current name if no rename is needed.
   */
  public String generateNewVariableName(
      String currentVariableName,
      String currentTypeName,
      String newTypeName,
      boolean isCollection) {
    if (shouldRenameVariable(currentVariableName, currentTypeName, isCollection)) {
      return generateVariableName(newTypeName, isCollection);
    }
    return currentVariableName;
  }
}
