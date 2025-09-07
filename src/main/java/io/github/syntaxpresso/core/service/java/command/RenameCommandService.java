package io.github.syntaxpresso.core.service.java.command;

import com.google.common.base.Strings;
import io.github.syntaxpresso.core.command.dto.RenameResponse;
import io.github.syntaxpresso.core.common.DataTransferObject;
import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.common.extra.SupportedIDE;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import io.github.syntaxpresso.core.service.extra.JavaIdentifierType;
import io.github.syntaxpresso.core.service.java.JavaLanguageService;
import io.github.syntaxpresso.core.service.java.command.extra.RenameOperation;
import io.github.syntaxpresso.core.service.java.command.extra.RenameSourceFileData;
import io.github.syntaxpresso.core.service.java.language.VariableNamingService;
import io.github.syntaxpresso.core.util.StringHelper;
import io.github.syntaxpresso.core.util.StringHelper.CaseFormat;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.treesitter.TSNode;

@Getter
@RequiredArgsConstructor
public class RenameCommandService {
  private final VariableNamingService variableNamingService;
  private final JavaLanguageService javaLanguageService;
  private final List<RenameOperation> renameOperations = new ArrayList<>();

  private void addRenameOperation(TSFile tsFile, TSNode tsNode, String text) {
    this.renameOperations.add(
        RenameOperation.builder().tsFile(tsFile).node(tsNode).text(text).build());
  }

  private void executeAllRenameOperations() {
    this.renameOperations.sort(
        (a, b) -> Integer.compare(b.getNode().getStartByte(), a.getNode().getStartByte()));
    for (RenameOperation renameOperation : this.renameOperations) {
      renameOperation
          .getTsFile()
          .updateSourceCode(renameOperation.getNode(), renameOperation.getText());
      try {
        renameOperation.getTsFile().save();

      } catch (IOException e) {
        System.out.println(1);
      }
    }
  }

  private boolean renameSourceFile(RenameSourceFileData sourceFileData, String newName) {
    Optional<TSNode> classDeclarationNode =
        this.javaLanguageService
            .getClassDeclarationService()
            .findClassByName(
                sourceFileData.getSourceFile(), sourceFileData.getSourceCursorPositionText());
    if (classDeclarationNode.isEmpty()) {
      return false;
    }
    Optional<TSNode> classDeclarationNameNode =
        this.javaLanguageService
            .getClassDeclarationService()
            .getClassDeclarationNameNode(
                sourceFileData.getSourceFile(), classDeclarationNode.get());
    if (classDeclarationNameNode.isEmpty()) {
      return false;
    }
    String newNamePascalCase = StringHelper.convert(newName, CaseFormat.PASCAL_CASE);
    Optional<TSNode> publicClassNode =
        this.javaLanguageService
            .getClassDeclarationService()
            .getPublicClass(sourceFileData.getSourceFile());
    if (publicClassNode.isEmpty()) {
      return false;
    }
    Optional<TSNode> publicClassNameNode =
        this.javaLanguageService
            .getClassDeclarationService()
            .getClassDeclarationNameNode(sourceFileData.getSourceFile(), publicClassNode.get());
    if (publicClassNameNode.isPresent()) {
      String classDeclarationName =
          sourceFileData.getSourceFile().getTextFromNode(classDeclarationNameNode.get());
      String publicClassName =
          sourceFileData.getSourceFile().getTextFromNode(publicClassNameNode.get());
      if (publicClassName.equals(classDeclarationName)) {
        sourceFileData.getSourceFile().rename(newNamePascalCase);
      }
    } else {
      return false;
    }
    this.addRenameOperation(
        sourceFileData.getSourceFile(), classDeclarationNameNode.get(), newNamePascalCase);
    return true;
  }

