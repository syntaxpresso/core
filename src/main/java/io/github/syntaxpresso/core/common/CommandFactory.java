package io.github.syntaxpresso.core.common;

import io.github.syntaxpresso.core.command.*;
import io.github.syntaxpresso.core.service.java.command.*;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine.IFactory;

@RequiredArgsConstructor
public class CommandFactory implements IFactory {
  private final RenameCommandService renameCommandService;
  private final GetMainClassCommandService getMainClassCommandService;
  private final CreateNewFileCommandService createNewFileCommandService;
  private final GetCursorPositionInfoCommandService getCursorPositionInfoCommandService;
  private final CreateNewJPAEntityCommandService createNewJPAEntityCommandService;
  private final CreateJPARepositoryCommandService createJPARepositoryCommandService;
  private final GetJPAEntityInfoCommandService getJPAEntityInfoCommandService;
  private final GetAllFilesCommandService getAllFilesCommandService;

  @Override
  @SuppressWarnings("unchecked")
  public <K> K create(Class<K> cls) throws Exception {
    if (cls == GetCursorPositionInfoCommand.class) {
      return (K) new GetCursorPositionInfoCommand(getCursorPositionInfoCommandService);
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
    if (cls == CreateNewJPAEntityCommand.class) {
      return (K) new CreateNewJPAEntityCommand(createNewJPAEntityCommandService);
    }
    if (cls == CreateJPARepositoryCommand.class) {
      return (K) new CreateJPARepositoryCommand(createJPARepositoryCommandService);
    }
    if (cls == GetJPAEntityInfoCommand.class) {
      return (K) new GetJPAEntityInfoCommand(getJPAEntityInfoCommandService);
    }
    if (cls == GetAllFilesCommand.class) {
      return (K) new GetAllFilesCommand(getAllFilesCommandService);
    }
    return cls.getDeclaredConstructor().newInstance();
  }
}
