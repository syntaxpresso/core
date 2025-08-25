package io.github.syntaxpresso.core.service.java.language;

import java.util.Optional;

public enum JavaBasicType {
  STRING("String", "java.lang"),
  LONG("Long", "java.lang"),
  INTEGER("Integer", "java.lang"),
  BOOLEAN("Boolean", "java.lang"),
  DOUBLE("Double", "java.lang"),
  BIG_DECIMAL("BigDecimal", "java.math"),
  INSTANT("Instant", "java.time"),
  LOCAL_DATE_TIME("LocalDateTime", "java.time"),
  LOCAL_DATE("LocalDate", "java.time"),
  LOCAL_TIME("LocalTime", "java.time"),
  OFFSET_DATE_TIME("OffsetDateTime", "java.time"),
  OFFSET_TIME("OffsetTime", "java.time"),
  UTIL_DATE("Date", "java.util"),
  SQL_DATE("Date", "java.sql"),
  SQL_TIME("Time", "java.sql"),
  TIMESTAMP("Timestamp", "java.sql"),
  TIME_ZONE("TimeZone", "java.util"),
  BYTE_ARRAY("Byte[]", "java.lang"),
  BLOB("Blob", "java.sql"),
  BYTE("Byte", "java.lang"),
  CHARACTER("Character", "java.lang"),
  SHORT("Short", "java.lang"),
  FLOAT("Float", "java.lang"),
  BIG_INTEGER("BigInteger", "java.math"),
  URL("URL", "java.net"),
  DURATION("Duration", "java.time"),
  ZONED_DATE_TIME("ZonedDateTime", "java.time"),
  CALENDAR("Calendar", "java.util"),
  LOCALE("Locale", "java.util"),
  CURRENCY("Currency", "java.util"),
  CLASS("Class", "java.lang"),
  UUID("UUID", "java.util"),
  CHARACTER_ARRAY("Character[]", "java.lang"),
  CLOB("Clob", "java.sql"),
  NCLOB("NClob", "java.sql"),
  GEOLATTE_GEOMETRY("Geometry", "org.geolatte.geom"),
  JTS_GEOMETRY("Geometry", "com.vividsolutions.jts.geom"),
  INET_ADDRESS("InetAddress", "java.net"),
  ZONE_OFFSET("ZoneOffset", "java.time"),
  // Primitive types (no import needed)
  BOOLEAN_PRIMITIVE("boolean", null),
  BYTE_PRIMITIVE("byte", null),
  FLOAT_PRIMITIVE("float", null),
  CHAR_PRIMITIVE("char", null),
  INT_PRIMITIVE("int", null),
  DOUBLE_PRIMITIVE("double", null),
  SHORT_PRIMITIVE("short", null),
  LONG_PRIMITIVE("long", null),
  BYTE_ARRAY_PRIMITIVE("byte[]", null),
  CHAR_ARRAY_PRIMITIVE("char[]", null);
  private final String typeName;
  private final String packageName;

  JavaBasicType(String typeName, String packageName) {
    this.typeName = typeName;
    this.packageName = packageName;
  }

  public String getTypeName() {
    return this.typeName;
  }

  public String getPackageName() {
    return this.packageName;
  }

  /**
   * Checks if this type needs an import statement.
   *
   * @return True if the type needs an import, false for primitives and java.lang types.
   */
  public boolean needsImport() {
    return this.packageName != null && !"java.lang".equals(this.packageName);
  }

  /**
   * Gets the fully qualified name for this type.
   *
   * @return The fully qualified name, or just the type name for primitives.
   */
  public String getFullyQualifiedName() {
    if (this.packageName == null) {
      return this.typeName;
    }
    return this.packageName + "." + this.typeName;
  }

  /**
   * Finds a JavaBasicType by its type name.
   *
   * @param typeName The type name to search for.
   * @return An Optional containing the matching JavaBasicType, or empty if not found.
   */
  public static Optional<JavaBasicType> fromTypeName(String typeName) {
    if (typeName == null) {
      return Optional.empty();
    }
    for (JavaBasicType basicType : values()) {
      if (basicType.typeName.equals(typeName)) {
        return Optional.of(basicType);
      }
    }
    return Optional.empty();
  }

  /**
   * Finds the first JavaBasicType by its type name and package. Useful for disambiguating types
   * like Date that exist in multiple packages.
   *
   * @param typeName The type name to search for.
   * @param packageName The package name to match.
   * @return An Optional containing the matching JavaBasicType, or empty if not found.
   */
  public static Optional<JavaBasicType> fromTypeNameAndPackage(
      String typeName, String packageName) {
    if (typeName == null) {
      return Optional.empty();
    }
    for (JavaBasicType basicType : values()) {
      if (basicType.typeName.equals(typeName)
          && ((basicType.packageName == null && packageName == null)
              || (basicType.packageName != null && basicType.packageName.equals(packageName)))) {
        return Optional.of(basicType);
      }
    }
    return Optional.empty();
  }
}
