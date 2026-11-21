package com.thanh.cinetogether.identity;

import java.util.Map;
import java.util.UUID;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/** Creates the caller's profile on their first authenticated request. */
@Component
public class ProfileProvisioner {

    private final UserProfiles userProfiles;

    ProfileProvisioner(UserProfiles userProfiles) {
        this.userProfiles = userProfiles;
    }

    public void ensureProfile(Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        String displayName = DisplayNames.initialDisplayName(userId, displayNameClaim(jwt),
                jwt.getClaimAsString("email"));
        userProfiles.createIfAbsent(userId, displayName);
    }

    // user_metadata is client-editable, so it only suggests a name; DisplayNames cleans it up.
    private static String displayNameClaim(Jwt jwt) {
        Map<String, Object> userMetadata = jwt.getClaimAsMap("user_metadata");
        if (userMetadata != null && userMetadata.get("display_name") instanceof String name) {
            return name;
        }
        return null;
    }
}
