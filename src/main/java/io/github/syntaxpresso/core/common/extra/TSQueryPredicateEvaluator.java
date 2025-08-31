package io.github.syntaxpresso.core.common.extra;

import io.github.syntaxpresso.core.common.TSFile;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.treesitter.TSNode;

/**
 * Evaluates extended Tree-sitter query predicates like #eq?, #match?, #any-of?. Handles predicate
 * evaluation for queries not natively supported by Tree-sitter.
 */
public class TSQueryPredicateEvaluator {

  private final TSFile tsFile;
  private static final Pattern PREDICATE_PATTERN = Pattern.compile("#([a-z-]+)\\?\\s*(.*)");
  private static final Pattern CAPTURE_PATTERN = Pattern.compile("@([a-zA-Z_][a-zA-Z0-9_]*)");
  private static final Pattern STRING_PATTERN =
      Pattern.compile("\"([^\"\\\\]*(\\\\.[^\"\\\\]*)*)\"");

  public TSQueryPredicateEvaluator(TSFile tsFile) {
    this.tsFile = tsFile;
  }

  /**
   * Extracts predicate expressions from a query string.
   *
   * @param queryString The query containing predicates
   * @return List of extracted predicate strings
   */
  public List<String> extractPredicates(String queryString) {
    List<String> predicates = new ArrayList<>();

    Pattern predicateBlockPattern = Pattern.compile("\\(\\s*(#[a-z-]+\\?[^)]+)\\)");
    Matcher matcher = predicateBlockPattern.matcher(queryString);
    while (matcher.find()) {
      predicates.add(matcher.group(1).trim());
    }
    return predicates;
  }

  /**
   * Removes all predicate expressions from a query string.
   *
   * @param queryString Query containing predicates
   * @return Clean query string without predicates
   */
  public String removePredicates(String queryString) {
    return queryString.replaceAll("\\s*\\(\\s*#[a-z-]+\\?[^)]+\\)", "");
  }

