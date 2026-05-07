# Backend Setup

This backend uses SQLite/GeoPackage only. It no longer requires MySQL, H2, Docker, or a Linux server.

## Required Environment

- JDK 17+
- Maven 3.8+
- SQLite database file at `data/tourism_system.gpkg`

## Database File

The default datasource is configured in `src/main/resources/application.yml`:\n\n```yaml\nspring:\n  datasource:\n    url: ${SQLITE_URL:jdbc:sqlite:data-structure-design-backend/data/tourism_system.gpkg}\n    driver-class-name: org.sqlite.JDBC\n    hikari:\n      jdbc-url: ${SQLITE_URL:jdbc:sqlite:data-structure-design-backend/data/tourism_system.gpkg}\n```yaml
spring:
  datasource:
    url: ${SQLITE_URL:jdbc:sqlite:data/tourism_system.gpkg}
    driver-class-name: org.sqlite.JDBC
    hikari:
      jdbc-url: ${SQLITE_URL:jdbc:sqlite:data/tourism_system.gpkg}
```

This default relative path is resolved from the process working directory. It is designed for IDE run configurations whose working directory is the repository root:\n\n```powershell\nD:\\gitCode\\zhzjzz\n```\n\nIf you run Maven from the backend directory, override `SQLITE_URL` first:\n\n```powershell\ncd D:\\gitCode\\zhzjzz\\data-structure-design-backend\n$env:SQLITE_URL="jdbc:sqlite:data/tourism_system.gpkg"\nmvn spring-boot:run\n```

The database should be here:

```text
data-structure-design-backend/data/tourism_system.gpkg
```

The application also auto-detects the database before Spring Boot starts. It checks these locations first: `data/tourism_system.gpkg`, `data-structure-design-backend/data/tourism_system.gpkg`, and the old typo path `data-structure-design-backend/data/tourism_system.gpkg`. If your database is somewhere else, set `SQLITE_URL` before running:

```powershell
$env:SQLITE_URL="jdbc:sqlite:D:/your/path/tourism_system.gpkg"
mvn spring-boot:run
```

Use forward slashes in JDBC URLs on Windows to avoid escaping issues.

## Run Locally

```powershell
cd D:\\gitCode\\zhzjzz\nmvn -f data-structure-design-backend/pom.xml spring-boot:run
```

If port 8080 is occupied:

```powershell
$env:SERVER_PORT="8081"
mvn spring-boot:run
```

Or stop the process using 8080:

```powershell
netstat -ano | findstr :8080
taskkill /PID <PID> /F
```

## Common Startup Errors

### `Failed to configure a DataSource: 'url' attribute is not specified`

Usually one of these is true:

- `src/main/resources/application.yml` is missing from the active run configuration.
- The app is being started from the wrong module/directory.
- The packaged `target/classes/application.yml` is stale.

Fix:

```powershell
cd D:\gitCode\zhzjzz\data-structure-design-backend
mvn clean compile
mvn spring-boot:run
```

### `path to ... does not exist`

The SQLite JDBC URL points to a non-existent file or folder.

Fix by either placing the DB at:

```text
data/tourism_system.gpkg
```

or overriding:

```powershell
$env:SQLITE_URL="jdbc:sqlite:D:/absolute/path/tourism_system.gpkg"
```

### `Port 8080 was already in use`

Use another port:

```powershell
$env:SERVER_PORT="8081"
mvn spring-boot:run
```

or stop the old process:

```powershell
netstat -ano | findstr :8080
taskkill /PID <PID> /F
```

## API Checks

After startup:

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- Health: `http://localhost:8080/actuator/health`

## Frontend Connection

The frontend deployed on GitHub Pages should call the backend through the ngrok URL configured in the frontend `.env`:

```env
VITE_API_BASE_URL=https://your-ngrok-domain.ngrok-free.app/api
```

Do not hard-code personal ngrok URLs in source code. Keep local values in `.env`, and commit `.env.example` only.



