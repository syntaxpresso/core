package io.github.syntaxpresso.core.service.java.jpa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import io.github.syntaxpresso.core.service.java.JavaLanguageService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("JPAEntityService Tests")
class JPAEntityServiceTest {
  @TempDir Path tempDir;
  private JPAEntityService jpaEntityService;
  private JavaLanguageService javaLanguageService;

  @BeforeEach
  void setUp() {
    this.javaLanguageService = new JavaLanguageService();
    this.jpaEntityService = new JPAEntityService(javaLanguageService);
  }

  @Nested
  @DisplayName("isJPAEntity Tests")
  class IsJPAEntityTests {

    @Test
    @DisplayName("Should return true for class with @Entity annotation")
    void shouldReturnTrueForEntityAnnotation() throws IOException {
      String javaCode =
          """
          package com.example;

          import jakarta.persistence.Entity;

          @Entity
          public class User {
              private Long id;
              private String name;
          }
          """;
      Path javaFile = tempDir.resolve("User.java");
      Files.writeString(javaFile, javaCode);
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, javaFile);

      boolean result = jpaEntityService.isJPAEntity(tsFile);

      assertTrue(result);
    }

    @Test
    @DisplayName("Should return true for class with fully qualified @jakarta.persistence.Entity")
    void shouldReturnTrueForFullyQualifiedEntity() throws IOException {
      String javaCode =
          """
          package com.example;

          @jakarta.persistence.Entity
          public class Product {
              private Long id;
              private String name;
          }
          """;
      Path javaFile = tempDir.resolve("Product.java");
      Files.writeString(javaFile, javaCode);
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, javaFile);

      boolean result = jpaEntityService.isJPAEntity(tsFile);

