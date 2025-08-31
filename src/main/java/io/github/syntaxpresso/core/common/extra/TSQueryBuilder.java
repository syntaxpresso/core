package io.github.syntaxpresso.core.common.extra;

import io.github.syntaxpresso.core.common.TSFile;
import java.util.*;
import java.util.stream.Collectors;
import org.treesitter.TSException;
import org.treesitter.TSNode;
import org.treesitter.TSQuery;
import org.treesitter.TSQueryCapture;
import org.treesitter.TSQueryCursor;
import org.treesitter.TSQueryMatch;

/**
 * A fluent builder for executing Tree-sitter queries with optional predicate support.
 *
 * <p>This builder provides a clean, chainable API for constructing and executing queries with
 * various options for filtering and result selection.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Simple query for nodes
 * List<TSNode> methods = tsFile.query("(method_declaration) @method")
 *     .execute();
 *
 * // Query with predicates returning specific capture
 * List<TSNode> getters = tsFile.query("""
 *     (method_declaration
 *       name: (identifier) @name
 *       (#match? @name "^get"))
 *     """)
 *     .returning("name")
 *     .execute();
 *
 * // Query returning all captures as maps
 * List<Map<String, TSNode>> matches = tsFile.query("""
 *     (method_declaration
 *       name: (identifier) @name
 *       parameters: (formal_parameters) @params
 *       (#match? @name "^set"))
 *     """)
 *     .returningAllCaptures()
 *     .execute();
 *
 * // Query with filtered captures
 * List<Map<String, TSNode>> filtered = tsFile.query(queryString)
 *     .returningCaptures("name", "params")
 *     .execute();
 *
 * // Scoped query within a specific node
 * List<TSNode> classMembers = tsFile.query("(field_declaration) @field")
 *     .within(classNode)
 *     .execute();
 * }</pre>
 */
public class TSQueryBuilder {
  private final TSFile file;
  private final String queryString;
  private TSNode scopeNode;
  private ReturnType returnType = ReturnType.NODES;
  private String singleCapture;
  private Set<String> multipleCaptures;

  private enum ReturnType {
    NODES,
    SINGLE_CAPTURE,
    ALL_CAPTURES,
    FILTERED_CAPTURES
  }

  /**
   * Creates a new query builder.
   *
   * @param file The TSFile to query
   * @param queryString The Tree-sitter query string
   */
  public TSQueryBuilder(TSFile file, String queryString) {
    this.file = file;
    this.queryString = queryString;
    this.scopeNode = file.getTree().getRootNode();
  }

  /**
   * Limits the query scope to a specific node's subtree.
   *
   * @param node The root node of the subtree to search within
   * @return This builder instance for method chaining
   */
  public TSQueryBuilder within(TSNode node) {
    this.scopeNode = node;
    return this;
  }

  /**
   * Sets the query to return nodes from a specific named capture.
   *
   * @param captureName The capture name to extract (without @ prefix)
   * @return This builder instance for method chaining
   */
  public TSQueryBuilder returning(String captureName) {
    this.returnType = ReturnType.SINGLE_CAPTURE;
    this.singleCapture = captureName;
    return this;
  }

  /**
   * Sets the query to return all named captures in map form.
   *
   * @return This builder instance for method chaining
   */
  public TSQueryBuilder returningAllCaptures() {
    this.returnType = ReturnType.ALL_CAPTURES;
    return this;
  }

  /**
   * Configures the query to return specific captures as maps.
   *
   * @param captureNames The names of captures to return (without @)
   * @return This builder for chaining
   */
  public TSQueryBuilder returningCaptures(String... captureNames) {
    this.returnType = ReturnType.FILTERED_CAPTURES;
    this.multipleCaptures = new HashSet<>(Arrays.asList(captureNames));
    return this;
  }

