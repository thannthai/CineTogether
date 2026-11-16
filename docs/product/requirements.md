# CineTogether — Product Requirements

**Version:** 0.7  
**Status:** Draft — Core MVP Requirements Defined  
**Phase:** Planning & Design

## 1. Introduction

This document defines the MVP scope, functional requirements, business rules, and acceptance criteria for CineTogether.

CineTogether provides two core experiences:

- **Watch Alone:** Browse and watch movies individually.
- **Watch Together:** Watch the same movie with others through synchronized playback and real-time chat.

The MVP supports private and public Watch Party rooms for small groups, with an emphasis on usability, synchronization,
and
reliability.

## 2. MVP Scope

| Area                  | MVP Features                                                                     |
|-----------------------|----------------------------------------------------------------------------------|
| Authentication        | Registration, login, logout                                                      |
| Movie Catalog         | Browse, search, movie details                                                    |
| Watch Alone           | Movie playback, episode selection, basic player controls                         |
| Watch Party           | Create PRIVATE or PUBLIC rooms; join through invitation links or public room IDs |
| Public Rooms          | Discovery page, public preview, join by public room ID                           |
| Synchronized Playback | Host-controlled movie selection, Play, Pause, Seek                               |
| Room Management       | Membership, Host transfer, kick, leave, room expiration                          |
| Realtime Chat         | Room messaging with temporary history                                            |
| Connection Recovery   | Reconnection, playback state recovery, Host failover                             |

Screen sharing, microphone, camera, and advanced movie management are outside the MVP.

## 3. Functional Requirements

### 3.1. Authentication & Access

- **FR-AUTH-01:** Visitors can browse, search, and view movie details, and browse and preview public Watch Party
  rooms (FR-PUB-04), without authentication.
- **FR-AUTH-02:** Users must log in before watching a movie.
- **FR-AUTH-03:** Users must log in before creating or joining a Watch Party.
- **FR-AUTH-04:** Users can register, log in, and log out.
- **FR-AUTH-05:** The backend validates authentication and authorization for protected operations.

### 3.2. Movie Catalog & Watch Alone

- **FR-MOV-01:** Visitors can browse, search, and view available movies.
- **FR-MOV-02:** Authenticated users can watch supported movies.
- **FR-MOV-03:** Users can select available episodes.
- **FR-MOV-04:** The player supports Play, Pause, Seek, Volume, and Fullscreen.
- **FR-MOV-05:** Movie metadata and playback sources are obtained through a supported movie provider.

Watchlist, watch history, continue watching, and recommendations are outside the MVP.

### 3.3. Watch Party — Room Management

- **FR-ROOM-01:** Authenticated users can create Watch Party rooms and choose PRIVATE or PUBLIC visibility
  (FR-PUB-01).
- **FR-ROOM-02:** Authenticated users can join rooms through valid invitation links, or through the public room
  identifier for PUBLIC rooms (FR-PUB-06).
- **FR-ROOM-03:** A room supports up to 6 participants, including the Host and reserved slots.
- **FR-ROOM-04:** Participants can view room members and leave voluntarily.
- **FR-ROOM-05:** The Host can kick participants or end the room. A kick removes the participant's current
  participation and releases its slot; it is not a ban (see FR-CONN-06).
- **FR-ROOM-06:** The Host can manually transfer ownership to another connected participant.
- **FR-ROOM-07:** When the Host chooses to leave, the application displays options to cancel, transfer ownership, or
  leave.
- **FR-ROOM-08:** If the Host leaves without manually transferring ownership and another connected participant exists,
  the system automatically transfers Host ownership to the earliest-joined connected participant, and the room remains
  active.
- **FR-ROOM-09:** If the Host voluntarily leaves and no other participant is connected, but at least one disconnected
  participant's reservation has not expired, the Host leaves immediately and the room remains active without an eligible
  Host until recovery (FR-HOST-07). If no connected participant and no unexpired reserved participant remains, the room
  ends.
- **FR-ROOM-10:** A room automatically expires 4 hours after creation.
- **FR-ROOM-11:** A room automatically closes after having no connected participants for 15 consecutive minutes,
  including a newly created room that no one has connected to.
- **FR-ROOM-12:** The backend enforces membership, capacity, and Host permissions.

### 3.4. Watch Party — Host Disconnection

