# Docker Quickstart

- Requirements: Docker Desktop
- Start (first build may take a few minutes):
  ```bash
  docker compose up -d --build
  ```
- Open the app: http://localhost:8081/
- Stop the stack:
  ```bash
  docker compose down
  ```
- Optional: enable AI suggestions (Gemini)
  ```bash
  # macOS/Linux
  export GEMINI_API_KEY="<your_key_here>"
  docker compose up -d --build
  
  # Windows PowerShell
  $env:GEMINI_API_KEY = "<your_key_here>"
  docker compose up -d --build
  ```

## Hot reload (dev)
- Run the dev stack with source bind-mount and auto-compile on changes:
  ```bash
  docker compose -f docker-compose.dev.yml up --build
  ```
- Edit Java or static files locally; the app restarts automatically (Spring Boot Devtools + inotify watcher).
- Stop:
  ```bash
  docker compose -f docker-compose.dev.yml down
  ```

Notes
- The MySQL data is stored in a named volume (`mysql_data` or `mysql_data_dev`).
- The app auto-initializes schema and seed data on startup.
- Never commit real API keys. Use environment variables as shown above.
