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
    String filePath = tsFile.getFile().getAbsolutePath();
    int startByte = tsNode.getStartByte();
    int endByte = tsNode.getEndByte();
    boolean alreadyExists =
        this.renameOperations.stream()
            .anyMatch(
                operation ->
                    operation.getTsFile().getFile().getAbsolutePath().equals(filePath)
                        && operation.getNode().getStartByte() == startByte
                        && operation.getNode().getEndByte() == endByte);
    if (!alreadyExists) {
      this.renameOperations.add(
          RenameOperation.builder().tsFile(tsFile).node(tsNode).text(text).build());
    }
  }

  private DataTransferObject<RenameResponse> executeAllRenameOperations() {
    this.renameOperations.sort(
        (a, b) -> Integer.compare(b.getNode().getStartByte(), a.getNode().getStartByte()));
    for (RenameOperation renameOperation : this.renameOperations) {
      renameOperation
          .getTsFile()
          .updateSourceCode(renameOperation.getNode(), renameOperation.getText());
      try {
        renameOperation.getTsFile().save();
      } catch (IOException e) {
        return DataTransferObject.error("Unable to save file: " + e.getMessage());
      }
    }
    return DataTransferObject.success();
  }

  private DataTransferObject<RenameResponse> renameSourceFile(
      RenameSourceFileData sourceFileData, String newName) {
    Optional<TSNode> classDeclarationNode =
        this.javaLanguageService
            .getClassDeclarationService()
            .findClassByName(
                sourceFileData.getSourceFile(), sourceFileData.getSourceCursorPositionText());
    if (classDeclarationNode.isEmpty()) {
      return DataTransferObject.error("Unable to find source file's related class.");
    }
    Optional<TSNode> classDeclarationNameNode =
        this.javaLanguageService
            .getClassDeclarationService()
            .getClassDeclarationNameNode(
                sourceFileData.getSourceFile(), classDeclarationNode.get());
    if (classDeclarationNameNode.isEmpty()) {
      return DataTransferObject.error("Unable to find source file's class name node.");
    }
    String newNamePascalCase = StringHelper.convert(newName, CaseFormat.PASCAL_CASE);
    Optional<TSNode> publicClassNode =
        this.javaLanguageService
            .getClassDeclarationService()
            .getPublicClass(sourceFileData.getSourceFile());
    if (publicClassNode.isEmpty()) {
      return DataTransferObject.error("Unable to find the public class of source file.");
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
      return DataTransferObject.error(
          "Unable to find the name of the public class of the source file.");
    }
    this.addRenameOperation(
        sourceFileData.getSourceFile(), classDeclarationNameNode.get(), newNamePascalCase);
    return DataTransferObject.success();
  }

  private DataTransferObject<RenameResponse> renameImportDeclaration(
      TSFile tsFile, RenameSourceFileData sourceFileData, String newName) {
    Optional<TSNode> importDeclarationNode =
        this.javaLanguageService
            .getImportDeclarationService()
            .findImportDeclarationNode(
                tsFile,
                sourceFileData.getSourcePackageScopeText(),
                sourceFileData.getSourceCursorPositionText());
    if (importDeclarationNode.isEmpty()) {
      return DataTransferObject.error("Unable to find import declaration to rename.");
    }
    boolean isWildCardImport =
        this.javaLanguageService
            .getImportDeclarationService()
            .isWildCardImport(tsFile, importDeclarationNode.get());
    if (isWildCardImport) {
      return DataTransferObject.success();
    }
    Optional<TSNode> importDeclarationClassNode =
        this.javaLanguageService
            .getImportDeclarationService()
            .getImportDeclarationClassNameNode(tsFile, importDeclarationNode.get());
    if (importDeclarationClassNode.isEmpty()) {
      return DataTransferObject.error("Unable to find class name in the import scope.");
    }
    this.addRenameOperation(tsFile, importDeclarationClassNode.get(), newName);
    return DataTransferObject.success();
  }

  private DataTransferObject<RenameResponse> renameFieldDeclarations(
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
        return DataTransferObject.error("Unable to collect base field data.");
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
    return DataTransferObject.success();
  }

  private DataTransferObject<RenameResponse> renameFormalParameters(
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
    return DataTransferObject.success();
  }

  private DataTransferObject<RenameResponse> renameLocalVariables(
      TSFile tsFile, TSNode classDeclarationNode, String oldName, String newName) {
    List<TSNode> allLocalVariableNodes =
        this.javaLanguageService
            .getLocalVariableDeclarationService()
            .findLocalVariableDeclarationByType(tsFile, oldName);
    for (TSNode localVariableNode : allLocalVariableNodes) {
      Optional<TSNode> localVariableNameNode =
          this.javaLanguageService
              .getLocalVariableDeclarationService()
              .getLocalVariableDeclarationNameNode(tsFile, localVariableNode);
      Optional<TSNode> localVariableTypeNode =
          this.javaLanguageService
              .getLocalVariableDeclarationService()
              .getVariableDeclarationClassTypeNode(tsFile, localVariableNode);
      Optional<TSNode> localVariableFullTypeNode =
          this.javaLanguageService
              .getLocalVariableDeclarationService()
              .getVariableDeclarationFullTypeNode(tsFile, localVariableNode);
      Optional<TSNode> localVariableValueType =
          this.javaLanguageService
              .getLocalVariableDeclarationService()
              .getLocalVariableValueClassTypeNode(tsFile, localVariableNode);
      boolean isCollection = false;
      if (localVariableFullTypeNode.isPresent()) {
        String localVariableFullTypeText = tsFile.getTextFromNode(localVariableFullTypeNode.get());
        isCollection = this.variableNamingService.isCollectionType(localVariableFullTypeText);
      }
      if (localVariableNameNode.isEmpty() || localVariableTypeNode.isEmpty()) {
        continue;
      }
      String newTypeName = StringHelper.toPascalCase(newName);
      String localVariableNameText = tsFile.getTextFromNode(localVariableNameNode.get());
      String localVariableTypeText = tsFile.getTextFromNode(localVariableTypeNode.get());
      boolean shouldRenameVariable =
          this.variableNamingService.shouldRenameVariable(
              localVariableNameText, localVariableTypeText, isCollection);
      if (shouldRenameVariable) {
        String generatedLocalVariableName =
            this.variableNamingService.generateNewVariableName(
                localVariableNameText, localVariableTypeText, newName, isCollection);
        this.addRenameOperation(tsFile, localVariableNameNode.get(), generatedLocalVariableName);
      }
      if (localVariableValueType.isPresent()) {
        this.addRenameOperation(tsFile, localVariableValueType.get(), newTypeName);
      }
      this.addRenameOperation(tsFile, localVariableTypeNode.get(), newTypeName);
    }
    return DataTransferObject.success();
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

  public DataTransferObject<RenameResponse> processClassRename(
      RenameSourceFileData sourceFileData, String newName) {
    DataTransferObject<RenameResponse> sourceFileRenamed =
        this.renameSourceFile(sourceFileData, newName);
    if (!sourceFileRenamed.getSucceed()) {
      return sourceFileRenamed;
    }
    List<TSFile> allJavaFiles =
        this.javaLanguageService.getAllJavaFilesFromCwd(sourceFileData.getCwd());
    for (TSFile tsFile : allJavaFiles) {
      Optional<TSNode> classDeclarationNode =
          this.javaLanguageService.getClassDeclarationService().getPublicClass(tsFile);
      if (classDeclarationNode.isEmpty()) {
        continue;
      }
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
      DataTransferObject<RenameResponse> importDeclarationsRename =
          this.renameImportDeclaration(tsFile, sourceFileData, newName);
      if (!importDeclarationsRename.getSucceed()) {
        return importDeclarationsRename;
      }
      DataTransferObject<RenameResponse> fieldDeclarationsRename =
          this.renameFieldDeclarations(
              tsFile,
              classDeclarationNode.get(),
              sourceFileData.getSourceCursorPositionText(),
              newName);
      if (!fieldDeclarationsRename.getSucceed()) {
        return fieldDeclarationsRename;
      }
      DataTransferObject<RenameResponse> formalParametersRename =
          this.renameFormalParameters(
              tsFile,
              classDeclarationNode.get(),
              sourceFileData.getSourceCursorPositionText(),
              newName);
      if (!formalParametersRename.getSucceed()) {
        return formalParametersRename;
      }
      DataTransferObject<RenameResponse> localVariablesRename =
          this.renameLocalVariables(
              tsFile,
              classDeclarationNode.get(),
              sourceFileData.getSourceCursorPositionText(),
              newName);
      if (!localVariablesRename.getSucceed()) {
        return localVariablesRename;
      }
    }
    return DataTransferObject.success();
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
      DataTransferObject<RenameResponse> classRename =
          this.processClassRename(sourceFileData, newName);
      if (!classRename.getSucceed()) {
        return classRename;
      }
    }
    if (sourceFileData.getSourceCursorPositionType().equals(JavaIdentifierType.METHOD_NAME)) {}
    DataTransferObject<RenameResponse> operationsExecution = this.executeAllRenameOperations();
    if (!operationsExecution.getSucceed()) {
      return operationsExecution;
    }
    return DataTransferObject.success(
        RenameResponse.builder()
            .filePath(filePath.toString())
            .renamedNodes(this.renameOperations.size())
            .newName(newName)
            .build());
  }
}
