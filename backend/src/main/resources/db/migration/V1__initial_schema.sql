-- CineTogether initial schema (MVP): five tables per docs/design/database.md.
-- Business invariants spanning several rows (capacity, Host eligibility, playback authorization)
-- are enforced by transactional application logic, not by this schema.

-- ---------------------------------------------------------------------------
-- app_users: application profile; id is the Supabase Auth user id (JWT `sub`).
-- No FK to Supabase's auth.users because identity is managed externally.
-- ---------------------------------------------------------------------------
CREATE TABLE app_users
(
    id           UUID                     NOT NULL,
    display_name VARCHAR(80)              NOT NULL,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_app_users PRIMARY KEY (id),
    CONSTRAINT ck_app_users_display_name_not_blank CHECK (btrim(display_name, E' \t\r\n') <> '')
);

-- ---------------------------------------------------------------------------
-- watch_rooms: room identity, visibility, Host ownership, and lifecycle.
-- host_user_id is NULL only while an ACTIVE room awaits a Host.
-- ---------------------------------------------------------------------------
CREATE TABLE watch_rooms
(
    id           UUID                     NOT NULL,
    invite_code  VARCHAR(64)              NOT NULL,
    visibility   VARCHAR(7)               NOT NULL DEFAULT 'PRIVATE',
    host_user_id UUID,
    status       VARCHAR(10)              NOT NULL,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    empty_since  TIMESTAMP WITH TIME ZONE,
    ended_at     TIMESTAMP WITH TIME ZONE,
    end_reason   VARCHAR(24),

    CONSTRAINT pk_watch_rooms PRIMARY KEY (id),
    CONSTRAINT uq_watch_rooms_invite_code UNIQUE (invite_code),
    CONSTRAINT fk_watch_rooms_host_user FOREIGN KEY (host_user_id) REFERENCES app_users (id),
    CONSTRAINT ck_watch_rooms_expires_after_created CHECK (expires_at > created_at),
    CONSTRAINT ck_watch_rooms_status CHECK (status IN ('ACTIVE', 'ENDED')),
    CONSTRAINT ck_watch_rooms_visibility CHECK (visibility IN ('PRIVATE', 'PUBLIC')),
    CONSTRAINT ck_watch_rooms_end_reason CHECK (end_reason IS NULL OR
                                                end_reason IN ('HOST_ENDED', 'MAX_LIFETIME', 'EMPTY_TIMEOUT',
                                                               'LAST_MEMBER_LEFT')),
    CONSTRAINT ck_watch_rooms_lifecycle CHECK (
        (status = 'ACTIVE' AND ended_at IS NULL AND end_reason IS NULL)
            OR (status = 'ENDED' AND ended_at IS NOT NULL AND end_reason IS NOT NULL))
);

-- ---------------------------------------------------------------------------
-- room_memberships: participation, presence, and reconnect reservation.
-- ---------------------------------------------------------------------------
CREATE TABLE room_memberships
(
    id                    UUID                     NOT NULL,
    room_id               UUID                     NOT NULL,
    user_id               UUID                     NOT NULL,
    status                VARCHAR(16)              NOT NULL,
    joined_at             TIMESTAMP WITH TIME ZONE NOT NULL,
    status_changed_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    connection_id         UUID,
    reconnect_deadline_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT pk_room_memberships PRIMARY KEY (id),
    CONSTRAINT uq_room_memberships_room_user UNIQUE (room_id, user_id),
    CONSTRAINT fk_room_memberships_room FOREIGN KEY (room_id) REFERENCES watch_rooms (id),
    CONSTRAINT fk_room_memberships_user FOREIGN KEY (user_id) REFERENCES app_users (id),
    CONSTRAINT ck_room_memberships_status CHECK (status IN ('CONNECTED', 'DISCONNECTED', 'LEFT', 'REMOVED', 'EXPIRED')),
    CONSTRAINT ck_room_memberships_presence CHECK (
        (status = 'CONNECTED' AND connection_id IS NOT NULL AND reconnect_deadline_at IS NULL)
            OR (status = 'DISCONNECTED' AND connection_id IS NULL AND reconnect_deadline_at IS NOT NULL)
            OR (status IN ('LEFT', 'REMOVED', 'EXPIRED') AND connection_id IS NULL AND
                reconnect_deadline_at IS NULL))
);

