package io.github.syntaxpresso.core.command;

import io.github.syntaxpresso.core.command.java.CreateNewFileCommand;
import io.github.syntaxpresso.core.command.java.CreateNewJPAEntityCommand;
import io.github.syntaxpresso.core.command.java.GetMainClassCommand;
import io.github.syntaxpresso.core.command.java.RenameCommand;
import picocli.CommandLine.Command;

@Command(
    name = "java",
    description = "Parent command for all Java-specific operations.",
    subcommands = {
      CreateNewFileCommand.class,
      GetMainClassCommand.class,
      RenameCommand.class,
      CreateNewJPAEntityCommand.class
    })
public class JavaCommand {}
