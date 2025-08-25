package io.github.syntaxpresso.core.service.java.language;

import io.github.syntaxpresso.core.util.StringHelper;

public class VariableNamingService {
  /**
   * Checks if a type represents a collection (List, Set, ArrayList, etc.).
   *
   * @param typeText The type text to check.
   * @return true if the type is a collection type, false otherwise.
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
   * @param typeName The type name in PascalCase.
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