-- ---------------------------------------------------------------------------
-- room_playback_states: authoritative shared playback snapshot, one row per room.
-- Stores provider identifiers only (no stream URLs or movie metadata).
-- ---------------------------------------------------------------------------
CREATE TABLE room_playback_states
(
    room_id             UUID                     NOT NULL,
    provider_key        VARCHAR(50),
    movie_external_id   VARCHAR(255),
    episode_external_id VARCHAR(255),
    status              VARCHAR(10)              NOT NULL,
    position_ms         BIGINT                   NOT NULL,
    reference_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    version             BIGINT                   NOT NULL,

    CONSTRAINT pk_room_playback_states PRIMARY KEY (room_id),
    CONSTRAINT fk_room_playback_states_room FOREIGN KEY (room_id) REFERENCES watch_rooms (id),
    CONSTRAINT ck_room_playback_states_status CHECK (status IN ('PLAYING', 'PAUSED')),
    CONSTRAINT ck_room_playback_states_position CHECK (position_ms >= 0),
    CONSTRAINT ck_room_playback_states_version CHECK (version >= 0),
    CONSTRAINT ck_room_playback_states_selection_pair CHECK ((provider_key IS NULL) = (movie_external_id IS NULL)),
    CONSTRAINT ck_room_playback_states_episode_needs_movie CHECK (episode_external_id IS NULL OR movie_external_id IS NOT NULL),
    CONSTRAINT ck_room_playback_states_playing_needs_movie CHECK (status <> 'PLAYING' OR movie_external_id IS NOT NULL)
);

-- ---------------------------------------------------------------------------
-- room_chat_messages: temporary, append-only room chat.
-- ---------------------------------------------------------------------------
CREATE TABLE room_chat_messages
(
    id             BIGINT GENERATED ALWAYS AS IDENTITY,
    room_id        UUID                     NOT NULL,
    sender_user_id UUID                     NOT NULL,
    content        VARCHAR(1000)            NOT NULL,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT pk_room_chat_messages PRIMARY KEY (id),
    CONSTRAINT fk_room_chat_messages_room FOREIGN KEY (room_id) REFERENCES watch_rooms (id),
    CONSTRAINT fk_room_chat_messages_sender FOREIGN KEY (sender_user_id) REFERENCES app_users (id),
    CONSTRAINT ck_room_chat_messages_content_not_blank CHECK (btrim(content, E' \t\r\n') <> '')
);

-- ---------------------------------------------------------------------------
-- Indexes (database.md §4.2). The unique invite_code and (room_id, user_id)
-- constraints above already provide their supporting indexes.
-- ---------------------------------------------------------------------------
CREATE INDEX idx_room_chat_messages_room_recent
    ON room_chat_messages (room_id, created_at DESC, id DESC);

CREATE INDEX idx_watch_rooms_active_expires_at
    ON watch_rooms (expires_at) WHERE status = 'ACTIVE';

CREATE INDEX idx_watch_rooms_active_empty_since
    ON watch_rooms (empty_since) WHERE status = 'ACTIVE' AND empty_since IS NOT NULL;

CREATE INDEX idx_room_memberships_disconnected_deadline
    ON room_memberships (reconnect_deadline_at) WHERE status = 'DISCONNECTED';

CREATE INDEX idx_watch_rooms_public_discovery
    ON watch_rooms (created_at DESC) WHERE status = 'ACTIVE' AND visibility = 'PUBLIC';
