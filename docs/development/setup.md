# CineTogether — Local Development Setup

Only commands that were actually run and observed are listed as verified. Anything else is marked as unverified.

## Prerequisites

- JDK 21 toolchain (Gradle resolves it; the shell default was JDK 22, which is fine for launching Gradle).
- Docker (for local PostgreSQL and for Testcontainers-based tests).
- Node.js and npm for the frontend (verified with Node 24.3 and npm 11.4; npm is the project package manager, see
  `frontend/package-lock.json`).

## Quick Start (fresh clone)

From the repository root:

```bash
cp backend/.env.example backend/.env                  # set DB_PASSWORD and SUPABASE_URL (see "Supabase Auth")
cp frontend/.env.example frontend/.env.local          # set the three VITE_* values
(cd backend && docker compose up -d)                  # PostgreSQL 17
(cd backend && ./gradlew bootRun)                     # Flyway migrates, API on :8080
(cd frontend && npm ci && npm run dev)                # UI on :5173
```

Run `bootRun` and `npm run dev` in separate terminals. `DB_PASSWORD` is applied to PostgreSQL the first time its volume
is created; if a volume already exists, use the password it was created with.

Verification status of this path: the clone-equivalent steps that do not need secrets were executed on a clean copy of
the committable files (`./gradlew test` without any `.env`, and `npm ci`, `npm run lint`, `npm run build` in
`frontend/`). The database, `bootRun`, and `npm run dev` steps were verified on the existing environment, not on a
brand-new database volume (creating one would require a second PostgreSQL instance).

## Database

PostgreSQL 17 runs from `backend/compose.yaml` (database `cinetogether`, user `cinetogether`, bound to
`127.0.0.1:5432`, data in the `postgres_data` volume). The file pins `name: cinetogether`, so the project is
`cinetogether`, the container is `cinetogether-postgres-1`, and the volume is `cinetogether_postgres_data` no matter
which directory holds the file. Do not remove that `name:` line: without it Compose would derive the project name
`backend` from the directory and create a new, empty volume.

Compose reads the password from `backend/.env`, which sits next to `compose.yaml`, so no `--env-file` is needed.
Run from `backend/`:

```bash
docker compose up -d      # start (idempotent; reuses the existing container and volume)
docker compose ps         # status
docker compose stop       # stop, keeps the data
```

- Verified: `config`, `up -d --dry-run`, and `ps` run from `backend/` without `--env-file` (they resolve
  `DB_PASSWORD` from `backend/.env`), reuse the existing container (same container ID and start time, no new volume),
  and `ps` shows it healthy on `127.0.0.1:5432->5432/tcp`. The Flyway history and tables were intact.
- Unverified: `stop`, and starting the database from a stopped state.
- Never use `docker compose down -v`: it deletes the data volume.

The `spring-boot-docker-compose` dependency was removed, so Spring Boot never starts containers itself: always start
the database manually with the commands above before `./gradlew bootRun`.

## Configuration

Create `backend/.env` (git-ignored; copy `backend/.env.example`) with:

```text
DB_URL=jdbc:postgresql://localhost:5432/cinetogether
DB_USERNAME=cinetogether
DB_PASSWORD=<local password>
SUPABASE_URL=https://<project-ref>.supabase.co
```

`backend/src/main/resources/application.yaml` reads `DB_URL` (defaulting to the URL above), `DB_USERNAME`, and
`DB_PASSWORD`. Hibernate runs with `ddl-auto: validate`; **Flyway owns the schema**.

`application.yaml` imports `backend/.env` through `spring.config.import: optional:file:.env[.properties]`, resolved
against the working directory, so the application must be started from `backend/` (as Gradle does). Real environment
variables take precedence over the file. In IntelliJ, set the Run Configuration's working directory to `backend/` or define
the variables there (unverified).

Without the file or the variables, startup fails with `password authentication failed for user "${DB_USERNAME}"`
(the placeholder is not resolved).

## Supabase Auth

Supabase handles login/registration in the React app; the Spring Boot backend only validates the access token (JWT)
as an OAuth2 Resource Server. No Supabase secret is needed anywhere in this repository.

### Variables

