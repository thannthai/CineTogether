# CineTogether — Development Progress

## Current Phase

Authentication & User Profile

## Milestones

- [x] Planning & Design
- [x] Development Environment Setup
- [ ] Authentication & User Profile
- [ ] Movie Catalog & Watch Alone
- [ ] Watch Party Room Management
- [ ] Public Room Discovery & Preview
- [ ] WebSocket & Synchronized Playback
- [ ] Realtime Chat & Connection Recovery
- [ ] Integration Testing & Deployment

## Completed

- Initial Flyway migration `V1__initial_schema.sql` created: `app_users`, `watch_rooms`, `room_memberships`,
  `room_playback_states`, `room_chat_messages`, with the constraints and indexes from `docs/design/database.md`.
- Applied to the local PostgreSQL 17 database through the application (Flyway reported version 1 applied); resulting
  schema inspected and 36 constraint behaviour checks passed (rolled back, no data left behind).
- Backend `application.yaml` now configures the datasource, `ddl-auto: validate`, and Flyway.
- PostgreSQL Compose configuration moved to `backend/compose.yaml` with a pinned project name; the existing container
  and `cinetogether_postgres_data` volume were reused unchanged, the redundant root `docker-compose.yaml` was removed,
  and the `spring-boot-docker-compose` dependency was removed so Spring Boot never starts a second database.
- Supabase Auth environment verified (see `setup.md`): React variables present (project URL and `sb_publishable_`
  key, no secret keys); the project signs with asymmetric ES256 (JWKS has an EC P-256 key); Spring Resource Server
  configured (`issuer-uri`, `jwk-set-uri`, `jws-algorithms: ES256`) and confirmed with forged tokens. A typo in the
  backend `SUPABASE_URL` was fixed. Env files are git-ignored and placeholder templates were added for backend and
  frontend. `./gradlew test` passes.
- React + Vite + TypeScript initialized directly in `frontend/` (npm, React 19, Vite 8, oxlint; no extra libraries).
  Verified: `npm install` (0 vulnerabilities), `npm run lint`, `npm run build`, `npm run dev` serving the app on
  `http://localhost:5173`, and the three `VITE_*` variables reaching the browser code with no backend-only values or
  secret keys in the bundle. `.env.local` was preserved and stays git-ignored.
- Frontend-to-backend connectivity verified in a real browser (`reachable (HTTP 401)` from the placeholder page). Added
  minimal CORS in `SecurityConfig` (allowed origin `http://localhost:5173`, configurable) with `SecurityConfigTests`;
  `./gradlew test` passes (4 tests). Before the change a preflight was rejected without CORS headers.
- Secret check: no `.env`/`.env.local` file is tracked or would be added; only `backend/.env.example` and
  `frontend/.env.example` (placeholders) will be committed.
- Final Development Environment readiness audit performed. Fixes found by it: Testcontainers now uses `postgres:17`
  (it silently used PostgreSQL 18 via `latest`), and the Gradle test task provides a placeholder `SUPABASE_URL` (tests
  failed on a clean clone without `.env`). Quick Start added to `setup.md`.
- Removed `spring-modulith-starter-jpa` (its `event_publication` entity failed schema validation). Verified afterwards:
  `./gradlew test` passes (`contextLoads()`), and the application starts against the local database.

- Authentication Task 1, Stateless & CSRF Hardening: anonymous requests were getting a `JSESSIONID` cookie (GET through
  the saved-request cache; POST/PUT/PATCH/DELETE through the CSRF token repository). `SecurityConfig` now uses
  `SessionCreationPolicy.STATELESS` and `csrf.disable()` (bearer-only API; both must be revisited if cookie auth or a BFF
  is added). Verified with `./gradlew test` (21 tests pass): `BearerOnlySecurityTests` (MockMvc: valid Bearer passes for
  every method, invalid Bearer gives 401 `invalid_token`, anonymous requests create no session) and
  `AnonymousHttpRequestTests` (random-port real HTTP: `401`, Bearer challenge, no `Set-Cookie`). The new HTTP test was
  seen failing against the old configuration (all methods) and against `STATELESS` alone (all but GET).

- Authentication Task 2, real token verification: added `backend/http/auth.http` (IntelliJ HTTP Client sign-up and
  sign-in against the Supabase Auth REST API, calls the backend with the Bearer token; the sign-in handler logs only
  non-sensitive claim facts) and a git-ignored private environment file with a tracked `.example`. A real access token
  was inspected: `alg=ES256` with `kid`, `iss=<SUPABASE_URL>/auth/v1`, `aud=authenticated`, lifetime 3600 s, and no
  `full_name`/`name` in `user_metadata`. Details are in `setup.md`. No backend code changed.

- Authentication Task 3, audience validation: `application.yaml` now sets `audiences: authenticated`. `JwtValidationTests`
  (real decoder, local JWKS, tokens signed in the test) was seen failing first: tokens with another or no `aud` were
  accepted (404 instead of 401). After the change `./gradlew test` passes (27 tests); valid tokens pass, and wrong or
  missing `aud`, another issuer, expiry, and a wrong signing key give 401.

## Current Task

Authentication & User Profile. Development Environment Setup is complete (database, Flyway V1, backend tests, Supabase
JWT configuration, React frontend, and frontend-to-backend connectivity are all verified). Task 1 (stateless hardening)
is done; the next steps are below.

## Blockers

- WebSocket origin restrictions are not configured yet (needed with the WebSocket milestone).

## Next Task

Remaining Authentication & User Profile work (Tasks 1 to 3 are done), in order:

1. REST errors as `application/problem+json` (401/403 and `VALIDATION_ERROR`).
2. `identity` module: idempotent `app_users` upsert on authenticated requests.
3. `GET /me` and `PATCH /me`.
4. Per-endpoint Security rules for the endpoints that exist so far (public catalog and public room rules come with
   their features).
5. Minimal React login/register/logout screen (FR-AUTH-04); remove the temporary setup check from
   `frontend/src/App.tsx`.

WebSocket `AUTH` and token renewal belong to the WebSocket milestone.

## Notes

Update progress after completing meaningful tasks or milestones. Record only verified progress.