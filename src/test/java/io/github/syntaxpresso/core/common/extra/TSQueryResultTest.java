package io.github.syntaxpresso.core.common.extra;

import static org.junit.jupiter.api.Assertions.*;

import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import java.util.*;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.treesitter.TSNode;

@DisplayName("TSQueryResult Tests")
class TSQueryResultTest {

  private TSFile tsFile;
  private List<TSNode> testNodes;
  private List<Map<String, TSNode>> testCaptures;

  private static final String JAVA_CODE = """
      package com.example;
      
      public class TestClass {
          private String name;
          private int age;
          
          public String getName() {
              return this.name;
          }
          
          public void setName(String name) {
              this.name = name;
          }
          
          public int getAge() {
              return this.age;
          }
      }
      """;

  @BeforeEach
  void setUp() {
    tsFile = new TSFile(SupportedLanguage.JAVA, JAVA_CODE);
    setupTestData();
  }

  private void setupTestData() {
    TSNode rootNode = tsFile.getTree().getRootNode();
    
    // Setup test nodes
    testNodes = new ArrayList<>();
    TSNode classNode = findNodeByType(rootNode, "class_declaration");
    TSNode getNameMethod = findMethodByName("getName");
    TSNode setNameMethod = findMethodByName("setName");
    
    testNodes.add(classNode);
    testNodes.add(getNameMethod);
    testNodes.add(setNameMethod);
    
    // Setup test captures
    testCaptures = new ArrayList<>();
    Map<String, TSNode> capture1 = new HashMap<>();
    capture1.put("method", getNameMethod);
    capture1.put("name", findNodeByType(getNameMethod, "identifier"));
    
    Map<String, TSNode> capture2 = new HashMap<>();
    capture2.put("method", setNameMethod);
    capture2.put("name", findNodeByType(setNameMethod, "identifier"));
    
    testCaptures.add(capture1);
    testCaptures.add(capture2);
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Should create result from capture maps")
    void shouldCreateResultFromCaptureMaps() {
      TSQueryResult result = new TSQueryResult(testCaptures);
      
      assertNotNull(result);
      assertEquals(2, result.size());
      assertFalse(result.isEmpty());
      assertTrue(result.hasResults());
    }

    @Test
    @DisplayName("Should create result from nodes list")
    void shouldCreateResultFromNodesList() {
      TSQueryResult result = new TSQueryResult(testNodes, "method");
      
      assertNotNull(result);
      assertEquals(3, result.size());
      assertFalse(result.isEmpty());
    }

    @Test
    @DisplayName("Should handle null captures list")
    void shouldHandleNullCapturesList() {
      TSQueryResult result = new TSQueryResult((List<Map<String, TSNode>>) null);
      
      assertNotNull(result);
      assertEquals(0, result.size());
      assertTrue(result.isEmpty());
      assertFalse(result.hasResults());
    }

    @Test
    @DisplayName("Should handle empty collections")
    void shouldHandleEmptyCollections() {
      TSQueryResult emptyCaptures = new TSQueryResult(new ArrayList<>());
      TSQueryResult emptyNodes = new TSQueryResult(new ArrayList<>(), "test");
      
      assertTrue(emptyCaptures.isEmpty());
      assertTrue(emptyNodes.isEmpty());
      assertEquals(0, emptyCaptures.size());
      assertEquals(0, emptyNodes.size());
    }
  }

  @Nested
  @DisplayName("Node Access Tests")
  class NodeAccessTests {

    @Test
    @DisplayName("Should get all nodes from single capture result")
    void shouldGetAllNodesFromSingleCaptureResult() {
      TSQueryResult result = new TSQueryResult(testNodes, "method");
      
      List<TSNode> nodes = result.nodes();
      
      assertEquals(3, nodes.size());
      assertTrue(nodes.containsAll(testNodes));
    }

    @Test
    @DisplayName("Should get all unique nodes from multi-capture result")
    void shouldGetAllUniqueNodesFromMultiCaptureResult() {
      TSQueryResult result = new TSQueryResult(testCaptures);
      
      List<TSNode> nodes = result.nodes();
      
      assertFalse(nodes.isEmpty());
      // Should be sorted by start byte
      for (int i = 1; i < nodes.size(); i++) {
        assertTrue(nodes.get(i - 1).getStartByte() <= nodes.get(i).getStartByte());
      }
    }

    @Test
    @DisplayName("Should get first node")
    void shouldGetFirstNode() {
      TSQueryResult result = new TSQueryResult(testNodes, "method");
      
      TSNode firstNode = result.firstNode();
      
      assertNotNull(firstNode);
      assertEquals(testNodes.get(0), firstNode);
    }

