Expense Tracker (Spring Boot + JDBC + MySQL)
-----------------------------------------------------------------------
build with 
$ & "$env:USERPROFILE\scoop\apps\maven\current\bin\mvn.cmd" package -DskipTests

$procs = Get-CimInstance Win32_Process | Where-Object { $_.CommandLine -like '*expenses-tracker-0.0.1-SNAPSHOT.jar*' }; if($procs){ $procs | ForEach-Object { Stop-Process -Id $_.ProcessId -Force } }; & "$env:USERPROFILE\scoop\apps\maven\current\bin\mvn.cmd" -DskipTests clean package

 run with 
$env:GEMINI_API_KEY = "AIzaSyBQ1qqJICZ4lBpKsZmBc_NUr6STO-Gsklg"; $env:GEMINI_MODEL = "gemini-2.0-flash"; java -jar .\target\expenses-tracker-0.0.1-SNAPSHOT.jar --server.port=8081
with AI 

 SORTED!!

 
 -----------------------------------------------------------------------
Prerequisites
- JDK 17+
- Maven 3.9+
- MySQL running locally with database `expenses_tracker`

Quick Start
1. Create DB (first time only):
   - CREATE DATABASE expenses_tracker;
2. Update credentials if needed in `src/main/resources/application.properties`.
3. Run the app:
   - java -jar target/expenses-tracker-0.0.1-SNAPSHOT.jar --server.port=8080
4. Open UI:
   - http://localhost:8080/

AI Category Suggestions (optional)
----------------------------------
The UI has a Smart Categorize button that suggests a category using AI.
If you set a Gemini API key, it will use Google Gemini; otherwise it uses a local rule-based fallback.

Set environment variables before running:
- On PowerShell (Windows):
  - $env:GEMINI_API_KEY = "<your_key_here>"
  - # optional: choose a model (default: gemini-2.0-flash)
  - $env:GEMINI_MODEL = "gemini-2.0-flash"

Privacy: only the title and notes you provide are sent to the AI.

REST API
- GET /api/expenses
- GET /api/expenses/{id}
- POST /api/expenses
- PUT /api/expenses/{id}
- DELETE /api/expenses/{id}

Database Schema
- Table: expenses(id, title, amount, category, expense_date, notes)
- See `schema.sql` and sample `data.sql`.

Sprint Plan (aligned with syllabus)
- Sprint 1: Requirement & Scope, ERD, Relational Model, Normalization draft
- Sprint 2: Backend setup (Spring Boot, JDBC, MySQL), schema + CRUD repository
- Sprint 3: REST controllers, validation, CORS, seed data
- Sprint 4: Basic frontend (HTML/CSS/JS) for CRUD, client-side validation
- Sprint 5: Testing (manual + basic validation), documentation

Documentation Checklist
- Title, Abstract, Introduction, Scope
- Requirements, ER diagram, Relational schema, Normalization notes
- Data Dictionary, SQL schema
- GUI screenshots, Source code structure
- Testing document (test cases, sample data)
- Conclusion

Notes
- Credentials currently set for local dev: root/password. Change for production.
- Uses Spring JDBC (JdbcTemplate) with MySQL Connector/J.


