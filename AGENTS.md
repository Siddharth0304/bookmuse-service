# AGENTS.md

## Project overview
Spring Boot REST API backed by PostgreSQL.
- Java 21, Spring Boot 4.1.2, Groovy
- Persistence: Spring Data JPA + Hibernate, schema managed by Flyway
- Database: PostgreSQL 16
- Tests: JUnit 5, Mockito, Testcontainers

## Project structure
```
src/main/java/com/example/app/
  controller/   REST controllers (HTTP only, no business logic)
  service/      Business logic and transactions
  repository/   Spring Data JPA repositories
  models/       JPA entities
  dto/          Request/response objects
  mapper/       Entity <-> DTO mapping
  exception/    Custom exceptions and global handler
  config/       Spring configuration
  security/     User authentication and authorization
src/main/resources/
  application.yml
  application-dev.yml
  db/migration/ Flyway scripts (V1__init.sql, V2__...)
src/test/java/  Mirrors the main package structure
```

## Build, run and test
- Build: `./gradlew clean build`
- Run locally: `./gradlew bootRun --args='--spring.profiles.active=dev'`
- Run all tests: `./gradlew test`
- Run one spec: `./gradlew test --tests "com.example.app.service.UserServiceSpec"`
- Start local Postgres: `docker compose up -d db`
- Always run `./gradlew test` before finishing a task and fix any failures.

## Groovy conventions
- Use `@CompileStatic` on services, controllers and config classes by default.
  Only use dynamic Groovy where it is clearly needed (e.g. in Spock specs).
- Do NOT use `def` for fields or method return types in main code; declare
  explicit types. `def` is acceptable for local variables in tests.
- Use constructor injection with `final` fields. Avoid field `@Autowired`.
- Avoid `@Canonical`/`@Immutable` on JPA entities (they break Hibernate proxies
  and lazy loading). Use plain classes with `@Entity` and explicit
  `equals`/`hashCode` based on the id.
- Use `@Slf4j` for logging. Never log passwords, tokens or personal data.
- Prefer `Optional` or the safe-navigation operator `?.` over returning `null`
  without handling. Avoid the Elvis operator `?:` hiding real errors.
- Use `==` for value comparison (Groovy maps it to `equals`); use `.is()` only
  for identity.
- No wildcard imports. Remove unused imports.
- Keep Groovy and Spring Boot versions compatible; do not upgrade Groovy
  without asking.

## Database rules
- NEVER edit an existing Flyway migration. Add a new one with the next
  version number, e.g. `V5__add_orders_table.sql`.
- Never use `spring.jpa.hibernate.ddl-auto=update` or `create`. Use `validate`.
- Use `snake_case` for tables and columns, plural table names (`users`).
- Every table needs a primary key, `created_at` and `updated_at`.
- Add indexes for foreign keys and frequently queried columns.
- Avoid N+1 queries: use `JOIN FETCH` or `@EntityGraph` for associations.
- Do not run destructive SQL (`DROP`, `TRUNCATE`, mass `DELETE`) without asking.
- Use FETCH=LAZY wherever required
- Avoid GString interpolation in queries. Always use bound parameters
  (`@Query` with named parameters) to prevent SQL injection.

## API conventions
- Controllers return DTOs, never entities.
- Validate input with `@Valid` and Jakarta Bean Validation annotations.
- Put `@Transactional` on service methods, not controllers or repositories.
- Handle errors through the global `@RestControllerAdvice`; throw custom
  exceptions from services and return correct HTTP status codes.

## Testing (Spock)
- Add or update a Spock spec for every behavior change.
- Specs are named `<ClassName>Spec` and live in `src/test/groovy`.
- Use `given:` / `when:` / `then:` / `where:` blocks, and Spock `Mock()` and
  `Stub()` instead of Mockito.
- Use data-driven `where:` tables for multiple input cases.
- Feature method names are descriptive strings, e.g.
  `def "returns 404 when user does not exist"()`.
- Repository and integration specs use Testcontainers with real PostgreSQL,
  not H2.

## Configuration and secrets
- Never commit secrets. Read DB credentials from environment variables:
  `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`.
- Do not modify `application-prod.yml` unless asked.
- Do not change dependency versions in `build.gradle` unless asked.

## Workflow
- Keep changes small and focused; do not refactor unrelated code.
- Briefly explain which files you changed and why.
- Ask before adding new dependencies or making breaking API changes.
- Update OpenAPI annotations when endpoints change.