| File (git-ignored)      | Variable                         | Purpose                                                   |
|-------------------------|----------------------------------|-----------------------------------------------------------|
| `frontend/.env.local`   | `VITE_SUPABASE_URL`              | Project URL used by the React client                      |
| `frontend/.env.local`   | `VITE_SUPABASE_PUBLISHABLE_KEY`  | Public client key (`sb_publishable_...`), safe for browsers |
| `frontend/.env.local`   | `VITE_API_BASE_URL`              | Backend base URL (`http://localhost:8080/api/v1`)         |
| `backend/.env`          | `SUPABASE_URL`                   | Same project URL; used to derive issuer and JWKS URL      |

Templates with placeholders are in `backend/.env.example` and `frontend/.env.example` (these are tracked).
**`SUPABASE_URL` and `VITE_SUPABASE_URL` must be identical.** A one-character typo in the backend value was found and
fixed during verification (the host did not resolve), which would have broken issuer and JWKS lookups.

Never put the service-role/secret key, the legacy JWT secret, or database passwords in any `VITE_*` variable (they are
bundled into the browser code) or commit them. `.env`, `.env.*` (except `.env.example`) and `frontend/.env.local` are
git-ignored; verified with `git check-ignore`, and no env file is tracked.

### JWT signing algorithm and JWKS (verified)

Evidence from the project's public endpoints (no secret used):

- `GET <SUPABASE_URL>/auth/v1/.well-known/jwks.json` returns HTTP 200 with one key: `kty=EC`, `crv=P-256`,
  `alg=ES256`, `use=sig`. So the project signs with an **asymmetric ES256** key and exposes it via JWKS.
- `GET <SUPABASE_URL>/auth/v1/.well-known/openid-configuration` gives `issuer = <SUPABASE_URL>/auth/v1` and
  `jwks_uri = <SUPABASE_URL>/auth/v1/.well-known/jwks.json`.

### Real access token (verified)