  /**
   * Configures the query to return specific captures as maps.
   *
   * @param captureNames The names of captures to return (without @)
   * @return This builder for chaining
   */
  public TSQueryBuilder returningCaptures(Set<String> captureNames) {
    this.returnType = ReturnType.FILTERED_CAPTURES;
    this.multipleCaptures = new HashSet<>(captureNames);
    return this;
  }

  /**
   * Common helper method to execute Tree-sitter queries with custom match processing. This method
   * handles the standard query execution pattern and delegates match processing to the provided
   * consumer function.
   *
   * @param node The node to run the query on.
   * @param queryString The Tree-sitter query string.
   * @param matchProcessor Function to process each match and query pair.
   * @param <T> The type of results to return.
   * @return A list of results from the match processor, or empty list if query fails.
   */
  private <T> List<T> executeQuery(
      TSNode node,
      String queryString,
      java.util.function.Function<java.util.Map.Entry<TSQueryMatch, TSQuery>, List<T>>
          matchProcessor) {
    List<T> results = new ArrayList<>();
    try {
      TSQuery query = new TSQuery(this.file.getParser().getLanguage(), queryString);
      TSQueryCursor cursor = new TSQueryCursor();
      cursor.exec(query, node);
      TSQueryMatch match = new TSQueryMatch();
      while (cursor.nextMatch(match)) {
        // Create a map entry to pass both match and query to the processor
        java.util.Map.Entry<TSQueryMatch, TSQuery> entry =
            new java.util.AbstractMap.SimpleEntry<>(match, query);
        results.addAll(matchProcessor.apply(entry));
      }
    } catch (TSException e) {
      return new ArrayList<>();
    }
    return results;
  }

  private List<TSNode> extractSingleCapture(List<Map<String, TSNode>> matches, String captureName) {
    Set<TSNode> resultNodes = new LinkedHashSet<>();
    for (Map<String, TSNode> match : matches) {
      TSNode node = match.get(captureName);
      if (node != null) {
        resultNodes.add(node);
      }
    }
    return new ArrayList<>(resultNodes)
        .stream()
            .sorted(Comparator.comparingInt(TSNode::getStartByte))
            .collect(Collectors.toList());
  }

  private List<Map<String, TSNode>> filterCaptures(
      List<Map<String, TSNode>> matches, Set<String> captureNames) {
    return matches.stream()
        .map(
            match -> {
              Map<String, TSNode> filtered = new HashMap<>();
              for (String name : captureNames) {
                if (match.containsKey(name)) {
                  filtered.put(name, match.get(name));
                }
              }
              return filtered;
            })
        .filter(map -> !map.isEmpty())
        .collect(Collectors.toList());
  }

  private List<TSNode> extractPrimaryNodes(
      List<Map<String, TSNode>> matches,
      String structuralQuery,
      TSQueryPredicateEvaluator evaluator) {
    String mainCapture = findMainCapture(structuralQuery);
    Set<TSNode> resultNodes = new LinkedHashSet<>();
    for (Map<String, TSNode> match : matches) {
      if (mainCapture != null && match.containsKey(mainCapture)) {
        resultNodes.add(match.get(mainCapture));
      } else {
        TSNode primaryNode = evaluator.findPrimaryNode(match);
        if (primaryNode != null) {
          resultNodes.add(primaryNode);
        }
      }
    }
    return new ArrayList<>(resultNodes)
        .stream()
            .sorted(Comparator.comparingInt(TSNode::getStartByte))
            .collect(Collectors.toList());
  }

  private String findMainCapture(String query) {
    java.util.regex.Pattern endPattern =
        java.util.regex.Pattern.compile("[)\\]]\\s*@([a-zA-Z_][a-zA-Z0-9_]*)\\s*$");
    java.util.regex.Matcher matcher = endPattern.matcher(query.trim());
    if (matcher.find()) {
      return matcher.group(1);
    }
    return null;
  }