- **FR-HOST-01:** Host disconnection does not immediately transfer ownership.
- **FR-HOST-02:** The Host has 30 seconds to reconnect before automatic ownership transfer.
- **FR-HOST-03:** If the Host remains disconnected, ownership transfers to the earliest-joined connected participant.
- **FR-HOST-04:** If the Host reconnects before the transfer, their Host role is preserved.
- **FR-HOST-05:** A former Host does not automatically regain ownership after transfer.
- **FR-HOST-06:** If no participants are connected, the room follows its empty-room expiration policy.
- **FR-HOST-07:** If the Host loses eligibility (reconnection period expired, voluntary leave, or removal) and no
  connected successor exists, the room remains active without an eligible Host. The first eligible user whose realtime
  connection is successfully established becomes the new Host. Joining through an invitation alone does not grant Host
  ownership. The previous Host may become Host again only by being that first eligible user.
- **FR-HOST-08:** A user who is not the current Host cannot perform Host-only operations. A former Host who rejoins
  while the room awaits a Host gains nothing until their realtime connection is the first established; if another user
  became Host meanwhile, they rejoin as a regular member.

Technical handling of Host transfer is defined in [Watch Party Design](../design/watch-party.md).

### 3.5. Watch Party — Synchronized Playback

Each participant streams the selected movie through their own CineTogether video player. The backend manages the
authoritative shared playback state.

- **FR-SYNC-01:** All participants in a room watch the same selected movie and episode.
- **FR-SYNC-02:** Only the Host can select or change the shared movie or episode.
- **FR-SYNC-03:** Changing content updates all participants' selection and resets shared playback to Paused at the
  beginning.
- **FR-SYNC-04:** Only the Host can perform shared Play, Pause, and Seek operations.
- **FR-SYNC-05:** Non-Host participants cannot deliberately control shared playback or seek independently.
- **FR-SYNC-06:** Volume and Fullscreen are controlled locally by each participant.
- **FR-SYNC-07:** The backend validates playback commands and distributes updated playback state.
- **FR-SYNC-08:** Clients apply the latest valid shared playback state and ignore outdated updates.
- **FR-SYNC-09:** Clients periodically check their playback position against the expected room position.
- **FR-SYNC-10:** When playback drift becomes significant, the affected client attempts to resynchronize.
- **FR-SYNC-11:** Individual buffering does not automatically pause the entire room.
- **FR-SYNC-12:** After buffering or reconnection, the player attempts to resume at the current shared playback
  position.
- **FR-SYNC-13:** If the browser blocks automatic playback, the user is prompted to interact with the player.

The MVP aims for reasonably synchronized playback, not frame-perfect synchronization.

### 3.6. Realtime Chat

- **FR-CHAT-01:** Authorized room participants can send and receive text messages in real time.
- **FR-CHAT-02:** Recent chat history remains available while the room is active.
- **FR-CHAT-03:** Participants joining or reconnecting can retrieve up to 50 recent messages.
- **FR-CHAT-04:** Chat history becomes inaccessible to users when the room ends.
- **FR-CHAT-05:** Temporary chat data must be deleted within 24 hours after room termination.
- **FR-CHAT-06:** The system validates chat messages and enforces room membership before accepting messages.

Permanent chat history is outside the MVP.

### 3.7. Connection & Recovery

- **FR-CONN-01:** A disconnected participant retains room membership and a reserved slot for 30 seconds.
- **FR-CONN-02:** Reconnecting within the grace period restores access to the existing room membership.
- **FR-CONN-03:** After 30 seconds without reconnection, the participant's membership and reserved slot expire.
- **FR-CONN-04:** Participants whose membership has expired must join again through the normal room access process.
- **FR-CONN-05:** Reconnecting participants retrieve the latest room and playback state.
- **FR-CONN-06:** A kicked participant's connection and reconnect reservation are invalidated and cannot be used to
  regain access. The participant may Join the same active room again through a valid invitation (or, for a PUBLIC room,
  its room identifier), subject to capacity;
  the existing membership is reused with a fresh join time and a new 30-second reservation, and a new successful
  realtime authentication is required.
- **FR-CONN-07:** The system distinguishes room membership from online connection status.
- **FR-CONN-08:** Room capacity checks include both connected participants and temporarily reserved slots. Expired
  reservations no longer count, even if cleanup has not yet run.
- **FR-CONN-09:** A membership created by Create or Join is reserved for 30 seconds pending the participant's realtime
  connection. Repeated Join requests do not extend an unexpired reservation.

### 3.8. Public Rooms

PUBLIC rooms reuse every Watch Party rule in this document; visibility only changes how a room is entered and
discovered.

- **FR-PUB-01:** When creating a room, the Host chooses PRIVATE or PUBLIC visibility. The default is PRIVATE. Changing
  visibility after creation is not part of the MVP.
