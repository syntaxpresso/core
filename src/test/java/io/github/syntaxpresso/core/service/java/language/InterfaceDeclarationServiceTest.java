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

@DisplayName("InterfaceDeclarationService Tests")
class InterfaceDeclarationServiceTest {
  private InterfaceDeclarationService interfaceDeclarationService;

  private static final String SIMPLE_INTERFACE_CODE =
      """
      public interface SimpleInterface {
          void method();
      }
      """;

  private static final String COMPLEX_INTERFACE_CODE =
      """
      package com.example;

      import java.util.List;
      import java.io.Serializable;

      @Repository
      public interface UserRepository extends JpaRepository<User, Long> {
          List<User> findByName(String name);
          Optional<User> findByEmail(String email);
      }
      """;

  private static final String MULTIPLE_INTERFACES_CODE =
      """
      package com.example;

      interface InternalInterface {
          void internalMethod();
      }

      public interface PublicInterface {
          void publicMethod();
      }

      interface AnotherInterface extends PublicInterface {
          void anotherMethod();
      }
      """;

  private static final String NO_PUBLIC_INTERFACE_CODE =
      """
      package com.example;

      interface InternalInterface {
          void method();
      }

      interface AnotherInternalInterface {
          void anotherMethod();
      }
      """;

  @BeforeEach
  void setUp() {
    this.interfaceDeclarationService = new InterfaceDeclarationService();
  }

  @Nested
  @DisplayName("findInterfaceByName Tests")
  class FindInterfaceByNameTests {

    @Test
    @DisplayName("Should find interface by exact name")
    void shouldFindInterfaceByExactName() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_INTERFACE_CODE);
      Optional<TSNode> result =
          interfaceDeclarationService.findInterfaceByName(tsFile, "SimpleInterface");

