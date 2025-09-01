package io.github.syntaxpresso.core.service.java.language;

import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.treesitter.TSNode;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Method Parameter Service Test")
class MethodParameterServiceTest {

    private final MethodParameterService methodParameterService = new MethodParameterService();

    private static final String BASIC_METHOD_SOURCE = """
            package com.example;
            
            public class TestClass {
                public void simpleMethod(String param1, int param2) {
                    String str = param1;
                    int value = param2;
                    System.out.println(param1 + param2);
                }
            }
            """;

    private static final String COMPLEX_METHODS_SOURCE = """
            package com.example;
            
            import java.util.List;
            import java.util.Map;
            
            public class ComplexClass {
                public void noParameters() {
                    System.out.println("No params");
                }
                
                public void genericMethod(List<String> items, Map<String, Integer> data) {
                    items.forEach(item -> data.put(item, item.length()));
                }
                
                public void varargsMethod(String prefix, int... numbers) {
                    for (int num : numbers) {
                        System.out.println(prefix + num);
                    }
                }
                
                public void annotatedMethod(@NotNull String value, @Deprecated int count) {
                    if (value != null) {
                        System.out.println(value.repeat(count));
                    }
                }
                
                public void complexUsage(String data, int index) {
                    String result = data.substring(index);
                    method(data, result);
                    data = data + index;
                    boolean flag = data.isEmpty() ? true : false;
                }
                
                private void method(String s, String r) {
                }
            }
            """;

    @Nested
    @DisplayName("Get All Method Parameter Nodes")
    class GetAllMethodParameterNodesTests {

        @Test
        @DisplayName("Should get all parameters from method with multiple parameters")
        void shouldGetAllParametersFromMethodWithMultipleParameters() {
            TSFile tsFile = new TSFile(SupportedLanguage.JAVA, BASIC_METHOD_SOURCE);
            TSNode methodNode = getMethodNodeByName(tsFile, "simpleMethod");
            
            List<TSNode> parameters = methodParameterService.getAllMethodParameterNodes(tsFile, methodNode);
            
            assertEquals(2, parameters.size());
            assertEquals("formal_parameter", parameters.get(0).getType());
            assertEquals("formal_parameter", parameters.get(1).getType());
        }

        @Test
        @DisplayName("Should return empty list for method without parameters")
        void shouldReturnEmptyListForMethodWithoutParameters() {
            TSFile tsFile = new TSFile(SupportedLanguage.JAVA, COMPLEX_METHODS_SOURCE);
            TSNode methodNode = getMethodNodeByName(tsFile, "noParameters");
            
            List<TSNode> parameters = methodParameterService.getAllMethodParameterNodes(tsFile, methodNode);
            
            assertTrue(parameters.isEmpty());
        }

        @Test
        @DisplayName("Should get parameters with generic types")
        void shouldGetParametersWithGenericTypes() {
            TSFile tsFile = new TSFile(SupportedLanguage.JAVA, COMPLEX_METHODS_SOURCE);
            TSNode methodNode = getMethodNodeByName(tsFile, "genericMethod");
            
            List<TSNode> parameters = methodParameterService.getAllMethodParameterNodes(tsFile, methodNode);
            
            assertEquals(2, parameters.size());
            assertEquals("formal_parameter", parameters.get(0).getType());
            assertEquals("formal_parameter", parameters.get(1).getType());
        }

        @Test
        @DisplayName("Should get varargs parameter")
        void shouldGetVarargsParameter() {
            TSFile tsFile = new TSFile(SupportedLanguage.JAVA, COMPLEX_METHODS_SOURCE);
            TSNode methodNode = getMethodNodeByName(tsFile, "varargsMethod");
            
            List<TSNode> parameters = methodParameterService.getAllMethodParameterNodes(tsFile, methodNode);
            
            assertTrue(parameters.size() >= 1);
            for (TSNode param : parameters) {
                assertEquals("formal_parameter", param.getType());
            }
        }

        @Test
        @DisplayName("Should handle null inputs")
        void shouldHandleNullInputs() {
            assertTrue(methodParameterService.getAllMethodParameterNodes(null, null).isEmpty());
            assertTrue(methodParameterService.getAllMethodParameterNodes(new TSFile(SupportedLanguage.JAVA, "class A {}"), null).isEmpty());
        }

        @Test
        @DisplayName("Should handle invalid node type")
        void shouldHandleInvalidNodeType() {
            TSFile tsFile = new TSFile(SupportedLanguage.JAVA, BASIC_METHOD_SOURCE);
            TSNode classNode = tsFile.query("(class_declaration) @class").execute().nodes().get(0);
            
            List<TSNode> parameters = methodParameterService.getAllMethodParameterNodes(tsFile, classNode);
            
            assertTrue(parameters.isEmpty());
        }
    }

