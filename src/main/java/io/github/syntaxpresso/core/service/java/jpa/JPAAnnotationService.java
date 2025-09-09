package io.github.syntaxpresso.core.service.java.jpa;

import io.github.syntaxpresso.core.common.TSFile;
import java.util.Optional;
import org.treesitter.TSNode;

public class JPAAnnotationService {
  public Optional<TSNode> getJPAEntityAnnotationInfo(TSFile tsFile, TSNode annotationNode) {
    return Optional.empty();
  }
}