      assertTrue(result);
    }

    @Test
    @DisplayName("Should return true for class with @Entity and wildcard import")
    void shouldReturnTrueForWildcardImport() throws IOException {
      String javaCode =
          """
          package com.example;

          import jakarta.persistence.*;

          @Entity
          public class Order {
              private Long id;
              private String orderNumber;
          }
          """;
      Path javaFile = tempDir.resolve("Order.java");
      Files.writeString(javaFile, javaCode);
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, javaFile);

      boolean result = jpaEntityService.isJPAEntity(tsFile);

      assertTrue(result);
    }

    @Test
    @DisplayName("Should return false for class without @Entity annotation")
    void shouldReturnFalseForNonEntity() throws IOException {
      String javaCode =
          """
          package com.example;

          public class RegularClass {
              private String value;
          }
          """;
      Path javaFile = tempDir.resolve("RegularClass.java");
      Files.writeString(javaFile, javaCode);
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, javaFile);

      boolean result = jpaEntityService.isJPAEntity(tsFile);

      assertFalse(result);
    }

    @Test
    @DisplayName("Should return false for class with other annotations")
    void shouldReturnFalseForOtherAnnotations() throws IOException {
      String javaCode =
          """
          package com.example;

          import org.springframework.stereotype.Service;

          @Service
          public class UserService {
              public void doSomething() {}
          }
          """;
      Path javaFile = tempDir.resolve("UserService.java");
      Files.writeString(javaFile, javaCode);
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, javaFile);

      boolean result = jpaEntityService.isJPAEntity(tsFile);

      assertFalse(result);
    }

    @Test
    @DisplayName("Should return true for class with @Entity(name = \"customName\")")
    void shouldReturnTrueForEntityWithNameAnnotation() throws IOException {
      String javaCode =
          """
          package com.example;

          import jakarta.persistence.Entity;

          @Entity(name = "user_table")
          public class User {
              private Long id;
              private String name;
          }
          """;
      Path javaFile = tempDir.resolve("User.java");
      Files.writeString(javaFile, javaCode);
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, javaFile);

      boolean result = jpaEntityService.isJPAEntity(tsFile);

      assertTrue(result);
    }

    @Test
    @DisplayName(
        "Should return true for fully qualified @jakarta.persistence.Entity(name = \"customName\")")
    void shouldReturnTrueForFullyQualifiedEntityWithName() throws IOException {
      String javaCode =
          """
          package com.example;

          @jakarta.persistence.Entity(name = "product_table")
          public class Product {
              private Long id;
              private String name;
          }
          """;
      Path javaFile = tempDir.resolve("Product.java");
      Files.writeString(javaFile, javaCode);
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, javaFile);

      boolean result = jpaEntityService.isJPAEntity(tsFile);

      assertTrue(result);
    }

    @Test
    @DisplayName("Should return true for @Entity(name = \"customName\") with wildcard import")
    void shouldReturnTrueForEntityWithNameAndWildcardImport() throws IOException {
      String javaCode =
          """
          package com.example;

          import jakarta.persistence.*;

          @Entity(name = "order_table")
          public class Order {
              private Long id;
              private String orderNumber;
          }
          """;
      Path javaFile = tempDir.resolve("Order.java");
      Files.writeString(javaFile, javaCode);
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, javaFile);

      boolean result = jpaEntityService.isJPAEntity(tsFile);

      assertTrue(result);
    }
  }

  @Nested
  @DisplayName("getJPAEntityName Tests")
  class GetJPAEntityNameTests {

    @Test
    @DisplayName("Should return class name for entity with marker annotation")
    void shouldReturnClassNameForMarkerAnnotation() throws IOException {
      String javaCode =
          """
          package com.example;

          import jakarta.persistence.Entity;

          @Entity
          public class Customer {
              private Long id;
              private String name;
          }
          """;
      Path javaFile = tempDir.resolve("Customer.java");
      Files.writeString(javaFile, javaCode);
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, javaFile);

      Optional<String> result = jpaEntityService.getJPAEntityName(tsFile);

      assertTrue(result.isPresent());
      assertEquals("Customer", result.get());
    }

    // @Test
    // @DisplayName("Should return class name for fully qualified entity annotation")
    // void shouldReturnClassNameForFullyQualifiedAnnotation() throws IOException {
    //   String javaCode =
    //       """
    //       package com.example;
    //
    //       @jakarta.persistence.Entity
    //       public class Invoice {
    //           private Long id;
    //           private String number;
    //       }
    //       """;
    //   Path javaFile = tempDir.resolve("Invoice.java");
    //   Files.writeString(javaFile, javaCode);
    //   TSFile tsFile = new TSFile(SupportedLanguage.JAVA, javaFile);
    //
    //   Optional<String> result = jpaEntityService.getJPAEntityName(tsFile);
    //
    //   assertTrue(result.isPresent());
    //   assertEquals("Invoice", result.get());
    // }
    //
    // @Test
    // @DisplayName("Should return empty for non-entity class")
    // void shouldReturnEmptyForNonEntity() throws IOException {
    //   String javaCode =
    //       """
    //       package com.example;
    //
    //       public class SimpleClass {
    //           private String value;
    //       }
    //       """;
    //   Path javaFile = tempDir.resolve("SimpleClass.java");
    //   Files.writeString(javaFile, javaCode);
    //   TSFile tsFile = new TSFile(SupportedLanguage.JAVA, javaFile);
    //
    //   Optional<String> result = jpaEntityService.getJPAEntityName(tsFile);
    //
    //   assertFalse(result.isPresent());
    // }
    //
    // @Test
    // @DisplayName("Should return class name with wildcard import")
    // void shouldReturnClassNameWithWildcardImport() throws IOException {
    //   String javaCode =
    //       """
    //       package com.example;
    //
    //       import jakarta.persistence.*;
    //
    //       @Entity
    //       public class Account {
    //           private Long id;
    //           private String accountNumber;
    //       }
    //       """;
    //   Path javaFile = tempDir.resolve("Account.java");
    //   Files.writeString(javaFile, javaCode);
    //   TSFile tsFile = new TSFile(SupportedLanguage.JAVA, javaFile);
    //
    //   Optional<String> result = jpaEntityService.getJPAEntityName(tsFile);
    //
    //   assertTrue(result.isPresent());
    //   assertEquals("Account", result.get());
    // }
    //
    // @Test
    // @DisplayName("Should return custom name for @Entity(name = \"customName\")")
    // void shouldReturnCustomNameForEntityWithNameAnnotation() throws IOException {
    //   String javaCode =
    //       """
    //       package com.example;
    //
    //       import jakarta.persistence.Entity;
    //
    //       @Entity(name = "user_table")
    //       public class User {
    //           private Long id;
    //           private String name;
    //       }
    //       """;
    //   Path javaFile = tempDir.resolve("User.java");
    //   Files.writeString(javaFile, javaCode);
    //   TSFile tsFile = new TSFile(SupportedLanguage.JAVA, javaFile);
    //
    //   Optional<String> result = jpaEntityService.getJPAEntityName(tsFile);
    //
    //   assertTrue(result.isPresent());
    //   assertEquals("user_table", result.get());
    // }
    //
    // @Test
    // @DisplayName("Should return custom name for fully qualified @jakarta.persistence.Entity(name
    // = \"customName\")")
    // void shouldReturnCustomNameForFullyQualifiedEntityWithName() throws IOException {
    //   String javaCode =
    //       """
    //       package com.example;
    //
    //       @jakarta.persistence.Entity(name = "product_table")
    //       public class Product {
    //           private Long id;
    //           private String name;
    //       }
    //       """;
    //   Path javaFile = tempDir.resolve("Product.java");
    //   Files.writeString(javaFile, javaCode);
    //   TSFile tsFile = new TSFile(SupportedLanguage.JAVA, javaFile);
    //
    //   Optional<String> result = jpaEntityService.getJPAEntityName(tsFile);
    //
    //   assertTrue(result.isPresent());
    //   assertEquals("product_table", result.get());
    // }
    //
    // @Test
    // @DisplayName("Should return custom name for @Entity(name = \"customName\") with wildcard
    // import")
    // void shouldReturnCustomNameForEntityWithNameAndWildcardImport() throws IOException {
    //   String javaCode =
    //       """
    //       package com.example;
    //
    //       import jakarta.persistence.*;
    //
    //       @Entity(name = "order_table")
    //       public class Order {
    //           private Long id;
    //           private String orderNumber;
    //       }
    //       """;
    //   Path javaFile = tempDir.resolve("Order.java");
    //   Files.writeString(javaFile, javaCode);
    //   TSFile tsFile = new TSFile(SupportedLanguage.JAVA, javaFile);
    //
    //   Optional<String> result = jpaEntityService.getJPAEntityName(tsFile);
    //
    //   assertTrue(result.isPresent());
    //   assertEquals("order_table", result.get());
    // }
  }
}
