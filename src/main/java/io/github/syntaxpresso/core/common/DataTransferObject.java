package io.github.syntaxpresso.core.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DataTransferObject<T> {
  /** Jackson ObjectMapper for JSON serialization */
  private static final ObjectMapper objectMapper = new ObjectMapper();

  /** Boolean flag indicating whether the operation succeeded */
  private Boolean succeed;

  /** Generic data payload, present only on successful operations */
  private T data;

  /** Error description, present only on failed operations */
  private String errorReason;

  /**
   * Creates a successful response with the provided data payload.
   *
   * <p>The resulting DTO will have {@code succeed=true}, the provided data, and {@code
   * errorReason=null}.
   *
   * @param <T> The type of the data payload
   * @param data The success data to include in the response
   * @return A new DataTransferObject representing a successful operation
   */
  public static <T> DataTransferObject<T> success(T data) {
    return new DataTransferObject<>(true, data, null);
  }

  /**
   * Creates a successful response without any data payload.
   *
   * <p>The resulting DTO will have {@code succeed=true}, {@code data=null}, and {@code
   * errorReason=null}. Useful for operations that don't need to return data (e.g., delete
   * operations).
   *
   * @param <T> The generic type parameter (unused but required for type safety)
   * @return A new DataTransferObject representing a successful operation without data
   */
  public static <T> DataTransferObject<T> success() {
    return new DataTransferObject<>(true, null, null);
  }

  /**
   * Creates a failure response with the provided error message.
   *
   * <p>The resulting DTO will have {@code succeed=false}, {@code data=null}, and the provided error
   * reason. This method should be used when an operation fails and needs to communicate the failure
   * reason to the caller.
   *
   * @param <T> The generic type parameter (unused but required for type safety)
   * @param reason A descriptive message explaining what went wrong
   * @return A new DataTransferObject representing a failed operation
   * @throws IllegalArgumentException if reason is null or empty
   */
  public static <T> DataTransferObject<T> error(String reason) {
    if (reason == null || reason.trim().isEmpty()) {
      throw new IllegalArgumentException("Error reason cannot be null or empty");
    }
    return new DataTransferObject<>(false, null, reason);
  }

  /**
   * Serializes this DataTransferObject to a compact JSON string.
   *
   * <p>Uses Jackson ObjectMapper to convert the object to JSON format. The output is compact
   * (single-line) for easy parsing by IDEs and tools. Null fields are excluded from the JSON output
   * due to the {@link JsonInclude.Include#NON_NULL} annotation on the class.
   *
   * <p>Example output:
   *
   * <pre>{@code
   * {"succeed":true,"data":{"field":"value"}}
   * {"succeed":false,"errorReason":"Something went wrong"}
   * }</pre>
   *
   * @return JSON string representation of this DataTransferObject
   * @throws RuntimeException if JSON serialization fails (wrapped by @SneakyThrows)
   */
  @Override
  @SneakyThrows
  public String toString() {
    return objectMapper.writeValueAsString(this);
  }
}
