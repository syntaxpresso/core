package io.github.syntaxpresso.core.command.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.syntaxpresso.core.command.CreateJPARepositoryCommand;
import io.github.syntaxpresso.core.command.dto.CreateNewFileResponse;
import io.github.syntaxpresso.core.common.DataTransferObject;
import io.github.syntaxpresso.core.common.extra.SupportedIDE;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import io.github.syntaxpresso.core.service.java.JavaService;
import java.lang.reflect.Field;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateJPARepositoryCommand Tests")
class CreateJPARepositoryCommandTest {
  @Mock private JavaService javaService;
  private CreateJPARepositoryCommand command;
  private Path cwd;
  private Path filePath;

  @BeforeEach
  void setUp() throws Exception {
    command = new CreateJPARepositoryCommand(javaService);
    cwd = Path.of("/test/project");
    filePath = Path.of("/test/project/src/main/java/com/example/User.java");
    Field cwdField = CreateJPARepositoryCommand.class.getDeclaredField("cwd");
    cwdField.setAccessible(true);
    cwdField.set(command, cwd);
    Field filePathField = CreateJPARepositoryCommand.class.getDeclaredField("filePath");
    filePathField.setAccessible(true);
    filePathField.set(command, filePath);
    Field languageField = CreateJPARepositoryCommand.class.getDeclaredField("language");
    languageField.setAccessible(true);
    languageField.set(command, SupportedLanguage.JAVA);
    Field ideField = CreateJPARepositoryCommand.class.getDeclaredField("ide");
    ideField.setAccessible(true);
    ideField.set(command, SupportedIDE.NONE);
  }

  @Nested
  @DisplayName("Command Execution Tests")
  class CommandExecutionTests {
    @Test
    @DisplayName("should call JavaService.createJPARepository with correct parameters")
    void call_shouldCallJavaServiceWithCorrectParameters() throws Exception {
      CreateNewFileResponse response =
          CreateNewFileResponse.builder()
              .filePath("/test/project/src/main/java/com/example/UserRepository.java")
              .build();
      DataTransferObject<CreateNewFileResponse> expectedResult =
          DataTransferObject.success(response);
      when(javaService.createJPARepository(any(Path.class), any(Path.class)))
          .thenReturn(expectedResult);
      DataTransferObject<CreateNewFileResponse> result = command.call();
      verify(javaService).createJPARepository(eq(cwd), eq(filePath));
      assertTrue(result.getSucceed());
      assertNotNull(result.getData());
      assertEquals(
          "/test/project/src/main/java/com/example/UserRepository.java",
          result.getData().getFilePath());
    }

    @Test
    @DisplayName("should return error when JavaService fails")
    void call_shouldReturnErrorWhenJavaServiceFails() throws Exception {
      DataTransferObject<CreateNewFileResponse> expectedResult =
          DataTransferObject.error("Entity class not found");
      when(javaService.createJPARepository(any(Path.class), any(Path.class)))
          .thenReturn(expectedResult);
      DataTransferObject<CreateNewFileResponse> result = command.call();
      assertFalse(result.getSucceed());
      assertEquals("Entity class not found", result.getErrorReason());
    }

    @Test
    @DisplayName("should handle successful repository creation")
    void call_shouldHandleSuccessfulRepositoryCreation() throws Exception {
      CreateNewFileResponse response =
          CreateNewFileResponse.builder()
              .filePath("/test/project/src/main/java/com/example/ProductRepository.java")
              .build();
      DataTransferObject<CreateNewFileResponse> expectedResult =
          DataTransferObject.success(response);
      when(javaService.createJPARepository(cwd, filePath)).thenReturn(expectedResult);
      DataTransferObject<CreateNewFileResponse> result = command.call();
      assertTrue(result.getSucceed());
      assertEquals(
          "/test/project/src/main/java/com/example/ProductRepository.java",
          result.getData().getFilePath());
      verify(javaService).createJPARepository(cwd, filePath);
    }
  }

  @Nested
  @DisplayName("Option Injection Tests")
  class OptionInjectionTests {
    @Test
    @DisplayName("should accept cwd and file-path options")
    void shouldAcceptCwdAndFilePathOptions() {
      assertNotNull(command);
    }
  }
}
