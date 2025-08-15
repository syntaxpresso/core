package io.github.syntaxpresso.core.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
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
  private static final ObjectMapper objectMapper =
      new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
  private Boolean succeed;
  private T data;
  private String errorReason;

  /** Static factory method for creating a success response. */
  public static <T> DataTransferObject<T> success(T data) {
    // The error reason will be null for success cases.
    return new DataTransferObject<>(true, data, null);
  }

  /** Static factory method for creating a success without response. */
  public static <T> DataTransferObject<T> success() {
    return new DataTransferObject<>(true, null, null);
  }

  /**
   * Static factory method for creating a failure response with a reason.
   *
   * @param reason A descriptive message about what went wrong.
   * @return A new DataTransferObject instance with succeed=false.
   */
  public static <T> DataTransferObject<T> error(String reason) {
    return new DataTransferObject<>(false, null, reason);
  }

  @Override
  @SneakyThrows
  public String toString() {
    return objectMapper.writeValueAsString(this);
  }
}
