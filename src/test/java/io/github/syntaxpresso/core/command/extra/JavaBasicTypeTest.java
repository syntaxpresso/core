package io.github.syntaxpresso.core.command.extra;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("JavaBasicType Tests")
class JavaBasicTypeTest {
  @Nested
  @DisplayName("Type Name Lookup Tests")
  class TypeNameLookupTests {
    @Test
    @DisplayName("should find type by name for wrapper types")
    void fromTypeName_withWrapperTypes_shouldReturnCorrectType() {
      Optional<JavaBasicType> result = JavaBasicType.getByTypeName("String");
      assertTrue(result.isPresent());
      assertEquals(JavaBasicType.LANG_STRING, result.get());
      assertEquals("java.lang", result.get().getPackageName().orElse(null));
    }

    @Test
    @DisplayName("should find type by name for primitive types")
    void fromTypeName_withPrimitiveTypes_shouldReturnCorrectType() {
      Optional<JavaBasicType> result = JavaBasicType.getByTypeName("long");
      assertTrue(result.isPresent());
      assertEquals(JavaBasicType.PRIMITIVE_LONG, result.get());
      assertEquals(null, result.get().getPackageName().orElse(null));
    }

    @Test
    @DisplayName("should find type by name for time types")
    void fromTypeName_withTimeTypes_shouldReturnCorrectType() {
      Optional<JavaBasicType> result = JavaBasicType.getByTypeName("LocalDateTime");
      assertTrue(result.isPresent());
      assertEquals(JavaBasicType.TIME_LOCAL_DATE_TIME, result.get());
      assertEquals("java.time", result.get().getPackageName().orElse(null));
    }

    @Test
    @DisplayName("should find type by name for math types")
    void fromTypeName_withMathTypes_shouldReturnCorrectType() {
      Optional<JavaBasicType> result = JavaBasicType.getByTypeName("BigDecimal");
      assertTrue(result.isPresent());
      assertEquals(JavaBasicType.MATH_BIG_DECIMAL, result.get());
      assertEquals("java.math", result.get().getPackageName().orElse(null));
    }

    @Test
    @DisplayName("should return empty for unknown type name")
    void fromTypeName_withUnknownType_shouldReturnEmpty() {
      Optional<JavaBasicType> result = JavaBasicType.getByTypeName("UnknownType");
      assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("should return empty for null type name")
    void fromTypeName_withNullTypeName_shouldReturnEmpty() {
      Optional<JavaBasicType> result = JavaBasicType.getByTypeName(null);
      assertTrue(result.isEmpty());
    }
  }

  @Nested
  @DisplayName("Type Name and Package Lookup Tests")
  class TypeNameAndPackageLookupTests {
    @Test
    @DisplayName("should find type by name and package")
    void fromTypeNameAndPackage_withMatchingNameAndPackage_shouldReturnCorrectType() {
      Optional<JavaBasicType> result = JavaBasicType.fromTypeNameAndPackage("Date", "java.util");
      assertTrue(result.isPresent());
      assertEquals(JavaBasicType.UTIL_DATE, result.get());
    }

    @Test
    @DisplayName("should distinguish between Date types by package")
    void fromTypeNameAndPackage_withDifferentPackages_shouldReturnCorrectType() {
      Optional<JavaBasicType> utilDate = JavaBasicType.fromTypeNameAndPackage("Date", "java.util");
      Optional<JavaBasicType> sqlDate = JavaBasicType.fromTypeNameAndPackage("Date", "java.sql");
      assertTrue(utilDate.isPresent());
      assertTrue(sqlDate.isPresent());
      assertEquals(JavaBasicType.UTIL_DATE, utilDate.get());
      assertEquals(JavaBasicType.SQL_DATE, sqlDate.get());
    }

    @Test
    @DisplayName("should handle primitive types with null package")
    void fromTypeNameAndPackage_withPrimitiveType_shouldReturnCorrectType() {
      Optional<JavaBasicType> result = JavaBasicType.fromTypeNameAndPackage("int", null);
      assertTrue(result.isPresent());
      assertEquals(JavaBasicType.PRIMITIVE_INT, result.get());
    }

    @Test
    @DisplayName("should return empty for mismatched package")
    void fromTypeNameAndPackage_withMismatchedPackage_shouldReturnEmpty() {
      Optional<JavaBasicType> result = JavaBasicType.fromTypeNameAndPackage("String", "java.util");
      assertTrue(result.isEmpty());
    }
  }

