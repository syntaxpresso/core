package io.github.syntaxpresso.core.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.syntaxpresso.core.command.dto.GetCursorPositionInfoResponse;
import io.github.syntaxpresso.core.common.DataTransferObject;
import io.github.syntaxpresso.core.common.extra.SupportedIDE;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import io.github.syntaxpresso.core.service.extra.JavaIdentifierType;
import io.github.syntaxpresso.core.service.java.JavaCommandService;
import io.github.syntaxpresso.core.service.java.JavaLanguageService;
import io.github.syntaxpresso.core.service.java.language.ClassDeclarationService;
import io.github.syntaxpresso.core.service.java.language.FieldDeclarationService;
import io.github.syntaxpresso.core.service.java.language.FormalParameterDeclarationService;
import io.github.syntaxpresso.core.service.java.language.ImportDeclarationService;
import io.github.syntaxpresso.core.service.java.language.LocalVariableDeclarationService;
import io.github.syntaxpresso.core.service.java.language.MethodDeclarationService;
import io.github.syntaxpresso.core.service.java.language.PackageDeclarationService;
import io.github.syntaxpresso.core.service.java.language.VariableNamingService;
import io.github.syntaxpresso.core.util.PathHelper;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("GetCursorPositionInfo Tests")
class GetCursorPositionInfoTest {
  private JavaCommandService javaService;
  private GetCursorPositionInfo command;
  private Path testJavaFile;
  private Integer line;
  private Integer column;

  @TempDir private Path tempDir;

