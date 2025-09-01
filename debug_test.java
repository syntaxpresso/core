import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import io.github.syntaxpresso.core.service.java.language.ClassDeclarationService;
import io.github.syntaxpresso.core.service.java.language.ClassFieldDeclarationService;
import org.treesitter.TSNode;
import java.util.Optional;

public class DebugTest {
    public static void main(String[] args) {
        ClassFieldDeclarationService service = new ClassFieldDeclarationService();
        ClassDeclarationService classService = new ClassDeclarationService();
        
        String sourceCode = """
            public class TestClass {
                private String name;
                private int age;
                private boolean active;
            }
            """;
        
        TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
        TSNode classDeclarationNode = classService.getAllClassDeclarations(tsFile).get(0).get("classDeclaration");

        Optional<TSNode> result = service.findClassFieldNodeByName(tsFile, "name", classDeclarationNode);
        
        System.out.println("Result present: " + result.isPresent());
        if (result.isPresent()) {
            System.out.println("Field type: " + result.get().getType());
            System.out.println("Field text: " + tsFile.getTextFromNode(result.get()));
        }
        
        // Let's also test what the query returns
        String queryString = String.format(
            """
            ((field_declaration
              declarator: (variable_declarator
                name: (identifier) @name))
             (#eq? @name "%s"))
            """,
            "name");
        
        System.out.println("Query: " + queryString);
        Optional<TSNode> queryResult = tsFile.query(queryString).within(classDeclarationNode).execute().firstNodeOptional();
        System.out.println("Query result present: " + queryResult.isPresent());
        if (queryResult.isPresent()) {
            System.out.println("Query result text: " + tsFile.getTextFromNode(queryResult.get()));
        }
    }
}