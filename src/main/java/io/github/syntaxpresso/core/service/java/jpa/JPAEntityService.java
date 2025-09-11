package io.github.syntaxpresso.core.service.java.jpa;

import com.google.common.base.Strings;
import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.service.java.JavaLanguageService;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.treesitter.TSNode;

@RequiredArgsConstructor
public class JPAEntityService {
  private final JavaLanguageService javaLanguageService;

  /**
   * Finds the JPA Entity annotation node in the class.
   *
   * @param file The TSFile containing the class to search.
   * @return Optional containing the Entity annotation node if found, empty otherwise.
   */
  private final Optional<TSNode> getJPAEntityAnnotationNode(TSFile file) {
    List<TSNode> mainClassAnnotationNodes =
        this.javaLanguageService.getClassDeclarationService().getAllClassAnnotations(file);
    for (TSNode annotation : mainClassAnnotationNodes) {
      String annotationName = file.getTextFromNode(annotation);
      if (annotationName.contains("Entity")) {
        List<TSNode> importNodes =
            this.javaLanguageService.getImportDeclarationService().findAllImportDeclarations(file);
        for (TSNode importNode : importNodes) {
          String importName = file.getTextFromNode(importNode);
          if (importName.equals("import jakarta.persistence.Entity;")
              || importName.equals("import jakarta.persistence.*;")) {
            return Optional.of(annotation);
          }
        }
      }
    }
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
  public Optional<String> getJPAEntityName(TSFile file) {
    Optional<TSNode> entityAnnotationNode = this.getJPAEntityAnnotationNode(file);
    if (entityAnnotationNode.isEmpty()) {
      return Optional.empty();
    }
    Optional<TSNode> mainClassDeclarationNode =
        this.javaLanguageService.getClassDeclarationService().getMainClass(file);
    if (mainClassDeclarationNode.isEmpty()) {
      return Optional.empty();
    }
    String entityName = null;
    if (entityAnnotationNode.get().getType().equals("annotation")) {
      Optional<TSNode> elementValuePairNode =
          file.findChildNodeByType(entityAnnotationNode.get(), "element_value_pair");
      if (elementValuePairNode.isEmpty()) {
        entityName =
            this.javaLanguageService
                .getClassDeclarationService()
                .getClassName(file, mainClassDeclarationNode.get())
                .orElse(null);
      }
    }
    if (Strings.isNullOrEmpty(entityName)
        && entityAnnotationNode.get().getType().equals("marker_annotation")) {
      entityName =
          this.javaLanguageService
              .getClassDeclarationService()
              .getClassName(file, mainClassDeclarationNode.get())
              .orElse(null);
    }
    return Optional.ofNullable(entityName);
  }
}
