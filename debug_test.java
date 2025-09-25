import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.service.java.language.*;
import io.github.syntaxpresso.core.command.extra.JavaBasicType;

public class DebugTest {
    public static void main(String[] args) {
        // Debug first failing test - insertion positions
        String testCode1 = """
            package com.example.test;
            
            public class TestClass {
                private String existingField;
            }
            """;
            
        TSFile tsFile1 = new TSFile(SupportedLanguage.JAVA, testCode1);
        ClassDeclarationService classDeclarationService = new ClassDeclarationService();
        var classNode = classDeclarationService.findClassByName(tsFile1, "TestClass");
        
        if (classNode.isPresent()) {
            FieldDeclarationService fieldService = new FieldDeclarationService(
                new PackageDeclarationService(), 
                new ImportDeclarationService()
            );
            
            var beforeFirstPoint = fieldService.getFieldInsertionPosition(
                tsFile1, classNode.get(), FieldDeclarationService.FieldInsertionPosition.BEFORE_FIRST_FIELD);
            
            System.out.println("Before first point: " + beforeFirstPoint);
            System.out.println("Original code:\n" + tsFile1.getSourceCode());
            
            if (beforeFirstPoint != null) {
                fieldService.addField(tsFile1, classNode.get(), beforeFirstPoint, 
                    "private", false, JavaBasicType.PRIMITIVE_INT, "beforeFirst", null);
                
                System.out.println("After adding beforeFirst:\n" + tsFile1.getSourceCode());
                
                int beforeFirstIndex = tsFile1.getSourceCode().indexOf("private int beforeFirst;");
                int existingFieldIndex = tsFile1.getSourceCode().indexOf("private String existingField;");
                
                System.out.println("beforeFirstIndex: " + beforeFirstIndex);
                System.out.println("existingFieldIndex: " + existingFieldIndex);
            }
        }
        
        System.out.println("\n=== Second failing test ===");
        
        // Debug second failing test - imports
        String testCode2 = """
            package com.example.test;
            
            public class TestClass {
                
            }
            """;
            
        TSFile tsFile2 = new TSFile(SupportedLanguage.JAVA, testCode2);
        var classNode2 = classDeclarationService.findClassByName(tsFile2, "TestClass");
        
        if (classNode2.isPresent()) {
            FieldDeclarationService fieldService = new FieldDeclarationService(
                new PackageDeclarationService(), 
                new ImportDeclarationService()
            );
            
            var insertionPoint = fieldService.getFieldInsertionPosition(
                tsFile2, classNode2.get(), FieldDeclarationService.FieldInsertionPosition.BEGINNING_OF_CLASS_BODY);
            
            System.out.println("Original code:\n" + tsFile2.getSourceCode());
            
            if (insertionPoint != null) {
                fieldService.addField(tsFile2, classNode2.get(), insertionPoint, 
                    "private", false, JavaBasicType.UTIL_UUID, "uuidField", null);
                
                System.out.println("After adding UUID field:\n" + tsFile2.getSourceCode());
                
                boolean hasImport = tsFile2.getSourceCode().contains("import java.util.UUID;");
                boolean hasField = tsFile2.getSourceCode().contains("private UUID uuidField;");
                
                System.out.println("Has import: " + hasImport);
                System.out.println("Has field: " + hasField);
            }
        }
    }
}