    @Test
    @DisplayName("Should return null for first node when empty")
    void shouldReturnNullForFirstNodeWhenEmpty() {
      TSQueryResult result = new TSQueryResult(new ArrayList<>(), "test");
      
      TSNode firstNode = result.firstNode();
      
      assertNull(firstNode);
    }

    @Test
    @DisplayName("Should get first node as Optional")
    void shouldGetFirstNodeAsOptional() {
      TSQueryResult result = new TSQueryResult(testNodes, "method");
      
      Optional<TSNode> firstNode = result.firstNodeOptional();
      
      assertTrue(firstNode.isPresent());
      assertEquals(testNodes.get(0), firstNode.get());
    }

    @Test
    @DisplayName("Should return empty Optional when no nodes")
    void shouldReturnEmptyOptionalWhenNoNodes() {
      TSQueryResult result = new TSQueryResult(new ArrayList<>(), "test");
      
      Optional<TSNode> firstNode = result.firstNodeOptional();
      
      assertFalse(firstNode.isPresent());
    }

    @Test
    @DisplayName("Should get single node")
    void shouldGetSingleNode() {
      List<TSNode> singleNode = Arrays.asList(testNodes.get(0));
      TSQueryResult result = new TSQueryResult(singleNode, "method");
      
      TSNode node = result.singleNode();
      
      assertEquals(testNodes.get(0), node);
    }

    @Test
    @DisplayName("Should throw exception for single node when empty")
    void shouldThrowExceptionForSingleNodeWhenEmpty() {
      TSQueryResult result = new TSQueryResult(new ArrayList<>(), "test");
      
      assertThrows(IllegalStateException.class, result::singleNode);
    }

    @Test
    @DisplayName("Should throw exception for single node when multiple")
    void shouldThrowExceptionForSingleNodeWhenMultiple() {
      TSQueryResult result = new TSQueryResult(testNodes, "method");
      
      IllegalStateException exception = assertThrows(IllegalStateException.class, result::singleNode);
      assertTrue(exception.getMessage().contains("3 results"));
    }
  }

  @Nested
  @DisplayName("Capture Access Tests")
  class CaptureAccessTests {

    @Test
    @DisplayName("Should get all captures")
    void shouldGetAllCaptures() {
      TSQueryResult result = new TSQueryResult(testCaptures);
      
      List<Map<String, TSNode>> captures = result.captures();
      
      assertEquals(2, captures.size());
      assertEquals(testCaptures, captures);
    }

    @Test
    @DisplayName("Should get first capture")
    void shouldGetFirstCapture() {
      TSQueryResult result = new TSQueryResult(testCaptures);
      
      Map<String, TSNode> firstCapture = result.firstCapture();
      
      assertNotNull(firstCapture);
      assertEquals(testCaptures.get(0), firstCapture);
    }

    @Test
    @DisplayName("Should return empty map for first capture when empty")
    void shouldReturnEmptyMapForFirstCaptureWhenEmpty() {
      TSQueryResult result = new TSQueryResult(new ArrayList<>());
      
      Map<String, TSNode> firstCapture = result.firstCapture();
      
      assertTrue(firstCapture.isEmpty());
    }

    @Test
    @DisplayName("Should get first capture as Optional")
    void shouldGetFirstCaptureAsOptional() {
      TSQueryResult result = new TSQueryResult(testCaptures);
      
      Optional<Map<String, TSNode>> firstCapture = result.firstCaptureOptional();
      
      assertTrue(firstCapture.isPresent());
      assertEquals(testCaptures.get(0), firstCapture.get());
    }

    @Test
    @DisplayName("Should get single capture")
    void shouldGetSingleCapture() {
      List<Map<String, TSNode>> singleCapture = Arrays.asList(testCaptures.get(0));
      TSQueryResult result = new TSQueryResult(singleCapture);
      
      Map<String, TSNode> capture = result.singleCapture();
      
      assertEquals(testCaptures.get(0), capture);
    }

    @Test
    @DisplayName("Should throw exception for single capture when empty")
    void shouldThrowExceptionForSingleCaptureWhenEmpty() {
      TSQueryResult result = new TSQueryResult(new ArrayList<>());
      
      assertThrows(IllegalStateException.class, result::singleCapture);
    }

    @Test
    @DisplayName("Should throw exception for single capture when multiple")
    void shouldThrowExceptionForSingleCaptureWhenMultiple() {
      TSQueryResult result = new TSQueryResult(testCaptures);
      
      IllegalStateException exception = assertThrows(IllegalStateException.class, result::singleCapture);
      assertTrue(exception.getMessage().contains("2 results"));
    }
  }

  @Nested
  @DisplayName("Specific Capture Access Tests")
  class SpecificCaptureAccessTests {

