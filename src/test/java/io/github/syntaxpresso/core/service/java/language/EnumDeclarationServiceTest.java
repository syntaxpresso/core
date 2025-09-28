package io.github.syntaxpresso.core.service.java.language;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.treesitter.TSNode;

@DisplayName("EnumDeclarationService Tests")
class EnumDeclarationServiceTest {
  private EnumDeclarationService enumDeclarationService;

  private static final String SIMPLE_ENUM_CODE =
      """
      public enum Color {
          RED, GREEN, BLUE
      }
      """;

  private static final String COMPLEX_ENUM_CODE =
      """
      package com.example;

      import java.util.Arrays;

      public enum Status {
          ACTIVE("active", 1),
          INACTIVE("inactive", 0),
          PENDING("pending", 2);

          private final String name;
          private final int code;

          Status(String name, int code) {
              this.name = name;
              this.code = code;
          }

          public String getName() {
              return name;
          }

          public int getCode() {
              return code;
          }
      }
      """;

  private static final String MULTIPLE_ENUMS_CODE =
      """
      package com.example;

      enum InternalEnum {
          INTERNAL_VALUE
      }

      public enum PublicEnum {
          PUBLIC_VALUE
      }

      enum AnotherEnum {
          ANOTHER_VALUE
      }
      """;

  private static final String NO_PUBLIC_ENUM_CODE =
      """
      package com.example;

      enum InternalEnum {
          VALUE1
      }

      enum AnotherInternalEnum {
          VALUE2
      }
      """;

  @BeforeEach
  void setUp() {
    this.enumDeclarationService = new EnumDeclarationService();
  }

  @Nested
  @DisplayName("findEnumByName Tests")
  class FindEnumByNameTests {

    @Test
    @DisplayName("Should find enum by exact name")
    void shouldFindEnumByExactName() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_ENUM_CODE);
      Optional<TSNode> result = enumDeclarationService.findEnumByName(tsFile, "Color");

