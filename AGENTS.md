# Agent Guidelines for Apache Fesod

This document provides guidelines and instructions for AI agents working with the Apache Fesod codebase.

## Project Overview

**Apache Fesod (Incubating)** is a high-performance and memory-efficient Java library for reading and writing spreadsheet files.

- **Homepage**: [fesod.apache.org](https://fesod.apache.org)
- **License**: Apache License 2.0
- **Java Version**: 1.8 or later
- **Build Tool**: Maven (with Maven Wrapper)

## Project Structure

```
fesod-ori/
├── fesod-bom/          # Bill of Materials for dependency management
├── fesod-common/       # Common utilities and shared code
├── fesod-distribution/ # Distribution packaging
├── fesod-examples/     # Example code and demos
├── fesod-shaded/       # Shaded JAR for reduced conflicts
├── fesod-sheet/        # Core spreadsheet processing module
├── website/            # Documentation website
└── tools/              # Development tools and scripts
```

## Build Commands

```bash
# Clean and build (tests are skipped by default via pom.xml)
./mvnw clean install

# Run all tests
./mvnw test

# Clean and build with tests enabled
./mvnw clean install -Dmaven.test.skip=false

# Skip tests during build (default behavior)
./mvnw clean install -DskipTests

# Check code style
./mvnw checkstyle:check

# Format code
./mvnw spotless:apply
```

> **Note:** The `pom.xml` sets `<maven.test.skip>true</maven.test.skip>` by default. When verifying code changes, always explicitly run tests with `-Dmaven.test.skip=false` or `./mvnw test`.

## Code Conventions

### Java Style
- Follow Apache Software Foundation code style guidelines
- Use Lombok for reducing boilerplate (see `lombok.config`)
- Include Apache license header in all source files
- Use 4 spaces for indentation (no tabs)

### Package Structure
- Main source: `src/main/java/org/apache/fesod/...`
- Tests: `src/test/java/org/apache/fesod/...`

### Naming Conventions
- Classes: PascalCase (e.g., `FesodSheet`, `DemoDataListener`)
- Methods/Variables: camelCase (e.g., `doRead()`, `analysisContext`)
- Constants: UPPER_SNAKE_CASE
- Test classes: Suffix with `Test` (e.g., `FesodSheetTest`)

## Key APIs

### Reading Spreadsheets

```java
public class DemoDataListener implements ReadListener<DemoData> {
    @Override
    public void invoke(DemoData data, AnalysisContext context) {
        // Process each row
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        // Final processing
    }
}

// Usage
FesodSheet.read(fileName, DemoData.class, new DemoDataListener())
    .sheet()
    .doRead();
```

### Writing Spreadsheets

```java
// With annotations
@ExcelProperty("Column Title")
private String field;

// Usage
FesodSheet.write(fileName, DemoData.class)
    .sheet("SheetName")
    .doWrite(dataList);
```

## Important Dependencies

- **Apache POI**: Underlying spreadsheet handling (may need exclusion if already in project)
- **Lombok**: For reducing boilerplate code
- **SLF4J**: Logging facade

## Testing Guidelines

- Unit tests should be in corresponding `*Test.java` files
- Use JUnit for testing
- Mock external dependencies
- Test edge cases: empty files, large files, malformed data
- Ensure tests are deterministic and independent

## Common Tasks

### Adding a New Feature
1. Create feature branch from `main`
2. Implement in appropriate module (usually `fesod-sheet`)
3. Add unit tests
4. Update documentation/examples
5. Run full build: `./mvnw clean install`

### Fixing a Bug
1. Reproduce the issue with a test case
2. Implement fix
3. Verify existing tests pass
4. Add regression test if applicable

### Code Quality Checks
```bash
# Run all checks
./mvnw clean verify

# Check for license headers
./mvnw license:check

# Add missing license headers
./mvnw license:format
```

## Communication

- **Mailing List**: dev@fesod.apache.org
  - Subscribe: dev-subscribe@fesod.apache.org
  - Unsubscribe: dev-unsubscribe@fesod.apache.org
- **GitHub**: [apache/fesod](https://github.com/apache/fesod)

## Resources

- [Contributing Guide](./CONTRIBUTING.md)
- [Apache Incubator Guidelines](https://incubator.apache.org/)
- [Project Website](https://fesod.apache.org)

## Notes for Agents

- This is an Apache Incubating project - follow Apache community guidelines
- All contributions require proper license headers
- Maintain backward compatibility when possible
- Document new features in both code and website
- Performance and memory efficiency are core values - benchmark changes
- **Tests are skipped by default** - always run tests explicitly when verifying changes: `./mvnw test` or `./mvnw clean install -Dmaven.test.skip=false`
