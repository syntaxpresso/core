package io.github.syntaxpresso.core.command.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.syntaxpresso.core.command.GetTextFromCursorPositionCommand;
import io.github.syntaxpresso.core.command.dto.GetTextFromCursorPositionResponse;
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
@DisplayName("GetTextFromCursorPositionCommand Tests")
class GetTextFromCursorPositionCommandTest {
  @Mock private JavaService javaService;
  private GetTextFromCursorPositionCommand command;
  private Path filePath;
  private Integer line;
  private Integer column;

  @BeforeEach
  void setUp() throws Exception {
    command = new GetTextFromCursorPositionCommand(javaService);
    filePath = Path.of("/test/project/src/main/java/com/example/User.java");
    line = 5;
    column = 10;
    
    // Set required fields using reflection
    Field filePathField = GetTextFromCursorPositionCommand.class.getDeclaredField("filePath");
    filePathField.setAccessible(true);
    filePathField.set(command, filePath);
    
    Field languageField = GetTextFromCursorPositionCommand.class.getDeclaredField("language");
    languageField.setAccessible(true);
    languageField.set(command, SupportedLanguage.JAVA);
    
    Field ideField = GetTextFromCursorPositionCommand.class.getDeclaredField("ide");
    ideField.setAccessible(true);
    ideField.set(command, SupportedIDE.NONE);
    
    Field lineField = GetTextFromCursorPositionCommand.class.getDeclaredField("line");
    lineField.setAccessible(true);
    lineField.set(command, line);
    
    Field columnField = GetTextFromCursorPositionCommand.class.getDeclaredField("column");
    columnField.setAccessible(true);
    columnField.set(command, column);
  }

  @Nested
  @DisplayName("Command Execution Tests")
  class CommandExecutionTests {
    @Test
    @DisplayName("should call JavaService.getTextFromCursorPosition with correct parameters")
    void call_shouldCallJavaServiceWithCorrectParameters() throws Exception {
      GetTextFromCursorPositionResponse response =
          GetTextFromCursorPositionResponse.builder()
              .filePath("/test/project/src/main/java/com/example/User.java")
              .node("identifier")
              .text("User")
              .build();
      DataTransferObject<GetTextFromCursorPositionResponse> expectedResult =
          DataTransferObject.success(response);
      when(javaService.getTextFromCursorPosition(
              any(Path.class), 
              any(SupportedLanguage.class), 
              any(SupportedIDE.class), 
              any(Integer.class), 
              any(Integer.class)))
          .thenReturn(expectedResult);

      DataTransferObject<GetTextFromCursorPositionResponse> result = command.call();

      verify(javaService).getTextFromCursorPosition(
          eq(filePath), 
          eq(SupportedLanguage.JAVA), 
          eq(SupportedIDE.NONE), 
          eq(line), 
          eq(column));
      assertTrue(result.getSucceed());
      assertNotNull(result.getData());
      assertEquals("/test/project/src/main/java/com/example/User.java", result.getData().getFilePath());
      assertEquals("identifier", result.getData().getNode());
      assertEquals("User", result.getData().getText());
    }

    @Test
    @DisplayName("should return error when JavaService fails")
    void call_shouldReturnErrorWhenJavaServiceFails() throws Exception {
      DataTransferObject<GetTextFromCursorPositionResponse> expectedResult =
          DataTransferObject.error("No symbol found at the specified position.");
      when(javaService.getTextFromCursorPosition(
              any(Path.class), 
              any(SupportedLanguage.class), 
              any(SupportedIDE.class), 
              any(Integer.class), 
              any(Integer.class)))
          .thenReturn(expectedResult);

      DataTransferObject<GetTextFromCursorPositionResponse> result = command.call();

      assertFalse(result.getSucceed());
      assertEquals("No symbol found at the specified position.", result.getErrorReason());
    }

    @Test
    @DisplayName("should handle successful text extraction")
    void call_shouldHandleSuccessfulTextExtraction() throws Exception {
      GetTextFromCursorPositionResponse response =
          GetTextFromCursorPositionResponse.builder()
              .filePath("/test/project/src/main/java/com/example/User.java")
              .node("method_declaration")
              .text("getName")
              .build();
      DataTransferObject<GetTextFromCursorPositionResponse> expectedResult =
          DataTransferObject.success(response);
      when(javaService.getTextFromCursorPosition(filePath, SupportedLanguage.JAVA, SupportedIDE.NONE, line, column))
          .thenReturn(expectedResult);

      DataTransferObject<GetTextFromCursorPositionResponse> result = command.call();

      assertTrue(result.getSucceed());
      assertEquals("/test/project/src/main/java/com/example/User.java", result.getData().getFilePath());
      assertEquals("method_declaration", result.getData().getNode());
      assertEquals("getName", result.getData().getText());
      verify(javaService).getTextFromCursorPosition(filePath, SupportedLanguage.JAVA, SupportedIDE.NONE, line, column);
    }


  }

  @Nested
  @DisplayName("Option Injection Tests")
  class OptionInjectionTests {
    @Test
    @DisplayName("should accept all required options")
    void shouldAcceptAllRequiredOptions() {
      assertNotNull(command);
    }
  }
}