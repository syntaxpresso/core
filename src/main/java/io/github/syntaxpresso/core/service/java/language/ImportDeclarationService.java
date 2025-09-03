package io.github.syntaxpresso.core.service.java.language;

import com.google.common.base.Strings;
import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.service.java.language.extra.ImportCapture;
import io.github.syntaxpresso.core.service.java.language.extra.ImportInsertionPoint;
import io.github.syntaxpresso.core.service.java.language.extra.ImportInsertionPoint.ImprtInsertionPosition;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.Getter;
import org.treesitter.TSNode;

@Getter
public class ImportDeclarationService {
  private final PackageDeclarationService packageDeclarationService =
      new PackageDeclarationService();

  public List<TSNode> getAllImportDeclarationNodes(TSFile tsFile) {
    if (tsFile == null || tsFile.getTree() == null) {
      return Collections.emptyList();
    }
    String queryString =
        """
        (import_declaration) @declaration
        """;
    return tsFile.query(queryString).execute().nodes();
  }

  public List<Map<String, TSNode>> getImportDeclarationNodeInfo(
      TSFile tsFile, TSNode importDeclarationNode) {
    if (tsFile == null
        || tsFile.getTree() == null
        || !importDeclarationNode.getType().equals("import_declaration")) {
      return Collections.emptyList();
    }
    String queryString =
        String.format(
            """
            (import_declaration
              (scoped_identifier
                  scope: (scoped_identifier) %s
                      name: (identifier) %s
                ) %s
                (asterisk)? %s
            ) %s
            """,
            ImportCapture.RELATIVE_IMPORT_SCOPE.getCaptureWithAt(),
            ImportCapture.CLASS_NAME.getCaptureWithAt(),
            ImportCapture.FULL_IMPORT_SCOPE.getCaptureWithAt(),
            ImportCapture.ASTERISK.getCaptureWithAt(),
            ImportCapture.IMPORT.getCaptureWithAt());
    return tsFile
        .query(queryString)
        .within(importDeclarationNode)
        .returningAllCaptures()
        .execute()
        .captures();
  }

  public Optional<TSNode> getImportDeclarationChildByCaptureName(
      TSFile tsFile, TSNode importDeclarationNode, ImportCapture capture) {
    if (tsFile == null
        || tsFile.getTree() == null
        || !importDeclarationNode.getType().equals("import_declaration")) {
      return Optional.empty();
    }
    List<Map<String, TSNode>> nodeInfo =
        this.getImportDeclarationNodeInfo(tsFile, importDeclarationNode);
    for (Map<String, TSNode> map : nodeInfo) {
      TSNode node = map.get(capture.getCaptureName());
      if (node != null) {
        return Optional.of(node);
      }
    }
    return Optional.empty();
  }

  public Optional<TSNode> findImportDeclarationNode(
      TSFile tsFile, String packageScope, String className) {
    if (tsFile == null
        || tsFile.getTree() == null
        || Strings.isNullOrEmpty(className)
        || Strings.isNullOrEmpty(packageScope)) {
      return Optional.empty();
    }
    List<TSNode> allImportDeclarationNodes = this.getAllImportDeclarationNodes(tsFile);
    for (TSNode importDeclarationNode : allImportDeclarationNodes) {
      List<Map<String, TSNode>> importDeclarationNodeInfo =
          this.getImportDeclarationNodeInfo(tsFile, importDeclarationNode);
      for (Map<String, TSNode> map : importDeclarationNodeInfo) {
        TSNode fullImportNode = map.get(ImportCapture.FULL_IMPORT_SCOPE.getCaptureName());
        TSNode classNameNode = map.get(ImportCapture.CLASS_NAME.getCaptureName());
        TSNode asteriskNode = map.get(ImportCapture.ASTERISK.getCaptureName());
        if (fullImportNode == null) {
          continue;
        }
        String fullImportText = tsFile.getTextFromNode(fullImportNode);
        if (asteriskNode != null) {
          if (packageScope.equals(fullImportText)) {
            return Optional.of(importDeclarationNode);
          }
        } else {
          if (classNameNode != null) {
            String classNameText = tsFile.getTextFromNode(classNameNode);
            if (className.equals(classNameText)) {
              return Optional.of(importDeclarationNode);
            }
          }
        }
      }
    }
    return Optional.empty();
  }

  public Boolean isClassImported(TSFile tsFile, String packageScope, String className) {
    return this.findImportDeclarationNode(tsFile, packageScope, className).isPresent();
  }