  private void renameFieldDeclarations(
      TSFile tsFile, TSNode classDeclarationNode, String oldFieldType, String newFieldType) {
    List<TSNode> allFieldDeclarationNodes =
        this.javaLanguageService
            .getClassDeclarationService()
            .getFieldDeclarationService()
            .findFieldDeclarationNodesByType(tsFile, oldFieldType, classDeclarationNode);
    for (TSNode fieldDeclarationNode : allFieldDeclarationNodes) {
      Optional<TSNode> fieldDeclarationFullTypeNode =
          this.javaLanguageService
              .getClassDeclarationService()
              .getFieldDeclarationService()
              .getFieldDeclarationFullTypeNode(tsFile, fieldDeclarationNode);
      Optional<TSNode> fieldDeclarationTypeNode =
          this.javaLanguageService
              .getClassDeclarationService()
              .getFieldDeclarationService()
              .getFieldDeclarationTypeNode(tsFile, fieldDeclarationNode);
      Optional<TSNode> fieldDeclarationNameNode =
          this.javaLanguageService
              .getClassDeclarationService()
              .getFieldDeclarationService()
              .getFieldDeclarationNameNode(tsFile, fieldDeclarationNode);
      if (fieldDeclarationNameNode.isEmpty()
          || fieldDeclarationTypeNode.isEmpty()
          || fieldDeclarationFullTypeNode.isEmpty()) {
        return;
      }
      String currentFieldFullType = tsFile.getTextFromNode(fieldDeclarationTypeNode.get());
      String currentFieldType = tsFile.getTextFromNode(fieldDeclarationTypeNode.get());
      String currentFieldName = tsFile.getTextFromNode(fieldDeclarationNameNode.get());
      if (!currentFieldType.equals(oldFieldType)) {
        continue;
      }
      boolean isCollectionType = this.variableNamingService.isCollectionType(currentFieldFullType);
      boolean shouldRenameVariable =
          this.variableNamingService.shouldRenameVariable(
              currentFieldName, currentFieldType, isCollectionType);
      Optional<TSNode> fieldVariableNameNode =
          this.javaLanguageService
              .getClassDeclarationService()
              .getFieldDeclarationService()
              .getFieldDeclarationNameNode(tsFile, fieldDeclarationNode);
      if (fieldVariableNameNode.isEmpty()) {
        continue;
      }
      String newVariableName =
          this.variableNamingService.generateNewVariableName(
              currentFieldName, currentFieldType, newFieldType, isCollectionType);
      List<TSNode> fieldDeclarationUsageNodes =
          this.javaLanguageService
              .getClassDeclarationService()
              .getFieldDeclarationService()
              .getAllFieldDeclarationUsageNodes(tsFile, fieldDeclarationNode, classDeclarationNode);
      for (TSNode fieldDeclarationUsageNode : fieldDeclarationUsageNodes.reversed()) {
        if (shouldRenameVariable) {
          this.addRenameOperation(tsFile, fieldDeclarationUsageNode, newVariableName);
        }
      }
      if (shouldRenameVariable) {
        this.addRenameOperation(tsFile, fieldDeclarationNameNode.get(), newVariableName);
      }
      this.addRenameOperation(tsFile, fieldDeclarationTypeNode.get(), newFieldType);
    }
  }

  private void renameFormalParameters(
      TSFile tsFile,
      TSNode classDeclarationNode,
      String oldParameterType,
      String newParameterType) {
    List<TSNode> allMethodDeclarationNodes =
        this.javaLanguageService
            .getClassDeclarationService()
            .getMethodDeclarationService()
            .getAllMethodDeclarationNodes(tsFile, classDeclarationNode);
    for (TSNode methodDeclarationNode : allMethodDeclarationNodes) {
      List<TSNode> allFormalParameterDeclarationNodes =
          this.getJavaLanguageService()
              .getClassDeclarationService()
              .getMethodDeclarationService()
              .getFormalParameterService()
              .getAllFormalParameterNodes(tsFile, methodDeclarationNode);
      for (TSNode formalParameterDeclarationNode : allFormalParameterDeclarationNodes) {
        Optional<TSNode> formalParamenterDeclarationTypeNode =
            this.javaLanguageService
                .getClassDeclarationService()
                .getMethodDeclarationService()
                .getFormalParameterService()
                .getFormalParameterTypeNode(tsFile, formalParameterDeclarationNode);
        Optional<TSNode> formalParamenterDeclarationFullTypeNode =
            this.javaLanguageService
                .getClassDeclarationService()
                .getMethodDeclarationService()
                .getFormalParameterService()
                .getFormalParameterFullTypeNode(tsFile, formalParameterDeclarationNode);
        Optional<TSNode> formalParamenterDeclarationNameNode =
            this.javaLanguageService
                .getClassDeclarationService()
                .getMethodDeclarationService()
                .getFormalParameterService()
                .getFormalParameterNameNode(tsFile, formalParameterDeclarationNode);
        if (formalParamenterDeclarationTypeNode.isEmpty()
            || formalParamenterDeclarationFullTypeNode.isEmpty()
            || formalParamenterDeclarationNameNode.isEmpty()) {
          continue;
        }
        List<TSNode> allFormalParameterDeclarationUsages =
            this.javaLanguageService
                .getClassDeclarationService()
                .getMethodDeclarationService()
                .getFormalParameterService()
                .findAllFormalParameterNodeUsages(
                    tsFile, formalParameterDeclarationNode, methodDeclarationNode);
        String currentParamenterFullType =
            tsFile.getTextFromNode(formalParamenterDeclarationFullTypeNode.get());
        String currentFieldType = tsFile.getTextFromNode(formalParamenterDeclarationTypeNode.get());
        String currentFieldName = tsFile.getTextFromNode(formalParamenterDeclarationNameNode.get());
        if (!oldParameterType.equals(currentFieldType)) {
          continue;
        }
        boolean isCollectionType =
            this.variableNamingService.isCollectionType(currentParamenterFullType);
        boolean shouldRenameVariable =
            this.variableNamingService.shouldRenameVariable(
                currentFieldName, oldParameterType, isCollectionType);
        String newVariableName =
            this.variableNamingService.generateNewVariableName(
                currentFieldName, oldParameterType, newParameterType, isCollectionType);
        for (TSNode formalParameterDeclarationUsage : allFormalParameterDeclarationUsages) {
          if (shouldRenameVariable) {
            this.addRenameOperation(tsFile, formalParameterDeclarationUsage, newVariableName);
          }
        }
        if (shouldRenameVariable) {
          this.addRenameOperation(
              tsFile, formalParamenterDeclarationNameNode.get(), newVariableName);
        }
        this.addRenameOperation(
            tsFile, formalParamenterDeclarationTypeNode.get(), newParameterType);
      }
    }
  }

