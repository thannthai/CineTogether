# CineTogether — API Design

**Version:** 0.4  
**Status:** Draft — Proposed MVP Contracts  
**Stack:** Spring Boot REST + Native WebSocket (JSON) · Supabase Auth

## 1. Scope & Conventions

This document defines the frontend–backend contracts for the private and public Watch Party MVP. Business rules belong
in [Product Requirements](../product/requirements.md) and [Watch Party Design](watch-party.md); persistence belongs
in [Database Design](database.md).

- REST base path: `/api/v1`.
- WebSocket endpoint: `/ws/rooms/{roomId}` (`wss://` in production).
- JSON fields: `camelCase`.
- Identifiers: UUID strings unless provider-defined.
- Timestamps: ISO 8601 UTC.
- Playback positions: integer milliseconds.
- Successful REST responses return JSON objects directly. `204 No Content` has no body.
- Paginated collections use `items`, `page`, `size`, and `hasNext`.
- Business state is server-authoritative.

Clients never submit their identity, Host role, server timestamps, or playback version as writable values.

**Out of scope:** public-room search, ranking, and moderation tools, watchlists, history, recommendations, permanent
chat, media uploads, transcoding, microphone, camera, screen sharing, and custom authentication endpoints.

Public Room Discovery and Preview are part of the MVP (§2.4).

### 1.1. Authentication & Authorization

Supabase Auth handles login, registration, password reset, and session refresh in the frontend.

Protected REST requests include:

```http
Authorization: Bearer <supabase-access-token>
```

Spring Security validates JWT signature, issuer, expiration, and intended audience. The verified `sub` identifies the
application user.

Asymmetric JWT signing with Supabase JWKS is the preferred configuration.

The backend creates the local `app_users` profile idempotently on the first authenticated REST request or WebSocket
`AUTH` (not only `GET /me`), using the display-name fallback in [Database Design](database.md) §3.1.

**Access rules:**

- Catalog browsing and movie details are public.
- Resolving playback sources requires authentication.
- Creating and joining rooms (PRIVATE or PUBLIC) requires authentication.
- Public room discovery and preview are public and expose only the fields listed in §2.4.
- Reading room state and chat requires authorized membership.
- Host-only operations require current Host ownership and connection eligibility.

For native browser WebSocket connections, the client opens the socket and immediately sends an `AUTH` message containing
its Supabase access token.

The server must not expose room data or accept other commands before authentication succeeds.

Tokens must never appear in WebSocket URLs, application logs, or broadcast payloads.

The server closes a socket that has not authenticated within a short timeout (for example 10 seconds) and enforces a
small frame-size limit before authentication.

**Token renewal:** Supabase access tokens expire (about one hour by default), which is shorter than a movie. The client
refreshes its token through Supabase and sends another `AUTH` message on the already authenticated socket. The server
validates it, requires the same user (`sub`), and extends the socket's expiry in place — no new `connection_id`, no
`SESSION_REPLACED`, and no membership status change. A different user produces `ERROR` and closes the socket. A socket
whose token expires without renewal is closed and follows the normal disconnect rules.

### 1.2. Error Responses

REST errors use `application/problem+json` with a stable machine-readable `code`.

Example:

```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "The room has reached its capacity.",
  "code": "ROOM_FULL",
  "requestId": "req-123"
}
```

`requestId` is not returned in the MVP: there is no request-correlation ID yet. Add it when request logging introduces
one.

| HTTP Status | Meaning                            |
|-------------|------------------------------------|
| `400`       | Invalid request                    |
| `401`       | Missing, invalid, or expired token |
| `403`       | Insufficient permissions           |
| `404`       | Resource or invitation not found   |
| `409`       | Room capacity or state conflict    |
| `410`       | Room ended or expired              |
| `429`       | Too many requests                  |
| `502 / 503` | Provider or service unavailable    |

Core error codes:

- `UNAUTHENTICATED` (`401`: missing or invalid access token)
- `FORBIDDEN` (`403`: authenticated but not allowed, when no more specific code applies)
- `VALIDATION_ERROR`
- `INVALID_INVITE`
- `ROOM_NOT_FOUND`
- `ROOM_FULL`
- `ROOM_ENDED`
- `MEMBERSHIP_REQUIRED`
- `MEMBER_REMOVED`
- `HOST_ONLY`
- `INVALID_SELECTION`
- `STALE_PLAYBACK_VERSION`
- `MOVIE_NOT_FOUND`
- `PROVIDER_UNAVAILABLE`
- `RATE_LIMITED`
- `AUTH_FAILED` (WebSocket `AUTH` rejected)

The frontend maps error codes to user-facing Vietnamese messages.

A `401` always has the same body, whatever the reason (missing, malformed, expired, or wrongly signed token), so it does
not reveal why a token was rejected. The reason is in the standard `WWW-Authenticate: Bearer error="invalid_token"`
header. `UNAUTHENTICATED`, `VALIDATION_ERROR`, `MOVIE_NOT_FOUND` and `PROVIDER_UNAVAILABLE` are implemented; `FORBIDDEN` is implemented with the
first feature that needs it.

