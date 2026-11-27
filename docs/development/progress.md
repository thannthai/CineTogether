# CineTogether — Development Progress

## Current Phase

Watch Alone is feature complete (2026-11-27); the next phase is Watch Party Room Management. The Authentication and
Movie Catalog & Watch Alone milestone boxes stay unchecked until the manual checks under "Still to check" are done.

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
- one part of the end-to-end check with a real Supabase token: on 2026-11-23, with an existing account, signing in,
  `GET /me` (200, profile created, name from the email because that account has no `display_name`) and the `playback`
  call (200) worked. Still to be checked: a new account whose sign-up carries `data.display_name`, and `PATCH /me`.

The per-endpoint Security rules (former Task 7) are merged into the catalog work, because the first public endpoints
(catalog) and the first authenticated one that is not `/me` (playback) appear there.

Catalog plan (agreed, not started): module `catalog` with the same four-package layout as `identity`; a `MovieProvider`
interface with a `KKPhimProvider` adapter (`phimapi.com`, the only provider, chosen by the user); tests use a local fake
provider server, never the real API. Order: pure rules, adapter against the fake server, provider failures
(`PROVIDER_UNAVAILABLE`, new `MOVIE_NOT_FOUND`), `GET /movies`, `GET /movies/{providerKey}/{movieId}`,
`GET /catalog/home` (`featured` is the 5 newest movies, a derived value), `GET .../playback` (authenticated), access
rules. `episodeId` is the episode slug; playback uses the first provider server that has an `.m3u8` link for it. No
cache in the MVP.

Frontend slice 2c (movie detail page, `/movies/:providerKey/:movieId`) is done: the landscape image as the background with the
poster, title, type and episode count, genre chips that open the filtered list (`genreSlug` maps the name to the filter
slug), the full synopsis, "Xem ngay" to the first episode (`/watch?episode=...`), "Xem cùng" opening the coming-soon dialog,
the episode list (hidden for a single-part movie), a "Phim cùng thể loại" row, the browser tab title, and loading, not found
(404, also for an unknown provider) and error-with-retry states. The watch route is still a placeholder (slice 4). Verified
with `npm run lint`, `npm test` (48 tests), `npm run build`, and in Chrome against the running backend for a series, a
single-part movie and two unknown addresses. The retry state of a provider failure was not provoked.

Frontend slice 3 (login and profile) is implemented, with the real Supabase check still to do by hand: `@supabase/supabase-js`
(lazy client in `shared/lib/supabase.ts`), `apiGetAuth` and `apiPatchAuth` send the Bearer token only for calls that need it
(`GET` and `PATCH /me`; public endpoints never get a token, since the backend answers 401 to a bad one), `features/auth`
with `AuthProvider` (session plus the profile from `GET /me`, with a fallback name from the sign-up metadata or the email),
the login and register dialog (display name, email, password of 6+ characters, terms; an "check your email" step when the
project asks for confirmation; an address that is already registered is detected), the profile dialog (`PATCH /me`) and the
account menu in the header and drawer. Pure helpers are unit tested (62 tests): display-name rule mirroring the backend,
email and password checks, and the Vietnamese messages for Supabase errors. Verified with `npm run lint`, `npm test`,
`npm run build` and in Chrome for the form validation (no request is sent while a field is invalid). Not verified yet: a
real sign-up and sign-in against Supabase, `GET /me` with the real token (including the name from `data.display_name`), and
`PATCH /me`; they are done by hand because the assistant must not create accounts or type passwords on a hosted service.
On 2026-11-26 the user signed in with a real account: the header shows the avatar menu with the name and the email, so the
sign-in and the session work. Still to check by hand: renaming in "Hồ sơ" (`PATCH /me`), the session surviving a page reload,
sign-out, and a new account whose sign-up carries a display name. The Authentication milestone stays open until then. Forgot-password is not built.

Frontend slice 4 (watch page, `/movies/:providerKey/:movieId/watch?episode=...`) is done: a guest only sees the invitation to
sign in or register and `playback` is not called; a signed-in user gets `GET .../playback` (with the token) and the stream
plays in a `<video>` through hls.js (loaded on demand, its own 578 kB chunk; native playback is only the fallback for
browsers without Media Source Extensions); the episode list switches episodes through the `episode` parameter (an unknown
one falls back to the first); states for loading, no source (404), provider failure with retry, and a stream the browser
cannot play with retry. Two findings: recent Chrome (157) answers "maybe" to `canPlayType` for HLS but its native player
stopped after the playlist, so hls.js must be preferred over native playback; and with Vite's dev server the first dynamic
import of hls.js broke when the dependency was re-optimized mid-visit, so `optimizeDeps.include` lists it in
`vite.config.ts`. Verified with `npm run lint`, `npm test` (68 tests), `npm run build`, and in Chrome with the user's
signed-in session against the real provider: the episode loaded (1920x802, 15:22), played and buffered, switching episodes
worked, and an unknown episode fell back to the first. Not checked: the guest view (the session was not signed out), the
no-source and provider-failure retry states, mobile, Safari. The main bundle is about 593 kB (React, Router, Supabase); not
split yet.