  /**
   * Evaluates if a match satisfies all given predicates.
   *
   * @param match Captured nodes by name
   * @param predicates Predicates to evaluate
   * @return true if all predicates are satisfied
   */
  public boolean evaluatePredicates(Map<String, TSNode> match, List<String> predicates) {
    for (String predicate : predicates) {
      if (!evaluateSinglePredicate(match, predicate)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Evaluates one predicate against a match.
   *
   * @param match Captured nodes by name
   * @param predicateStr Single predicate expression
   * @return true if predicate is satisfied
   */
  private boolean evaluateSinglePredicate(Map<String, TSNode> match, String predicateStr) {
    Matcher matcher = PREDICATE_PATTERN.matcher(predicateStr);
    if (!matcher.matches()) {
      return false;
    }
    String predicateName = matcher.group(1);
    String args = matcher.group(2).trim();
    switch (predicateName) {
      case "eq":
        return evaluateEq(match, args);
      case "not-eq":
        return !evaluateEq(match, args);
      case "match":
        return evaluateMatch(match, args);
      case "not-match":
        return !evaluateMatch(match, args);
      case "any-of":
        return evaluateAnyOf(match, args);
      case "not-any-of":
        return !evaluateAnyOf(match, args);
      case "contains":
        return evaluateContains(match, args);
      case "not-contains":
        return !evaluateContains(match, args);
      case "is":
        return evaluateIs(match, args);
      case "is-not":
        return !evaluateIs(match, args);
      default:
        // Unknown predicate, default to true to not filter out matches
        return true;
    }
  }

  /** Evaluates equality between captures or capture and string. */
  private boolean evaluateEq(Map<String, TSNode> match, String args) {
    List<String> argList = parseArguments(args);
    if (argList.size() != 2) {
      return false;
    }
    String value1 = resolveValue(match, argList.get(0));
    String value2 = resolveValue(match, argList.get(1));
    return value1 != null && value1.equals(value2);
  }

  /** Evaluates regex pattern matching against captured text. */
  private boolean evaluateMatch(Map<String, TSNode> match, String args) {
    List<String> argList = parseArguments(args);
    if (argList.size() != 2) {
      return false;
    }
    String captureValue = resolveValue(match, argList.get(0));
    String patternStr = stripQuotes(argList.get(1));
    if (captureValue == null || patternStr == null) {
      return false;
    }
    try {
      Pattern pattern = Pattern.compile(patternStr);
      return pattern.matcher(captureValue).find();
    } catch (Exception e) {
      return false;
    }
  }

  /** Checks if a capture matches any provided value. */
  private boolean evaluateAnyOf(Map<String, TSNode> match, String args) {
    List<String> argList = parseArguments(args);
    if (argList.size() < 2) {
      return false;
    }
    String captureValue = resolveValue(match, argList.get(0));
    if (captureValue == null) {
      return false;
    }
    for (int i = 1; i < argList.size(); i++) {
      String value = stripQuotes(argList.get(i));
      if (captureValue.equals(value)) {
        return true;
      }
    }
    return false;
  }

  /** Checks if captured text contains a given substring. */
  private boolean evaluateContains(Map<String, TSNode> match, String args) {
    List<String> argList = parseArguments(args);
    if (argList.size() != 2) {
      return false;
    }
    String captureValue = resolveValue(match, argList.get(0));
    String substring = stripQuotes(argList.get(1));
    return captureValue != null && substring != null && captureValue.contains(substring);
  }

  /** Evaluates node type and semantic properties. */
  private boolean evaluateIs(Map<String, TSNode> match, String args) {
    List<String> argList = parseArguments(args);
    if (argList.size() != 2) {
      return false;
    }
    String captureName = stripCapture(argList.get(0));
    String property = stripQuotes(argList.get(1));
    if (captureName == null || property == null) {
      return false;
    }
    TSNode node = match.get(captureName);
    if (node == null) {
      return false;
    }
    switch (property) {
      case "definition":
      case "definition.method":
      case "definition.function":
      case "definition.class":
        return isDefinition(node);
      case "reference":
      case "reference.call":
        return isReference(node);
      case "local":
        return isLocal(node);
      default:
        return false;
    }
  }

  /** Extracts and orders predicate arguments by position. */
  private List<String> parseArguments(String args) {
    List<String> result = new ArrayList<>();

    Matcher stringMatcher = STRING_PATTERN.matcher(args);
    List<int[]> stringPositions = new ArrayList<>();
    while (stringMatcher.find()) {
      stringPositions.add(new int[] {stringMatcher.start(), stringMatcher.end()});
    }
    Matcher captureMatcher = CAPTURE_PATTERN.matcher(args);
    List<int[]> capturePositions = new ArrayList<>();
    while (captureMatcher.find()) {
      capturePositions.add(new int[] {captureMatcher.start(), captureMatcher.end()});
    }
    List<int[]> allPositions = new ArrayList<>();
    allPositions.addAll(stringPositions);
    allPositions.addAll(capturePositions);
    allPositions.sort(Comparator.comparingInt(a -> a[0]));
    for (int[] pos : allPositions) {
      result.add(args.substring(pos[0], pos[1]));
    }

    return result;
  }

  /** Gets text value from a capture node or literal string. */
  private String resolveValue(Map<String, TSNode> match, String arg) {
    if (arg.startsWith("@")) {
      // It's a capture
      String captureName = arg.substring(1);
      TSNode node = match.get(captureName);
      if (node != null) {
        return tsFile.getTextFromNode(node);
      }
      return null;
    } else if (arg.startsWith("\"") && arg.endsWith("\"")) {
      // It's a literal string
      return stripQuotes(arg);
    }
    return arg;
  }

  /** Removes surrounding quotes from string literals. */
  private String stripQuotes(String str) {
    if (str.startsWith("\"") && str.endsWith("\"") && str.length() >= 2) {
      return str.substring(1, str.length() - 1);
    }
    return str;
  }

  /** Removes @ prefix from capture names. */
  private String stripCapture(String str) {
    if (str.startsWith("@")) {
      return str.substring(1);
    }
    return str;
  }

  /** Identifies definition nodes by type. */
  private boolean isDefinition(TSNode node) {
    String nodeType = node.getType();
    return nodeType.contains("declaration")
        || nodeType.contains("definition")
        || nodeType.equals("function")
        || nodeType.equals("method")
        || nodeType.equals("class")
        || nodeType.equals("interface");
  }

  /** Identifies reference and call nodes by type. */
  private boolean isReference(TSNode node) {
    String nodeType = node.getType();
    return nodeType.contains("call")
        || nodeType.contains("invocation")
        || nodeType.contains("reference")
        || nodeType.equals("identifier");
  }

  /** Determines if node is within a method/function scope. */
  private boolean isLocal(TSNode node) {
    Optional<TSNode> parent = tsFile.findParentNodeByType(node, "method_declaration");
    if (!parent.isPresent()) {
      parent = tsFile.findParentNodeByType(node, "function_declaration");
    }
    return parent.isPresent();
  }

  /**
   * Finds the largest non-auxiliary node in a match.
   *
   * @param match Captured nodes by name
   * @return The primary (largest) node, or null if none found
   */
  public TSNode findPrimaryNode(Map<String, TSNode> match) {
    if (match.isEmpty()) {
      return null;
    }
    TSNode primaryNode = null;
    int largestSpan = -1;
    for (Map.Entry<String, TSNode> entry : match.entrySet()) {
      TSNode node = entry.getValue();
      String captureName = entry.getKey();
      if (isLikelyAuxiliaryCapture(captureName)) {
        continue;
      }
      int span = node.getEndByte() - node.getStartByte();
      if (span > largestSpan) {
        largestSpan = span;
        primaryNode = node;
      }
    }
    if (primaryNode == null) {
      for (TSNode node : match.values()) {
        int span = node.getEndByte() - node.getStartByte();
        if (span > largestSpan) {
          largestSpan = span;
          primaryNode = node;
        }
      }
    }
    return primaryNode;
  }

  /**
   * Checks if a capture is used only for predicate evaluation.
   *
   * @param captureName Capture name without @ prefix
   * @return true if capture is auxiliary
   */
  public boolean isLikelyAuxiliaryCapture(String captureName) {
    Set<String> auxiliaryNames =
        Set.of("name", "value", "key", "id", "identifier", "type", "param", "arg", "attr", "prop");
    return auxiliaryNames.contains(captureName.toLowerCase());
  }
}
