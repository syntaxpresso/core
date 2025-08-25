package io.github.syntaxpresso.core.command.dto;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GetJPAEntityInfoResponse implements Serializable {
  private Boolean isJPAEntity;
  private String entityPath;
  private String entityPackageName;
  private String idFieldType;
  private String idFieldPackageName;
  private String recommendedRepositoryName;
  private String recommendedRepositoryPackageName;
  private List<Map<String, String>> recommendedTypes;
}
