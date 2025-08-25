package io.github.syntaxpresso.core.service.java.jpa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.service.java.language.ImportDeclarationService;
import io.github.syntaxpresso.core.service.java.language.PackageDeclarationService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.treesitter.TSNode;

@ExtendWith(MockitoExtension.class)
@DisplayName("JPAService Tests")
class JPAServiceTest {
  @Mock private ImportDeclarationService importDeclarationService;
  @Mock private PackageDeclarationService packageDeclarationService;
  @Mock private TSFile tsFile;
  @Mock private TSNode classDeclarationNode;
  @Mock private TSNode fieldNode;
  @Mock private TSNode annotationNode;
  @Mock private TSNode typeNode;
  @Mock private TSNode interfaceNode;
  @Mock private TSNode nameNode;
  private JPAService jpaService;

  @BeforeEach
  void setUp() {
    jpaService = new JPAService(importDeclarationService);
    lenient()
        .when(importDeclarationService.getPackageDeclarationService())
        .thenReturn(packageDeclarationService);
  }

  @Nested
  @DisplayName("JPA Entity Detection Tests")
  class JPAEntityDetectionTests {
    @Test
    @DisplayName("should return true when class has @Entity annotation")
    void isJPAEntity_withEntityAnnotation_shouldReturnTrue() {
      lenient()
          .when(tsFile.query(any(TSNode.class), anyString()))
          .thenReturn(List.of(annotationNode));
      lenient().when(tsFile.getTextFromNode(annotationNode)).thenReturn("Entity");
      boolean result = jpaService.isJPAEntity(tsFile, classDeclarationNode);
      assertTrue(result);
    }

    @Test
    @DisplayName("should return false when class has no @Entity annotation")
    void isJPAEntity_withoutEntityAnnotation_shouldReturnFalse() {
      lenient()
          .when(tsFile.query(any(TSNode.class), anyString()))
          .thenReturn(List.of(annotationNode));
      lenient().when(tsFile.getTextFromNode(annotationNode)).thenReturn("Component");
      boolean result = jpaService.isJPAEntity(tsFile, classDeclarationNode);
      assertFalse(result);
    }

    @Test
    @DisplayName("should return false when class has no annotations")
    void isJPAEntity_withNoAnnotations_shouldReturnFalse() {
      lenient().when(tsFile.query(any(TSNode.class), anyString())).thenReturn(List.of());
      boolean result = jpaService.isJPAEntity(tsFile, classDeclarationNode);
      assertFalse(result);
    }

    @Test
    @DisplayName("should return true when class has multiple annotations including @Entity")
    void isJPAEntity_withMultipleAnnotationsIncludingEntity_shouldReturnTrue() {
      TSNode componentAnnotation = mock(TSNode.class);
      TSNode entityAnnotation = mock(TSNode.class);
      lenient()
          .when(tsFile.query(any(TSNode.class), anyString()))
          .thenReturn(List.of(componentAnnotation, entityAnnotation));
      lenient().when(tsFile.getTextFromNode(componentAnnotation)).thenReturn("Component");
      lenient().when(tsFile.getTextFromNode(entityAnnotation)).thenReturn("Entity");
      boolean result = jpaService.isJPAEntity(tsFile, classDeclarationNode);
      assertTrue(result);
    }
  }

  @Nested
  @DisplayName("Field Type Extraction Tests")
  class FieldTypeExtractionTests {
    @Test
    @DisplayName("should return empty when node is not field_declaration")
    void extractFieldType_withNonFieldDeclaration_shouldReturnEmpty() {
      lenient().when(fieldNode.getType()).thenReturn("method_declaration");
      Optional<String> result = jpaService.extractFieldType(tsFile, fieldNode);
      assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("should extract field type when type is found")
    void extractFieldType_withValidTypeNode_shouldReturnType() {
      TSNode typeChildNode = mock(TSNode.class);
      TSNode otherChildNode1 = mock(TSNode.class);
      TSNode otherChildNode2 = mock(TSNode.class);
      lenient().when(fieldNode.getType()).thenReturn("field_declaration");
      lenient().when(fieldNode.getChildCount()).thenReturn(3);
      lenient().when(fieldNode.getChild(0)).thenReturn(otherChildNode1);
      lenient().when(fieldNode.getChild(1)).thenReturn(typeChildNode);
      lenient().when(fieldNode.getChild(2)).thenReturn(otherChildNode2);
      lenient().when(otherChildNode1.getType()).thenReturn("modifiers");
      lenient().when(typeChildNode.getType()).thenReturn("type_identifier");
      lenient().when(otherChildNode2.getType()).thenReturn("variable_declarator");
      lenient().when(tsFile.getTextFromNode(typeChildNode)).thenReturn("String");
      Optional<String> result = jpaService.extractFieldType(tsFile, fieldNode);
      assertTrue(result.isPresent());
      assertEquals("String", result.get());
    }

    @Test
    @DisplayName("should return empty when no valid type found")
    void extractFieldType_withNoValidType_shouldReturnEmpty() {
      TSNode childNode1 = mock(TSNode.class);
      TSNode childNode2 = mock(TSNode.class);
      lenient().when(fieldNode.getType()).thenReturn("field_declaration");
      lenient().when(fieldNode.getChildCount()).thenReturn(2);
      lenient().when(fieldNode.getChild(0)).thenReturn(childNode1);
      lenient().when(fieldNode.getChild(1)).thenReturn(childNode2);
      lenient().when(childNode1.getType()).thenReturn("modifiers");
      lenient().when(childNode2.getType()).thenReturn("identifier");
      Optional<String> result = jpaService.extractFieldType(tsFile, fieldNode);
      assertTrue(result.isEmpty());
    }
  }

