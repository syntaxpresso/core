import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import io.github.syntaxpresso.core.service.java.language.ClassDeclarationService;
import io.github.syntaxpresso.core.service.java.language.ClassFieldDeclarationService;
import org.treesitter.TSNode;
import java.util.Optional;
import java.util.List;

class DebugFieldTest {
    public static void main(String[] args) {
        ClassFieldDeclarationService service = new ClassFieldDeclarationService();
        ClassDeclarationService classService = new ClassDeclarationService();
        
        String sourceCode = """
            public class TestClass {
                private String name;
                private int age;
            }
            """;
        
        TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
        List<?> allClasses = classService.getAllClassDeclarations(tsFile);
        System.out.println("Classes found: " + allClasses.size());
        
        if (allClasses.size() > 0) {
            Object firstClass = allClasses.get(0);
            System.out.println("First class type: " + firstClass.getClass().getName());
            
            if (firstClass instanceof java.util.Map) {
                java.util.Map<String, TSNode> classMap = (java.util.Map<String, TSNode>) firstClass;
                TSNode classDeclarationNode = classMap.get("classDeclaration");
                System.out.println("Class node type: " + classDeclarationNode.getType());
                
                List<TSNode> allFields = service.getAllClassFieldDeclarationNodes(tsFile, classDeclarationNode);
                System.out.println("Fields found: " + allFields.size());
                
                for (TSNode field : allFields) {
                    System.out.println("Field: " + tsFile.getTextFromNode(field));
                }
                
                // Test findClassFieldNodeByName
                Optional<TSNode> result = service.findClassFieldNodeByName(tsFile, "name", classDeclarationNode);
                System.out.println("Find by name result: " + result.isPresent());
                
                // Test findClassFieldNodesByType  
                List<TSNode> stringFields = service.findClassFieldNodesByType(tsFile, "String", classDeclarationNode);
                System.out.println("String fields found: " + stringFields.size());
            }
        }
    }
}