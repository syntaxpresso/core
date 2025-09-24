package io.github.syntaxpresso.core.service.java.command.extra;

import io.github.syntaxpresso.core.command.dto.CreateJPARepositoryResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Represents the result of preparing JPA repository data.
 * Can represent success with repository data, requirement for external symbol source,
 * or an error condition.
 */
@Getter
@RequiredArgsConstructor
public class PrepareDataResult {
  private final JPARepositoryData repositoryData;
  private final CreateJPARepositoryResponse requiresSymbolResponse;
  private final String errorMessage;
  private final ResultType type;

  public enum ResultType {
    SUCCESS,
    REQUIRES_SYMBOL_SOURCE,
    ERROR
  }

  /**
   * Creates a successful result with repository data.
   *
   * @param repositoryData the prepared JPA repository data
   * @return a success result
   */
  public static PrepareDataResult success(JPARepositoryData repositoryData) {
    return new PrepareDataResult(repositoryData, null, null, ResultType.SUCCESS);
  }

  /**
   * Creates a result indicating that external symbol source is required.
   *
   * @param response the response containing symbol information
   * @return a requires symbol source result
   */
  public static PrepareDataResult requiresSymbolSource(CreateJPARepositoryResponse response) {
    return new PrepareDataResult(null, response, null, ResultType.REQUIRES_SYMBOL_SOURCE);
  }

  /**
   * Creates an error result with the specified error message.
   *
   * @param errorMessage the error message
   * @return an error result
   */
  public static PrepareDataResult error(String errorMessage) {
    return new PrepareDataResult(null, null, errorMessage, ResultType.ERROR);
  }

  /**
   * Checks if this result represents a successful operation.
   *
   * @return true if successful, false otherwise
   */
  public boolean isSuccess() {
    return this.type == ResultType.SUCCESS;
  }

  /**
   * Checks if this result requires external symbol source.
   *
   * @return true if requires symbol source, false otherwise
   */
  public boolean requiresSymbolSource() {
    return this.type == ResultType.REQUIRES_SYMBOL_SOURCE;
  }

  /**
   * Checks if this result represents an error.
   *
   * @return true if error, false otherwise
   */
  public boolean isError() {
    return this.type == ResultType.ERROR;
  }
}