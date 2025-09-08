package io.github.syntaxpresso.core.common.extra;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.treesitter.TSNode;

/**
 * A unified result type for Tree-sitter queries that provides convenient access to both simple node
 * results and complex capture results.
 *
 * <p>This class wraps query results and provides multiple ways to access them:
 *
 * <ul>
 *   <li>As individual nodes via {@link #nodes()}, {@link #firstNode()}, etc.
 *   <li>As capture maps via {@link #captures()}, {@link #firstCapture()}, etc.
 *   <li>As streams for functional processing
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Simple query - get first method
 * TSNode method = tsFile.query("(method_declaration)")
 *     .execute()
 *     .firstNode();
 *
 * // Get all nodes
 * List<TSNode> allMethods = tsFile.query("(method_declaration)")
 *     .execute()
 *     .nodes();
 *
 * // Work with captures
 * tsFile.query("(method_declaration name: (identifier) @name)")
 *     .execute()
 *     .forEachCapture(capture -> {
 *         TSNode name = capture.get("name");
 *         System.out.println(tsFile.getTextFromNode(name));
 *     });
 * }</pre>
 */
public class TSQueryResult {
  private final List<Map<String, TSNode>> matches;
  private final boolean isSingleCapture;
  private final String primaryCapture;

  /**
   * Creates a result from matches with multiple captures. Used when returningAllCaptures() or
   * returningCaptures() is used.
   */
  public TSQueryResult(List<Map<String, TSNode>> matches) {
    this.matches = matches != null ? matches : new ArrayList<>();
    this.isSingleCapture = false;
    this.primaryCapture = null;
  }

  /**
   * Creates a result from a list of nodes (single capture or primary nodes). Used for default
   * queries or returning(captureName).
   *
   * @param nodes List of nodes to wrap
   * @param captureName Name of the capture (use "_" for unnamed/primary nodes)
   */
  public TSQueryResult(List<TSNode> nodes, String captureName) {
    this.matches =
        nodes.stream()
            .map(node -> Collections.singletonMap(captureName, node))
            .collect(Collectors.toList());
    this.isSingleCapture = true;
    this.primaryCapture = captureName;
  }

  /**
   * Gets all nodes from the primary/single capture. For simple queries, returns all matched nodes.
   * For queries with captures, returns nodes from the main capture.
   *
   * @return List of nodes, never null
   */
  public List<TSNode> nodes() {
    if (isSingleCapture && primaryCapture != null) {
      return matches.stream()
          .map(m -> m.get(primaryCapture))
          .filter(Objects::nonNull)
          .collect(Collectors.toList());
    }
    // For complex captures, return all unique nodes
    return matches.stream()
        .flatMap(m -> m.values().stream())
        .distinct()
        .sorted(Comparator.comparingInt(TSNode::getStartByte))
        .collect(Collectors.toList());
  }

  /**
   * Gets all capture maps. Each map represents one match with capture names as keys.
   *
   * @return List of capture maps, never null
   */
  public List<Map<String, TSNode>> captures() {
    return new ArrayList<>(matches);
  }

  /**
   * Gets the first node, or null if no results.
   *
   * @return First node or null
   */
  public TSNode firstNode() {
    List<TSNode> nodeList = nodes();
    return nodeList.isEmpty() ? null : nodeList.get(0);
  }

  /**
   * Gets the first node as Optional.
   *
   * @return Optional containing first node
   */
  public Optional<TSNode> firstNodeOptional() {
    return Optional.ofNullable(firstNode());
  }

  /**
   * Gets the first capture map, or empty map if no results.
   *
   * @return First capture map or empty map
   */
  public Map<String, TSNode> firstCapture() {
    return matches.isEmpty() ? Collections.emptyMap() : matches.get(0);
  }

  /**
   * Gets the first capture map as Optional.
   *
   * @return Optional containing first capture map
   */
  public Optional<Map<String, TSNode>> firstCaptureOptional() {
    return matches.isEmpty() ? Optional.empty() : Optional.of(matches.get(0));
  }

  /**
   * Gets exactly one node, throwing if there are zero or multiple.
   *
   * @return The single node
   * @throws IllegalStateException if not exactly one result
   */
  public TSNode singleNode() {
    List<TSNode> nodeList = nodes();
    if (nodeList.isEmpty()) {
      throw new IllegalStateException("Query returned no results, expected exactly one");
    }
    if (nodeList.size() > 1) {
      throw new IllegalStateException(
          "Query returned " + nodeList.size() + " results, expected exactly one");
    }
    return nodeList.get(0);
  }