Watch page player rework: the browser's own controls are replaced by a custom bar (`features/watch/Player.tsx`,
`PlayerControls.tsx`, `usePlayerState.ts`, `useHls.ts`): the progress bar (with the buffered range) sits above the buttons,
then play and pause, back and forward 5 seconds (also the Left and Right keys), volume and mute (the slider opens when the pointer is on the volume button), previous and next episode buttons
(shown for a series and disabled on the first and last episode, absent for a single movie) and, on the right, playback speed, picture-in-picture (hidden where unsupported) and full
screen; Space or K, M and F are shortcuts, the controls hide after 2.5 s of inactivity while playing, a click on the video
toggles play and a double click toggles full screen. When an episode ends the next one starts. Volume, mute and speed are
remembered (`playerPrefs.ts`); the speed also sets `defaultPlaybackRate`, because loading a stream resets the speed. The
container goes full screen so the controls stay. Browsers without Media Source Extensions (iPhone Safari) keep the native
controls. Not built on purpose: quality choice (every sampled KKPhim stream has a single ~1080p level; the selector should
come from `hls.levels` and show only with more than one level), skip intro (no data), mic, CC and download. Unit tests
cover time formatting, seek clamping, neighbouring episodes and the saved preferences (82 tests). Verified in Chrome with
the signed-in session: play, pause, 5 second seeks, speed menu and its memory across episodes, picture-in-picture in and out,
next episode, automatic switch at the end of an episode, no next button on the last one, and the failure state (episodes 7
and 8 of the sampled series return 404 at the provider). Not verified: full screen (the browser used by the assistant
refused the request, now caught instead of an unhandled error), auto-hide of the controls, touch, Safari, the volume
slider and the guest view.

Home interest cards: each theme card shows the poster of a movie of that theme (one request per theme, `pickDistinct`
makes sure no movie appears twice and prefers one released this year; the API has no popularity data, so it is a recently
updated movie, not a "hot" one), with the bold theme name and "Khám phá" with an arrow; a theme without a cover, or while
the covers load, keeps its flat pastel color. The row has padding on top so a lifted card is no longer clipped. The
home page now makes eight more provider requests (one per theme); if the provider turns out to be slow or rate limited,
cache or reduce them. The previous and next episode buttons of the player are arrows only.

