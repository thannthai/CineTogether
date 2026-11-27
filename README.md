# CineTogether

CineTogether is a movie-watching web application built around one idea: films are better with other people. You can
browse and watch movies on your own today, and the design extends this to private and public Watch Party rooms where
people watch the same film in sync and talk while it plays.

## Features

**Watch Alone** (implemented)

- Browse the catalog by newest, single films, series, cinema releases and anime; filter by genre, one or several
  countries, and release year; search by title.
- A home page with a hero of well-known films of the year, themed shortcuts and rows of recent releases.
- Movie details with episodes, quality and episode-progress labels, and a hover preview that plays the trailer.
- A custom HLS player: seek, play/pause, volume, playback speed, picture-in-picture, full screen, keyboard shortcuts,
  next and previous episode, and remembered preferences.
- Sign up, sign in and a profile name through Supabase Auth; light and dark themes.

**Watch Together** (designed, see `docs/`)

- Private rooms joined by invitation and public rooms discovered in a list; Host-controlled synchronized playback and
  realtime chat. The Watch Together page in the app is a preview with sample rooms.

## Tech Stack

| Area     | Technology                                                                               |
|----------|------------------------------------------------------------------------------------------|
| Backend  | Java 21, Spring Boot 4, Spring Modulith (modular monolith), Spring Data JPA, Flyway      |
| Database | PostgreSQL 17                                                                            |
| Security | Spring Security as a resource server, Supabase Auth JWT (ES256), stateless Bearer tokens |
| API      | REST with `application/problem+json` errors; native WebSocket planned for Watch Party    |
| Frontend | React 19, TypeScript, Vite, React Router, hls.js                                         |
| Tests    | JUnit and Testcontainers (real PostgreSQL) on the backend, Vitest on the frontend        |
| Build    | Gradle Kotlin DSL, npm                                                                   |

## Architecture

- **Backend** (`backend/`): a Spring Modulith application. The `identity` module owns user profiles; the `catalog`
  module
  reads movies, details and playback sources through a replaceable provider adapter, and the provider is never called
  inside a database transaction. Module boundaries are verified by a test.
- **Frontend** (`frontend/`): feature-first structure (`features/catalog`, `watch`, `auth`, `party`), each with its own
  `api/`, `components/`, `hooks/`, `lib/` and `pages/`, and one stylesheet per component.
- **Documentation** (`docs/`): product requirements, architecture, API and database design, the Watch Party design, and
  development notes.

## Getting Started

You need JDK 21, Docker, Node.js with npm, and a Supabase project for authentication. The short version:

```bash
cp backend/.env.example backend/.env                  # database password and Supabase URL
cp frontend/.env.example frontend/.env.local          # Supabase URL and key, API base URL
(cd backend && docker compose up -d)                  # PostgreSQL
(cd backend && ./gradlew bootRun)                     # API on http://localhost:8080
(cd frontend && npm ci && npm run dev)                # app on http://localhost:5173
```

The full, verified instructions are in `docs/development/setup.md`.

## Tests

```bash
(cd backend && ./gradlew test)      # needs Docker for the Testcontainers PostgreSQL
(cd frontend && npm test)
```

## Documentation

| Document                            | Content                                  |
|-------------------------------------|------------------------------------------|
| `docs/product/requirements.md`      | Scope and business rules                 |
| `docs/architecture/architecture.md` | Architecture and module boundaries       |
| `docs/architecture/conventions.md`  | Frontend structure and style conventions |
| `docs/design/database.md`           | Persistence and transaction rules        |
| `docs/design/api.md`                | REST and WebSocket contracts             |
| `docs/design/watch-party.md`        | Synchronization, lifecycle and recovery  |
| `docs/development/setup.md`         | Local development setup                  |