## 2. REST API

### 2.1. Identity

| Method  | Endpoint | Access        | Purpose                       |
|---------|----------|---------------|-------------------------------|
| `GET`   | `/me`    | Authenticated | Retrieve current user profile |
| `PATCH` | `/me`    | Authenticated | Update display name           |

`GET /me` returns the profile, which already exists because the authentication layer creates it on any authenticated
request.

Example response:

```json
{
  "id": "11111111-1111-4111-8111-111111111111",
  "displayName": "Minh",
  "createdAt": "2026-11-15T08:00:00Z"
}
```

`PATCH /me` request:

```json
{
  "displayName": "Minh Nguyen"
}
```

`PATCH /me` returns `200` with the updated profile, in the same shape as `GET /me`. `displayName` is trimmed and must be
1 to 80 characters, counted as characters (an emoji is one). A name with no visible characters or with control or
invisible format characters (zero-width space, right-to-left override, line breaks) is rejected, while the zero-width
joiner that builds emoji sequences is allowed. An invalid name, a missing `displayName`, or a body that is not valid JSON
returns `400` with code `VALIDATION_ERROR`, and the profile is left unchanged. Extra fields in the body, such as `id`,
are ignored.

The user ID is always derived from the validated JWT. Both endpoints return `401` (`UNAUTHENTICATED`) without a valid
token, before the body is read.

### 2.2. Movie Catalog & Watch Alone

| Method | Endpoint                                   | Access        | Purpose                                  |
|--------|--------------------------------------------|---------------|------------------------------------------|
| `GET`  | `/catalog/home`                            | Public        | Featured movies and homepage collections |
| `GET`  | `/movies`                                  | Public        | Browse, search, and filter movies        |
| `GET`  | `/movies/{providerKey}/{movieId}`          | Public        | Movie details and episodes               |
| `GET`  | `/movies/{providerKey}/{movieId}/playback` | Authenticated | Resolve a playable media source          |

**Browse query parameters:**

| Parameter | Type    | Description                |
|-----------|---------|----------------------------|
| `q`       | String  | Search keyword             |
| `type`    | Enum    | `MOVIE` or `SERIES`        |
| `genre`   | String  | Genre filter               |
| `country` | String  | Country filter, one or more slugs (see below) |
| `list`    | Enum    | `CINEMA` (cinema releases) or `ANIME` (the provider's animation list) |
| `year`    | Integer | Release year, from `1900` to `2100` |
| `page`    | Integer | Default `0`                |
| `size`    | Integer | Default `20`, maximum `50` |

`country` takes up to 5 country slugs, either comma-separated (`country=han-quoc,trung-quoc`) or repeated. A movie
matches when it belongs to any of them (OR). Repeated values are ignored and do not count towards the limit. A slug must
be lowercase letters and digits separated by single hyphens; more than 5 slugs or an invalid slug is rejected with `400`
(`VALIDATION_ERROR`). `genre` stays a single value: the provider cannot match several genres at once (it treats them as
OR), so an AND filter would need many provider requests per page.

`list` reads one of the provider's own lists: `CINEMA` for cinema releases, `ANIME` for animation (mostly Japanese and
Chinese; add `country=nhat-ban` for Japanese anime). It replaces the list chosen by `type` when both are present, `genre`
and `country` still apply, and, like `type`, it is ignored when `q` is present. Any other value is rejected with `400`
(`VALIDATION_ERROR`).

`year` keeps only the movies released that year. It applies to every list and, like `genre` and `country`, to search. A
value that is not a number or is outside `1900` to `2100` is rejected with `400` (`VALIDATION_ERROR`).

`page` starts at `0`. A negative `page`, or a `size` below `1` or above `50`, is rejected with `400`
(`VALIDATION_ERROR`) instead of being adjusted silently.

`type` is `MOVIE` for single-part films and `SERIES` for everything else the provider lists, including animation and TV
shows. The provider cannot filter by type in one request, so the `type` filter is limited: `type=MOVIE` returns the
single-part films, `type=SERIES` returns only the provider's TV series list (not animation or TV shows, which still
appear, labelled `SERIES`, when there is no `type` filter and in search results), and `type` is ignored when `q` is
present because the provider's search cannot filter by it.

The API should not invent popularity statistics or filtering capabilities unsupported by the movie provider.

A `type` other than `MOVIE` or `SERIES`, or a `page` or `size` that is not a number, is also `400`
(`VALIDATION_ERROR`). When the movie provider cannot be reached, answers too slowly (3 s to connect, 5 s to read) or sends an
unusable response, the API answers `503` with code `PROVIDER_UNAVAILABLE`; the detail never exposes the cause.

`GET /movies` returns:

```json
{
  "items": [],
  "page": 0,
  "size": 20,
  "hasNext": false
}
```

