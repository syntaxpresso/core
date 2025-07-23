package io.github.syntaxpresso.core.java.command.enums;

public enum JavaFileTemplate {
  CLASS("package %s;%n%npublic class %s {\n\n}"),
  INTERFACE("package %s;%n%npublic interface %s {\n\n}"),
  ENUM("package %s;%n%npublic enum %s {\n\n}"),
  RECORD("package %s;%n%npublic record %s(\n\n) {\n\n}"),
  ANNOTATION("package %s;%n%npublic @interface %s {\n\n}");

  private final String template;

  JavaFileTemplate(String template) {
    this.template = template;
  }

  public String getSourceContent(String packageName, String fileName) {
    return String.format(this.template, packageName, fileName);
  }
}
