package io.github.syntaxpresso.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.syntaxpresso.core.command.CreateJPARepositoryCommand;
import io.github.syntaxpresso.core.command.CreateNewFileCommand;
import io.github.syntaxpresso.core.command.GetMainClassCommand;
import io.github.syntaxpresso.core.command.GetTextFromCursorPositionCommand;
import io.github.syntaxpresso.core.command.RenameCommand;
import io.github.syntaxpresso.core.common.CommandFactory;
import java.util.concurrent.Callable;
import picocli.CommandLine;

@CommandLine.Command(
    subcommands = {
      RenameCommand.class,
      GetMainClassCommand.class,
      CreateNewFileCommand.class,
      CreateJPARepositoryCommand.class,
      GetTextFromCursorPositionCommand.class
    })
public class Core {
  public static void main(String[] args) {
    CommandLine commandLine = new CommandLine(new Core(), new CommandFactory());
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
}
