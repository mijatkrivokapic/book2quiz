# book2quiz

An e-learning platform that turns **course books (PDF)** into **chapters**, converts each
chapter to **Markdown**, and uses the **Claude API** to generate knowledge-assessment
material — **structural / surface characteristics** and **quiz questions** — with a
human **review & approval** workflow.

The pedagogical model: for each chapter you identify the *structural characteristics*
(the conditions that truly determine when knowledge applies) and *surface
characteristics* (incidental details a novice might over-weight). Questions are then
generated to test the structural characteristics while systematically varying the
surface ones. Everything the AI produces starts as **PENDING** and must be **approved**
by a reviewer.

---

## Table of contents

- [Tech stack](#tech-stack)
- [Features](#features)
- [Running the app](#running-the-app)
- [Configuration](#configuration)
- [Service URLs](#service-urls)

---

## Tech stack

| Component        | Technology                                                        |
|------------------|-------------------------------------------------------------------|
| Backend          | Java 21, Spring Boot 4.1.1 (Web MVC, Data JPA, Validation), Lombok |
| AI               | Anthropic Java SDK 2.58.0 (Claude, structured outputs)            |
| PDF processing   | Apache PDFBox 3.0.3 (chapter detection & splitting)              |
| Object storage   | MinIO Java client 8.5.7                                           |
| Markdown worker  | Python 3.11, FastAPI, Docling 2.25.0, EasyOCR                     |
| Database         | PostgreSQL 15                                                     |
| Frontend         | Angular 19.2, Angular Material, `marked` 15 (Markdown rendering)  |
| Infra            | Docker Compose                                                    |

---

## Features

### Course & book management
- **Courses**: create, list, rename, delete; a course groups books (and lessons).
- **Books**: upload a **PDF** (stored in MinIO; DB keeps only the object key), rename,
  replace the file, download via a presigned URL, delete.

### Chapter extraction (async, PDFBox)
- Trigger extraction for a book; the backend downloads the PDF and detects chapters:
  1. **PDF outline / bookmarks** (preferred) — flat top-level entries → title + start page.
  2. **Heuristic fallback** — a `PDFTextStripper` that flags lines matching configurable
     patterns (`Chapter N`, `Poglavlje N`, `N. Title`) rendered in a font significantly
     larger than the body text.
  3. **Single-chapter fallback** — if nothing is detected, the whole book becomes one
     chapter, flagged for review.
- The PDF is **split into one PDF per chapter** (`books/{bookId}/chapters/{n}.pdf`).
- Runs on a background executor; the book/chapter tracks status
  (`PENDING / PROCESSING / DONE / FAILED`); the UI polls and shows progress; failed
  chapters can be **retried**.

### Markdown conversion (async, Docling worker)
- Each chapter PDF is sent to the Python worker, converted to Markdown (with **OCR**
  fallback for scanned pages), and stored at `books/{bookId}/chapters/{n}.md`.
- Configurable worker URL, generous timeout, and retries with exponential backoff.

### Chapter detail page
- A view **toggle**: **Content** / **Characteristics** / **Questions**.
- **Content**: rendered Markdown (via `marked`, sanitized) with an **edit** mode
  (live side-by-side preview) that saves back to MinIO; download the chapter PDF.
- **Manual chapter creation**: add a chapter by **uploading a PDF** (converted to
  Markdown in the background) or by **typing Markdown** directly.
- **Rename** and **delete** chapters (delete also removes the MinIO objects).

### Characteristics (per chapter)
- **Structural** and **surface** characteristics: full CRUD, inline editing.
- **AI generation** (async): generate both lists from the chapter's Markdown in one
  Claude call; results are added as **PENDING**.
- **Approval workflow**: each item shows PENDING/APPROVED; approve or revert. Manually
  added items are APPROVED by default.

### Constraints (per chapter)
- Per-chapter constraints (e.g. “questions must cite the source page”): CRUD. Fed into
  quiz generation as *local constraints*.

### Questions (per chapter)
- Three question types (polymorphic):
  - **MULTIPLE_CHOICE** — text, distractors, correct option, feedback, hints.
  - **MULTIPLE_RESPONSE** — text, options (each with `isCorrect`, feedback, hints), hints.
  - **SHORT_ANSWER** — text, acceptable answers, feedback, hints.
- **Manual** create/edit/delete via a type-aware editor dialog.
- **AI generation** (async) from the chapter's Markdown + its characteristics +
  constraints; generated questions are **PENDING**.
- **Approval workflow**: approve / revert per question; manual questions are APPROVED.
- **Structured outputs** guarantee schema-valid JSON; a `QuizValidator` enforces
  semantic rules (no duplicate distractors, at least one correct response, etc.).

---

## Running the app

### Prerequisites
- **Docker** and **Docker Compose**.
- An **Anthropic API key** (only needed for AI generation) — get one at
  <https://console.anthropic.com/>.

### 1. Configure environment

Copy the example env file and fill in values:

```bash
cp .env.example .env
```

Edit `.env`:

```dotenv
POSTGRES_DB_NAME=book2quizdb
POSTGRES_USER=postgres
POSTGRES_PASSWORD=change-me

MINIO_INTERNAL_URL=http://minio:9000
MINIO_EXTERNAL_URL=http://localhost:9000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin123

PGADMIN_DEFAULT_EMAIL=admin@admin.com
PGADMIN_DEFAULT_PASSWORD=admin

# Required for quiz / characteristic generation. Leave as-is to run everything else.
ANTHROPIC_API_KEY=sk-ant-...
```

> The API key is read **only** from `ANTHROPIC_API_KEY`. Without it the app still starts
> and every non-AI feature works; only *Generate* actions fail with a clear error.

### 2. Build & start everything

```bash
docker compose up -d --build
```

First build is slow: the **markdown-worker** image installs Docling + a CPU build of
PyTorch (a few hundred MB). Subsequent starts are fast.

### 3. Open the app

- Frontend: <http://localhost:4200>

Then, a typical first run:
1. Create a **Course**.
2. Open it and **Add a book** (upload a PDF).
3. On the book, open **Chapters** and click **Extract chapters** — watch the statuses go
   `PROCESSING → DONE` (chapter PDFs split, then Markdown converted).
4. Open a chapter → **Characteristics** → **Generate** (needs `ANTHROPIC_API_KEY`),
   then review/approve.
5. Open a chapter → **Questions** → **Generate**, then review/approve.

### Stopping / resetting

```bash
docker compose down          # stop
docker compose down -v       # stop AND wipe DB + MinIO volumes (fresh start)
```

---

## Configuration

### Environment variables (`.env` / compose)

| Variable | Purpose |
|---|---|
| `POSTGRES_DB_NAME`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | PostgreSQL database |
| `MINIO_INTERNAL_URL` | MinIO URL used by backend & worker inside the compose network (`http://minio:9000`) |
| `MINIO_EXTERNAL_URL` | MinIO URL used to build presigned download links (`http://localhost:9000`) |
| `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY` | MinIO credentials |
| `ANTHROPIC_API_KEY` | Claude API key (AI generation) |
| `PGADMIN_DEFAULT_EMAIL`, `PGADMIN_DEFAULT_PASSWORD` | pgAdmin login |

### Backend application properties (with defaults)

Everything about the model call and prompts is configurable (nothing hardcoded). Key
groups in `backend/src/main/resources/application.properties`:

```properties
# Quiz generation (Claude)
quiz.prompt-version=v1
quiz.max-material-chars=100000
quiz.anthropic.model=claude-opus-5
quiz.anthropic.max-tokens=16000
# quiz.anthropic.temperature is intentionally unset (Opus/Sonnet 5 reject it)
quiz.anthropic.timeout=5m
quiz.anthropic.max-retries=2
quiz.anthropic.structured-outputs.enabled=true
quiz.anthropic.structured-outputs.beta-header=
quiz.prompt.system-file=classpath:prompts/quiz-system.txt
quiz.prompt.base-constraints-file=classpath:prompts/quiz-base-constraints.txt
quiz.prompt.schema-file=classpath:prompts/quiz-schema.json

# Characteristic generation (shares the Anthropic model settings above)
characteristics.prompt-version=v1
characteristics.max-material-chars=100000
characteristics.system-file=classpath:prompts/characteristics-system.txt
characteristics.schema-file=classpath:prompts/characteristics-schema.json

# Heuristic chapter detection (used when a PDF has no outline)
chapter-detection.font-size-ratio=1.3
chapter-detection.patterns[0]=^chapter\\s+\\d+.*
chapter-detection.patterns[1]=^poglavlje\\s+\\d+.*
chapter-detection.patterns[2]=^\\d+\\.\\s+\\S.*

# Markdown worker client
markdown-worker.base-url=${MARKDOWN_WORKER_URL:http://markdown-worker:8000}
markdown-worker.read-timeout=10m
markdown-worker.retries=2
```

### Editing prompts

The prompt material lives in `backend/src/main/resources/prompts/`:

| File | Role |
|---|---|
| `quiz-system.txt` | System prompt for quiz generation (role, procedure, question schemas) |
| `quiz-base-constraints.txt` | Global constraints, one per line (`#` = comment) |
| `quiz-schema.json` | JSON schema for structured outputs (question types via `anyOf`) |
| `characteristics-system.txt` | System prompt for characteristic generation |
| `characteristics-schema.json` | JSON schema for characteristic output |

To use external files in production without a rebuild, point the properties at
`file:/path/...` instead of `classpath:...`.

---

## Service URLs

| Service | URL | Notes |
|---|---|---|
| Frontend | <http://localhost:4200> | Main UI |
| Backend API | <http://localhost:8080/api> | REST |
| Markdown worker | <http://localhost:8000> | `GET /health`, `POST /convert` |
| MinIO console | <http://localhost:9001> | login = `MINIO_ACCESS_KEY` / `MINIO_SECRET_KEY` |
| MinIO S3 API | <http://localhost:9000> | |
| pgAdmin | <http://localhost:5050> | login = `PGADMIN_DEFAULT_*` |
| PostgreSQL | `localhost:5432` | |

---
