package io.github.syntaxpresso.core.common;

import io.github.syntaxpresso.core.command.CreateJPARepositoryCommand;
import io.github.syntaxpresso.core.command.CreateNewFileCommand;
import io.github.syntaxpresso.core.command.GetMainClassCommand;
import io.github.syntaxpresso.core.command.GetTextFromCursorPositionCommand;
import io.github.syntaxpresso.core.command.RenameCommand;
import io.github.syntaxpresso.core.service.java.JavaService;
import picocli.CommandLine.IFactory;

public class CommandFactory implements IFactory {
  private final JavaService javaService = new JavaService();

  @Override
  @SuppressWarnings("unchecked")
  public <K> K create(Class<K> cls) throws Exception {
    if (cls == CreateNewFileCommand.class) {
      return (K) new CreateNewFileCommand(javaService);
    }
    if (cls == GetMainClassCommand.class) {
      return (K) new GetMainClassCommand(javaService);
    }
    if (cls == RenameCommand.class) {
      return (K) new RenameCommand(javaService);
    }
    if (cls == CreateJPARepositoryCommand.class) {
      return (K) new CreateJPARepositoryCommand(javaService);
    }
    if (cls == GetTextFromCursorPositionCommand.class) {
      return (K) new GetTextFromCursorPositionCommand(javaService);
    }
    return cls.getDeclaredConstructor().newInstance();
  }
}
