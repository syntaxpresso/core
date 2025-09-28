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

@DisplayName("RecordDeclarationService Tests")
class RecordDeclarationServiceTest {
  private RecordDeclarationService recordDeclarationService;

  private static final String SIMPLE_RECORD_CODE =
      """
      public record Person(String name, int age) {}
      """;

  private static final String COMPLEX_RECORD_CODE =
      """
      package com.example;

      import java.time.LocalDate;
      import java.util.Objects;

      public record Employee(
          String firstName,
          String lastName,
          LocalDate birthDate,
          double salary
      ) {
          public Employee {
              Objects.requireNonNull(firstName);
              Objects.requireNonNull(lastName);
              if (salary < 0) {
                  throw new IllegalArgumentException("Salary cannot be negative");
              }
          }

          public String fullName() {
              return firstName + " " + lastName;
          }
      }
      """;

  private static final String MULTIPLE_RECORDS_CODE =
      """
      package com.example;

      record InternalRecord(String value) {}

      public record PublicRecord(int id, String name) {}

      record AnotherRecord(boolean flag) {}
      """;

  private static final String NO_PUBLIC_RECORD_CODE =
      """
      package com.example;

      record InternalRecord(String value1) {}

      record AnotherInternalRecord(String value2) {}
      """;

  @BeforeEach
  void setUp() {
    this.recordDeclarationService = new RecordDeclarationService();
  }

  @Nested
  @DisplayName("findRecordByName Tests")
  class FindRecordByNameTests {

    @Test
    @DisplayName("Should find record by exact name")
    void shouldFindRecordByExactName() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_RECORD_CODE);
      Optional<TSNode> result = recordDeclarationService.findRecordByName(tsFile, "Person");