- **FR-PUB-02:** PRIVATE rooms are accessible only through their invitation link/code and never appear in discovery or
  preview.
- **FR-PUB-03:** A PUBLIC room appears in the discovery list only when it is ACTIVE, neither the four-hour maximum
  lifetime nor the 15-minute empty-room deadline has passed, and at least one membership occupies a valid slot
  (CONNECTED, or DISCONNECTED with an unexpired reconnect deadline). ENDED or expired rooms never appear.
- **FR-PUB-04:** Visitors without authentication can browse the discovery list and preview a PUBLIC room directly by its
  room identifier. A PUBLIC room with no valid occupied slot is excluded from the list but remains previewable and
  joinable by room identifier while it is ACTIVE and neither deadline has passed.
- **FR-PUB-05:** Discovery and preview expose only `roomId`, `hostDisplayName`, `selection` (provider key, movie ID,
  episode ID), `participantCount`, `capacity`, `available`, and `createdAt`. `hostDisplayName` is null when the room is
  awaiting a Host or the recorded Host's reconnect grace has expired. They never expose invitation codes, user IDs,
  membership IDs, participant identities, chat, connection details, or any PRIVATE room.
- **FR-PUB-06:** Authentication is required to join any room. A PUBLIC room can be joined through its public room
  identifier or its invitation code, subject to the same capacity and lifecycle validation as any room.
- **FR-PUB-07:** A PUBLIC room awaiting a Host remains subject to FR-HOST-07: the first eligible user whose realtime
  connection is established becomes Host.
- **FR-PUB-08:** Kick behaves identically in PRIVATE and PUBLIC rooms. Ban is outside the MVP, so a kicked participant
  in a PUBLIC room can rejoin while the room remains joinable (FR-CONN-06).

## 4. Business Rules

### 4.1. Room Lifecycle

- **BR-ROOM-01:** Each active room has at most one designated Host; it has none only while awaiting a Host
  (FR-HOST-07).
- **BR-ROOM-02:** A room cannot exceed 6 occupied or reserved membership slots.
- **BR-ROOM-03:** Room access requires authentication and valid authorization.
- **BR-ROOM-04:** The Host can end the room at any time.
- **BR-ROOM-05:** A room expires 4 hours after creation.
- **BR-ROOM-06:** A room closes after 15 consecutive minutes without connected participants.
- **BR-ROOM-07:** Ended rooms cannot be rejoined through their previous invitation links.

### 4.2. Host Ownership

- **BR-HOST-01:** The room creator becomes the initial Host.
- **BR-HOST-02:** Only the current Host can manually transfer ownership.
- **BR-HOST-03:** Automatic transfer selects the earliest-joined eligible connected participant.
- **BR-HOST-04:** Voluntary Host departure triggers immediate transfer when an eligible successor exists.
- **BR-HOST-05:** Unexpected Host disconnection allows 30 seconds for reconnection before transfer.
- **BR-HOST-06:** A former Host does not automatically regain ownership after transfer.
- **BR-HOST-07:** Host-only operations must be authorized by the backend.
- **BR-HOST-08:** When no eligible Host exists, the first eligible user to establish a realtime connection becomes
  Host. The assignment is atomic and is never granted by invitation Join alone.
- **BR-HOST-09:** Voluntary Host departure: transfer to the earliest-joined connected member if one exists; otherwise,
  if unexpired reserved members remain, the Host leaves and the room awaits a new Host; otherwise the room ends with
  `LAST_MEMBER_LEFT`.

### 4.3. Playback Synchronization

- **BR-SYNC-01:** The backend is authoritative for shared movie selection and playback state.
- **BR-SYNC-02:** Only the current Host can change shared playback state.
- **BR-SYNC-03:** Local Volume and Fullscreen changes do not affect other participants.
- **BR-SYNC-04:** Individual buffering does not pause the entire room.
- **BR-SYNC-05:** Clients recover toward the latest valid shared playback state.
- **BR-SYNC-06:** Frame-perfect playback synchronization is not required.

### 4.4. Chat & Content

- **BR-CHAT-01:** Chat history is temporary and scoped to the room.
- **BR-CHAT-02:** Chat history is inaccessible after room termination and deleted within 24 hours.
- **BR-MOV-01:** Watch Party playback uses only movies supported by CineTogether.
- **BR-MOV-02:** Screen sharing and arbitrary external media streams are not supported in the MVP.
- **BR-MOV-03:** Production playback must use content sources with appropriate usage rights.

