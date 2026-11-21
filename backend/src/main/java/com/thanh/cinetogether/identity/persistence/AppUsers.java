package com.thanh.cinetogether.identity.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUsers extends JpaRepository<AppUser, UUID> {
}
