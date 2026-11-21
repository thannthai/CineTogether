package com.thanh.cinetogether.identity.application;

import java.time.Instant;
import java.util.UUID;

public record Profile(UUID id, String displayName, Instant createdAt) {
}
