package io.github.syntaxpresso.core.service.java.extra;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class MethodDeclarationService {

  private final FormalParameterService formalParameterService;
  private final LocalVariableDeclarationService localVariableDeclarationService;
}
