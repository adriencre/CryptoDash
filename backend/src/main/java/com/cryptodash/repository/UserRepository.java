package com.cryptodash.repository;

import com.cryptodash.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    Optional<User> findByAccountName(String accountName);
    boolean existsByEmail(String email);
    boolean existsByAccountName(String accountName);
}