  /**
   * Gets exactly one capture map, throwing if there are zero or multiple.
   *
   * @return The single capture map
   * @throws IllegalStateException if not exactly one result
   */
  public Map<String, TSNode> singleCapture() {
    if (matches.isEmpty()) {
      throw new IllegalStateException("Query returned no results, expected exactly one");
    }
    if (matches.size() > 1) {
      throw new IllegalStateException(
          "Query returned " + matches.size() + " results, expected exactly one");
    }
    return matches.get(0);
  }

  /**
   * Checks if the query returned no results.
   *
   * @return true if no results
   */
  public boolean isEmpty() {
    return matches.isEmpty();
  }

  /**
   * Checks if the query returned any results.
   *
   * @return true if there are results
   */
  public boolean hasResults() {
    return !matches.isEmpty();
  }

  /**
   * Gets the number of matches.
   *
   * @return Number of matches
   */
  public int size() {
    return matches.size();
  }

  /**
   * Gets nodes from a specific capture across all matches.
   *
   * @param captureName Name of the capture (without @)
   * @return List of nodes from that capture
   */
  public List<TSNode> nodesFrom(String captureName) {
    return matches.stream()
        .map(m -> m.get(captureName))
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  /**
   * Gets the first node from a specific capture.
   *
   * @param captureName Name of the capture (without @)
   * @return First node from that capture, or null
   */
  public TSNode firstNodeFrom(String captureName) {
    for (Map<String, TSNode> match : matches) {
      TSNode node = match.get(captureName);
      if (node != null) {
        return node;
      }
    }
    return null;
  }

  /**
   * Creates a stream of all nodes.
   *
   * @return Stream of nodes
   */
  public Stream<TSNode> streamNodes() {
    return nodes().stream();
  }

  /**
   * Creates a stream of all capture maps.
   *
   * @return Stream of capture maps
   */
  public Stream<Map<String, TSNode>> streamCaptures() {
    return matches.stream();
  }

  /**
   * Applies a function to each capture map.
   *
   * @param action Consumer to apply to each capture map
   */
  public void forEachCapture(java.util.function.Consumer<Map<String, TSNode>> action) {
    matches.forEach(action);
  }

  /**
   * Applies a function to each node.
   *
   * @param action Consumer to apply to each node
   */
  public void forEachNode(java.util.function.Consumer<TSNode> action) {
    nodes().forEach(action);
  }

  /**
   * Filters matches based on a predicate.
   *
   * @param predicate Test for each capture map
   * @return New TSQueryResult with filtered matches
   */
  public TSQueryResult filter(java.util.function.Predicate<Map<String, TSNode>> predicate) {
    List<Map<String, TSNode>> filtered =
        matches.stream().filter(predicate).collect(Collectors.toList());
    return new TSQueryResult(filtered);
  }

  /**
   * Filters matches where a specific capture matches a predicate.
   *
   * @param captureName Name of the capture to test
   * @param predicate Test for the capture's node
   * @return New TSQueryResult with filtered matches
   */
  public TSQueryResult filterByCapture(
      String captureName, java.util.function.Predicate<TSNode> predicate) {
    List<Map<String, TSNode>> filtered =
        matches.stream()
            .filter(
                match -> {
                  TSNode node = match.get(captureName);
                  return node != null && predicate.test(node);
                })
            .collect(Collectors.toList());
    return new TSQueryResult(filtered);
  }

  /**
   * Maps each capture map to another type.
   *
   * @param mapper Function to transform each capture map
   * @param <T> Target type
   * @return List of transformed values
   */
  public <T> List<T> map(java.util.function.Function<Map<String, TSNode>, T> mapper) {
    return matches.stream().map(mapper).collect(Collectors.toList());
  }

  /**
   * Maps each node to another type.
   *
   * @param mapper Function to transform each node
   * @param <T> Target type
   * @return List of transformed values
   */
  public <T> List<T> mapNodes(java.util.function.Function<TSNode, T> mapper) {
    return nodes().stream().map(mapper).collect(Collectors.toList());
  }

  /**
   * Collects nodes using a custom collector.
   *
   * @param collector Collector to use
   * @param <R> Result type
   * @return Collected result
   */
  public <R> R collectNodes(java.util.stream.Collector<TSNode, ?, R> collector) {
    return nodes().stream().collect(collector);
  }

  /**
   * Collects capture maps using a custom collector.
   *
   * @param collector Collector to use
   * @param <R> Result type
   * @return Collected result
   */
  public <R> R collectCaptures(java.util.stream.Collector<Map<String, TSNode>, ?, R> collector) {
    return matches.stream().collect(collector);
  }
}