Each `MovieSummary` includes `providerKey`, `movieId`, `title`, `type`, and optional `originalTitle`, `posterUrl`,
`releaseYear`, `genres`, `quality` (for example `FHD`), `language` (for example `Vietsub`), `progress` and
`ratingCount` (how many people rated it on IMDb, else TMDb: how well known it is; null when unknown). `progress` is
`{ "current": 8, "total": 24, "completed": false }`: the latest episode, the planned number of episodes and whether the
series is finished. `current` and `total` are null when the provider does not say (the list never has `total` for a
series that is still airing, only the detail does), and a single-part movie is `{ "current": null, "total": null,
"completed": true }`. Every optional field is null when unknown.

`GET /catalog/home` returns:

```json
{
  "featured": [],
  "sections": [
    {
      "key": "new-updates",
      "title": "Mới cập nhật",
      "items": []
    }
  ]
}
```

Only collections supported by the provider or curated data are returned.

The provider has no "featured" list, so the home page is derived from provider lists. `new-updates` is the first page of
the newest updates in the provider's order (the provider orders it by last update, so an old series with a new episode
can be first there); an unavailable provider is `503`. `featured` is up to 5 movies of this year or later, a mix so the hero is not all one kind: the 2 best known cinema films
(from the first 50 films of the cinema list), the best known Korean series (one page of 50 Korean series of this year) and
the 2 best known other series, anime included (one page of 50 movies of this year, any type). "Best known" is the highest
`ratingCount`: how many people rated a movie tells how well known it is. A movie is picked once, and a place nobody can
fill goes to the best known movie left in any of these lists. A movie without a poster is never featured, and one with an
unknown `ratingCount` ranks after the known ones in the provider's order. The most rated comes first. Only when fewer
than 5 exist, for example early in a new year, does a page of last year's movies fill the missing places (they come after
this year's, ranked the same way); movies of older years are never featured however much they are rated, so fewer than 5
(or none) can still be returned. A list that fails is skipped and the page still loads. No call per movie is needed.

