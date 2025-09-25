import io.github.syntaxpresso.core.command.extra.JavaBasicType;
import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.service.java.language.FieldDeclarationService;
import io.github.syntaxpresso.core.service.java.language.ImportDeclarationService;
import io.github.syntaxpresso.core.service.java.language.PackageDeclarationService;
import io.github.syntaxpresso.core.service.java.language.extra.FieldInsertionPoint;
import io.github.syntaxpresso.core.service.java.language.extra.FieldInsertionPoint.FieldInsertionPosition;
import io.github.syntaxpresso.core.util.PathHelper;
import org.treesitter.TSNode;
import java.util.Optional;

public class TestAddFieldStructured {
    public static void main(String[] args) {
        String javaCode = """
            package com.example;
            
            public class User {
                private String existingField;
            }
            """;
        
        try {
            // Create TSFile and services
            TSFile tsFile = TSFile.fromSourceCode(javaCode, "java");
            PathHelper pathHelper = new PathHelper();
            PackageDeclarationService packageService = new PackageDeclarationService(pathHelper);
            ImportDeclarationService importService = new ImportDeclarationService();
            FieldDeclarationService fieldService = new FieldDeclarationService(packageService, importService);
            
            // Get class node
            Optional<TSNode> classNode = tsFile.query("(class_declaration) @class").execute().firstNodeAsOptional();
            if (classNode.isEmpty()) {
                System.out.println("FAIL: Could not find class node");
                return;
            }
            
            // Get insertion point
            FieldInsertionPoint insertionPoint = fieldService.getFieldInsertionPosition(
                tsFile, classNode.get(), FieldInsertionPosition.AFTER_LAST_FIELD);
            
            if (insertionPoint == null) {
                System.out.println("FAIL: Could not get insertion point");
                return;
            }
            
            // Test 1: Add JavaBasicType field without initialization
            fieldService.addField(tsFile, classNode.get(), insertionPoint,
                "private", false, JavaBasicType.LANG_STRING, "name", null);
            
            // Test 2: Add JavaBasicType field with initialization
            insertionPoint = fieldService.getFieldInsertionPosition(
                tsFile, classNode.get(), FieldInsertionPosition.AFTER_LAST_FIELD);
            fieldService.addField(tsFile, classNode.get(), insertionPoint,
                "public", true, JavaBasicType.PRIMITIVE_INT, "count", "0");
            
            // Test 3: Add custom type field
            insertionPoint = fieldService.getFieldInsertionPosition(
                tsFile, classNode.get(), FieldInsertionPosition.AFTER_LAST_FIELD);
            fieldService.addField(tsFile, classNode.get(), insertionPoint,
                "protected", false, "List<User>", "users", "new ArrayList<>()");
            
            // Print the result
            System.out.println("SUCCESS: Generated code:");
            System.out.println(tsFile.getSourceCode());
            
        } catch (Exception e) {
            System.out.println("FAIL: " + e.getMessage());
            e.printStackTrace();
        }
    }
}