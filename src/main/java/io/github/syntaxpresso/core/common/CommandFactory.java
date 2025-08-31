// package io.github.syntaxpresso.core.common;
//
// import io.github.syntaxpresso.core.command.CreateJPARepositoryCommand;
// import io.github.syntaxpresso.core.command.CreateNewFileCommand;
// import io.github.syntaxpresso.core.command.GetCursorPositionInfo;
// import io.github.syntaxpresso.core.command.GetJPAEntityInfoCommand;
// import io.github.syntaxpresso.core.command.GetMainClassCommand;
// import io.github.syntaxpresso.core.command.ParseSourceCodeCommand;
// import io.github.syntaxpresso.core.command.RenameCommand;
// import io.github.syntaxpresso.core.service.java.JavaCommandService;
// import picocli.CommandLine.IFactory;
//
// public class CommandFactory implements IFactory {
//   private final JavaCommandService javaCommandService = new JavaCommandService();
//
//   @Override
//   @SuppressWarnings("unchecked")
//   public <K> K create(Class<K> cls) throws Exception {
//     if (cls == CreateNewFileCommand.class) {
//       return (K) new CreateNewFileCommand(javaCommandService);
//     }
//     if (cls == GetMainClassCommand.class) {
//       return (K) new GetMainClassCommand(javaCommandService);
//     }
//     if (cls == RenameCommand.class) {
//       return (K) new RenameCommand(javaCommandService);
//     }
//     if (cls == CreateJPARepositoryCommand.class) {
//       return (K) new CreateJPARepositoryCommand(javaCommandService);
//     }
//     if (cls == GetCursorPositionInfo.class) {
//       return (K) new GetCursorPositionInfo(javaCommandService);
//     }
//     if (cls == GetJPAEntityInfoCommand.class) {
//       return (K) new GetJPAEntityInfoCommand(javaCommandService);
//     }
//     if (cls == ParseSourceCodeCommand.class) {
//       return (K) new ParseSourceCodeCommand(javaCommandService);
//     }
//     return cls.getDeclaredConstructor().newInstance();
//   }
// }
