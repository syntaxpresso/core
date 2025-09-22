package io.github.syntaxpresso.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.syntaxpresso.core.command.CreateNewFileCommand;
import io.github.syntaxpresso.core.command.GetCursorPositionInfoCommand;
import io.github.syntaxpresso.core.command.GetMainClassCommand;
import io.github.syntaxpresso.core.command.ParseSourceCodeCommand;
import io.github.syntaxpresso.core.command.RenameCommand;
import io.github.syntaxpresso.core.command.*;
import io.github.syntaxpresso.core.common.CommandFactory;
import io.github.syntaxpresso.core.service.java.JavaLanguageService;
import io.github.syntaxpresso.core.service.java.command.*;
import io.github.syntaxpresso.core.service.java.language.*;
import io.github.syntaxpresso.core.util.PathHelper;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
    subcommands = {
      RenameCommand.class,
      GetMainClassCommand.class,
      CreateNewFileCommand.class,
      GetCursorPositionInfoCommand.class,
      ParseSourceCodeCommand.class,
      CreateNewJPAEntityCommand.class
    })
public class Core {

  public static void main(String[] args) {
    CommandFactory commandFactory = createCommandFactory();
    CommandLine commandLine = new CommandLine(new Core(), commandFactory);
    commandLine.setExecutionStrategy(
        parseResult -> {
          if (!parseResult.subcommands().isEmpty()) {
            CommandLine.ParseResult lastSubcommand = parseResult;
            while (!lastSubcommand.subcommands().isEmpty()) {
              lastSubcommand =
                  lastSubcommand.subcommands().get(lastSubcommand.subcommands().size() - 1);
            }
            Object userObject = lastSubcommand.commandSpec().userObject();
            if (userObject instanceof Callable) {
              try {
                Object result = ((Callable<?>) userObject).call();
                if (result != null) {
                  try {
                    System.out.println(new ObjectMapper().writeValueAsString(result));
                  } catch (Exception e) {
                    System.out.println(result.toString());
                  }
                }
                return 0;
              } catch (Exception e) {
                e.printStackTrace();
                return 1;
              }
            }
          }
          // Fallback to default execution if no subcommands
          return new CommandLine.RunLast().execute(parseResult);
        });
    int exitCode = commandLine.execute(args);
    System.exit(exitCode);
  }

  private static CommandFactory createCommandFactory() {
    PathHelper pathHelper = new PathHelper();
    VariableNamingService variableNamingService = new VariableNamingService();
    FormalParameterService formalParameterDeclarationService = new FormalParameterService();
    MethodDeclarationService methodDeclarationService =
        new MethodDeclarationService(formalParameterDeclarationService);
    FieldDeclarationService fieldDeclarationService = new FieldDeclarationService();
    ClassDeclarationService classDeclarationService =
        new ClassDeclarationService(fieldDeclarationService, methodDeclarationService);
    PackageDeclarationService packageDeclarationService = new PackageDeclarationService();
    ImportDeclarationService importDeclarationService = new ImportDeclarationService();
    LocalVariableDeclarationService localVariableDeclarationService =
        new LocalVariableDeclarationService();
    AnnotationService annotationService = new AnnotationService();
    JavaLanguageService javaLanguageService =
        new JavaLanguageService(
            pathHelper,
            variableNamingService,
            classDeclarationService,
            packageDeclarationService,
            importDeclarationService,
            localVariableDeclarationService,
            annotationService);
    RenameCommandService renameCommandService =
        new RenameCommandService(variableNamingService, javaLanguageService);
    GetMainClassCommandService getMainClassCommandService =
        new GetMainClassCommandService(javaLanguageService);
    CreateNewFileCommandService createNewFileCommandService =
        new CreateNewFileCommandService(javaLanguageService, pathHelper);
    GetCursorPositionInfoCommandService getCursorPositionInfoCommandService =
        new GetCursorPositionInfoCommandService(javaLanguageService, pathHelper);
    ParseSourceCodeCommandService parseSourceCodeCommandService =
        new ParseSourceCodeCommandService();
    CreateNewJPAEntityCommandService createNewJPAEntityCommandService =
        new CreateNewJPAEntityCommandService(javaLanguageService, createNewFileCommandService);
    return new CommandFactory(
        renameCommandService,
        getMainClassCommandService,
        createNewFileCommandService,
        getCursorPositionInfoCommandService,
        parseSourceCodeCommandService,
        createNewJPAEntityCommandService);
  }
}