  public Optional<TSNode> getImportDeclarationRelativeImportScopeNode(
      TSFile tsFile, TSNode importDeclarationNode) {
    return this.getImportDeclarationChildByCaptureName(
        tsFile, importDeclarationNode, ImportCapture.RELATIVE_IMPORT_SCOPE);
  }

  public Optional<TSNode> getImportDeclarationClassNameNode(
      TSFile tsFile, TSNode importDeclarationNode) {
    return this.getImportDeclarationChildByCaptureName(
        tsFile, importDeclarationNode, ImportCapture.CLASS_NAME);
  }

  public Optional<TSNode> getImportDeclarationFullImportScopeNode(
      TSFile tsFile, TSNode importDeclarationNode) {
    return this.getImportDeclarationChildByCaptureName(
        tsFile, importDeclarationNode, ImportCapture.FULL_IMPORT_SCOPE);
  }

  private ImportInsertionPoint getImportInsertionPoint(
      TSFile tsFile, TSNode packageDeclarationNode) {
    if (tsFile == null || tsFile.getTree() == null) {
      return null;
    }
    ImportInsertionPoint insertPoint = new ImportInsertionPoint();
    List<TSNode> existingImports = this.getAllImportDeclarationNodes(tsFile);
    if (existingImports.isEmpty()) {
      insertPoint.setInsertByte(packageDeclarationNode.getEndByte());
      insertPoint.setPosition(ImprtInsertionPosition.AFTER_PACKAGE_DECLARATION);
      return insertPoint;
    } else if (packageDeclarationNode == null) {
      insertPoint.setInsertByte(0);
      insertPoint.setPosition(ImprtInsertionPosition.BEGINNING);
      return insertPoint;
    }
    TSNode lastImportNode = existingImports.get(existingImports.size() - 1);
    insertPoint.setInsertByte(lastImportNode.getEndByte());
    insertPoint.setPosition(ImprtInsertionPosition.AFTER_LAST_IMPORT);
    return insertPoint;
  }

  public void addImport(
      TSFile tsFile, String packageScope, String className, TSNode packageDeclarationNode) {
    if (this.isClassImported(tsFile, packageScope, className)) {
      return;
    }
    ImportInsertionPoint insertionPoint =
        this.getImportInsertionPoint(tsFile, packageDeclarationNode);
    String fullImportStatement = packageScope + "." + className;
    String importStatement = null;
    if (insertionPoint.getPosition().equals(ImprtInsertionPosition.AFTER_PACKAGE_DECLARATION)) {
      importStatement = "\n\nimport " + fullImportStatement + ";";
      tsFile.updateSourceCode(
          insertionPoint.getInsertByte(), insertionPoint.getInsertByte(), importStatement);
    } else if (insertionPoint.getPosition().equals(ImprtInsertionPosition.BEGINNING)) {
      // No package, no imports - insert at start
      importStatement = "import " + fullImportStatement + ";\n";
      tsFile.updateSourceCode(
          insertionPoint.getInsertByte(), insertionPoint.getInsertByte(), importStatement);
    } else {
      // After existing imports
      importStatement = "\nimport " + fullImportStatement + ";";
      tsFile.updateSourceCode(
          insertionPoint.getInsertByte(), insertionPoint.getInsertByte(), importStatement);
    }
  }

  public boolean updateImport(
      TSFile tsFile,
      String oldPackageScope,
      String newPackageScope,
      String oldClassName,
      String newClassName) {
    if (this.isClassImported(tsFile, oldPackageScope, newClassName)) {
      return false;
    }
    Optional<TSNode> existingImport =
        this.findImportDeclarationNode(tsFile, oldPackageScope, newClassName);
    if (existingImport.isEmpty()) {
      return false;
    }
    List<Map<String, TSNode>> existingImportInfo =
        this.getImportDeclarationNodeInfo(tsFile, existingImport.get());
    boolean isModified = false;
    for (Map<String, TSNode> map : existingImportInfo) {
      TSNode fullImportScopeNode = map.get(ImportCapture.FULL_IMPORT_SCOPE.getCaptureName());
      TSNode classNameNode = map.get(ImportCapture.CLASS_NAME.getCaptureName());
      TSNode asteriskNode = map.get(ImportCapture.ASTERISK.getCaptureName());
      if (fullImportScopeNode != null) {
        tsFile.updateSourceCode(fullImportScopeNode, newPackageScope);
        isModified = true;
      }
      if (asteriskNode != null && classNameNode != null) {
        tsFile.updateSourceCode(classNameNode, newClassName);
        isModified = true;
      }
    }
    return isModified;
  }
}
