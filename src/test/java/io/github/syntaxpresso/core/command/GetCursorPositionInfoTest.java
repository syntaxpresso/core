package io.github.syntaxpresso.core.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.syntaxpresso.core.command.dto.GetCursorPositionInfoResponse;
import io.github.syntaxpresso.core.common.DataTransferObject;
import io.github.syntaxpresso.core.common.extra.SupportedIDE;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import io.github.syntaxpresso.core.service.extra.JavaIdentifierType;
import io.github.syntaxpresso.core.service.java.JavaCommandService;
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
@DisplayName("GetCursorPositionInfo Tests")
class GetCursorPositionInfoTest {
  @Mock private JavaCommandService javaService;
  private GetCursorPositionInfo command;
  private Path filePath;
  private Integer line;
  private Integer column;

  @BeforeEach
  void setUp() throws Exception {
    command = new GetCursorPositionInfo(javaService);
    filePath = Path.of("/test/project/src/main/java/com/example/User.java");
    line = 5;
    column = 10;
    // Set required fields using reflection
    Field filePathField = GetCursorPositionInfo.class.getDeclaredField("filePath");
    filePathField.setAccessible(true);
    filePathField.set(command, filePath);
    Field languageField = GetCursorPositionInfo.class.getDeclaredField("language");
    languageField.setAccessible(true);
    languageField.set(command, SupportedLanguage.JAVA);
    Field ideField = GetCursorPositionInfo.class.getDeclaredField("ide");
    ideField.setAccessible(true);
    ideField.set(command, SupportedIDE.NONE);
    Field lineField = GetCursorPositionInfo.class.getDeclaredField("line");
    lineField.setAccessible(true);
    lineField.set(command, line);
    Field columnField = GetCursorPositionInfo.class.getDeclaredField("column");
    columnField.setAccessible(true);
    columnField.set(command, column);
  }

  @Nested
  @DisplayName("Command Execution Tests")
  class CommandExecutionTests {
    @Test
    @DisplayName("should call JavaService.getTextFromCursorPosition with correct parameters")
    void call_shouldCallJavaServiceWithCorrectParameters() throws Exception {
      GetCursorPositionInfoResponse response =
          GetCursorPositionInfoResponse.builder()
              .filePath("/test/project/src/main/java/com/example/User.java")
              .language(SupportedLanguage.JAVA)
              .node("identifier")
              .nodeType(JavaIdentifierType.CLASS_NAME)
              .nodeText("User")
              .build();
      DataTransferObject<GetCursorPositionInfoResponse> expectedResult =
          DataTransferObject.success(response);
      when(javaService.getTextFromCursorPosition(
              any(Path.class),
              any(SupportedLanguage.class),
              any(SupportedIDE.class),
              any(Integer.class),
              any(Integer.class)))
          .thenReturn(expectedResult);
      DataTransferObject<GetCursorPositionInfoResponse> result = command.call();
      verify(javaService)
          .getTextFromCursorPosition(
              eq(filePath),
              eq(SupportedLanguage.JAVA),
              eq(SupportedIDE.NONE),
              eq(line),
              eq(column));
      assertTrue(result.getSucceed());
      assertNotNull(result.getData());
      assertEquals(
          "/test/project/src/main/java/com/example/User.java", result.getData().getFilePath());
      assertEquals(SupportedLanguage.JAVA, result.getData().getLanguage());
      assertEquals("identifier", result.getData().getNode());
      assertEquals(JavaIdentifierType.CLASS_NAME, result.getData().getNodeType());
      assertEquals("User", result.getData().getNodeText());
    }

    @Test
    @DisplayName("should return error when JavaService fails")
    void call_shouldReturnErrorWhenJavaServiceFails() throws Exception {
      DataTransferObject<GetCursorPositionInfoResponse> expectedResult =
          DataTransferObject.error("No symbol found at the specified position.");
      when(javaService.getTextFromCursorPosition(
              any(Path.class),
              any(SupportedLanguage.class),
              any(SupportedIDE.class),
              any(Integer.class),
              any(Integer.class)))
          .thenReturn(expectedResult);
      DataTransferObject<GetCursorPositionInfoResponse> result = command.call();
      assertFalse(result.getSucceed());
      assertEquals("No symbol found at the specified position.", result.getErrorReason());
    }