    @Test
    @DisplayName("Should get nodes from specific capture")
    void shouldGetNodesFromSpecificCapture() {
      TSQueryResult result = new TSQueryResult(testCaptures);
      
      List<TSNode> nameNodes = result.nodesFrom("name");
      
      assertEquals(2, nameNodes.size());
      assertNotNull(nameNodes.get(0));
      assertNotNull(nameNodes.get(1));
    }

    @Test
    @DisplayName("Should return empty list for non-existent capture")
    void shouldReturnEmptyListForNonExistentCapture() {
      TSQueryResult result = new TSQueryResult(testCaptures);
      
      List<TSNode> nodes = result.nodesFrom("nonexistent");
      
      assertTrue(nodes.isEmpty());
    }

    @Test
    @DisplayName("Should get first node from specific capture")
    void shouldGetFirstNodeFromSpecificCapture() {
      TSQueryResult result = new TSQueryResult(testCaptures);
      
      TSNode firstNameNode = result.firstNodeFrom("name");
      
      assertNotNull(firstNameNode);
      assertEquals(testCaptures.get(0).get("name"), firstNameNode);
    }

    @Test
    @DisplayName("Should return null for first node from non-existent capture")
    void shouldReturnNullForFirstNodeFromNonExistentCapture() {
      TSQueryResult result = new TSQueryResult(testCaptures);
      
      TSNode node = result.firstNodeFrom("nonexistent");
      
      assertNull(node);
    }
  }

  @Nested
  @DisplayName("Stream Operations Tests")
  class StreamOperationsTests {

    @Test
    @DisplayName("Should create stream of nodes")
    void shouldCreateStreamOfNodes() {
      TSQueryResult result = new TSQueryResult(testNodes, "method");
      
      List<TSNode> streamedNodes = result.streamNodes()
          .collect(Collectors.toList());
      
      assertEquals(testNodes.size(), streamedNodes.size());
      assertTrue(streamedNodes.containsAll(testNodes));
    }

    @Test
    @DisplayName("Should create stream of captures")
    void shouldCreateStreamOfCaptures() {
      TSQueryResult result = new TSQueryResult(testCaptures);
      
      List<Map<String, TSNode>> streamedCaptures = result.streamCaptures()
          .collect(Collectors.toList());
      
      assertEquals(testCaptures.size(), streamedCaptures.size());
      assertEquals(testCaptures, streamedCaptures);
    }

    @Test
    @DisplayName("Should apply forEach to captures")
    void shouldApplyForEachToCaptures() {
      TSQueryResult result = new TSQueryResult(testCaptures);
      List<String> captureKeys = new ArrayList<>();
      
      result.forEachCapture(capture -> captureKeys.addAll(capture.keySet()));
      
      assertFalse(captureKeys.isEmpty());
      assertTrue(captureKeys.contains("method"));
      assertTrue(captureKeys.contains("name"));
    }

    @Test
    @DisplayName("Should apply forEach to nodes")
    void shouldApplyForEachToNodes() {
      TSQueryResult result = new TSQueryResult(testNodes, "method");
      List<TSNode> processedNodes = new ArrayList<>();
      
      result.forEachNode(processedNodes::add);
      
      assertEquals(testNodes.size(), processedNodes.size());
      assertTrue(processedNodes.containsAll(testNodes));
    }
  }

  @Nested
  @DisplayName("Filtering Tests")
  class FilteringTests {

    @Test
    @DisplayName("Should filter matches by predicate")
    void shouldFilterMatchesByPredicate() {
      TSQueryResult result = new TSQueryResult(testCaptures);
      
      TSQueryResult filtered = result.filter(capture -> 
          capture.containsKey("method") && 
          tsFile.getTextFromNode(capture.get("name")).startsWith("get"));
      
      assertEquals(1, filtered.size());
    }

    @Test
    @DisplayName("Should filter by specific capture")
    void shouldFilterBySpecificCapture() {
      TSQueryResult result = new TSQueryResult(testCaptures);
      
      TSQueryResult filtered = result.filterByCapture("name", node ->
          tsFile.getTextFromNode(node).equals("getName"));
      
      assertEquals(1, filtered.size());
    }

    @Test
    @DisplayName("Should handle filter with no matches")
    void shouldHandleFilterWithNoMatches() {
      TSQueryResult result = new TSQueryResult(testCaptures);
      
      TSQueryResult filtered = result.filter(capture -> false);
      
      assertTrue(filtered.isEmpty());
    }
  }

  @Nested
  @DisplayName("Mapping Tests")
  class MappingTests {

    @Test
    @DisplayName("Should map captures to string list")
    void shouldMapCapturesToStringList() {
      TSQueryResult result = new TSQueryResult(testCaptures);
      
      List<String> methodNames = result.map(capture -> {
        TSNode nameNode = capture.get("name");
        return nameNode != null ? tsFile.getTextFromNode(nameNode) : "";
      });
      
      assertEquals(2, methodNames.size());
      assertTrue(methodNames.contains("getName"));
      assertTrue(methodNames.contains("setName"));
    }