More movie information (done, verified 2026-11-26: backend 189 tests, frontend 102 tests, checked in Chrome with real
data): the API now carries `originalTitle`, `quality`, `language` and `progress` on every movie, and the detail adds
`releaseYear`, `duration`, `views`, `actors`, `directors` and `trailerUrl` (docs/design/api.md §2.2). The home hero first
ranked this year's movies by views read from the detail of each (replaced on 2026-11-27, see below). The UI shows quality, language and "Tập 8/24" / "Hoàn tất 36/36" / "Full" stickers, the
Vietnamese title with the English one smaller, the whole hover popup opens the detail page (pointer cursor), and the popup
plays the muted YouTube trailer when there is one (cropped so YouTube's own controls are hidden), else the backdrop.
The detail page shows year, duration, directors and actors (views are not shown).

Movie tag and card polish (done 2026-11-26, checked in Chrome by the user; `npm run lint`, `npm test` 102 tests, `npm run build`):
the movie type tag ("Phim bộ" / "Phim lẻ") is gone from the hero, detail page, popup and search rows; search rows show the
original title and "year • episode label". The movie card has the quality (FHD or HD) at the top right of the poster, the
episode label at the bottom left, and the year above the title; the language tag is not on cards (it stays in the popup and
detail page, not in the hero). Quality and language are neutral chips (translucent purple-grey with a 1px inner outline); only
the episode tag is colored. The hero opens the detail page on a click (pointer cursor; links and buttons keep their action).
The glass buttons, the popup's secondary buttons and the "Sắp ra mắt" tag have thinner or no borders. The popup trailer keeps
the 144% crop and now fades its top and bottom edges into the popup color to hide YouTube's title and seek thumbnail strip;
YouTube's center pause and previous/next buttons still show (dropping `loop` and `playlist` may remove the arrows, not tried).
Findings: the provider's list endpoints carry no total episode count (only the detail does), so cards show "Tập 2", not
"Tập 2/12", unless the show is finished; hero, popup and detail show the total. Some synopses end with "…" because KKPhim
itself cuts them. KKPhim has Thuyết Minh and Lồng Tiếng versions as separate servers for about 1 in 5 movies, but the
backend picks only the first server, so there is no audio-version choice yet (idea for later, needs an API change).

Several countries in the browse filter (done 2026-11-27; backend `./gradlew test` 200 tests, frontend `npm test` 111 tests,
lint and build pass): `GET /movies` takes up to 5 `country` slugs, comma-separated or repeated, matched with OR (the
provider treats a comma as OR; its genre filter does too, so `genre` stays a single value because AND would need many
provider requests per page). Rules live in `CountryFilter` (blank and repeated values dropped, max 5, lowercase slug with
single hyphens); a breach is 400 `VALIDATION_ERROR`; `docs/design/api.md` is updated. In the UI the country row is
multi-select ("Tất cả" lights only when nothing else is chosen, the rest dim at 5), the title lists the names, each country
has its own "Đang lọc" chip and the filter count counts each country (`countrySelection.ts` holds the pure rules). Checked
with `curl` against the running backend and the real provider (OR result, 400 for 6 slugs and for a bad slug). Not yet
checked by eye in the browser. The filter panel, header menu, search field and buttons also got thinner (1px) or no borders.

Cinema, anime, year, this year's rows and a better hero (done 2026-11-27; backend `./gradlew test` 227 tests, frontend
`npm test` 125 tests, lint and build pass; the ranking was checked on real provider data with a script, and the lists with
`curl` on the running backend): `GET /movies` has `list` (`CINEMA`, `ANIME`: the provider's cinema and animation lists; the
list wins over `type` and search ignores it) and `year` (1900 to 2100, 400 otherwise; it also applies to search). Every list
item now carries `ratingCount` (IMDb vote count, else TMDb, read from the list, so no call per movie): it tells how well
known a movie is, which is a better "hot" signal than the provider's own view count (small, and it needs a detail call per
movie). The hero (`HeroPicks`) is a mix: the 2 best known cinema films, the best known Korean series and the 2 best known
other series (anime included), most rated first; a place nobody can fill goes to the best known movie left. `GET
/catalog/home` also has a `cinema` section ("Phim chiếu rạp"): up to 14 cinema films of this year, the best known first;
both are left out when their list fails, and the home page makes about 4 provider calls instead of about 26. Chinese
series are not given a hero place: their IMDb counts are tiny (Douban is where they are rated), so the data cannot tell
which one is well known. The data is cumulative, so it favours well-known titles over what is trending this week. Years
are never hard coded: movies of this year come first and last year's fill only the missing places, so early in a new year
the hero and the cinema row are not empty; the "Dành cho gia đình" row asks for the current year and tops up from the
previous one (`recentYears.ts`). The menu has "Chiếu rạp" and "Anime" (`/movies?list=...`, no type row in the filter
panel, their own chip and title; add Nhật Bản for Japanese anime); TV Shows stays hidden. The filter panel has a "Năm" row
(the current year and the nine before it, read at run time). Lists keep the provider's order, which is the last update,
not the release year (the provider can sort by year but then a new episode of an older series no longer lifts it, so the
year filter was chosen over a sort option). Known concern: the provider answered HTTP 429 once after many calls in a row;
the home page now makes far fewer calls, so a cache with a few minutes of expiry in the backend (Caffeine behind
`MovieProvider`, not for `playback`) is still worth doing but less urgent. Not checked by eye in the browser yet: the "Năm"
row, the cinema and anime pages, the new hero (the running backend must be restarted to serve it). The home rows' "Xem tất
cả" links do not carry the year.

Slice 5 (done): `/together` is a "Sắp ra mắt" page with sample rooms built from real posters, labelled "Dữ liệu minh họa";
every action opens the shared Soon dialog. Rooms and the Watch Party engine are still not built.

UI polish (done 2026-11-27, `npm run lint`, `npm test` 127 tests and `npm run build` pass): the language tag shows only
Vietsub (Thuyết Minh and Lồng Tiếng are never shown, and no audio-version choice is planned); header and movie title text
is larger and the header spacing tighter; "Khám phá tất cả" has an arrow; the footer is a centered block (logo, slogan, one
sentence about watching with friends and people who love films, copyright), with no social icons or link row because the
project has none yet. Checked by eye by the user while working; the cases below were not.

