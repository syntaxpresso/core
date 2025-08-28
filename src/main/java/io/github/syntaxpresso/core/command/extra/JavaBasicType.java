package io.github.syntaxpresso.core.command.extra;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public enum JavaBasicType {
  LANG_STRING("String", "java.lang"),
  LANG_LONG("Long", "java.lang"),
  LANG_INTEGER("Integer", "java.lang"),
  LANG_BOOLEAN("Boolean", "java.lang"),
  LANG_DOUBLE("Double", "java.lang"),
  LANG_BYTE_ARRAY("Byte[]", "java.lang"),
  LANG_BYTE("Byte", "java.lang"),
  LANG_CHARACTER("Character", "java.lang"),
  LANG_SHORT("Short", "java.lang"),
  LANG_FLOAT("Float", "java.lang"),
  LANG_CLASS("Class", "java.lang"),
  LANG_CHARACTER_ARRAY("Character[]", "java.lang"),
  MATH_BIG_DECIMAL("BigDecimal", "java.math"),
  MATH_BIG_INTEGER("BigInteger", "java.math"),
  TIME_INSTANT("Instant", "java.time"),
  TIME_LOCAL_DATE_TIME("LocalDateTime", "java.time"),
  TIME_LOCAL_DATE("LocalDate", "java.time"),
  TIME_LOCAL_TIME("LocalTime", "java.time"),
  TIME_OFFSET_DATE_TIME("OffsetDateTime", "java.time"),
  TIME_OFFSET_TIME("OffsetTime", "java.time"),
  TIME_DURATION("Duration", "java.time"),
  TIME_ZONED_DATE_TIME("ZonedDateTime", "java.time"),
  TIME_ZONE_OFFSET("ZoneOffset", "java.time"),
  UTIL_DATE("Date", "java.util"),
  UTIL_TIME_ZONE("TimeZone", "java.util"),
  UTIL_CALENDAR("Calendar", "java.util"),
  UTIL_LOCALE("Locale", "java.util"),
  UTIL_CURRENCY("Currency", "java.util"),
  UTIL_UUID("UUID", "java.util"),
  SQL_DATE("Date", "java.sql"),
  SQL_TIME("Time", "java.sql"),
  SQL_TIMESTAMP("Timestamp", "java.sql"),
  SQL_BLOB("Blob", "java.sql"),
  SQL_CLOB("Clob", "java.sql"),
  SQL_NCLOB("NClob", "java.sql"),
  NET_URL("URL", "java.net"),
  NET_INET_ADDRESS("InetAddress", "java.net"),
  GEOLATTE_GEOMETRY("Geometry", "org.geolatte.geom"),
  VIVIDSOLUTIONS_GEOMETRY("Geometry", "com.vividsolutions.jts.geom"),
  PRIMITIVE_BOOLEAN("boolean", null),
  PRIMITIVE_BYTE("byte", null),
  PRIMITIVE_FLOAT("float", null),
  PRIMITIVE_CHAR("char", null),
  PRIMITIVE_INT("int", null),
  PRIMITIVE_DOUBLE("double", null),
  PRIMITIVE_SHORT("short", null),
  PRIMITIVE_LONG("long", null),
  PRIMITIVE_BYTE_ARRAY("byte[]", null),
  PRIMITIVE_CHAR_ARRAY("char[]", null);
  private final String typeName;
  private final String packageName;
  private static final Set<JavaBasicType> RECOMMENDED_ID_TYPES;

  static {
    // A highly efficient Set implementation for enums, containing recommended types.
    RECOMMENDED_ID_TYPES =
        EnumSet.of(
            PRIMITIVE_INT,
            LANG_INTEGER,
            PRIMITIVE_LONG,
            LANG_LONG,
            PRIMITIVE_SHORT,
            LANG_SHORT,
            PRIMITIVE_BYTE,
            LANG_BYTE,
            MATH_BIG_INTEGER,
            LANG_STRING,
            UTIL_UUID,
            PRIMITIVE_CHAR,
            LANG_CHARACTER);
  }

  JavaBasicType(String typeName, String packageName) {
    this.typeName = typeName;
    this.packageName = packageName;
  }

  public String getTypeName() {
    return typeName;
  }

  public Optional<String> getPackageName() {
    return Optional.ofNullable(packageName);
  }

  public String getFullyQualifiedName() {
    return getPackageName().map(pkg -> pkg + "." + typeName).orElse(typeName);
  }

  public boolean isPrimitive() {
    return packageName == null;
  }

  /**
   * Gets a list of recommended ID types, with each type formatted as a simple map suitable for
   * direct serialization to JSON.
   *
   * @return A List of maps, where each map represents the details of a recommended type.
   */
  public static List<Map<String, String>> getRecommendedIdTypesForJson() {
    List<Map<String, String>> recommendedList = new ArrayList<>();
    // Iterate only over the predefined set of recommended types for efficiency.
    for (JavaBasicType type : RECOMMENDED_ID_TYPES) {
      Map<String, String> typeDetails = new HashMap<>();
      typeDetails.put("typeName", type.getTypeName());
      typeDetails.put("fullyQualifiedName", type.getFullyQualifiedName());
      typeDetails.put("isPrimitive", String.valueOf(type.isPrimitive()));
      recommendedList.add(typeDetails);
    }
    return recommendedList;
  }

  /**
   * Finds a JavaBasicType by its type name.
   *
   * @param typeName the simple type name to search for
   * @return Optional containing the matching JavaBasicType, or empty if not found
   */
  public static Optional<JavaBasicType> getByTypeName(String typeName) {
    for (JavaBasicType type : values()) {
      if (type.getTypeName().equals(typeName)) {
        return Optional.of(type);
      }
    }
    return Optional.empty();
  }

  /**
   * Gets the package name for a given type name.
   *
   * @param typeName the simple type name to search for
   * @return Optional containing the package name, or empty if type not found or is primitive
   */
  public static Optional<String> getPackageNameByTypeName(String typeName) {
    return getByTypeName(typeName)
        .flatMap(JavaBasicType::getPackageName);
  }

  @Override
  public String toString() {
    return getFullyQualifiedName();
  }
}