  @BeforeEach
  void setUp() throws Exception {
    PathHelper pathHelper = new PathHelper();
    VariableNamingService variableNamingService = new VariableNamingService();
    FieldDeclarationService fieldDeclarationService = new FieldDeclarationService();
    FormalParameterDeclarationService formalParameterDeclarationService =
        new FormalParameterDeclarationService();
    MethodDeclarationService methodDeclarationService =
        new MethodDeclarationService(formalParameterDeclarationService);
    ClassDeclarationService classDeclarationService =
        new ClassDeclarationService(fieldDeclarationService, methodDeclarationService);
    PackageDeclarationService packageDeclarationService = new PackageDeclarationService();
    ImportDeclarationService importDeclarationService = new ImportDeclarationService();
    LocalVariableDeclarationService localVariableDeclarationService =
        new LocalVariableDeclarationService();

    JavaLanguageService javaLanguageService =
        new JavaLanguageService(
            pathHelper,
            variableNamingService,
            classDeclarationService,
            packageDeclarationService,
            importDeclarationService,
            localVariableDeclarationService);

    javaService = new JavaCommandService(pathHelper, javaLanguageService);
    command = new GetCursorPositionInfo(javaService);

    String javaCode =
        """
        package com.example;

        public class User {
            private String name;
            private int age;

            public String getName() {
                return this.name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public void processUser(User user) {
                String localVar = "test";
                System.out.println(localVar);
            }
        }
        """;

    testJavaFile = tempDir.resolve("User.java");
    Files.write(testJavaFile, javaCode.getBytes());

    line = 5;
    column = 10;

    // Set required fields using reflection
    Field filePathField = GetCursorPositionInfo.class.getDeclaredField("filePath");
    filePathField.setAccessible(true);
    filePathField.set(command, testJavaFile);
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

  private void setFieldsForPosition(int line, int column) throws Exception {
    Field lineField = GetCursorPositionInfo.class.getDeclaredField("line");
    lineField.setAccessible(true);
    lineField.set(command, line);
    Field columnField = GetCursorPositionInfo.class.getDeclaredField("column");
    columnField.setAccessible(true);
    columnField.set(command, column);
  }

  private void setFilePathField(Path filePath) throws Exception {
    Field filePathField = GetCursorPositionInfo.class.getDeclaredField("filePath");
    filePathField.setAccessible(true);
    filePathField.set(command, filePath);
  }

  @Nested
  @DisplayName("Command Execution Tests")
  class CommandExecutionTests {
    @Test
    @DisplayName("should extract class name from cursor position")
    void call_shouldExtractClassNameFromCursorPosition() throws Exception {
      setFieldsForPosition(3, 14);
      DataTransferObject<GetCursorPositionInfoResponse> result = command.call();
      assertTrue(result.getSucceed());
      assertNotNull(result.getData());
      assertEquals(testJavaFile.toString(), result.getData().getFilePath());
      assertEquals(SupportedLanguage.JAVA, result.getData().getLanguage());
      assertEquals(JavaIdentifierType.CLASS_NAME, result.getData().getNodeType());
      assertEquals("User", result.getData().getNodeText());
    }

    @Test
    @DisplayName("should return error for non-existent file")
    void call_shouldReturnErrorForNonExistentFile() throws Exception {
      Path nonExistentFile = tempDir.resolve("NonExistent.java");
      setFilePathField(nonExistentFile);
      DataTransferObject<GetCursorPositionInfoResponse> result = command.call();
      assertFalse(result.getSucceed());
      assertTrue(result.getErrorReason().contains("File does not exist"));
    }

    @Test
    @DisplayName("should extract field name from cursor position")
    void call_shouldExtractFieldName() throws Exception {
      setFieldsForPosition(4, 20);
      DataTransferObject<GetCursorPositionInfoResponse> result = command.call();
      assertTrue(result.getSucceed());
      assertEquals(JavaIdentifierType.FIELD_NAME, result.getData().getNodeType());
      assertEquals("name", result.getData().getNodeText());
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
      setFieldsForPosition(3, 14);
      DataTransferObject<GetCursorPositionInfoResponse> result = command.call();
      assertTrue(result.getSucceed());
      assertEquals(SupportedLanguage.JAVA, result.getData().getLanguage());
      assertEquals(JavaIdentifierType.CLASS_NAME, result.getData().getNodeType());
      assertEquals("User", result.getData().getNodeText());
    }

    @Test
    @DisplayName("should handle METHOD_NAME nodeType correctly")
    void call_shouldHandleMethodNameNodeType() throws Exception {
      setFieldsForPosition(7, 19);
      DataTransferObject<GetCursorPositionInfoResponse> result = command.call();
      assertTrue(result.getSucceed());
      assertEquals(SupportedLanguage.JAVA, result.getData().getLanguage());
      assertEquals(JavaIdentifierType.METHOD_NAME, result.getData().getNodeType());
      assertEquals("getName", result.getData().getNodeText());
    }

    @Test
    @DisplayName("should handle FIELD_NAME nodeType correctly")
    void call_shouldHandleFieldNameNodeType() throws Exception {
      setFieldsForPosition(4, 20);
      DataTransferObject<GetCursorPositionInfoResponse> result = command.call();
      assertTrue(result.getSucceed());
      assertEquals(SupportedLanguage.JAVA, result.getData().getLanguage());
      assertEquals(JavaIdentifierType.FIELD_NAME, result.getData().getNodeType());
      assertEquals("name", result.getData().getNodeText());
    }

    @Test
    @DisplayName("should handle LOCAL_VARIABLE_NAME nodeType correctly")
    void call_shouldHandleLocalVariableNameNodeType() throws Exception {
      setFieldsForPosition(16, 16);
      DataTransferObject<GetCursorPositionInfoResponse> result = command.call();
      assertTrue(result.getSucceed());
      assertEquals(SupportedLanguage.JAVA, result.getData().getLanguage());
      assertEquals(JavaIdentifierType.LOCAL_VARIABLE_NAME, result.getData().getNodeType());
      assertEquals("localVar", result.getData().getNodeText());
    }

    @Test
    @DisplayName("should handle FORMAL_PARAMETER_NAME nodeType correctly")
    void call_shouldHandleFormalParameterNameNodeType() throws Exception {
      setFieldsForPosition(11, 33);
      DataTransferObject<GetCursorPositionInfoResponse> result = command.call();
      assertTrue(result.getSucceed());
      assertEquals(SupportedLanguage.JAVA, result.getData().getLanguage());
      assertEquals(JavaIdentifierType.FORMAL_PARAMETER_NAME, result.getData().getNodeType());
      assertEquals("name", result.getData().getNodeText());
    }

    @Test
    @DisplayName("should return error for position without identifier")
    void call_shouldReturnErrorForInvalidPosition() throws Exception {
      setFieldsForPosition(1, 1);
      DataTransferObject<GetCursorPositionInfoResponse> result = command.call();
      assertFalse(result.getSucceed());
      assertTrue(result.getErrorReason().contains("Unable to determine symbol type"));
    }
  }

  @Nested
  @DisplayName("Complete Response Structure Tests")
  class CompleteResponseStructureTests {
    @Test
    @DisplayName("should validate all fields are populated correctly")
    void call_shouldValidateAllFieldsPopulated() throws Exception {
      setFieldsForPosition(3, 14);
      DataTransferObject<GetCursorPositionInfoResponse> result = command.call();
      assertTrue(result.getSucceed());
      GetCursorPositionInfoResponse data = result.getData();
      assertNotNull(data);
      assertEquals(testJavaFile.toString(), data.getFilePath());
      assertEquals(SupportedLanguage.JAVA, data.getLanguage());
      assertNotNull(data.getNode());
      assertEquals(JavaIdentifierType.CLASS_NAME, data.getNodeType());
      assertEquals("User", data.getNodeText());
    }

    @Test
    @DisplayName("should handle error for non-Java file")
    void call_shouldHandleNonJavaFile() throws Exception {
      Path nonJavaFile = tempDir.resolve("test.txt");
      Files.write(nonJavaFile, "not java code".getBytes());
      setFilePathField(nonJavaFile);
      DataTransferObject<GetCursorPositionInfoResponse> result = command.call();
      assertFalse(result.getSucceed());
      assertTrue(result.getErrorReason().contains("not a .java file"));
    }
  }

  @Nested
  @DisplayName("JSON Serialization Tests")
  class JsonSerializationTests {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("should serialize response with all fields to JSON correctly")
    void serialization_shouldHandleCompleteResponse() throws Exception {
      setFieldsForPosition(3, 14);
      DataTransferObject<GetCursorPositionInfoResponse> result = command.call();
      assertTrue(result.getSucceed());
      String json = objectMapper.writeValueAsString(result);
      assertNotNull(json);
      assertTrue(json.contains("\"language\":\"JAVA\""));
      assertTrue(json.contains("\"nodeType\":\"CLASS_NAME\""));
      assertTrue(json.contains("\"nodeText\":\"User\""));
      assertTrue(json.contains("\"succeed\":true"));
    }

    @Test
    @DisplayName("should serialize and deserialize response correctly")
    void serialization_shouldRoundTripCorrectly() throws Exception {
      setFieldsForPosition(3, 14);
      DataTransferObject<GetCursorPositionInfoResponse> originalDto = command.call();
      assertTrue(originalDto.getSucceed());
      String json = objectMapper.writeValueAsString(originalDto);
      @SuppressWarnings("unchecked")
      DataTransferObject<GetCursorPositionInfoResponse> deserializedDto =
          objectMapper.readValue(json, DataTransferObject.class);
      assertNotNull(deserializedDto);
      assertTrue(deserializedDto.getSucceed());
    }

    @Test
    @DisplayName("should serialize language field correctly")
    void serialization_shouldHandleLanguageField() throws Exception {
      setFieldsForPosition(3, 14);
      DataTransferObject<GetCursorPositionInfoResponse> result = command.call();
      assertTrue(result.getSucceed());
      String json = objectMapper.writeValueAsString(result);
      assertNotNull(json);
      assertTrue(json.contains("\"language\":\"JAVA\""));
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
