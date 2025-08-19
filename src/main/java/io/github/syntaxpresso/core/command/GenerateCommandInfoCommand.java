// core/src/main/java/io/github/syntaxpresso/core/command/GenerateCommandInfoCommand.java
package io.github.syntaxpresso.core.command;

import com.google.gson.Gson;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

@Command(
    name = "generate-command-info",
    hidden = true,
    description = "Generates command info as JSON for editor integration.")
public class GenerateCommandInfoCommand implements Runnable {
  @Spec CommandSpec spec;

  @Override
  public void run() {
    Map<String, Object> allCommands = new HashMap<>();
    // Start the recursive search from the root command's CommandLine instance
    findCommands(spec.root().commandLine(), "", allCommands);
    System.out.println(new Gson().toJson(allCommands));
  }

  private void findCommands(
      CommandLine commandLine, String prefix, Map<String, Object> commandMap) {
    for (Map.Entry<String, CommandLine> entry : commandLine.getSubcommands().entrySet()) {
      String commandName = entry.getKey();
      CommandLine subCommandLine = entry.getValue();
      CommandSpec subSpec = subCommandLine.getCommandSpec();
      if (!subCommandLine.getSubcommands().isEmpty()) {
        findCommands(subCommandLine, prefix + commandName + "-", commandMap);
      } else {
        if (commandName.equals("generate-command-info")) {
          continue;
        }
        String fullCommandName = prefix + commandName;
        commandMap.put(
            fullCommandName,
            Map.of(
                "description",
                subSpec.usageMessage().description() != null
                    ? String.join(" ", subSpec.usageMessage().description())
                    : "",
                "options",
                subSpec.options().stream()
                    .filter(opt -> opt.longestName() != null)
                    .map(
                        opt ->
                            Map.of(
                                "name", opt.longestName(),
                                "description",
                                    opt.description() != null
                                        ? String.join(" ", opt.description())
                                        : "",
                                "type",
                                    opt.type() != null ? opt.type().getSimpleName() : "boolean"))
                    .collect(Collectors.toList())));
      }
    }
  }
}