  /**
   * Executes a Tree-sitter query on a specific node and returns all matches with their named
   * captures as key-value pairs.
   *
   * <p>This method differs from {@link #query(TSNode, String)} by preserving capture names and
   * returning all matches rather than just unique nodes. Each match is represented as a Map where
   * the key is the capture name (e.g., "@function", "@parameter") and the value is the
   * corresponding {@link TSNode}.
   *
   * <p>Results are automatically sorted by the start byte position of the first node in each match
   * to ensure consistent ordering across multiple executions.
   *
   * <p>Example usage:
   *
   * <pre>{@code
   * // Find a specific class node first
   * TSNode classNode = tsFile.query("(class_declaration) @class").get(0);
   *
   * // Query for method declarations within that class only
   * String query = "(method_declaration name: (identifier) @method_name " +
   *                "parameters: (formal_parameters) @params)";
   * List<Map<String, TSNode>> matches = tsFile.queryForCaptures(classNode, query);
   *
   * for (Map<String, TSNode> match : matches) {
   *     TSNode methodName = match.get("method_name");
   *     TSNode params = match.get("params");
   *     // Process the captured nodes within the specific class...
   * }
   * }</pre>
   *
   * @param node The node to run the query on. The query will only search within this node's
   *     subtree.
   * @param queryString The Tree-sitter query string with named captures (e.g., "@capture_name").
   *     Must be a valid Tree-sitter query syntax.
   * @return A list of maps, where each map represents a match and contains capture names as keys
   *     and their corresponding {@link TSNode} objects as values. Returns an empty list if no
   *     matches are found or if the query execution fails.
   */
  protected List<Map<String, TSNode>> queryForCaptures(TSNode node, String queryString) {
    List<Map<String, TSNode>> allMatches =
        executeQuery(
            node,
            queryString,
            entry -> {
              TSQueryMatch match = entry.getKey();
              TSQuery query = entry.getValue();

              Map<String, TSNode> matchMap = new HashMap<>();
              for (TSQueryCapture capture : match.getCaptures()) {
                String captureName = query.getCaptureNameForId(capture.getIndex());
                matchMap.put(captureName, capture.getNode());
              }
              // Return single match as list
              return java.util.Collections.singletonList(matchMap);
            });
    // Sort by start byte for consistency
    allMatches.sort(
        (m1, m2) -> {
          if (m1.isEmpty() || m2.isEmpty()) return 0;
          TSNode n1 = m1.values().iterator().next();
          TSNode n2 = m2.values().iterator().next();
          return Integer.compare(n1.getStartByte(), n2.getStartByte());
        });

    return allMatches;
  }

  /**
   * Executes the configured query and returns a TSQueryResult.
   *
   * @return TSQueryResult containing the query results
   */
  public TSQueryResult execute() {
    TSQueryPredicateEvaluator evaluator = new TSQueryPredicateEvaluator(file);
    List<String> predicates = evaluator.extractPredicates(queryString);
    String structuralQuery = evaluator.removePredicates(queryString);
    List<Map<String, TSNode>> matches = this.queryForCaptures(scopeNode, structuralQuery);
    // Apply predicate filtering if needed
    if (!predicates.isEmpty()) {
      matches =
          matches.stream()
              .filter(match -> evaluator.evaluatePredicates(match, predicates))
              .collect(Collectors.toList());
    }
    // Return appropriate TSQueryResult based on return type
    switch (returnType) {
      case SINGLE_CAPTURE:
        List<TSNode> singleNodes = extractSingleCapture(matches, singleCapture);
        return new TSQueryResult(singleNodes, singleCapture);
      case ALL_CAPTURES:
        return new TSQueryResult(matches);
      case FILTERED_CAPTURES:
        List<Map<String, TSNode>> filtered = filterCaptures(matches, multipleCaptures);
        return new TSQueryResult(filtered);
      case NODES:
      default:
        List<TSNode> primaryNodes = extractPrimaryNodes(matches, structuralQuery, evaluator);
        return new TSQueryResult(primaryNodes, "_");
    }
  }
}
