# Agent Guidelines for SyntaxPresso Core

## Build & Test Commands

- **Build**: `./gradlew build` or `./gradlew shadowJar` for fat jar
- **Run**: `./gradlew run --args="<command-args>"`
- **Test All**: `./gradlew test`
- **Single Test**: `./gradlew test --tests "com.example.ClassName.methodName"`
- **Clean**: `./gradlew clean`

## Code Style & Conventions

- **Language**: Java 24 with GraalVM, uses Lombok extensively
- **Package Structure**: `io.github.syntaxpresso.core.*` - follow existing hierarchy
- **Imports**: Group standard Java, then third-party, then internal (see existing files)
- **Annotations**: Use Lombok (`@Data`, `@Builder`, `@RequiredArgsConstructor`) for boilerplate
- **Error Handling**: Use `DataTransferObject<T>` wrapper with `.success()` and `.error()` methods
- **Naming**: CamelCase classes, camelCase methods/fields, UPPER_CASE constants
- **DTOs**: Place in `*.dto` packages, use record-style with Lombok
- **Services**: Business logic in `*.service` packages, use constructor injection
- **Testing**: JUnit 5 with `@DisplayName`, `@Nested` classes, Mockito for mocking
- **CLI**: Uses Picocli framework with `@CommandLine.Command` annotations
- **Formatting**: Prevent empty lines inside methods and functions; when invoking class fields, use "this"
- **Comments**: Only use comments when strictly necessary to understand complex code

## Key Dependencies

- Tree-sitter for parsing, Picocli for CLI, Jackson for JSON, Guava utilities
- Test: JUnit 5, Mockito, use `@TempDir` for file system tests
