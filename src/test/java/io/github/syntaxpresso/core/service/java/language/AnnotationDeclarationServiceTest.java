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

@DisplayName("AnnotationDeclarationService Tests")
class AnnotationDeclarationServiceTest {
  private AnnotationDeclarationService annotationDeclarationService;

  private static final String SIMPLE_ANNOTATION_CODE =
      """
      public @interface SimpleAnnotation {
          String value();
      }
      """;

  private static final String COMPLEX_ANNOTATION_CODE =
      """
      package com.example;

      import java.lang.annotation.ElementType;
      import java.lang.annotation.Retention;
      import java.lang.annotation.RetentionPolicy;
      import java.lang.annotation.Target;

      @Target({ElementType.TYPE, ElementType.METHOD})
      @Retention(RetentionPolicy.RUNTIME)
      public @interface ValidatedService {
          String name() default "";
          Class<?>[] groups() default {};
          boolean required() default true;
          int priority() default 0;
      }
      """;

  private static final String MULTIPLE_ANNOTATIONS_CODE =
      """
      package com.example;

      @interface InternalAnnotation {
          String value();
      }

      public @interface PublicAnnotation {
          int number();
      }

      @interface AnotherAnnotation {
          boolean flag() default false;
      }
      """;

  private static final String NO_PUBLIC_ANNOTATION_CODE =
      """
      package com.example;

      @interface InternalAnnotation {
          String value1();
      }

      @interface AnotherInternalAnnotation {
          String value2();
      }
      """;

  @BeforeEach
  void setUp() {
    this.annotationDeclarationService = new AnnotationDeclarationService();
  }

  @Nested
  @DisplayName("findAnnotationByName Tests")
  class FindAnnotationByNameTests {

    @Test
    @DisplayName("Should find annotation by exact name")
    void shouldFindAnnotationByExactName() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_ANNOTATION_CODE);
      Optional<TSNode> result =
          annotationDeclarationService.findAnnotationByName(tsFile, "SimpleAnnotation");