      assertTrue(result.isPresent());
      assertEquals("enum_declaration", result.get().getType());
    }

    @Test
    @DisplayName("Should find complex enum by name")
    void shouldFindComplexEnumByName() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, COMPLEX_ENUM_CODE);
      Optional<TSNode> result = enumDeclarationService.findEnumByName(tsFile, "Status");

      assertTrue(result.isPresent());
      assertEquals("enum_declaration", result.get().getType());
    }

    @Test
    @DisplayName("Should find specific enum from multiple enums")
    void shouldFindSpecificEnumFromMultiple() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, MULTIPLE_ENUMS_CODE);

      Optional<TSNode> internalResult =
          enumDeclarationService.findEnumByName(tsFile, "InternalEnum");
      Optional<TSNode> publicResult = enumDeclarationService.findEnumByName(tsFile, "PublicEnum");
      Optional<TSNode> anotherResult = enumDeclarationService.findEnumByName(tsFile, "AnotherEnum");

      assertTrue(internalResult.isPresent());
      assertTrue(publicResult.isPresent());
      assertTrue(anotherResult.isPresent());
      assertEquals("enum_declaration", internalResult.get().getType());
      assertEquals("enum_declaration", publicResult.get().getType());
      assertEquals("enum_declaration", anotherResult.get().getType());
    }

    @Test
    @DisplayName("Should return empty when enum not found")
    void shouldReturnEmptyWhenEnumNotFound() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_ENUM_CODE);
      Optional<TSNode> result = enumDeclarationService.findEnumByName(tsFile, "NonExistentEnum");

      assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should return empty when TSFile is null")
    void shouldReturnEmptyWhenTSFileIsNull() {
      Optional<TSNode> result = enumDeclarationService.findEnumByName(null, "Color");

      assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should return empty when enum name is null")
    void shouldReturnEmptyWhenEnumNameIsNull() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_ENUM_CODE);
      Optional<TSNode> result = enumDeclarationService.findEnumByName(tsFile, null);

      assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should return empty when enum name is empty")
    void shouldReturnEmptyWhenEnumNameIsEmpty() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_ENUM_CODE);
      Optional<TSNode> result = enumDeclarationService.findEnumByName(tsFile, "");

      assertFalse(result.isPresent());
    }
  }

  @Nested
  @DisplayName("getPublicEnum Tests")
  class GetPublicEnumTests {

    @Test
    @DisplayName("Should get public enum matching filename")
    void shouldGetPublicEnumMatchingFilename(@TempDir Path tempDir) throws IOException {
      Path enumFile = tempDir.resolve("Color.java");
      Files.writeString(enumFile, SIMPLE_ENUM_CODE);
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, enumFile);

      Optional<TSNode> result = enumDeclarationService.getPublicEnum(tsFile);

      assertTrue(result.isPresent());
      assertEquals("enum_declaration", result.get().getType());
    }

    @Test
    @DisplayName("Should get public enum matching filename for complex enum")
    void shouldGetPublicEnumMatchingFilenameForComplex(@TempDir Path tempDir) throws IOException {
      Path enumFile = tempDir.resolve("Status.java");
      Files.writeString(enumFile, COMPLEX_ENUM_CODE);
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, enumFile);

      Optional<TSNode> result = enumDeclarationService.getPublicEnum(tsFile);

      assertTrue(result.isPresent());
      assertEquals("enum_declaration", result.get().getType());
    }

    @Test
    @DisplayName("Should return empty when filename doesn't match any enum")
    void shouldReturnEmptyWhenFilenameDoesntMatchAnyEnum(@TempDir Path tempDir) throws IOException {
      Path enumFile = tempDir.resolve("WrongName.java");
      Files.writeString(enumFile, MULTIPLE_ENUMS_CODE);
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, enumFile);

      Optional<TSNode> result = enumDeclarationService.getPublicEnum(tsFile);

      assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should return empty when no public enum exists")
    void shouldReturnEmptyWhenNoPublicEnumExists(@TempDir Path tempDir) throws IOException {
      Path enumFile = tempDir.resolve("NoPublic.java");
      Files.writeString(enumFile, NO_PUBLIC_ENUM_CODE);
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, enumFile);

      Optional<TSNode> result = enumDeclarationService.getPublicEnum(tsFile);

      assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should fallback to first public enum when no filename available")
    void shouldFallbackToFirstPublicEnumWhenNoFilenameAvailable() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, MULTIPLE_ENUMS_CODE);

      Optional<TSNode> result = enumDeclarationService.getPublicEnum(tsFile);

      assertTrue(result.isPresent());
      assertEquals("enum_declaration", result.get().getType());
    }

    @Test
    @DisplayName("Should return empty when TSFile is null")
    void shouldReturnEmptyWhenTSFileIsNull() {
      Optional<TSNode> result = enumDeclarationService.getPublicEnum(null);

      assertFalse(result.isPresent());
    }
  }

  @Nested
  @DisplayName("getEnumNameNode Tests")
  class GetEnumNameNodeTests {

    @Test
    @DisplayName("Should get enum name node from simple enum")
    void shouldGetEnumNameNodeFromSimpleEnum() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_ENUM_CODE);
      Optional<TSNode> enumNode = enumDeclarationService.findEnumByName(tsFile, "Color");

      assertTrue(enumNode.isPresent());
      Optional<TSNode> nameNode = enumDeclarationService.getEnumNameNode(tsFile, enumNode.get());

      assertTrue(nameNode.isPresent());
      assertEquals("identifier", nameNode.get().getType());
      assertEquals("Color", tsFile.getTextFromNode(nameNode.get()));
    }

    @Test
    @DisplayName("Should get enum name node from complex enum")
    void shouldGetEnumNameNodeFromComplexEnum() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, COMPLEX_ENUM_CODE);
      Optional<TSNode> enumNode = enumDeclarationService.findEnumByName(tsFile, "Status");

      assertTrue(enumNode.isPresent());
      Optional<TSNode> nameNode = enumDeclarationService.getEnumNameNode(tsFile, enumNode.get());

      assertTrue(nameNode.isPresent());
      assertEquals("identifier", nameNode.get().getType());
      assertEquals("Status", tsFile.getTextFromNode(nameNode.get()));
    }

    @Test
    @DisplayName("Should get enum name node from multiple enums")
    void shouldGetEnumNameNodeFromMultipleEnums() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, MULTIPLE_ENUMS_CODE);
      Optional<TSNode> publicEnumNode = enumDeclarationService.findEnumByName(tsFile, "PublicEnum");

      assertTrue(publicEnumNode.isPresent());
      Optional<TSNode> nameNode =
          enumDeclarationService.getEnumNameNode(tsFile, publicEnumNode.get());

      assertTrue(nameNode.isPresent());
      assertEquals("identifier", nameNode.get().getType());
      assertEquals("PublicEnum", tsFile.getTextFromNode(nameNode.get()));
    }

    @Test
    @DisplayName("Should return empty when TSFile is null")
    void shouldReturnEmptyWhenTSFileIsNull() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_ENUM_CODE);
      Optional<TSNode> enumNode = enumDeclarationService.findEnumByName(tsFile, "Color");

      assertTrue(enumNode.isPresent());
      Optional<TSNode> nameNode = enumDeclarationService.getEnumNameNode(null, enumNode.get());

      assertFalse(nameNode.isPresent());
    }

    @Test
    @DisplayName("Should return empty when node is not enum_declaration")
    void shouldReturnEmptyWhenNodeIsNotEnumDeclaration() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_ENUM_CODE);
      TSNode rootNode = tsFile.getTree().getRootNode();

      Optional<TSNode> nameNode = enumDeclarationService.getEnumNameNode(tsFile, rootNode);

      assertFalse(nameNode.isPresent());
    }
  }

  @Nested
  @DisplayName("Edge Cases and Error Handling")
  class EdgeCasesAndErrorHandlingTests {

    @Test
    @DisplayName("Should handle enum with methods and fields")
    void shouldHandleEnumWithMethodsAndFields() {
      String enumWithMethodsCode =
          """
          public enum Size {
              SMALL(1), MEDIUM(2), LARGE(3);
              
              private final int value;
              
              Size(int value) {
                  this.value = value;
              }
              
              public int getValue() {
                  return value;
              }
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, enumWithMethodsCode);
      Optional<TSNode> result = enumDeclarationService.findEnumByName(tsFile, "Size");

      assertTrue(result.isPresent());
      assertEquals("enum_declaration", result.get().getType());
    }

    @Test
    @DisplayName("Should handle enum with annotations")
    void shouldHandleEnumWithAnnotations() {
      String annotatedEnumCode =
          """
          @Entity
          @Table(name = "status")
          public enum Status {
              ACTIVE, INACTIVE
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, annotatedEnumCode);
      Optional<TSNode> result = enumDeclarationService.findEnumByName(tsFile, "Status");

      assertTrue(result.isPresent());
      assertEquals("enum_declaration", result.get().getType());
    }

    @Test
    @DisplayName("Should handle nested enums")
    void shouldHandleNestedEnums() {
      String nestedEnumCode =
          """
          public class OuterClass {
              public enum InnerEnum {
                  VALUE1, VALUE2
              }
              
              private enum PrivateEnum {
                  PRIVATE_VALUE
              }
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, nestedEnumCode);
      Optional<TSNode> result = enumDeclarationService.findEnumByName(tsFile, "InnerEnum");

      assertTrue(result.isPresent());
      assertEquals("enum_declaration", result.get().getType());
    }

    @Test
    @DisplayName("Should handle empty enum")
    void shouldHandleEmptyEnum() {
      String emptyEnumCode =
          """
          public enum EmptyEnum {
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, emptyEnumCode);
      Optional<TSNode> result = enumDeclarationService.findEnumByName(tsFile, "EmptyEnum");

      assertTrue(result.isPresent());
      assertEquals("enum_declaration", result.get().getType());
    }
  }
}