    @Test
    @DisplayName("should handle successful text extraction")
    void call_shouldHandleSuccessfulTextExtraction() throws Exception {
      GetCursorPositionInfoResponse response =
          GetCursorPositionInfoResponse.builder()
              .filePath("/test/project/src/main/java/com/example/User.java")
              .language(SupportedLanguage.JAVA)
              .node("method_declaration")
              .nodeType(JavaIdentifierType.METHOD_NAME)
              .nodeText("getName")
              .build();
      DataTransferObject<GetCursorPositionInfoResponse> expectedResult =
          DataTransferObject.success(response);
      when(javaService.getTextFromCursorPosition(
              filePath, SupportedLanguage.JAVA, SupportedIDE.NONE, line, column))
          .thenReturn(expectedResult);
      DataTransferObject<GetCursorPositionInfoResponse> result = command.call();
      assertTrue(result.getSucceed());
      assertEquals(
          "/test/project/src/main/java/com/example/User.java", result.getData().getFilePath());
      assertEquals(SupportedLanguage.JAVA, result.getData().getLanguage());
      assertEquals("method_declaration", result.getData().getNode());
      assertEquals(JavaIdentifierType.METHOD_NAME, result.getData().getNodeType());
      assertEquals("getName", result.getData().getNodeText());
      verify(javaService)
          .getTextFromCursorPosition(
              filePath, SupportedLanguage.JAVA, SupportedIDE.NONE, line, column);
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

  @Nested
  @DisplayName("NodeType Field Tests")
  class NodeTypeFieldTests {
    @Test
    @DisplayName("should handle CLASS_NAME nodeType correctly")
    void call_shouldHandleClassNameNodeType() throws Exception {
      GetCursorPositionInfoResponse response =
          GetCursorPositionInfoResponse.builder()
              .filePath("/test/project/src/main/java/com/example/User.java")
              .language(SupportedLanguage.JAVA)
              .node("identifier")
              .nodeType(JavaIdentifierType.CLASS_NAME)
              .nodeText("User")
              .build();
      DataTransferObject<GetCursorPositionInfoResponse> expectedResult =
          DataTransferObject.success(response);
      when(javaService.getTextFromCursorPosition(
              filePath, SupportedLanguage.JAVA, SupportedIDE.NONE, line, column))
          .thenReturn(expectedResult);
      DataTransferObject<GetCursorPositionInfoResponse> result = command.call();
      assertTrue(result.getSucceed());
      assertEquals(SupportedLanguage.JAVA, result.getData().getLanguage());
      assertEquals(JavaIdentifierType.CLASS_NAME, result.getData().getNodeType());
      assertEquals("User", result.getData().getNodeText());
      assertEquals("identifier", result.getData().getNode());
    }

    @Test
    @DisplayName("should handle METHOD_NAME nodeType correctly")
    void call_shouldHandleMethodNameNodeType() throws Exception {
      GetCursorPositionInfoResponse response =
          GetCursorPositionInfoResponse.builder()
              .filePath("/test/project/src/main/java/com/example/User.java")
              .language(SupportedLanguage.JAVA)
              .node("method_declaration")
              .nodeType(JavaIdentifierType.METHOD_NAME)
              .nodeText("getName")
              .build();
      DataTransferObject<GetCursorPositionInfoResponse> expectedResult =
          DataTransferObject.success(response);
      when(javaService.getTextFromCursorPosition(
              filePath, SupportedLanguage.JAVA, SupportedIDE.NONE, line, column))
          .thenReturn(expectedResult);
      DataTransferObject<GetCursorPositionInfoResponse> result = command.call();
      assertTrue(result.getSucceed());
      assertEquals(SupportedLanguage.JAVA, result.getData().getLanguage());
      assertEquals(JavaIdentifierType.METHOD_NAME, result.getData().getNodeType());
      assertEquals("getName", result.getData().getNodeText());
      assertEquals("method_declaration", result.getData().getNode());
    }

    @Test
    @DisplayName("should handle FIELD_NAME nodeType correctly")
    void call_shouldHandleFieldNameNodeType() throws Exception {
      GetCursorPositionInfoResponse response =
          GetCursorPositionInfoResponse.builder()
              .filePath("/test/project/src/main/java/com/example/User.java")
              .language(SupportedLanguage.JAVA)
              .node("field_declaration")
              .nodeType(JavaIdentifierType.FIELD_NAME)
              .nodeText("name")
              .build();
      DataTransferObject<GetCursorPositionInfoResponse> expectedResult =
          DataTransferObject.success(response);
      when(javaService.getTextFromCursorPosition(
              filePath, SupportedLanguage.JAVA, SupportedIDE.NONE, line, column))
          .thenReturn(expectedResult);
      DataTransferObject<GetCursorPositionInfoResponse> result = command.call();
      assertTrue(result.getSucceed());
      assertEquals(SupportedLanguage.JAVA, result.getData().getLanguage());
      assertEquals(JavaIdentifierType.FIELD_NAME, result.getData().getNodeType());
      assertEquals("name", result.getData().getNodeText());
      assertEquals("field_declaration", result.getData().getNode());
    }

    @Test
    @DisplayName("should handle LOCAL_VARIABLE_NAME nodeType correctly")
    void call_shouldHandleLocalVariableNameNodeType() throws Exception {
      GetCursorPositionInfoResponse response =
          GetCursorPositionInfoResponse.builder()
              .filePath("/test/project/src/main/java/com/example/User.java")
              .language(SupportedLanguage.JAVA)
              .node("local_variable_declaration")
              .nodeType(JavaIdentifierType.LOCAL_VARIABLE_NAME)
              .nodeText("localVar")
              .build();
      DataTransferObject<GetCursorPositionInfoResponse> expectedResult =
          DataTransferObject.success(response);
      when(javaService.getTextFromCursorPosition(
              filePath, SupportedLanguage.JAVA, SupportedIDE.NONE, line, column))
          .thenReturn(expectedResult);
      DataTransferObject<GetCursorPositionInfoResponse> result = command.call();
      assertTrue(result.getSucceed());
      assertEquals(SupportedLanguage.JAVA, result.getData().getLanguage());
      assertEquals(JavaIdentifierType.LOCAL_VARIABLE_NAME, result.getData().getNodeType());
      assertEquals("localVar", result.getData().getNodeText());
      assertEquals("local_variable_declaration", result.getData().getNode());
    }

    @Test
    @DisplayName("should handle FORMAL_PARAMETER_NAME nodeType correctly")
    void call_shouldHandleFormalParameterNameNodeType() throws Exception {
      GetCursorPositionInfoResponse response =
          GetCursorPositionInfoResponse.builder()
              .filePath("/test/project/src/main/java/com/example/User.java")
              .language(SupportedLanguage.JAVA)
              .node("formal_parameter")
              .nodeType(JavaIdentifierType.FORMAL_PARAMETER_NAME)
              .nodeText("param")
              .build();
      DataTransferObject<GetCursorPositionInfoResponse> expectedResult =
          DataTransferObject.success(response);
      when(javaService.getTextFromCursorPosition(
              filePath, SupportedLanguage.JAVA, SupportedIDE.NONE, line, column))
          .thenReturn(expectedResult);
      DataTransferObject<GetCursorPositionInfoResponse> result = command.call();
      assertTrue(result.getSucceed());
      assertEquals(SupportedLanguage.JAVA, result.getData().getLanguage());
      assertEquals(JavaIdentifierType.FORMAL_PARAMETER_NAME, result.getData().getNodeType());
      assertEquals("param", result.getData().getNodeText());
      assertEquals("formal_parameter", result.getData().getNode());
    }

    @Test
    @DisplayName("should handle null nodeType gracefully")
    void call_shouldHandleNullNodeType() throws Exception {
      GetCursorPositionInfoResponse response =
          GetCursorPositionInfoResponse.builder()
              .filePath("/test/project/src/main/java/com/example/User.java")
              .language(SupportedLanguage.JAVA)
              .node("identifier")
              .nodeType(null)
              .nodeText("Unknown")
              .build();
      DataTransferObject<GetCursorPositionInfoResponse> expectedResult =
          DataTransferObject.success(response);
      when(javaService.getTextFromCursorPosition(
              filePath, SupportedLanguage.JAVA, SupportedIDE.NONE, line, column))
          .thenReturn(expectedResult);
      DataTransferObject<GetCursorPositionInfoResponse> result = command.call();
      assertTrue(result.getSucceed());
      assertEquals(SupportedLanguage.JAVA, result.getData().getLanguage());
      assertNull(result.getData().getNodeType());
      assertEquals("Unknown", result.getData().getNodeText());
    }
  }

  @Nested
  @DisplayName("Complete Response Structure Tests")
  class CompleteResponseStructureTests {
    @Test
    @DisplayName("should validate all fields are populated correctly")
    void call_shouldValidateAllFieldsPopulated() throws Exception {
      GetCursorPositionInfoResponse response =
          GetCursorPositionInfoResponse.builder()
              .filePath("/test/project/src/main/java/com/example/User.java")
              .language(SupportedLanguage.JAVA)
              .node("identifier")
              .nodeType(JavaIdentifierType.CLASS_NAME)
              .nodeText("User")
              .build();
      DataTransferObject<GetCursorPositionInfoResponse> expectedResult =
          DataTransferObject.success(response);
      when(javaService.getTextFromCursorPosition(
              filePath, SupportedLanguage.JAVA, SupportedIDE.NONE, line, column))
          .thenReturn(expectedResult);
      DataTransferObject<GetCursorPositionInfoResponse> result = command.call();
      assertTrue(result.getSucceed());
      GetCursorPositionInfoResponse data = result.getData();
      assertNotNull(data);
      assertEquals("/test/project/src/main/java/com/example/User.java", data.getFilePath());
      assertEquals(SupportedLanguage.JAVA, data.getLanguage());
      assertEquals("identifier", data.getNode());
      assertEquals(JavaIdentifierType.CLASS_NAME, data.getNodeType());
      assertEquals("User", data.getNodeText());
    }

    @Test
    @DisplayName("should handle response with minimal required fields")
    void call_shouldHandleMinimalResponse() throws Exception {
      GetCursorPositionInfoResponse response =
          GetCursorPositionInfoResponse.builder()
              .filePath("/test/file.java")
              .language(SupportedLanguage.JAVA)
              .nodeText("text")
              .build();
      DataTransferObject<GetCursorPositionInfoResponse> expectedResult =
          DataTransferObject.success(response);
      when(javaService.getTextFromCursorPosition(
              filePath, SupportedLanguage.JAVA, SupportedIDE.NONE, line, column))
          .thenReturn(expectedResult);
      DataTransferObject<GetCursorPositionInfoResponse> result = command.call();
      assertTrue(result.getSucceed());
      GetCursorPositionInfoResponse data = result.getData();
      assertEquals("/test/file.java", data.getFilePath());
      assertEquals(SupportedLanguage.JAVA, data.getLanguage());
      assertEquals("text", data.getNodeText());
      assertNull(data.getNode());
      assertNull(data.getNodeType());
    }
  }

  @Nested
  @DisplayName("JSON Serialization Tests")
  class JsonSerializationTests {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("should serialize response with all fields to JSON correctly")
    void serialization_shouldHandleCompleteResponse() throws Exception {
      GetCursorPositionInfoResponse response =
          GetCursorPositionInfoResponse.builder()
              .filePath("/test/project/src/main/java/com/example/User.java")
              .language(SupportedLanguage.JAVA)
              .node("identifier")
              .nodeType(JavaIdentifierType.CLASS_NAME)
              .nodeText("User")
              .build();
      DataTransferObject<GetCursorPositionInfoResponse> dto = DataTransferObject.success(response);
      String json = objectMapper.writeValueAsString(dto);
      assertNotNull(json);
      assertTrue(
          json.contains("\"filePath\":\"/test/project/src/main/java/com/example/User.java\""));
      assertTrue(json.contains("\"language\":\"JAVA\""));
      assertTrue(json.contains("\"node\":\"identifier\""));
      assertTrue(json.contains("\"nodeType\":\"CLASS_NAME\""));
      assertTrue(json.contains("\"nodeText\":\"User\""));
      assertTrue(json.contains("\"succeed\":true"));
    }

    @Test
    @DisplayName("should serialize and deserialize response correctly")
    void serialization_shouldRoundTripCorrectly() throws Exception {
      GetCursorPositionInfoResponse originalResponse =
          GetCursorPositionInfoResponse.builder()
              .filePath("/test/file.java")
              .language(SupportedLanguage.JAVA)
              .node("method_declaration")
              .nodeType(JavaIdentifierType.METHOD_NAME)
              .nodeText("testMethod")
              .build();
      DataTransferObject<GetCursorPositionInfoResponse> originalDto =
          DataTransferObject.success(originalResponse);
      String json = objectMapper.writeValueAsString(originalDto);
      @SuppressWarnings("unchecked")
      DataTransferObject<GetCursorPositionInfoResponse> deserializedDto =
          objectMapper.readValue(json, DataTransferObject.class);
      assertNotNull(deserializedDto);
      assertTrue(deserializedDto.getSucceed());
    }

    @Test
    @DisplayName("should handle null nodeType in JSON serialization")
    void serialization_shouldHandleNullNodeType() throws Exception {
      GetCursorPositionInfoResponse response =
          GetCursorPositionInfoResponse.builder()
              .filePath("/test/file.java")
              .language(SupportedLanguage.JAVA)
              .node("identifier")
              .nodeType(null)
              .nodeText("text")
              .build();
      DataTransferObject<GetCursorPositionInfoResponse> dto = DataTransferObject.success(response);
      String json = objectMapper.writeValueAsString(dto);
      assertNotNull(json);
      assertTrue(json.contains("\"nodeType\":null"));
      assertTrue(json.contains("\"language\":\"JAVA\""));
    }

    @Test
    @DisplayName("should serialize language field correctly")
    void serialization_shouldHandleLanguageField() throws Exception {
      GetCursorPositionInfoResponse response =
          GetCursorPositionInfoResponse.builder()
              .filePath("/test/file.java")
              .language(SupportedLanguage.JAVA)
              .nodeText("text")
              .build();
      DataTransferObject<GetCursorPositionInfoResponse> dto = DataTransferObject.success(response);
      String json = objectMapper.writeValueAsString(dto);
      assertNotNull(json);
      assertTrue(json.contains("\"language\":\"JAVA\""));
      assertTrue(json.contains("\"filePath\":\"/test/file.java\""));
      assertTrue(json.contains("\"succeed\":true"));
    }
  }

  @Nested
  @DisplayName("Builder Pattern and Edge Case Tests")
  class BuilderPatternTests {
    @Test
    @DisplayName("should create response using builder with all fields")
    void builder_shouldCreateCompleteResponse() {
      GetCursorPositionInfoResponse response =
          GetCursorPositionInfoResponse.builder()
              .filePath("/test/file.java")
              .language(SupportedLanguage.JAVA)
              .node("identifier")
              .nodeType(JavaIdentifierType.FIELD_NAME)
              .nodeText("fieldName")
              .build();
      assertEquals("/test/file.java", response.getFilePath());
      assertEquals(SupportedLanguage.JAVA, response.getLanguage());
      assertEquals("identifier", response.getNode());
      assertEquals(JavaIdentifierType.FIELD_NAME, response.getNodeType());
      assertEquals("fieldName", response.getNodeText());
    }

    @Test
    @DisplayName("should create response using builder with partial fields")
    void builder_shouldCreatePartialResponse() {
      GetCursorPositionInfoResponse response =
          GetCursorPositionInfoResponse.builder()
              .filePath("/test/file.java")
              .language(SupportedLanguage.JAVA)
              .nodeText("text")
              .build();
      assertEquals("/test/file.java", response.getFilePath());
      assertEquals(SupportedLanguage.JAVA, response.getLanguage());
      assertEquals("text", response.getNodeText());
      assertNull(response.getNode());
      assertNull(response.getNodeType());
    }

    @Test
    @DisplayName("should create response using no-args constructor")
    void constructor_shouldCreateEmptyResponse() {
      GetCursorPositionInfoResponse response = new GetCursorPositionInfoResponse();
      assertNull(response.getFilePath());
      assertNull(response.getLanguage());
      assertNull(response.getNode());
      assertNull(response.getNodeType());
      assertNull(response.getNodeText());
    }

    @Test
    @DisplayName("should create response using all-args constructor")
    void constructor_shouldCreateCompleteResponseWithAllArgs() {
      GetCursorPositionInfoResponse response =
          new GetCursorPositionInfoResponse(
              "/test/file.java",
              SupportedLanguage.JAVA,
              "identifier",
              JavaIdentifierType.LOCAL_VARIABLE_NAME,
              "varName");
      assertEquals("/test/file.java", response.getFilePath());
      assertEquals(SupportedLanguage.JAVA, response.getLanguage());
      assertEquals("identifier", response.getNode());
      assertEquals(JavaIdentifierType.LOCAL_VARIABLE_NAME, response.getNodeType());
      assertEquals("varName", response.getNodeText());
    }

    @Test
    @DisplayName("should handle setter methods correctly")
    void setters_shouldUpdateFieldsCorrectly() {
      GetCursorPositionInfoResponse response = new GetCursorPositionInfoResponse();
      response.setFilePath("/updated/file.java");
      response.setLanguage(SupportedLanguage.JAVA);
      response.setNode("updated_node");
      response.setNodeType(JavaIdentifierType.METHOD_NAME);
      response.setNodeText("updatedText");
      assertEquals("/updated/file.java", response.getFilePath());
      assertEquals(SupportedLanguage.JAVA, response.getLanguage());
      assertEquals("updated_node", response.getNode());
      assertEquals(JavaIdentifierType.METHOD_NAME, response.getNodeType());
      assertEquals("updatedText", response.getNodeText());
    }
  }
}