### 4.5. Room Visibility

- **BR-PUB-01:** PRIVATE and PUBLIC rooms follow identical rules for membership, capacity, Host ownership,
  synchronized playback, chat, reconnection, Kick, and expiration.
- **BR-PUB-02:** Visibility never grants Host privileges, bypasses authentication, or bypasses capacity limits.
- **BR-PUB-03:** An ENDED room is not joinable and is not listed or previewed as active.

## 5. Non-Functional Requirements

### Security

- Enforce authentication, room membership, and Host permissions on the backend.
- Protect invitation links and prevent unauthorized room operations.
- Public discovery and preview must never expose PRIVATE rooms, invitation codes, or participant identities.
- Keep credentials and application secrets out of frontend code and version control.

### Reliability

- Support recovery after temporary connection loss.
- Prevent outdated playback updates from overriding newer state.
- Handle concurrent joins, membership changes, and Host transfers consistently.
- Avoid room-wide interruptions caused by one participant buffering.

### Performance

- Support rooms containing up to 6 participants.
- Provide responsive realtime chat and playback control for small groups.
- Do not require frame-perfect synchronization.

### Maintainability & Testability

- Keep important business rules independently testable.
- Test authorization, room lifecycle, reconnection, and playback behavior.
- Keep movie provider integrations replaceable.

## 6. MVP Acceptance Criteria

### Authentication & Watch Alone

Visitors can browse movies without logging in. Authentication is required before movie playback.

### Private and Public Watch Party

Authenticated users can create PRIVATE or PUBLIC rooms. PRIVATE rooms admit authenticated participants through
invitation links; both types let participants watch the same movie.

### Public Room Discovery

Visitors can browse and preview ACTIVE PUBLIC rooms without logging in; PRIVATE and ENDED rooms never appear. A PUBLIC
room with no occupied slot is not listed but can still be
previewed and joined by ID until it expires. Joining
requires authentication and follows the normal capacity and lifecycle rules.

### Room Capacity

Rooms cannot exceed 6 participants, including temporarily reserved slots.

### Host Permissions

Only the current Host can change movies, episodes, and shared playback state.

### Host Departure & Disconnection

Voluntary departure transfers Host ownership when a successor exists. Unexpected disconnection allows 30 seconds for
recovery before automatic transfer. If no connected successor exists, the room stays active awaiting a Host: the first
eligible participant to connect becomes Host, until the room expires or ends. Joining through an invitation alone never
grants Host ownership.

### Synchronized Playback

Connected participants follow the latest shared playback state. Clients recover from buffering, outdated events, and
reconnection without requiring frame-perfect synchronization.

### Reconnection

Participants reconnecting within 30 seconds recover their existing membership. After expiration, they must join again.

### Realtime Chat

Participants exchange messages, retrieve up to 50 recent messages, and lose access to chat history once the room ends.

### Room Expiration

Rooms end after 4 hours, after 15 consecutive minutes without connected participants, or when explicitly ended by the
Host.

### Authorization

Unauthorized users and removed participants cannot perform protected room operations until the removed participant rejoins through a valid invitation code, or by room ID for a PUBLIC room, subject to authentication and capacity checks.

## 7. Out of Scope — MVP

- Public room search, filtering, ranking, and moderation tooling beyond Host kick, including Ban.
- Screen sharing and Presenter roles.
- Microphone and camera communication.
- Watchlist, watch history, and continue watching.
- Advanced recommendations.
- Permanent chat history.
- Advanced community moderation.
- Large-scale distributed infrastructure.
- Custom media transcoding and streaming infrastructure.

## 8. Remaining Open Decisions

| Area                     | Open Decision                                                                     |
|--------------------------|-----------------------------------------------------------------------------------|
| Playback Synchronization | Final drift tolerance, correction algorithm, and handling of persistent buffering |
| Movie Provider           | Validate playback compatibility and legal content usage rights for production     |
| Authentication           | Verify the Supabase project's signing-key/JWKS configuration                      |

### Movie Provider Note

KKPhim is the current candidate movie provider. Technical experiments have demonstrated movie metadata retrieval and HLS
playback.

However, synchronized playback compatibility and content usage rights have not been fully validated.

The application should support replacing the movie provider without redesigning its core business logic.

## 9. Related Documents

- [Product Overview](overview.md)
- [System Architecture](../architecture/architecture.md)
- [Watch Party Design](../design/watch-party.md)
- [Database Design](../design/database.md)
- [API Design](../design/api.md)

---

*This document is a living specification and will evolve alongside CineTogether's design and implementation.*