package io.github.syntaxpresso.core.command.dto;

import java.io.Serializable;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GetJPAEntityInfoResponse implements Serializable {
  private Boolean isJPAEntity;
  private String entityType;
  private String entityPackageName;
  private String entityPath;
  private String idFieldType;
  private String idFieldPackageName;
}
