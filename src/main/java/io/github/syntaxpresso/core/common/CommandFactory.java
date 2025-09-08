package io.github.syntaxpresso.core.common;

import io.github.syntaxpresso.core.command.CreateNewFileCommand;
import io.github.syntaxpresso.core.command.GetCursorPositionInfo;
import io.github.syntaxpresso.core.command.GetMainClassCommand;
import io.github.syntaxpresso.core.command.ParseSourceCodeCommand;
import io.github.syntaxpresso.core.command.RenameCommand;
import io.github.syntaxpresso.core.service.java.command.CreateNewFileCommandService;
import io.github.syntaxpresso.core.service.java.command.GetCursorPositionInfoCommandService;
import io.github.syntaxpresso.core.service.java.command.GetMainClassCommandService;
import io.github.syntaxpresso.core.service.java.command.ParseSourceCodeCommandService;
import io.github.syntaxpresso.core.service.java.command.RenameCommandService;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine.IFactory;

@RequiredArgsConstructor
public class CommandFactory implements IFactory {
  private final RenameCommandService renameCommandService;
  private final GetMainClassCommandService getMainClassCommandService;
  private final CreateNewFileCommandService createNewFileCommandService;
  private final GetCursorPositionInfoCommandService getCursorPositionInfoCommandService;
  private final ParseSourceCodeCommandService parseSourceCodeCommandService;

  @Override
  @SuppressWarnings("unchecked")
  public <K> K create(Class<K> cls) throws Exception {
    if (cls == ParseSourceCodeCommand.class) {
      return (K) new ParseSourceCodeCommand(parseSourceCodeCommandService);
    }
    if (cls == GetCursorPositionInfo.class) {
      return (K) new GetCursorPositionInfo(getCursorPositionInfoCommandService);
    }
    if (cls == CreateNewFileCommand.class) {
      return (K) new CreateNewFileCommand(createNewFileCommandService);
    }
    if (cls == GetMainClassCommand.class) {
      return (K) new GetMainClassCommand(getMainClassCommandService);
    }
    if (cls == RenameCommand.class) {
      return (K) new RenameCommand(renameCommandService);
    }
    // if (cls == CreateJPARepositoryCommand.class) {
    //   return (K) new CreateJPARepositoryCommand(javaCommandService);
    // }
    // if (cls == GetJPAEntityInfoCommand.class) {
    //   return (K) new GetJPAEntityInfoCommand(javaCommandService);
    // }
    return cls.getDeclaredConstructor().newInstance();
  }
}
