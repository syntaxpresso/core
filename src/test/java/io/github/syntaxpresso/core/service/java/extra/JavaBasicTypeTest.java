package io.github.syntaxpresso.core.service.java.extra;

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
      Optional<JavaBasicType> result = JavaBasicType.fromTypeName("String");
      assertTrue(result.isPresent());
      assertEquals(JavaBasicType.STRING, result.get());
      assertEquals("java.lang", result.get().getPackageName());
    }

    @Test
    @DisplayName("should find type by name for primitive types")
    void fromTypeName_withPrimitiveTypes_shouldReturnCorrectType() {
      Optional<JavaBasicType> result = JavaBasicType.fromTypeName("long");
      assertTrue(result.isPresent());
      assertEquals(JavaBasicType.LONG_PRIMITIVE, result.get());
      assertEquals(null, result.get().getPackageName());
    }

    @Test
    @DisplayName("should find type by name for time types")
    void fromTypeName_withTimeTypes_shouldReturnCorrectType() {
      Optional<JavaBasicType> result = JavaBasicType.fromTypeName("LocalDateTime");
      assertTrue(result.isPresent());
      assertEquals(JavaBasicType.LOCAL_DATE_TIME, result.get());
      assertEquals("java.time", result.get().getPackageName());
    }

    @Test
    @DisplayName("should find type by name for math types")
    void fromTypeName_withMathTypes_shouldReturnCorrectType() {
      Optional<JavaBasicType> result = JavaBasicType.fromTypeName("BigDecimal");
      assertTrue(result.isPresent());
      assertEquals(JavaBasicType.BIG_DECIMAL, result.get());
      assertEquals("java.math", result.get().getPackageName());
    }

    @Test
    @DisplayName("should return empty for unknown type name")
    void fromTypeName_withUnknownType_shouldReturnEmpty() {
      Optional<JavaBasicType> result = JavaBasicType.fromTypeName("UnknownType");
      assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("should return empty for null type name")
    void fromTypeName_withNullTypeName_shouldReturnEmpty() {
      Optional<JavaBasicType> result = JavaBasicType.fromTypeName(null);
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
      assertEquals(JavaBasicType.INT_PRIMITIVE, result.get());
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
      assertFalse(JavaBasicType.STRING.needsImport());
      assertFalse(JavaBasicType.LONG.needsImport());
      assertFalse(JavaBasicType.INTEGER.needsImport());
    }

    @Test
    @DisplayName("should not need import for primitive types")
    void needsImport_withPrimitiveTypes_shouldReturnFalse() {
      assertFalse(JavaBasicType.INT_PRIMITIVE.needsImport());
      assertFalse(JavaBasicType.LONG_PRIMITIVE.needsImport());
      assertFalse(JavaBasicType.BOOLEAN_PRIMITIVE.needsImport());
    }

    @Test
    @DisplayName("should need import for non-java.lang types")
    void needsImport_withNonJavaLangTypes_shouldReturnTrue() {
      assertTrue(JavaBasicType.BIG_DECIMAL.needsImport());
      assertTrue(JavaBasicType.LOCAL_DATE_TIME.needsImport());
      assertTrue(JavaBasicType.UTIL_DATE.needsImport());
    }
  }

  @Nested
  @DisplayName("Fully Qualified Name Tests")
  class FullyQualifiedNameTests {
    @Test
    @DisplayName("should return fully qualified name for types with package")
    void getFullyQualifiedName_withPackage_shouldReturnFullName() {
      assertEquals("java.math.BigDecimal", JavaBasicType.BIG_DECIMAL.getFullyQualifiedName());
      assertEquals(
          "java.time.LocalDateTime", JavaBasicType.LOCAL_DATE_TIME.getFullyQualifiedName());
      assertEquals("java.lang.String", JavaBasicType.STRING.getFullyQualifiedName());
    }

    @Test
    @DisplayName("should return type name for primitive types")
    void getFullyQualifiedName_withPrimitiveTypes_shouldReturnTypeName() {
      assertEquals("int", JavaBasicType.INT_PRIMITIVE.getFullyQualifiedName());
      assertEquals("long", JavaBasicType.LONG_PRIMITIVE.getFullyQualifiedName());
      assertEquals("boolean", JavaBasicType.BOOLEAN_PRIMITIVE.getFullyQualifiedName());
    }
  }

  @Nested
  @DisplayName("JPA Common Types Tests")
  class JPACommonTypesTests {
    @Test
    @DisplayName("should handle common JPA ID types")
    void shouldHandleCommonJPAIdTypes() {
      assertTrue(JavaBasicType.fromTypeName("Long").isPresent());
      assertTrue(JavaBasicType.fromTypeName("Integer").isPresent());
      assertTrue(JavaBasicType.fromTypeName("String").isPresent());
      assertTrue(JavaBasicType.fromTypeName("UUID").isPresent());
      assertTrue(JavaBasicType.fromTypeName("BigDecimal").isPresent());
    }

    @Test
    @DisplayName("should handle primitive JPA ID types")
    void shouldHandlePrimitiveJPAIdTypes() {
      assertTrue(JavaBasicType.fromTypeName("long").isPresent());
      assertTrue(JavaBasicType.fromTypeName("int").isPresent());
      assertFalse(JavaBasicType.fromTypeName("long").get().needsImport());
      assertFalse(JavaBasicType.fromTypeName("int").get().needsImport());
    }

    @Test
    @DisplayName("should handle JPA temporal types")
    void shouldHandleJPATemporalTypes() {
      assertTrue(JavaBasicType.fromTypeName("LocalDateTime").isPresent());
      assertTrue(JavaBasicType.fromTypeName("LocalDate").isPresent());
      assertTrue(JavaBasicType.fromTypeName("Instant").isPresent());
      assertTrue(JavaBasicType.fromTypeName("Date").isPresent());
      assertTrue(JavaBasicType.fromTypeName("LocalDateTime").get().needsImport());
      assertTrue(JavaBasicType.fromTypeName("LocalDate").get().needsImport());
      assertTrue(JavaBasicType.fromTypeName("Instant").get().needsImport());
    }
  }
}

