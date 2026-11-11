# Verification record

Implementation was checked with Java 21.0.6 on Windows in the restricted workspace.

- All production and test Java sources were compiled into class files. The compiler emitted no source errors, but its ZIP-filesystem cleanup raised `java.nio.file.AccessDeniedException` when resolving dependency JAR paths. Consequently, the ordinary Gradle compile/test pipeline could not complete in this sandbox.
- For executable verification, the compiled production and test classes were packaged into temporary JARs under ignored `build/` and supplied to Gradle's test task through a temporary init script. Compilation tasks were excluded for this workaround only; the committed build configuration does not skip compilation or tests.
- The resulting test run passed **17 unit/MVC tests**, with **4 database integration tests skipped**, zero failures, and zero errors. This covers validation, JWT signatures/version revocation, authentication service behavior, ownership and membership denial, notification self-action suppression, genre normalization, registration HTTP behavior, and CORS origin checks.
- Docker's daemon was unavailable. An isolated local PostgreSQL initialization was also blocked by Windows sandbox token/filesystem restrictions. Therefore PostgreSQL migrations, native recommendation SQL, locking behavior, and full HTTP integration flows remain unverified here. The committed integration suite targets PostgreSQL 16 through Testcontainers, or an explicitly configured disposable external database.

Before release, run `./gradlew clean build` in a normal JDK 21 environment with Docker running and confirm all database integration tests execute successfully. The sandbox workaround is not a substitute for that release check.

## Multiple genre filtering

The genre-filter update was checked using the same temporary compilation/JAR workaround. The standard `gradlew test` command remains blocked by dependency JAR access restrictions. The workaround run passed **21 unit/MVC tests**, with **5 PostgreSQL integration tests skipped**, zero failures, and zero errors. New MVC coverage verifies repeated and comma-separated genre parameters, search/pagination binding, single-genre and unfiltered compatibility, and authentication requirements. The added PostgreSQL test covers OR matching combined with text search, exact genre boundaries, normalization, blank/duplicate filters, literal wildcard characters, and filtered pagination; it remains unexecuted here because Docker is unavailable.

## Administrator visibility

After adding caller-dependent visibility, the temporary compilation/JAR workaround passed **48 tests**, with **6 PostgreSQL integration tests skipped**, zero failures, and zero errors. The normal `gradlew test` command was attempted and remains blocked by dependency JAR access restrictions.

The new policy tests cover ADMIN, USER, other non-admin roles, internal unauthenticated reads, and shared book catalogue visibility. Hibernate validates selection and count HQL for all 12 account-bearing entity types against the real entity metadata using the PostgreSQL dialect, without an H2 database or JDBC connection. A new PostgreSQL/HTTP integration scenario checks user lists and direct profiles/libraries, follower counts and pagination, posts/comments/engagement, clubs/members/discussions/replies, notifications, and ADMIN visibility. That scenario remains unexecuted here because Docker is unavailable; run the suite with PostgreSQL before release.
