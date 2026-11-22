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

- Authentication Task 4, REST authentication errors: a missing or invalid token now gets `401` with an
  `application/problem+json` body (`code: UNAUTHENTICATED`) and keeps the `WWW-Authenticate: Bearer` header, via
  `ProblemJsonAuthenticationEntryPoint`. `ProblemJsonErrorTests` failed first (no body, no content type). `./gradlew
  test` passes (29 tests). `FORBIDDEN` (403) and `VALIDATION_ERROR` (400) are deferred to the first features that need
  them (400 with `PATCH /me`); `requestId` is not returned in the MVP. `api.md` §1.2 was updated accordingly.

- Authentication Task 5, `identity` module and profile provisioning: an authenticated request now creates the caller's
  `app_users` row once (`INSERT ... ON CONFLICT DO NOTHING`, own short transaction), named from
  `user_metadata.display_name`, else the email local part, else `Viewer-` plus 6 ID characters. Added `DisplayNames`,
  `UserProfiles`, the public `ProfileProvisioner`, and `ProfileProvisioningFilter` in `config`. Verified with
  `./gradlew test` (52 tests pass): unit tests for the name rule, PostgreSQL tests for the upsert (the concurrency test
  catches a check-then-insert implementation in 5 of 5 runs, versus 1 of 3 before it was repeated for 20 rounds), request
  tests for provisioning (valid token creates, anonymous and invalid token create nothing, a database failure stops the
  request), and `ModularityTests`. Not done: the JPA entity (deferred to `GET /me`), and a check with a real token that
  carries `display_name`.

- Authentication Task 6, `GET /me` and `PATCH /me`: `GET` returns the caller's `id`, `displayName` and `createdAt`;
  `PATCH` renames the caller (200 with the updated profile, `updated_at` set from an injected `Clock`, `created_at`
  unchanged). A bad name (blank, over 80 characters counted as characters, control or invisible characters, missing
  field) or an unreadable body gets 400 `problem+json` with `VALIDATION_ERROR` and leaves the profile unchanged;
  anonymous calls get 401 before the body is read; an `id` in the body is ignored. `identity` now uses the layout
  `application/`, `domain/`, `persistence/`, `web/` (documented in `architecture.md`); the `AppUser` JPA entity matches
  `app_users` under `ddl-auto: validate`. Verified with `./gradlew test` (79 tests pass), including temporary
  deliberate bugs that the tests caught (id taken from the body, `PATCH /me` open to anonymous callers, the old
  session and CSRF configuration). Redundant tests were removed. Not done: a check with a real token end to end.

## Current Task

Movie Catalog & Watch Alone, backend first. Authentication & User Profile is complete on the backend (Tasks 1 to 6);
two parts are deliberately postponed, so its milestone box stays unchecked:

- the React login, register and logout screens (FR-AUTH-04, former Task 8), to be built with the whole frontend;
- an end-to-end check with a real Supabase token (`GET` and `PATCH /me`, name from `display_name`), still to be run
  with `backend/http/auth.http`.

The per-endpoint Security rules (former Task 7) are merged into the catalog work, because the first public endpoints
(catalog) and the first authenticated one that is not `/me` (playback) appear there.

Catalog plan (agreed, not started): module `catalog` with the same four-package layout as `identity`; a `MovieProvider`
interface with a `KKPhimProvider` adapter (`phimapi.com`, the only provider, chosen by the user); tests use a local fake
provider server, never the real API. Order: pure rules, adapter against the fake server, provider failures
(`PROVIDER_UNAVAILABLE`, new `MOVIE_NOT_FOUND`), `GET /movies`, `GET /movies/{providerKey}/{movieId}`,
`GET /catalog/home` (`featured` is the 5 newest movies, a derived value), `GET .../playback` (authenticated), access
rules. `episodeId` is the episode slug; playback uses the first provider server that has an `.m3u8` link for it. No
cache in the MVP.

## Blockers

- WebSocket origin restrictions are not configured yet (needed with the WebSocket milestone).
- Provider rights: `BR-MOV-03` requires usage rights for production playback, and KKPhim is unofficial with no license
  guarantee. Treat it as a development and demo source until rights are confirmed.

## Next Task

Catalog behavior 1 (pure rules) is done: `Paging` (defaults, limits, 400 on invalid values) and `MovieType` in
`catalog/domain`, and the KKPhim-specific mapping (`single` is a MOVIE and every other type a SERIES, provider pages
start at 1, `hasNext`, absolute image URLs) in `catalog/infrastructure`. Verified with `./gradlew test` (98 tests
pass). Adapter use of the provider: lists and search come from `/v1/api/danh-sach/{slug}` and `/v1/api/tim-kiem`
(`/v1/api/danh-sach/phim-moi-cap-nhat` has `type` and categories, unlike the older `/danh-sach/phim-moi-cap-nhat`), and
details from `/phim/{slug}`.

Behavior 2a (browse and search) is done: `MovieProvider.browse` and `KKPhimProvider` (`RestClient`) read lists from
`/v1/api/danh-sach/{phim-moi-cap-nhat|phim-le|phim-bo}` and search from `/v1/api/tim-kiem`, with `category`, `country`,
`page` and `limit` (the provider ignores a `type` parameter, so type picks the list). User values go in as URI
variables so they are encoded. Verified with `./gradlew test` (104 tests pass) against a local fake provider
(`FakeProviderServer`), and once with a throwaway test against the real API (lists, filters, search with `&` and
accents). Not done yet: provider failures and timeouts (behavior 3), details with episodes (2b), playback source (2c).

Behavior 2b (details with episodes) is done: `MovieProvider.detail` returns `Optional<MovieDetail>`, empty when the
provider answers 404. `KKPhimProvider` reads `GET /phim/{slug}`, removes HTML from the description
(`KKPhimMapping.plainText`; the real descriptions all contain HTML), and merges the episodes of several servers once
per slug, first server wins. Verified with `./gradlew test` (116 tests pass) and once with a throwaway test against the
real API (24 movies: clean descriptions, episodes and posters present, unknown slug is empty).

Catalog code is grouped as `domain/movie`, `domain/browse` and `infrastructure`, and the security tests that sat at
the test root now live in `config` (see `architecture.md`). The provider's JSON records are in `KKPhimResponses`.

Behavior 2c (playback source) is done: `MovieProvider.playbackSource(movieId, episodeId)` returns
`Optional<PlaybackSource>` (HLS); `KKPhimProvider` picks the first server that has an `.m3u8` link for the episode and
ignores a link that is not an http(s) `.m3u8` URL or is an embed page. Verified with `./gradlew test` (124 tests pass) and
once with a throwaway test against the real API (24 of 24 episodes resolved). The backend does not check that the stream
plays; that is for the frontend stage.

Next: behavior 3, provider failures (timeouts, 5xx, invalid JSON) as `PROVIDER_UNAVAILABLE` and an unknown movie as
`MOVIE_NOT_FOUND`.

WebSocket `AUTH` and token renewal belong to the WebSocket milestone.

## Notes

Update progress after completing meaningful tasks or milestones. Record only verified progress.