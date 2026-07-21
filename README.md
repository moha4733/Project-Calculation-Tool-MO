# Project Calculation Tool

Et API-first projektstyrings- og kalkulationsværktøj til Alpha Solution.

Systemet er migreret væk fra legacy Thymeleaf/MVC flows og bruger nu Spring Boot REST API, JPA, JWT-auth og en vanilla HTML/CSS/JS frontend, som serveres fra `/`.

## Funktioner

- Login og registrering via REST API.
- JWT-baseret adgang til beskyttede API endpoints.
- Projektledere kan oprette og administrere projekter, subprojekter, tasks, subtasks og projektmedlemmer.
- Team members kan se projekter de er medlem af.
- Team members kan opdatere status og noter på egne tasks/subtasks.
- Project member dropdowns viser kun relevante medarbejdere.
- Vanilla frontend ligger separat i `frontend/` og deployes fra `src/main/resources/static`.

## Tech Stack

| Teknologi | Brug |
| --- | --- |
| Java 17 | Backend runtime |
| Spring Boot | REST API og static frontend hosting |
| Spring Security | JWT-baseret API security |
| Spring Data JPA | Databaseadgang |
| MySQL | Docker/prod database |
| H2 | Lokal testprofil |
| Vanilla HTML/CSS/JS | Frontend |
| Docker Compose | Lokal app + database |

## Lokal Kørsel

### Med Docker Compose

Kopier eventuelt miljøvariablerne:

```bash
cp .env.example .env
```

Start app og MySQL:

```bash
docker compose up --build
```

Åbn appen:

```text
http://localhost:8080/
```

MySQL kører som standard på port `3306`, og data gemmes i Docker volume `mysql_data`.

Stop miljøet:

```bash
docker compose down
```

Stop og slet lokal database-volume:

```bash
docker compose down -v
```

### Uden Docker

Kør tests:

```bash
./mvnw test
```

Kør lokalt med H2:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=h2
```

Kør lokalt med MySQL:

```bash
DB_URL="jdbc:mysql://localhost:3306/calculation_tool?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC" \
DB_USERNAME="calculation_user" \
DB_PASSWORD="calculation_password" \
./mvnw spring-boot:run -Dspring-boot.run.profiles=mysql
```

## API

De vigtigste API scopes:

- `/api/auth`
- `/api/employees`
- `/api/projects`
- `/api/projects/{projectId}/members`
- `/api/projects/{projectId}/subprojects`
- `/api/projects/{projectId}/subprojects/{subProjectId}/tasks`
- `/api/projects/{projectId}/subprojects/{subProjectId}/tasks/{taskId}/subtasks`

## Verifikation

Seneste fulde testkørsel:

```text
Tests run: 75, Failures: 0, Errors: 0, Skipped: 0
```

Derudover er hovedflowet smoke-testet i browser på `http://localhost:8080/`.

## Contributors

- [@aden0020](https://github.com/Aden0020)
- [@aljamour](https://github.com/aljamour)
- [@Junes2003](https://github.com/Junes2003)
- [@moha4733](https://github.com/moha4733)