An access token obtained by signing in a throwaway test account with email and password (see "Getting a token without
the frontend" below) was inspected. Only non-sensitive facts are recorded here:

| Item                   | Observed value                                                              |
|------------------------|-----------------------------------------------------------------------------|
| `alg` (header)         | `ES256`, with a `kid` present (matches the JWKS key)                        |
| `iss`                  | `<SUPABASE_URL>/auth/v1` (matches `issuer-uri`)                             |
| `aud`                  | `authenticated`                                                             |
| `role`                 | `authenticated`                                                             |
| Lifetime               | 3600 seconds (`exp - iat`)                                                  |
| Claim names            | `iss, sub, aud, exp, iat, email, phone, app_metadata, user_metadata, role, aal, amr, session_id, is_anonymous` |
| `user_metadata` keys   | `email, email_verified, phone_verified, sub`, with no `full_name` or `name` |

Consequences: audience validation uses `authenticated`, and for email/password sign-ups the initial `display_name`
falls back to the email local part (`docs/design/database.md` §3.1), because no name claim exists.

This project requires email confirmation: a new account can sign in only after its confirmation link is opened.
Supabase also rejects example domains such as `example.com` as invalid addresses.

### Getting a token without the frontend

`backend/http/auth.http` (IntelliJ HTTP Client; Postman can do the same calls) signs up and signs in against the
Supabase Auth REST API using only the publishable key, then calls the backend with `Authorization: Bearer`. Setup:

```bash
cp backend/http/http-client.private.env.json.example backend/http/http-client.private.env.json   # git-ignored
```

Fill in the project URL, the publishable key, and a throwaway test account (a real mailbox you can read, for example a
`+` alias, and a throwaway password), then pick the `dev` environment in the editor. The sign-in request's handler stores
the token in a client variable and logs only non-sensitive facts (`alg`, `kid` present, `iss` match, `aud`, `role`,
`exp`, claim names) in the Response Handler tab. Never paste the Response tab anywhere: it contains the access and
refresh tokens, and IntelliJ also saves responses under `.idea/httpRequests/` (git-ignored).

### Spring Boot configuration (prepared, verified at startup)

`application.yaml` sets `spring.security.oauth2.resourceserver.jwt` with `issuer-uri`, `jwk-set-uri`, and
`jws-algorithms: ES256`. The algorithm must be listed because Spring accepts only RS256 by default. Verified by running
the app and sending forged tokens (valid structure, wrong signature):

| Request                         | Result                                                                             |
|---------------------------------|------------------------------------------------------------------------------------|
| No token                        | `401`, `WWW-Authenticate: Bearer`                                                  |
| Forged `ES256` token (known kid)| `401 invalid_token`: `Signed JWT rejected: Invalid signature` (JWKS key was used)  |
| Forged `RS256` token            | `401 invalid_token`: `Another algorithm expected, or no matching key(s) found`     |

Catalog: `GET /api/v1/catalog/home`, `GET /api/v1/movies` and `GET /api/v1/movies/{providerKey}/{movieId}` are public;
`GET /api/v1/movies/{providerKey}/{movieId}/playback?episodeId=` needs a token. Movies come through `KKPhimProvider`. The provider is configured
in `application.yaml` (`app.catalog.kkphim.base-url`, default `https://phimapi.com`, with `connect-timeout` 3 s and
`read-timeout` 5 s). Verified by running the application and calling it with `curl`: the newest list, a type, genre and
country filter, paging, and a search containing `&` all returned real data; `size=51`, `type=BOOK` and `page=abc` gave
`400` (`VALIDATION_ERROR`), and `/api/v1/me` was still `401`. Also verified against the real provider: home, details, unknown movie (404), wrong provider key (404), and `playback`
without a token (401). With the provider address pointed at a closed port, the public endpoints answered `503`
(`PROVIDER_UNAVAILABLE`) at once. A real HLS link (master playlist, variant playlist and a `.ts` segment) answered `200`/`206` with
`Access-Control-Allow-Origin: *`, so a browser on another origin can read it (a plain GET needs no preflight). Not yet
checked: a slow provider, and playing the stream in a browser. The real provider is
sometimes slow or fails (one `503` after about 3 s, one fast `503` on `playback`); the cause of every `503` is logged
as a `WARN` line, while the client only sees the fixed message. Opening an `.m3u8` link directly in desktop Chrome shows
an empty player: Chrome cannot play HLS without a library such as hls.js.

Profile endpoints: `GET /api/v1/me` returns the caller's `id`, `displayName` and `createdAt`; `PATCH /api/v1/me`
renames the caller (200 with the updated profile, 400 `VALIDATION_ERROR` for a bad name or body, see
`docs/design/api.md` §2.1). The user is always the token's subject. `Profiles` takes a `java.time.Clock` (bean in
`ClockConfig`) so tests can set `updated_at` deterministically; the DB clock (`created_at`) and the JVM clock can differ.
Checked with a real token (existing account): sign-in, `GET /me` (200) and `playback` (200). Not yet checked: a new account
whose sign-up carries `data.display_name` (re-registering an existing email does not change its metadata; the sign-up
response then has `identities: []`), and `PATCH /me`.

The API is stateless and bearer-only: `SecurityConfig` sets `SessionCreationPolicy.STATELESS` and disables CSRF, so the
server never creates an `HttpSession` or sets a `JSESSIONID` cookie, even for rejected anonymous requests. Both settings
must be revisited together if cookie-based authentication (for example a BFF) is ever introduced. Verified with real
HTTP requests (see Tests): anonymous GET/POST/PUT/PATCH/DELETE return `401` with `WWW-Authenticate: Bearer` and no
`Set-Cookie`.

`SUPABASE_URL` has no default, so a missing value breaks startup of the security configuration. CI must provide it.
Audience validation is on: `audiences: authenticated` makes Spring Boot add an audience validator to the decoder (it
checks `aud` only when this property is set), so tokens with another audience or none are rejected. Verified by
`JwtValidationTests`, which runs the real decoder against a local JWKS and signed test tokens: a valid token passes;
a wrong or missing `aud`, another issuer, an expired token, and a token signed with another key all give `401`. The
`/me` endpoint is not implemented yet.

