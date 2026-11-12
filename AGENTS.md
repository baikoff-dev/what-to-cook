# What to Cook

## Project

- This is a Java and Spring Boot application.
- The base Java package is `io.github.baikoff.whattocook`.
- Use Maven through the included Maven Wrapper.
- The main interface language is Russian.
- PostgreSQL runs through Docker Compose.
- Flyway is the only mechanism allowed to change the database schema.
- Hibernate validates the schema and must not create or update it.

## Development workflow

- Implement features in small, understandable iterations.
- Explain important Java and Spring concepts.
- Add tests for business logic and important user scenarios.
- Run relevant tests after changes.
- Do not add dependencies without explaining their purpose.
- Do not create commits or push without an explicit request.
- Do not modify files unrelated to the current task.

## Commands

- Windows tests: `.\mvnw.cmd test`
- Windows run: `.\mvnw.cmd spring-boot:run`
- Start database: `docker compose up -d`
- Database status: `docker compose ps`
- Database logs: `docker compose logs postgres`
- Stop database: `docker compose stop`
- Unix tests: `./mvnw test`
- Unix run: `./mvnw spring-boot:run`