Known concerns: the home page now makes about 4 provider calls (the provider answered HTTP 429 once when called many times
in a row); the trailer embed depends on YouTube allowing embedding, and its centre pause and previous/next buttons still
show; the theme cover cards pick a recently updated movie of this year, not the best known one.

Still to check by hand (not done yet): the Authentication items listed above (rename in "Hồ sơ", session after a reload,
sign-out, a new account with a display name), the watch page for a guest, the no-source and provider-failure states, full
screen and the auto-hiding controls, touch and phone layout, Safari, and the new "Năm" row, cinema and anime pages and hero.

Improve later (not blocking Watch Party): a backend cache with a few minutes of expiry (Caffeine behind `MovieProvider`,
never for `playback`; less urgent now); a TV Shows list; the total episode count on cards (the provider's lists do not carry
it, only the detail does); a quality selector (every stream has one level today); dropping `loop` and `playlist` from the
trailer embed to try to lose its arrows; forgot-password; a fuller synopsis source such as TMDb (KKPhim cuts some synopses
itself); the year on the home rows' "Xem tất cả" links; a trending signal (the rating counts are cumulative, so the hero
favours well-known titles over this week's hits).

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

Behavior 3 (provider failures) is done at the adapter level: `KKPhimProvider` uses a connect timeout (3 s) and a read
timeout (5 s), and wraps every client failure (timeout, refused connection, 5xx, 4xx other than 404, a body that is not
JSON, a list without `data`) in `ProviderUnavailableException`; a 404 stays an empty `Optional`. Verified with
`./gradlew test` (131 tests pass) against the fake provider, which can now answer with an error status or slowly. The
timeouts are not configurable from `application.yaml` yet, and the HTTP codes (`PROVIDER_UNAVAILABLE`,
`MOVIE_NOT_FOUND`) are produced later by the web layer.

Behavior 4 (`GET /movies`) is done: the `Catalog` service, `MoviesController`, `CatalogExceptionHandler` (400, and 503
`PROVIDER_UNAVAILABLE`), `CatalogConfig` (the provider bean from `application.yaml`), `GlobalExceptionHandler` now also
maps an invalid parameter value to 400, and `SecurityConfig` makes `GET /api/v1/movies` public. Decisions: `GET /movies`
uses the single configured provider (no `providerKey` parameter); an unavailable provider is 503; watching a movie
(`playback`) stays authenticated (option A). Verified with `./gradlew test` (139 tests pass) and by running the application
against the real provider (public calls, filters, search, bad parameters).

Behavior 5 (`GET /movies/{providerKey}/{movieId}`) is done: public; `Catalog.detail` answers 404 `MOVIE_NOT_FOUND` when
the `providerKey` is not the configured provider (the provider is not called) or the provider does not know the movie.
`SecurityConfig` permits `/api/v1/movies/*/*`, which does not match `/playback`. Verified with `./gradlew test` (143
tests pass).

Behavior 6 (`GET /catalog/home`) is done: public, derived from one `browse` call (`featured` = first 5, one section
`new-updates`). Verified with `./gradlew test` (147 tests pass).

Behavior 7 (`GET /movies/{providerKey}/{movieId}/playback`) is done: authenticated (it is not matched by the public
`/movies/*/*` rule; a test with a widened matcher turned red, so it guards this). Same 404 `MOVIE_NOT_FOUND` rules as the
details; a missing `episodeId` is 400 `VALIDATION_ERROR` (new handler in `GlobalExceptionHandler`). Verified with
`./gradlew test` (153 tests pass).

Behavior 8 (access rules) is done: `CatalogAccessRulesTests` lists every catalog route with and without a token (and
the CORS preflight); closing the home route made it fail. The `SecurityConfig` Javadoc and `setup.md` were updated.
Verified with `./gradlew test` (161 tests pass).

The catalog backend (behaviors 1 to 8) is done and was checked against the real provider on 2026-11-23: home, details,
404s, playback (200 with a real token) and 503 with a dead provider address. The provider is sometimes slow or fails;
the cause of every 503 is logged as WARN. Desktop Chrome cannot reliably play an `.m3u8` link by itself, so the frontend needs
hls.js (the stream host sends `Access-Control-Allow-Origin: *`).

