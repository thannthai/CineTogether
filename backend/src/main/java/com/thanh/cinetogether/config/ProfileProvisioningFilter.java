package com.thanh.cinetogether.config;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import com.thanh.cinetogether.identity.ProfileProvisioner;

/**
 * Runs after the bearer token is validated and ensures the caller has a profile. Not a bean: Spring Boot would
 * register a filter bean a second time in the servlet chain, outside Spring Security.
 */
class ProfileProvisioningFilter extends OncePerRequestFilter {

    private final ProfileProvisioner profileProvisioner;

    ProfileProvisioningFilter(ProfileProvisioner profileProvisioner) {
        this.profileProvisioner = profileProvisioner;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken authentication) {
            profileProvisioner.ensureProfile(authentication.getToken());
        }
        chain.doFilter(request, response);
    }

}