  @Nested
  @DisplayName("Import Requirements Tests")
  class ImportRequirementsTests {
    @Test
    @DisplayName("should not need import for java.lang types")
    void needsImport_withJavaLangTypes_shouldReturnFalse() {
      assertFalse(JavaBasicType.LANG_STRING.needsImport());
      assertFalse(JavaBasicType.LANG_LONG.needsImport());
      assertFalse(JavaBasicType.LANG_INTEGER.needsImport());
    }

    @Test
    @DisplayName("should not need import for primitive types")
    void needsImport_withPrimitiveTypes_shouldReturnFalse() {
      assertFalse(JavaBasicType.PRIMITIVE_INT.needsImport());
      assertFalse(JavaBasicType.PRIMITIVE_LONG.needsImport());
      assertFalse(JavaBasicType.PRIMITIVE_BOOLEAN.needsImport());
    }

    @Test
    @DisplayName("should need import for non-java.lang types")
    void needsImport_withNonJavaLangTypes_shouldReturnTrue() {
      assertTrue(JavaBasicType.MATH_BIG_DECIMAL.needsImport());
      assertTrue(JavaBasicType.TIME_LOCAL_DATE_TIME.needsImport());
      assertTrue(JavaBasicType.UTIL_DATE.needsImport());
    }
  }

  @Nested
  @DisplayName("Fully Qualified Name Tests")
  class FullyQualifiedNameTests {
    @Test
    @DisplayName("should return fully qualified name for types with package")
    void getFullyQualifiedName_withPackage_shouldReturnFullName() {
      assertEquals("java.math.BigDecimal", JavaBasicType.MATH_BIG_DECIMAL.getFullyQualifiedName());
      assertEquals(
          "java.time.LocalDateTime", JavaBasicType.TIME_LOCAL_DATE_TIME.getFullyQualifiedName());
      assertEquals("java.lang.String", JavaBasicType.LANG_STRING.getFullyQualifiedName());
    }

    @Test
    @DisplayName("should return type name for primitive types")
    void getFullyQualifiedName_withPrimitiveTypes_shouldReturnTypeName() {
      assertEquals("int", JavaBasicType.PRIMITIVE_INT.getFullyQualifiedName());
      assertEquals("long", JavaBasicType.PRIMITIVE_LONG.getFullyQualifiedName());
      assertEquals("boolean", JavaBasicType.PRIMITIVE_BOOLEAN.getFullyQualifiedName());
    }
  }

  @Nested
  @DisplayName("JPA Common Types Tests")
  class JPACommonTypesTests {
    @Test
    @DisplayName("should handle common JPA ID types")
    void shouldHandleCommonJPAIdTypes() {
      assertTrue(JavaBasicType.getByTypeName("Long").isPresent());
      assertTrue(JavaBasicType.getByTypeName("Integer").isPresent());
      assertTrue(JavaBasicType.getByTypeName("String").isPresent());
      assertTrue(JavaBasicType.getByTypeName("UUID").isPresent());
      assertTrue(JavaBasicType.getByTypeName("BigDecimal").isPresent());
    }

    @Test
    @DisplayName("should handle primitive JPA ID types")
    void shouldHandlePrimitiveJPAIdTypes() {
      assertTrue(JavaBasicType.getByTypeName("long").isPresent());
      assertTrue(JavaBasicType.getByTypeName("int").isPresent());
      assertFalse(JavaBasicType.getByTypeName("long").get().needsImport());
      assertFalse(JavaBasicType.getByTypeName("int").get().needsImport());
    }

    @Test
    @DisplayName("should handle JPA temporal types")
    void shouldHandleJPATemporalTypes() {
      assertTrue(JavaBasicType.getByTypeName("LocalDateTime").isPresent());
      assertTrue(JavaBasicType.getByTypeName("LocalDate").isPresent());
      assertTrue(JavaBasicType.getByTypeName("Instant").isPresent());
      assertTrue(JavaBasicType.getByTypeName("Date").isPresent());
      assertTrue(JavaBasicType.getByTypeName("LocalDateTime").get().needsImport());
      assertTrue(JavaBasicType.getByTypeName("LocalDate").get().needsImport());
      assertTrue(JavaBasicType.getByTypeName("Instant").get().needsImport());
    }
  }
}
