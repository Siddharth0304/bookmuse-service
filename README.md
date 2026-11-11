# BookMuse backend

Spring Boot 4.1.2, Java 21, Gradle, PostgreSQL 16, Spring Security, BCrypt, signed JWT access tokens, rotating opaque refresh tokens, Flyway, Lombok, and springdoc OpenAPI. This repository contains the backend only.

## Run locally

Install JDK 21 and Docker Desktop. Copy `.env.example` to `.env`, replace every example credential, and generate a random JWT secret of at least 32 bytes. `.env` is ignored by Git. Spring reads environment variables; it does **not** automatically load `.env`.

PowerShell:

```powershell
Copy-Item .env.example .env
# Edit .env before continuing, then load its values into this terminal:
Get-Content .env | ForEach-Object {
    if ($_ -match '^([A-Z_]+)=(.*)$') {
        [Environment]::SetEnvironmentVariable($matches[1], $matches[2], 'Process')
    }
}
docker compose up -d db
.\gradlew.bat bootRun
```

On Linux/macOS use `./gradlew bootRun` after exporting the same variables. Build with `./gradlew clean build`; run all tests with `./gradlew test` (Windows: `.\gradlew.bat test`). The Gradle wrapper downloads its distribution and dependencies on first use. If your machine sets an unwritable Gradle home, set `GRADLE_USER_HOME` to a writable directory.

| Variable | Purpose / default |
|---|---|
| `DB_URL` | JDBC URL; `jdbc:postgresql://localhost:5432/bookmuse` |
| `DB_USERNAME`, `DB_PASSWORD` | Required database credentials |
| `JWT_SECRET` | Required random signing secret, at least 32 UTF-8 bytes |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | Required initial admin; password 8–20 characters |
| `FRONTEND_ORIGIN` | Exact allowed browser origin; `http://localhost:5173` |
| `PORT` | HTTP port; `8080` |
| `ACCESS_TOKEN_SECONDS` | Access JWT lifetime; `900` (minimum 60) |
| `REFRESH_TOKEN_DAYS` | Refresh lifetime; `30` (1–365) |

Flyway runs before Hibernate validates the schema. At startup, an admin is created only if one does not exist; a partial unique index prevents a second admin. Changing the environment password does not reset an existing account. There is no role-assignment API, admin deletion API, or admin registration path. Keep bootstrap credentials in a secret manager.

Swagger UI: `http://localhost:8080/swagger-ui/index.html`; machine-readable specification: `/v3/api-docs`. Use **Authorize** with the access token. The complete endpoint table is in [docs/API.md](docs/API.md), and the ER overview is in [docs/SCHEMA.md](docs/SCHEMA.md).

## Sample data

For frontend development, see [docs/FRONTEND_GUIDE.md](docs/FRONTEND_GUIDE.md) for all endpoints, validation rules, TypeScript response types, authentication flows, and UI integration notes.

After startup, run `./scripts/seed.ps1` in PowerShell with `ADMIN_EMAIL` and `ADMIN_PASSWORD` exported. It adds three example books through the admin API and skips matching existing titles. Set `DEMO_EMAIL` and `DEMO_PASSWORD` to optionally register a sample reader. It never prints tokens or passwords. The admin login uses the environment account; no default account password is committed.

## Architecture and behavior

Administrator visibility depends on the caller's role. ADMIN callers can see administrator accounts; other roles cannot see them in user/follower/member lists or access their profile/library directly. Administrator-authored community content and administrator-actor notifications are likewise hidden from other roles to avoid exposing account IDs. Database read queries apply this rule before pagination and aggregation; hidden direct resources return 404. Books remain available in the shared catalogue. See [docs/API.md](docs/API.md#administrator-visibility) for details.

`controller → service → repository`, with request/response DTOs and mapping in `mapper`. Services own transactions and enforce admin, ownership, and membership rules. `UserRepository` uses Spring Data JPA; `DomainRepository` centralizes parameterized JPA queries, pagination, locking, and grouped counts. Entities reference foreign keys by UUID; database constraints enforce relationships without lazy-loading serialization or association N+1 queries. Profile and post page counts are fetched in batches. PostgreSQL executes recommendation scoring and pagination in SQL.

Passwords must contain 8–20 characters. Registration always creates `USER`. Access JWTs are checked against current database status and token version on every authenticated request. Blocking revokes refresh tokens and increments the version, permanently rejecting existing access tokens even after activation. Refresh tokens are single-use and stored only as SHA-256 hashes; concurrent reuse is serialized with a database lock. Logout revokes the supplied refresh token; its access JWT remains valid until expiration.

Lists use zero-based `page` and `size` (default 20, maximum 100), returning `content`, `number`, `size`, `totalElements`, and `totalPages`. Ordering is fixed per endpoint with an ID tie-breaker; client `sort` is not applied. Errors use `application/problem+json`: 400 validation, 401 authentication, 403 authorization, 404 missing resources, 409 conflicts. Progress is an optional integer percentage; `READ` sets it to 100. Genres are trimmed, lowercase, unique labels. Shares count each recorded event; likes, follows, library additions, and club joins are unique per user/target.

Club creators automatically join and must remain members until they delete the club. Discussion access requires membership. Notifications go to the followed reader, post author, club creator, or discussion author, excluding self-actions. Book deletion cascades to its library entries, posts, likes, shares, and comments; club deletion cascades to memberships, discussions, and replies. Notifications retain historical resource IDs, so a linked resource may subsequently return 404.

Recommendations assign weight 2 to each favourite genre and weight 1 for each occurrence of a genre in the reader's library. Candidate genre weights are summed; library popularity breaks ties. Owned books are excluded; readers without affinities receive popular books. The scoring comment is in `RecommendationRepository`.

## Verification and deployment

JUnit/Mockito tests exercise password validation, token signatures and revocation, registration, refresh rotation, blocked logins, ownership, and membership. MVC slice tests exercise registration and invalid input. `BackendIntegrationTest` uses real PostgreSQL 16 through Testcontainers and exercises migrations, authentication, social events, clubs, pagination, and recommendations. It is explicitly **skipped when Docker is unavailable**, unless `TEST_DB_URL`, `TEST_DB_USERNAME`, and `TEST_DB_PASSWORD` identify a dedicated, disposable PostgreSQL test database. The suite applies migrations and inserts test data into that database; never point it at a shared or production database. Check test reports before release. No coverage percentage is prescribed.

For deployment, use TLS at the ingress, a dedicated database role, managed backups, and environment-specific CORS/secrets. Apply ingress login/request rate limits, restrict public Swagger access if required, and rotate signing keys through your deployment process (rotation logs out all access-token holders). Expired refresh-token cleanup and email/password recovery are not implemented. Do not expose database port 5432 publicly. Never edit deployed Flyway scripts.
