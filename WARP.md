# WARP.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

Project: Expense Tracker (Spring Boot + JDBC + MySQL)

Common commands
- Prerequisites
  - JDK 17+
  - Maven 3.9+
  - MySQL with database expenses_tracker (created automatically if createDatabaseIfNotExist=true remains in the JDBC URL)

- Build (skip tests)
```bash
mvn -q -DskipTests clean package
```

- Build (with tests)
```bash
mvn -q clean package
```

- Run in dev mode (no packaging)
```bash
mvn -q spring-boot:run
```

- Run the packaged app (default port from application.properties is 8081)
```bash
java -jar target/expenses-tracker-0.0.1-SNAPSHOT.jar
```

- Override port at runtime
```bash
java -jar target/expenses-tracker-0.0.1-SNAPSHOT.jar --server.port=8080
```

- Run all tests
```bash
mvn -q test
```

- Run a single test class or method (JUnit)
```bash
# Class
mvn -q -Dtest=SomeTest test
# Single method
mvn -q -Dtest=SomeTest#someMethod test
```

- Windows/PowerShell examples
```powershell
# If mvn is not on PATH (Scoop default install)
& "$env:USERPROFILE\scoop\apps\maven\current\bin\mvn.cmd" -q -DskipTests clean package

# Set AI env vars (optional; used by /api/ai/suggest-category)
$env:GEMINI_API_KEY = "{{GEMINI_API_KEY}}"
$env:GEMINI_MODEL = "gemini-2.0-flash"  # optional; defaults to gemini-2.0-flash

# Run the app (matches application.properties)
java -jar .\target\expenses-tracker-0.0.1-SNAPSHOT.jar
```

Notes on lint/format and tests
- Lint/format: Not configured in pom.xml (no Checkstyle/Spotless).
- Tests: Currently no test classes in src/test/java; mvn test will run zero tests unless tests are added.

High-level architecture
- Entry point
  - com.example.expenses.ExpensesTrackerApplication (Spring Boot 3 application)

- HTTP layer (REST controllers)
  - ExpenseController (/api/expenses): CRUD for expenses. If expenseDate is missing on create/update, it’s set to today.
  - AiController (/api/ai/suggest-category): Returns a suggested category from title/notes.
    - If GEMINI_API_KEY is set, calls Google Gemini (model via GEMINI_MODEL, default gemini-2.0-flash).
    - Without GEMINI_API_KEY, returns a rule-based fallback (no external calls).
  - UserController (/api/users): Minimal users CRUD (list, get, create).
  - AccountController (/api): Accounts CRUD and lookups
    - GET /api/accounts, GET /api/accounts/{id}
    - GET /api/users/{userId}/accounts
    - POST /api/accounts

- Persistence (JdbcTemplate)
  - ExpenseRepository: Explicit SQL for SELECT/INSERT/UPDATE/DELETE on expenses. Ensures categories lookup exists:
    - INSERT IGNORE INTO categories(name) … then resolves categoryId for the expense.
    - Maps columns via BeanPropertyRowMapper to com.example.expenses.model.Expense.
  - UserRepository and AccountRepository: Straightforward CRUD using BeanPropertyRowMapper.

- Database initialization
  - Spring SQL init is enabled:
    - schema.sql: tables users, accounts, categories, expenses with basic indexes.
    - data.sql: seed example expenses.
  - Configured via spring.sql.init.* in application.properties.

- Configuration (src/main/resources/application.properties)
  - server.port=8081 (default)
  - spring.datasource.url=jdbc:mysql://localhost:3306/expenses_tracker?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
  - spring.datasource.username=root, spring.datasource.password=password (adjust locally)
  - Logging for JdbcTemplate and parameter binding (DEBUG/TRACE)
  - CORS allowed origins: * (via property and @CrossOrigin)

- Static UI (src/main/resources/static)
  - index.html + main.js + styles.css served by Spring Boot static resources.
  - Frontend calls:
    - /api/expenses for CRUD
    - POST /api/ai/suggest-category for AI category hints
  - Uses Chart.js for simple category and time-series insights.

Important repo-specific caveats
- Port mismatch in README: README mentions 8080 in places, but application.properties defaults to 8081. You can either update README to 8081 or keep README as-is and run with --server.port=8080.
- README shows a hard-coded GEMINI_API_KEY. Replace this with a placeholder (e.g., "{{GEMINI_API_KEY}}") and avoid committing real credentials in documentation or code.
