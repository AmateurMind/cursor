# Expense Tracker – DBMS Project



Multi-user expense tracking app with AI-powered categorization (OpenAI), dark/light theme, and real-time analytics.

## Tech Stack
- **Backend:** Spring Boot (Java), MySQL
- **Frontend:** React + Vite + TypeScript + Tailwind CSS
- **AI:** OpenAI GPT-4o-mini for smart category suggestions

---

## Prerequisites
- MySQL 8.x running on `localhost:3306`
- Node.js 18+ and npm
- Java 17+
- Maven 3.6+
- OpenAI API key

---

## Setup (First Time Only)

### 1. Database
Create the database:
```sql
CREATE DATABASE expenses_tracker;
```

### 2. Backend Config
Edit `src/main/resources/application.properties`:
```properties
spring.datasource.username=root
spring.datasource.password=YOUR_MYSQL_PASSWORD
```

### 3. Environment Variables
Set your OpenAI key (PowerShell):
```powershell
$env:OPENAI_API_KEY="your-openai-key-here"
```

### 4. Install Frontend Dependencies
```powershell
cd frontend
npm install
cd ..
```

---

## Run Commands

### Start Backend (Port 8081)
```powershell
# Stop any old instances
$procs = Get-CimInstance Win32_Process | Where-Object { $_.CommandLine -like '*expenses-tracker-0.0.1-SNAPSHOT.jar*' }
if($procs){ $procs | ForEach-Object { Stop-Process -Id $_.ProcessId -Force } }

# Build and run
& "$env:USERPROFILE\scoop\apps\maven\current\bin\mvn.cmd" -DskipTests clean package
$env:OPENAI_API_KEY="your-openai-key-here"
java -jar .\target\expenses-tracker-0.0.1-SNAPSHOT.jar --server.port=8081
```

### Start Frontend (Port 5173)
Open a **new terminal**:
```powershell
cd frontend
npm run dev
```

Open: http://localhost:5173

---

## Demo Account
- Email: `demo@demo.com`
- Password: `demo123`
- Or click **"Use demo"** in the UI to auto-create/login

---

## Features
- ✅ Multi-user auth (register/login with remember me)
- ✅ Add/Edit/Delete expenses with categories
- ✅ Smart Categorize (AI suggests category from title/notes)
- ✅ Category dropdown + manual input
- ✅ Dark/Light theme toggle
- ✅ Dashboard with stats cards and pie chart
- ✅ Transaction history table
- ✅ Per-user data isolation

---

## API Endpoints
- `POST /api/auth/register` – Register user
- `POST /api/auth/login` – Login
- `GET /api/expenses?userId=` – List expenses
- `POST /api/expenses` – Create expense
- `PUT /api/expenses/{id}` – Update expense
- `DELETE /api/expenses/{id}` – Delete expense
- `POST /api/ai/suggest-category` – Smart categorize
- `GET /api/categories` – List categories

---

## Database Schema
- **users**: id, email, name, password_hash
- **categories**: id, name, type, color
- **accounts**: id, user_id, name, type, currency_code
- **expenses**: id, user_id, account_id, category_id, title, amount, category, expense_date, notes

---

## Troubleshooting

### Backend won't start
- Check MySQL is running: `mysql -u root -p`
- Verify `application.properties` has correct credentials

### Frontend errors (401/404)
- Ensure backend is running on 8081
- Check `vite.config.ts` proxy: `target: 'http://localhost:8081'`

### Data reset on restart
- Already disabled in `application.properties` (schema/data auto-init commented out)
- Your data persists between restarts now

### AI not working
- Verify `OPENAI_API_KEY` is set before starting backend
- Check backend logs for API errors

---

## Notes
- Credentials set for local dev: root/password
- Uses Spring JDBC (JdbcTemplate) with MySQL Connector/J
- Data persists between restarts (auto-init disabled)

TO BUILD AND RUN WITH OPENAI:

$procs = Get-CimInstance Win32_Process | Where-Object { $_.CommandLine -like '*expenses-tracker-0.0.1-SNAPSHOT.jar*' }; if($procs){ $procs | ForEach-Object { Stop-Process -Id $_.ProcessId -Force } }
& "$env:USERPROFILE\scoop\apps\maven\current\bin\mvn.cmd" -DskipTests clean package
$env:OPENAI_API_KEY="xyz"; java -jar .\target\expenses-tracker-0.0.1-SNAPSHOT.jar --server.port=8081