      assertTrue(result.isPresent());
      assertEquals("annotation_type_declaration", result.get().getType());
    }

    @Test
    @DisplayName("Should find complex annotation by name")
    void shouldFindComplexAnnotationByName() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, COMPLEX_ANNOTATION_CODE);
      Optional<TSNode> result =
          annotationDeclarationService.findAnnotationByName(tsFile, "ValidatedService");

      assertTrue(result.isPresent());
      assertEquals("annotation_type_declaration", result.get().getType());
    }

    @Test
    @DisplayName("Should find specific annotation from multiple annotations")
    void shouldFindSpecificAnnotationFromMultiple() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, MULTIPLE_ANNOTATIONS_CODE);

      Optional<TSNode> internalResult =
          annotationDeclarationService.findAnnotationByName(tsFile, "InternalAnnotation");
      Optional<TSNode> publicResult =
          annotationDeclarationService.findAnnotationByName(tsFile, "PublicAnnotation");
      Optional<TSNode> anotherResult =
          annotationDeclarationService.findAnnotationByName(tsFile, "AnotherAnnotation");

      assertTrue(internalResult.isPresent());
      assertTrue(publicResult.isPresent());
      assertTrue(anotherResult.isPresent());
      assertEquals("annotation_type_declaration", internalResult.get().getType());
      assertEquals("annotation_type_declaration", publicResult.get().getType());
      assertEquals("annotation_type_declaration", anotherResult.get().getType());
    }

    @Test
    @DisplayName("Should return empty when annotation not found")
    void shouldReturnEmptyWhenAnnotationNotFound() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_ANNOTATION_CODE);
      Optional<TSNode> result =
          annotationDeclarationService.findAnnotationByName(tsFile, "NonExistentAnnotation");

      assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should return empty when TSFile is null")
    void shouldReturnEmptyWhenTSFileIsNull() {
      Optional<TSNode> result =
          annotationDeclarationService.findAnnotationByName(null, "SimpleAnnotation");

      assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should return empty when annotation name is null")
    void shouldReturnEmptyWhenAnnotationNameIsNull() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_ANNOTATION_CODE);
      Optional<TSNode> result = annotationDeclarationService.findAnnotationByName(tsFile, null);

      assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should return empty when annotation name is empty")
    void shouldReturnEmptyWhenAnnotationNameIsEmpty() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_ANNOTATION_CODE);
      Optional<TSNode> result = annotationDeclarationService.findAnnotationByName(tsFile, "");

      assertFalse(result.isPresent());
    }
  }

  @Nested
  @DisplayName("getPublicAnnotation Tests")
  class GetPublicAnnotationTests {

    @Test
    @DisplayName("Should get public annotation matching filename")
    void shouldGetPublicAnnotationMatchingFilename(@TempDir Path tempDir) throws IOException {
      Path annotationFile = tempDir.resolve("SimpleAnnotation.java");
      Files.writeString(annotationFile, SIMPLE_ANNOTATION_CODE);
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, annotationFile);

      Optional<TSNode> result = annotationDeclarationService.getPublicAnnotation(tsFile);

      assertTrue(result.isPresent());
      assertEquals("annotation_type_declaration", result.get().getType());
    }

    @Test
    @DisplayName("Should get public annotation matching filename for complex annotation")
    void shouldGetPublicAnnotationMatchingFilenameForComplex(@TempDir Path tempDir)
        throws IOException {
      Path annotationFile = tempDir.resolve("ValidatedService.java");
      Files.writeString(annotationFile, COMPLEX_ANNOTATION_CODE);
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, annotationFile);

      Optional<TSNode> result = annotationDeclarationService.getPublicAnnotation(tsFile);

      assertTrue(result.isPresent());
      assertEquals("annotation_type_declaration", result.get().getType());
    }

    @Test
    @DisplayName("Should return empty when filename doesn't match any annotation")
    void shouldReturnEmptyWhenFilenameDoesntMatchAnyAnnotation(@TempDir Path tempDir)
        throws IOException {
      Path annotationFile = tempDir.resolve("WrongName.java");
      Files.writeString(annotationFile, MULTIPLE_ANNOTATIONS_CODE);
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, annotationFile);

      Optional<TSNode> result = annotationDeclarationService.getPublicAnnotation(tsFile);

      assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should return empty when no public annotation exists")
    void shouldReturnEmptyWhenNoPublicAnnotationExists(@TempDir Path tempDir) throws IOException {
      Path annotationFile = tempDir.resolve("NoPublic.java");
      Files.writeString(annotationFile, NO_PUBLIC_ANNOTATION_CODE);
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, annotationFile);

      Optional<TSNode> result = annotationDeclarationService.getPublicAnnotation(tsFile);

      assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should fallback to first public annotation when no filename available")
    void shouldFallbackToFirstPublicAnnotationWhenNoFilenameAvailable() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, MULTIPLE_ANNOTATIONS_CODE);

      Optional<TSNode> result = annotationDeclarationService.getPublicAnnotation(tsFile);

      assertTrue(result.isPresent());
      assertEquals("annotation_type_declaration", result.get().getType());
    }

    @Test
    @DisplayName("Should return empty when TSFile is null")
    void shouldReturnEmptyWhenTSFileIsNull() {
      Optional<TSNode> result = annotationDeclarationService.getPublicAnnotation(null);

      assertFalse(result.isPresent());
    }
  }

  @Nested
  @DisplayName("getAnnotationNameNode Tests")
  class GetAnnotationNameNodeTests {

    @Test
    @DisplayName("Should get annotation name node from simple annotation")
    void shouldGetAnnotationNameNodeFromSimpleAnnotation() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_ANNOTATION_CODE);
      Optional<TSNode> annotationNode =
          annotationDeclarationService.findAnnotationByName(tsFile, "SimpleAnnotation");

      assertTrue(annotationNode.isPresent());
      Optional<TSNode> nameNode =
          annotationDeclarationService.getAnnotationNameNode(tsFile, annotationNode.get());

      assertTrue(nameNode.isPresent());
      assertEquals("identifier", nameNode.get().getType());
      assertEquals("SimpleAnnotation", tsFile.getTextFromNode(nameNode.get()));
    }

    @Test
    @DisplayName("Should get annotation name node from complex annotation")
    void shouldGetAnnotationNameNodeFromComplexAnnotation() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, COMPLEX_ANNOTATION_CODE);
      Optional<TSNode> annotationNode =
          annotationDeclarationService.findAnnotationByName(tsFile, "ValidatedService");

      assertTrue(annotationNode.isPresent());
      Optional<TSNode> nameNode =
          annotationDeclarationService.getAnnotationNameNode(tsFile, annotationNode.get());

      assertTrue(nameNode.isPresent());
      assertEquals("identifier", nameNode.get().getType());
      assertEquals("ValidatedService", tsFile.getTextFromNode(nameNode.get()));
    }

    @Test
    @DisplayName("Should get annotation name node from multiple annotations")
    void shouldGetAnnotationNameNodeFromMultipleAnnotations() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, MULTIPLE_ANNOTATIONS_CODE);
      Optional<TSNode> publicAnnotationNode =
          annotationDeclarationService.findAnnotationByName(tsFile, "PublicAnnotation");

      assertTrue(publicAnnotationNode.isPresent());
      Optional<TSNode> nameNode =
          annotationDeclarationService.getAnnotationNameNode(tsFile, publicAnnotationNode.get());

      assertTrue(nameNode.isPresent());
      assertEquals("identifier", nameNode.get().getType());
      assertEquals("PublicAnnotation", tsFile.getTextFromNode(nameNode.get()));
    }

    @Test
    @DisplayName("Should return empty when TSFile is null")
    void shouldReturnEmptyWhenTSFileIsNull() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_ANNOTATION_CODE);
      Optional<TSNode> annotationNode =
          annotationDeclarationService.findAnnotationByName(tsFile, "SimpleAnnotation");

      assertTrue(annotationNode.isPresent());
      Optional<TSNode> nameNode =
          annotationDeclarationService.getAnnotationNameNode(null, annotationNode.get());

      assertFalse(nameNode.isPresent());
    }

    @Test
    @DisplayName("Should return empty when node is not annotation_type_declaration")
    void shouldReturnEmptyWhenNodeIsNotAnnotationTypeDeclaration() {
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, SIMPLE_ANNOTATION_CODE);
      TSNode rootNode = tsFile.getTree().getRootNode();

      Optional<TSNode> nameNode =
          annotationDeclarationService.getAnnotationNameNode(tsFile, rootNode);

      assertFalse(nameNode.isPresent());
    }
  }

  @Nested
  @DisplayName("Edge Cases and Error Handling")
  class EdgeCasesAndErrorHandlingTests {

    @Test
    @DisplayName("Should handle annotation with default values")
    void shouldHandleAnnotationWithDefaultValues() {
      String annotationWithDefaultsCode =
          """
          public @interface Config {
              String name() default "default";
              int timeout() default 30;
              boolean enabled() default true;
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, annotationWithDefaultsCode);
      Optional<TSNode> result = annotationDeclarationService.findAnnotationByName(tsFile, "Config");

      assertTrue(result.isPresent());
      assertEquals("annotation_type_declaration", result.get().getType());
    }

    @Test
    @DisplayName("Should handle annotation with array parameters")
    void shouldHandleAnnotationWithArrayParameters() {
      String annotationWithArraysCode =
          """
          public @interface Roles {
              String[] value();
              Class<?>[] types() default {};
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, annotationWithArraysCode);
      Optional<TSNode> result = annotationDeclarationService.findAnnotationByName(tsFile, "Roles");

      assertTrue(result.isPresent());
      assertEquals("annotation_type_declaration", result.get().getType());
    }

    @Test
    @DisplayName("Should handle nested annotations")
    void shouldHandleNestedAnnotations() {
      String nestedAnnotationCode =
          """
          public class OuterClass {
              public @interface InnerAnnotation {
                  String value();
              }
              
              private @interface PrivateAnnotation {
                  int number();
              }
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, nestedAnnotationCode);
      Optional<TSNode> result =
          annotationDeclarationService.findAnnotationByName(tsFile, "InnerAnnotation");

      assertTrue(result.isPresent());
      assertEquals("annotation_type_declaration", result.get().getType());
    }

    @Test
    @DisplayName("Should handle marker annotation")
    void shouldHandleMarkerAnnotation() {
      String markerAnnotationCode =
          """
          public @interface MarkerAnnotation {
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, markerAnnotationCode);
      Optional<TSNode> result =
          annotationDeclarationService.findAnnotationByName(tsFile, "MarkerAnnotation");

      assertTrue(result.isPresent());
      assertEquals("annotation_type_declaration", result.get().getType());
    }

    @Test
    @DisplayName("Should handle annotation with meta-annotations")
    void shouldHandleAnnotationWithMetaAnnotations() {
      String metaAnnotatedCode =
          """
          @Target(ElementType.METHOD)
          @Retention(RetentionPolicy.RUNTIME)
          @Documented
          @Inherited
          public @interface CustomValidator {
              String message() default "Validation failed";
              Class<?>[] groups() default {};
          }
          """;
      TSFile tsFile = new TSFile(SupportedLanguage.JAVA, metaAnnotatedCode);
      Optional<TSNode> result =
          annotationDeclarationService.findAnnotationByName(tsFile, "CustomValidator");

      assertTrue(result.isPresent());
      assertEquals("annotation_type_declaration", result.get().getType());
    }
  }
}