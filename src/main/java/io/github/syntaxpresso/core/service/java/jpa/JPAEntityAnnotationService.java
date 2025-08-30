package io.github.syntaxpresso.core.service.java.jpa;

import com.google.common.base.Strings;
import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import io.github.syntaxpresso.core.service.java.JavaLanguageService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.treesitter.TSNode;

@RequiredArgsConstructor
public class JPAEntityAnnotationService {
  private final JavaLanguageService javaLanguageService;

  /**
   * Finds the JPA Entity annotation node in the class.
   *
   * @param file The TSFile containing the class to search.
   * @return Optional containing the Entity annotation node if found, empty otherwise.
   */
  public Optional<TSNode> getJPAEntityAnnotationNode(TSFile file) {
    List<Map<String, TSNode>> importNodes =
        this.javaLanguageService.getImportDeclarationService().getAllImportDeclarations(file);
    for (Map<String, TSNode> map : importNodes) {
      boolean isWildCardImport = map.containsKey("isWildCard");
      if (isWildCardImport) {
        TSNode scopeNode = map.get("package");
        String scopeText = file.getTextFromNode(scopeNode);
        System.out.printf("Wildcard Import -> Package: %s%n", scopeText);
      } else {
        // Otherwise, it's a single import with scope and name
        TSNode scopeNode = map.get("package");
        TSNode nameNode = map.get("class");
        String scopeText = file.getTextFromNode(scopeNode);
        String nameText = file.getTextFromNode(nameNode);
        System.out.printf("Single Import   -> Package: %s, Class: %s%n", scopeText, nameText);
      }
    }
    // for (TSNode importNode : importNodes) {
    //   String importName = file.getTextFromNode(importNode);
    //   if (importName.equals("jakarta.persistence.Entity")
    //       || importName.equals("jakarta.persistence")) {
    //     isJPAEntityImported = true;
    //   }
    // }
    // if (!isJPAEntityImported) {
    //   return Optional.empty();
    // }
    // List<TSNode> mainClassAnnotationNodes =
    //     this.javaLanguageService.getClassDeclarationService().getAllClassAnnotations(file);
    // for (TSNode annotation : mainClassAnnotationNodes) {
    //   String annotationName = file.getTextFromNode(annotation);
    //   if ("jakarta.persistence.Entity".equals(annotationName)) {
    //     return Optional.of(annotation);
    //   }
    //   if ("Entity".equals(annotationName)) {
    //     return Optional.of(annotation);
    //   }
    // }
    return Optional.empty();
  }

  /**
   * Checks if a class is a JPA entity.
   *
   * @param file The TSFile containing the class.
   * @return True if the class is a JPA entity, false otherwise.
   */
  public boolean isJPAEntity(TSFile file) {
    Optional<TSNode> entityAnnotationNode = this.getJPAEntityAnnotationNode(file);
    return entityAnnotationNode.isPresent();
  }

  /**
   * Gets the name of the JPA entity from the Entity annotation or class name.
   *
   * @param file The TSFile containing the JPA entity class.
   * @return Optional containing the entity name if the class is a JPA entity, empty otherwise.
   */
  public Optional<String> getJPAEntityName(TSFile file, TSNode annotationNode) {
    Optional<TSNode> mainClassDeclarationNode =
        this.javaLanguageService.getClassDeclarationService().getMainClass(file);
    if (mainClassDeclarationNode.isEmpty()) {
      return Optional.empty();
    }
    if (!annotationNode.getType().equals("annotation")
        && !annotationNode.getType().equals("marker_annotation")) {
      return Optional.empty();
    }
    Optional<String> mainClassName =
        this.javaLanguageService
            .getClassDeclarationService()
            .getClassName(file, mainClassDeclarationNode.get());
    if (annotationNode.getType().equals("annotation")) {
      String annotationQuery =
          "(annotation"
              + "  name: (identifier) @annotation_name"
              + "  arguments: (annotation_argument_list"
              + "    (element_value_pair"
              + "      key: (identifier) @key"
              + "      value: (_) @value"
              + "    ) @pair"
              + "  )"
              + ")";
      List<Map<String, TSNode>> maps = file.queryForCaptures(annotationNode, annotationQuery);
      for (Map<String, TSNode> map : maps) {
        TSNode annotationNameNode = map.get("annotation_name");
        String annotationName = file.getTextFromNode(annotationNameNode);
        if (!annotationName.equals("Entity")) {
          continue;
        }
        TSNode keyNode = map.get("key");
        String keyName = file.getTextFromNode(keyNode);
        if (!keyName.equals("name")) {
          continue;
        }
        TSNode valueNode = map.get("value");
        String valueName = file.getTextFromNode(valueNode);
        if (!Strings.isNullOrEmpty(valueName) || valueNode != null) {
          return Optional.of(valueName);
        }
        return Optional.ofNullable(mainClassName.get());
      }
    } else {
      return Optional.ofNullable(mainClassName.get());
    }
    return Optional.empty();
  }

  public void testQuery() {
    String sourceCode =
        "class Example {\n"
            + "  @MyAnnotation(keyA = \"value1\", keyB = \"value2\")\n"
            + "  void method() {}\n"
            + "}";
    TSFile file = new TSFile(SupportedLanguage.JAVA, sourceCode);
    String queryString =
        "(annotation"
            + "  name: (identifier) @annotation_name"
            + "  arguments: (annotation_argument_list"
            + "    (element_value_pair"
            + "      key: (identifier) @key"
            + "      value: (_) @value"
            + "    ) @pair"
            + "  )"
            + ")";
    List<Map<String, TSNode>> nodes = file.queryForCaptures(queryString);
    for (Map<String, TSNode> matchMap : nodes) {
      TSNode annotationNameNode = matchMap.get("annotation_name");
      TSNode keyNode = matchMap.get("key");
      TSNode valueNode = matchMap.get("value");
      // Now you can safely get the text, knowing they belong together!
      String annotationName = file.getTextFromNode(annotationNameNode);
      String keyText = file.getTextFromNode(keyNode);
      String valueText = file.getTextFromNode(valueNode);

      System.out.printf(
          "Annotation: '%s' -> Key: '%s', Value: %s%n", annotationName, keyText, valueText);
    }
  }
}