  private RenameSourceFileData prepareSourceFileData(
      final Path cwd,
      final Path filePath,
      final SupportedIDE ide,
      final int line,
      final int column) {
    TSFile tsFile = new TSFile(SupportedLanguage.JAVA, filePath);
    TSNode cursorPositionNode = tsFile.getNodeFromPosition(line, column, ide);
    if (cursorPositionNode == null) {
      return null;
    }
    String cursorPositionNodeName;
    cursorPositionNodeName = tsFile.getTextFromNode(cursorPositionNode);
    if (Strings.isNullOrEmpty(cursorPositionNodeName)) {
      return null;
    }
    Optional<TSNode> packageNode =
        this.javaLanguageService.getPackageDeclarationService().getPackageDeclarationNode(tsFile);
    if (packageNode.isEmpty()) {
      return null;
    }
    Optional<TSNode> packageScopeNode =
        this.javaLanguageService
            .getPackageDeclarationService()
            .getPackageScopeNode(tsFile, packageNode.get());
    if (packageScopeNode.isEmpty()) {
      return null;
    }
    String packageScopeText = tsFile.getTextFromNode(packageScopeNode.get());
    JavaIdentifierType cursorPositionNodeType =
        this.javaLanguageService.getIdentifierType(cursorPositionNode, ide);
    if (cursorPositionNodeType == null) {
      return null;
    }
    return RenameSourceFileData.builder()
        .cwd(cwd)
        .sourceFile(tsFile)
        .sourcePackageNode(packageNode.get())
        .sourcePackageScopeNode(packageScopeNode.get())
        .sourcePackageScopeText(packageScopeText)
        .sourceCursorPositionNode(cursorPositionNode)
        .sourceCursorPositionText(cursorPositionNodeName)
        .sourceCursorPositionType(cursorPositionNodeType)
        .build();
  }

  public boolean processClassRename(RenameSourceFileData sourceFileData, String newName) {
    boolean sourceFileRenamed = this.renameSourceFile(sourceFileData, newName);
    if (!sourceFileRenamed) {
      return false;
    }
    List<TSFile> allJavaFiles =
        this.javaLanguageService.getAllJavaFilesFromCwd(sourceFileData.getCwd());
    for (TSFile tsFile : allJavaFiles) {
      Optional<TSNode> classDeclarationNode =
          this.javaLanguageService.getClassDeclarationService().getPublicClass(tsFile);
      if (classDeclarationNode.isEmpty()) {
        return false;
      }
      // TODO: check if is checking same package and wildcard import.
      boolean isImported =
          this.javaLanguageService
              .getImportDeclarationService()
              .isClassImported(
                  tsFile,
                  sourceFileData.getSourcePackageScopeText(),
                  sourceFileData.getSourceCursorPositionText());
      if (!isImported) {
        continue;
      }
      this.renameFieldDeclarations(
          tsFile,
          classDeclarationNode.get(),
          sourceFileData.getSourceCursorPositionText(),
          newName);
      this.renameFormalParameters(
          tsFile,
          classDeclarationNode.get(),
          sourceFileData.getSourceCursorPositionText(),
          newName);
    }
    return true;
  }

  public DataTransferObject<RenameResponse> rename(
      final Path cwd,
      final Path filePath,
      final SupportedIDE ide,
      final int line,
      final int column,
      final String newName) {
    RenameSourceFileData sourceFileData =
        this.prepareSourceFileData(cwd, filePath, ide, line, column);
    if (sourceFileData == null) {
      DataTransferObject.error("Unable to fetch source file data.");
    }
    if (sourceFileData.getSourceCursorPositionType().equals(JavaIdentifierType.CLASS_NAME)) {
      boolean classRenameSuccessful = this.processClassRename(sourceFileData, newName);
    }
    if (sourceFileData.getSourceCursorPositionType().equals(JavaIdentifierType.METHOD_NAME)) {}
    this.executeAllRenameOperations();
    return DataTransferObject.success(
        RenameResponse.builder()
            .filePath(filePath.toString())
            .renamedNodes(this.renameOperations.size())
            .newName(newName)
            .build());
  }
}