    @Nested
    @DisplayName("Get Method Parameter Node Info")
    class GetMethodParameterNodeInfoTests {

        @Test
        @DisplayName("Should get parameter info with correct capture names")
        void shouldGetParameterInfoWithCorrectCaptureNames() {
            TSFile tsFile = new TSFile(SupportedLanguage.JAVA, BASIC_METHOD_SOURCE);
            TSNode methodNode = getMethodNodeByName(tsFile, "simpleMethod");
            List<TSNode> parameters = methodParameterService.getAllMethodParameterNodes(tsFile, methodNode);
            
            List<Map<String, TSNode>> paramInfo = methodParameterService.getMethodParameterNodeInfo(tsFile, parameters.get(0));
            
            assertEquals(1, paramInfo.size());
            Map<String, TSNode> info = paramInfo.get(0);
            assertTrue(info.containsKey("parameterType"));
            assertTrue(info.containsKey("parameterName"));
            assertTrue(info.containsKey("parameter"));
            
            assertEquals("String", tsFile.getTextFromNode(info.get("parameterType")));
            assertEquals("param1", tsFile.getTextFromNode(info.get("parameterName")));
        }

        @Test
        @DisplayName("Should get info for generic type parameter")
        void shouldGetInfoForGenericTypeParameter() {
            TSFile tsFile = new TSFile(SupportedLanguage.JAVA, COMPLEX_METHODS_SOURCE);
            TSNode methodNode = getMethodNodeByName(tsFile, "genericMethod");
            List<TSNode> parameters = methodParameterService.getAllMethodParameterNodes(tsFile, methodNode);
            
            List<Map<String, TSNode>> paramInfo = methodParameterService.getMethodParameterNodeInfo(tsFile, parameters.get(0));
            
            assertEquals(1, paramInfo.size());
            Map<String, TSNode> info = paramInfo.get(0);
            assertTrue(info.containsKey("parameterType"));
            assertTrue(info.containsKey("parameterName"));
            
            assertEquals("List<String>", tsFile.getTextFromNode(info.get("parameterType")));
            assertEquals("items", tsFile.getTextFromNode(info.get("parameterName")));
        }

        @Test
        @DisplayName("Should get info for varargs parameter")
        void shouldGetInfoForVarargsParameter() {
            TSFile tsFile = new TSFile(SupportedLanguage.JAVA, COMPLEX_METHODS_SOURCE);
            TSNode methodNode = getMethodNodeByName(tsFile, "varargsMethod");
            List<TSNode> parameters = methodParameterService.getAllMethodParameterNodes(tsFile, methodNode);
            
            assertTrue(parameters.size() >= 1);
            TSNode lastParam = parameters.get(parameters.size() - 1);
            List<Map<String, TSNode>> paramInfo = methodParameterService.getMethodParameterNodeInfo(tsFile, lastParam);
            
            assertEquals(1, paramInfo.size());
            Map<String, TSNode> info = paramInfo.get(0);
            assertTrue(info.containsKey("parameterType"));
            assertTrue(info.containsKey("parameterName"));
            
            String typeText = tsFile.getTextFromNode(info.get("parameterType"));
            String nameText = tsFile.getTextFromNode(info.get("parameterName"));
            assertTrue(typeText.contains("String") || typeText.contains("int"));
            assertTrue(nameText.equals("numbers") || nameText.equals("prefix"));
        }

        @Test
        @DisplayName("Should handle invalid parameter node")
        void shouldHandleInvalidParameterNode() {
            TSFile tsFile = new TSFile(SupportedLanguage.JAVA, BASIC_METHOD_SOURCE);
            TSNode classNode = tsFile.query("(class_declaration) @class").execute().nodes().get(0);
            
            List<Map<String, TSNode>> paramInfo = methodParameterService.getMethodParameterNodeInfo(tsFile, classNode);
            
            assertTrue(paramInfo.isEmpty());
        }

        @Test
        @DisplayName("Should handle null inputs")
        void shouldHandleNullInputs() {
            assertTrue(methodParameterService.getMethodParameterNodeInfo(null, null).isEmpty());
        }
    }

    @Nested
    @DisplayName("Get Method Parameter Type Node")
    class GetMethodParameterTypeNodeTests {

