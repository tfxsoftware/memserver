package com.tfxsoftware.memserver.modules.users;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    boolean existsByEmailIgnoreCase(String email);
    boolean existsByUsernameIgnoreCase(String username);
    
    // Find user by email (case-insensitive) - more common for email lookups
    Optional<User> findByEmailIgnoreCase(String email);

}

