package io.github.syntaxpresso.core.common.extra;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import org.treesitter.TSLanguage;
import org.treesitter.TreeSitterJava;

@Getter
public enum SupportedLanguage {
  JAVA(new TreeSitterJava(), ".java");
  private final TSLanguage language;
  private final String fileExtension;
  private static final Map<TSLanguage, SupportedLanguage> languageMap =
      Stream.of(values())
          .collect(Collectors.toMap(SupportedLanguage::getLanguage, Function.identity()));

  SupportedLanguage(TSLanguage language, String fileExtension) {
    this.language = language;
    this.fileExtension = fileExtension;
  }

  public static Optional<SupportedLanguage> fromLanguage(TSLanguage language) {
    return Optional.ofNullable(languageMap.get(language));
  }
}
