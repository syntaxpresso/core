package io.github.syntaxpresso.core.command.extra;

public enum JavaFileTemplate {
  CLASS("package %s;%n%npublic class %s {\n\n}"),
  INTERFACE("package %s;%n%npublic interface %s {\n\n}"),
  ENUM("package %s;%n%npublic enum %s {\n\n}"),
  RECORD("package %s;%n%npublic record %s(\n\n) {\n\n}"),
  ANNOTATION("package %s;%n%npublic @interface %s {\n\n}"),
  JPA_REPOSITORY("package %s;%n%nimport org.springframework.data.jpa.repository.JpaRepository;%nimport org.springframework.stereotype.Repository;%n%n@Repository%npublic interface %sRepository extends JpaRepository<%s, %s> {%n%n}");
  private final String template;

  JavaFileTemplate(String template) {
    this.template = template;
  }

  public String getSourceContent(String packageName, String className) {
    return String.format(this.template, packageName, className);
  }

  public String getSourceContent(String packageName, String className, String entityType, String idType) {
    if (this == JPA_REPOSITORY) {
      return String.format(this.template, packageName, entityType, entityType, idType);
    }
    return String.format(this.template, packageName, className);
  }
}