A second section, `cinema` ("Phim chiếu rạp"), follows `new-updates`: up to 14 cinema films of this year or later, most
rated first (last year's fill the missing places, as for `featured`), taken from the first 50 films of the provider's
cinema list. The section is left out when the cinema list is unavailable or has no film of this year; that never makes the
home page fail.

**Movie detail response:**

```json
{
  "providerKey": "provider-a",
  "movieId": "movie-123",
  "title": "Example Movie",
  "originalTitle": "Example Movie (English)",
  "type": "SERIES",
  "releaseYear": 2026,
  "quality": "FHD",
  "language": "Vietsub",
  "duration": "19 phút/tập",
  "progress": { "current": 8, "total": 24, "completed": false },
  "views": 812,
  "actors": ["Actor One"],
  "directors": [],
  "trailerUrl": "https://www.youtube.com/watch?v=abc123",
  "synopsis": "...",
  "posterUrl": "https://media.example/poster.jpg",
  "backdropUrl": "https://media.example/backdrop.jpg",
  "genres": [
    "Drama"
  ],
  "episodes": [
    {
      "episodeId": "ep-01",
      "title": "Tập 1"
    }
  ]
}
```

`backdropUrl` is the provider's landscape image (null when it has none); clients use it as a page background and fall back
to `posterUrl`. The movie list items carry only `posterUrl`. The detail also carries what the list does not: `releaseYear`,
`duration` (the provider's text, for example "19 phút/tập"), `views`, `actors`, `directors` (placeholders such as "Đang cập
nhật" are dropped, so the list can be empty), `trailerUrl` (only an `http` or `https` link; most movies have none) and the
total in `progress`. A client shows a trailer only as the page that the link points to or inside an embed of a known
player; it never runs the link as script.

`synopsis` is plain text: the provider's HTML is removed, so clients show it as text. A single-part movie has one entry in
`episodes`. When the provider lists the same episode on several servers, `episodes` contains it once, identified by
`episodeId` (the provider's episode slug).

**Playback source request:**

```http
GET /api/v1/movies/provider-a/movie-123/playback?episodeId=ep-01
```

Response:

```json
{
  "providerKey": "provider-a",
  "movieId": "movie-123",
  "episodeId": "ep-01",
  "streamUrl": "https://licensed-media.example/stream.m3u8",
  "format": "HLS"
}
```

The backend resolves and validates the selected movie and episode through its provider adapter. `streamUrl` is an
`http` or `https` link to a `.m3u8` playlist; the provider's embed-page links are never used. When the provider lists the
episode on several servers, the first server that has an HLS link for it is used. An unknown movie or episode, or an
episode with no HLS link, is `404` (`MOVIE_NOT_FOUND`); a missing `episodeId` is `400` (`VALIDATION_ERROR`). The link comes from a third-party server, so the backend cannot check that it plays.

Each browser streams independently from an authorized source. The Spring Boot backend does not relay video bytes.

Production playback requires an appropriately licensed provider and browser-compatible HLS delivery.

### 2.3. Watch Party Rooms

All endpoints below use the `/api/v1` prefix.

| Method | Endpoint                                      | Access         | Purpose                                             |
|--------|-----------------------------------------------|----------------|-----------------------------------------------------|
| `POST` | `/rooms`                                      | Authenticated  | Create a PRIVATE or PUBLIC room                     |
| `POST` | `/rooms/join`                                 | Authenticated  | Join or resume by invitation code or PUBLIC room ID |
| `GET`  | `/rooms/{roomId}`                             | Active member  | Retrieve authoritative room snapshot                |
| `GET`  | `/rooms/{roomId}/messages`                    | Active member  | Retrieve recent messages                            |
| `POST` | `/rooms/{roomId}/leave`                       | Member         | Leave the room                                      |
| `POST` | `/rooms/{roomId}/transfer-host`               | Connected Host | Transfer Host ownership                             |
| `POST` | `/rooms/{roomId}/members/{membershipId}/kick` | Connected Host | Remove a member                                     |
| `POST` | `/rooms/{roomId}/end`                         | Connected Host | End the room                                        |

#### A. Create Room

```http
POST /api/v1/rooms
Content-Type: application/json
```

Request with optional visibility and movie selection:

```json
{
  "visibility": "PUBLIC",
  "selection": {
    "providerKey": "provider-a",
    "movieId": "movie-123",
    "episodeId": "ep-01"
  }
}
```

Omit `selection` to create a room without an initial movie.

`visibility` is `PRIVATE` or `PUBLIC` and defaults to `PRIVATE` when omitted. It is fixed for the room's lifetime; an
invalid value produces `VALIDATION_ERROR`.

Response: `201 Created`

```json
{
  "roomId": "22222222-2222-4222-8222-222222222222",
  "visibility": "PUBLIC",
  "inviteCode": "opaque-random-code",
  "expiresAt": "2026-11-15T12:00:00Z"
}
```

Every room, including PUBLIC ones, receives an invitation code; for PUBLIC rooms it is an alternative to the room ID.

The frontend constructs its invitation route:

```text
https://cinetogether.example/join/{inviteCode}
```

Room creation initializes the room, creator membership, and playback snapshot atomically.

An initial movie selection starts paused at position zero.

The creator's membership is reserved as `DISCONNECTED` with `reconnectDeadlineAt` 30 seconds after creation, and the
room's `empty_since` starts at creation. The user becomes `CONNECTED` only after successfully attaching to the room
WebSocket; if the creator does not attach in time, the reservation expires and the room awaits a Host (§4.5).

A provided `selection` is validated with the provider before the database transaction opens.

#### B. Join Room

```http
POST /api/v1/rooms/join
Content-Type: application/json
```

Request by invitation code (any room):

```json
{
  "inviteCode": "opaque-random-code"
}
```

Request by public room identifier (`PUBLIC` rooms only):

```json
{
  "roomId": "22222222-2222-4222-8222-222222222222"
}
```

Exactly one of `inviteCode` and `roomId` must be supplied; otherwise `VALIDATION_ERROR`. A `roomId` Join for a
`PRIVATE` or unknown room returns `404 ROOM_NOT_FOUND`, so private rooms cannot be probed. Both variants then follow
identical rules and return the same response.

Response: `200 OK`

```json
{
  "roomId": "22222222-2222-4222-8222-222222222222",
  "membershipId": "33333333-3333-4333-8333-333333333333",
  "status": "DISCONNECTED",
  "reconnectDeadlineAt": "2026-11-15T08:00:30Z"
}
```

The response reflects the actual membership state. An existing connected member may receive `CONNECTED`.

The backend must:

- Validate the invitation code or the `PUBLIC` room ID, and room availability (`ACTIVE` and within both expiration
  deadlines).
- Enforce room capacity, counting only `CONNECTED` and unexpired `DISCONNECTED` memberships; an expired reservation
  releases its slot even before cleanup runs.
- Reuse the existing membership for the same user.
- Leave an existing unexpired reservation unchanged on repeated Join calls; only a new reservation or the
  reactivation of a `LEFT`/`EXPIRED`/`REMOVED` membership receives `now + 30 seconds`.
- Revalidate room capacity when reactivating a `LEFT`, `EXPIRED`, or `REMOVED` membership.
- Allow a `REMOVED` member to Join the same `ACTIVE` room again: the membership record is reused with a fresh
  `joined_at`
  (kick is not a ban). The previous connection stays invalid; a new successful WebSocket `AUTH` is required.

A successful REST Join reserves membership; it does not establish presence and never grants Host ownership.

#### C. Get Room Snapshot

```http
GET /api/v1/rooms/{roomId}
```

The response uses the shared `RoomSnapshot` structure.

**Room**

- `id`
- `status`
- `visibility`
- `hostUserId` (`null` while the room awaits a Host)
- `inviteCode`
- `expiresAt`
- `capacity`
- `occupiedSlots`
- `connectedCount`

**Members**

- `membershipId`
- `userId`
- `displayName`
- `status`
- `joinedAt`

**Current user**

- `membershipId`
- `status`
- `reconnectDeadlineAt`

**Playback**

- `selection`
- `status`
- `positionMs`
- `referenceAt`
- `version`

**Synchronization metadata**

- `serverTime`

Example:

```json
{
  "room": {
    "id": "22222222-2222-4222-8222-222222222222",
    "status": "ACTIVE",
    "visibility": "PRIVATE",
    "hostUserId": "11111111-1111-4111-8111-111111111111",
    "inviteCode": "opaque-random-code",
    "expiresAt": "2026-11-15T12:00:00Z",
    "capacity": 6,
    "occupiedSlots": 1,
    "connectedCount": 1
  },
  "members": [
    {
      "membershipId": "33333333-3333-4333-8333-333333333333",
      "userId": "11111111-1111-4111-8111-111111111111",
      "displayName": "Minh",
      "status": "CONNECTED",
      "joinedAt": "2026-11-15T08:00:00Z"
    }
  ],
  "me": {
    "membershipId": "33333333-3333-4333-8333-333333333333",
    "status": "CONNECTED",
    "reconnectDeadlineAt": null
  },
  "playback": {
    "selection": null,
    "status": "PAUSED",
    "positionMs": 0,
    "referenceAt": "2026-11-15T08:00:00Z",
    "version": 0
  },
  "serverTime": "2026-11-15T08:00:01Z"
}
```

The actual `members` array contains all active and reserved members.

Only `CONNECTED` and unexpired `DISCONNECTED` memberships are included.

If `hostUserId` is `null`, the room is awaiting a Host and no one holds Host controls until the first WebSocket
attachment commits (§4.5).

A disconnected member with a valid reservation can read the snapshot during reconnection.

Volume, fullscreen, and local buffering state are not synchronized through this response.

#### D. Get Chat History

```http
GET /api/v1/rooms/{roomId}/messages?limit=50
```

Response:

```json
{
  "items": [
    {
      "id": 42,
      "senderUserId": "11111111-1111-4111-8111-111111111111",
      "senderDisplayName": "Minh",
      "content": "Xin chào mọi người!",
      "createdAt": "2026-11-15T08:15:00Z"
    }
  ]
}
```

Return the latest messages in chronological order. `senderDisplayName` is the sender's current display name, joined
from `app_users`, so history stays renderable after the sender has left.

- Default limit: `50`.
- Maximum limit: `50`.
- Only authorized members can retrieve messages.
- History is inaccessible after the room ends.

#### E. Leave Room

```http
POST /api/v1/rooms/{roomId}/leave
```

No request body.

Example response:

```json
{
  "outcome": "HOST_TRANSFERRED",
  "newHostUserId": "44444444-4444-4444-8444-444444444444"
}
```

Possible outcomes:

- `LEFT` — a regular member left, or the Host left while only reserved (`DISCONNECTED`) members remained; in the latter
  case the room stays `ACTIVE` awaiting a Host.
- `HOST_TRANSFERRED` — the Host left and ownership passed to the earliest-joined `CONNECTED` member.
- `ROOM_ENDED` — the Host left and no `CONNECTED` or unexpired `DISCONNECTED` member remained (`LAST_MEMBER_LEFT`).

`newHostUserId` is populated only when ownership is transferred.

Host Leave follows Watch Party Design §5.2 (Cases A–C) in a single transaction. The frontend confirmation dialog does
not
require a separate API.

#### F. Transfer Host

```http
POST /api/v1/rooms/{roomId}/transfer-host
```

Request:

```json
{
  "targetMembershipId": "33333333-3333-4333-8333-333333333333"
}
```

Response:

```json
{
  "hostUserId": "11111111-1111-4111-8111-111111111111"
}
```

The target must be another connected, eligible member.

Host transfer must be atomic.

#### G. Kick Member

```http
POST /api/v1/rooms/{roomId}/members/{membershipId}/kick
```

Response: `204 No Content`

The target membership transitions to `REMOVED`, its slot is released, and the corresponding active room connection is
invalidated. The removed user may Join again with a valid invitation code, or by `roomId` for a `PUBLIC` room (§2.3 B),
which
requires a new REST Join and a new successful WebSocket `AUTH`.

The Host cannot kick themselves.

#### H. End Room

```http
POST /api/v1/rooms/{roomId}/end
```

Response: `204 No Content`

The room transitions to `ENDED`.

Protected room operations and chat access become unavailable.

All room mutations must validate the current room state and permissions on the server.

### 2.4. Public Room Discovery & Preview

| Method | Endpoint                 | Access | Purpose                        |
|--------|--------------------------|--------|--------------------------------|
| `GET`  | `/public-rooms`          | Public | List discoverable PUBLIC rooms |
| `GET`  | `/public-rooms/{roomId}` | Public | Preview one PUBLIC room        |

Both endpoints are unauthenticated, read-only, and rate-limited (`429 RATE_LIMITED`). Joining still requires
authentication through `POST /rooms/join` with `roomId`.

**List query parameters:** `page` (default `0`) and `size` (default `20`, maximum `50`). Rooms are ordered by newest
`createdAt` first; the MVP has no search, filtering, or sorting options. The response uses the standard pagination
envelope (`items`, `page`, `size`, `hasNext`).

`PublicRoomSummary`:

```json
{
  "roomId": "22222222-2222-4222-8222-222222222222",
  "hostDisplayName": "Minh",
  "selection": {
    "providerKey": "provider-a",
    "movieId": "movie-123",
    "episodeId": "ep-01"
  },
  "participantCount": 3,
  "capacity": 6,
  "available": true,
  "createdAt": "2026-11-15T08:00:00Z"
}
```

- `hostDisplayName` is `null` when `host_user_id` is null (the room awaits a Host) **or** the recorded Host's membership
  is neither `CONNECTED` nor `DISCONNECTED` with an unexpired reconnect deadline. This is evaluated against the current
  time without mutating state, so an expired Host's name never leaks before cleanup. A Host within the 30-second grace
  keeps their name. `selection` is `null` when no movie is selected.
- `participantCount` counts `CONNECTED` and unexpired `DISCONNECTED` members (the slots counted for capacity);
  `available` is `participantCount < capacity`.
- `selection` carries provider identifiers only. The frontend resolves title and poster through
  `GET /movies/{providerKey}/{movieId}`; discovery never calls the provider.
- Never exposed: `inviteCode`, user IDs, membership IDs, member names, chat, connection state, or any PRIVATE room.

**Discovery List eligibility.** A room is listed only if it is `PUBLIC`, `ACTIVE`, within both expiration deadlines (the
four-hour maximum and the 15-minute empty-room deadline, checked read-only against the current time even if cleanup has
not run), and has `participantCount >= 1`. A `PUBLIC` room with no valid occupied slot is not listed.

**Direct Preview/Join eligibility.** `GET /public-rooms/{roomId}` and `POST /rooms/join` with `roomId` require only a
`PUBLIC` room that is `ACTIVE` and within both deadlines; the `participantCount >= 1` filter does not apply. An unlisted
room with no valid slot can therefore still be previewed (`participantCount` 0, `hostDisplayName` `null`) and joined by
ID, with no additional state or recovery rule. Preview returns the same restricted `PublicRoomSummary`. An unknown or
`PRIVATE` ID returns `404 ROOM_NOT_FOUND` (indistinguishable); an `ENDED` or expired room returns `410 ROOM_ENDED`.
Both endpoints are read-only and take no room lock.

**Moderation limitation.** Kick behaves identically for both visibilities (§2.3 G). A kicked user may Join again by
invitation code, or by `roomId` for a `PUBLIC` room, while the room is joinable. Ban is outside the MVP.

## 3. WebSocket Contract

### 3.1. Connection Establishment

The client follows this sequence:

1. Create or join a room through REST.
2. Open `wss://<backend-host>/ws/rooms/{roomId}`.
3. Immediately send an `AUTH` message.
4. Backend validates the access token, membership, and room availability.
5. Under the room lock, the backend assigns the connection, marks the member `CONNECTED`, clears the reconnect
   deadline and `empty_since`, and — only if the room is awaiting a Host — makes this user the Host.
6. Backend returns `ROOM_SNAPSHOT`.
7. Frontend requests recent chat history through REST.

The connection identifier is generated by the server.

Only one active Watch Party connection per user per room is permitted.

When a user connects from another device, the new connection replaces the previous one without creating another
membership or triggering Host failover.

### 3.2. Message Envelope

WebSocket messages use JSON.

Example:

```json
{
  "type": "PLAYBACK_COMMAND",
  "requestId": "client-generated-uuid",
  "roomId": "22222222-2222-4222-8222-222222222222",
  "payload": {}
}
```

| Field        | Description                                         |
|--------------|-----------------------------------------------------|
| `type`       | Message type                                        |
| `requestId`  | Correlates client request with acknowledgment/error |
| `roomId`     | Associated Watch Party                              |
| `payload`    | Message-specific data                               |
| `occurredAt` | Server timestamp on broadcast events                |

`requestId` is required for client requests but does not guarantee durable idempotency.

### 3.3. Client → Server Messages

| Type               | Purpose                            | Permission               |
|--------------------|------------------------------------|--------------------------|
| `AUTH`             | Authenticate and attach connection | Valid token + membership |
| `STATE_REQUEST`    | Request latest room snapshot       | Connected member         |
| `PLAYBACK_COMMAND` | Change shared playback             | Connected Host           |
| `CHAT_SEND`        | Send chat message                  | Connected member         |

#### AUTH

```json
{
  "type": "AUTH",
  "requestId": "auth-001",
  "payload": {
    "accessToken": "<supabase-access-token>"
  }
}
```

`AUTH` must be the first message on a new socket. A later `AUTH` on the same authenticated socket renews the token (
§1.1).

An unsuccessful authentication produces an error and closes the socket without exposing room data.

#### STATE_REQUEST

```json
{
  "type": "STATE_REQUEST",
  "requestId": "state-001",
  "payload": {}
}
```

The server responds with `ROOM_SNAPSHOT`.

#### PLAYBACK_COMMAND

Supported actions:

| Action   | Required data | Behavior                            |
|----------|---------------|-------------------------------------|
| `SELECT` | `selection`   | Change movie/episode, pause at zero |
| `PLAY`   | None          | Resume shared playback              |
| `PAUSE`  | None          | Pause at authoritative position     |
| `SEEK`   | `positionMs`  | Change shared playback position     |

Every command includes `expectedVersion`.

**SELECT example:**

```json
{
  "type": "PLAYBACK_COMMAND",
  "requestId": "cmd-001",
  "payload": {
    "action": "SELECT",
    "expectedVersion": 3,
    "selection": {
      "providerKey": "provider-a",
      "movieId": "movie-123",
      "episodeId": "ep-01"
    }
  }
}
```

**SEEK example:**

```json
{
  "type": "PLAYBACK_COMMAND",
  "requestId": "cmd-002",
  "payload": {
    "action": "SEEK",
    "expectedVersion": 7,
    "positionMs": 45000
  }
}
```

Playback commands are accepted only when:

- The sender is the current connected Host.
- The room is active.
- The selection or position is valid.
- `expectedVersion` matches the authoritative playback version.

For `SELECT`, provider validation happens before the database transaction; the commit revalidates room state, Host
eligibility, the sender's connection, and `expectedVersion`.

Accepted commands update the playback snapshot and increment its version exactly once.

A version mismatch produces `STALE_PLAYBACK_VERSION`; the client must resynchronize.

Clients should await acknowledgments or serialize successive Host commands.

#### CHAT_SEND

```json
{
  "type": "CHAT_SEND",
  "requestId": "chat-001",
  "payload": {
    "content": "Xin chào mọi người!"
  }
}
```

The backend validates room membership and message content before persistence and broadcast.

Message content must be plain text, nonblank, and limited to the database-supported maximum.

Chat messages do not change the playback state version.

Without persistent client-message idempotency keys, clients must not blindly retry unacknowledged sends.

### 3.4. Server → Client Messages

| Type               | Purpose                                     |
|--------------------|---------------------------------------------|
| `ROOM_SNAPSHOT`    | Full authoritative room snapshot            |
| `ROOM_CHANGED`     | Notify membership or Host changes           |
| `PLAYBACK_UPDATED` | Broadcast new authoritative playback        |
| `CHAT_MESSAGE`     | Deliver persisted chat message              |
| `ROOM_ENDED`       | Notify room termination                     |
| `COMMAND_ACK`      | Confirm accepted client command             |
| `MEMBER_KICKED`    | Notify removed member and close connection  |
| `SESSION_REPLACED` | Notify old connection of device replacement |
| `ERROR`            | Report command or connection errors         |

#### ROOM_CHANGED

Payload:

```json
{
  "reason": "HOST_CHANGED"
}
```

Supported reasons:

- `MEMBER_JOINED`
- `MEMBER_LEFT`
- `MEMBER_EXPIRED`
- `MEMBER_DISCONNECTED`
- `MEMBER_RECONNECTED`
- `HOST_CHANGED`
- `MEMBER_REMOVED`

Clients receiving this event retrieve the current room snapshot.

No additional global room version is introduced in the MVP database.

#### PLAYBACK_UPDATED

Example:

```json
{
  "type": "PLAYBACK_UPDATED",
  "roomId": "22222222-2222-4222-8222-222222222222",
  "occurredAt": "2026-11-15T08:10:00Z",
  "payload": {
    "selection": {
      "providerKey": "provider-a",
      "movieId": "movie-123",
      "episodeId": "ep-01"
    },
    "status": "PLAYING",
    "positionMs": 45000,
    "referenceAt": "2026-11-15T08:10:00Z",
    "version": 8
  }
}
```

The client uses the authoritative reference position, reference timestamp, and local playback observations to correct
meaningful drift.

Older versions must be ignored.

#### CHAT_MESSAGE

```json
{
  "type": "CHAT_MESSAGE",
  "roomId": "22222222-2222-4222-8222-222222222222",
  "payload": {
    "id": 42,
    "senderUserId": "11111111-1111-4111-8111-111111111111",
    "senderDisplayName": "Minh",
    "content": "Xin chào mọi người!",
    "createdAt": "2026-11-15T08:15:00Z"
  }
}
```

Clients can deduplicate received messages by server-assigned message ID.

#### ROOM_ENDED

Payload:

```json
{
  "reason": "HOST_ENDED"
}
```

Supported reasons:

- `HOST_ENDED`
- `MAX_LIFETIME`
- `EMPTY_TIMEOUT`
- `LAST_MEMBER_LEFT`

#### COMMAND_ACK

The server echoes the original `requestId` and includes the resulting playback version or message ID when applicable.

#### ERROR

The server provides a stable error code, readable message, and `requestId` where available.

For stale playback commands, it may also include `currentVersion`.

### 3.5. Connection Security & Delivery

The backend must:

- Validate the authenticated identity during WebSocket attachment.
- Recheck membership, active connection, and Host permissions for each command.
- Reject commands from superseded connections.
- Invalidate sockets when members are removed or rooms end.
- Close sockets whose JWT expires without a renewing `AUTH` (§1.1), unless a freshly authenticated replacement has
  already attached.
- Restrict WebSocket origins and use TLS in production.
- Apply input-size limits and reasonable rate limits.
- Detect dead connections with WebSocket ping/idle timeouts and treat a detected loss as a disconnect, applied only if
  it matches the membership's current `connection_id`.

The backend commits authoritative state before broadcasting realtime events.

WebSocket delivery is best-effort. The MVP does not maintain a durable event log or guarantee exactly-once delivery.

Clients must recover through authoritative snapshots when events are missed, delayed, or reordered.

## 4. Required Client Flows

### 4.1. Watch Alone

```text
Browse Catalog
    ↓
Get Movie Details
    ↓
Authenticate if needed
    ↓
Get Playback Source
    ↓
Play HLS Locally
```

Local volume, fullscreen, pause, and seek remain client-side.

### 4.2. Create Watch Party

```text
Authenticated User
    ↓
POST /rooms
    ↓
Open Room WebSocket
    ↓
AUTH
    ↓
ROOM_SNAPSHOT
    ↓
GET Recent Messages
    ↓
Watch Party Ready
```

### 4.3. Join Through Invitation

```text
Open Invitation Link
    ↓
Login if necessary
    ↓
POST /rooms/join
    ↓
Open Room WebSocket
    ↓
AUTH
    ↓
ROOM_SNAPSHOT
    ↓
Load Chat and Media Source
```

After authentication, the frontend must preserve the intended invitation destination.

### 4.4. Reconnect

```text
Connection Lost
    ↓
Membership Reserved
    ↓
Reconnect Before Deadline?
    ├── Yes → WebSocket AUTH → Snapshot
    └── No  → REST Join Again
                  ↓
             Capacity Check
                  ↓
             WebSocket AUTH
```

A reconnecting client must replace locally estimated shared state with the current authoritative snapshot.

Opening the same room on another device replaces the previous connection without consuming an additional slot.

### 4.5. Host Recovery

If the Host disconnects, the backend follows the 30-second grace period. After it expires (the Host's membership becomes
`EXPIRED`) — and likewise after a voluntary Host Leave with only reserved members remaining — one transaction decides:

- If a `CONNECTED` successor exists, ownership transfers to the earliest-joined one.
- Otherwise `hostUserId` becomes `null` and the room stays `ACTIVE` awaiting a Host, under its empty-room timeout
  (`EMPTY_TIMEOUT`) and four-hour limit (`MAX_LIFETIME`). Host-only commands fail with `HOST_ONLY`.
- **The first user whose WebSocket attachment commits becomes Host.** REST Join and slot reservation alone never grant
  Host ownership, including a Join by the previous Host.
- The previous Host may become Host again only by being that first attachment. If another user is already Host, they are
  a regular Member.

### 4.6. Leave and End

```text
POST /leave or /end
    ↓
Database Transaction
    ↓
Commit Updated State
    ↓
ROOM_CHANGED / ROOM_ENDED
    ↓
Update Clients
```

Clients missing an event must detect the latest state through snapshot requests or an appropriate room-ended error.

### 4.7. Discover and Join a Public Room

```text
GET /public-rooms (no login)
    ↓
Preview: GET /public-rooms/{roomId}
    ↓
Login if necessary
    ↓
POST /rooms/join { roomId }
    ↓
Open Room WebSocket → AUTH → ROOM_SNAPSHOT
```

After login the frontend returns the user to the previewed room. From the Join step onward the flow is identical to
§4.3 and §4.4.

## 5. Open Contract Decisions

The following details remain to be finalized before implementing the affected behavior:

| Area                | Decision                                                   |
|---------------------|------------------------------------------------------------|
| Movie provider      | Validate movie, episode, and playback-source identifiers   |
| WebSocket operation | Finalize heartbeat intervals, close codes, and rate limits |

These decisions do not currently require additional endpoints or database entities.

## 6. Related Documents

- [Product Requirements](../product/requirements.md)
- [System Architecture](../architecture/architecture.md)
- [Watch Party Design](watch-party.md)
- [Database Design](database.md)

---

*This document defines the proposed MVP API contract. Final provider compatibility, validation, and concurrent behavior
will be verified through integration and contract tests.*