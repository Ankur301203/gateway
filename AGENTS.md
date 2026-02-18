# Repository Guidelines

## Project Structure & Module Organization
- `src/main/java/com/gateway/`: application code, organized by layer (`config`, `database`, `domain`, `handlers`, `http`, `netty`, `repository`, `server`, `service`, `util`).
- `src/main/resources/`: configuration and assets, including `application.properties`, `logback.xml`, and SQL migrations in `db/migration/`.
- `src/test/java/`: tests (currently `com.gateway.AppTest`).
- `Dockerfile`, `docker-compose.yml`: container and local stack setup.
- `target/`: build output (generated).

## Build, Test, and Development Commands
- `mvn clean package`: compile and build the shaded JAR in `target/`.
- `mvn test`: run unit tests via Surefire.
- `java -jar target/gatewayaas-1.0.0.jar`: run the server after packaging (adjust JAR name if version differs).
- `docker-compose up --build`: start the app + PostgreSQL in containers.
- `./run.sh`: local convenience script for build + run (uses the JAR it references).

## Coding Style & Naming Conventions
- Indentation: 4 spaces, standard Java formatting.
- Packages use lowercase (e.g., `com.gateway.service`).
- Classes: `UpperCamelCase`; methods/fields: `lowerCamelCase`; constants: `UPPER_SNAKE_CASE`.
- Prefer explicit, descriptive names for handlers and services (e.g., `HealthCheckService`).

## Testing Guidelines
- Framework: JUnit 3 (see `src/test/java/com/gateway/AppTest.java`).
- Test classes should end in `Test` and live under `src/test/java` with matching packages.
- Run tests with `mvn test`. No coverage thresholds are configured.

## Commit & Pull Request Guidelines
- Git history shows no enforced convention; use clear, imperative messages (e.g., “Add route validation”) instead of vague “fix”.
- PRs should include: a short summary, test steps (commands run), and any configuration or migration changes (e.g., new SQL in `src/main/resources/db/migration/`).

## Configuration & Security Notes
- Edit `src/main/resources/application.properties` for `server.port`, `db.url`, and `jwt.secret`.
- Do not commit real secrets; use local overrides or environment-specific values when running in production.