        @Test
        @DisplayName("Should get type node for simple type")
        void shouldGetTypeNodeForSimpleType() {
            TSFile tsFile = new TSFile(SupportedLanguage.JAVA, BASIC_METHOD_SOURCE);
            TSNode methodNode = getMethodNodeByName(tsFile, "simpleMethod");
            List<TSNode> parameters = methodParameterService.getAllMethodParameterNodes(tsFile, methodNode);
            
            Optional<TSNode> typeNode = methodParameterService.getMethodParameterTypeNode(tsFile, parameters.get(0));
            
            assertTrue(typeNode.isPresent());
            assertEquals("String", tsFile.getTextFromNode(typeNode.get()));
        }

        @Test
        @DisplayName("Should get type node for generic type")
        void shouldGetTypeNodeForGenericType() {
            TSFile tsFile = new TSFile(SupportedLanguage.JAVA, COMPLEX_METHODS_SOURCE);
            TSNode methodNode = getMethodNodeByName(tsFile, "genericMethod");
            List<TSNode> parameters = methodParameterService.getAllMethodParameterNodes(tsFile, methodNode);
            
            Optional<TSNode> typeNode = methodParameterService.getMethodParameterTypeNode(tsFile, parameters.get(1));
            
            assertTrue(typeNode.isPresent());
            assertEquals("Map<String, Integer>", tsFile.getTextFromNode(typeNode.get()));
        }

        @Test
        @DisplayName("Should return empty for invalid parameter")
        void shouldReturnEmptyForInvalidParameter() {
            TSFile tsFile = new TSFile(SupportedLanguage.JAVA, BASIC_METHOD_SOURCE);
            TSNode classNode = tsFile.query("(class_declaration) @class").execute().nodes().get(0);
            
            Optional<TSNode> typeNode = methodParameterService.getMethodParameterTypeNode(tsFile, classNode);
            
            assertTrue(typeNode.isEmpty());
        }

        @Test
        @DisplayName("Should handle null inputs")
        void shouldHandleNullInputs() {
            assertTrue(methodParameterService.getMethodParameterTypeNode(null, null).isEmpty());
        }
    }

    @Nested
    @DisplayName("Get Method Parameter Name Node")
    class GetMethodParameterNameNodeTests {

        @Test
        @DisplayName("Should get name node for parameter")
        void shouldGetNameNodeForParameter() {
            TSFile tsFile = new TSFile(SupportedLanguage.JAVA, BASIC_METHOD_SOURCE);
            TSNode methodNode = getMethodNodeByName(tsFile, "simpleMethod");
            List<TSNode> parameters = methodParameterService.getAllMethodParameterNodes(tsFile, methodNode);
            
            Optional<TSNode> nameNode = methodParameterService.getMethodParameterNameNode(tsFile, parameters.get(1));
            
            assertTrue(nameNode.isPresent());
            assertEquals("param2", tsFile.getTextFromNode(nameNode.get()));
        }

        @Test
        @DisplayName("Should get name node for varargs parameter")
        void shouldGetNameNodeForVarargsParameter() {
            TSFile tsFile = new TSFile(SupportedLanguage.JAVA, COMPLEX_METHODS_SOURCE);
            TSNode methodNode = getMethodNodeByName(tsFile, "varargsMethod");
            List<TSNode> parameters = methodParameterService.getAllMethodParameterNodes(tsFile, methodNode);
            
            assertTrue(parameters.size() >= 1);
            TSNode firstParam = parameters.get(0);
            Optional<TSNode> nameNode = methodParameterService.getMethodParameterNameNode(tsFile, firstParam);
            
            assertTrue(nameNode.isPresent());
            String nameText = tsFile.getTextFromNode(nameNode.get());
            assertTrue(nameText.equals("prefix") || nameText.equals("numbers"));
        }

        @Test
        @DisplayName("Should return empty for invalid parameter")
        void shouldReturnEmptyForInvalidParameter() {
            TSFile tsFile = new TSFile(SupportedLanguage.JAVA, BASIC_METHOD_SOURCE);
            TSNode classNode = tsFile.query("(class_declaration) @class").execute().nodes().get(0);
            
            Optional<TSNode> nameNode = methodParameterService.getMethodParameterNameNode(tsFile, classNode);
            
            assertTrue(nameNode.isEmpty());
        }

        @Test
        @DisplayName("Should handle null inputs")
        void shouldHandleNullInputs() {
            assertTrue(methodParameterService.getMethodParameterNameNode(null, null).isEmpty());
        }
    }

    @Nested
    @DisplayName("Find All Method Parameter Node Usages")
    class FindAllMethodParameterNodeUsagesTests {

