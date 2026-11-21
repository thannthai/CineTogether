package com.thanh.cinetogether.identity.application;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.thanh.cinetogether.identity.domain.DisplayNames;
import com.thanh.cinetogether.identity.persistence.AppUser;
import com.thanh.cinetogether.identity.persistence.AppUsers;

@Service
public class Profiles {

    private final AppUsers appUsers;
    private final Clock clock;

    Profiles(AppUsers appUsers, Clock clock) {
        this.appUsers = appUsers;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Profile get(UUID userId) {
        // ProfileProvisioningFilter creates the profile before any controller runs, so it must exist here.
        AppUser user = appUsers.findById(userId)
                .orElseThrow(() -> new IllegalStateException("No profile for authenticated user " + userId));
        return new Profile(user.getId(), user.getDisplayName(), user.getCreatedAt());
    }

    // The entity is managed, so Hibernate saves the change when the transaction commits.
    @Transactional
    public Profile rename(UUID userId, String newName) {
        String displayName = DisplayNames.validated(newName);
        AppUser user = appUsers.findById(userId)
                .orElseThrow(() -> new IllegalStateException("No profile for authenticated user " + userId));
        user.rename(displayName, Instant.now(clock));
        return new Profile(user.getId(), user.getDisplayName(), user.getCreatedAt());
    }

}
