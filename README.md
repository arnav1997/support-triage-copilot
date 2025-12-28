# Support Triage Copilot

A lightweight support ticket management app built as a fast-paced portfolio project.

- **Backend:** Java + Spring Boot + JPA + Flyway + Postgres  
- **Frontend:** React + TypeScript (Vite)  
- **Workflow:** Create tickets → manage ticket details → add notes (activity feed) → search/filter inbox

---

## What’s built (so far)

### Day 1
- Create tickets (subject, requester email, body, status, priority, category, tags)
- Inbox view (ticket list)
- Ticket detail view

### Day 2
- Ticket detail **edit workflow** (status/priority/category/tags)
- Notes (Activity feed)
  - `GET /api/tickets/{id}/notes`
  - `POST /api/tickets/{id}/notes`
- Inbox **search + filter**
  - `GET /api/tickets?status=&q=`

### Day 3
- AI triage (local LLM via **Ollama**)
  - `POST /api/ai/triage`
  - Returns structured JSON: **category**, **priority**, **tags**, **rationale**, **entities**, **aiRunId**
  - Persists each run in `ai_runs` (input/output stored as **jsonb**)

---

## Tech stack

### Backend
- Java (Spring Boot)
- Spring Data JPA
- Flyway migrations
- Postgres 16 (Docker Compose)
- REST APIs

### Frontend
- React + TypeScript
- Vite
- React Router

---

## Prerequisites

- Java 17+ (or whatever your Spring Boot build targets)
- Node.js 18+ recommended
- Docker + Docker Compose
- Ollama installed + model pulled:
  - `ollama pull llama3.2`
  - Ollama running locally (default: `http://localhost:11434`)


---

## Quickstart (local dev)

### 1) Start Postgres (Docker)
From repo root:

```bash
docker compose up -d
```

If you ever want a clean reset:

```bash
docker compose down -v
docker compose up -d
```

### 2) Run Backend (Spring Boot)
From the backend folder:

```bash
./mvnw spring-boot:run
```

Backend: `http://localhost:8080`

> Flyway migrations run automatically on startup.

> Make sure Ollama is running locally and the model is available (e.g., `llama3.2`) before calling `/api/ai/triage`.

### 3) Run Frontend (Vite)
From the frontend folder:

```bash
cd support-triage-frontend
npm install
npm run dev
```

Frontend: `http://localhost:5173`

---

## API

### Tickets
- `GET /api/tickets?status=&q=`  
  - `status` is **case-insensitive** (`open`, `OPEN`, etc.)
  - `q` searches across: **subject**, **body**, **requesterEmail**, **category**
- `POST /api/tickets`
- `GET /api/tickets/{id}`
- `PATCH /api/tickets/{id}`

### Notes (Activity feed)
- `GET /api/tickets/{id}/notes` (newest first)
- `POST /api/tickets/{id}/notes`

### AI
- `POST /api/ai/triage`
  - Body: `{ "ticketId": number }`
  - Response: `{ category, priority, tags, rationale, entities, aiRunId }`

---

## Example requests

### Create a ticket
```bash
curl -X POST http://localhost:8080/api/tickets \
  -H "Content-Type: application/json" \
  -d '{
    "subject":"Can’t log in",
    "requesterEmail":"customer@example.com",
    "body":"Getting an error when I try to sign in.",
    "priority":"HIGH",
    "category":"AUTH",
    "tags":["login","sev2"]
  }'
```

### Update a ticket
```bash
curl -X PATCH http://localhost:8080/api/tickets/1 \
  -H "Content-Type: application/json" \
  -d '{
    "status":"IN_PROGRESS",
    "priority":"URGENT",
    "category":"AUTH",
    "tags":["login","sev1"]
  }'
```

### Add a note
```bash
curl -X POST http://localhost:8080/api/tickets/1/notes \
  -H "Content-Type: application/json" \
  -d '{"type":"note","body":"Investigating this now."}'
```

### Search tickets
```bash
curl "http://localhost:8080/api/tickets?status=open&q=auth"
```

### AI triage a ticket
```bash
curl -X POST http://localhost:8080/api/ai/triage \
  -H "Content-Type: application/json" \
  -d '{ "ticketId": 1 }'
```

Example Response:
```json
{
  "category": "bug",
  "priority": "HIGH",
  "tags": ["login"],
  "rationale": "…",
  "entities": {
    "requesterEmail": null,
    "orderId": null,
    "product": null,
    "errorCode": null
  },
  "aiRunId": 5
}
```

---

## Development notes

- Notes are modeled as an **append-only activity log** (simple + extensible).
- Ticket status filtering is parsed case-insensitively; invalid statuses return **400**.
- The Notes API returns **DTOs** (not entities) to avoid tight coupling between persistence and API.

---

## Roadmap (next)

- AI runs panel in Ticket Detail (summarize `ai_runs` for a ticket)
- System-generated notes (e.g., “status changed from OPEN → RESOLVED”)
- Pagination for inbox + notes
- Auth (agents/users), note visibility (internal vs customer-facing)
- Better tags UX + tag querying

---

## License

MIT License

Copyright (c) 2025 Arnav Mahapatra

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