Profile provisioning: after the bearer token is validated, `ProfileProvisioningFilter` (in `config`) calls
`ProfileProvisioner.ensureProfile` (the public entry point of the `identity` module), which inserts the caller's
`app_users` row with `INSERT ... ON CONFLICT (id) DO NOTHING` in its own short transaction. An existing profile is never
changed, so a name edited later is kept; anonymous and invalid-token requests create nothing; a database failure stops
the request. The initial name is `user_metadata.display_name` from the token (cleaned: trimmed, at most 80 characters),
else the email local part, else `Viewer-` plus the first 6 characters of the user ID. `user_metadata` is editable by the
client, so it only suggests a name. Verified by `DisplayNamesTests`, `UserProfilesTests` (including 8 concurrent calls
for one user, repeated for 20 rounds), and `ProfileProvisioningTests`; the concurrency test was seen failing against a
check-then-insert implementation. Not yet checked with a real token carrying `display_name`: sign up a new test
account with `data.display_name` (see `backend/http/auth.http`).

A missing or invalid token gets `401` with an `application/problem+json` body (`code: UNAUTHENTICATED`, see
`docs/design/api.md` §1.2), the same for every reason, plus the standard `WWW-Authenticate: Bearer` header (with
`error="invalid_token"` for a bad token). Verified by `ProblemJsonErrorTests` through MockMvc only; the body has not been
checked over real HTTP.

### Pitfall: stale application processes

`bootRun` started with a timeout or Ctrl+C can leave the JVM running and holding port 8080, which then answers with
old configuration. If behavior looks stale, find the process that is *listening* (not merely connected, which would
also match browsers or curl) and stop only that one:

```bash
lsof -nP -iTCP:8080 -sTCP:LISTEN
kill <PID>
```

## Frontend (React + Vite + TypeScript)

The app lives directly in `frontend/` (no nested folder). Stack: React 19, Vite 8, TypeScript, React Router, oxlint,
Vitest for pure-logic unit tests; no UI framework or Supabase client yet (added with the Authentication milestone).

Setup and run, from `frontend/`:

```bash
cp .env.example .env.local     # then fill in the real values (git-ignored)
npm install
npm run dev                    # http://localhost:5173 (port is fixed with strictPort)
npm run lint
npm test                       # Vitest (pure logic only; screens are checked in the browser)
npm run build                  # type-checks (tsc -b) then builds to dist/
```

Verified: `npm run lint` (clean), `npm test` (14 tests pass), `npm run build` (success), and the app in Chrome (header,
dropdowns, mobile drawer, light and dark theme, deep links such as `/together`).

Structure (feature-first, see `docs/architecture/conventions.md`): `src/main.tsx` (entry), `src/app/` (router, layout,
header, footer, placeholder pages), `src/features/catalog/` (filter options and URL helpers so far), `src/shared/`
(`ui/` icons, `lib/` theme hook), `src/styles/` (design tokens and base styles), `src/config/env.ts` (validated access
to the `VITE_*` variables; fails fast with a clear message if one is missing), `src/vite-env.d.ts` (variable typings).

Environment variables (`frontend/.env.local`): `VITE_SUPABASE_URL`, `VITE_SUPABASE_PUBLISHABLE_KEY`,
`VITE_API_BASE_URL`. Verified that all three reach the browser code (present in the production bundle and injected in
dev), and that no backend-only value (database password, `SUPABASE_URL` from `backend/.env`) or secret-key marker does.
Only `VITE_*` variables are exposed to the browser, so never put secrets there.

### Backend connectivity and CORS

The frontend at `http://localhost:5173` calls the API at `http://localhost:8080/api/v1`, a different origin, so the
backend allows it. `SecurityConfig` (`com.thanh.cinetogether.config`) enables CORS for `/api/**` for the origins in
`app.cors.allowed-origins` (default `http://localhost:5173`, override with `CORS_ALLOWED_ORIGINS`; comma-separated), with
methods GET/POST/PATCH/PUT/DELETE/OPTIONS and headers `Authorization`/`Content-Type`, and no credentials (bearer tokens,
no cookies). The same class keeps the existing rule that every request needs a valid JWT; per-endpoint public rules come
with their features.

Verified:

- Before the change, a preflight from `localhost:5173` got `401` with no `Access-Control-Allow-Origin` (a browser
  would block it).
- After: preflight from `http://localhost:5173` returns `200` with `Access-Control-Allow-Origin`; an unknown origin gets
  `403`; a request without a token gets `401` with `WWW-Authenticate: Bearer`.
- Automated: `SecurityConfigTests` covers the two CORS cases (the missing-token case is covered by other tests).
- In a real browser (Chrome), the placeholder page at `http://localhost:5173` showed the Supabase variables as configured
  and `Backend (http://localhost:8080/api/v1): reachable (HTTP 401)`. The 401 is expected without a login.