      assertTrue(result.isPresent());
      assertEquals("interface_declaration", result.get().getType());
    }

    @Test
    @DisplayName("Should find complex interface by name")
    void shouldFindComplexInterfaceByName() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, COMPLEX_INTERFACE_CODE);
      Optional<TSNode> result =
          interfaceDeclarationService.findInterfaceByName(tsFile, "UserRepository");

      assertTrue(result.isPresent());
      assertEquals("interface_declaration", result.get().getType());
    }

    @Test
    @DisplayName("Should find specific interface from multiple interfaces")
    void shouldFindSpecificInterfaceFromMultiple() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, MULTIPLE_INTERFACES_CODE);

      Optional<TSNode> internalResult =
          interfaceDeclarationService.findInterfaceByName(tsFile, "InternalInterface");
      Optional<TSNode> publicResult =
          interfaceDeclarationService.findInterfaceByName(tsFile, "PublicInterface");
      Optional<TSNode> anotherResult =
          interfaceDeclarationService.findInterfaceByName(tsFile, "AnotherInterface");

      assertTrue(internalResult.isPresent());
      assertTrue(publicResult.isPresent());
      assertTrue(anotherResult.isPresent());
      assertEquals("interface_declaration", internalResult.get().getType());
      assertEquals("interface_declaration", publicResult.get().getType());
      assertEquals("interface_declaration", anotherResult.get().getType());
    }

    @Test
    @DisplayName("Should return empty when interface not found")
    void shouldReturnEmptyWhenInterfaceNotFound() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_INTERFACE_CODE);
      Optional<TSNode> result =
          interfaceDeclarationService.findInterfaceByName(tsFile, "NonExistentInterface");

      assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should return empty when TSFile is null")
    void shouldReturnEmptyWhenTSFileIsNull() {
      Optional<TSNode> result =
          interfaceDeclarationService.findInterfaceByName(null, "SimpleInterface");

      assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should return empty when interface name is null")
    void shouldReturnEmptyWhenInterfaceNameIsNull() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_INTERFACE_CODE);
      Optional<TSNode> result = interfaceDeclarationService.findInterfaceByName(tsFile, null);

      assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should return empty when interface name is empty")
    void shouldReturnEmptyWhenInterfaceNameIsEmpty() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_INTERFACE_CODE);
      Optional<TSNode> result = interfaceDeclarationService.findInterfaceByName(tsFile, "");

      assertFalse(result.isPresent());
    }
  }

  @Nested
  @DisplayName("getPublicInterface Tests")
  class GetPublicInterfaceTests {

    @Test
    @DisplayName("Should get public interface matching filename")
    void shouldGetPublicInterfaceMatchingFilename(@TempDir Path tempDir) throws IOException {
      Path interfaceFile = tempDir.resolve("SimpleInterface.java");
      Files.writeString(interfaceFile, SIMPLE_INTERFACE_CODE);
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, interfaceFile);

      Optional<TSNode> result = interfaceDeclarationService.getPublicInterface(tsFile);

      assertTrue(result.isPresent());
      assertEquals("interface_declaration", result.get().getType());
    }

    @Test
    @DisplayName("Should get public interface matching filename for complex interface")
    void shouldGetPublicInterfaceMatchingFilenameForComplex(@TempDir Path tempDir)
        throws IOException {
      Path interfaceFile = tempDir.resolve("UserRepository.java");
      Files.writeString(interfaceFile, COMPLEX_INTERFACE_CODE);
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, interfaceFile);

      Optional<TSNode> result = interfaceDeclarationService.getPublicInterface(tsFile);

      assertTrue(result.isPresent());
      assertEquals("interface_declaration", result.get().getType());
    }

    @Test
    @DisplayName("Should return empty when filename doesn't match any interface")
    void shouldReturnEmptyWhenFilenameDoesntMatchAnyInterface(@TempDir Path tempDir)
        throws IOException {
      Path interfaceFile = tempDir.resolve("WrongName.java");
      Files.writeString(interfaceFile, MULTIPLE_INTERFACES_CODE);
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, interfaceFile);

      Optional<TSNode> result = interfaceDeclarationService.getPublicInterface(tsFile);

      // Since filename is "WrongName" and no interface has that name, should return empty
      // getPublicInterface only falls back to getFirstPublicInterface when NO filename is available
      assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should return empty when no public interface exists")
    void shouldReturnEmptyWhenNoPublicInterfaceExists(@TempDir Path tempDir) throws IOException {
      Path interfaceFile = tempDir.resolve("NoPublic.java");
      Files.writeString(interfaceFile, NO_PUBLIC_INTERFACE_CODE);
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, interfaceFile);

      Optional<TSNode> result = interfaceDeclarationService.getPublicInterface(tsFile);

      assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should fallback to first public interface when no filename available")
    void shouldFallbackToFirstPublicInterfaceWhenNoFilenameAvailable() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, MULTIPLE_INTERFACES_CODE);

      Optional<TSNode> result = interfaceDeclarationService.getPublicInterface(tsFile);

      assertTrue(result.isPresent());
      assertEquals("interface_declaration", result.get().getType());
    }

    @Test
    @DisplayName("Should return empty when TSFile is null")
    void shouldReturnEmptyWhenTSFileIsNull() {
      Optional<TSNode> result = interfaceDeclarationService.getPublicInterface(null);

      assertFalse(result.isPresent());
    }
  }

  @Nested
  @DisplayName("getInterfaceNameNode Tests")
  class GetInterfaceNameNodeTests {

    @Test
    @DisplayName("Should get interface name node from simple interface")
    void shouldGetInterfaceNameNodeFromSimpleInterface() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_INTERFACE_CODE);
      Optional<TSNode> interfaceNode =
          interfaceDeclarationService.findInterfaceByName(tsFile, "SimpleInterface");

      assertTrue(interfaceNode.isPresent());
      Optional<TSNode> nameNode =
          interfaceDeclarationService.getInterfaceNameNode(tsFile, interfaceNode.get());

      assertTrue(nameNode.isPresent());
      assertEquals("identifier", nameNode.get().getType());
      assertEquals("SimpleInterface", tsFile.getTextFromNode(nameNode.get()));
    }

    @Test
    @DisplayName("Should get interface name node from complex interface")
    void shouldGetInterfaceNameNodeFromComplexInterface() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, COMPLEX_INTERFACE_CODE);
      Optional<TSNode> interfaceNode =
          interfaceDeclarationService.findInterfaceByName(tsFile, "UserRepository");

      assertTrue(interfaceNode.isPresent());
      Optional<TSNode> nameNode =
          interfaceDeclarationService.getInterfaceNameNode(tsFile, interfaceNode.get());

      assertTrue(nameNode.isPresent());
      assertEquals("identifier", nameNode.get().getType());
      assertEquals("UserRepository", tsFile.getTextFromNode(nameNode.get()));
    }

    @Test
    @DisplayName("Should get interface name node from multiple interfaces")
    void shouldGetInterfaceNameNodeFromMultipleInterfaces() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, MULTIPLE_INTERFACES_CODE);
      Optional<TSNode> publicInterfaceNode =
          interfaceDeclarationService.findInterfaceByName(tsFile, "PublicInterface");

      assertTrue(publicInterfaceNode.isPresent());
      Optional<TSNode> nameNode =
          interfaceDeclarationService.getInterfaceNameNode(tsFile, publicInterfaceNode.get());

      assertTrue(nameNode.isPresent());
      assertEquals("identifier", nameNode.get().getType());
      assertEquals("PublicInterface", tsFile.getTextFromNode(nameNode.get()));
    }

    @Test
    @DisplayName("Should return empty when TSFile is null")
    void shouldReturnEmptyWhenTSFileIsNull() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_INTERFACE_CODE);
      Optional<TSNode> interfaceNode =
          interfaceDeclarationService.findInterfaceByName(tsFile, "SimpleInterface");

      assertTrue(interfaceNode.isPresent());
      Optional<TSNode> nameNode =
          interfaceDeclarationService.getInterfaceNameNode(null, interfaceNode.get());

      assertFalse(nameNode.isPresent());
    }

    @Test
    @DisplayName("Should return empty when node is not interface_declaration")
    void shouldReturnEmptyWhenNodeIsNotInterfaceDeclaration() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_INTERFACE_CODE);
      TSNode rootNode = tsFile.getTree().getRootNode();

      Optional<TSNode> nameNode =
          interfaceDeclarationService.getInterfaceNameNode(tsFile, rootNode);

      assertFalse(nameNode.isPresent());
    }
  }

  @Nested
  @DisplayName("Edge Cases and Error Handling")
  class EdgeCasesAndErrorHandlingTests {

    @Test
    @DisplayName("Should handle interface with generic parameters")
    void shouldHandleInterfaceWithGenericParameters() {
      String genericInterfaceCode =
          """
          public interface GenericInterface<T, U extends Serializable> {
              T process(U input);
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, genericInterfaceCode);
      Optional<TSNode> result =
          interfaceDeclarationService.findInterfaceByName(tsFile, "GenericInterface");

      assertTrue(result.isPresent());
      assertEquals("interface_declaration", result.get().getType());
    }

    @Test
    @DisplayName("Should handle interface with multiple inheritance")
    void shouldHandleInterfaceWithMultipleInheritance() {
      String multipleInheritanceCode =
          """
          public interface MultipleInterface extends Interface1, Interface2, Interface3 {
              void method();
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, multipleInheritanceCode);
      Optional<TSNode> result =
          interfaceDeclarationService.findInterfaceByName(tsFile, "MultipleInterface");

      assertTrue(result.isPresent());
      assertEquals("interface_declaration", result.get().getType());
    }

    @Test
    @DisplayName("Should handle interface with annotations")
    void shouldHandleInterfaceWithAnnotations() {
      String annotatedInterfaceCode =
          """
          @Repository
          @Component
          @CustomAnnotation(value = "test")
          public interface AnnotatedInterface {
              void method();
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, annotatedInterfaceCode);
      Optional<TSNode> result =
          interfaceDeclarationService.findInterfaceByName(tsFile, "AnnotatedInterface");

      assertTrue(result.isPresent());
      assertEquals("interface_declaration", result.get().getType());
    }

    @Test
    @DisplayName("Should handle nested interfaces")
    void shouldHandleNestedInterfaces() {
      String nestedInterfaceCode =
          """
          public class OuterClass {
              public interface InnerInterface {
                  void method();
              }

              private interface PrivateInterface {
                  void privateMethod();
              }
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, nestedInterfaceCode);
      Optional<TSNode> result =
          interfaceDeclarationService.findInterfaceByName(tsFile, "InnerInterface");

      assertTrue(result.isPresent());
      assertEquals("interface_declaration", result.get().getType());
    }

    @Test
    @DisplayName("Should handle empty interface")
    void shouldHandleEmptyInterface() {
      String emptyInterfaceCode =
          """
          public interface EmptyInterface {
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, emptyInterfaceCode);
      Optional<TSNode> result =
          interfaceDeclarationService.findInterfaceByName(tsFile, "EmptyInterface");

      assertTrue(result.isPresent());
      assertEquals("interface_declaration", result.get().getType());
    }

    @Test
    @DisplayName("Should return empty for malformed interface")
    void shouldReturnEmptyForMalformedInterface() {
      String malformedCode =
          """
          public interface MalformedInterface
          void method();
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, malformedCode);
      Optional<TSNode> result =
          interfaceDeclarationService.findInterfaceByName(tsFile, "MalformedInterface");

      // Tree-sitter cannot parse this malformed interface correctly
      assertFalse(result.isPresent());
    }
  }
}

