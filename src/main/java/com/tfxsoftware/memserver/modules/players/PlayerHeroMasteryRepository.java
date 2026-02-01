package com.tfxsoftware.memserver.modules.players;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlayerHeroMasteryRepository extends JpaRepository<PlayerHeroMastery, UUID> {
    Optional<PlayerHeroMastery> findByPlayerIdAndHeroId(UUID playerId, UUID heroId);
}
