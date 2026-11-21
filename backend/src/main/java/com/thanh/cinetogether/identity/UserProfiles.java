package com.thanh.cinetogether.identity;

import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class UserProfiles {

    private final JdbcClient jdbc;

    UserProfiles(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    // DO NOTHING, not DO UPDATE: an existing profile (possibly renamed by the user) must stay as it is.
    @Transactional
    void createIfAbsent(UUID userId, String displayName) {
        jdbc.sql("""
                        INSERT INTO app_users (id, display_name, created_at, updated_at)
                        VALUES (:id, :displayName, now(), now())
                        ON CONFLICT (id) DO NOTHING
                        """)
                .param("id", userId)
                .param("displayName", displayName)
                .update();
    }

}