  @Nested
  @DisplayName("Repository Configuration Tests")
  class RepositoryConfigurationTests {
    @Test
    @DisplayName("should configure repository with JPA imports and extends clause")
    void configureRepositoryFile_shouldAddImportsAndExtendsClause() {
      TSFile repositoryFile = mock(TSFile.class);
      when(packageDeclarationService.getPackageName(repositoryFile))
          .thenReturn(Optional.of("com.example.repository"));
      when(repositoryFile.query(anyString())).thenReturn(List.of(interfaceNode));
      when(interfaceNode.getChildByFieldName("name")).thenReturn(nameNode);
      jpaService.configureRepositoryFile(repositoryFile, "User", "Long", "com.example.entity");
      verify(importDeclarationService)
          .addImport(repositoryFile, "org.springframework.data.jpa.repository.JpaRepository");
      verify(importDeclarationService).addImport(repositoryFile, "com.example.entity.User");
      verify(repositoryFile).insertTextAfterNode(nameNode, " extends JpaRepository<User, Long>");
    }

    @Test
    @DisplayName("should not add entity import when same package")
    void configureRepositoryFile_withSamePackage_shouldNotAddEntityImport() {
      TSFile repositoryFile = mock(TSFile.class);
      when(packageDeclarationService.getPackageName(repositoryFile))
          .thenReturn(Optional.of("com.example.entity"));
      when(repositoryFile.query(anyString())).thenReturn(List.of(interfaceNode));
      when(interfaceNode.getChildByFieldName("name")).thenReturn(nameNode);
      jpaService.configureRepositoryFile(repositoryFile, "User", "Long", "com.example.entity");
      verify(importDeclarationService)
          .addImport(repositoryFile, "org.springframework.data.jpa.repository.JpaRepository");
      verify(importDeclarationService, never())
          .addImport(repositoryFile, "com.example.entity.User");
      verify(repositoryFile).insertTextAfterNode(nameNode, " extends JpaRepository<User, Long>");
    }

    @Test
    @DisplayName("should add import for BigDecimal ID type")
    void configureRepositoryFile_withBigDecimalIdType_shouldAddImport() {
      TSFile repositoryFile = mock(TSFile.class);
      when(packageDeclarationService.getPackageName(repositoryFile))
          .thenReturn(Optional.of("com.example.repository"));
      when(repositoryFile.query(anyString())).thenReturn(List.of(interfaceNode));
      when(interfaceNode.getChildByFieldName("name")).thenReturn(nameNode);
      jpaService.configureRepositoryFile(
          repositoryFile, "User", "BigDecimal", "com.example.entity");
      verify(importDeclarationService).addImport(repositoryFile, "java.math.BigDecimal");
    }

    @Test
    @DisplayName("should not add import for primitive ID type")
    void configureRepositoryFile_withPrimitiveIdType_shouldNotAddImport() {
      TSFile repositoryFile = mock(TSFile.class);
      when(packageDeclarationService.getPackageName(repositoryFile))
          .thenReturn(Optional.of("com.example.repository"));
      when(repositoryFile.query(anyString())).thenReturn(List.of(interfaceNode));
      when(interfaceNode.getChildByFieldName("name")).thenReturn(nameNode);
      jpaService.configureRepositoryFile(repositoryFile, "User", "long", "com.example.entity");
      verify(importDeclarationService, never()).addImport(eq(repositoryFile), eq("java.lang.Long"));
    }

    @Test
    @DisplayName("should not add extends clause when no interface found")
    void configureRepositoryFile_withNoInterface_shouldNotAddExtendsClause() {
      TSFile repositoryFile = mock(TSFile.class);
      when(packageDeclarationService.getPackageName(repositoryFile))
          .thenReturn(Optional.of("com.example.repository"));
      when(repositoryFile.query(anyString())).thenReturn(List.of());
      jpaService.configureRepositoryFile(repositoryFile, "User", "Long", "com.example.entity");
      verify(repositoryFile, never()).insertTextAfterNode(any(), anyString());
    }

    @Test
    @DisplayName("should not add extends clause when interface has no name")
    void configureRepositoryFile_withInterfaceNoName_shouldNotAddExtendsClause() {
      TSFile repositoryFile = mock(TSFile.class);
      when(packageDeclarationService.getPackageName(repositoryFile))
          .thenReturn(Optional.of("com.example.repository"));
      when(repositoryFile.query(anyString())).thenReturn(List.of(interfaceNode));
      when(interfaceNode.getChildByFieldName("name")).thenReturn(null);
      jpaService.configureRepositoryFile(repositoryFile, "User", "Long", "com.example.entity");
      verify(repositoryFile, never()).insertTextAfterNode(any(), anyString());
    }
  }
}
