package com.tfxsoftware.memserver.modules.players;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import com.tfxsoftware.memserver.modules.heroes.Hero.HeroRole;

@Repository
public interface PlayerRoleMasteryRepository extends JpaRepository<PlayerRoleMastery, UUID> {
    Optional<PlayerRoleMastery> findByPlayerIdAndRole(UUID playerId, HeroRole role);
}
