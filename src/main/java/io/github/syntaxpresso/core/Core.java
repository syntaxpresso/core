package io.github.syntaxpresso.core;

import com.google.gson.Gson;
import io.github.syntaxpresso.core.command.GenerateCommandInfoCommand;
import io.github.syntaxpresso.core.command.GenericCommand;
import io.github.syntaxpresso.core.command.JavaCommand;
import io.github.syntaxpresso.core.common.CommandFactory;
import java.util.concurrent.Callable;
import picocli.CommandLine;

@CommandLine.Command(
    subcommands = {JavaCommand.class, GenericCommand.class, GenerateCommandInfoCommand.class})
public class Core {
  public static void main(String[] args) {
    CommandLine commandLine = new CommandLine(new Core(), new CommandFactory());
    commandLine.setExecutionStrategy(
        parseResult -> {
          int exitCode = new CommandLine.RunLast().execute(parseResult);
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
                  System.out.println(new Gson().toJson(result));
                }
              } catch (Exception e) {
                e.printStackTrace();
                return 1;
              }
            }
          }
          return exitCode;
        });
    int exitCode = commandLine.execute(args);
    System.exit(exitCode);
  }
}
