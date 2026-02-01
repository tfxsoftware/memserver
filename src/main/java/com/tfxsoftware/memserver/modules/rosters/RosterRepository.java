package com.tfxsoftware.memserver.modules.rosters;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RosterRepository extends JpaRepository<Roster, UUID> {
    boolean existsByOwnerId(UUID ownerId);
}
