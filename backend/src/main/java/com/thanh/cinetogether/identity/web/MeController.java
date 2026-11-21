package com.thanh.cinetogether.identity.web;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.thanh.cinetogether.identity.application.Profile;
import com.thanh.cinetogether.identity.application.Profiles;

@RestController
@RequestMapping("/api/v1/me")
class MeController {

    private final Profiles profiles;

    MeController(Profiles profiles) {
        this.profiles = profiles;
    }

    // The user is always the token's subject, never something the client sends.
    @GetMapping
    Profile get(@AuthenticationPrincipal Jwt jwt) {
        return profiles.get(UUID.fromString(jwt.getSubject()));
    }

    @PatchMapping
    Profile update(@AuthenticationPrincipal Jwt jwt, @RequestBody UpdateProfileRequest request) {
        return profiles.rename(UUID.fromString(jwt.getSubject()), request.displayName());
    }

}
