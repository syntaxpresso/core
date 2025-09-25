package io.github.syntaxpresso.core.command;

import io.github.syntaxpresso.core.common.DataTransferObject;
import io.github.syntaxpresso.core.common.extra.SupportedIDE;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import io.github.syntaxpresso.core.service.java.command.CreateEntityFieldCommandService;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@RequiredArgsConstructor
@Command(
    name = "create-jpa-entity-field",
    description = "Create JPA Repository for the current JPA Entity.")
public class CreateJPAEntityFieldCommand implements Callable<DataTransferObject<Void>> {
  private final CreateEntityFieldCommandService createEntityFieldCommandService;

  @Option(names = "--cwd", description = "Current Working Directory", required = true)
  private Path cwd;

  @Option(names = "--file-path", description = "The path to the file", required = true)
  private Path filePath;

  @Option(names = "--field-type", description = "Field type", required = false)
  private String fieldType;

  @Option(
      names = "--field-type-package-name",
      description = "Package name of field type",
      required = false)
  private String fieldTypePackageName;

  @Option(names = "--field-type-path", description = "Field type path", required = false)
  private Path fieldTypePath;

  @Option(names = "--field-name", description = "Field name", required = false)
  private String fieldName;

  @Option(names = "--field-length", description = "Field length", required = false)
  private Integer fieldLength;

  @Option(names = "--field-precision", description = "Field precision", required = false)
  private Integer fieldPrecision;

  @Option(names = "--field-scale", description = "Field scale", required = false)
  private Integer fieldScale;

  @Option(
      names = "--field-timezone-storage",
      description = "Field timezone storage",
      required = false)
  private String fieldTimezoneStorage;

  @Option(names = "--field-temporal", description = "Field temporal", required = false)
  private String fieldTemporal;

  @Option(names = "--field-id-generation", description = "Field ID generation", required = false)
  private String fieldIdGeneration;

  @Option(
      names = "--field-id-generation-type",
      description = "Field ID generation type",
      required = false)
  private String fieldIdGenerationType;

  @Option(names = "--field-generator-name", description = "Field generator name", required = false)
  private String fieldGeneratorName;

  @Option(names = "--field-sequence-name", description = "Field sequence name", required = false)
  private String fieldSequenceName;

  @Option(names = "--field-initial-value", description = "Field initial value", required = false)
  private Integer fieldInitialValue;

  @Option(
      names = "--field-allocation-size",
      description = "Field allocation size",
      required = false)
  private Integer fieldAllocationSize;

  @Option(
      names = "--language",
      description = "The language related to the command execution.",
      required = true)
  private SupportedLanguage language;

  @Option(
      names = "--ide",
      description = "The IDE the command is being called from.",
      required = true)
  private SupportedIDE ide = SupportedIDE.NONE;

  @Override
  public DataTransferObject<Void> call() {
    if (this.language.equals(SupportedLanguage.JAVA)) {}
    return DataTransferObject.error("Language not supported.");
  }
}
