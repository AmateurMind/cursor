Expense Tracker (Spring Boot + JDBC + MySQL)
-----------------------------------------------------------------------
build with 
& "$env:USERPROFILE\scoop\apps\maven\current\bin\mvn.cmd" -DskipTests package


run with 
 java -jar target/expenses-tracker-0.0.1-SNAPSHOT.jar --server.port=8081
 and open this http://localhost:8081/
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
   - mvn spring-boot:run
4. Open UI:
   - http://localhost:8080/

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


