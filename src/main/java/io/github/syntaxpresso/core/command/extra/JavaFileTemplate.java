package io.github.syntaxpresso.core.command.extra;

public enum JavaFileTemplate {
  CLASS("package %s;%n%npublic class %s {}"),
  INTERFACE("package %s;%n%npublic interface %s {}"),
  ENUM("package %s;%n%npublic enum %s {}"),
  RECORD("package %s;%n%npublic record %s() {}"),
  ANNOTATION("package %s;%n%npublic @interface %s {}");
  private final String template;

  JavaFileTemplate(String template) {
    this.template = template;
  }

  public String getSourceContent(String packageName, String className) {
    return String.format(this.template, packageName, className);
  }

  public String getSourceContent(
      String packageName, String className, String entityType, String idType) {
    return String.format(this.template, packageName, className);
  }
}