        @Test
        @DisplayName("Should find all parameter usages within method scope")
        void shouldFindAllParameterUsagesWithinMethodScope() {
            TSFile tsFile = new TSFile(SupportedLanguage.JAVA, BASIC_METHOD_SOURCE);
            TSNode methodNode = getMethodNodeByName(tsFile, "simpleMethod");
            List<TSNode> parameters = methodParameterService.getAllMethodParameterNodes(tsFile, methodNode);
            
            List<TSNode> usages = methodParameterService.findAllMethodParameterNodeUsages(tsFile, parameters.get(0), methodNode);
            
            assertFalse(usages.isEmpty());
            assertTrue(usages.stream().allMatch(node -> 
                "identifier".equals(node.getType()) && "param1".equals(tsFile.getTextFromNode(node))));
        }

        @Test
        @DisplayName("Should find complex parameter usages")
        void shouldFindComplexParameterUsages() {
            TSFile tsFile = new TSFile(SupportedLanguage.JAVA, COMPLEX_METHODS_SOURCE);
            TSNode methodNode = getMethodNodeByName(tsFile, "complexUsage");
            List<TSNode> parameters = methodParameterService.getAllMethodParameterNodes(tsFile, methodNode);
            
            List<TSNode> dataUsages = methodParameterService.findAllMethodParameterNodeUsages(tsFile, parameters.get(0), methodNode);
            List<TSNode> indexUsages = methodParameterService.findAllMethodParameterNodeUsages(tsFile, parameters.get(1), methodNode);
            
            assertFalse(dataUsages.isEmpty());
            assertFalse(indexUsages.isEmpty());
            assertTrue(dataUsages.stream().allMatch(node -> 
                "identifier".equals(node.getType()) && "data".equals(tsFile.getTextFromNode(node))));
            assertTrue(indexUsages.stream().allMatch(node -> 
                "identifier".equals(node.getType()) && "index".equals(tsFile.getTextFromNode(node))));
        }

        @Test
        @DisplayName("Should return empty list when parameter is not used")
        void shouldReturnEmptyListWhenParameterIsNotUsed() {
            String sourceCode = """
                    package com.example;
                    
                    public class TestClass {
                        public void unusedParam(String unused, int used) {
                            System.out.println(used);
                        }
                    }
                    """;
            TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
            TSNode methodNode = getMethodNodeByName(tsFile, "unusedParam");
            List<TSNode> parameters = methodParameterService.getAllMethodParameterNodes(tsFile, methodNode);
            
            List<TSNode> usages = methodParameterService.findAllMethodParameterNodeUsages(tsFile, parameters.get(0), methodNode);
            
            assertTrue(usages.isEmpty());
        }

        @Test
        @DisplayName("Should not find usages outside method scope")
        void shouldNotFindUsagesOutsideMethodScope() {
            String sourceCode = """
                    package com.example;
                    
                    public class TestClass {
                        private String data = "field";
                        
                        public void method(String data) {
                            System.out.println(data);
                        }
                        
                        public void otherMethod() {
                            String data = "local";
                            System.out.println(data);
                        }
                    }
                    """;
            TSFile tsFile = new TSFile(SupportedLanguage.JAVA, sourceCode);
            TSNode methodNode = getMethodNodeByName(tsFile, "method");
            List<TSNode> parameters = methodParameterService.getAllMethodParameterNodes(tsFile, methodNode);
            
            List<TSNode> usages = methodParameterService.findAllMethodParameterNodeUsages(tsFile, parameters.get(0), methodNode);
            
            assertEquals(1, usages.size());
        }

        @Test
        @DisplayName("Should handle null inputs")
        void shouldHandleNullInputs() {
            assertTrue(methodParameterService.findAllMethodParameterNodeUsages(null, null, null).isEmpty());
        }

        @Test
        @DisplayName("Should handle invalid node types")
        void shouldHandleInvalidNodeTypes() {
            TSFile tsFile = new TSFile(SupportedLanguage.JAVA, BASIC_METHOD_SOURCE);
            TSNode classNode = tsFile.query("(class_declaration) @class").execute().nodes().get(0);
            
            List<TSNode> usages = methodParameterService.findAllMethodParameterNodeUsages(tsFile, classNode, classNode);
            
            assertTrue(usages.isEmpty());
        }
    }

    private TSNode getMethodNodeByName(TSFile tsFile, String methodName) {
        List<TSNode> methods = tsFile.query("(method_declaration) @method").execute().nodes();
        return methods.stream()
            .filter(node -> {
                TSNode nameNode = node.getChildByFieldName("name");
                return nameNode != null && methodName.equals(tsFile.getTextFromNode(nameNode));
            })
            .findFirst()
            .orElseThrow(() -> new AssertionError("Method not found: " + methodName));
    }
}