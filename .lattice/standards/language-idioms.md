---
language: kotlin
version: "2.1.0"
---

# Language Idioms: Kotlin

## Error Handling

Kotlin has no checked exceptions. Sealed `Result<T>` type for recoverable operations via `runCatching`. `try/catch` for imperative error handling. Custom exceptions extend `RuntimeException`.

## Type System & Object Model

Data classes for value objects. Sealed classes/interfaces for restricted hierarchies. Null safety via `?` nullable types. Extension functions. Composition over inheritance, interfaces preferred. `object` keyword for singletons.

## Naming Conventions

`camelCase` for variables, methods, properties. `PascalCase` for classes, interfaces, objects, enums. `SCREAMING_SNAKE_CASE` for constants. `lowercase.dotted` for packages. No Hungarian notation or `I` prefix for interfaces.

## Testing Patterns

JUnit 5 (`@Test`, `@ParameterizedTest`). MockK for mocking. AssertJ or kotlin.test assertions. Test files co-located or in `src/test/`. `@Nested` for grouping. Data-driven tests via `@CsvSource` or `@MethodSource`.

## Parameter & Function Design

Named arguments and default parameter values. Data class for config objects with >3 params. Single-expression functions. No method overloading needed due to defaults and named args.

## Dependency Management

Manual constructor injection — dependencies passed via constructor parameters. Interfaces for key abstractions. Composition root in Application class or Activity.