      assertTrue(result.isPresent());
      assertEquals("record_declaration", result.get().getType());
    }

    @Test
    @DisplayName("Should find complex record by name")
    void shouldFindComplexRecordByName() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, COMPLEX_RECORD_CODE);
      Optional<TSNode> result = recordDeclarationService.findRecordByName(tsFile, "Employee");

      assertTrue(result.isPresent());
      assertEquals("record_declaration", result.get().getType());
    }

    @Test
    @DisplayName("Should find specific record from multiple records")
    void shouldFindSpecificRecordFromMultiple() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, MULTIPLE_RECORDS_CODE);

      Optional<TSNode> internalResult =
          recordDeclarationService.findRecordByName(tsFile, "InternalRecord");
      Optional<TSNode> publicResult =
          recordDeclarationService.findRecordByName(tsFile, "PublicRecord");
      Optional<TSNode> anotherResult =
          recordDeclarationService.findRecordByName(tsFile, "AnotherRecord");

      assertTrue(internalResult.isPresent());
      assertTrue(publicResult.isPresent());
      assertTrue(anotherResult.isPresent());
      assertEquals("record_declaration", internalResult.get().getType());
      assertEquals("record_declaration", publicResult.get().getType());
      assertEquals("record_declaration", anotherResult.get().getType());
    }

    @Test
    @DisplayName("Should return empty when record not found")
    void shouldReturnEmptyWhenRecordNotFound() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_RECORD_CODE);
      Optional<TSNode> result =
          recordDeclarationService.findRecordByName(tsFile, "NonExistentRecord");

      assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should return empty when TSFile is null")
    void shouldReturnEmptyWhenTSFileIsNull() {
      Optional<TSNode> result = recordDeclarationService.findRecordByName(null, "Person");

      assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should return empty when record name is null")
    void shouldReturnEmptyWhenRecordNameIsNull() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_RECORD_CODE);
      Optional<TSNode> result = recordDeclarationService.findRecordByName(tsFile, null);

      assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should return empty when record name is empty")
    void shouldReturnEmptyWhenRecordNameIsEmpty() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_RECORD_CODE);
      Optional<TSNode> result = recordDeclarationService.findRecordByName(tsFile, "");

      assertFalse(result.isPresent());
    }
  }

  @Nested
  @DisplayName("getPublicRecord Tests")
  class GetPublicRecordTests {

    @Test
    @DisplayName("Should get public record matching filename")
    void shouldGetPublicRecordMatchingFilename(@TempDir Path tempDir) throws IOException {
      Path recordFile = tempDir.resolve("Person.java");
      Files.writeString(recordFile, SIMPLE_RECORD_CODE);
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, recordFile);

      Optional<TSNode> result = recordDeclarationService.getPublicRecord(tsFile);

      assertTrue(result.isPresent());
      assertEquals("record_declaration", result.get().getType());
    }

    @Test
    @DisplayName("Should get public record matching filename for complex record")
    void shouldGetPublicRecordMatchingFilenameForComplex(@TempDir Path tempDir) throws IOException {
      Path recordFile = tempDir.resolve("Employee.java");
      Files.writeString(recordFile, COMPLEX_RECORD_CODE);
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, recordFile);

      Optional<TSNode> result = recordDeclarationService.getPublicRecord(tsFile);

      assertTrue(result.isPresent());
      assertEquals("record_declaration", result.get().getType());
    }

    @Test
    @DisplayName("Should return empty when filename doesn't match any record")
    void shouldReturnEmptyWhenFilenameDoesntMatchAnyRecord(@TempDir Path tempDir)
        throws IOException {
      Path recordFile = tempDir.resolve("WrongName.java");
      Files.writeString(recordFile, MULTIPLE_RECORDS_CODE);
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, recordFile);

      Optional<TSNode> result = recordDeclarationService.getPublicRecord(tsFile);

      assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should return empty when no public record exists")
    void shouldReturnEmptyWhenNoPublicRecordExists(@TempDir Path tempDir) throws IOException {
      Path recordFile = tempDir.resolve("NoPublic.java");
      Files.writeString(recordFile, NO_PUBLIC_RECORD_CODE);
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, recordFile);

      Optional<TSNode> result = recordDeclarationService.getPublicRecord(tsFile);

      assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should fallback to first public record when no filename available")
    void shouldFallbackToFirstPublicRecordWhenNoFilenameAvailable() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, MULTIPLE_RECORDS_CODE);

      Optional<TSNode> result = recordDeclarationService.getPublicRecord(tsFile);

      assertTrue(result.isPresent());
      assertEquals("record_declaration", result.get().getType());
    }

    @Test
    @DisplayName("Should return empty when TSFile is null")
    void shouldReturnEmptyWhenTSFileIsNull() {
      Optional<TSNode> result = recordDeclarationService.getPublicRecord(null);

      assertFalse(result.isPresent());
    }
  }

  @Nested
  @DisplayName("getRecordNameNode Tests")
  class GetRecordNameNodeTests {

    @Test
    @DisplayName("Should get record name node from simple record")
    void shouldGetRecordNameNodeFromSimpleRecord() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_RECORD_CODE);
      Optional<TSNode> recordNode = recordDeclarationService.findRecordByName(tsFile, "Person");

      assertTrue(recordNode.isPresent());
      Optional<TSNode> nameNode =
          recordDeclarationService.getRecordNameNode(tsFile, recordNode.get());

      assertTrue(nameNode.isPresent());
      assertEquals("identifier", nameNode.get().getType());
      assertEquals("Person", tsFile.getTextFromNode(nameNode.get()));
    }

    @Test
    @DisplayName("Should get record name node from complex record")
    void shouldGetRecordNameNodeFromComplexRecord() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, COMPLEX_RECORD_CODE);
      Optional<TSNode> recordNode = recordDeclarationService.findRecordByName(tsFile, "Employee");

      assertTrue(recordNode.isPresent());
      Optional<TSNode> nameNode =
          recordDeclarationService.getRecordNameNode(tsFile, recordNode.get());

      assertTrue(nameNode.isPresent());
      assertEquals("identifier", nameNode.get().getType());
      assertEquals("Employee", tsFile.getTextFromNode(nameNode.get()));
    }

    @Test
    @DisplayName("Should get record name node from multiple records")
    void shouldGetRecordNameNodeFromMultipleRecords() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, MULTIPLE_RECORDS_CODE);
      Optional<TSNode> publicRecordNode =
          recordDeclarationService.findRecordByName(tsFile, "PublicRecord");

      assertTrue(publicRecordNode.isPresent());
      Optional<TSNode> nameNode =
          recordDeclarationService.getRecordNameNode(tsFile, publicRecordNode.get());

      assertTrue(nameNode.isPresent());
      assertEquals("identifier", nameNode.get().getType());
      assertEquals("PublicRecord", tsFile.getTextFromNode(nameNode.get()));
    }

    @Test
    @DisplayName("Should return empty when TSFile is null")
    void shouldReturnEmptyWhenTSFileIsNull() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_RECORD_CODE);
      Optional<TSNode> recordNode = recordDeclarationService.findRecordByName(tsFile, "Person");

      assertTrue(recordNode.isPresent());
      Optional<TSNode> nameNode = recordDeclarationService.getRecordNameNode(null, recordNode.get());

      assertFalse(nameNode.isPresent());
    }

    @Test
    @DisplayName("Should return empty when node is not record_declaration")
    void shouldReturnEmptyWhenNodeIsNotRecordDeclaration() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_RECORD_CODE);
      TSNode rootNode = tsFile.getTree().getRootNode();

      Optional<TSNode> nameNode = recordDeclarationService.getRecordNameNode(tsFile, rootNode);

      assertFalse(nameNode.isPresent());
    }
  }

  @Nested
  @DisplayName("Edge Cases and Error Handling")
  class EdgeCasesAndErrorHandlingTests {

    @Test
    @DisplayName("Should handle record with generic parameters")
    void shouldHandleRecordWithGenericParameters() {
      String genericRecordCode =
          """
          public record Container<T>(T value) {}
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, genericRecordCode);
      Optional<TSNode> result = recordDeclarationService.findRecordByName(tsFile, "Container");

      assertTrue(result.isPresent());
      assertEquals("record_declaration", result.get().getType());
    }

    @Test
    @DisplayName("Should handle record with annotations")
    void shouldHandleRecordWithAnnotations() {
      String annotatedRecordCode =
          """
          @Entity
          @Table(name = "users")
          public record User(
              @Id Long id,
              @Column(name = "username") String username
          ) {}
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, annotatedRecordCode);
      Optional<TSNode> result = recordDeclarationService.findRecordByName(tsFile, "User");

      assertTrue(result.isPresent());
      assertEquals("record_declaration", result.get().getType());
    }

    @Test
    @DisplayName("Should handle nested records")
    void shouldHandleNestedRecords() {
      String nestedRecordCode =
          """
          public class OuterClass {
              public record InnerRecord(String value) {}
              
              private record PrivateRecord(int number) {}
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, nestedRecordCode);
      Optional<TSNode> result = recordDeclarationService.findRecordByName(tsFile, "InnerRecord");

      assertTrue(result.isPresent());
      assertEquals("record_declaration", result.get().getType());
    }

    @Test
    @DisplayName("Should handle record with compact constructor")
    void shouldHandleRecordWithCompactConstructor() {
      String compactConstructorCode =
          """
          public record Range(int start, int end) {
              public Range {
                  if (start > end) {
                      throw new IllegalArgumentException("Start cannot be greater than end");
                  }
              }
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, compactConstructorCode);
      Optional<TSNode> result = recordDeclarationService.findRecordByName(tsFile, "Range");

      assertTrue(result.isPresent());
      assertEquals("record_declaration", result.get().getType());
    }

    @Test
    @DisplayName("Should handle record with static methods")
    void shouldHandleRecordWithStaticMethods() {
      String recordWithStaticMethodsCode =
          """
          public record Point(int x, int y) {
              public static Point origin() {
                  return new Point(0, 0);
              }
              
              public double distanceFromOrigin() {
                  return Math.sqrt(x * x + y * y);
              }
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, recordWithStaticMethodsCode);
      Optional<TSNode> result = recordDeclarationService.findRecordByName(tsFile, "Point");

      assertTrue(result.isPresent());
      assertEquals("record_declaration", result.get().getType());
    }
  }
}