The setup-check block in `App.tsx` is temporary and should be removed when real screens are built. WebSocket origin
rules are separate and not configured yet.

## Database Migrations (Flyway)

Migrations live in `backend/src/main/resources/db/migration/` and run automatically when the application starts.
Never edit an applied migration; add a new `V<n>__description.sql`.

Verified command (from `backend/`):

```bash
./gradlew bootRun
```

Verified: with this command and no `DB_*` variables exported in the shell, no new container appeared, Flyway reported
`Schema "public" is up to date`, and the application started. `./gradlew test` also passed.

Observed result on an empty database (a later run reports `Schema "public" is up to date` and the application starts):

```text
Migrating schema "public" to version "1 - initial schema"
Successfully applied 1 migration to schema "public", now at version "v1"
```

### Verify the schema

```bash
docker exec cinetogether-postgres-1 psql -U cinetogether -d cinetogether \
  -c "select version, description, success from flyway_schema_history" -c "\dt"
```

Verified result after `V1__initial_schema.sql`: `app_users`, `watch_rooms`, `room_memberships`,
`room_playback_states`, `room_chat_messages`, plus `flyway_schema_history`. Primary keys, foreign keys, unique and
check constraints, the `visibility` default, the chat identity column, and the five partial/composite indexes from
`docs/design/database.md` were present, and 36 constraint behaviour cases (valid and invalid rows, rolled back) passed.

## Tests

```bash
cd backend && ./gradlew test
```

Verified: 161 tests pass (auth and profile, plus catalog: `PagingTests`, `KKPhimMappingTests`, `KKPhimProviderTests`,
the endpoint tests `MoviesEndpointTests`, `MovieDetailEndpointTests`, `HomeEndpointTests`, `PlaybackEndpointTests`, and
`CatalogAccessRulesTests`; plus `ModularityTests`). The earlier 4-test run also passed on a
clean copy of the repository with no `.env` (the Gradle test task supplies a placeholder `SUPABASE_URL`, so tests never
depend on a developer's `.env`); the clean-copy run was not repeated for the later suites. The default `contextLoads()` test
was removed: every full-application test already needs the same context.

Run one class with `./gradlew test --tests 'com.thanh.cinetogether.config.BearerOnlySecurityTests'`.
`BearerOnlySecurityTests` uses `@WebMvcTest` (no database, no Docker) with a mocked `JwtDecoder`;
`JwtValidationTests` uses `@WebMvcTest` with the real decoder, a local JWKS server, and tokens signed in the test;
`ProblemJsonErrorTests` uses `@WebMvcTest` with a mocked `JwtDecoder` to check the 401 response body;
`AnonymousHttpRequestTests` starts the full application on a random port and sends real HTTP requests;
`UserProfilesTests`, `ProfileProvisioningTests` and `MeEndpointTests` run the full application against the
Testcontainers PostgreSQL; the web-slice tests use the `@SecurityWebMvcTest` annotation, which keeps feature modules out;
the catalog endpoint tests run the full application with a mocked `MovieProvider`, so it never calls the real provider (the
provider tests use `FakeProviderServer`, a local server with canned JSON); `DisplayNamesTests` and the catalog mapping
tests are plain unit tests; `ModularityTests` runs Spring Modulith's `verify()` (see Known Issues).

Tests use Testcontainers with `postgres:17` (the same major version as the local database; Docker must be running) and
do not touch the local database. Verified: the full-application tests start; Flyway applies V1 to the fresh container
and Hibernate `validate` succeeds, which also checks that the `AppUser` entity matches the `app_users` table.

## Known Issues

- Spring Modulith treats each direct subpackage of `com.thanh.cinetogether` as a module: today `identity` and `config`.
  `ModularityTests` fails on a dependency cycle or on use of another module's internals (seen failing with a temporary
  `identity` to `config` dependency). `config` is infrastructure that depends on `identity`'s public `ProfileProvisioner`,
  not the other way round.
- `spring-modulith-starter-jpa` was removed because its `event_publication` entity failed `ddl-auto: validate` (no
  migration creates that table). If event publication is needed later, re-add the dependency together with a
  migration for the table whose columns match the entity.