    @Test
    @DisplayName("Should map nodes to string list")
    void shouldMapNodesToStringList() {
      TSQueryResult result = new TSQueryResult(testCaptures);
      List<TSNode> nameNodes = result.nodesFrom("name");
      TSQueryResult nameResult = new TSQueryResult(nameNodes, "name");
      
      List<String> nodeTexts = nameResult.mapNodes(node -> 
          tsFile.getTextFromNode(node));
      
      assertEquals(2, nodeTexts.size());
      assertTrue(nodeTexts.contains("getName"));
      assertTrue(nodeTexts.contains("setName"));
    }

    @Test
    @DisplayName("Should collect nodes with custom collector")
    void shouldCollectNodesWithCustomCollector() {
      TSQueryResult result = new TSQueryResult(testNodes, "method");
      
      String concatenated = result.mapNodes(node -> node.getType())
          .stream()
          .collect(Collectors.joining(", ", "[", "]"));
      
      assertNotNull(concatenated);
      assertTrue(concatenated.startsWith("["));
      assertTrue(concatenated.endsWith("]"));
    }

    @Test
    @DisplayName("Should collect captures with custom collector")
    void shouldCollectCapturesWithCustomCollector() {
      TSQueryResult result = new TSQueryResult(testCaptures);
      
      Map<String, Long> captureCount = result.collectCaptures(
          Collectors.groupingBy(
              capture -> capture.containsKey("method") ? "has_method" : "no_method",
              Collectors.counting()));
      
      assertEquals(1, captureCount.size());
      assertEquals(2L, captureCount.get("has_method"));
    }
  }

  @Nested
  @DisplayName("Edge Cases Tests")
  class EdgeCasesTests {

    @Test
    @DisplayName("Should handle mixed null values in captures")
    void shouldHandleMixedNullValuesInCaptures() {
      Map<String, TSNode> captureWithNull = new HashMap<>();
      captureWithNull.put("method", testNodes.get(0));
      captureWithNull.put("name", null);
      
      List<Map<String, TSNode>> capturesWithNull = Arrays.asList(captureWithNull);
      TSQueryResult result = new TSQueryResult(capturesWithNull);
      
      List<TSNode> nameNodes = result.nodesFrom("name");
      assertTrue(nameNodes.isEmpty());
      
      List<TSNode> methodNodes = result.nodesFrom("method");
      assertEquals(1, methodNodes.size());
    }

    @Test
    @DisplayName("Should handle empty capture maps")
    void shouldHandleEmptyCaptureMap() {
      List<Map<String, TSNode>> emptyCaptures = Arrays.asList(new HashMap<>());
      TSQueryResult result = new TSQueryResult(emptyCaptures);
      
      assertEquals(1, result.size());
      assertTrue(result.firstCapture().isEmpty());
      assertTrue(result.nodes().isEmpty());
    }

    @Test
    @DisplayName("Should maintain consistency between different access methods")
    void shouldMaintainConsistencyBetweenDifferentAccessMethods() {
      TSQueryResult result = new TSQueryResult(testNodes, "method");
      
      assertEquals(result.size(), result.nodes().size());
      assertEquals(result.isEmpty(), result.nodes().isEmpty());
      assertEquals(result.hasResults(), !result.nodes().isEmpty());
      
      if (!result.isEmpty()) {
        assertEquals(result.firstNode(), result.nodes().get(0));
      }
    }
  }

  // Helper methods
  private TSNode findNodeByType(TSNode node, String type) {
    if (node == null) return null;
    if (type.equals(node.getType())) {
      return node;
    }
    for (int i = 0; i < node.getNamedChildCount(); i++) {
      TSNode child = node.getNamedChild(i);
      TSNode result = findNodeByType(child, type);
      if (result != null) {
        return result;
      }
    }
    return null;
  }

  private TSNode findMethodByName(String methodName) {
    TSNode rootNode = tsFile.getTree().getRootNode();
    return findMethodByNameRecursive(rootNode, methodName);
  }

  private TSNode findMethodByNameRecursive(TSNode node, String methodName) {
    if (node == null) return null;
    
    if ("method_declaration".equals(node.getType())) {
      TSNode nameNode = findNodeByType(node, "identifier");
      if (nameNode != null && methodName.equals(tsFile.getTextFromNode(nameNode))) {
        return node;
      }
    }
    
    for (int i = 0; i < node.getNamedChildCount(); i++) {
      TSNode child = node.getNamedChild(i);
      TSNode result = findMethodByNameRecursive(child, methodName);
      if (result != null) {
        return result;
      }
    }
    return null;
  }
}