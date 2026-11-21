package com.thanh.cinetogether.identity;

import java.util.Map;
import java.util.UUID;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import com.thanh.cinetogether.identity.domain.DisplayNames;
import com.thanh.cinetogether.identity.persistence.UserProfiles;

/** Creates the caller's profile on their first authenticated request. */
@Component
public class ProfileProvisioner {

    private final UserProfiles userProfiles;

    ProfileProvisioner(UserProfiles userProfiles) {
        this.userProfiles = userProfiles;
    }

    public void ensureProfile(Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        // user_metadata is client-editable, so it only suggests a name; DisplayNames cleans it up.
        Map<String, Object> userMetadata = jwt.getClaimAsMap("user_metadata");
        String suggestedName = userMetadata != null && userMetadata.get("display_name") instanceof String name
                ? name : null;
        userProfiles.createIfAbsent(userId,
                DisplayNames.initialDisplayName(userId, suggestedName, jwt.getClaimAsString("email")));
    }

}