Next: the frontend, built in one go (both milestones' UI). Plan: the user designs it in Figma Make, the Figma MCP
plugin (`figma@claude-plugins-official`, authenticated as the Figma user `tcthanh`, seat View) is already installed;
then the generated code is brought into `frontend/` (React 19, Vite, TypeScript, oxlint; no router or UI library yet)
and wired to the API: Supabase client (register with a display-name field, sign in, sign out), catalog screens, the
hls.js player, `GET` and `PATCH /me`. Still to check there: a new account whose sign-up carries `data.display_name`.
Closing the Authentication milestone box needs those login screens.

Frontend conventions were agreed on 2026-11-23: feature-first structure and rules in `docs/architecture/conventions.md`,
short summary in `CLAUDE.md`. The folders are created as the work reaches them.

Frontend slice 1 (foundation) is done: React Router (routes `/`, `/movies`, `/movies/:providerKey/:movieId`,
`.../watch`, `/together`, 404), design tokens with a light and a dark pastel theme (saved choice, system default, no
flash on load), the Nunito font, header (menu, mega menus, mobile drawer, theme toggle) and footer, ported from the
Figma Make prototype with the round-3 design fixes (no trailing dots, no gradient on the wordmark, no sparkle).
Vitest was added for pure logic (filter-to-URL helpers and the highlighted menu item; 14 tests). Verified with
`npm run lint`, `npm test`, `npm run build`, and in Chrome. Decisions: the menu hides Chiếu rạp, Anime and TV Shows
until the backend can list them (KKPhim has the lists `phim-chieu-rap`, `hoat-hinh`, `tv-shows`, but `GET /movies`
only maps new, single and series; Anime is the `hoat-hinh` list, not a genre); genre and country menus use the real
KKPhim names and slugs (no "Mỹ", it is "Âu Mỹ"). Placeholders remain for every page, the search button only opens
`/movies`, and Đăng nhập / Đăng ký do nothing yet. Frontend slice 2a (home page) is done: `shared/lib` has the API client (`apiGet`, problem+json errors mapped to Vietnamese
messages, `useAsync`), `features/catalog` has the types, API calls and the home screen (hero from `featured` with the
selected movie's detail loaded for the synopsis and the poster as a blurred backdrop, interest cards linking to genre
and country filters, one row for the home response and independent rows for single movies, series and the family
genre), and `features/party` has the Watch Together strip; every room action opens the shared "coming soon" dialog.
`config/env.ts` now reads the `VITE_*` values lazily (`checkEnv()` still fails fast at startup) so unit tests can import
modules that use it. Verified with `npm run lint`, `npm test` (24 tests), `npm run build`, and in Chrome against the
running backend (real posters and synopsis, thumbnail switching, dialog, dark theme, card to detail route). Not checked:
the error and retry states of the rows (no provider failure was provoked). Not in the design because the API has no
data for it: "Đang thịnh hành", ratings, quality badges. Hero background: the movie detail now carries `backdropUrl` (KKPhim's landscape `thumb_url`, 780x439; the portrait poster
is only 344x484 and looked bad stretched). Added to `MovieDetail`, the KKPhim mapping and `api.md` §2.2 (list items still
carry only `posterUrl`); `./gradlew test` passes (162 tests). The hero shows the image full width with the five thumbnails
at the bottom right and falls back to the poster when a movie has no landscape image. Verified in Chrome against the
running backend. The image is upscaled about 2x on wide screens, so it is soft rather than razor sharp.
Home `featured` now prefers movies released this year (or later), any type, then fills from the rest in the provider's
order (`Featured.pick` with the injected `Clock`; `api.md` §2.2 updated), because the provider sorts by last update and an
old series with a new episode used to lead the hero. `./gradlew test` passes (169 tests); checked on the running backend:
all five featured movies are from 2026 while `new-updates` keeps the provider's order.

Frontend slice 2b is done: the browse page (`/movies`) with the title for the current filters, a search field, a filter
panel (type, genre, country; choices are applied with a button and kept in the URL together with `q` and `page`), removable
filter chips, a 24-movie grid and numbered pagination that never shows a last page number (the API only has `hasNext`);
the quick search palette (button or Cmd/Ctrl+K, six suggestions, keyboard navigation) opened from the header and the
mobile drawer; and the hover preview on every movie card (opens after 300 ms on devices with a mouse, shows the landscape
image, synopsis and actions; details are cached for the visit by `memoAsync`). The unit tests cover the URL and page
helpers, the title, the page window, the cache and the popup position (45 tests). Verified with `npm run lint`,
`npm test`, `npm run build`, and in Chrome against the running backend (apply filters, page 2, search with results and
with none, palette with keyboard, popup content and closing). Not checked: the error and retry states, the mobile
layout, and touch behaviour (the browser window of the session could not be resized). A search ignores the type filter
(the API says so) although the chip stays visible. Next: slice 2c (movie detail page with episodes), then slice 3
(login and profile), slice 4 (player), slice 5 (Xem chung mock).

WebSocket `AUTH` and token renewal belong to the WebSocket milestone.

## Notes

Update progress after completing meaningful tasks or milestones. Record only verified progress.